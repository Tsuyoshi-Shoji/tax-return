package org.example.features.entry.form;

import java.util.ArrayList;
import java.util.List;

public class EntryForm {
    private Long editId;
    private Long version;
    private String incomeType;
    private String expenseType;
    private String category;
    private String date;
    private String amount;
    private String receiveMethodId;
    private String paymentMethodId;
    private boolean excludeFromReturn;
    private boolean homeApportionment;
    private String details;
    private String entryMode;
    private String targetMonth;
    private List<MonthlyExpenseRowForm> monthlyRows = new ArrayList<>();
    private String csrfToken;
    private String submissionToken;
    private String confirmationToken;
    private String returnQuery;

    public Long getEditId() {
        return editId;
    }

    public void setEditId(Long editId) {
        this.editId = editId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getIncomeType() {
        return incomeType;
    }

    public void setIncomeType(String incomeType) {
        this.incomeType = incomeType;
    }

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

    public String getReceiveMethodId() {
        return receiveMethodId;
    }

    public void setReceiveMethodId(String receiveMethodId) {
        this.receiveMethodId = receiveMethodId;
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

    public String getEntryMode() {
        return entryMode;
    }

    public void setEntryMode(String entryMode) {
        this.entryMode = entryMode;
    }

    public String getTargetMonth() {
        return targetMonth;
    }

    public void setTargetMonth(String targetMonth) {
        this.targetMonth = targetMonth;
    }

    public List<MonthlyExpenseRowForm> getMonthlyRows() {
        return monthlyRows;
    }

    public void setMonthlyRows(List<MonthlyExpenseRowForm> monthlyRows) {
        this.monthlyRows = monthlyRows == null ? new ArrayList<>() : monthlyRows;
    }

    public boolean isMonthlyMode() {
        return "monthly".equalsIgnoreCase(entryMode);
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public String getSubmissionToken() {
        return submissionToken;
    }

    public void setSubmissionToken(String submissionToken) {
        this.submissionToken = submissionToken;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public String getReturnQuery() {
        return returnQuery;
    }

    public void setReturnQuery(String returnQuery) {
        this.returnQuery = returnQuery;
    }
}
