package org.example.features.income.form;

public class IncomeForm {

    private Long id;
    private String incomeType;
    private String category;
    private String date;
    private String amount;
    private String details;
    private String receiveMethodId;
    private String businessTartget;
    private String returnTo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIncomeType() { return incomeType; }
    public void setIncomeType(String incomeType) { this.incomeType = incomeType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getReceiveMethodId() { return receiveMethodId; }
    public void setReceiveMethodId(String receiveMethodId) { this.receiveMethodId = receiveMethodId; }
    public String getBusinessTartget() { return businessTartget; }
    public void setBusinessTartget(String businessTartget) { this.businessTartget = businessTartget; }
    public String getReturnTo() { return returnTo; }
    public void setReturnTo(String returnTo) { this.returnTo = returnTo; }
}

