package org.example.integration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.EvidenceDriveConnection;
import org.example.entity.FiscalYear;
import org.example.entity.JournalEntry;
import org.example.entity.RetentionRecord;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.evidence.DriveGateway;
import org.example.features.taxreturn.evidence.EvidenceFileService;
import org.example.features.taxreturn.evidence.EvidenceFileService.EvidenceException;
import org.example.features.taxreturn.evidence.EvidenceUploadValidator;
import org.example.features.taxreturn.evidence.GoogleDriveSettings;
import org.example.features.taxreturn.evidence.TokenCipher;
import org.example.features.taxreturn.retention.RetentionPageData;
import org.example.features.taxreturn.retention.RetentionRecordForm;
import org.example.features.taxreturn.retention.RetentionRules;
import org.example.features.taxreturn.retention.RetentionSearch;
import org.example.features.taxreturn.retention.RetentionService;
import org.example.features.taxreturn.retention.RetentionServiceImpl;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.EvidenceDriveConnectionRepository;
import org.example.repository.EvidenceFileRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.RetentionRecordRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 証憑ファイル保管（Google Drive 連携）の検証。Drive への通信は偽実装に差し替え、H2 上で
 * 受入検査・トークン暗号化・所有者分離・改変検知・期限前削除拒否・検索を確認する。
 */
public final class EvidenceFileHarness {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final LocalDate DEADLINE = LocalDate.of(2032, 3, 15);

    public static void main(String[] args) throws Exception {
        verifyValidator();
        verifyCipher();
        verifyRules();
        verifyUnconfigured();
        Path dir = Path.of("target", "evidence-file-harness");
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(dir);
        System.setProperty("evidence.harness.url",
                "jdbc:h2:file:" + dir.toAbsolutePath().resolve("db") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
            new EvidenceFileHarness().run(context);
        }
        System.out.println("Evidence file harness passed");
    }

    private static void verifyValidator() {
        byte[] pdf = "%PDF-1.7\n1 0 obj << /Type /Catalog >> endobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
        require(EvidenceUploadValidator.validate("receipt.pdf", pdf).ok(), "valid PDF rejected");
        require("image/jpeg".equals(EvidenceUploadValidator.validate("a.JPEG", jpeg).mimeType()), "JPEG not detected");
        require(!EvidenceUploadValidator.validate("fake.pdf", png).ok(), "PNG disguised as PDF accepted");
        require(!EvidenceUploadValidator.validate("run.exe", pdf).ok(), "unsupported extension accepted");
        require(!EvidenceUploadValidator.validate("page.svg", png).ok(), "SVG extension accepted");
        require(!EvidenceUploadValidator.validate("empty.pdf", new byte[0]).ok(), "empty file accepted");
        byte[] large = new byte[(int) EvidenceUploadValidator.MAX_BYTES + 1];
        System.arraycopy(pdf, 0, large, 0, pdf.length);
        require(!EvidenceUploadValidator.validate("large.pdf", large).ok(), "file over 10MB accepted");
        byte[] script = "%PDF-1.7\n<< /OpenAction << /S /JavaScript /JS (app.alert(1)) >> >>"
                .getBytes(StandardCharsets.ISO_8859_1);
        require(!EvidenceUploadValidator.validate("script.pdf", script).ok(), "PDF with JavaScript accepted");
        require("a.pdf".equals(EvidenceUploadValidator.displayName("..\\..\\dir/a.pdf")), "path not stripped from name");
        require("ab.pdf".equals(EvidenceUploadValidator.displayName("a\u0000\u202Eb.pdf")), "control chars not stripped");
    }

    private static void verifyCipher() {
        TokenCipher cipher = new TokenCipher(KEY);
        String encrypted = cipher.encrypt("refresh-secret");
        require(encrypted.startsWith("v1:") && !encrypted.contains("refresh-secret"), "token not encrypted");
        require(!encrypted.equals(cipher.encrypt("refresh-secret")), "encryption is deterministic");
        require("refresh-secret".equals(cipher.decrypt(encrypted)), "token round trip failed");
        char[] chars = encrypted.toCharArray();
        chars[chars.length - 3] = chars[chars.length - 3] == 'A' ? 'B' : 'A';
        requireThrows(() -> cipher.decrypt(new String(chars)), IllegalStateException.class, "tampered token decrypted");
        requireThrows(() -> new TokenCipher(Base64.getEncoder().encodeToString(new byte[16])),
                IllegalStateException.class, "short key accepted");
        String otherKey = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
        requireThrows(() -> new TokenCipher(otherKey).decrypt(encrypted), IllegalStateException.class,
                "token decrypted with another key");
    }

