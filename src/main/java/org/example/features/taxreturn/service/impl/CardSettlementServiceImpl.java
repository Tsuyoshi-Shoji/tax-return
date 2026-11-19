package org.example.features.taxreturn.service.impl;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;

import org.example.entity.AccountingTransaction;
import org.example.entity.ExpenseEntry;
import org.example.entity.FiscalYear;
import org.example.entity.JournalEntry;
import org.example.entity.JournalLine;
import org.example.entity.TransactionSettlementEvent;
import org.example.features.accounting.AccountMapping;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.accounting.service.AccountMappingService;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.dto.CardSettlementPageData;
import org.example.features.taxreturn.form.CardSettlementForm;
import org.example.features.taxreturn.service.CardSettlementService;
import org.example.repository.AccountingTransactionRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.TransactionSettlementEventRepository;
import org.example.repository.TransactionSettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardSettlementServiceImpl implements CardSettlementService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");
    private static final String CLOSED = "closed";
    private static final String STATUS_DRAFT = "draft";
    private static final String TX_TYPE_TRANSFER = "TRANSFER";
    private static final String SETTLEMENT_STATUS_SETTLED = "SETTLED";
    private static final String SETTLEMENT_STATUS_UNSETTLED = "UNSETTLED";
    private static final String SETTLEMENT_STATUS_PARTIALLY_SETTLED = "PARTIALLY_SETTLED";
    private static final String SOURCE_TYPE_CARD_SETTLEMENT = "CARD_SETTLEMENT";

    private final BusinessProfileRepository businessProfiles;
    private final FiscalYearRepository fiscalYears;
    private final ExpenseEntryRepository expenseEntries;
    private final AccountingTransactionRepository transactions;
    private final JournalEntryRepository journals;
    private final TransactionSettlementRepository settlementAllocations;
    private final TransactionSettlementEventRepository settlementEvents;
    private final AccountMappingService accountMappingService;

    public CardSettlementServiceImpl(BusinessProfileRepository businessProfiles, FiscalYearRepository fiscalYears,
            ExpenseEntryRepository expenseEntries, AccountingTransactionRepository transactions,
            JournalEntryRepository journals, TransactionSettlementRepository settlementAllocations,
            TransactionSettlementEventRepository settlementEvents, AccountMappingService accountMappingService) {
        this.businessProfiles = businessProfiles;
        this.fiscalYears = fiscalYears;
        this.expenseEntries = expenseEntries;
        this.transactions = transactions;
        this.journals = journals;
        this.settlementAllocations = settlementAllocations;
        this.settlementEvents = settlementEvents;
        this.accountMappingService = accountMappingService;
    }

    @Override
    @Transactional(readOnly = true)
    public CardSettlementPageData load(Long appUserId, int year) {
        FiscalYear fiscalYear = loadOwnedFiscalYear(appUserId, year);
        AccountMapping mapping = accountMappingService.snapshot();
        List<CardSettlementPageData.AccountOption> accountOptions = List.of(
                new CardSettlementPageData.AccountOption(
                        AccountMasterSeed.ACC_BANK_DEPOSIT,
                        mapping.byCode(AccountMasterSeed.ACC_BANK_DEPOSIT).map(AccountMapping.AccountRef::name)
                                .orElse("普通預金")),
                new CardSettlementPageData.AccountOption(
                        AccountMasterSeed.ACC_CASH,
                        mapping.byCode(AccountMasterSeed.ACC_CASH).map(AccountMapping.AccountRef::name)
                                .orElse("現金")));
        return new CardSettlementPageData(
                loadCandidates(fiscalYear.getId()),
                accountOptions,
                LocalDate.now().toString(),
                UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    @Transactional
    public List<String> save(Long appUserId, Integer year, CardSettlementForm form) {
        FiscalYear fiscalYear = loadOwnedFiscalYear(appUserId, year);
        if (CLOSED.equals(fiscalYear.getStatus()) || fiscalYear.getClosedAt() != null) {
            return List.of("締め済み年度は登録できません。");
        }

        List<String> errors = new ArrayList<>();
        List<Long> sourceTransactionIds = parseIds(form.getSourceTransactionIds(), errors);
        LocalDate settlementDate = parseDate(form.getSettlementDate(), fiscalYear, errors);
        BigDecimal feeAmount = parseAmount(form.getFeeAmount(), "手数料", false, errors);
        String accountCode = text(form.getAccountCode(), 50);
        String memo = text(form.getMemo(), 255);
        String idempotencyKey = text(form.getIdempotencyKey(), 64);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            errors.add("送信キーを取得できませんでした。画面を再読み込みして再実行してください。");
        }
        if (accountCode == null || accountCode.isBlank()) {
            errors.add("引き落とし口座を選択してください。");
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        List<TransactionSettlementEvent> existingEvents = settlementEvents
                .findByFiscalYearIdAndIdempotencyKey(fiscalYear.getId(), idempotencyKey);
        if (!existingEvents.isEmpty()) {
            return List.of();
        }

        AccountMapping mapping = accountMappingService.snapshot();
        AccountMapping.AccountRef creditAccount = mapping.byCode(accountCode).orElse(null);
        if (creditAccount == null) {
            return List.of("引き落とし口座が不正です。");
        }

        List<AccountingTransaction> sources = new ArrayList<>();
        List<BigDecimal> sourceOutstanding = new ArrayList<>();
        BigDecimal settlementTotal = BigDecimal.ZERO;
        for (Long sourceTransactionId : sourceTransactionIds) {
            AccountingTransaction source = transactions.findOwnedByIdForUpdate(sourceTransactionId, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Source transaction not found"));
            if (!source.getFiscalYear().getId().equals(fiscalYear.getId())) {
                return List.of("対象年度外の取引は登録できません。");
            }
            ExpenseEntry sourceExpense = expenseEntries.findOwnedCardByAccountingTransactionId(sourceTransactionId, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Card expense not found"));
            if (sourceExpense.getAccountingTransaction() == null
                    || !sourceExpense.getAccountingTransaction().getId().equals(sourceTransactionId)) {
                return List.of("対象取引の整合性に問題があります。");
            }

            BigDecimal settledTotalBefore = settledTotal(sourceTransactionId);
            BigDecimal outstanding = safe(source.getTotalAmount()).subtract(settledTotalBefore);
            if (outstanding.signum() <= 0) {
                return List.of("選択した取引に消込済みのものが含まれています。最新状態で再選択してください。");
            }
            sources.add(source);
            sourceOutstanding.add(outstanding);
            settlementTotal = settlementTotal.add(outstanding);
        }
        settlementTotal = settlementTotal.add(feeAmount);

        AccountingTransaction settlementTx = new AccountingTransaction();
        settlementTx.setFiscalYear(fiscalYear);
        settlementTx.setTransactionDate(settlementDate);
        settlementTx.setTransactionType(TX_TYPE_TRANSFER);
        settlementTx.setSummary(buildSummary(sources, memo));
        settlementTx.setTotalAmount(settlementTotal);
        settlementTx.setStatus(STATUS_DRAFT);
        settlementTx.setSettlementStatus(SETTLEMENT_STATUS_SETTLED);
        settlementTx.setSettlementReferenceId(sourceTransactionIds.get(0));
        settlementTx = transactions.save(settlementTx);

        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setFiscalYear(fiscalYear);
        journalEntry.setAccountingTransaction(settlementTx);
        journalEntry.setSourceType(SOURCE_TYPE_CARD_SETTLEMENT);
        journalEntry.setSourceId(settlementTx.getId());
        journalEntry.setEntryDate(settlementDate);
        journalEntry.setSummary(settlementTx.getSummary());
        journalEntry.setStatus(STATUS_DRAFT);
        List<JournalLine> lines = new ArrayList<>();
        int lineNo = 1;
        for (int i = 0; i < sources.size(); i++) {
            AccountingTransaction source = sources.get(i);
            BigDecimal outstanding = sourceOutstanding.get(i);
            lines.add(line(mapping, lineNo++, "D", "未払金", outstanding, source.getSummary()));
        }
        if (feeAmount.signum() > 0) {
            lines.add(line(mapping, lineNo++, "D", "支払手数料", feeAmount, "カード引き落とし手数料"));
        }
        lines.add(line(mapping, lineNo, "C", creditAccount.name(), settlementTotal, settlementTx.getSummary()));
        journalEntry.setLines(lines);
        journals.save(journalEntry);

        for (int i = 0; i < sources.size(); i++) {
            AccountingTransaction source = sources.get(i);
            BigDecimal outstanding = sourceOutstanding.get(i);
            TransactionSettlementEvent event = new TransactionSettlementEvent();
            event.setFiscalYear(fiscalYear);
            event.setSourceTransaction(source);
            event.setSettlementTransaction(settlementTx);
            event.setSettledAmount(outstanding);
            event.setFeeAmount(BigDecimal.ZERO);
            event.setIdempotencyKey(idempotencyKey);
            settlementEvents.save(event);
            source.setSettlementStatus(SETTLEMENT_STATUS_SETTLED);
            source.setSettlementReferenceId(settlementTx.getId());
            transactions.save(source);
        }

        return List.of();
    }

    private List<CardSettlementPageData.Candidate> loadCandidates(Long fiscalYearId) {
        return expenseEntries.findCardSettlementCandidatesByFiscalYearId(fiscalYearId).stream()
                .map(entry -> {
                    AccountingTransaction tx = entry.getAccountingTransaction();
                    if (tx == null || tx.getId() == null) {
                        return null;
                    }
                    BigDecimal total = safe(tx.getTotalAmount());
                    BigDecimal settled = settledTotal(tx.getId());
                    BigDecimal outstanding = total.subtract(settled);
                    if (outstanding.signum() <= 0) {
                        return null;
                    }
                    String summary = text(entry.getDetails(), 255);
                    if (summary == null || summary.isBlank()) {
                        summary = tx.getSummary();
                    }
                    if (summary == null || summary.isBlank()) {
                        summary = "クレジットカード利用";
                    }
                    return new CardSettlementPageData.Candidate(
                            tx.getId(),
                            tx.getTransactionDate() == null ? "" : tx.getTransactionDate().toString(),
                            summary,
                            total,
                            settled,
                            outstanding);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CardSettlementPageData.Candidate::getTransactionDate)
                        .thenComparing(CardSettlementPageData.Candidate::getTransactionId))
                .toList();
    }

    private BigDecimal settledTotal(Long transactionId) {
        return safe(settlementAllocations.sumSettledAmountBySourceTransactionId(transactionId))
                .add(safe(settlementEvents.sumSettledAmountBySourceTransactionId(transactionId)));
    }

    private FiscalYear loadOwnedFiscalYear(Long appUserId, Integer year) {
        var profile = businessProfiles.findByAppUserId(appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Business profile not found"));
        List<FiscalYear> years = fiscalYears.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId());
        return years.stream().filter(y -> year == null || year.equals(y.getTaxYear())).findFirst()
                .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
    }

    private static LocalDate parseDate(String raw, FiscalYear fiscalYear, List<String> errors) {
        String value = text(raw, 20);
        if (value == null || value.isBlank()) {
            errors.add("引き落とし日を入力してください。");
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(value);
            if (parsed.isBefore(fiscalYear.getPeriodStart()) || parsed.isAfter(fiscalYear.getPeriodEnd())) {
                errors.add("引き落とし日は対象年度内の日付を指定してください。");
            }
            return parsed;
        } catch (RuntimeException ex) {
            errors.add("引き落とし日はYYYY-MM-DD形式で入力してください。");
            return null;
        }
    }

    private static List<Long> parseIds(List<String> rawValues, List<String> errors) {
        if (rawValues == null || rawValues.isEmpty()) {
            errors.add("登録対象を1件以上選択してください。");
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String raw : rawValues) {
            String value = text(raw, 30);
            if (value == null || value.isBlank() || !value.matches("\\d+")) {
                errors.add("登録対象に不正な値が含まれています。");
                return List.of();
            }
            ids.add(Long.valueOf(value));
        }
        if (ids.isEmpty()) {
            errors.add("登録対象を1件以上選択してください。");
            return List.of();
        }
        return new ArrayList<>(ids);
    }

    private static BigDecimal parseAmount(String raw, String label, boolean required, List<String> errors) {
        String value = text(raw, 32);
        if (value == null || value.isBlank()) {
            if (required) {
                errors.add(label + "を入力してください。");
            }
            return BigDecimal.ZERO;
        }
        if (!value.matches("\\d{1,10}(\\.\\d{1,2})?")) {
            errors.add(label + "は0〜9,999,999,999.99の数値で入力してください。");
            return BigDecimal.ZERO;
        }
        BigDecimal parsed = new BigDecimal(value);
        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            errors.add(label + "は0〜9,999,999,999.99の範囲で入力してください。");
            return BigDecimal.ZERO;
        }
        if (required && parsed.signum() <= 0) {
            errors.add(label + "は1円以上で入力してください。");
            return BigDecimal.ZERO;
        }
        if (!required && parsed.signum() < 0) {
            errors.add(label + "は0円以上で入力してください。");
            return BigDecimal.ZERO;
        }
        return parsed;
    }

    private static String buildSummary(List<AccountingTransaction> sources, String memo) {
        String first = sources.isEmpty() || sources.get(0).getSummary() == null || sources.get(0).getSummary().isBlank()
                ? "クレジットカード利用分"
                : sources.get(0).getSummary().trim();
        if (first.length() > 120) {
            first = first.substring(0, 120);
        }
        String base = "カード引き落とし(" + sources.size() + "件): " + first;
        if (sources.size() > 1) {
            base = base + " ほか";
        }
        String summary;
        if (memo == null || memo.isBlank()) {
            summary = base;
        } else {
            summary = base + " / " + memo;
        }
        return summary.length() > 255 ? summary.substring(0, 255) : summary;
    }

    private JournalLine line(AccountMapping mapping, int lineNo, String side, String accountName, BigDecimal amount, String memo) {
        JournalLine line = new JournalLine();
        line.setLineNo(lineNo);
        line.setDebitCredit(side);
        line.setAccountCode(mapping.byName(accountName).map(AccountMapping.AccountRef::code).orElse(null));
        line.setAccountName(accountName);
        line.setAmount(safe(amount));
        line.setMemo(text(memo, 255));
        return line;
    }

    private static String text(String value, int maxLength) {
        String normalized = value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

