package org.example.features.auth;

public class AuthUserContext {
    private final Long userId;
    private final String displayName;
    private final String email;
    
    // MFA（多要素認証）関連フィールド
    private boolean mfaPending = false;  // MFA検証待ちフラグ

    public AuthUserContext(Long userId, String displayName, String email) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isMfaPending() {
        return mfaPending;
    }

    public void setMfaPending(boolean mfaPending) {
        this.mfaPending = mfaPending;
    }
}