    private static void verifyUnconfigured() {
        GoogleDriveSettings blank = new GoogleDriveSettings("", "", "", "");
        require(!blank.isConfigured(), "blank settings configured");
        require(!new GoogleDriveSettings("id", "secret", "http://evil.example/cb", KEY).isConfigured(),
                "non-HTTPS redirect URI accepted");
        require(new GoogleDriveSettings("id", "secret", "https://app.example/cb", KEY).isConfigured(),
                "complete settings not configured");
        require(!new GoogleDriveSettings("", "id", "secret", "https://app.example/cb", KEY).isEnabled()
                && !new GoogleDriveSettings("yes", "id", "secret", "https://app.example/cb", KEY).isEnabled()
                && new GoogleDriveSettings("TRUE", "id", "secret", "https://app.example/cb", KEY).isEnabled(),
                "evidence upload must stay disabled unless explicitly enabled");
    }

    private void run(AnnotationConfigApplicationContext context) {
        FakeDrive drive = context.getBean(FakeDrive.class);
        EvidenceFileService service = context.getBean(EvidenceFileService.class);
        RetentionService retention = context.getBean(RetentionService.class);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        Long owner = seedUser(context, tx, "owner@example.com");
        Long other = seedUser(context, tx, "other@example.com");
        Long receiptId = seedRecord(context, tx, owner, "EVIDENCE", "領収書", "文具購入", DEADLINE, true);
        Long bookId = seedRecord(context, tx, owner, "BOOK", "仕訳帳", "2025年分仕訳帳", LocalDate.of(2026, 1, 31), false);

        require(!service.status(owner).isConnected(), "connected before OAuth");
        var start = service.beginAuthorization();
        require(start.url().contains("code_challenge=") && start.url().contains(start.state())
                && !start.url().contains(start.codeVerifier()), "authorization URL must carry state and PKCE challenge only");

        drive.grantScope = "openid";
        require(!service.completeAuthorization(owner, "code", start.codeVerifier()).isEmpty(), "missing scope accepted");
        require(drive.revoked.contains("refresh-token"), "token with missing scope not revoked");
        require(!service.status(owner).isConnected(), "connection saved without drive.file scope");

        drive.grantScope = GoogleDriveSettings.DRIVE_FILE_SCOPE;
        require(service.completeAuthorization(owner, "code", start.codeVerifier()).isEmpty(), "OAuth completion failed");
        require(start.codeVerifier().equals(drive.lastVerifier), "PKCE verifier not sent on code exchange");
        EvidenceDriveConnection connection = context.getBean(EvidenceDriveConnectionRepository.class)
                .findByAppUserId(owner).orElseThrow();
        require(!connection.getEncryptedRefreshToken().contains("refresh-token")
                && "refresh-token".equals(new TokenCipher(KEY).decrypt(connection.getEncryptedRefreshToken())),
                "refresh token not stored encrypted");
        require(service.status(owner).isConnected(), "status not connected after OAuth");

        byte[] pdf = "%PDF-1.7\nreceipt\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
        require(!service.upload(owner, receiptId, "fake.pdf", new byte[] {1, 2, 3}).isEmpty(), "invalid file uploaded");
        require(drive.files.isEmpty(), "rejected file reached Drive");
        require(service.upload(owner, receiptId, "C:\\Users\\me\\領収書.pdf", pdf).isEmpty(), "upload failed");
        require(drive.files.size() == 1, "file not stored on Drive");
        String driveName = drive.names.values().iterator().next();
        require(driveName.startsWith("evidence-" + receiptId + "-") && driveName.endsWith(".pdf")
                && !driveName.contains("領収書"), "Drive name must be server-generated");
        require(drive.parents.values().iterator().next().equals(connection.getFolderId()), "file not placed in app folder");
        require(!service.upload(owner, receiptId, "again.pdf", pdf).isEmpty(), "second file for same record accepted");

        requireThrows(() -> service.upload(other, receiptId, "x.pdf", pdf), EntryNotFoundException.class,
                "other user uploaded to owner's record");
        requireThrows(() -> service.download(other, receiptId), EntryNotFoundException.class,
                "other user downloaded owner's file");
        requireThrows(() -> service.deleteExpired(other, bookId, DEADLINE.plusYears(10)), EntryNotFoundException.class,
                "other user deleted owner's record");

        var download = service.download(owner, receiptId);
        require(java.util.Arrays.equals(pdf, download.content()) && "application/pdf".equals(download.mimeType())
                && download.fileName().equals("evidence-" + receiptId + ".pdf"), "download content mismatch");

        String fileId = drive.files.keySet().iterator().next();
        drive.files.put(fileId, "%PDF-1.7\naltered\n%%EOF".getBytes(StandardCharsets.ISO_8859_1));
        requireThrows(() -> service.download(owner, receiptId), EvidenceException.class, "tampered file downloaded");
        RetentionPageData alerts = retention.search(owner, 2025, new RetentionSearch(null, null, "FILE_ALERT"),
                LocalDate.of(2026, 2, 1));
        require(alerts.getRecords().size() == 1 && "MISMATCH".equals(alerts.getRecords().get(0).getFile().getVerificationStatus()),
                "hash mismatch not flagged");
        drive.files.put(fileId, pdf);
        require(service.verify(owner, receiptId).isEmpty(), "verify of intact file failed");
        drive.trashed.add(fileId);
        require(!service.verify(owner, receiptId).isEmpty(), "trashed file not reported");
        drive.trashed.clear();
        drive.files.remove(fileId);
        require(!service.verify(owner, receiptId).isEmpty(), "missing file not reported");
        drive.files.put(fileId, pdf);
        require(service.verify(owner, receiptId).isEmpty(), "restored file not verified");

        EvidenceFileService disabled = disabledService(context, drive);
        require(!disabled.status(owner).isEnabled() && !disabled.status(owner).isConnected(),
                "disabled feature reported as available");
        requireThrows(disabled::beginAuthorization, EvidenceException.class, "OAuth started while disabled");
        int storedFiles = drive.files.size();
        require(!disabled.upload(owner, bookId, "r.pdf", pdf).isEmpty() && drive.files.size() == storedFiles,
                "upload accepted while disabled");
        requireThrows(() -> disabled.upload(other, bookId, "r.pdf", pdf), EntryNotFoundException.class,
                "ownership not checked while disabled");
        requireThrows(() -> disabled.download(owner, receiptId), EvidenceException.class, "download while disabled");
        require(!disabled.verify(owner, receiptId).isEmpty(), "verify while disabled");
        require(!disabled.deleteExpired(owner, receiptId, DEADLINE.plusDays(1)).isEmpty()
                && drive.files.containsKey(fileId), "record with Drive file deleted while disabled");

        LocalDate today = LocalDate.of(2031, 3, 16);
        RetentionPageData all = retention.search(owner, 2025, RetentionSearch.all(), today);
        require(all.getTotalCount() == 2 && all.getExpiredCount() == 1, "expired count wrong");
        require(all.getMinimumDeadline().equals(LocalDate.of(2031, 3, 15)) && all.getManualRuleCount() == 2,
                "minimum deadline guidance wrong");
        RetentionPageData early = retention.search(owner, 2025, RetentionSearch.all(), LocalDate.of(2026, 2, 1));
        require(early.getExpiredCount() == 0, "manual deadline below minimum treated as expired");
        require(retention.search(owner, 2025, new RetentionSearch("領収書", null, null), today).getRecords().size() == 1,
                "keyword search on original file name failed");
        require(retention.search(owner, 2025, new RetentionSearch("文具", "EVIDENCE", "WITH_FILE"), today)
                .getRecords().size() == 1, "combined search failed");
        require(retention.search(owner, 2025, new RetentionSearch(null, "BOOK", "EXPIRED"), today)
                .getRecords().size() == 1, "expired filter failed");
        require(retention.search(owner, 2025, new RetentionSearch(null, null, "WITHOUT_FILE"), today)
                .getRecords().get(0).getId().equals(bookId), "without-file filter failed");
        require(retention.search(owner, 2025, new RetentionSearch(null, "bogus", "bogus"), today).getRecords().size() == 2,
                "invalid search values not ignored");

        require(!service.deleteExpired(owner, receiptId, DEADLINE).isEmpty(), "deleted on deadline day");
        require(!service.deleteExpired(owner, receiptId, today).isEmpty(), "deleted before deadline");
        require(drive.files.containsKey(fileId), "Drive file deleted before deadline");
        require(!service.deleteExpired(owner, bookId, LocalDate.of(2026, 2, 1)).isEmpty(),
                "deleted before minimum retention deadline");
        require(!retention.placeLegalHold(owner, bookId, "  ").isEmpty(), "hold without reason accepted");
        requireThrows(() -> retention.placeLegalHold(other, bookId, "x"), EntryNotFoundException.class,
                "other user placed hold");
        require(retention.placeLegalHold(owner, bookId, "税務調査対応").isEmpty(), "hold failed");
        require(!retention.placeLegalHold(owner, bookId, "再保全").isEmpty(), "double hold accepted");
        RetentionPageData held = retention.search(owner, 2025, new RetentionSearch(null, null, "HELD"), today);
        require(held.getRecords().size() == 1 && held.getHeldCount() == 1
                && "HELD".equals(held.getRecords().get(0).getDeadlineStatus())
                && !held.getRecords().get(0).isDeletable(), "held guidance wrong");
        require(!service.deleteExpired(owner, bookId, today).isEmpty(), "held record deleted");
        requireThrows(() -> retention.releaseLegalHold(other, bookId), EntryNotFoundException.class,
                "other user released hold");
        require(retention.releaseLegalHold(owner, bookId).isEmpty(), "release failed");
        require(!retention.releaseLegalHold(owner, bookId).isEmpty(), "release of unheld record accepted");
        require(disabled.deleteExpired(owner, bookId, today).isEmpty(),
                "ledger record without file not deletable while upload is disabled");
        require(service.deleteExpired(owner, receiptId, DEADLINE.plusDays(1)).isEmpty(), "expired record not deleted");
        require(drive.files.isEmpty(), "Drive file not deleted after deadline");
        require(context.getBean(RetentionRecordRepository.class).count() == 0
                && context.getBean(EvidenceFileRepository.class).count() == 0, "ledger rows not deleted");

        Long later = seedRecord(context, tx, owner, "BOOK", "総勘定元帳", "元帳", DEADLINE, false);
        drive.revokeOnRefresh = true;
        require(!service.upload(owner, later, "r.pdf", pdf).isEmpty(), "upload succeeded with revoked grant");
        require(!service.status(owner).isConnected(), "revoked grant did not remove connection");
        drive.revokeOnRefresh = false;

        var restart = service.beginAuthorization();
        require(service.completeAuthorization(owner, "code", restart.codeVerifier()).isEmpty(), "reconnect failed");
        require(service.disconnect(owner).isEmpty() && !service.status(owner).isConnected(), "disconnect failed");
        require(drive.revoked.size() >= 2, "disconnect did not revoke token");
        requireThrows(() -> service.download(owner, later), EntryNotFoundException.class, "download without file");
        verifyRuleRegistration(context, tx, retention, owner);
    }

