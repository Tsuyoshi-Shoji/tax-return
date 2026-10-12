package org.example.features.auth.service.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.example.entity.AppUser;
import org.example.features.auth.service.MfaService;
import org.example.repository.AppUserRepository;
import org.springframework.stereotype.Service;

@Service
public class MfaServiceImpl implements MfaService {

    private static final String ALGORITHM = "HmacSHA1";
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP = 30; // 秒単位
    private static final int BACKUP_CODES_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    private final AppUserRepository appUserRepository;

    public MfaServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160 bits
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public String getQrCodeUri(String email, String secret) {
        // Google Authenticator 互換の QR コード URI
        // フォーマット: otpauth://totp/label?secret=XXX&issuer=TaxReturn
        String issuer = "TaxReturn";
        String label = String.format("%s (%s)", issuer, email);

        return String.format(
            "otpauth://totp/%s?secret=%s&issuer=%s",
            label,
            secret,
            issuer
        );
    }

    @Override
    public boolean verifyTotp(String secret, String code) {
        try {
            if (secret == null || code == null || code.length() != CODE_DIGITS) {
                return false;
            }

            long currentTime = System.currentTimeMillis() / 1000;
            long currentTimeStep = currentTime / TIME_STEP;

            // 現在時刻 ± 1ステップの範囲で検証（時刻ズレ対応）
            for (long step = currentTimeStep - 1; step <= currentTimeStep + 1; step++) {
                if (step < 0) continue;

                String totp = generateTotp(secret, step);
                if (totp.equals(code)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String generateTotp(String secret, long timeStep) throws Exception {
        byte[] secretBytes = Base64.getDecoder().decode(secret);
        byte[] timeBytes = new byte[8];

        for (int i = 7; i >= 0; i--) {
            timeBytes[i] = (byte) (timeStep & 0xff);
            timeStep >>= 8;
        }

        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(secretBytes, 0, secretBytes.length, ALGORITHM));
        byte[] hash = mac.doFinal(timeBytes);

        int offset = hash[hash.length - 1] & 0xf;
        long truncatedHash = 0;

        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xff);
        }

        truncatedHash &= 0x7fffffff;
        truncatedHash %= Math.pow(10, CODE_DIGITS);

        return String.format("%0" + CODE_DIGITS + "d", truncatedHash);
    }

    @Override
    public String generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();

        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                code.append(String.format("%04d", random.nextInt(10000)));
            }
            codes.add(code.toString());
        }

        return String.join(",", codes);
    }

    @Override
    public boolean verifyAndConsumeBackupCode(AppUser user, String code) {
        if (user.getBackupCodes() == null || code == null) {
            return false;
        }

        String[] codes = user.getBackupCodes().split(",");
        List<String> remainingCodes = new ArrayList<>();

        boolean found = false;
        for (String backupCode : codes) {
            if (!found && backupCode.trim().equals(code.trim())) {
                found = true;
                // このコードは除外（消費）
            } else {
                remainingCodes.add(backupCode);
            }
        }

        if (found) {
            // バックアップコードを更新
            user.setBackupCodes(String.join(",", remainingCodes));
            appUserRepository.save(user);
        }

        return found;
    }

    @Override
    public void enableMfa(AppUser user, String secret, String backupCodes) {
        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        user.setBackupCodes(backupCodes);
        appUserRepository.save(user);
    }

    @Override
    public void disableMfa(AppUser user) {
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setBackupCodes(null);
        appUserRepository.save(user);
    }
}
