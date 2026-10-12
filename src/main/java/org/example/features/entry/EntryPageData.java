package org.example.features.entry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.features.entry.form.EntryForm;

public class EntryPageData {
    private String entryKind;
    private EntryForm entryForm;
    private List<EntryOption> categories = new ArrayList<>();
    private List<EntryOption> items = new ArrayList<>();
    private List<EntryOption> methods = new ArrayList<>();
    private BigDecimal apportionmentRate;
    private String today;
    private Map<String, String> fieldErrors = new LinkedHashMap<>();
    private String failureMessage;
    private String successMessage;
    private boolean confirmationRequired;
    private String activeMenu;
    private String currentUserDisplayName;
    private String currentUserEmail;
    private String fiscalYearLabel;

    public String getEntryKind() {
        return entryKind;
    }

    public void setEntryKind(String entryKind) {
        this.entryKind = entryKind;
    }

    public EntryForm getEntryForm() {
        return entryForm;
    }

    public void setEntryForm(EntryForm entryForm) {
        this.entryForm = entryForm;
    }

    public List<EntryOption> getCategories() {
        return categories;
    }

    public void setCategories(List<EntryOption> categories) {
        this.categories = categories;
    }

    public List<EntryOption> getItems() {
        return items;
    }

    public void setItems(List<EntryOption> items) {
        this.items = items;
    }

    public List<EntryOption> getMethods() {
        return methods;
    }

    public void setMethods(List<EntryOption> methods) {
        this.methods = methods;
    }

    public BigDecimal getApportionmentRate() {
        return apportionmentRate;
    }

    public void setApportionmentRate(BigDecimal apportionmentRate) {
        this.apportionmentRate = apportionmentRate;
    }

    public String getToday() {
        return today;
    }

    public void setToday(String today) {
        this.today = today;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public String getActiveMenu() {
        return activeMenu;
    }

    public void setActiveMenu(String activeMenu) {
        this.activeMenu = activeMenu;
    }

    public String getCurrentUserDisplayName() {
        return currentUserDisplayName;
    }

    public void setCurrentUserDisplayName(String currentUserDisplayName) {
        this.currentUserDisplayName = currentUserDisplayName;
    }

    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public void setCurrentUserEmail(String currentUserEmail) {
        this.currentUserEmail = currentUserEmail;
    }

    public String getFiscalYearLabel() {
        return fiscalYearLabel;
    }

    public void setFiscalYearLabel(String fiscalYearLabel) {
        this.fiscalYearLabel = fiscalYearLabel;
    }
}