    private static void verifyRules() {
        var blueBook = RetentionRules.calculate(RetentionRules.Category.BOOK_MAIN, true, 2025, null, false);
        require(blueBook.startDate().equals(LocalDate.of(2026, 3, 16))
                && blueBook.deadline().equals(LocalDate.of(2033, 3, 15)), "blue book deadline wrong");
        require(RetentionRules.calculate(RetentionRules.Category.BOOK_MAIN, true, 2025, null, true).deadline()
                .equals(LocalDate.of(2033, 3, 15)), "consumption tax shortened book deadline");
        LocalDate may = LocalDate.of(2025, 5, 10);
        require(RetentionRules.calculate(RetentionRules.Category.CASH_DOC, true, 2025, may, false).deadline()
                .equals(LocalDate.of(2033, 3, 15)), "blue cash document deadline wrong");
        var other = RetentionRules.calculate(RetentionRules.Category.OTHER_DOC, true, 2025, may, false);
        require(other.deadline().equals(LocalDate.of(2031, 3, 15)) && other.years() == 5, "blue other doc wrong");
        var otherTax = RetentionRules.calculate(RetentionRules.Category.OTHER_DOC, true, 2025, may, true);
        require(otherTax.deadline().equals(LocalDate.of(2033, 2, 28)) && otherTax.legalBasis().contains("消費税法施行令50条")
                && otherTax.legalBasis().contains(RetentionRules.CHECKED_ON), "consumption tax rule not applied");
        require(RetentionRules.calculate(RetentionRules.Category.SETTLEMENT_DOC, true, 2025, LocalDate.of(2026, 2, 20),
                false).deadline().equals(LocalDate.of(2034, 3, 15)), "document date year not used as start");
        var oldDoc = RetentionRules.calculate(RetentionRules.Category.OTHER_DOC, true, 2025, LocalDate.of(2024, 12, 1),
                false);
        require(oldDoc.deadline().equals(LocalDate.of(2031, 3, 15)) && oldDoc.legalBasis().contains("最低保存期限"),
                "minimum deadline not applied to older document");
        require(RetentionRules.calculate(RetentionRules.Category.BOOK_MAIN, false, 2025, null, false).deadline()
                .equals(LocalDate.of(2033, 3, 15)), "white statutory book deadline wrong");
        require(RetentionRules.calculate(RetentionRules.Category.BOOK_OTHER, false, 2025, null, false).deadline()
                .equals(LocalDate.of(2031, 3, 15)), "white other book deadline wrong");
        require(RetentionRules.calculate(RetentionRules.Category.CASH_DOC, false, 2025, may, true).deadline()
                .equals(LocalDate.of(2033, 2, 28)), "white consumption tax document deadline wrong");
        require(!RetentionRules.supportsYear(2023) && !RetentionRules.supportsYear(2027), "unconfirmed year supported");
        requireThrows(() -> RetentionRules.calculate(RetentionRules.Category.BOOK_MAIN, true, 2023, null, false),
                IllegalArgumentException.class, "unsupported year calculated");
        require(RetentionRules.effectiveDeadline(LocalDate.of(2026, 1, 31), 2025).equals(LocalDate.of(2031, 3, 15))
                && RetentionRules.effectiveDeadline(LocalDate.of(2040, 1, 1), 2025).equals(LocalDate.of(2040, 1, 1)),
                "effective deadline wrong");
    }

