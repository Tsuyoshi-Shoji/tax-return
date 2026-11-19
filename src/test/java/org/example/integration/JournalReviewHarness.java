package org.example.integration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.example.controller.JournalReviewController;
import org.example.entity.AccountingTransaction;
import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.FiscalYear;
import org.example.entity.HouseholdConsumption;
import org.example.entity.JournalEntry;
import org.example.entity.JournalLine;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.entry.form.EntryForm;
import org.example.features.entry.service.EntryService;
import org.example.features.taxreturn.dto.JournalReviewPage;
import org.example.features.taxreturn.dto.CardSettlementPageData;
import org.example.features.taxreturn.form.CardSettlementForm;
import org.example.features.taxreturn.service.CardSettlementService;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.features.taxreturn.service.JournalReviewService;
import org.example.features.taxreturn.service.TaxReturnService;
import org.example.repository.AccountingTransactionRepository;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.HouseholdConsumptionRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.PaymentMethodRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jakarta.el.ELProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public final class JournalReviewHarness {
    private static final int YEAR = 2025;
    private AnnotationConfigApplicationContext context;
    private TransactionTemplate tx;
    private JournalReviewService service;
    private JournalEntryRepository journals;
    private FiscalYearRepository years;
    private final List<Long> ids = new ArrayList<>();
    private Long userId;
    private Long otherUserId;
    private Long yearId;
    private Long otherEntryId;

    public static void main(String[] args) throws Exception {
        new JournalReviewHarness().run();
    }

    private void run() throws Exception {
        verifyViewConditions();
        String url = "jdbc:h2:mem:journal-review;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
        ProductionBootstrapHarness.applyMigrations(url, 1, 19);
        System.setProperty("bootstrap.url", url);
        try (var app = new AnnotationConfigApplicationContext(ProductionBootstrapHarness.Config.class)) {
            context = app;
            tx = new TransactionTemplate(app.getBean(PlatformTransactionManager.class));
            service = app.getBean(JournalReviewService.class);
            journals = app.getBean(JournalEntryRepository.class);
            years = app.getBean(FiscalYearRepository.class);
            seed();
            verifyPagingAndOwnership();
            verifyController();
            verifyStaleAndInvalid();
            verifyConcurrentConfirmation();
            verifyBulkConfirmation();
            confirmAll();
            verifyEditedEntry();
            verifyMissingJournalGeneration();
            verifySettlementDateCorrection();
            verifySettlementScreen();
            verifyGeneratedEntry();
            verifyClosedYear();
        }
        System.out.println("Journal review harness passed");
    }

    private void verifyViewConditions() throws Exception {
        String jsp = Files.readString(Path.of("src", "main", "webapp", "WEB-INF", "views", "TaxReturn",
                "journal-review.jsp"));
        String generation = viewCondition(jsp, "missing\\.[^\"}]+");
        String confirmation = viewCondition(jsp, "not review.closed and entry\\.[^\"}]+");
        ELProcessor el = new ELProcessor();
        for (String kind : new String[] { "income", "expense", null }) {
            el.defineBean("missing", new JournalReviewPage.MissingTransaction(1L, LocalDate.of(YEAR, 3, 1),
                    "Synthetic missing journal", BigDecimal.TEN, kind, kind == null ? null : 2L));
            require(Boolean.valueOf(kind != null).equals(el.eval(generation)),
                    "The JSP must offer generation only when source data exists");
        }
        for (boolean closed : new boolean[] { false, true }) {
            el.defineBean("review", new JournalReviewPage(YEAR, List.of(), closed, "all", 0, 1, 1, 1, 0,
                    List.of(), List.of()));
            for (boolean reviewed : new boolean[] { false, true }) {
                for (String error : new String[] { null, "Synthetic invalid entry" }) {
                    el.defineBean("entry", new JournalReviewPage.Entry(1L, LocalDate.of(YEAR, 3, 1),
                            "Synthetic entry", reviewed, "synthetic-fingerprint", List.of(), error,
                            "ACCOUNTING_TRANSACTION"));
                    require(Boolean.valueOf(!closed && !reviewed && error == null).equals(el.eval(confirmation)),
                            "The JSP must offer bulk selection only for valid, unreviewed entries in an open year");
                }
            }
        }
    }

    private static String viewCondition(String jsp, String expressionPattern) {
        var matcher = Pattern.compile("test=\"\\$\\{(" + expressionPattern + ")}\"").matcher(jsp);
        require(matcher.find(), "Missing JSP condition: " + expressionPattern);
        String expression = matcher.group(1);
        require(!matcher.find(), "Ambiguous JSP condition: " + expressionPattern);
        return expression;
    }

    private void seed() {
        tx.executeWithoutResult(status -> {
            FiscalYear year = seedYear("review-owner");
            userId = year.getBusinessProfile().getAppUser().getId();
            yearId = year.getId();
            for (int i = 0; i < 42; i++) {
                ids.add(seedEntry(year, "Synthetic entry " + i).getId());
            }
            FiscalYear other = seedYear("review-other");
            otherUserId = other.getBusinessProfile().getAppUser().getId();
            otherEntryId = seedEntry(other, "Other owner's entry").getId();
            FiscalYear next = new FiscalYear();
            next.setBusinessProfile(year.getBusinessProfile());
            next.setTaxYear(2026);
            next.setPeriodStart(LocalDate.of(2026, 1, 1));
            next.setPeriodEnd(LocalDate.of(2026, 12, 31));
            next.setStatus("open");
            years.save(next);
        });
    }

    private FiscalYear seedYear(String name) {
        AppUser user = new AppUser();
        user.setLoginId(name);
        user.setEmail(name + "@example.test");
        user.setDisplayName(name);
        user.setPasswordHash("synthetic-unused-hash");
        user.setStatus("active");
        context.getBean(AppUserRepository.class).save(user);
        BusinessProfile profile = new BusinessProfile();
        profile.setAppUser(user);
        profile.setBusinessName("Synthetic business");
        profile.setFilingType("blue");
        context.getBean(BusinessProfileRepository.class).save(profile);
        FiscalYear year = new FiscalYear();
        year.setBusinessProfile(profile);
        year.setTaxYear(YEAR);
        year.setPeriodStart(LocalDate.of(YEAR, 1, 1));
        year.setPeriodEnd(LocalDate.of(YEAR, 12, 31));
        year.setStatus("open");
        return years.save(year);
    }

    private JournalEntry seedEntry(FiscalYear year, String summary) {
        AccountingTransaction transaction = new AccountingTransaction();
        transaction.setFiscalYear(year);
        transaction.setTransactionDate(LocalDate.of(YEAR, 3, 1));
        transaction.setTransactionType("income");
        transaction.setSummary(summary);
        transaction.setTotalAmount(new BigDecimal("100"));
        transaction.setStatus("draft");
        transaction.setSettlementStatus("SETTLED");
        context.getBean(AccountingTransactionRepository.class).save(transaction);
        JournalEntry entry = new JournalEntry();
        entry.setFiscalYear(year);
        entry.setAccountingTransaction(transaction);
        entry.setSourceType("ACCOUNTING_TRANSACTION");
        entry.setSourceId(transaction.getId());
        entry.setEntryDate(transaction.getTransactionDate());
        entry.setSummary(summary);
        entry.setStatus("draft");
        entry.addLine(line(1, "D", AccountMasterSeed.ACC_CASH, "Cash", "60"));
        entry.addLine(line(2, "D", AccountMasterSeed.ACC_BANK_DEPOSIT, "Bank", "40"));
        entry.addLine(line(3, "C", AccountMasterSeed.ACC_SALES, "Sales", "100"));
        return journals.save(entry);
    }

    private static JournalLine line(int no, String side, String code, String name, String amount) {
        JournalLine line = new JournalLine();
        line.setLineNo(no);
        line.setDebitCredit(side);
        line.setAccountCode(code);
        line.setAccountName(name);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private void verifyPagingAndOwnership() {
        Set<Long> visible = new HashSet<>();
        for (int p = 0; p < 3; p++) {
            JournalReviewPage page = service.load(userId, YEAR, "all", p);
            require(page.getTotalCount() == 42 && page.getDraftCount() == 42 && page.getTotalPages() == 3,
                    "42 entries must be reachable, not just five preview entries");
            for (var entry : page.getEntries()) {
                require(visible.add(entry.id()), "Duplicate entry across pages");
                require(entry.lines().size() == 3, "Compound journal lines must all be shown");
            }
        }
        require(visible.equals(Set.copyOf(ids)), "Missing or cross-owner entries");
        require(service.load(userId, YEAR, "reviewed", 0).getEntries().isEmpty(), "Unexpected reviewed entry");
        require(service.load(userId, YEAR, "all", 999).getPageNumber() == 2, "Clamp stale page links");
        expect(IllegalArgumentException.class, () -> service.load(userId, YEAR, "unknown", 0));
        expect(IllegalArgumentException.class, () -> service.load(userId, YEAR, "all", -1));
        expect(EntryNotFoundException.class, () -> service.load(userId, 2024, "all", 0));
        var foreign = service.load(otherUserId, YEAR, "all", 0).getEntries().get(0);
        expect(EntryNotFoundException.class, () -> service.confirm(userId, YEAR, otherEntryId, foreign.fingerprint()));
        var own = entry(ids.get(0));
        expect(EntryNotFoundException.class, () -> service.confirm(userId, 2026, own.id(), own.fingerprint()));
        require(context.getBean(TaxReturnService.class).load(userId, YEAR, null).getWarnings().stream()
                .anyMatch(w -> w.contains("未確認の仕訳が42件")), "Warning must count actual pending journal entries");
    }

    private void verifyController() {
        Fixture fixture = new Fixture(context.getBean(JournalReviewController.class), userId);
        fixture.show();
        String firstToken = fixture.reviewToken;
        fixture.post(ids.get(0), "false");
        require(fixture.status == 409, "Explicit confirmation must be required");
        fixture.show();
        String validCsrf = fixture.csrf;
        fixture.csrf = "invalid";
        expectStatus(HttpStatus.FORBIDDEN, () -> fixture.post(ids.get(0), "true"));
        fixture.csrf = validCsrf;
        fixture.post(otherEntryId, "true");
        require(fixture.status == 409, "Unseen or foreign IDs cannot be confirmed using this page's token");
        fixture.show();
        fixture.extraField = true;
        expectStatus(HttpStatus.BAD_REQUEST, () -> fixture.post(ids.get(0), "true"));
        fixture.extraField = false;
        fixture.show();
        fixture.post(ids.get(0), "true");
        require(fixture.status == 200 && fixture.result.startsWith("redirect:"), "Successful confirm must redirect");
        fixture.post(ids.get(0), "true");
        require(fixture.result.startsWith("redirect:"), "Duplicate submission must be idempotent");
        require(entry(ids.get(0)).reviewed(), "Confirmation must be persistent after the request");
        require(tx.execute(s -> "draft".equals(journals.findById(ids.get(0)).orElseThrow()
                .getAccountingTransaction().getStatus())), "Review must not finalize or lock the underlying transaction");
        for (int i = 0; i < 6; i++) {
            fixture.show();
        }
        fixture.reviewToken = firstToken;
        fixture.post(ids.get(1), "true");
        require(fixture.status == 409, "Evicted server confirmation must be rejected");
        fixture.attributes.remove(AuthSessionAttributes.CURRENT_USER);
        expectStatus(HttpStatus.UNAUTHORIZED, fixture::show);
        fixture.attributes.put(AuthSessionAttributes.CURRENT_USER, new AuthUserContext(otherUserId, "Other", ""));
        fixture.reviewToken = firstToken;
        fixture.post(ids.get(1), "true");
        require(fixture.status == 409, "Confirmation must remain bound to its authenticated owner");
    }

    private void verifyStaleAndInvalid() {
        Long id = ids.get(1);
        var old = entry(id);
        tx.executeWithoutResult(s -> journals.findById(id).orElseThrow().setSummary("Changed after display"));
        expect(EntryConflictException.class, () -> service.confirm(userId, YEAR, id, old.fingerprint()));
        require(!entry(id).reviewed(), "Stale confirmation cannot modify status");
        tx.executeWithoutResult(s -> journals.findById(id).orElseThrow().getLines().get(0).setAmount(new BigDecimal("61")));
        var unbalanced = entry(id);
        require(unbalanced.error() != null, "Unbalanced data needs a visible error");
        expect(EntryConflictException.class, () -> service.confirm(userId, YEAR, id, unbalanced.fingerprint()));
        tx.executeWithoutResult(s -> {
            var e = journals.findById(id).orElseThrow();
            e.getLines().stream().filter(l -> l.getLineNo() == 1).findFirst().orElseThrow().setAmount(new BigDecimal("60"));
            e.getAccountingTransaction().setTotalAmount(new BigDecimal("101"));
        });
        var mismatch = entry(id);
        require(mismatch.error() != null, "Transaction/journal amount mismatch must block confirmation");
        expect(EntryConflictException.class, () -> service.confirm(userId, YEAR, id, mismatch.fingerprint()));
        tx.executeWithoutResult(s -> journals.findById(id).orElseThrow().getAccountingTransaction()
                .setTotalAmount(new BigDecimal("100")));
    }

    private void confirmAll() {
        while (true) {
            var pending = service.load(userId, YEAR, "draft", 0);
            if (pending.getEntries().isEmpty()) {
                break;
            }
            for (var entry : pending.getEntries()) {
                service.confirm(userId, YEAR, entry.id(), entry.fingerprint());
            }
        }
        require(service.load(userId, YEAR, "reviewed", 0).getTotalCount() == 42, "All 42 entries must be reviewed");
        require(service.load(otherUserId, YEAR, "draft", 0).getDraftCount() == 1, "Other owner's state changed");
        require(context.getBean(TaxReturnService.class).load(userId, YEAR, null).getWarnings().stream()
                .noneMatch(w -> w.contains("未確認の仕訳")), "Clearing review must remove the tax-return warning");
    }

    private void verifyConcurrentConfirmation() throws Exception {
        var target = entry(ids.get(2));
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Runnable confirm = () -> {
                try {
                    require(start.await(5, TimeUnit.SECONDS), "Concurrent confirmation did not start");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                service.confirm(userId, YEAR, target.id(), target.fingerprint());
            };
            var first = executor.submit(confirm);
            var second = executor.submit(confirm);
            start.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            require(entry(target.id()).reviewed(), "Concurrent confirmation did not persist");

            var pending = entry(ids.get(3));
            var waiting = tx.execute(s -> {
                var year = years.findOwnedByIdForUpdate(yearId, userId).orElseThrow();
                year.setStatus("closed");
                year.setClosedAt(LocalDateTime.now());
                years.flush();
                CountDownLatch started = new CountDownLatch(1);
                var future = executor.submit(() -> {
                    started.countDown();
                    service.confirm(userId, YEAR, pending.id(), pending.fingerprint());
                });
                try {
                    require(started.await(5, TimeUnit.SECONDS), "Confirmation worker did not start");
                    future.get(150, TimeUnit.MILLISECONDS);
                    throw new AssertionError("Confirmation bypassed the fiscal-year lock");
                } catch (TimeoutException expected) {
                    return future;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                } catch (ExecutionException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            try {
                waiting.get(5, TimeUnit.SECONDS);
                throw new AssertionError("Confirmation was accepted after a concurrent close");
            } catch (ExecutionException ex) {
                require(ex.getCause() instanceof EntryConflictException, "Wrong concurrent-close failure");
            }
            require(!entry(pending.id()).reviewed(), "Concurrent close must prevent confirmation");
            tx.executeWithoutResult(s -> {
                var year = years.findById(yearId).orElseThrow();
                year.setStatus("open");
                year.setClosedAt(null);
            });
        }
    }

    private void verifyEditedEntry() {
        EntryService entries = context.getBean(EntryService.class);
        entries.loadPage(EntryKind.EXPENSE, userId, null);
        EntryForm form = new EntryForm();
        form.setExpenseType("expense");
        form.setCategory("1");
        form.setDate("2025-03-02");
        form.setAmount("500");
        form.setPaymentMethodId("1");
        form.setDetails("Synthetic expense");
        var prepared = entries.prepareSubmission(EntryKind.EXPENSE, userId, form);
        require(prepared.getValidatedEntry() != null, "Create form was rejected");
        entries.save(prepared.getValidatedEntry());
        Long expenseId = context.getBean(ExpenseEntryRepository.class).findAll().get(0).getId();
        var pending = service.load(userId, YEAR, "draft", 0).getEntries().get(0);
        service.confirm(userId, YEAR, pending.id(), pending.fingerprint());
        var edit = entries.loadPage(EntryKind.EXPENSE, userId, expenseId).getEntryForm();
        edit.setAmount("600");
        var changed = entries.prepareSubmission(EntryKind.EXPENSE, userId, edit);
        require(changed.getValidatedEntry() != null, "Reviewed entry must remain editable");
        entries.save(changed.getValidatedEntry());
        require(!entry(pending.id()).reviewed(), "Editing reviewed expense must reset its review");
        expect(EntryConflictException.class, () -> service.confirm(userId, YEAR, pending.id(), pending.fingerprint()));
    }

    private void verifyBulkConfirmation() {
        var first = entry(ids.get(5));
        var stale = entry(ids.get(6));
        tx.executeWithoutResult(s -> journals.findById(stale.id()).orElseThrow().setSummary("Changed before bulk"));
        Map<Long, String> mixed = new HashMap<>();
        mixed.put(first.id(), first.fingerprint());
        mixed.put(stale.id(), stale.fingerprint());
        expect(EntryConflictException.class, () -> service.confirmAll(userId, YEAR, mixed));
        require(!entry(first.id()).reviewed(), "Bulk confirmation must be all-or-nothing");

        Fixture fixture = new Fixture(context.getBean(JournalReviewController.class), userId);
        fixture.show();
        fixture.postBulk(List.of(ids.get(3), ids.get(4)), "false");
        require(fixture.status == 409, "Bulk confirmation requires explicit attestation");
        fixture.show();
        fixture.postBulk(List.of(ids.get(3), otherEntryId), "true");
        require(fixture.status == 409 && !entry(ids.get(3)).reviewed(),
                "Bulk confirmation must reject IDs not shown with the token and change nothing");
        fixture.show();
        fixture.postBulk(List.of(), "true");
        require(fixture.status == 409, "Empty selection must be rejected");
        fixture.show();
        List<Long> tooMany = new ArrayList<>();
        for (long i = 1; i <= 21; i++) {
            tooMany.add(i);
        }
        expectStatus(HttpStatus.BAD_REQUEST, () -> fixture.postBulk(tooMany, "true"));
        fixture.show();
        fixture.postBulk(List.of(ids.get(3), ids.get(4), first.id()), "true");
        require(fixture.status == 200 && fixture.result.startsWith("redirect:"), "Bulk confirm must redirect");
        require(entry(ids.get(3)).reviewed() && entry(ids.get(4)).reviewed() && entry(first.id()).reviewed(),
                "Selected entries must all be reviewed");
        require(!entry(stale.id()).reviewed(), "Unselected entries must stay unreviewed");
    }

    private void verifyMissingJournalGeneration() {
        EntryService entries = context.getBean(EntryService.class);
        var expense = context.getBean(ExpenseEntryRepository.class).findAll().get(0);
        Long txId = tx.execute(s -> {
            Long id = context.getBean(ExpenseEntryRepository.class).findById(expense.getId()).orElseThrow()
                    .getAccountingTransaction().getId();
            journals.delete(journals.findByAccountingTransactionId(id).orElseThrow());
            return id;
        });
        var listed = service.load(userId, YEAR, "all", 0).getMissingTransactions().stream()
                .filter(m -> m.transactionId().equals(txId)).findFirst().orElseThrow();
        require(listed.isGeneratable() && "expense".equals(listed.kind()) && expense.getId().equals(listed.entryId()),
                "Missing journal must identify its editable source");
        expect(EntryNotFoundException.class, () -> entries.generateMissingJournal(otherUserId, YEAR, txId));
        Fixture fixture = new Fixture(context.getBean(JournalReviewController.class), userId);
        fixture.show();
        fixture.generate(txId);
        require(fixture.result.startsWith("redirect:"), "Generation must redirect");
        var generated = tx.execute(s -> journals.findByAccountingTransactionId(txId).orElseThrow());
        require("draft".equals(generated.getStatus()), "Generated journal must need review");
        require(service.load(userId, YEAR, "all", 0).getMissingTransactions().stream()
                .noneMatch(m -> m.transactionId().equals(txId)), "Generated transaction must leave the missing list");
        expect(EntryConflictException.class, () -> entries.generateMissingJournal(userId, YEAR, txId));
        var created = entry(generated.getId());
        service.confirm(userId, YEAR, created.id(), created.fingerprint());
    }

    private void verifySettlementDateCorrection() {
        CardSettlementService settlements = context.getBean(CardSettlementService.class);
        Long settlementTxId = tx.execute(s -> {
            FiscalYear year = years.findById(yearId).orElseThrow();
            AccountingTransaction transfer = new AccountingTransaction();
            transfer.setFiscalYear(year);
            transfer.setTransactionDate(LocalDate.of(YEAR, 4, 27));
            transfer.setTransactionType("TRANSFER");
            transfer.setSummary("Synthetic card settlement");
            transfer.setTotalAmount(new BigDecimal("100"));
            transfer.setStatus("draft");
            transfer.setSettlementStatus("SETTLED");
            context.getBean(AccountingTransactionRepository.class).save(transfer);
            JournalEntry entry = new JournalEntry();
            entry.setFiscalYear(year);
            entry.setAccountingTransaction(transfer);
            entry.setSourceType("CARD_SETTLEMENT");
            entry.setSourceId(transfer.getId());
            entry.setEntryDate(transfer.getTransactionDate());
            entry.setSummary("Synthetic card settlement");
            entry.setStatus("reviewed");
            entry.addLine(line(1, "D", AccountMasterSeed.ACC_CASH, "Payable", "100"));
            entry.addLine(line(2, "C", AccountMasterSeed.ACC_BANK_DEPOSIT, "Bank", "100"));
            journals.save(entry);
            return transfer.getId();
        });
        var registered = settlements.load(userId, YEAR).getRegistered();
        require(registered.size() == 1 && registered.get(0).transactionId().equals(settlementTxId)
                && registered.get(0).reviewed(), "Registered settlement must be listed for correction");
        expect(EntryNotFoundException.class,
                () -> settlements.updateSettlementDate(otherUserId, YEAR, settlementTxId, "2025-04-27", "2025-04-28"));
        require(!settlements.updateSettlementDate(userId, YEAR, settlementTxId, "2025-04-27", "2026-01-05").isEmpty(),
                "Out-of-year settlement date must be rejected");
        require(!settlements.updateSettlementDate(userId, YEAR, settlementTxId, "2025-04-26", "2025-04-28").isEmpty(),
                "Stale settlement date must be rejected");
        Fixture fixture = new Fixture(context.getBean(JournalReviewController.class), userId);
        fixture.show();
        fixture.correctDate(settlementTxId, "2025-04-27", "2026-01-05");
        require(fixture.status == 400 && "TaxReturn/journal-review".equals(fixture.result)
                && fixture.model.containsAttribute("settlementErrors")
                && fixture.model.containsAttribute("cardSettlement"),
                "Date errors must render the journal screen with settlement data");
        String validCsrf = fixture.csrf;
        fixture.csrf = "invalid";
        expectStatus(HttpStatus.FORBIDDEN,
                () -> fixture.correctDate(settlementTxId, "2025-04-27", "2025-04-28"));
        fixture.csrf = validCsrf;
        fixture.extraField = true;
        expectStatus(HttpStatus.BAD_REQUEST,
                () -> fixture.correctDate(settlementTxId, "2025-04-27", "2025-04-28"));
        fixture.extraField = false;
        fixture.correctDate(settlementTxId, "2025-04-27", "2025-04-28");
        require(fixture.result.equals("redirect:/tax-return/journal?year=2025&status=reviewed&page=1"
                + "#heading-card-settlement-registered"),
                "Date correction must stay on the journal screen and preserve display conditions");
        tx.executeWithoutResult(s -> {
            JournalEntry journal = journals.findBySourceTypeAndSourceId("CARD_SETTLEMENT", settlementTxId).orElseThrow();
            require(LocalDate.of(YEAR, 4, 28).equals(journal.getEntryDate())
                    && LocalDate.of(YEAR, 4, 28).equals(journal.getAccountingTransaction().getTransactionDate()),
                    "Transaction and journal dates must move together");
            require("draft".equals(journal.getStatus()), "Corrected settlement journal must need review again");
        });
        tx.executeWithoutResult(s -> {
            FiscalYear year = years.findById(yearId).orElseThrow();
            year.setStatus("closed");
            year.setClosedAt(LocalDateTime.now());
        });
        try {
            require(!settlements.updateSettlementDate(userId, YEAR, settlementTxId, "2025-04-28", "2025-04-29").isEmpty(),
                    "Closed year settlement must not be corrected");
        } finally {
            tx.executeWithoutResult(s -> {
                FiscalYear year = years.findById(yearId).orElseThrow();
                year.setStatus("open");
                year.setClosedAt(null);
            });
        }
        var corrected = service.load(userId, YEAR, "draft", 0).getEntries().stream()
                .filter(e -> "CARD_SETTLEMENT".equals(e.sourceType())).findFirst().orElseThrow();
        service.confirm(userId, YEAR, corrected.id(), corrected.fingerprint());
    }

    private void verifySettlementScreen() {
        Long owner = tx.execute(s -> seedYear("settlement-screen").getBusinessProfile().getAppUser().getId());
        EntryService entries = context.getBean(EntryService.class);
        var expensePage = entries.loadPage(EntryKind.EXPENSE, owner, null);
        String methodId = context.getBean(PaymentMethodRepository.class).findAll().stream()
                .filter(p -> "card".equals(p.getMethodCode())).findFirst().orElseThrow().getId().toString();
        EntryForm expense = new EntryForm();
        expense.setExpenseType("expense");
        expense.setCategory(expensePage.getItems().stream().filter(i -> "expense".equals(i.getGroup()))
                .findFirst().orElseThrow().getId());
        expense.setDate("2025-03-02");
        expense.setAmount("500");
        expense.setPaymentMethodId(methodId);
        expense.setDetails("Synthetic card purchase");
        var prepared = entries.prepareSubmission(EntryKind.EXPENSE, owner, expense);
        require(prepared.getValidatedEntry() != null, "Card purchase seed rejected");
        entries.save(prepared.getValidatedEntry());

        Fixture fixture = new Fixture(context.getBean(JournalReviewController.class), owner);
        fixture.show();
        CardSettlementPageData data = (CardSettlementPageData) fixture.model.get("cardSettlement");
        require(data.getCandidates().size() == 1 && data.getRegistered().isEmpty(),
                "Journal screen must load its own settlement candidates");
        CardSettlementForm form = new CardSettlementForm();
        form.setIdempotencyKey(data.getIdempotencyKey());
        form.setSettlementDate("2025-04-27");
        fixture.saveSettlement(form);
        require(fixture.status == 400 && "TaxReturn/journal-review".equals(fixture.result)
                && fixture.model.containsAttribute("settlementErrors"),
                "Registration errors must stay on the journal screen");
        form.setSourceTransactionIds(List.of(data.getCandidates().get(0).getTransactionId().toString()));
        String validCsrf = fixture.csrf;
        fixture.csrf = "invalid";
        expectStatus(HttpStatus.FORBIDDEN, () -> fixture.saveSettlement(form));
        fixture.csrf = validCsrf;
        fixture.extraField = true;
        expectStatus(HttpStatus.BAD_REQUEST, () -> fixture.saveSettlement(form));
        fixture.extraField = false;
        Fixture foreign = new Fixture(context.getBean(JournalReviewController.class), otherUserId);
        foreign.show();
        require(((CardSettlementPageData) foreign.model.get("cardSettlement")).getCandidates().isEmpty(),
                "Journal screen must not show another owner's settlement candidates");
        expect(EntryNotFoundException.class, () -> foreign.saveSettlement(form));
        fixture.saveSettlement(form);
        require(fixture.result.equals("redirect:/tax-return/journal?year=2025&status=all&page=0"
                + "#section-card-settlement"), "Registration must return to the journal screen");
        fixture.saveSettlement(form);
        fixture.show();
        data = (CardSettlementPageData) fixture.model.get("cardSettlement");
        require(data.getCandidates().isEmpty() && data.getRegistered().size() == 1,
                "Registration must persist exactly once and refresh the journal screen");
        Long settlementId = data.getRegistered().get(0).transactionId();
        expect(EntryNotFoundException.class,
                () -> foreign.correctDate(settlementId, "2025-04-27", "2025-04-28"));
        tx.executeWithoutResult(s -> years.findOwnedByTaxYearForUpdate(YEAR, owner).orElseThrow().setStatus("closed"));
        fixture.correctDate(settlementId, "2025-04-27", "2025-04-28");
        require(fixture.status == 400 && "TaxReturn/journal-review".equals(fixture.result)
                && ((JournalReviewPage) fixture.model.get("review")).isClosed(),
                "Closed year rejection must stay on the read-only journal screen");
        fixture.saveSettlement(form);
        require(fixture.status == 400, "Closed year registration must remain blocked");
        fixture.attributes.remove(AuthSessionAttributes.CURRENT_USER);
        expectStatus(HttpStatus.UNAUTHORIZED, () -> fixture.saveSettlement(form));
    }

    private void verifyGeneratedEntry() {
        Long consumptionId = tx.execute(s -> {
            HouseholdConsumption consumption = new HouseholdConsumption();
            consumption.setFiscalYear(years.findById(yearId).orElseThrow());
            consumption.setTransactionDate(LocalDate.of(YEAR, 3, 3));
            consumption.setDetails("Synthetic household consumption");
            consumption.setAmount(new BigDecimal("1000"));
            return context.getBean(HouseholdConsumptionRepository.class).save(consumption).getId();
        });
        ClosingJournalService closing = context.getBean(ClosingJournalService.class);
        closing.rebuild(yearId);
        var generated = service.load(userId, YEAR, "draft", 0).getEntries().stream()
                .filter(e -> e.summary().contains("Synthetic household")).findFirst().orElseThrow();
        service.confirm(userId, YEAR, generated.id(), generated.fingerprint());
        closing.rebuild(yearId);
        var unchanged = entry(generated.id());
        require(unchanged.reviewed() && unchanged.fingerprint().equals(generated.fingerprint()),
                "Unchanged automatic journals must retain confirmation through rebuild/startup");
        tx.executeWithoutResult(s -> context.getBean(HouseholdConsumptionRepository.class)
                .findById(consumptionId).orElseThrow().setAmount(new BigDecimal("2000")));
        closing.rebuild(yearId);
        require(!entry(generated.id()).reviewed(), "Changed automatic journal must require another review");
        expect(EntryConflictException.class,
                () -> service.confirm(userId, YEAR, generated.id(), generated.fingerprint()));
    }

    private void verifyClosedYear() {
        var pending = service.load(userId, YEAR, "draft", 0).getEntries().get(0);
        tx.executeWithoutResult(s -> {
            FiscalYear year = years.findById(yearId).orElseThrow();
            year.setStatus("closed");
            year.setClosedAt(LocalDateTime.now());
        });
        require(service.load(userId, YEAR, "all", 0).isClosed(), "Closed year should be read-only");
        expect(EntryConflictException.class, () -> service.confirm(userId, YEAR, pending.id(), pending.fingerprint()));
        require(!entry(pending.id()).reviewed(), "Closed year confirmation changed data");
        tx.executeWithoutResult(s -> {
            FiscalYear year = years.findById(yearId).orElseThrow();
            year.setStatus("open");
            year.setClosedAt(null);
        });
        service.confirm(userId, YEAR, pending.id(), pending.fingerprint());
        tx.executeWithoutResult(s -> {
            AccountingTransaction missing = new AccountingTransaction();
            missing.setFiscalYear(years.findById(yearId).orElseThrow());
            missing.setTransactionDate(LocalDate.of(YEAR, 3, 4));
            missing.setTransactionType("income");
            missing.setSummary("Synthetic missing journal");
            missing.setTotalAmount(new BigDecimal("100"));
            missing.setStatus("draft");
            missing.setSettlementStatus("SETTLED");
            context.getBean(AccountingTransactionRepository.class).save(missing);
        });
        require(service.load(userId, YEAR, "all", 0).getMissingCount() == 1, "Missing journals must not hide pending data");
        var orphan = service.load(userId, YEAR, "all", 0).getMissingTransactions().get(0);
        require(!orphan.isGeneratable(), "Transactions without source data must not offer generation");
        expect(EntryConflictException.class,
                () -> context.getBean(EntryService.class).generateMissingJournal(userId, YEAR, orphan.transactionId()));
    }

    private JournalReviewPage.Entry entry(Long id) {
        for (int page = 0; ; page++) {
            var data = service.load(userId, YEAR, "all", page);
            for (var entry : data.getEntries()) {
                if (entry.id().equals(id)) {
                    return entry;
                }
            }
            if (page + 1 >= data.getTotalPages()) {
                throw new AssertionError("Entry not visible: " + id);
            }
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            if (type.isInstance(ex)) {
                return;
            }
            throw ex;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void expectStatus(HttpStatus status, Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException ex) {
            require(ex.getStatusCode().equals(status), "Unexpected HTTP status: " + ex.getStatusCode());
            return;
        }
        throw new AssertionError("Expected HTTP " + status);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Fixture {
        private final JournalReviewController controller;
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private Map<String, String[]> parameters = new HashMap<>();
        private final ExtendedModelMap model = new ExtendedModelMap();
        private String csrf;
        private String reviewToken;
        private String result;
        private int status = 200;
        private boolean extraField;
        private final HttpSession session = proxy(HttpSession.class, (o, m, a) -> switch (m.getName()) {
            case "getAttribute" -> attributes.get((String) a[0]);
            case "setAttribute" -> { attributes.put((String) a[0], a[1]); yield null; }
            default -> throw new UnsupportedOperationException(m.getName());
        });
        private final HttpServletRequest request = proxy(HttpServletRequest.class, (o, m, a) -> switch (m.getName()) {
            case "getSession" -> session;
            case "getParameterMap" -> parameters;
            default -> throw new UnsupportedOperationException(m.getName());
        });
        private final HttpServletResponse response = proxy(HttpServletResponse.class, (o, m, a) -> switch (m.getName()) {
            case "setStatus" -> { status = (Integer) a[0]; yield null; }
            case "setHeader" -> { headers.put((String) a[0], (String) a[1]); yield null; }
            case "setDateHeader" -> null;
            default -> throw new UnsupportedOperationException(m.getName());
        });

        private Fixture(JournalReviewController controller, Long userId) {
            this.controller = controller;
            attributes.put(AuthSessionAttributes.CURRENT_USER, new AuthUserContext(userId, "Synthetic user", ""));
        }

        private void show() {
            status = 200;
            model.clear();
            require("TaxReturn/journal-review".equals(controller.index(YEAR, "all", 0, request, response, model)),
                    "Wrong review view");
            csrf = (String) model.get("csrfToken");
            reviewToken = (String) model.get("reviewToken");
            require(headers.get("Cache-Control").contains("no-store"), "Confidential page must not be cached");
            require(model.containsAttribute("cardSettlement"), "Settlement operations must be on the journal screen");
        }

        private void post(Long id, String confirmed) {
            status = 200;
            parameters = new HashMap<>();
            parameters.put("id", new String[] {id.toString()});
            if (extraField) {
                parameters.put("userId", new String[] {"2"});
            }
            result = controller.confirm(YEAR, id, "all", 0, csrf, reviewToken, confirmed, request, response,
                    model, new RedirectAttributesModelMap());
        }

        private void postBulk(List<Long> selected, String confirmed) {
            status = 200;
            parameters = new HashMap<>();
            parameters.put("ids", selected.stream().map(String::valueOf).toArray(String[]::new));
            result = controller.confirmBulk(YEAR, selected, "all", 0, csrf, reviewToken, confirmed, request, response,
                    model, new RedirectAttributesModelMap());
        }

        private void generate(Long transactionId) {
            status = 200;
            parameters = new HashMap<>();
            parameters.put("transactionId", new String[] {transactionId.toString()});
            result = controller.generate(YEAR, transactionId, "all", 0, csrf, request, response, model,
                    new RedirectAttributesModelMap());
        }

        private void saveSettlement(CardSettlementForm form) {
            status = 200;
            parameters = new HashMap<>();
            if (extraField) {
                parameters.put("userId", new String[] {"2"});
            }
            form.setCsrfToken(csrf);
            result = controller.saveCardSettlement(YEAR, "all", 0, form, request, response, model,
                    new RedirectAttributesModelMap());
        }

        private void correctDate(Long id, String current, String updated) {
            status = 200;
            parameters = new HashMap<>();
            if (extraField) {
                parameters.put("userId", new String[] {"2"});
            }
            result = controller.updateCardSettlementDate(YEAR, "reviewed", 1, id, current, updated, csrf,
                    request, response, model, new RedirectAttributesModelMap());
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
