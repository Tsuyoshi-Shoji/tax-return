package org.example.features.entry.session;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpSession;
import org.example.features.entry.EntryForbiddenException;
import org.example.features.entry.EntryKind;
import org.example.features.entry.session.EntrySessionTokenManager.PreparedTokens;
import org.example.features.entry.session.EntrySessionTokenManager.SubmissionDecision;

public final class EntrySessionTokenManagerHarness {
    private static final Long USER_ID = 1L;

    public static void main(String[] args) throws Exception {
        for (EntryKind kind : EntryKind.values()) {
            verifyPendingConfirmation(kind);
            verifyChangedHighPayload(kind);
            verifyChangedLowPayload(kind);
            verifyEditAndVersionChanges(kind);
            verifyCompletedReplay(kind);
            verifyScopeAndCsrf(kind);
            verifyExpiration(kind, false);
            verifyExpiration(kind, true);
        }
        verifyConcurrentInitialization();
        System.out.println("Entry session token manager harness passed (17 scenarios)");
    }

    private static void verifyPendingConfirmation(EntryKind kind) {
        Fixture f = new Fixture(kind);
        String first = confirmation(f.evaluate("high-a", true, null));
        require(first.equals(confirmation(f.evaluate("high-a", true, ""))), "missing confirmation changed token");
        require(first.equals(confirmation(f.evaluate("high-a", true, "invalid"))), "invalid confirmation bypassed check");
        proceed(f.evaluate("high-a", true, first));
        Fixture otherForm = new Fixture(f.manager, f.session, kind);
        String other = confirmation(otherForm.evaluate("high-a", true, first));
        require(!first.equals(other), "confirmation crossed submission boundary");
        proceed(otherForm.evaluate("high-a", true, other));
    }

    private static void verifyChangedHighPayload(EntryKind kind) {
        Fixture f = new Fixture(kind);
        String old = confirmation(f.evaluate("high-a", true, null));
        String fresh = confirmation(f.evaluate("high-b", true, old));
        require(!old.equals(fresh), "changed high payload reused confirmation");
        require(fresh.equals(confirmation(f.evaluate("high-b", true, old))), "stale confirmation bypassed check");
        proceed(f.evaluate("high-b", true, fresh));
        // Returning to an earlier payload must not revive its old confirmation.
        String returned = confirmation(f.evaluate("high-a", true, old));
        require(!returned.equals(old) && !returned.equals(fresh), "old payload revived confirmation");
        proceed(f.evaluate("high-a", true, returned));
    }

    private static void verifyChangedLowPayload(EntryKind kind) {
        Fixture f = new Fixture(kind);
        String old = confirmation(f.evaluate("high-a", true, null));
        proceed(f.evaluate("low-b", false, old));
        proceed(f.evaluate("low-c", false, null));
        String fresh = confirmation(f.evaluate("high-a", true, old));
        require(!old.equals(fresh), "low payload failed to invalidate high confirmation");
        proceed(f.evaluate("high-a", true, fresh));

        Fixture savedLow = new Fixture(kind);
        String high = confirmation(savedLow.evaluate("high-a", true, null));
        proceed(savedLow.evaluate("low-b", false, high));
        savedLow.complete("raw-low-b", "low-b");
        replay(savedLow.evaluate("low-b", false, high), kind);
        forbidden(() -> savedLow.evaluate("high-a", true, high));
    }

    private static void verifyEditAndVersionChanges(EntryKind kind) throws Exception {
        Fixture f = new Fixture(kind);
        // The manager receives only an opaque canonical hash; the service includes editId/version
        // in that hash and separately authorizes the target before evaluating the submission.
        String original = editPayload(kind, 10L, 0L);
        String otherEdit = editPayload(kind, 11L, 0L);
        String newerVersion = editPayload(kind, 11L, 1L);
        String old = confirmation(f.evaluate(original, true, null));
        String fresh = confirmation(f.evaluate(otherEdit, true, old));
        require(!old.equals(fresh), "confirmation accepted a different edit target");
        String versionConfirmation = confirmation(f.evaluate(newerVersion, true, fresh));
        require(!fresh.equals(versionConfirmation), "confirmation accepted a different edit version");
        proceed(f.evaluate(newerVersion, true, versionConfirmation));
        f.complete("raw-edit", newerVersion);
        forbidden(() -> f.evaluate(original, true, old));
        forbidden(() -> f.evaluate(otherEdit, true, fresh));
        replay(f.evaluate(newerVersion, true, versionConfirmation), kind);
    }

