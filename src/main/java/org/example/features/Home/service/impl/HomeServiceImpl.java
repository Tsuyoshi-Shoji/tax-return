package org.example.features.home.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;

import org.example.entity.AccountingTransaction;
import org.example.entity.BusinessProfile;
import org.example.entity.FiscalYear;
import org.example.features.home.dto.FiscalYearOptionDto;
import org.example.features.home.dto.HomeSummaryDto;
import org.example.features.home.dto.HomeViewDto;
import org.example.features.home.dto.RecentTransactionDto;
import org.example.features.home.service.HomeService;
import org.example.repository.AccountingTransactionRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.IncomeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeServiceImpl implements HomeService {
    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final AccountingTransactionRepository accountingTransactionRepository;

    public HomeServiceImpl(
            BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository,
            IncomeEntryRepository incomeEntryRepository,
            ExpenseEntryRepository expenseEntryRepository,
            AccountingTransactionRepository accountingTransactionRepository) {
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.expenseEntryRepository = expenseEntryRepository;
        this.accountingTransactionRepository = accountingTransactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public HomeViewDto getHomeView(Long appUserId, Long selectedFiscalYearId) {
        BusinessProfile profile = businessProfileRepository.findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Business profile not found."));
        List<FiscalYear> fiscalYears = fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId());
        List<FiscalYearOptionDto> options = fiscalYears.stream()
                .map(f -> new FiscalYearOptionDto(f.getId(), f.getTaxYear() + "年"))
                .toList();
        if (fiscalYears.isEmpty()) {
            HomeSummaryDto emptySummary = new HomeSummaryDto(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
            return new HomeViewDto(options, null, emptySummary, Collections.emptyList(), 0, LocalDate.now());
        }

        FiscalYear selected = resolveFiscalYear(fiscalYears, selectedFiscalYearId);
        LocalDate fiscalStart = selected.getPeriodStart();
        LocalDate fiscalEnd = selected.getPeriodEnd();
        LocalDate now = LocalDate.now();

        BigDecimal annualIncome = incomeEntryRepository.sumBusinessAmountInRange(selected.getId(), fiscalStart, fiscalEnd);
        BigDecimal annualExpense = expenseEntryRepository.sumBusinessAmountInRange(selected.getId(), fiscalStart, fiscalEnd);
        BigDecimal annualProfit = annualIncome.subtract(annualExpense);

        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.with(TemporalAdjusters.lastDayOfMonth());
        monthStart = maxDate(monthStart, fiscalStart);
        monthEnd = minDate(monthEnd, fiscalEnd);
        BigDecimal monthlyIncome = incomeEntryRepository.sumBusinessAmountInRange(selected.getId(), monthStart, monthEnd);
        BigDecimal monthlyExpense = expenseEntryRepository.sumBusinessAmountInRange(selected.getId(), monthStart, monthEnd);
        BigDecimal monthlyProfit = monthlyIncome.subtract(monthlyExpense);

        LocalDate prevMonthDate = now.minusMonths(1);
        LocalDate prevStart = prevMonthDate.withDayOfMonth(1);
        LocalDate prevEnd = prevMonthDate.with(TemporalAdjusters.lastDayOfMonth());
        prevStart = maxDate(prevStart, fiscalStart);
        prevEnd = minDate(prevEnd, fiscalEnd);
        BigDecimal prevIncome = incomeEntryRepository.sumBusinessAmountInRange(selected.getId(), prevStart, prevEnd);
        BigDecimal prevExpense = expenseEntryRepository.sumBusinessAmountInRange(selected.getId(), prevStart, prevEnd);
        BigDecimal prevProfit = prevIncome.subtract(prevExpense);

        HomeSummaryDto summary = new HomeSummaryDto(
                annualProfit,
                monthlyProfit.subtract(prevProfit),
                annualProfit,
                annualIncome,
                monthlyIncome,
                annualExpense,
                monthlyExpense);

        List<AccountingTransaction> tx = accountingTransactionRepository
                .findTop5ByFiscalYearIdAndStatusOrderByTransactionDateDescIdDesc(selected.getId(), "finalized");
        if (tx.isEmpty()) {
            tx = accountingTransactionRepository.findTop5ByFiscalYearIdOrderByTransactionDateDescIdDesc(selected.getId());
        }
        List<RecentTransactionDto> recent = tx.stream()
                .map(t -> new RecentTransactionDto(
                        t.getTransactionDate(),
                        "income".equalsIgnoreCase(t.getTransactionType()) ? "収益" : "支出",
                        t.getTransactionType(),
                        t.getSummary(),
                        t.getTotalAmount()))
                .toList();

        LocalDate deadlineDate = calculateDeadline(now);
        long daysUntilDeadline = Math.max(0L, ChronoUnit.DAYS.between(now, deadlineDate));
        return new HomeViewDto(options, selected.getId(), summary, recent, daysUntilDeadline, deadlineDate);
    }

    private FiscalYear resolveFiscalYear(List<FiscalYear> fiscalYears, Long selectedFiscalYearId) {
        if (selectedFiscalYearId == null) {
            return fiscalYears.get(0);
        }
        return fiscalYears.stream()
                .filter(f -> f.getId().equals(selectedFiscalYearId))
                .findFirst()
                .orElseThrow(SecurityException::new);
    }

    private LocalDate calculateDeadline(LocalDate now) {
        LocalDate base = LocalDate.of(now.getYear(), 3, 15);
        if (now.isAfter(base)) {
            return LocalDate.of(now.getYear() + 1, 3, 15);
        }
        return base;
    }

    private LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
