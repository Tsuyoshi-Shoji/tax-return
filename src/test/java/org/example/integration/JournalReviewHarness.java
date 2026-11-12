package org.example.integration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

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
        String url = "jdbc:h2:mem:journal-review;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
        ProductionBootstrapHarness.applyMigrations(url, 1, 17);
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
            confirmAll();
            verifyEditedEntry();
            verifyGeneratedEntry();
            verifyClosedYear();
        }
        System.out.println("Journal review harness passed");
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
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
