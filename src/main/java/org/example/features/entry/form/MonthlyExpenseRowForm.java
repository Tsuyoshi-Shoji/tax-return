package org.example.features.entry.form;

public class MonthlyExpenseRowForm {
    private String expenseType;
    private String category;
    private String date;
    private String amount;
    private String paymentMethodId;
    private boolean excludeFromReturn;
    private boolean homeApportionment;
    private String details;

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public boolean isExcludeFromReturn() {
        return excludeFromReturn;
    }

    public void setExcludeFromReturn(boolean excludeFromReturn) {
        this.excludeFromReturn = excludeFromReturn;
    }

    public boolean isHomeApportionment() {
        return homeApportionment;
    }

    public void setHomeApportionment(boolean homeApportionment) {
        this.homeApportionment = homeApportionment;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

