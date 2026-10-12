package org.example.features.setting.dto;

import java.util.List;

public class SettingOperationResult {
    private final boolean success;
    private final String message;
    private final List<String> errors;
    private final String updatedDisplayName;

    private SettingOperationResult(boolean success, String message, List<String> errors, String updatedDisplayName) {
        this.success = success;
        this.message = message;
        this.errors = errors;
        this.updatedDisplayName = updatedDisplayName;
    }

    public static SettingOperationResult success(String message) {
        return new SettingOperationResult(true, message, List.of(), null);
    }

    public static SettingOperationResult success(String message, String updatedDisplayName) {
        return new SettingOperationResult(true, message, List.of(), updatedDisplayName);
    }

    public static SettingOperationResult invalid(List<String> errors) {
        return new SettingOperationResult(false, null, errors, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<String> getErrors() { return errors; }
    public String getUpdatedDisplayName() { return updatedDisplayName; }
}