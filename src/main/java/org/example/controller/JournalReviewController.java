package org.example.controller;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.common.session.FormCsrfTokenManager;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.dto.JournalReviewPage;
import org.example.features.taxreturn.service.JournalReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class JournalReviewController {
    private static final String SCOPE = "journal-review";
    private static final String PENDING = JournalReviewController.class.getName() + ".pending";
    private static final long TTL = 10 * 60 * 1000L;
    private static final Set<String> FIELDS = Set.of("year", "status", "page", "id", "csrfToken",
            "reviewToken", "confirmed");
    private static final Logger AUDIT = LoggerFactory.getLogger("audit.journal-review");
    private final JournalReviewService service;
    private final FormCsrfTokenManager csrf;

    public JournalReviewController(JournalReviewService service, FormCsrfTokenManager csrf) {
        this.service = service;
        this.csrf = csrf;
    }

    @GetMapping("/tax-return/journal")
    public String index(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "status", defaultValue = "all") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        return render(currentUser(request), year, status, page, request, response, model);
    }

    @PostMapping("/tax-return/journal/confirm")
    public String confirm(@RequestParam("year") int year, @RequestParam("id") Long id,
            @RequestParam(value = "status", defaultValue = "all") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            @RequestParam(value = "reviewToken", required = false) String reviewToken,
            @RequestParam(value = "confirmed", required = false) String confirmed,
            HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirect) {
        AuthUserContext user = currentUser(request);
        if (!csrf.isValid(request.getSession(), SCOPE, csrfToken)) {
            AUDIT.info("confirm userId={} result=csrf-rejected", user.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (request.getParameterMap().entrySet().stream()
                .anyMatch(e -> !FIELDS.contains(e.getKey()) || e.getValue().length != 1)
                || !Set.of("all", "draft", "reviewed").contains(status) || page < 0 || page > 100000 || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try {
            if (!"true".equals(confirmed)) {
                throw new EntryConflictException("仕訳の内容を確認し、確認欄にチェックしてください。");
            }
            String fingerprint = expected(request.getSession(), reviewToken, user.getUserId(), year, id);
            service.confirm(user.getUserId(), year, id, fingerprint);
        } catch (EntryConflictException ex) {
            AUDIT.info("confirm userId={} year={} journalId={} result=confirmation-rejected", user.getUserId(), year, id);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            model.addAttribute("reviewError", ex.getMessage());
            return render(user, year, status, page, request, response, model);
        }
        redirect.addFlashAttribute("reviewMessage", "仕訳を確認済みにしました。");
        return "redirect:/tax-return/journal?year=" + year + "&status=" + status + "&page=" + page;
    }

    private String render(AuthUserContext user, Integer year, String status, int page,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        JournalReviewPage data;
        try {
            data = service.load(user.getUserId(), year, status, page);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid display conditions");
        }
        model.addAttribute("review", data);
        model.addAttribute("reviewToken", issue(request.getSession(), user.getUserId(), data));
        model.addAttribute("csrfToken", csrf.issue(request.getSession(), SCOPE));
        model.addAttribute("activeMenu", "tax-return");
        model.addAttribute("currentUserDisplayName", user.getDisplayName());
        model.addAttribute("currentUserEmail", user.getEmail());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        return "TaxReturn/journal-review";
    }

    private static String issue(HttpSession session, Long userId, JournalReviewPage page) {
        synchronized (session) {
            ConfirmationPages pages = pages(session);
            long now = System.currentTimeMillis();
            pages.values.values().removeIf(p -> p.expiresAt() < now);
            while (pages.values.size() >= 5) {
                pages.values.remove(pages.values.keySet().iterator().next());
            }
            String token = UUID.randomUUID().toString();
            pages.values.put(token, new PendingPage(userId, page.getYear(), now + TTL, page.getEntries().stream()
                    .collect(Collectors.toMap(JournalReviewPage.Entry::id, JournalReviewPage.Entry::fingerprint))));
            session.setAttribute(PENDING, pages);
            return token;
        }
    }

    private static String expected(HttpSession session, String token, Long userId, int year, Long id) {
        synchronized (session) {
            PendingPage pending = token == null ? null : pages(session).values.get(token);
            if (pending == null || !pending.userId().equals(userId) || pending.year() != year
                    || pending.expiresAt() < System.currentTimeMillis() || !pending.fingerprints().containsKey(id)) {
                throw new EntryConflictException("確認画面の有効期限が切れたか、対象が変わりました。最新の内容を確認してください。");
            }
            return pending.fingerprints().get(id);
        }
    }

    private static ConfirmationPages pages(HttpSession session) {
        Object value = session.getAttribute(PENDING);
        return value instanceof ConfirmationPages pages ? pages : new ConfirmationPages();
    }

    private static AuthUserContext currentUser(HttpServletRequest request) {
        Object value = request.getSession().getAttribute(AuthSessionAttributes.CURRENT_USER);
        if (!(value instanceof AuthUserContext user) || user.isMfaPending()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    @ExceptionHandler(EntryNotFoundException.class)
    public void notFound() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private static final class ConfirmationPages implements Serializable {
        private final Map<String, PendingPage> values = new LinkedHashMap<>();
    }

    private record PendingPage(Long userId, int year, long expiresAt, Map<Long, String> fingerprints)
            implements Serializable {
    }
}
