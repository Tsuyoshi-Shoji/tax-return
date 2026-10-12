package org.example.features.auth.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.example.features.auth.AuthUserContext;

public class AuthOperationResult {
    private final boolean success;
    private final int statusCode;
    private final String message;
    private final List<String> validationErrors;
    private final AuthUserContext userContext;
    private final boolean duplicateEmail;

    private AuthOperationResult(
            boolean success,
            int statusCode,
            String message,
            List<String> validationErrors,
            AuthUserContext userContext,
            boolean duplicateEmail) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.validationErrors = validationErrors == null ? Collections.emptyList() : validationErrors;
        this.userContext = userContext;
        this.duplicateEmail = duplicateEmail;
    }

    public static AuthOperationResult success(AuthUserContext userContext, String message) {
        return new AuthOperationResult(true, 200, message, Collections.emptyList(), userContext, false);
    }

    public static AuthOperationResult invalid(List<String> errors, String message) {
        return new AuthOperationResult(false, 400, message, new ArrayList<>(errors), null, false);
    }

    public static AuthOperationResult unauthorized(String message) {
        return new AuthOperationResult(false, 401, message, Collections.emptyList(), null, false);
    }

    public static AuthOperationResult duplicateEmail(List<String> errors, String message) {
        return new AuthOperationResult(false, 409, message, new ArrayList<>(errors), null, true);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public AuthUserContext getUserContext() {
        return userContext;
    }

    public boolean isDuplicateEmail() {
        return duplicateEmail;
    }
}
