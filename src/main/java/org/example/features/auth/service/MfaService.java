package org.example.features.auth.service;

import org.example.entity.AppUser;

public interface MfaService {
    /**
     * MFA秘密鍵を生成する
     */
    String generateSecret();

    /**
     * QRコード用のプロビジョニングURI を生成する
     */
    String getQrCodeUri(String email, String secret);

    /**
     * OTPコードを検証する
     */
    boolean verifyTotp(String secret, String code);

    /**
     * バックアップコードを生成する（カンマ区切り）
     */
    String generateBackupCodes();

    /**
     * バックアップコードを検証・消費する
     */
    boolean verifyAndConsumeBackupCode(AppUser user, String code);

    /**
     * ユーザーのMFAを有効化する
     */
    void enableMfa(AppUser user, String secret, String backupCodes);

    /**
     * ユーザーのMFAを無効化する
     */
    void disableMfa(AppUser user);
}