    private void verifyRuleRegistration(AnnotationConfigApplicationContext context, TransactionTemplate tx,
            RetentionService retention, Long owner) {
        LocalDate today = LocalDate.of(2026, 6, 1);
        RetentionRecordForm book = ruleForm("BOOK", "BOOK_MAIN", null);
        book.setLegalBasis("ignored");
        book.setRetentionDeadline("2026-01-01");
        require(retention.add(owner, 2025, book, today).isEmpty(), "rule-based book not registered");
        RetentionRecord saved = context.getBean(RetentionRecordRepository.class).findAll().stream()
                .filter(r -> "BOOK_MAIN".equals(r.getRetentionRuleCode())).findFirst().orElseThrow();
        require(saved.getRetentionDeadline().equals(LocalDate.of(2033, 3, 15))
                && saved.getRetentionStartDate().equals(LocalDate.of(2026, 3, 16))
                && saved.getLegalBasis().contains("所得税法施行規則63条")
                && RetentionRules.RULE_VERSION.equals(saved.getRuleVersion()), "server-side rule not applied");

        Long journalId = retention.search(owner, 2025, RetentionSearch.all(), today).getJournalEntries().get(0).getId();
        RetentionRecordForm receipt = ruleForm("EVIDENCE", "OTHER_DOC", journalId);
        receipt.setDocumentDate("2025-05-10");
        receipt.setConsumptionTax(true);
        require(retention.add(owner, 2025, receipt, today).isEmpty(), "rule-based document not registered");
        require(context.getBean(RetentionRecordRepository.class).findAll().stream()
                .anyMatch(r -> "OTHER_DOC".equals(r.getRetentionRuleCode()) && r.isConsumptionTaxApplies()
                        && r.getRetentionDeadline().equals(LocalDate.of(2033, 2, 28))
                        && LocalDate.of(2025, 5, 10).equals(r.getDocumentDate())), "document rule not stored");

        require(!retention.add(owner, 2025, ruleForm("EVIDENCE", "BOOK_MAIN", journalId), today).isEmpty(),
                "book rule accepted for evidence");
        require(!retention.add(owner, 2025, ruleForm("BOOK", "CASH_DOC", null), today).isEmpty(),
                "document rule accepted for book");
        require(!retention.add(owner, 2025, ruleForm("EVIDENCE", "CASH_DOC", journalId), today).isEmpty(),
                "document rule without document date accepted");
        RetentionRecordForm future = ruleForm("EVIDENCE", "CASH_DOC", journalId);
        future.setDocumentDate("2026-06-02");
        require(!retention.add(owner, 2025, future, today).isEmpty(), "future document date accepted");
        require(!retention.add(owner, 2025, ruleForm("BOOK", "BOGUS", null), today).isEmpty(), "unknown rule accepted");

        RetentionRecordForm manual = ruleForm("BOOK", null, null);
        manual.setRetentionStartDate("2026-03-16");
        manual.setRetentionDeadline("2030-03-15");
        manual.setLegalBasis("利用者確認");
        require(!retention.add(owner, 2025, manual, today).isEmpty(), "manual deadline below minimum accepted");
        manual.setRetentionDeadline("2031-03-15");
        require(retention.add(owner, 2025, manual, today).isEmpty(), "manual deadline at minimum rejected");

        tx.executeWithoutResult(status -> {
            BusinessProfile profile = context.getBean(BusinessProfileRepository.class).findByAppUserId(owner).orElseThrow();
            FiscalYear old = new FiscalYear();
            old.setBusinessProfile(profile);
            old.setTaxYear(2023);
            old.setPeriodStart(LocalDate.of(2023, 1, 1));
            old.setPeriodEnd(LocalDate.of(2023, 12, 31));
            old.setStatus("open");
            context.getBean(FiscalYearRepository.class).save(old);
        });
        require(!retention.add(owner, 2023, ruleForm("BOOK", "BOOK_MAIN", null), today).isEmpty(),
                "rule applied to unconfirmed year");
        require(!retention.search(owner, 2023, RetentionSearch.all(), today).isRuleYearSupported(),
                "unconfirmed year reported as supported");
    }

