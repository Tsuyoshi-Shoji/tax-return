package org.example.features.setting.dto;

import java.util.List;

public class SettingOperationResult {

    private final boolean success;
    private final String message;
    private final List<String> errors;

    private SettingOperationResult(boolean success, String message, List<String> errors) {
        this.success = success;
        this.message = message;
        this.errors = List.copyOf(errors);
    }

    public static SettingOperationResult success(String message) {
        return new SettingOperationResult(true, message, List.of());
    }

    public static SettingOperationResult failure(List<String> errors) {
        return new SettingOperationResult(false, "", errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getFirstErrorMessage() {
        return errors.isEmpty() ? "入力内容を確認してください。" : errors.getFirst();
    }
}

