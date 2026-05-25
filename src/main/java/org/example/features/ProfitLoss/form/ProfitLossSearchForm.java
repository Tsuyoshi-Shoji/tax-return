package org.example.features.profitloss.form;

import java.util.ArrayList;
import java.util.List;

public class ProfitLossSearchForm {

    private String dateFrom;
    private String dateTo;
    private List<String> types = new ArrayList<>();
    private String items;
    private String amountFrom;
    private String amountTo;
    private boolean showTaxExcluded = true;
    private Integer incomePage = 1;
    private Integer expensePage = 1;

    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types == null ? new ArrayList<>() : types; }
    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }
    public String getAmountFrom() { return amountFrom; }
    public void setAmountFrom(String amountFrom) { this.amountFrom = amountFrom; }
    public String getAmountTo() { return amountTo; }
    public void setAmountTo(String amountTo) { this.amountTo = amountTo; }
    public boolean isShowTaxExcluded() { return showTaxExcluded; }
    public void setShowTaxExcluded(boolean showTaxExcluded) { this.showTaxExcluded = showTaxExcluded; }
    public Integer getIncomePage() { return incomePage == null || incomePage < 1 ? 1 : incomePage; }
    public void setIncomePage(Integer incomePage) { this.incomePage = incomePage; }
    public Integer getExpensePage() { return expensePage == null || expensePage < 1 ? 1 : expensePage; }
    public void setExpensePage(Integer expensePage) { this.expensePage = expensePage; }
}