    private static void verifyCompletedReplay(EntryKind kind) {
        Fixture f = new Fixture(kind);
        proceed(f.manager.checkCompletedReplay(f.session, kind, USER_ID, f.token(), "raw-original"));
        String old = confirmation(f.evaluate("high-original", true, null));
        String fresh = confirmation(f.evaluate("high-edited", true, old));
        proceed(f.evaluate("high-edited", true, fresh));
        forbidden(() -> f.complete("raw-original", "high-original"));
        f.complete("raw-edited", "high-edited");
        replay(f.manager.checkCompletedReplay(f.session, kind, USER_ID, f.token(), "raw-edited"), kind);
        replay(f.evaluate("high-edited", true, fresh), kind);
        forbidden(() -> f.manager.checkCompletedReplay(f.session, kind, USER_ID, f.token(), "raw-original"));
        forbidden(() -> f.evaluate("high-original", true, old));
        forbidden(() -> f.evaluate("low-new", false, null));
        replay(f.manager.checkCompletedReplay(f.session, kind, USER_ID, f.token(), "raw-edited"), kind);
    }

    private static void verifyScopeAndCsrf(EntryKind kind) {
        Fixture f = new Fixture(kind);
        EntryKind other = kind == EntryKind.INCOME ? EntryKind.EXPENSE : EntryKind.INCOME;
        f.manager.validateCsrf(f.session, kind, USER_ID, f.tokens.getCsrfToken());
        f.manager.validateSubmissionToken(f.session, kind, USER_ID, f.token());
        forbidden(() -> f.manager.validateCsrf(f.session, kind, USER_ID, null));
        forbidden(() -> f.manager.validateCsrf(f.session, kind, USER_ID, ""));
        forbidden(() -> f.manager.validateCsrf(f.session, kind, USER_ID, "invalid"));
        f.manager.issueTokens(f.session, other, USER_ID);
        forbidden(() -> f.manager.validateCsrf(f.session, other, USER_ID, f.tokens.getCsrfToken()));
        HttpSession otherSession = session(null, null);
        forbidden(() -> f.manager.validateCsrf(otherSession, kind, USER_ID, f.tokens.getCsrfToken()));
        forbidden(() -> f.manager.validateSubmissionToken(otherSession, kind, USER_ID, f.token()));
        forbidden(() -> f.manager.validateSubmissionToken(f.session, other, USER_ID, f.token()));
        forbidden(() -> f.manager.validateSubmissionToken(f.session, kind, 2L, f.token()));
        forbidden(() -> f.manager.validateSubmissionToken(f.session, kind, null, f.token()));
        forbidden(() -> f.manager.validateSubmissionToken(f.session, kind, USER_ID, "unknown"));
        forbidden(() -> f.manager.evaluateSubmission(f.session, other, USER_ID, f.token(), "a", true, null));
        forbidden(() -> f.manager.evaluateSubmission(f.session, kind, 2L, f.token(), "a", true, null));
        forbidden(() -> f.manager.evaluateSubmission(f.session, kind, USER_ID, null, "a", true, null));
        proceed(f.evaluate("a", false, null));
        forbidden(() -> f.manager.markCompleted(f.session, other, USER_ID, f.token(), "raw", "a", kind.getPath(), "saved"));
        forbidden(() -> f.manager.markCompleted(f.session, kind, 2L, f.token(), "raw", "a", kind.getPath(), "saved"));
        f.complete("raw", "a");
        forbidden(() -> f.manager.evaluateSubmission(f.session, other, USER_ID, f.token(), "a", false, null));
        forbidden(() -> f.manager.evaluateSubmission(f.session, kind, 2L, f.token(), "a", false, null));
        // The controller always validates scope before the early replay lookup.
        require(!f.manager.checkCompletedReplay(f.session, kind, 2L, f.token(), "raw").isReplay(),
                "replay metadata leaked across users");
        require(!f.manager.checkCompletedReplay(f.session, other, USER_ID, f.token(), "raw").isReplay(),
                "replay metadata leaked across kinds");
    }

    private static void verifyExpiration(EntryKind kind, boolean completed) throws Exception {
        Fixture f = new Fixture(kind);
        String token = confirmation(f.evaluate("high", true, null));
        if (completed) {
            proceed(f.evaluate("high", true, token));
            f.complete("raw", "high");
        }
        Object state = f.manager.getSessionMonitor(f.session);
        synchronized (state) {
            Field submissions = state.getClass().getDeclaredField("submissions");
            submissions.setAccessible(true);
            Object submission = ((Map<?, ?>) submissions.get(state)).get(f.token());
            Field updatedAt = submission.getClass().getDeclaredField("updatedAt");
            updatedAt.setAccessible(true);
            updatedAt.set(submission, Instant.now().minusSeconds(31 * 60));
        }
        forbidden(() -> f.evaluate("high", true, token));
        forbidden(() -> f.manager.validateSubmissionToken(f.session, kind, USER_ID, f.token()));
        require(!f.manager.checkCompletedReplay(f.session, kind, USER_ID, f.token(), "raw").isReplay(),
                "expired completed token replayed");
    }

