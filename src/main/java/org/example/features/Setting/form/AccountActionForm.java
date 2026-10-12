package org.example.features.setting.form;

public class AccountActionForm {
    private String confirmText;
    private String currentPassword;
    private String csrfToken;

    public String getConfirmText() { return confirmText; }
    public void setConfirmText(String confirmText) { this.confirmText = confirmText; }
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getCsrfToken() { return csrfToken; }
    public void setCsrfToken(String csrfToken) { this.csrfToken = csrfToken; }
}