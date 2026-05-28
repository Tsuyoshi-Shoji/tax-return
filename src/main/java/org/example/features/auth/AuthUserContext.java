package org.example.features.auth;

public final class AuthUserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private AuthUserContext() {
    }

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}

