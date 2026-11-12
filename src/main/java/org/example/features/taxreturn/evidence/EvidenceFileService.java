package org.example.features.taxreturn.evidence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.example.entity.EvidenceDriveConnection;
import org.example.entity.EvidenceFile;
import org.example.entity.RetentionRecord;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.evidence.DriveGateway.DriveException;
import org.example.features.taxreturn.evidence.DriveGateway.RemoteState;
import org.example.features.taxreturn.evidence.DriveGateway.RemoteStatus;
import org.example.features.taxreturn.evidence.DriveGateway.StoredFile;
import org.example.features.taxreturn.evidence.DriveGateway.TokenGrant;
import org.example.features.taxreturn.retention.RetentionRules;
import org.example.repository.AppUserRepository;
import org.example.repository.EvidenceDriveConnectionRepository;
import org.example.repository.EvidenceFileRepository;
import org.example.repository.RetentionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 証憑ファイルを利用者自身の Google Drive（drive.file スコープ）へ保管し、台帳から検索・取得・削除する。
 * 所有者確認は認証済み利用者IDと台帳の年度所有者で行い、削除は保存期限経過後に限る。
 * 外部通信はDBトランザクションの外で行い、DB保存失敗時はアップロード済みファイルを補償削除する。
 */
@Service
public class EvidenceFileService {
    public static final String PROVIDER = "GOOGLE_DRIVE";
    private static final Logger AUDIT = LoggerFactory.getLogger("audit.evidence");
    private static final String FOLDER_NAME = "確定申告アプリ 証憑";
    static final String DISABLED_MESSAGE = "証憑ファイルの保管機能は現在停止しています（今後提供予定）。保存管理台帳の登録・検索・期限管理は利用できます。";

