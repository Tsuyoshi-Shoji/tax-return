package org.example.features.taxreturn.evidence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Google Drive 連携の設定。秘密情報は環境変数からのみ受け取り、未設定時は機能を無効化する。
 * 証憑ファイルの保管機能は将来提供予定のため、{@code EVIDENCE_UPLOAD_ENABLED=true} を設定するまで停止する。
 */
@Component
public class GoogleDriveSettings {
    public static final String DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenKey;
    private final boolean enabled;

    @Autowired
    public GoogleDriveSettings(
            @Value("${EVIDENCE_UPLOAD_ENABLED:false}") String enabled,
            @Value("${GOOGLE_OAUTH_CLIENT_ID:}") String clientId,
            @Value("${GOOGLE_OAUTH_CLIENT_SECRET:}") String clientSecret,
            @Value("${GOOGLE_OAUTH_REDIRECT_URI:}") String redirectUri,
            @Value("${EVIDENCE_TOKEN_KEY:}") String tokenKey) {
        this.enabled = "true".equalsIgnoreCase(trim(enabled));
        this.clientId = trim(clientId);
        this.clientSecret = trim(clientSecret);
        this.redirectUri = trim(redirectUri);
        this.tokenKey = trim(tokenKey);
    }

    /** 機能を有効化した設定（検証用）。 */
    public GoogleDriveSettings(String clientId, String clientSecret, String redirectUri, String tokenKey) {
        this("true", clientId, clientSecret, redirectUri, tokenKey);
    }

    /** 証憑ファイルの保管機能を提供するか。停止中も保存管理台帳は利用できる。 */
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return !clientId.isEmpty() && !clientSecret.isEmpty() && !tokenKey.isEmpty()
                && (redirectUri.startsWith("https://") || redirectUri.startsWith("http://localhost"));
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getTokenKey() {
        return tokenKey;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "GoogleDriveSettings[enabled=" + enabled + ", configured=" + isConfigured() + "]";
    }
}
