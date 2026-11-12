package org.example.features.taxreturn.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.entity.FiscalYear;
import org.example.entity.FixedAsset;
import org.example.entity.FixedAssetEvent;
import org.example.entity.HouseholdConsumption;
import org.example.entity.JournalEntry;
import org.example.entity.JournalLine;
import org.example.entity.SettlementAdjustment;
import org.example.features.accounting.AccountMapping;
import org.example.features.accounting.AccountMapping.AccountRef;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.accounting.service.AccountMappingService;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.features.taxreturn.asset.DepreciationRules;
import org.example.features.taxreturn.balance.BalanceSheetService;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.repository.FiscalYearRepository;
import org.example.repository.FixedAssetRepository;
import org.example.repository.FixedAssetEventRepository;
import org.example.repository.HouseholdConsumptionRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.SettlementAdjustmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClosingJournalServiceImpl implements ClosingJournalService {
    public static final String SOURCE_SETTLEMENT = "SETTLEMENT_ADJUSTMENT";
    public static final String SOURCE_DEPRECIATION = "DEPRECIATION";
    public static final String SOURCE_ASSET_ACQUISITION = "FIXED_ASSET_ACQUISITION";
    public static final String SOURCE_HOUSEHOLD_CONSUMPTION = "HOUSEHOLD_CONSUMPTION";
    public static final String SOURCE_FIXED_ASSET_EVENT = "FIXED_ASSET_EVENT";
    private static final String CLOSED = "closed";
    private static final String DRAFT = "draft";

    private final FiscalYearRepository fiscalYearRepository;
    private final SettlementAdjustmentRepository settlementRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final FixedAssetRepository fixedAssetRepository;
    private final HouseholdConsumptionRepository householdConsumptionRepository;
    private final FixedAssetEventRepository fixedAssetEventRepository;
    private final AccountMappingService accountMappingService;
    private final BalanceSheetService balanceSheetService;

    public ClosingJournalServiceImpl(
            FiscalYearRepository fiscalYearRepository,
            SettlementAdjustmentRepository settlementRepository,
            JournalEntryRepository journalEntryRepository,
            FixedAssetRepository fixedAssetRepository,
            HouseholdConsumptionRepository householdConsumptionRepository,
            FixedAssetEventRepository fixedAssetEventRepository,
            AccountMappingService accountMappingService,
            BalanceSheetService balanceSheetService) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.settlementRepository = settlementRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.fixedAssetRepository = fixedAssetRepository;
        this.householdConsumptionRepository = householdConsumptionRepository;
        this.fixedAssetEventRepository = fixedAssetEventRepository;
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
        List<FixedAssetEvent> events = fixedAssetEventRepository.findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(businessProfileId);
        for (FiscalYear year : years) {
            if (year.getTaxYear() < fromYear || CLOSED.equals(year.getStatus())) {
                continue;
            }
            rebuildAcquisitions(year, assets, events, mapping);
            rebuildDepreciation(year, assets, events, mapping);
            journalEntryRepository.flush();
            rebuildFixedAssetEvents(year, assets, events, mapping);
            journalEntryRepository.flush();
            rebuildHouseholdConsumption(year, mapping);
            journalEntryRepository.flush();
            rebuildSettlement(year, mapping);
            journalEntryRepository.flush();
        }
    }

    private void rebuildHouseholdConsumption(FiscalYear year, AccountMapping mapping) {
        Set<Long> keep = new HashSet<>();
        for (HouseholdConsumption consumption : householdConsumptionRepository
                .findByFiscalYearIdOrderByTransactionDateAscIdAsc(year.getId())) {
            keep.add(consumption.getId());
            JournalEntry entry = journalEntryRepository
                    .findBySourceTypeAndSourceId(SOURCE_HOUSEHOLD_CONSUMPTION, consumption.getId())
                    .orElseGet(JournalEntry::new);
            fill(entry, year, SOURCE_HOUSEHOLD_CONSUMPTION, consumption.getId(),
                    consumption.getTransactionDate(), "家事消費等：" + consumption.getDetails());
            replaceLines(entry, List.of(
                    line(mapping, 1, "D", AccountMasterSeed.ACC_OWNER_DRAW, "事業主貸",
                            consumption.getAmount(), consumption.getDetails()),
                    line(mapping, 2, "C", AccountMasterSeed.ACC_SALES, "売上高",
                            consumption.getAmount(), consumption.getDetails())));
        }
        for (JournalEntry stale : journalEntryRepository.findByFiscalYearIdAndSourceType(
                year.getId(), SOURCE_HOUSEHOLD_CONSUMPTION)) {
            if (!keep.contains(stale.getSourceId())) {
                journalEntryRepository.delete(stale);
            }
        }
    }

    private void rebuildAcquisitions(FiscalYear year, List<FixedAsset> assets, List<FixedAssetEvent> events,
            AccountMapping mapping) {
        Set<Long> keep = new HashSet<>();
        for (FixedAsset asset : assets) {
            LocalDate recognitionDate = DepreciationRules.recognitionDate(asset, events);
            if (recognitionDate.isBefore(year.getPeriodStart()) || recognitionDate.isAfter(year.getPeriodEnd())) {
                continue;
            }
            FixedAssetEvent transfer = events.stream()
                    .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                    .filter(e -> FixedAssetEvent.INHERITANCE.equals(e.getEventType())
                            || FixedAssetEvent.GIFT.equals(e.getEventType()))
                    .findFirst().orElse(null);
            boolean transferred = transfer != null && recognitionDate.equals(transfer.getEffectiveDate());
            BigDecimal acquisitionValue = transferred ? transfer.getOpeningBookValue()
                    : DepreciationCalculator.initialBookValue(asset);
            List<JournalLine> lines = new ArrayList<>();
            String assetName = mapping.byCode(asset.getAccountCode()).map(AccountRef::name).orElse(asset.getAccountCode());
            if (transferred) {
                lines.add(line(mapping, 1, "D", asset.getAccountCode(), assetName, acquisitionValue, asset.getAssetName()));
                lines.add(line(mapping, 2, "C", AccountMasterSeed.ACC_OWNER_CONTRIBUTION, "事業主借", acquisitionValue,
                        FixedAssetEvent.INHERITANCE.equals(transfer.getEventType()) ? "相続による資産引継ぎ" : "贈与による資産引継ぎ"));
            } else if ("OWNER_FUNDS".equals(asset.getAcquisitionFunding())) {
                lines.add(line(mapping, 1, "D", asset.getAccountCode(), assetName, acquisitionValue, asset.getAssetName()));
                lines.add(line(mapping, 2, "C", AccountMasterSeed.ACC_OWNER_CONTRIBUTION, "事業主借", acquisitionValue,
                        asset.isPrivateUseConversion() ? "非業務用から転用（転用前減価控除後）"
                                : "CAPITAL_IMPROVEMENT".equals(asset.getAssetOrigin()) ? "資本的支出（別資産）" : "私的資金で取得"));
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
            fill(entry, year, SOURCE_ASSET_ACQUISITION, asset.getId(), recognitionDate,
                    (transferred ? "固定資産の相続・贈与引継ぎ："
                            : asset.isPrivateUseConversion() ? "固定資産の転用：" : "固定資産の取得：") + asset.getAssetName());
            replaceLines(entry, lines);
        }
        for (JournalEntry stale : journalEntryRepository.findByFiscalYearIdAndSourceType(year.getId(), SOURCE_ASSET_ACQUISITION)) {
            if (!keep.contains(stale.getSourceId())) {
                journalEntryRepository.delete(stale);
            }
        }
    }

    private void rebuildDepreciation(FiscalYear year, List<FixedAsset> assets, List<FixedAssetEvent> events,
            AccountMapping mapping) {
        BigDecimal business = BigDecimal.ZERO;
        BigDecimal privatePortion = BigDecimal.ZERO;
        Map<String, BigDecimal> credits = new LinkedHashMap<>();
        for (FixedAsset asset : assets) {
            if (DepreciationRules.recognitionDate(asset, events).isAfter(year.getPeriodEnd())) {
                continue;
            }
            DepreciationCalculator.YearResult r = DepreciationCalculator.forYear(
                    asset, year.getTaxYear(), ratioForYear(asset, events, year.getPeriodStart()), events, assets);
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
        replaceLines(entry, lines);
    }

    private void rebuildFixedAssetEvents(FiscalYear year, List<FixedAsset> assets, List<FixedAssetEvent> events,
            AccountMapping mapping) {
        Map<Long, FixedAsset> assetsById = new LinkedHashMap<>();
        assets.forEach(asset -> assetsById.put(asset.getId(), asset));
        Set<Long> keep = new HashSet<>();
        for (FixedAssetEvent event : events) {
            if (!FixedAssetEvent.SALE.equals(event.getEventType()) && !FixedAssetEvent.DISPOSAL.equals(event.getEventType())) {
                continue;
            }
            FixedAsset asset = assetsById.get(event.getFixedAsset().getId());
            if (asset == null || event.getEffectiveDate().isBefore(year.getPeriodStart())
                    || event.getEffectiveDate().isAfter(year.getPeriodEnd())) {
                continue;
            }
            BigDecimal ratio = ratioForYear(asset, events, year.getPeriodStart());
            boolean lump = DepreciationCalculator.LUMP_SUM_3Y.equals(asset.getDepreciationMethod());
            DepreciationCalculator.YearResult result = DepreciationCalculator.forYear(asset, year.getTaxYear(), ratio, events, assets);
            BigDecimal bookValue = lump ? BigDecimal.ZERO
                    : result.closing();
            BigDecimal proceeds = FixedAssetEvent.SALE.equals(event.getEventType()) ? safe(event.getProceeds()) : BigDecimal.ZERO;
            List<JournalLine> lines = new ArrayList<>();
            int no = 1;
            BigDecimal businessProceeds = lump ? proceeds.multiply(ratio)
                    .divide(new BigDecimal("100"), 0, RoundingMode.DOWN) : proceeds;
            BigDecimal difference = businessProceeds.subtract(bookValue);
            if (proceeds.signum() > 0) {
                lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_BANK_DEPOSIT, "普通預金",
                        proceeds, "固定資産売却代金"));
            }
            if (FixedAssetEvent.SALE.equals(event.getEventType())) {
                if (lump && proceeds.compareTo(businessProceeds) > 0) {
                    lines.add(line(mapping, no++, "C", AccountMasterSeed.ACC_OWNER_CONTRIBUTION, "事業主借",
                            proceeds.subtract(businessProceeds), "一括償却資産売却の家事分"));
                }
                if (difference.signum() > 0) {
                    lines.add(line(mapping, no++, "C", lump ? AccountMasterSeed.ACC_MISC_INCOME : AccountMasterSeed.ACC_OWNER_CONTRIBUTION,
                            lump ? "雑収入" : "事業主借", difference,
                            lump ? "一括償却資産の売却収入" : "譲渡所得等は別途確認（事業損益に含めない）"));
                } else if (difference.signum() < 0) {
                    lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_OWNER_DRAW, "事業主貸",
                            difference.abs(), "譲渡所得等は別途確認（事業損益に含めない）"));
                }
            } else if (bookValue.signum() > 0) {
                BigDecimal loss = bookValue.multiply(ratio).divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
                if (loss.signum() > 0) {
                    lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_MISC_EXPENSE, "雑費", loss, "除却損（事業分）"));
                }
                if (bookValue.compareTo(loss) > 0) {
                    lines.add(line(mapping, no++, "D", AccountMasterSeed.ACC_OWNER_DRAW, "事業主貸",
                            bookValue.subtract(loss), "除却損（家事分）"));
                }
            }
            if (bookValue.signum() > 0) {
                String name = mapping.byCode(asset.getAccountCode()).map(AccountRef::name).orElse(asset.getAccountCode());
                lines.add(line(mapping, no, "C", asset.getAccountCode(), name, bookValue, "固定資産の帳簿価額"));
            }
            if (lines.isEmpty()) {
                continue;
            }
            keep.add(event.getId());
            JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceId(SOURCE_FIXED_ASSET_EVENT, event.getId())
                    .orElseGet(JournalEntry::new);
            String summary = FixedAssetEvent.SALE.equals(event.getEventType()) ? "固定資産売却" : "固定資産除却";
            fill(entry, year, SOURCE_FIXED_ASSET_EVENT, event.getId(), event.getEffectiveDate(), summary + "：" + asset.getAssetName());
            replaceLines(entry, lines);
        }
        for (JournalEntry stale : journalEntryRepository.findByFiscalYearIdAndSourceType(year.getId(), SOURCE_FIXED_ASSET_EVENT)) {
            if (!keep.contains(stale.getSourceId())) {
                journalEntryRepository.delete(stale);
            }
        }
    }

    public static BigDecimal ratioForYear(FixedAsset asset, List<FixedAssetEvent> events, java.time.LocalDate periodStart) {
        return events.stream()
                .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                .filter(e -> FixedAssetEvent.BUSINESS_RATIO_CHANGE.equals(e.getEventType()))
                .filter(e -> !e.getEffectiveDate().isAfter(periodStart))
                .max(Comparator.comparing(FixedAssetEvent::getEffectiveDate))
                .map(FixedAssetEvent::getBusinessRatio)
                .orElse(asset.getBusinessRatio());
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
        replaceLines(entry, lines);
    }

    /**
     * 既存の仕訳明細を新しい内容へ入れ替える。先に明細をクリアしてflushすることで、
     * line_no を再利用する新しい行の挿入が、orphanRemovalによる旧行の削除より先に
     * 実行されて一意制約（journal_entry_id, line_no）に違反するのを防ぐ。
     */
    private void replaceLines(JournalEntry entry, List<JournalLine> lines) {
        if (entry.getId() != null) {
            entry.setLines(null);
            journalEntryRepository.saveAndFlush(entry);
        }
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
