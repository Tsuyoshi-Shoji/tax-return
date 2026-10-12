package org.example.features.taxreturn.form;

public class SettlementForm {
    private String openingInventory = "0";
    private String closingInventory = "0";
    private String depreciation = "0";
    private String accountsReceivable = "0";
    private String accountsPayable = "0";
    private String unpaidExpenses = "0";
    private String unreceivedIncome = "0";
    private String openingBalance = "0";
    private String closingCash = "0";
    private String csrfToken;

    public String getOpeningInventory() { return openingInventory; }
    public void setOpeningInventory(String v) { this.openingInventory = v; }
    public String getClosingInventory() { return closingInventory; }
    public void setClosingInventory(String v) { this.closingInventory = v; }
    public String getDepreciation() { return depreciation; }
    public void setDepreciation(String v) { this.depreciation = v; }
    public String getAccountsReceivable() { return accountsReceivable; }
    public void setAccountsReceivable(String v) { this.accountsReceivable = v; }
    public String getAccountsPayable() { return accountsPayable; }
    public void setAccountsPayable(String v) { this.accountsPayable = v; }
    public String getUnpaidExpenses() { return unpaidExpenses; }
    public void setUnpaidExpenses(String v) { this.unpaidExpenses = v; }
    public String getUnreceivedIncome() { return unreceivedIncome; }
    public void setUnreceivedIncome(String v) { this.unreceivedIncome = v; }
    public String getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(String v) { this.openingBalance = v; }
    public String getClosingCash() { return closingCash; }
    public void setClosingCash(String v) { this.closingCash = v; }
    public String getCsrfToken() { return csrfToken; }
    public void setCsrfToken(String csrfToken) { this.csrfToken = csrfToken; }
}
