package org.example.features.taxreturn.balance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.entity.FiscalYear;
import org.example.entity.FixedAsset;
import org.example.entity.JournalEntry;
import org.example.entity.JournalLine;
import org.example.entity.SettlementAdjustment;
import org.example.features.accounting.AccountMapping;
import org.example.features.accounting.AccountMapping.AccountRef;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.repository.FiscalYearRepository;
import org.example.repository.FixedAssetRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.SettlementAdjustmentRepository;
import org.springframework.stereotype.Component;

/**
 * 仕訳の残高から貸借対照表を作成する。
 * <ul>
 * <li>前年分の帳簿がある場合：資産・負債の期首残高は前年の期末残高を繰り越し、元入金＝期首資産合計−期首負債合計とする。
 * 事業主貸・事業主借・青色申告特別控除前の所得金額は期首に0へ戻す（元入金へ振替）。</li>
 * <li>最初の年分：期首残高は決算整理の「期首残高」（普通預金）・「期首棚卸」、および期首より前に取得した固定資産台帳の未償却残高とする。</li>
 * <li>期末残高＝期首残高＋当年分の仕訳（全ソース）の増減。</li>
 * </ul>
 */
@Component
public class BalanceSheetService {
    public static final String UNCLASSIFIED_KEY = "#UNCLASSIFIED";
    private static final String UNCLASSIFIED_NAME = "未分類（科目未設定の仕訳）";

    private final FiscalYearRepository fiscalYearRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final SettlementAdjustmentRepository settlementRepository;
    private final FixedAssetRepository fixedAssetRepository;

