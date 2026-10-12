package org.example.features.profitloss.dto;

import java.math.BigDecimal;
import java.util.List;

import org.example.features.home.dto.FiscalYearOptionDto;
import org.example.features.profitloss.form.ProfitLossSearchForm;

public class ProfitLossPageData {
    private final ProfitLossSearchForm form;
    private final List<String> validationErrors;
    private final List<FiscalYearOptionDto> fiscalYearOptions;
    private final Long selectedFiscalYearId;
    private final String fiscalYearLabel;
    private final boolean incomeTypeSelected;
    private final boolean expenseTypeSelected;
    private final List<ProfitLossItemOption> itemOptions;
    private final BigDecimal totalProfit;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal summaryIncome;
    private final BigDecimal summaryExpense;
    private final BigDecimal totalOwnerDraw;
    private final List<ProfitLossListRow> incomeRows;
    private final List<ProfitLossListRow> expenseRows;
    private final long incomeTotalCount;
    private final long expenseTotalCount;
    private final int incomeCurrentPage;
    private final int incomeTotalPages;
    private final int expenseCurrentPage;
    private final int expenseTotalPages;
    private final List<Integer> incomePageNumbers;
    private final List<Integer> expensePageNumbers;

    public ProfitLossPageData(
            ProfitLossSearchForm form,
            List<String> validationErrors,
            List<FiscalYearOptionDto> fiscalYearOptions,
            Long selectedFiscalYearId,
            String fiscalYearLabel,
            boolean incomeTypeSelected,
            boolean expenseTypeSelected,
            List<ProfitLossItemOption> itemOptions,
            BigDecimal totalProfit,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal summaryIncome,
            BigDecimal summaryExpense,
            BigDecimal totalOwnerDraw,
            List<ProfitLossListRow> incomeRows,
            List<ProfitLossListRow> expenseRows,
            long incomeTotalCount,
            long expenseTotalCount,
            int incomeCurrentPage,
            int incomeTotalPages,
            int expenseCurrentPage,
            int expenseTotalPages,
            List<Integer> incomePageNumbers,
            List<Integer> expensePageNumbers) {
        this.form = form;
        this.validationErrors = validationErrors;
        this.fiscalYearOptions = fiscalYearOptions;
        this.selectedFiscalYearId = selectedFiscalYearId;
        this.fiscalYearLabel = fiscalYearLabel;
        this.incomeTypeSelected = incomeTypeSelected;
        this.expenseTypeSelected = expenseTypeSelected;
        this.itemOptions = itemOptions;
        this.totalProfit = totalProfit;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.summaryIncome = summaryIncome;
        this.summaryExpense = summaryExpense;
        this.totalOwnerDraw = totalOwnerDraw;
        this.incomeRows = incomeRows;
        this.expenseRows = expenseRows;
        this.incomeTotalCount = incomeTotalCount;
        this.expenseTotalCount = expenseTotalCount;
        this.incomeCurrentPage = incomeCurrentPage;
        this.incomeTotalPages = incomeTotalPages;
        this.expenseCurrentPage = expenseCurrentPage;
        this.expenseTotalPages = expenseTotalPages;
        this.incomePageNumbers = incomePageNumbers;
        this.expensePageNumbers = expensePageNumbers;
    }

    public ProfitLossSearchForm getForm() {
        return form;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public List<FiscalYearOptionDto> getFiscalYearOptions() {
        return fiscalYearOptions;
    }

    public Long getSelectedFiscalYearId() {
        return selectedFiscalYearId;
    }

    public String getFiscalYearLabel() {
        return fiscalYearLabel;
    }

    public boolean isIncomeTypeSelected() {
        return incomeTypeSelected;
    }

    public boolean isExpenseTypeSelected() {
        return expenseTypeSelected;
    }

    public List<ProfitLossItemOption> getItemOptions() {
        return itemOptions;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getSummaryIncome() {
        return summaryIncome;
    }

    public BigDecimal getTotalOwnerDraw() {
        return totalOwnerDraw;
    }

    public BigDecimal getSummaryExpense() {
        return summaryExpense;
    }

    public List<ProfitLossListRow> getIncomeRows() {
        return incomeRows;
    }

    public List<ProfitLossListRow> getExpenseRows() {
        return expenseRows;
    }

    public long getIncomeTotalCount() {
        return incomeTotalCount;
    }

    public long getExpenseTotalCount() {
        return expenseTotalCount;
    }

    public int getIncomeCurrentPage() {
        return incomeCurrentPage;
    }

    public int getIncomeTotalPages() {
        return incomeTotalPages;
    }

    public int getExpenseCurrentPage() {
        return expenseCurrentPage;
    }

    public int getExpenseTotalPages() {
        return expenseTotalPages;
    }

    public List<Integer> getIncomePageNumbers() {
        return incomePageNumbers;
    }

    public List<Integer> getExpensePageNumbers() {
        return expensePageNumbers;
    }
}
