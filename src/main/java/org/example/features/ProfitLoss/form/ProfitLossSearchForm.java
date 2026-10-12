package org.example.features.profitloss.form;

import java.util.ArrayList;
import java.util.List;

public class ProfitLossSearchForm {
    private String dateFrom;
    private String dateTo;
    private List<String> types = new ArrayList<>();
    private String taxTarget = "show";
    private String item;
    private List<String> items = new ArrayList<>();
    private String amountFrom;
    private String amountTo;
    private Integer incomePage = 1;
    private Integer expensePage = 1;

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types == null ? new ArrayList<>() : new ArrayList<>(types);
    }

    public String getTaxTarget() {
        return taxTarget;
    }

    public void setTaxTarget(String taxTarget) {
        this.taxTarget = taxTarget;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public String getAmountFrom() {
        return amountFrom;
    }

    public void setAmountFrom(String amountFrom) {
        this.amountFrom = amountFrom;
    }

    public String getAmountTo() {
        return amountTo;
    }

    public void setAmountTo(String amountTo) {
        this.amountTo = amountTo;
    }

    public Integer getIncomePage() {
        return incomePage;
    }

    public void setIncomePage(Integer incomePage) {
        this.incomePage = incomePage;
    }

    public Integer getExpensePage() {
        return expensePage;
    }

    public void setExpensePage(Integer expensePage) {
        this.expensePage = expensePage;
    }
}
