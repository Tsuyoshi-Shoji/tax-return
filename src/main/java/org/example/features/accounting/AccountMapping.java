package org.example.features.accounting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.entity.AccountStatementMapping;
import org.example.entity.ExpenseItem;
import org.example.entity.IncomeItem;
import org.example.entity.LedgerAccount;
import org.example.entity.StatementLine;
import org.example.entity.UiItemAccountMapping;

/**
 * 勘定科目マスタ・品目対応・様式欄対応を一括で読み込んだ不変スナップショット。
 * 1リクエスト内で多数の明細を変換するため、行ごとのDB問い合わせを避ける。
 */
public final class AccountMapping {
    private final Map<String, AccountRef> accountsByCode = new LinkedHashMap<>();
    private final Map<String, AccountRef> accountsByName = new LinkedHashMap<>();
    private final Map<String, List<UiItemAccountMapping>> itemMappings = new LinkedHashMap<>();
    private final List<StatementLine> statementLines;
    private final List<AccountStatementMapping> statementMappings;

    public AccountMapping(List<LedgerAccount> accounts, List<UiItemAccountMapping> itemMappings,
            List<StatementLine> statementLines, List<AccountStatementMapping> statementMappings) {
        for (LedgerAccount account : accounts) {
            if (!account.isActive()) {
                continue;
            }
            AccountRef ref = toRef(account, false, null);
            accountsByCode.put(account.getAccountCode(), ref);
            accountsByName.putIfAbsent(account.getAccountName(), ref);
        }
        for (UiItemAccountMapping mapping : itemMappings) {
            this.itemMappings.computeIfAbsent(
                    key(mapping.getEntryKind(), mapping.getCategoryCode(), mapping.getItemCode()), k -> new ArrayList<>())
                    .add(mapping);
        }
        this.itemMappings.values().forEach(list -> list.sort(
                Comparator.comparing(UiItemAccountMapping::getEffectiveFrom).reversed()));
        this.statementLines = List.copyOf(statementLines);
        this.statementMappings = List.copyOf(statementMappings);
    }

    public Optional<AccountRef> byCode(String code) {
        return Optional.ofNullable(code == null ? null : accountsByCode.get(code));
    }

    public Optional<AccountRef> byName(String name) {
        return Optional.ofNullable(name == null ? null : accountsByName.get(name));
    }

    public List<AccountRef> accounts() {
        return accountsByCode.values().stream()
                .sorted(Comparator.comparingInt(AccountRef::displayOrder).thenComparing(AccountRef::code))
                .toList();
    }

    public Optional<AccountRef> forIncomeItem(IncomeItem item, LocalDate date) {
        if (item == null || item.getIncomeCategory() == null) {
            return Optional.empty();
        }
        return resolve(AccountMasterSeed.KIND_INCOME, item.getIncomeCategory().getCategoryCode(), item.getItemCode(), date);
    }

    public Optional<AccountRef> forExpenseItem(ExpenseItem item, LocalDate date) {
        if (item == null || item.getExpenseCategory() == null) {
            return Optional.empty();
        }
        return resolve(AccountMasterSeed.KIND_EXPENSE, item.getExpenseCategory().getCategoryCode(), item.getItemCode(), date);
    }

    public Optional<AccountRef> resolve(String kind, String categoryCode, String itemCode, LocalDate date) {
        List<UiItemAccountMapping> candidates = itemMappings.get(key(kind, categoryCode, itemCode));
        if (candidates == null) {
            return Optional.empty();
        }
        LocalDate target = date == null ? LocalDate.now() : date;
        for (UiItemAccountMapping mapping : candidates) {
            boolean started = !mapping.getEffectiveFrom().isAfter(target);
            boolean notEnded = mapping.getEffectiveTo() == null || !mapping.getEffectiveTo().isBefore(target);
            if (started && notEnded && mapping.getLedgerAccount().isActive()) {
                return Optional.of(toRef(mapping.getLedgerAccount(), mapping.isRequiresReview(), mapping.getReviewNote()));
            }
        }
        return Optional.empty();
    }

    /** 指定年度に有効な様式欄と科目→欄の対応を返す。該当様式がなければ空のレイアウト。 */
    public StatementLayout layout(String formCode, int year) {
        List<StatementLine> lines = statementLines.stream()
                .filter(l -> formCode.equals(l.getFormCode()))
                .filter(l -> activeInYear(l.getEffectiveFromYear(), l.getEffectiveToYear(), year))
                .sorted(Comparator.comparing(StatementLine::getDisplayOrder).thenComparing(StatementLine::getLineNumber))
                .toList();
        Map<String, String> lineByAccount = new LinkedHashMap<>();
        statementMappings.stream()
                .filter(m -> formCode.equals(m.getFormCode()))
                .filter(m -> activeInYear(m.getEffectiveFromYear(), m.getEffectiveToYear(), year))
                .sorted(Comparator.comparing(AccountStatementMapping::getEffectiveFromYear).reversed())
                .forEach(m -> lineByAccount.putIfAbsent(m.getLedgerAccount().getAccountCode(), m.getLineCode()));
        return new StatementLayout(lines, Collections.unmodifiableMap(lineByAccount));
    }

    private static boolean activeInYear(Integer from, Integer to, int year) {
        return (from == null || from <= year) && (to == null || to >= year);
    }

    private static String key(String kind, String categoryCode, String itemCode) {
        return kind + ":" + categoryCode + ":" + itemCode;
    }

    private static AccountRef toRef(LedgerAccount account, boolean requiresReview, String reviewNote) {
        return new AccountRef(account.getAccountCode(), account.getAccountName(), account.getAccountClass(),
                account.getDisplayOrder() == null ? Integer.MAX_VALUE : account.getDisplayOrder(),
                requiresReview, reviewNote);
    }

    public record AccountRef(String code, String name, String accountClass, int displayOrder,
            boolean requiresReview, String reviewNote) {
        public boolean isProfitAndLoss() {
            return AccountMasterSeed.CLASS_REVENUE.equals(accountClass) || AccountMasterSeed.CLASS_EXPENSE.equals(accountClass);
        }
    }

    public record StatementLayout(List<StatementLine> lines, Map<String, String> lineByAccountCode) {
        public boolean isEmpty() {
            return lines.isEmpty();
        }
    }
}