    private static RetentionRecordForm ruleForm(String kind, String rule, Long journalId) {
        RetentionRecordForm form = new RetentionRecordForm();
        form.setRecordKind(kind);
        form.setDocumentType("ハーネス");
        form.setTitle("ルール確認");
        form.setRuleCode(rule);
        form.setJournalEntryId(journalId);
        return form;
    }

    private Long seedUser(AnnotationConfigApplicationContext context, TransactionTemplate tx, String loginId) {
        return tx.execute(status -> {
            AppUser user = new AppUser();
            user.setLoginId(loginId);
            user.setEmail(loginId);
            user.setPasswordHash("hash");
            user.setDisplayName("Harness");
            user.setStatus("active");
            user = context.getBean(AppUserRepository.class).save(user);
            BusinessProfile profile = new BusinessProfile();
            profile.setAppUser(user);
            profile.setBusinessName("Harness");
            profile.setFilingType("blue");
            profile = context.getBean(BusinessProfileRepository.class).save(profile);
            FiscalYear fy = new FiscalYear();
            fy.setBusinessProfile(profile);
            fy.setTaxYear(2025);
            fy.setPeriodStart(LocalDate.of(2025, 1, 1));
            fy.setPeriodEnd(LocalDate.of(2025, 12, 31));
            fy.setStatus("open");
            context.getBean(FiscalYearRepository.class).save(fy);
            return user.getId();
        });
    }