    public BalanceSheetService(FiscalYearRepository fiscalYearRepository, JournalEntryRepository journalEntryRepository,
            SettlementAdjustmentRepository settlementRepository, FixedAssetRepository fixedAssetRepository) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.settlementRepository = settlementRepository;
        this.fixedAssetRepository = fixedAssetRepository;
    }

    public Result compute(FiscalYear target, AccountMapping mapping) {
        return compute(target, mapping, Set.of());
    }

    /**
     * @param excludedSourceTypesInTarget 対象年度で集計から除く仕訳の作成元（決算整理仕訳の再計算時に自分自身を除くため）
     */
    public Result compute(FiscalYear target, AccountMapping mapping, Set<String> excludedSourceTypesInTarget) {
        Long profileId = target.getBusinessProfile().getId();
        List<FiscalYear> years = new ArrayList<>(fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profileId));
        years.sort(Comparator.comparing(FiscalYear::getTaxYear));
        List<FixedAsset> assets = fixedAssetRepository.findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(profileId);
        Result previous = null;
        for (FiscalYear year : years) {
            if (year.getTaxYear() > target.getTaxYear()) {
                break;
            }
            boolean isTarget = year.getId().equals(target.getId());
            boolean carried = previous != null && previous.taxYear == year.getTaxYear() - 1;
            Result result = new Result(year.getTaxYear(), carried);
            if (carried) {
                carryForward(previous, result);
            } else {
                initialOpening(year, assets, mapping, result);
            }
            applyJournal(year, mapping, isTarget ? excludedSourceTypesInTarget : Set.of(), result);
            result.finish();
            if (isTarget) {
                return result;
            }
            previous = result;
        }
        Result empty = new Result(target.getTaxYear(), false);
        empty.finish();
        return empty;
    }

    private void carryForward(Result previous, Result result) {
        for (Balance b : previous.balances.values()) {
            if (b.isCarried()) {
                Balance nb = result.balance(b.key, b.code, b.name, b.accountClass, b.normalBalance, b.order);
                nb.opening = b.closing;
            }
        }
    }

    private void initialOpening(FiscalYear year, List<FixedAsset> assets, AccountMapping mapping, Result result) {
        SettlementAdjustment settlement = settlementRepository.findByFiscalYearId(year.getId()).orElse(null);
        if (settlement != null) {
            addOpening(result, mapping, AccountMasterSeed.ACC_BANK_DEPOSIT, safe(settlement.getOpeningBalance()));
            addOpening(result, mapping, AccountMasterSeed.ACC_INVENTORY, safe(settlement.getOpeningInventory()));
        }
        for (FixedAsset asset : assets) {
            if (asset.getAcquisitionDate().isBefore(year.getPeriodStart())) {
                addOpening(result, mapping, asset.getAccountCode(),
                        DepreciationCalculator.bookValueAtStartOf(asset, year.getTaxYear()));
            }
        }
    }

    private void addOpening(Result result, AccountMapping mapping, String code, BigDecimal amount) {
        if (amount.signum() == 0) {
            return;
        }
        AccountRef ref = mapping.byCode(code).orElse(null);
        Balance b = ref == null
                ? result.balance("#" + code, code, code, AccountMasterSeed.CLASS_ASSET, "D", Integer.MAX_VALUE)
                : result.balance(ref);
        b.opening = b.opening.add(amount);
    }

    private void applyJournal(FiscalYear year, AccountMapping mapping, Set<String> excluded, Result result) {
        Set<JournalEntry> entries = new LinkedHashSet<>(journalEntryRepository.findAllWithLinesByFiscalYearId(year.getId()));
        for (JournalEntry entry : entries) {
            if (entry.getSourceType() != null && excluded.contains(entry.getSourceType())) {
                continue;
            }
            for (JournalLine line : entry.getLines()) {
                BigDecimal signed = "D".equalsIgnoreCase(line.getDebitCredit()) ? safe(line.getAmount()) : safe(line.getAmount()).negate();
                AccountRef ref = line.getAccountCode() != null
                        ? mapping.byCode(line.getAccountCode()).orElse(null)
                        : mapping.byName(line.getAccountName()).orElse(null);
                if (ref == null) {
                    Balance b = result.balance(UNCLASSIFIED_KEY, null, UNCLASSIFIED_NAME, AccountMasterSeed.CLASS_ASSET, "D", Integer.MAX_VALUE);
                    b.debitNet = b.debitNet.add(signed);
                    result.unclassifiedAccounts.add(line.getAccountName());
                    continue;
                }
                if (ref.isProfitAndLoss()) {
                    result.profitBeforeBlue = result.profitBeforeBlue.subtract(signed);
                    continue;
                }
                result.balance(ref).debitNet = result.balance(ref).debitNet.add(signed);
            }
        }
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static final class Balance {
        private final String key;
        private final String code;
        private final String name;
        private final String accountClass;
        private final String normalBalance;
        private final int order;
        private BigDecimal opening = BigDecimal.ZERO;
        private BigDecimal debitNet = BigDecimal.ZERO;
        private BigDecimal closing = BigDecimal.ZERO;

        Balance(String key, String code, String name, String accountClass, String normalBalance, int order) {
            this.key = key;
            this.code = code;
            this.name = name;
            this.accountClass = accountClass;
            this.normalBalance = normalBalance;
            this.order = order;
        }

        boolean isCarried() {
            return !AccountMasterSeed.ACC_OWNER_DRAW.equals(code) && !AccountMasterSeed.ACC_OWNER_CONTRIBUTION.equals(code)
                    && !AccountMasterSeed.ACC_CAPITAL.equals(code);
        }

        /** 資産側（借方残）に表示するか。事業主貸・減価償却累計額（控除科目）を含む。 */
        public boolean isAssetSide() {
            return AccountMasterSeed.CLASS_ASSET.equals(accountClass) || AccountMasterSeed.ACC_OWNER_DRAW.equals(code);
        }

        public String getKey() { return key; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getAccountClass() { return accountClass; }
        public int getOrder() { return order; }
        public BigDecimal getOpening() { return opening; }
        public BigDecimal getClosing() { return closing; }
    }

    public static final class Result {
        private final int taxYear;
        private final boolean carriedForward;
        private final Map<String, Balance> balances = new LinkedHashMap<>();
        private final Set<String> unclassifiedAccounts = new LinkedHashSet<>();
        private BigDecimal profitBeforeBlue = BigDecimal.ZERO;
        private BigDecimal capital = BigDecimal.ZERO;
        private BigDecimal assetsOpening = BigDecimal.ZERO;
        private BigDecimal assetsClosing = BigDecimal.ZERO;
        private BigDecimal liabilitiesOpening = BigDecimal.ZERO;
        private BigDecimal liabilitiesClosing = BigDecimal.ZERO;

        Result(int taxYear, boolean carriedForward) {
            this.taxYear = taxYear;
            this.carriedForward = carriedForward;
        }

        Balance balance(AccountRef ref) {
            return balance(ref.code(), ref.code(), ref.name(), ref.accountClass(),
                    isCreditNormal(ref) ? "C" : "D", ref.displayOrder());
        }

        private static boolean isCreditNormal(AccountRef ref) {
            return AccountMasterSeed.CLASS_LIABILITY.equals(ref.accountClass())
                    || AccountMasterSeed.ACC_OWNER_CONTRIBUTION.equals(ref.code())
                    || AccountMasterSeed.ACC_CAPITAL.equals(ref.code());
        }

        Balance balance(String key, String code, String name, String accountClass, String normal, int order) {
            return balances.computeIfAbsent(key, k -> new Balance(key, code, name, accountClass, normal, order));
        }

        void finish() {
            BigDecimal openingAssets = BigDecimal.ZERO;
            BigDecimal openingLiabilities = BigDecimal.ZERO;
            for (Balance b : balances.values()) {
                if (AccountMasterSeed.ACC_CAPITAL.equals(b.code)) {
                    continue;
                }
                if (b.isAssetSide()) {
                    b.closing = b.opening.add(b.debitNet);
                    openingAssets = openingAssets.add(b.opening);
                } else {
                    b.closing = b.opening.subtract(b.debitNet);
                    openingLiabilities = openingLiabilities.add(b.opening);
                }
            }
            capital = openingAssets.subtract(openingLiabilities);
            Balance capitalBalance = balances.get(AccountMasterSeed.ACC_CAPITAL);
            if (capitalBalance != null && capitalBalance.debitNet.signum() != 0) {
                // 元入金を直接動かす仕訳は想定しない。差額は元入金の増減として扱う。
                capital = capital.subtract(capitalBalance.debitNet);
            }
            assetsOpening = openingAssets;
            liabilitiesOpening = openingLiabilities.add(capital);
            assetsClosing = BigDecimal.ZERO;
            liabilitiesClosing = capital.add(profitBeforeBlue);
            for (Balance b : balances.values()) {
                if (AccountMasterSeed.ACC_CAPITAL.equals(b.code)) {
                    continue;
                }
                if (b.isAssetSide()) {
                    assetsClosing = assetsClosing.add(b.closing);
                } else {
                    liabilitiesClosing = liabilitiesClosing.add(b.closing);
                }
            }
        }

        public List<Balance> assetSide() {
            return sorted(true);
        }

        public List<Balance> liabilitySide() {
            return sorted(false);
        }

        private List<Balance> sorted(boolean assetSide) {
            return balances.values().stream()
                    .filter(b -> !AccountMasterSeed.ACC_CAPITAL.equals(b.code))
                    .filter(b -> b.isAssetSide() == assetSide)
                    .sorted(Comparator.comparingInt(Balance::getOrder).thenComparing(Balance::getName))
                    .toList();
        }

        public BigDecimal closingOf(String code) {
            Balance b = balances.get(code);
            return b == null ? BigDecimal.ZERO : b.closing;
        }

        public BigDecimal openingOf(String code) {
            Balance b = balances.get(code);
            return b == null ? BigDecimal.ZERO : b.opening;
        }

        public int getTaxYear() { return taxYear; }
        public boolean isCarriedForward() { return carriedForward; }
        public Set<String> getUnclassifiedAccounts() { return unclassifiedAccounts; }
        public BigDecimal getProfitBeforeBlue() { return profitBeforeBlue; }
        public BigDecimal getCapital() { return capital; }
        public BigDecimal getAssetsOpening() { return assetsOpening; }
        public BigDecimal getAssetsClosing() { return assetsClosing; }
        public BigDecimal getLiabilitiesOpening() { return liabilitiesOpening; }
        public BigDecimal getLiabilitiesClosing() { return liabilitiesClosing; }
    }
}
