package org.example.features.entry.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.features.entry.EntryForbiddenException;
import org.example.features.entry.EntryKind;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;

@Component
public class EntrySessionTokenManager {
    private static final String SESSION_KEY = EntrySessionTokenManager.class.getName() + ".STATE";
    private static final Duration FORM_TTL = Duration.ofMinutes(30);
    private static final int MAX_FORMS = 20;

    private final SecureRandom secureRandom = new SecureRandom();

    public PreparedTokens issueTokens(HttpSession session, EntryKind kind, Long appUserId) {
        SessionState state = getState(session);
        synchronized (state) {
            prune(state);
            String csrfToken = state.csrfTokens.computeIfAbsent(kind.getCode(), key -> randomToken());
            String submissionToken = randomToken();
            SubmissionState submissionState = new SubmissionState();
            submissionState.kind = kind.getCode();
            submissionState.appUserId = appUserId;
            submissionState.createdAt = Instant.now();
            submissionState.updatedAt = submissionState.createdAt;
            state.submissions.put(submissionToken, submissionState);
            trim(state);
            return new PreparedTokens(csrfToken, submissionToken);
        }
    }

    public Object getSessionMonitor(HttpSession session) {
        return getState(session);
    }

    public void validateCsrf(HttpSession session, EntryKind kind, Long appUserId, String providedToken) {
        SessionState state = getState(session);
        synchronized (state) {
            prune(state);
            String expected = state.csrfTokens.get(kind.getCode());
            if (expected == null || !equalsConstantTime(expected, providedToken)) {
                throw new EntryForbiddenException("フォームの送信情報が無効です。画面を開き直してください。");
            }
        }
    }

    public void validateSubmissionToken(HttpSession session, EntryKind kind, Long appUserId, String submissionToken) {
        SessionState state = getState(session);
        synchronized (state) {
            prune(state);
            SubmissionState submissionState = state.submissions.get(submissionToken);
            if (submissionState == null
                    || submissionState.appUserId == null
                    || !submissionState.appUserId.equals(appUserId)
                    || !kind.getCode().equals(submissionState.kind)) {
                throw new EntryForbiddenException("フォームの有効期限が切れたか、送信情報が不正です。画面を開き直してください。");
            }
        }
    }

    public SubmissionDecision evaluateSubmission(
            HttpSession session,
            EntryKind kind,
            Long appUserId,
            String submissionToken,
            String payloadHash,
            boolean confirmationRequired,
            String confirmationToken) {
        SessionState state = getState(session);
        synchronized (state) {
            prune(state);
            SubmissionState submissionState = state.submissions.get(submissionToken);
            if (submissionState == null
                    || submissionState.appUserId == null
                    || !submissionState.appUserId.equals(appUserId)
                    || !kind.getCode().equals(submissionState.kind)) {
                throw new EntryForbiddenException("フォームの有効期限が切れたか、送信情報が不正です。画面を開き直してください。");
            }
            submissionState.updatedAt = Instant.now();
            if (submissionState.completed) {
                if (equalsConstantTime(submissionState.payloadHash, payloadHash)) {
                    return SubmissionDecision.replay(submissionState.redirectPath, submissionState.successMessage);
                }
                throw new EntryForbiddenException("同じ送信トークンで内容を変更できません。画面を開き直してください。");
            }
            if (submissionState.payloadHash != null && !equalsConstantTime(submissionState.payloadHash, payloadHash)) {
                submissionState.confirmationToken = null;
                submissionState.confirmationPayloadHash = null;
            }
            submissionState.payloadHash = payloadHash;
            if (confirmationRequired) {
                if (submissionState.confirmationToken == null
                        || !equalsConstantTime(submissionState.confirmationPayloadHash, payloadHash)) {
                    submissionState.confirmationToken = randomToken();
                    submissionState.confirmationPayloadHash = payloadHash;
                    return SubmissionDecision.confirmation(submissionState.confirmationToken);
                }
                if (!equalsConstantTime(submissionState.confirmationToken, confirmationToken)) {
                    return SubmissionDecision.confirmation(submissionState.confirmationToken);
                }
            }
            return SubmissionDecision.proceed();
        }
    }

    public SubmissionDecision checkCompletedReplay(
            HttpSession session,
            EntryKind kind,
            Long appUserId,
            String submissionToken,
            String rawFormHash) {
        SessionState state = getState(session);
        synchronized (state) {
            prune(state);
            SubmissionState submissionState = state.submissions.get(submissionToken);
            if (submissionState == null
                    || submissionState.appUserId == null
                    || !submissionState.appUserId.equals(appUserId)
                    || !kind.getCode().equals(submissionState.kind)
                    || !submissionState.completed) {
                return SubmissionDecision.proceed();
            }
            if (equalsConstantTime(submissionState.rawFormHash, rawFormHash)) {
                return SubmissionDecision.replay(submissionState.redirectPath, submissionState.successMessage);
            }
            throw new EntryForbiddenException("同じ送信トークンで内容を変更できません。画面を開き直してください。");
        }
    }