    private Long seedRecord(AnnotationConfigApplicationContext context, TransactionTemplate tx, Long userId, String kind,
            String type, String title, LocalDate deadline, boolean withJournal) {
        return tx.execute(status -> {
            BusinessProfile profile = context.getBean(BusinessProfileRepository.class).findByAppUserId(userId).orElseThrow();
            FiscalYear fy = context.getBean(FiscalYearRepository.class)
                    .findByBusinessProfileIdOrderByTaxYearDesc(profile.getId()).get(0);
            JournalEntry entry = null;
            if (withJournal) {
                entry = new JournalEntry();
                entry.setFiscalYear(fy);
                entry.setSourceType("MANUAL");
                entry.setEntryDate(LocalDate.of(2025, 4, 1));
                entry.setSummary("文具購入");
                entry.setStatus("draft");
                entry = context.getBean(JournalEntryRepository.class).save(entry);
            }
            RetentionRecord record = new RetentionRecord();
            record.setFiscalYear(fy);
            record.setJournalEntry(entry);
            record.setRecordKind(kind);
            record.setDocumentType(type);
            record.setTitle(title);
            record.setRetentionStartDate(LocalDate.of(2026, 3, 16));
            record.setRetentionDeadline(deadline);
            record.setLegalBasis("ハーネス用の確認根拠");
            return context.getBean(RetentionRecordRepository.class).save(record).getId();
        });
    }