    private final GoogleDriveSettings settings;
    private final DriveGateway gateway;
    private final EvidenceDriveConnectionRepository connectionRepository;
    private final EvidenceFileRepository fileRepository;
    private final RetentionRecordRepository recordRepository;
    private final AppUserRepository appUserRepository;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    public EvidenceFileService(GoogleDriveSettings settings, DriveGateway gateway,
            EvidenceDriveConnectionRepository connectionRepository, EvidenceFileRepository fileRepository,
            RetentionRecordRepository recordRepository, AppUserRepository appUserRepository,
            PlatformTransactionManager transactionManager) {
        this.settings = settings;
        this.gateway = gateway;
        this.connectionRepository = connectionRepository;
        this.fileRepository = fileRepository;
        this.recordRepository = recordRepository;
        this.appUserRepository = appUserRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public EvidenceDriveStatus status(Long appUserId) {
        if (!settings.isEnabled()) {
            return EvidenceDriveStatus.disabled();
        }
        if (!settings.isConfigured()) {
            return new EvidenceDriveStatus(true, false, false, null);
        }
        return connectionRepository.findByAppUserId(appUserId)
                .map(c -> new EvidenceDriveStatus(true, true, true, c.getConnectedAt()))
                .orElseGet(() -> new EvidenceDriveStatus(true, true, false, null));
    }

    /** OAuth 認可要求を開始する。state と code_verifier は呼出し元がセッションへ保存する。 */
    public AuthorizationStart beginAuthorization() {
        requireConfigured();
        String state = randomToken(32);
        String verifier = randomToken(64);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
        return new AuthorizationStart(state, verifier, gateway.authorizationUrl(state, challenge));
    }

    public List<String> completeAuthorization(Long appUserId, String code, String codeVerifier) {
        requireConfigured();
        if (code == null || code.isBlank() || code.length() > 2048) {
            return List.of("Google から認可コードを受け取れませんでした。");
        }
        try {
            TokenGrant grant = gateway.exchangeCode(code, codeVerifier);
            if (grant.refreshToken() == null || grant.accessToken() == null) {
                return List.of("Google から長期利用の許可を取得できませんでした。再度連携してください。");
            }
            if (grant.scope() == null || !List.of(grant.scope().split(" ")).contains(GoogleDriveSettings.DRIVE_FILE_SCOPE)) {
                gateway.revoke(grant.refreshToken());
                return List.of("Google Drive へのファイル保存が許可されませんでした。同意画面で許可してください。");
            }
            EvidenceDriveConnection existing = connectionRepository.findByAppUserId(appUserId).orElse(null);
            String folderId = existing == null ? null : existing.getFolderId();
            if (folderId == null || gateway.status(grant.accessToken(), folderId).state() != RemoteState.PRESENT) {
                folderId = gateway.createFolder(grant.accessToken(), FOLDER_NAME);
            }
            String encrypted = cipher().encrypt(grant.refreshToken());
            String finalFolderId = folderId;
            tx.executeWithoutResult(status -> {
                EvidenceDriveConnection connection = connectionRepository.findByAppUserId(appUserId)
                        .orElseGet(EvidenceDriveConnection::new);
                if (connection.getId() == null) {
                    connection.setAppUser(appUserRepository.getReferenceById(appUserId));
                }
                connection.setEncryptedRefreshToken(encrypted);
                connection.setFolderId(finalFolderId);
                connection.setGrantedScope(GoogleDriveSettings.DRIVE_FILE_SCOPE);
                connection.setConnectedAt(LocalDateTime.now());
                connectionRepository.save(connection);
            });
            audit("connect", appUserId, null, "success");
            return List.of();
        } catch (DriveException ex) {
            audit("connect", appUserId, null, "failure");
            return List.of(ex.getMessage());
        }
    }

    public List<String> disconnect(Long appUserId) {
        EvidenceDriveConnection connection = connectionRepository.findByAppUserId(appUserId).orElse(null);
        if (connection == null) {
            return List.of();
        }
        try {
            gateway.revoke(cipher().decrypt(connection.getEncryptedRefreshToken()));
        } catch (RuntimeException ex) {
            // 失効要求が失敗しても、アプリ側の保管トークンは必ず削除する。
            audit("revoke", appUserId, null, "failure");
        }
        tx.executeWithoutResult(status -> connectionRepository.deleteById(connection.getId()));
        audit("disconnect", appUserId, null, "success");
        return List.of();
    }

    public List<String> upload(Long appUserId, Long recordId, String originalName, byte[] content) {
        RetentionRecord record = ownedRecord(appUserId, recordId);
        if (!settings.isEnabled()) {
            audit("upload", appUserId, recordId, "disabled");
            return List.of(DISABLED_MESSAGE);
        }
        if (fileRepository.findByRetentionRecordId(record.getId()).isPresent()) {
            return List.of("この台帳記録には既に証憑ファイルがあります。");
        }
        EvidenceUploadValidator.Result checked = EvidenceUploadValidator.validate(originalName, content);
        if (!checked.ok()) {
            audit("upload", appUserId, recordId, "rejected");
            return List.of(checked.error());
        }
        String accessToken;
        try {
            accessToken = accessToken(appUserId);
        } catch (EvidenceException ex) {
            return List.of(ex.getMessage());
        }
        StoredFile stored;
        try {
            String driveName = "evidence-" + record.getId() + "-" + UUID.randomUUID() + "." + checked.extension();
            stored = gateway.upload(accessToken, connection(appUserId).getFolderId(), driveName,
                    checked.mimeType(), content);
        } catch (DriveException ex) {
            audit("upload", appUserId, recordId, "failure");
            return List.of(ex.getMessage());
        }
        if (stored.size() >= 0 && stored.size() != content.length) {
            safeDelete(accessToken, stored.id());
            audit("upload", appUserId, recordId, "size-mismatch");
            return List.of("Google Drive へ保存されたファイルのサイズが一致しませんでした。再度アップロードしてください。");
        }
        try {
            tx.executeWithoutResult(status -> {
                EvidenceFile file = new EvidenceFile();
                file.setRetentionRecord(recordRepository.getReferenceById(record.getId()));
                file.setStorageProvider(PROVIDER);
                file.setExternalFileId(stored.id());
                file.setOriginalFileName(checked.displayName());
                file.setMimeType(checked.mimeType());
                file.setFileSize((long) content.length);
                file.setSha256(checked.sha256());
                file.setUploadedAt(LocalDateTime.now());
                file.setVerificationStatus("UPLOADED");
                fileRepository.saveAndFlush(file);
            });
        } catch (RuntimeException ex) {
            safeDelete(accessToken, stored.id());
            audit("upload", appUserId, recordId, "db-failure");
            return List.of("証憑ファイルの登録に失敗しました。同時に別のファイルが登録された可能性があります。");
        }
        audit("upload", appUserId, recordId, "success");
        return List.of();
    }

    public EvidenceDownload download(Long appUserId, Long recordId) {
        RetentionRecord record = ownedRecord(appUserId, recordId);
        EvidenceFile file = fileRepository.findByRetentionRecordId(record.getId())
                .orElseThrow(() -> new EntryNotFoundException("証憑ファイルがありません。"));
        byte[] content;
        try {
            content = gateway.download(accessToken(appUserId), file.getExternalFileId(),
                    EvidenceUploadValidator.MAX_BYTES);
        } catch (DriveException ex) {
            audit("download", appUserId, recordId, "failure");
            throw new EvidenceException(ex.getMessage());
        }
        if (!EvidenceUploadValidator.sha256(content).equals(file.getSha256())) {
            updateStatus(file.getId(), "MISMATCH");
            audit("download", appUserId, recordId, "hash-mismatch");
            throw new EvidenceException("Google Drive 上のファイルが登録時の内容と一致しません（変更・差替えの可能性）。");
        }
        audit("download", appUserId, recordId, "success");
        String extension = "application/pdf".equals(file.getMimeType()) ? "pdf"
                : "image/png".equals(file.getMimeType()) ? "png" : "jpg";
        return new EvidenceDownload(content, file.getMimeType(), "evidence-" + record.getId() + "." + extension);
    }

    /** Drive 上の存在・ゴミ箱・サイズを確認し、検証状態を更新する。 */
    public List<String> verify(Long appUserId, Long recordId) {
        RetentionRecord record = ownedRecord(appUserId, recordId);
        EvidenceFile file = fileRepository.findByRetentionRecordId(record.getId()).orElse(null);
        if (file == null) {
            return List.of("証憑ファイルがありません。");
        }
        try {
            RemoteStatus remote = gateway.status(accessToken(appUserId), file.getExternalFileId());
            String result = switch (remote.state()) {
                case MISSING -> "MISSING";
                case TRASHED -> "TRASHED";
                case PRESENT -> remote.size() == file.getFileSize() ? "OK" : "MISMATCH";
            };
            updateStatus(file.getId(), result);
            audit("verify", appUserId, recordId, result);
            return "OK".equals(result) ? List.of() : List.of(statusMessage(result));
        } catch (DriveException ex) {
            audit("verify", appUserId, recordId, "failure");
            return List.of(ex.getMessage());
        } catch (EvidenceException ex) {
            return List.of(ex.getMessage());
        }
    }

    /**
     * 保存期限を経過した台帳記録と証憑ファイルを削除する。期限当日までと保全中は拒否する。
     * 期限は登録値と対象年分の最低保存期限の遅い方で判定する。
     */
    public List<String> deleteExpired(Long appUserId, Long recordId, LocalDate today) {
        RetentionRecord record = ownedRecord(appUserId, recordId);
        if (record.isLegalHold()) {
            audit("delete", appUserId, recordId, "legal-hold");
            return List.of("この記録は保全中のため削除できません。保全を解除してから操作してください。");
        }
        LocalDate deadline = RetentionRules.effectiveDeadline(record.getRetentionDeadline(),
                record.getFiscalYear().getTaxYear());
        if (!today.isAfter(deadline)) {
            audit("delete", appUserId, recordId, "before-deadline");
            return List.of("保存期限（" + deadline + "）を経過するまで削除できません。");
        }
        EvidenceFile file = fileRepository.findByRetentionRecordId(record.getId()).orElse(null);
        if (file != null) {
            try {
                gateway.delete(accessToken(appUserId), file.getExternalFileId());
            } catch (DriveException | EvidenceException ex) {
                audit("delete", appUserId, recordId, "failure");
                return List.of(ex.getMessage());
            }
        }
        tx.executeWithoutResult(status -> {
            fileRepository.findByRetentionRecordId(record.getId()).ifPresent(fileRepository::delete);
            recordRepository.deleteById(record.getId());
        });
        audit("delete", appUserId, recordId, "success");
        return List.of();
    }

    public static String statusMessage(String status) {
        return switch (status) {
            case "MISSING" -> "Google Drive 上でファイルが見つかりません（削除された可能性があります）。";
            case "TRASHED" -> "Google Drive 上でファイルがゴミ箱に移動されています。保存期限まで復元してください。";
            case "MISMATCH" -> "Google Drive 上のファイルが登録時と一致しません（変更・差替えの可能性）。";
            default -> "";
        };
    }

    private RetentionRecord ownedRecord(Long appUserId, Long recordId) {
        if (recordId == null) {
            throw new EntryNotFoundException("台帳記録が見つかりません。");
        }
        return recordRepository.findOwned(recordId, appUserId)
                .orElseThrow(() -> new EntryNotFoundException("台帳記録が見つかりません。"));
    }

    private EvidenceDriveConnection connection(Long appUserId) {
        requireConfigured();
        return connectionRepository.findByAppUserId(appUserId)
                .orElseThrow(() -> new EvidenceException("Google Drive と連携してから操作してください。"));
    }

    private String accessToken(Long appUserId) {
        EvidenceDriveConnection connection = connection(appUserId);
        try {
            return gateway.accessToken(cipher().decrypt(connection.getEncryptedRefreshToken()));
        } catch (DriveException ex) {
            if (ex.isAuthorizationRevoked()) {
                tx.executeWithoutResult(status -> connectionRepository.deleteById(connection.getId()));
                audit("token-revoked", appUserId, null, "disconnected");
                throw new EvidenceException("Google Drive の連携が解除されています。再度連携してください。");
            }
            throw new EvidenceException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new EvidenceException("保管トークンを復号できません。管理者に暗号鍵の設定を確認してもらってください。");
        }
    }

    private void updateStatus(Long fileId, String status) {
        tx.executeWithoutResult(s -> fileRepository.findById(fileId).ifPresent(file -> {
            file.setVerificationStatus(status);
            file.setLastVerifiedAt(LocalDateTime.now());
        }));
    }

    private void safeDelete(String accessToken, String fileId) {
        try {
            gateway.delete(accessToken, fileId);
        } catch (RuntimeException ignored) {
            // 補償削除の失敗は監査ログで追跡する。
            AUDIT.warn("evidence compensating delete failed");
        }
    }

    private TokenCipher cipher() {
        try {
            return new TokenCipher(settings.getTokenKey());
        } catch (IllegalStateException ex) {
            throw new EvidenceException("証憑保管の暗号鍵が正しく設定されていません。");
        }
    }

    private void requireConfigured() {
        if (!settings.isEnabled()) {
            throw new EvidenceException(DISABLED_MESSAGE);
        }
        if (!settings.isConfigured()) {
            throw new EvidenceException("Google Drive 連携は未設定です。管理者が設定を完了するまで利用できません。");
        }
    }

    private String randomToken(int bytes) {
        byte[] data = new byte[bytes];
        random.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void audit(String action, Long appUserId, Long recordId, String result) {
        AUDIT.info("evidence action={} user={} record={} result={}", action, appUserId, recordId, result);
    }

    public record AuthorizationStart(String state, String codeVerifier, String url) {
        @Override
        public String toString() {
            return "AuthorizationStart[]";
        }
    }

    public record EvidenceDownload(byte[] content, String mimeType, String fileName) {
    }

    public record EvidenceDriveStatus(boolean enabled, boolean configured, boolean connected, LocalDateTime connectedAt) {
        public static EvidenceDriveStatus disabled() {
            return new EvidenceDriveStatus(false, false, false, null);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isConfigured() {
            return configured;
        }

        public boolean isConnected() {
            return connected;
        }

        public LocalDateTime getConnectedAt() {
            return connectedAt;
        }
    }

    /** 利用者へ表示してよい失敗。 */
    public static class EvidenceException extends RuntimeException {
        public EvidenceException(String message) {
            super(message);
        }
    }
}
