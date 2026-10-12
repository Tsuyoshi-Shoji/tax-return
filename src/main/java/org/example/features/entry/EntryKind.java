package org.example.features.entry;

import org.example.features.entry.form.EntryForm;

public enum EntryKind {
    INCOME("income", "/income", "Income/income", "income"),
    EXPENSE("expense", "/expense", "Expense/expense", "expense");

    private final String code;
    private final String path;
    private final String viewName;
    private final String activeMenu;

    EntryKind(String code, String path, String viewName, String activeMenu) {
        this.code = code;
        this.path = path;
        this.viewName = viewName;
        this.activeMenu = activeMenu;
    }

    public String getCode() {
        return code;
    }

    public String getPath() {
        return path;
    }

    public String getViewName() {
        return viewName;
    }

    public String getActiveMenu() {
        return activeMenu;
    }

    public boolean isIncome() {
        return this == INCOME;
    }

    public boolean isExpense() {
        return this == EXPENSE;
    }

    public String readType(EntryForm form) {
        return isIncome() ? form.getIncomeType() : form.getExpenseType();
    }

    public void writeType(EntryForm form, String value) {
        if (isIncome()) {
            form.setIncomeType(value);
            form.setExpenseType("");
            return;
        }
        form.setExpenseType(value);
        form.setIncomeType("");
    }

    public String readMethodId(EntryForm form) {
        return isIncome() ? form.getReceiveMethodId() : form.getPaymentMethodId();
    }

    public void writeMethodId(EntryForm form, String value) {
        if (isIncome()) {
            form.setReceiveMethodId(value);
            form.setPaymentMethodId("");
            return;
        }
        form.setPaymentMethodId(value);
        form.setReceiveMethodId("");
    }
}
