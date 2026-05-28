package org.example.features.auth.dto;

import java.util.List;

public class AuthOperationResult {

    private final boolean success;
    private final String message;
    private final List<String> errors;
    private final Long userId;
    private final String email;
    private final String displayName;

    private AuthOperationResult(boolean success, String message, List<String> errors, Long userId, String email, String displayName) {
        this.success = success;
        this.message = message;
        this.errors = List.copyOf(errors);
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
    }

    public static AuthOperationResult success(String message) {
        return new AuthOperationResult(true, message, List.of(), null, "", "");
    }

    public static AuthOperationResult loginSuccess(Long userId, String email, String displayName) {
        return new AuthOperationResult(true, "ログインしました。", List.of(), userId, email, displayName);
    }

    public static AuthOperationResult failure(List<String> errors) {
        return new AuthOperationResult(false, "", errors, null, "", "");
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

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFirstErrorMessage() {
        return errors.isEmpty() ? "入力内容を確認してください。" : errors.getFirst();
    }
}