    public void markCompleted(
            HttpSession session,
            EntryKind kind,
            Long appUserId,
            String submissionToken,
            String rawFormHash,
            String payloadHash,
            String redirectPath,
            String successMessage) {
        SessionState state = getState(session);
        synchronized (state) {
            SubmissionState submissionState = state.submissions.get(submissionToken);
            if (submissionState == null
                    || !kind.getCode().equals(submissionState.kind)
                    || !appUserId.equals(submissionState.appUserId)
                    || !equalsConstantTime(submissionState.payloadHash, payloadHash)) {
                throw new EntryForbiddenException("フォームの送信状態が無効です。画面を開き直してください。");
            }
            submissionState.completed = true;
            submissionState.rawFormHash = rawFormHash;
            submissionState.redirectPath = redirectPath;
            submissionState.successMessage = successMessage;
            submissionState.updatedAt = Instant.now();
        }
    }

    private SessionState getState(HttpSession session) {
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof SessionState state) {
            return state;
        }
        synchronized (session) {
            existing = session.getAttribute(SESSION_KEY);
            if (existing instanceof SessionState state) {
                return state;
            }
            SessionState created = new SessionState();
            session.setAttribute(SESSION_KEY, created);
            return created;
        }
    }

    private void prune(SessionState state) {
        Instant threshold = Instant.now().minus(FORM_TTL);
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, SubmissionState> entry : state.submissions.entrySet()) {
            if (entry.getValue().updatedAt.isBefore(threshold)) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(state.submissions::remove);
    }

    private void trim(SessionState state) {
        while (state.submissions.size() > MAX_FORMS) {
            String oldestKey = state.submissions.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().updatedAt))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestKey == null) {
                return;
            }
            state.submissions.remove(oldestKey);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean equalsConstantTime(String left, String right) {
        byte[] leftBytes = left == null ? new byte[0] : left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right == null ? new byte[0] : right.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }

    private static final class SessionState {
        private final Map<String, String> csrfTokens = new LinkedHashMap<>();
        private final Map<String, SubmissionState> submissions = new LinkedHashMap<>();
    }

    private static final class SubmissionState {
        private String kind;
        private Long appUserId;
        private Instant createdAt;
        private Instant updatedAt;
        private String payloadHash;
        private String rawFormHash;
        private String confirmationToken;
        private String confirmationPayloadHash;
        private boolean completed;
        private String redirectPath;
        private String successMessage;
    }

    public static final class PreparedTokens {
        private final String csrfToken;
        private final String submissionToken;

        public PreparedTokens(String csrfToken, String submissionToken) {
            this.csrfToken = csrfToken;
            this.submissionToken = submissionToken;
        }

        public String getCsrfToken() {
            return csrfToken;
        }

        public String getSubmissionToken() {
            return submissionToken;
        }
    }

    public static final class SubmissionDecision {
        private final boolean proceed;
        private final boolean confirmationRequired;
        private final boolean replay;
        private final String confirmationToken;
        private final String redirectPath;
        private final String successMessage;

        private SubmissionDecision(
                boolean proceed,
                boolean confirmationRequired,
                boolean replay,
                String confirmationToken,
                String redirectPath,
                String successMessage) {
            this.proceed = proceed;
            this.confirmationRequired = confirmationRequired;
            this.replay = replay;
            this.confirmationToken = confirmationToken;
            this.redirectPath = redirectPath;
            this.successMessage = successMessage;
        }

        public static SubmissionDecision proceed() {
            return new SubmissionDecision(true, false, false, null, null, null);
        }

        public static SubmissionDecision confirmation(String confirmationToken) {
            return new SubmissionDecision(false, true, false, confirmationToken, null, null);
        }

        public static SubmissionDecision replay(String redirectPath, String successMessage) {
            return new SubmissionDecision(false, false, true, null, redirectPath, successMessage);
        }

        public boolean isProceed() {
            return proceed;
        }

        public boolean isConfirmationRequired() {
            return confirmationRequired;
        }

        public boolean isReplay() {
            return replay;
        }

        public String getConfirmationToken() {
            return confirmationToken;
        }

        public String getRedirectPath() {
            return redirectPath;
        }

        public String getSuccessMessage() {
            return successMessage;
        }
    }
}
