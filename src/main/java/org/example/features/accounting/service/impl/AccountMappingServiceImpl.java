package org.example.features.accounting.service.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.example.entity.AccountStatementMapping;
import org.example.entity.ExpenseItem;
import org.example.entity.JournalLine;
import org.example.entity.LedgerAccount;
import org.example.entity.StatementLine;
import org.example.entity.UiItemAccountMapping;
import org.example.features.accounting.AccountMapping;
import org.example.features.accounting.AccountMapping.AccountRef;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.accounting.AccountMasterSeed.AccountSeed;
import org.example.features.accounting.AccountMasterSeed.ItemMappingSeed;
import org.example.features.accounting.AccountMasterSeed.LineSeed;
import org.example.features.accounting.AccountMasterSeed.StatementMappingSeed;
import org.example.features.accounting.service.AccountMappingService;
import org.example.repository.AccountStatementMappingRepository;
import org.example.repository.ExpenseItemRepository;
import org.example.repository.JournalLineRepository;
import org.example.repository.LedgerAccountRepository;
import org.example.repository.StatementLineRepository;
import org.example.repository.UiItemAccountMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountMappingServiceImpl implements AccountMappingService {
    private static final String EXPENSE_CATEGORY = "expense";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final StatementLineRepository statementLineRepository;
    private final AccountStatementMappingRepository accountStatementMappingRepository;
    private final UiItemAccountMappingRepository uiItemAccountMappingRepository;
    private final JournalLineRepository journalLineRepository;
    private final ExpenseItemRepository expenseItemRepository;

    public AccountMappingServiceImpl(
            LedgerAccountRepository ledgerAccountRepository,
            StatementLineRepository statementLineRepository,
            AccountStatementMappingRepository accountStatementMappingRepository,
            UiItemAccountMappingRepository uiItemAccountMappingRepository,
            JournalLineRepository journalLineRepository,
            ExpenseItemRepository expenseItemRepository) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.statementLineRepository = statementLineRepository;
        this.accountStatementMappingRepository = accountStatementMappingRepository;
        this.uiItemAccountMappingRepository = uiItemAccountMappingRepository;
        this.journalLineRepository = journalLineRepository;
        this.expenseItemRepository = expenseItemRepository;
    }

    @Override
    @Transactional
    public void ensureMasterData() {
        Map<String, LedgerAccount> accounts = new LinkedHashMap<>();
        ledgerAccountRepository.findAll().forEach(a -> accounts.put(a.getAccountCode(), a));
        for (AccountSeed seed : AccountMasterSeed.ACCOUNTS) {
            if (accounts.containsKey(seed.code())) {
                continue;
            }
            LedgerAccount account = new LedgerAccount();
            account.setAccountCode(seed.code());
            account.setAccountName(seed.name());
            account.setAccountClass(seed.accountClass());
            account.setNormalBalance(seed.normalBalance());
            account.setDisplayOrder(seed.order());
            account.setActive(true);
            accounts.put(seed.code(), ledgerAccountRepository.save(account));
        }

        String form = AccountMasterSeed.FORM_BLUE_GENERAL_PL;
        int fromYear = AccountMasterSeed.FORM_BLUE_GENERAL_PL_FROM_YEAR;
        Set<String> existingLines = new HashSet<>();
        statementLineRepository.findByFormCodeOrderByDisplayOrderAscIdAsc(form)
                .forEach(l -> existingLines.add(l.getLineCode() + "@" + l.getEffectiveFromYear()));
        for (LineSeed seed : AccountMasterSeed.BLUE_GENERAL_PL_LINES) {
            if (existingLines.contains(seed.lineCode() + "@" + fromYear)) {
                continue;
            }
            StatementLine line = new StatementLine();
            line.setFormCode(form);
            line.setLineCode(seed.lineCode());
            line.setLineNumber(seed.number());
            line.setLineLabel(seed.label());
            line.setLineType(seed.type());
            line.setDisplayOrder(seed.number());
            line.setEffectiveFromYear(fromYear);
            statementLineRepository.save(line);
        }

        Set<String> existingStatementMappings = new HashSet<>();
        accountStatementMappingRepository.findByFormCode(form).forEach(m -> existingStatementMappings.add(
                m.getLedgerAccount().getAccountCode() + "@" + m.getEffectiveFromYear()));
        for (StatementMappingSeed seed : AccountMasterSeed.BLUE_GENERAL_PL_MAPPINGS) {
            LedgerAccount account = accounts.get(seed.accountCode());
            if (account == null || existingStatementMappings.contains(seed.accountCode() + "@" + fromYear)) {
                continue;
            }
            AccountStatementMapping mapping = new AccountStatementMapping();
            mapping.setLedgerAccount(account);
            mapping.setFormCode(form);
            mapping.setLineCode(seed.lineCode());
            mapping.setEffectiveFromYear(fromYear);
            accountStatementMappingRepository.save(mapping);
        }

        Set<String> existingItemMappings = new HashSet<>();
        uiItemAccountMappingRepository.findAll().forEach(m -> existingItemMappings.add(
                m.getEntryKind() + ":" + m.getCategoryCode() + ":" + m.getItemCode() + "@" + m.getEffectiveFrom()));
        for (ItemMappingSeed seed : AccountMasterSeed.ITEM_MAPPINGS) {
            String key = seed.kind() + ":" + seed.categoryCode() + ":" + seed.itemCode()
                    + "@" + AccountMasterSeed.ITEM_MAPPING_EFFECTIVE_FROM;
            LedgerAccount account = accounts.get(seed.accountCode());
            if (account == null || existingItemMappings.contains(key)) {
                continue;
            }
            UiItemAccountMapping mapping = new UiItemAccountMapping();
            mapping.setEntryKind(seed.kind());
            mapping.setCategoryCode(seed.categoryCode());
            mapping.setItemCode(seed.itemCode());
            mapping.setLedgerAccount(account);
            mapping.setRequiresReview(seed.requiresReview());
            mapping.setReviewNote(seed.reviewNote());
            mapping.setEffectiveFrom(AccountMasterSeed.ITEM_MAPPING_EFFECTIVE_FROM);
            uiItemAccountMappingRepository.save(mapping);
        }
    }

    @Override
    @Transactional
    public int backfillJournalAccountCodes() {
        List<JournalLine> lines = journalLineRepository.findUncodedLinesInOpenFiscalYears();
        if (lines.isEmpty()) {
            return 0;
        }
        AccountMapping mapping = snapshot();
        // 旧実装は「経費」区分の借方に品目名をそのまま科目名として記録していたため、品目名から科目へ読み替える。
        Map<String, ExpenseItem> expenseItemsByName = new LinkedHashMap<>();
        for (ExpenseItem item : expenseItemRepository.findAll()) {
            if (item.getExpenseCategory() != null && EXPENSE_CATEGORY.equals(item.getExpenseCategory().getCategoryCode())) {
                expenseItemsByName.putIfAbsent(item.getItemName(), item);
            }
        }
        int updated = 0;
        for (JournalLine line : lines) {
            Optional<AccountRef> account = mapping.byName(line.getAccountName());
            if (account.isEmpty() && "D".equalsIgnoreCase(line.getDebitCredit())) {
                ExpenseItem item = expenseItemsByName.get(line.getAccountName());
                if (item != null) {
                    account = mapping.forExpenseItem(item, line.getJournalEntry().getEntryDate());
                }
            }
            if (account.isEmpty()) {
                continue;
            }
            line.setAccountCode(account.get().code());
            line.setAccountName(account.get().name());
            updated++;
        }
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountMapping snapshot() {
        return new AccountMapping(
                ledgerAccountRepository.findAllByOrderByDisplayOrderAscIdAsc(),
                uiItemAccountMappingRepository.findAll(),
                statementLineRepository.findAll(),
                accountStatementMappingRepository.findAll());
    }
}
