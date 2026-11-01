package org.example.features.taxreturn.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.example.features.accounting.service.AccountMappingService;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.features.taxreturn.balance.BalanceSheetService;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.repository.FiscalYearRepository;
import org.example.repository.FixedAssetRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.SettlementAdjustmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClosingJournalServiceImpl implements ClosingJournalService {
    public static final String SOURCE_SETTLEMENT = "SETTLEMENT_ADJUSTMENT";
    public static final String SOURCE_DEPRECIATION = "DEPRECIATION";
    public static final String SOURCE_ASSET_ACQUISITION = "FIXED_ASSET_ACQUISITION";
    private static final String CLOSED = "closed";
    private static final String DRAFT = "draft";

    private final FiscalYearRepository fiscalYearRepository;
    private final SettlementAdjustmentRepository settlementRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final FixedAssetRepository fixedAssetRepository;
    private final AccountMappingService accountMappingService;
    private final BalanceSheetService balanceSheetService;

    public ClosingJournalServiceImpl(
            FiscalYearRepository fiscalYearRepository,
            SettlementAdjustmentRepository settlementRepository,
            JournalEntryRepository journalEntryRepository,
            FixedAssetRepository fixedAssetRepository,
            AccountMappingService accountMappingService,
            BalanceSheetService balanceSheetService) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.settlementRepository = settlementRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.fixedAssetRepository = fixedAssetRepository;
        this.accountMappingService = accountMappingService;
        this.balanceSheetService = balanceSheetService;
    }

    @Override
    @Transactional
    public void rebuild(Long fiscalYearId) {
        FiscalYear fiscalYear = fiscalYearRepository.findById(fiscalYearId).orElse(null);
        if (fiscalYear == null) {
            return;
        }
        // 当年の変更は翌年以降の期首残高（繰越）にも影響するため、以降の未確定年度もあわせて再生成する。
        rebuildFrom(fiscalYear.getBusinessProfile().getId(), fiscalYear.getTaxYear());
    }

    @Override
    @Transactional
    public void rebuildOpenYears(Long businessProfileId) {
        rebuildFrom(businessProfileId, Integer.MIN_VALUE);
    }

    private void rebuildFrom(Long businessProfileId, int fromYear) {
        List<FiscalYear> years = new ArrayList<>(fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(businessProfileId));
        years.sort(Comparator.comparing(FiscalYear::getTaxYear));
        AccountMapping mapping = accountMappingService.snapshot();
        List<FixedAsset> assets = fixedAssetRepository.findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(businessProfileId);
        for (FiscalYear year : years) {
            if (year.getTaxYear() < fromYear || CLOSED.equals(year.getStatus())) {
                continue;
            }
            rebuildAcquisitions(year, assets, mapping);
            rebuildDepreciation(year, assets, mapping);
            journalEntryRepository.flush();
            rebuildSettlement(year, mapping);
            journalEntryRepository.flush();
        }
    }

    private void rebuildAcquisitions(FiscalYear year, List<FixedAsset> assets, AccountMapping mapping) {
        Set<Long> keep = new HashSet<>();
        for (FixedAsset asset : assets) {
            if (asset.getAcquisitionDate().isBefore(year.getPeriodStart()) || asset.getAcquisitionDate().isAfter(year.getPeriodEnd())) {
                continue;
            }
            List<JournalLine> lines = new ArrayList<>();
            String assetName = mapping.byCode(asset.getAccountCode()).map(AccountRef::name).orElse(asset.getAccountCode());
            if ("OWNER_FUNDS".equals(asset.getAcquisitionFunding())) {
                lines.add(line(mapping, 1, "D", asset.getAccountCode(), assetName, asset.getAcquisitionCost(), asset.getAssetName()));
                lines.add(line(mapping, 2, "C", AccountMasterSeed.ACC_OWNER_CONTRIBUTION, "事業主借", asset.getAcquisitionCost(), "私的資金で取得"));
            } else if (!AccountMasterSeed.ACC_TOOLS_EQUIPMENT.equals(asset.getAccountCode())) {
                // 支出登録の「固定資産購入」は工具器具備品で仕訳されるため、台帳の資産科目へ振り替える。
                lines.add(line(mapping, 1, "D", asset.getAccountCode(), assetName, asset.getAcquisitionCost(), asset.getAssetName()));
                lines.add(line(mapping, 2, "C", AccountMasterSeed.ACC_TOOLS_EQUIPMENT, "工具器具備品", asset.getAcquisitionCost(), "資産科目への振替"));
            }
            if (lines.isEmpty()) {
                continue;
            }
            keep.add(asset.getId());
            JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceId(SOURCE_ASSET_ACQUISITION, asset.getId())
                    .orElseGet(JournalEntry::new);
            fill(entry, year, SOURCE_ASSET_ACQUISITION, asset.getId(), asset.getAcquisitionDate(), "固定資産の取得：" + asset.getAssetName());
            entry.setLines(lines);
            journalEntryRepository.save(entry);
        }
        for (JournalEntry stale : journalEntryRepository.findByFiscalYearIdAndSourceType(year.getId(), SOURCE_ASSET_ACQUISITION)) {
            if (!keep.contains(stale.getSourceId())) {
                journalEntryRepository.delete(stale);
            }
        }
    }

    private void rebuildDepreciation(FiscalYear year, List<FixedAsset> assets, AccountMapping mapping) {
        BigDecimal business = BigDecimal.ZERO;
        BigDecimal privatePortion = BigDecimal.ZERO;
        Map<String, BigDecimal> credits = new LinkedHashMap<>();
        for (FixedAsset asset : assets) {
            if (asset.getAcquisitionDate().isAfter(year.getPeriodEnd())) {
                continue;
            }
            DepreciationCalculator.YearResult r = DepreciationCalculator.forYear(asset, year.getTaxYear());
            if (r.depreciation().signum() <= 0) {
                continue;
            }
            business = business.add(r.businessPortion());
            privatePortion = privatePortion.add(r.privatePortion());
            credits.merge(asset.getAccountCode(), r.depreciation(), BigDecimal::add);
        }
        JournalEntry existing = journalEntryRepository.findBySourceTypeAndSourceId(SOURCE_DEPRECIATION, year.getId()).orElse(null);
        if (credits.isEmpty()) {
            if (existing != null) {
                journalEntryRepository.delete(existing);
            }
            return;
        }
        List<JournalLine> lines = new ArrayList<>();
        int no = 1;
        if (business.signum() > 0) {
            lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_DEPRECIATION, "減価償却費", business, "固定資産台帳"));
        }
        if (privatePortion.signum() > 0) {
            lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_OWNER_DRAW, "事業主貸", privatePortion, "減価償却費の家事分"));
        }
        for (Map.Entry<String, BigDecimal> credit : credits.entrySet()) {
            String name = mapping.byCode(credit.getKey()).map(AccountRef::name).orElse(credit.getKey());
            lines.add(line(mapping, no++, "C", credit.getKey(), name, credit.getValue(), "減価償却（直接法）"));
        }
        JournalEntry entry = existing != null ? existing : new JournalEntry();
        fill(entry, year, SOURCE_DEPRECIATION, year.getId(), year.getPeriodEnd(), "減価償却費の計上");
        entry.setLines(lines);
        journalEntryRepository.save(entry);
    }

    private void rebuildSettlement(FiscalYear year, AccountMapping mapping) {
        SettlementAdjustment settlement = settlementRepository.findByFiscalYearId(year.getId()).orElse(null);
        if (settlement == null) {
            return;
        }
        JournalEntry existing = journalEntryRepository.findBySourceTypeAndSourceId(SOURCE_SETTLEMENT, settlement.getId()).orElse(null);
        List<JournalLine> lines = new ArrayList<>();
        int no = 1;
        BigDecimal inventoryDelta = safe(settlement.getOpeningInventory()).subtract(safe(settlement.getClosingInventory()));
        if (inventoryDelta.signum() > 0) {
            lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_PURCHASES, "仕入高", inventoryDelta, "棚卸調整"));
            lines.add(line(mapping, no++, "C", AccountMasterSeed.ACC_INVENTORY, "棚卸資産", inventoryDelta, "棚卸調整"));
        } else if (inventoryDelta.signum() < 0) {
            lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_INVENTORY, "棚卸資産", inventoryDelta.abs(), "棚卸調整"));
            lines.add(line(mapping, no++, "C", AccountMasterSeed.ACC_PURCHASES, "仕入高", inventoryDelta.abs(), "棚卸調整"));
        }
        if (settlement.isBalancesConfirmed()) {
            BalanceSheetService.Result book = balanceSheetService.compute(year, mapping, Set.of(SOURCE_SETTLEMENT));
            no = adjust(lines, no, mapping, book, AccountMasterSeed.ACC_ACCOUNTS_RECEIVABLE, "売掛金", settlement.getAccountsReceivable(), true);
            no = adjust(lines, no, mapping, book, AccountMasterSeed.ACC_OTHER_RECEIVABLE, "未収入金", settlement.getUnreceivedIncome(), true);
            no = adjust(lines, no, mapping, book, AccountMasterSeed.ACC_ACCOUNTS_PAYABLE, "買掛金", settlement.getAccountsPayable(), false);
            adjust(lines, no, mapping, book, AccountMasterSeed.ACC_ACCRUED, "未払金", settlement.getUnpaidExpenses(), false);
        }
        if (lines.isEmpty()) {
            if (existing != null) {
                journalEntryRepository.delete(existing);
            }
            return;
        }
        JournalEntry entry = existing != null ? existing : new JournalEntry();
        fill(entry, year, SOURCE_SETTLEMENT, settlement.getId(), year.getPeriodEnd(), "決算整理仕訳");
        entry.setLines(lines);
        journalEntryRepository.save(entry);
    }

    /** 期末残高（入力値）と帳簿残高の差額を普通預金との間で消込（決済）する。 */
    private int adjust(List<JournalLine> lines, int no, AccountMapping mapping, BalanceSheetService.Result book,
            String code, String name, BigDecimal target, boolean asset) {
        BigDecimal diff = safe(target).subtract(book.closingOf(code));
        if (diff.signum() == 0) {
            return no;
        }
        boolean debitAccount = asset == diff.signum() > 0;
        String memo = "期末残高への調整（差額を普通預金で決済）";
        if (debitAccount) {
            lines.add(line(mapping, no++, "D", code, name, diff.abs(), memo));
            lines.add(line(mapping, no++, "C", AccountMasterSeed.ACC_BANK_DEPOSIT, "普通預金", diff.abs(), memo));
        } else {
            lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_BANK_DEPOSIT, "普通預金", diff.abs(), memo));
            lines.add(line(mapping, no++, "C", code, name, diff.abs(), memo));
        }
        return no;
    }

    private static void fill(JournalEntry entry, FiscalYear year, String sourceType, Long sourceId,
            java.time.LocalDate date, String summary) {
        entry.setFiscalYear(year);
        entry.setAccountingTransaction(null);
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        entry.setEntryDate(date);
        entry.setSummary(summary);
        entry.setStatus(DRAFT);
    }

    private static JournalLine line(AccountMapping mapping, int no, String dc, String code, String name, BigDecimal amount, String memo) {
        JournalLine line = new JournalLine();
        line.setLineNo(no);
        line.setDebitCredit(dc);
        line.setAccountCode(mapping.byCode(code).map(AccountRef::code).orElse(null));
        line.setAccountName(mapping.byCode(code).map(AccountRef::name).orElse(name));
        line.setAmount(amount);
        line.setMemo(memo);
        return line;
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
