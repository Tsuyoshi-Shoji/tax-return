package org.example.features.taxreturn.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.example.entity.FiscalYear;
import org.example.entity.JournalEntry;
import org.example.entity.JournalLine;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.report.dto.ReportYearOption;
import org.example.features.taxreturn.closing.YearSnapshot;
import org.example.features.taxreturn.dto.JournalReviewPage;
import org.example.features.taxreturn.service.JournalReviewService;
import org.example.repository.AccountingTransactionRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.JournalEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalReviewServiceImpl implements JournalReviewService {
    private static final int PAGE_SIZE = 20;
    private static final Logger AUDIT = LoggerFactory.getLogger("audit.journal-review");
    private final BusinessProfileRepository profiles;
    private final FiscalYearRepository years;
    private final JournalEntryRepository journals;
    private final AccountingTransactionRepository transactions;

    public JournalReviewServiceImpl(BusinessProfileRepository profiles, FiscalYearRepository years,
            JournalEntryRepository journals, AccountingTransactionRepository transactions) {
        this.profiles = profiles;
        this.years = years;
        this.journals = journals;
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public JournalReviewPage load(Long userId, Integer year, String status, int pageNumber) {
        if (!List.of("all", "draft", "reviewed").contains(status) || pageNumber < 0 || pageNumber > 100000) {
            throw new IllegalArgumentException("表示条件が不正です。");
        }
        List<FiscalYear> options = ownedYears(userId);
        FiscalYear selected = resolve(options, year);
        var page = journals.findReviewPage(selected.getId(), userId, "all".equals(status) ? null : status,
                PageRequest.of(pageNumber, PAGE_SIZE));
        if (pageNumber > 0 && pageNumber >= page.getTotalPages()) {
            pageNumber = Math.max(0, page.getTotalPages() - 1);
            page = journals.findReviewPage(selected.getId(), userId, "all".equals(status) ? null : status,
                    PageRequest.of(pageNumber, PAGE_SIZE));
        }
        return new JournalReviewPage(selected.getTaxYear(), options.stream()
                .map(y -> new ReportYearOption(y.getTaxYear(), y.getTaxYear() + "年分")).toList(),
                !"open".equals(selected.getStatus()) || selected.getClosedAt() != null, status,
                pageNumber, page.getTotalPages(), page.getTotalElements(),
                journals.countByFiscalYearIdAndStatus(selected.getId(), "draft"),
                transactions.countDraftsWithoutJournal(selected.getId()),
                page.getContent().stream().map(e -> view(e, selected)).toList());
    }

    @Override
    @Transactional
    public void confirm(Long userId, int year, Long entryId, String fingerprint) {
        FiscalYear locked = years.findOwnedByTaxYearForUpdate(year, userId)
                .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
        JournalEntry entry = journals.findOwnedForReview(entryId, locked.getId(), userId)
                .orElseThrow(() -> new EntryNotFoundException("Journal not found"));
        if (!"open".equals(locked.getStatus()) || locked.getClosedAt() != null) {
            reject(userId, year, entryId, "closed", "締め済み年度の仕訳は変更できません。");
        }
        if (!Objects.equals(fingerprint(entry), fingerprint)) {
            reject(userId, year, entryId, "stale", "表示後に仕訳が変更されました。最新の内容を確認してください。");
        }
        String error = validationError(entry, locked);
        if (error != null) {
            reject(userId, year, entryId, "invalid", error);
        }
        if (reviewed(entry)) {
            return;
        }
        if (!"draft".equals(entry.getStatus())) {
            reject(userId, year, entryId, "status", "この状態の仕訳は確認済みにできません。");
        }
        // 取引の確定とは分離し、収支の修正時は既存の仕訳同期処理で未確認へ戻す。
        entry.setStatus("reviewed");
        journals.flush();
        AUDIT.info("confirm userId={} year={} journalId={} fingerprint={} result=reviewed",
                userId, year, entryId, fingerprint);
    }

    private List<FiscalYear> ownedYears(Long userId) {
        var profile = profiles.findByAppUserId(userId)
                .orElseThrow(() -> new EntryNotFoundException("Business profile not found"));
        return years.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId());
    }

    private static FiscalYear resolve(List<FiscalYear> options, Integer year) {
        return options.stream().filter(y -> year == null || year.equals(y.getTaxYear())).findFirst()
                .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
    }

    private static JournalReviewPage.Entry view(JournalEntry e, FiscalYear year) {
        return new JournalReviewPage.Entry(e.getId(), e.getEntryDate(), e.getSummary(), reviewed(e), fingerprint(e),
                sorted(e).stream().map(l -> new JournalReviewPage.Line(l.getDebitCredit(), l.getAccountCode(),
                        l.getAccountName(), l.getAmount(), l.getMemo())).toList(), validationError(e, year));
    }

    private static boolean reviewed(JournalEntry e) {
        return "reviewed".equals(e.getStatus()) || "finalized".equals(e.getStatus());
    }

    private static List<JournalLine> sorted(JournalEntry entry) {
        return entry.getLines().stream().sorted(Comparator.comparing(JournalLine::getLineNo)).toList();
    }

    private static String validationError(JournalEntry entry, FiscalYear year) {
        if (entry.getEntryDate() == null || entry.getEntryDate().isBefore(year.getPeriodStart())
                || entry.getEntryDate().isAfter(year.getPeriodEnd())) {
            return "年度外の仕訳です。元データを修正してください。";
        }
        var transaction = entry.getAccountingTransaction();
        if (transaction != null && (!Objects.equals(transaction.getFiscalYear().getId(), year.getId())
                || !Objects.equals(transaction.getId(), entry.getSourceId())
                || !Objects.equals(transaction.getTransactionDate(), entry.getEntryDate()))) {
            return "元の取引と仕訳が一致していません。元データを確認してください。";
        }
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        if (entry.getLines().isEmpty()) {
            return "明細がないため確認済みにできません。";
        }
        for (JournalLine line : entry.getLines()) {
            if (line.getAmount() == null || line.getAmount().signum() < 0
                    || line.getAccountCode() == null || line.getAccountCode().isBlank()
                    || line.getAccountName() == null || line.getAccountName().isBlank()
                    || !("D".equals(line.getDebitCredit()) || "C".equals(line.getDebitCredit()))) {
                return "科目・金額・借貸区分が不正です。元データを修正してください。";
            }
            if ("D".equals(line.getDebitCredit())) {
                debit = debit.add(line.getAmount());
            } else {
                credit = credit.add(line.getAmount());
            }
        }
        if (debit.signum() <= 0 || debit.compareTo(credit) != 0) {
            return "借方・貸方の金額が一致しないか、合計が0円です。元データを修正してください。";
        }
        return transaction != null && (transaction.getTotalAmount() == null
                || debit.compareTo(transaction.getTotalAmount()) != 0)
                ? "元の取引金額と仕訳の合計が一致していません。元データを確認してください。" : null;
    }

    private static String fingerprint(JournalEntry entry) {
        StringBuilder text = new StringBuilder();
        append(text, entry.getId(), entry.getFiscalYear().getId(), entry.getEntryDate(), entry.getSummary(),
                entry.getSourceType(), entry.getSourceId());
        var tx = entry.getAccountingTransaction();
        if (tx != null) {
            append(text, tx.getId(), tx.getFiscalYear().getId(), tx.getTransactionDate(), tx.getSummary(),
                    tx.getTotalAmount(), tx.getStatus(), tx.getSettlementStatus(), tx.getSettlementReferenceId());
        }
        for (JournalLine line : sorted(entry)) {
            append(text, line.getId(), line.getLineNo(), line.getDebitCredit(), line.getAccountCode(),
                    line.getAccountName(), line.getAmount(), line.getMemo());
        }
        return YearSnapshot.sha256(text.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void append(StringBuilder text, Object... fields) {
        for (Object field : fields) {
            String value = field == null ? "" : field.toString();
            text.append(value.length()).append(':').append(value).append(';');
        }
    }

    private static void reject(Long userId, int year, Long entryId, String result, String message) {
        AUDIT.info("confirm userId={} year={} journalId={} result={}", userId, year, entryId, result);
        throw new EntryConflictException(message);
    }
}
