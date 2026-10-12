package org.example.features.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.example.entity.BusinessProfile;
import org.example.entity.ExpenseEntry;
import org.example.entity.FiscalYear;
import org.example.entity.IncomeEntry;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.report.dto.ReportCalendarDay;
import org.example.features.report.dto.ReportDetailPageData;
import org.example.features.report.dto.ReportItemRow;
import org.example.features.report.dto.ReportMonthOption;
import org.example.features.report.dto.ReportMonthRow;
import org.example.features.report.dto.ReportPageData;
import org.example.features.report.dto.ReportSection;
import org.example.features.report.dto.ReportYearOption;
import org.example.features.report.service.ReportService;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.IncomeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {
    private static final List<String> INCOME_COLORS = List.of("#0a9b5c", "#37b97f", "#6fcf9f", "#9bdcb9", "#b8c9e0", "#cfd8e6");
    private static final List<String> EXPENSE_COLORS = List.of("#ff4d5d", "#ff8a9a", "#ffb24d", "#ffcf7a", "#f08cb0", "#a891c9");
    private static final String EMPTY_COLOR = "#e5ebf5";
    private static final int MAX_PIE_ITEMS = 5;
    private static final String OTHER_LABEL = "その他";

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;

    public ReportServiceImpl(
            BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository,
            IncomeEntryRepository incomeEntryRepository,
            ExpenseEntryRepository expenseEntryRepository) {
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.expenseEntryRepository = expenseEntryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportPageData loadReport(Long appUserId, Integer year) {
        List<FiscalYear> fiscalYears = loadFiscalYears(appUserId);
        FiscalYear selected = resolve(fiscalYears, year);
        Entries entries = loadEntries(selected, appUserId);
        return new ReportPageData(
                yearOptions(fiscalYears),
                selected.getTaxYear(),
                yearLabel(selected.getTaxYear()),
                buildSection(entries.incomes, entries.expenses, false, true),
                buildSection(entries.incomes, entries.expenses, true, true));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailPageData loadDetail(Long appUserId, Integer year, Integer month) {
        List<FiscalYear> fiscalYears = loadFiscalYears(appUserId);
        FiscalYear selected = resolve(fiscalYears, year);
        int taxYear = selected.getTaxYear();
        int targetMonth;
        if (month == null) {
            LocalDate today = LocalDate.now();
            targetMonth = today.getYear() == taxYear ? today.getMonthValue() : 1;
        } else if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month");
        } else {
            targetMonth = month;
        }
        YearMonth target = YearMonth.of(taxYear, targetMonth);
        Entries entries = loadEntries(selected, appUserId);
        List<IncomeEntry> incomes = entries.incomes.stream().filter(e -> inMonth(e.getTransactionDate(), target)).toList();
        List<ExpenseEntry> expenses = entries.expenses.stream().filter(e -> inMonth(e.getTransactionDate(), target)).toList();

        Set<Integer> knownYears = new TreeSet<>();
        fiscalYears.forEach(f -> knownYears.add(f.getTaxYear()));
        YearMonth prev = target.minusMonths(1);
        YearMonth next = target.plusMonths(1);

        List<ReportMonthOption> monthOptions = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            monthOptions.add(new ReportMonthOption(m, String.format("%d年%02d月（%s%d月）", taxYear, m, eraPrefix(taxYear), m)));
        }
        return new ReportDetailPageData(
                taxYear,
                targetMonth,
                String.format("%d年%d月", taxYear, targetMonth),
                monthOptions,
                knownYears.contains(prev.getYear()) ? new int[] {prev.getYear(), prev.getMonthValue()} : null,
                knownYears.contains(next.getYear()) ? new int[] {next.getYear(), next.getMonthValue()} : null,
                buildSection(incomes, expenses, false, false),
                buildSection(incomes, expenses, true, false),
                buildCalendar(target, incomes, expenses));
    }

    private List<FiscalYear> loadFiscalYears(Long appUserId) {
        BusinessProfile profile = businessProfileRepository.findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Business profile not found."));
        List<FiscalYear> fiscalYears = fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId());
        if (fiscalYears.isEmpty()) {
            throw new EntryNotFoundException("Fiscal year not found");
        }
        return fiscalYears;
    }

    private FiscalYear resolve(List<FiscalYear> fiscalYears, Integer year) {
        if (year == null) {
            return fiscalYears.get(0);
        }
        return fiscalYears.stream()
                .filter(f -> Integer.valueOf(f.getTaxYear()).equals(year))
                .findFirst()
                .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
    }

    private Entries loadEntries(FiscalYear fiscalYear, Long appUserId) {
        Entries entries = new Entries();
        entries.incomes = incomeEntryRepository.findAllOwnedByFiscalYear(fiscalYear.getId(), appUserId);
        entries.expenses = expenseEntryRepository.findAllOwnedByFiscalYear(fiscalYear.getId(), appUserId);
        return entries;
    }

    private List<ReportYearOption> yearOptions(List<FiscalYear> fiscalYears) {
        return fiscalYears.stream()
                .map(f -> new ReportYearOption(f.getTaxYear(), yearLabel(f.getTaxYear())))
                .toList();
    }

    private String yearLabel(int year) {
        return year >= 2019 ? year + "年（令和" + (year - 2018) + "年）" : year + "年";
    }

    private String eraPrefix(int year) {
        return year >= 2019 ? "令和" + (year - 2018) + "年" : "";
    }

    private boolean inMonth(LocalDate date, YearMonth month) {
        return date != null && YearMonth.from(date).equals(month);
    }

    private ReportSection buildSection(List<IncomeEntry> incomes, List<ExpenseEntry> expenses, boolean businessOnly, boolean monthly) {
        List<IncomeEntry> targetIncomes = incomes.stream()
                .filter(e -> e.getAmount() != null && e.getTransactionDate() != null)
                .filter(e -> !businessOnly || !e.isExcludeFromReturn())
                .toList();
        List<ExpenseEntry> targetExpenses = expenses.stream()
                .filter(e -> e.getAmount() != null && e.getTransactionDate() != null)
                .filter(e -> !businessOnly || !e.isExcludeFromReturn())
                .toList();
        Function<ExpenseEntry, BigDecimal> expenseAmount = e -> businessOnly && e.getDeductibleAmount() != null
                ? e.getDeductibleAmount()
                : e.getAmount();

        BigDecimal[] incomeByMonth = zeros();
        BigDecimal[] expenseByMonth = zeros();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> incomeItems = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseItems = new LinkedHashMap<>();
        for (IncomeEntry e : targetIncomes) {
            int idx = e.getTransactionDate().getMonthValue() - 1;
            incomeByMonth[idx] = incomeByMonth[idx].add(e.getAmount());
            totalIncome = totalIncome.add(e.getAmount());
            incomeItems.merge(e.getIncomeItem() == null ? "未分類" : e.getIncomeItem().getItemName(), e.getAmount(), BigDecimal::add);
        }
        for (ExpenseEntry e : targetExpenses) {
            BigDecimal amount = expenseAmount.apply(e);
            int idx = e.getTransactionDate().getMonthValue() - 1;
            expenseByMonth[idx] = expenseByMonth[idx].add(amount);
            totalExpense = totalExpense.add(amount);
            expenseItems.merge(e.getExpenseItem() == null ? "未分類" : e.getExpenseItem().getItemName(), amount, BigDecimal::add);
        }

        List<ReportMonthRow> months = new ArrayList<>();
        List<BigDecimal> ticks = new ArrayList<>();
        if (monthly) {
            BigDecimal max = BigDecimal.ZERO;
            for (int i = 0; i < 12; i++) {
                max = max.max(incomeByMonth[i]).max(expenseByMonth[i]);
            }
            BigDecimal step = niceStep(max);
            BigDecimal axisMax = step.multiply(BigDecimal.valueOf(4));
            for (int i = 0; i <= 4; i++) {
                ticks.add(step.multiply(BigDecimal.valueOf(i)));
            }
            for (int i = 0; i < 12; i++) {
                months.add(new ReportMonthRow(i + 1, incomeByMonth[i], expenseByMonth[i],
                        barPercent(incomeByMonth[i], axisMax), barPercent(expenseByMonth[i], axisMax)));
            }
        }
        List<ReportItemRow> incomeRows = itemRows(incomeItems, totalIncome, INCOME_COLORS);
        List<ReportItemRow> expenseRows = itemRows(expenseItems, totalExpense, EXPENSE_COLORS);
        return new ReportSection(totalIncome, totalExpense, months, ticks, incomeRows, expenseRows,
                gradient(incomeRows), gradient(expenseRows));
    }

    private BigDecimal[] zeros() {
        BigDecimal[] values = new BigDecimal[12];
        java.util.Arrays.fill(values, BigDecimal.ZERO);
        return values;
    }

    private BigDecimal niceStep(BigDecimal max) {
        if (max.signum() <= 0) {
            return BigDecimal.valueOf(25000);
        }
        BigDecimal raw = max.divide(BigDecimal.valueOf(4), 2, RoundingMode.UP);
        BigDecimal magnitude = BigDecimal.ONE;
        while (magnitude.multiply(BigDecimal.TEN).compareTo(raw) <= 0) {
            magnitude = magnitude.multiply(BigDecimal.TEN);
        }
        for (String factor : new String[] {"1", "2", "2.5", "5", "10"}) {
            BigDecimal candidate = magnitude.multiply(new BigDecimal(factor));
            if (candidate.compareTo(raw) >= 0) {
                return candidate;
            }
        }
        return magnitude.multiply(BigDecimal.TEN);
    }

    private int barPercent(BigDecimal value, BigDecimal axisMax) {
        if (axisMax.signum() <= 0 || value.signum() <= 0) {
            return 0;
        }
        return Math.max(1, value.multiply(BigDecimal.valueOf(100)).divide(axisMax, 0, RoundingMode.HALF_UP).intValue());
    }

    private List<ReportItemRow> itemRows(Map<String, BigDecimal> items, BigDecimal total, List<String> colors) {
        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(items.entrySet());
        sorted.sort(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()));
        List<ReportItemRow> rows = new ArrayList<>();
        BigDecimal other = BigDecimal.ZERO;
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, BigDecimal> entry = sorted.get(i);
            if (i < MAX_PIE_ITEMS || sorted.size() == MAX_PIE_ITEMS + 1 && i == MAX_PIE_ITEMS) {
                rows.add(new ReportItemRow(entry.getKey(), entry.getValue(), percent(entry.getValue(), total), colors.get(i)));
            } else {
                other = other.add(entry.getValue());
            }
        }
        if (other.signum() != 0) {
            rows.add(new ReportItemRow(OTHER_LABEL, other, percent(other, total), colors.get(colors.size() - 1)));
        }
        return rows;
    }

    private BigDecimal percent(BigDecimal value, BigDecimal total) {
        if (total.signum() <= 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }

    private String gradient(List<ReportItemRow> rows) {
        BigDecimal total = rows.stream().map(ReportItemRow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (rows.isEmpty() || total.signum() <= 0) {
            return "conic-gradient(" + EMPTY_COLOR + " 0% 100%)";
        }
        StringBuilder sb = new StringBuilder("conic-gradient(");
        BigDecimal cumulative = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            ReportItemRow row = rows.get(i);
            BigDecimal start = cumulative.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
            cumulative = cumulative.add(row.getAmount());
            BigDecimal end = i == rows.size() - 1
                    ? BigDecimal.valueOf(100)
                    : cumulative.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(row.getColor()).append(' ')
                    .append(String.format(Locale.ROOT, "%s%% %s%%", start.toPlainString(), end.toPlainString()));
        }
        return sb.append(')').toString();
    }

    private List<List<ReportCalendarDay>> buildCalendar(YearMonth target, List<IncomeEntry> incomes, List<ExpenseEntry> expenses) {
        Set<Integer> incomeDays = new TreeSet<>();
        Set<Integer> expenseDays = new TreeSet<>();
        Set<Integer> excludedDays = new TreeSet<>();
        for (IncomeEntry e : incomes) {
            if (e.getTransactionDate() == null) {
                continue;
            }
            if (e.isExcludeFromReturn()) {
                excludedDays.add(e.getTransactionDate().getDayOfMonth());
            } else {
                incomeDays.add(e.getTransactionDate().getDayOfMonth());
            }
        }
        for (ExpenseEntry e : expenses) {
            if (e.getTransactionDate() == null) {
                continue;
            }
            if (e.isExcludeFromReturn()) {
                excludedDays.add(e.getTransactionDate().getDayOfMonth());
            } else {
                expenseDays.add(e.getTransactionDate().getDayOfMonth());
            }
        }
        LocalDate first = target.atDay(1);
        LocalDate cursor = first.minusDays(first.getDayOfWeek().getValue() % 7);
        LocalDate last = target.atEndOfMonth();
        List<List<ReportCalendarDay>> weeks = new ArrayList<>();
        while (!cursor.isAfter(last) || cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
            List<ReportCalendarDay> week = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                boolean inMonth = YearMonth.from(cursor).equals(target);
                int day = cursor.getDayOfMonth();
                week.add(new ReportCalendarDay(day, inMonth,
                        inMonth && incomeDays.contains(day),
                        inMonth && expenseDays.contains(day),
                        inMonth && excludedDays.contains(day)));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
        return weeks;
    }

    private static final class Entries {
        private List<IncomeEntry> incomes;
        private List<ExpenseEntry> expenses;
    }
}