package org.example.features.entry.service;

public class EntrySaveResult {
    private final String redirectPath;
    private final String successMessage;

    public EntrySaveResult(String redirectPath, String successMessage) {
        this.redirectPath = redirectPath;
        this.successMessage = successMessage;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public String getSuccessMessage() {
        return successMessage;
    }
}