    private static EvidenceFileService disabledService(AnnotationConfigApplicationContext context, FakeDrive drive) {
        return new EvidenceFileService(new GoogleDriveSettings("false", "client", "secret",
                "https://app.example/callback", KEY), drive, context.getBean(EvidenceDriveConnectionRepository.class),
                context.getBean(EvidenceFileRepository.class), context.getBean(RetentionRecordRepository.class),
                context.getBean(org.example.repository.AppUserRepository.class),
                context.getBean(PlatformTransactionManager.class));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireThrows(Runnable action, Class<? extends Throwable> type, String message) {
        try {
            action.run();
        } catch (Throwable ex) {
            if (type.isInstance(ex)) {
                return;
            }
            throw new IllegalStateException(message + " (unexpected " + ex + ")");
        }
        throw new IllegalStateException(message);
    }

    /** Google Drive / OAuth の偽実装。 */
    static final class FakeDrive implements DriveGateway {
        final Map<String, byte[]> files = new HashMap<>();
        final Map<String, String> names = new HashMap<>();
        final Map<String, String> parents = new HashMap<>();
        final List<String> trashed = new ArrayList<>();
        final List<String> folders = new ArrayList<>();
        final List<String> revoked = new ArrayList<>();
        String grantScope = GoogleDriveSettings.DRIVE_FILE_SCOPE;
        String lastVerifier;
        boolean revokeOnRefresh;
        private int sequence;

        @Override
        public String authorizationUrl(String state, String codeChallenge) {
            return "https://accounts.example/auth?state=" + state + "&code_challenge=" + codeChallenge;
        }

        @Override
        public TokenGrant exchangeCode(String code, String codeVerifier) {
            lastVerifier = codeVerifier;
            return new TokenGrant("refresh-token", "access-token", grantScope);
        }

        @Override
        public String accessToken(String refreshToken) {
            if (revokeOnRefresh) {
                throw new DriveException("revoked", true);
            }
            require("refresh-token".equals(refreshToken), "unexpected refresh token");
            return "access-token";
        }

        @Override
        public String createFolder(String accessToken, String name) {
            String id = "folder" + (++sequence);
            folders.add(id);
            return id;
        }

        @Override
        public StoredFile upload(String accessToken, String folderId, String name, String mimeType, byte[] content) {
            String id = "file" + (++sequence);
            files.put(id, content.clone());
            names.put(id, name);
            parents.put(id, folderId);
            return new StoredFile(id, content.length);
        }

        @Override
        public RemoteStatus status(String accessToken, String fileId) {
            if (folders.contains(fileId)) {
                return new RemoteStatus(RemoteState.PRESENT, 0);
            }
            byte[] content = files.get(fileId);
            if (content == null) {
                return new RemoteStatus(RemoteState.MISSING, 0);
            }
            return new RemoteStatus(trashed.contains(fileId) ? RemoteState.TRASHED : RemoteState.PRESENT, content.length);
        }

        @Override
        public byte[] download(String accessToken, String fileId, long maxBytes) {
            byte[] content = files.get(fileId);
            if (content == null) {
                throw new DriveException("missing", false);
            }
            return content.clone();
        }

        @Override
        public void delete(String accessToken, String fileId) {
            files.remove(fileId);
        }

        @Override
        public void revoke(String token) {
            revoked.add(token);
        }
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "org.example.repository")
    @Import({EvidenceFileService.class, RetentionServiceImpl.class})
    static class Config {
        @Bean
        FakeDrive driveGateway() {
            return new FakeDrive();
        }

        @Bean
        GoogleDriveSettings googleDriveSettings() {
            return new GoogleDriveSettings("client", "secret", "https://app.example/callback", KEY);
        }

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource(System.getProperty("evidence.harness.url"), "sa", "");
            ds.setDriverClassName("org.h2.Driver");
            return ds;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            emf.setPackagesToScan("org.example.entity");
            emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            Properties p = new Properties();
            p.put("hibernate.hbm2ddl.auto", "create");
            p.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            emf.setJpaProperties(p);
            return emf;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }
    }
}