    private static void verifyConcurrentInitialization() throws Exception {
        EntrySessionTokenManager manager = new EntrySessionTokenManager();
        AtomicInteger stateWrites = new AtomicInteger();
        HttpSession session = session(new CountDownLatch(2), stateWrites);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<PreparedTokens> first = pool.submit(() -> manager.issueTokens(session, EntryKind.INCOME, USER_ID));
            Future<PreparedTokens> second = pool.submit(() -> manager.issueTokens(session, EntryKind.INCOME, USER_ID));
            PreparedTokens a = first.get(10, TimeUnit.SECONDS);
            PreparedTokens b = second.get(10, TimeUnit.SECONDS);
            require(stateWrites.get() == 1, "session state initialized more than once");
            require(a.getCsrfToken().equals(b.getCsrfToken()), "concurrent initialization lost CSRF state");
            require(!a.getSubmissionToken().equals(b.getSubmissionToken()), "submission tokens are not unique");
            manager.validateSubmissionToken(session, EntryKind.INCOME, USER_ID, a.getSubmissionToken());
            manager.validateSubmissionToken(session, EntryKind.INCOME, USER_ID, b.getSubmissionToken());
            Object monitor = manager.getSessionMonitor(session);
            synchronized (monitor) {
                require(manager.getSessionMonitor(session) == monitor, "session monitor changed");
                proceed(manager.evaluateSubmission(session, EntryKind.INCOME, USER_ID, a.getSubmissionToken(), "a", false, null));
                manager.markCompleted(session, EntryKind.INCOME, USER_ID, a.getSubmissionToken(), "raw", "a", "/income", "saved");
            }
            replay(manager.checkCompletedReplay(session, EntryKind.INCOME, USER_ID, a.getSubmissionToken(), "raw"), EntryKind.INCOME);
        } finally {
            pool.shutdownNow();
            require(pool.awaitTermination(10, TimeUnit.SECONDS), "concurrent harness workers did not stop");
        }
    }

    private static HttpSession session(CountDownLatch firstReads, AtomicInteger stateWrites) {
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        AtomicInteger reads = new AtomicInteger();
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(), new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAttribute":
                            Object value = attributes.get((String) args[0]);
                            // Both initial callers observe an empty session before either can publish state.
                            if (firstReads != null && reads.incrementAndGet() <= 2) {
                                firstReads.countDown();
                                require(firstReads.await(5, TimeUnit.SECONDS), "initial session reads did not overlap");
                            }
                            return value;
                        case "setAttribute":
                            attributes.put((String) args[0], args[1]);
                            if (stateWrites != null) {
                                stateWrites.incrementAndGet();
                            }
                            return null;
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "toString":
                            return "SyntheticHttpSession";
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static String editPayload(EntryKind kind, Long editId, Long version) throws Exception {
        String payload = kind.getCode() + "|" + USER_ID + "|" + editId + "|" + version + "|synthetic-high-entry";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static String confirmation(SubmissionDecision decision) {
        require(decision.isConfirmationRequired() && !decision.isProceed() && !decision.isReplay(),
                "expected confirmation, not proceed/replay");
        require(decision.getConfirmationToken() != null && !decision.getConfirmationToken().isBlank(),
                "confirmation token missing");
        return decision.getConfirmationToken();
    }

    private static void proceed(SubmissionDecision decision) {
        require(decision.isProceed() && !decision.isConfirmationRequired() && !decision.isReplay(), "expected proceed");
    }

    private static void replay(SubmissionDecision decision, EntryKind kind) {
        require(decision.isReplay() && !decision.isProceed() && !decision.isConfirmationRequired(), "expected replay");
        require(kind.getPath().equals(decision.getRedirectPath()), "replay redirect changed");
        require("saved".equals(decision.getSuccessMessage()), "replay message changed");
    }

    private static void forbidden(Runnable operation) {
        try {
            operation.run();
        } catch (EntryForbiddenException expected) {
            return;
        }
        throw new AssertionError("expected EntryForbiddenException");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Fixture {
        private final EntrySessionTokenManager manager;
        private final HttpSession session;
        private final EntryKind kind;
        private final PreparedTokens tokens;

        private Fixture(EntryKind kind) {
            this(new EntrySessionTokenManager(), session(null, null), kind);
        }

        private Fixture(EntrySessionTokenManager manager, HttpSession session, EntryKind kind) {
            this.manager = manager;
            this.session = session;
            this.kind = kind;
            this.tokens = manager.issueTokens(session, kind, USER_ID);
        }

        private String token() {
            return tokens.getSubmissionToken();
        }

        private SubmissionDecision evaluate(String payload, boolean high, String confirmation) {
            return manager.evaluateSubmission(session, kind, USER_ID, token(), payload, high, confirmation);
        }

        private void complete(String raw, String payload) {
            manager.markCompleted(session, kind, USER_ID, token(), raw, payload, kind.getPath(), "saved");
        }
    }
}
