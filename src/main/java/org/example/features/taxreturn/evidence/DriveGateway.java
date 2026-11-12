package org.example.features.taxreturn.evidence;

/**
 * 証憑ファイル保管先（Google Drive）への操作。テストでは偽実装に差し替える。
 */
public interface DriveGateway {
    String authorizationUrl(String state, String codeChallenge);

    TokenGrant exchangeCode(String code, String codeVerifier);

    String accessToken(String refreshToken);

    String createFolder(String accessToken, String name);

    StoredFile upload(String accessToken, String folderId, String name, String mimeType, byte[] content);

    RemoteStatus status(String accessToken, String fileId);

    byte[] download(String accessToken, String fileId, long maxBytes);

    void delete(String accessToken, String fileId);

    void revoke(String token);

    record TokenGrant(String refreshToken, String accessToken, String scope) {
        @Override
        public String toString() {
            return "TokenGrant[scope=" + scope + "]";
        }
    }

    record StoredFile(String id, long size) {
    }

    enum RemoteState {
        PRESENT, TRASHED, MISSING
    }

    record RemoteStatus(RemoteState state, long size) {
    }

    /** 外部サービスの失敗。メッセージに応答本文やトークンを含めない。 */
    class DriveException extends RuntimeException {
        private final boolean authorizationRevoked;

        public DriveException(String message, boolean authorizationRevoked) {
            super(message);
            this.authorizationRevoked = authorizationRevoked;
        }

        public boolean isAuthorizationRevoked() {
            return authorizationRevoked;
        }
    }
}
