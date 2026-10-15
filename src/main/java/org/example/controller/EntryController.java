package org.example.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryForbiddenException;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.form.EntryForm;
import org.example.features.entry.form.MonthlyExpenseRowForm;
import org.example.features.entry.service.EntryPrepareResult;
import org.example.features.entry.service.EntrySaveResult;
import org.example.features.entry.service.EntryService;
import org.example.features.entry.service.ValidatedEntryCommand;
import org.example.features.entry.session.EntrySessionTokenManager;
import org.example.features.entry.session.EntrySessionTokenManager.PreparedTokens;
import org.example.features.entry.session.EntrySessionTokenManager.SubmissionDecision;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class EntryController {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntryController.class);

    private final EntryService entryService;
    private final EntrySessionTokenManager tokenManager;

    public EntryController(EntryService entryService, EntrySessionTokenManager tokenManager) {
        this.entryService = entryService;
        this.tokenManager = tokenManager;
    }

    @InitBinder("entryForm")
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(
                "editId",
                "version",
                "incomeType",
                "expenseType",
                "category",
                "date",
                "amount",
                "receiveMethodId",
                "paymentMethodId",
                "excludeFromReturn",
                "homeApportionment",
                "details",
                "entryMode",
                "targetMonth",
                "monthlyRows[*].expenseType",
                "monthlyRows[*].category",
                "monthlyRows[*].date",
                "monthlyRows[*].amount",
                "monthlyRows[*].paymentMethodId",
                "monthlyRows[*].excludeFromReturn",
                "monthlyRows[*].homeApportionment",
                "monthlyRows[*].details",
                "csrfToken",
                "submissionToken",
                "confirmationToken");
    }

    @GetMapping("/income")
    public String showIncome(
            @RequestParam(value = "editId", required = false) Long editId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            @ModelAttribute("successMessage") String successMessage) {
        return renderGetPage(EntryKind.INCOME, editId, request, response, model, successMessage);
    }

    @GetMapping("/expense")
    public String showExpense(
            @RequestParam(value = "editId", required = false) Long editId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            @ModelAttribute("successMessage") String successMessage) {
        return renderGetPage(EntryKind.EXPENSE, editId, request, response, model, successMessage);
    }

    @PostMapping("/income")
    public String submitIncome(
            @ModelAttribute("entryForm") EntryForm entryForm,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        return handlePost(EntryKind.INCOME, entryForm, bindingResult, request, response, model, redirectAttributes);
    }

    @PostMapping("/expense")
    public String submitExpense(
            @ModelAttribute("entryForm") EntryForm entryForm,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        return handlePost(EntryKind.EXPENSE, entryForm, bindingResult, request, response, model, redirectAttributes);
    }

    private String renderGetPage(
            EntryKind kind,
            Long editId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            String successMessage) {
        AuthUserContext context = requireContext(request);
        try {
            EntryPageData pageData = entryService.loadPage(kind, context.getUserId(), editId);
            if (successMessage != null && !successMessage.isBlank()) {
                pageData.setSuccessMessage(successMessage);
            }
            return renderPage(kind, pageData, request.getSession(), response, model, context, false);
        } catch (EntryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
    }

    private String handlePost(
            EntryKind kind,
            EntryForm entryForm,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = requireContext(request);
        HttpSession session = request.getSession();
        applyNoStore(response);
        rejectSuppressedOrBindingErrors(bindingResult, request);
        try {
            tokenManager.validateCsrf(session, kind, context.getUserId(), entryForm.getCsrfToken());
            tokenManager.validateSubmissionToken(session, kind, context.getUserId(), entryForm.getSubmissionToken());
            String rawFormHash = hashRawForm(kind, context.getUserId(), entryForm);
            SubmissionDecision completedDecision = tokenManager.checkCompletedReplay(
                    session,
                    kind,
                    context.getUserId(),
                    entryForm.getSubmissionToken(),
                    rawFormHash);
            if (completedDecision.isReplay()) {
                redirectAttributes.addFlashAttribute("successMessage", completedDecision.getSuccessMessage());
                return "redirect:" + completedDecision.getRedirectPath();
            }
            EntryPrepareResult prepared = entryService.prepareSubmission(kind, context.getUserId(), entryForm);
            EntryPageData pageData = prepared.getPageData();
            if (prepared.getValidatedEntry() == null) {
                response.setStatus(pageData.getFailureMessage() == null ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_CONFLICT);
                return renderPage(kind, pageData, session, response, model, context, true);
            }
            ValidatedEntryCommand command = prepared.getValidatedEntry();
            synchronized (tokenManager.getSessionMonitor(session)) {
                SubmissionDecision decision = tokenManager.evaluateSubmission(
                        session,
                        kind,
                        context.getUserId(),
                        pageData.getEntryForm().getSubmissionToken(),
                        command.getPayloadHash(),
                        command.isConfirmationRequired(),
                        pageData.getEntryForm().getConfirmationToken());
                if (decision.isReplay()) {
                    redirectAttributes.addFlashAttribute("successMessage", decision.getSuccessMessage());
                    return "redirect:" + decision.getRedirectPath();
                }
                if (decision.isConfirmationRequired()) {
                    pageData.setConfirmationRequired(true);
                    pageData.getEntryForm().setConfirmationToken(decision.getConfirmationToken());
                    return renderPage(kind, pageData, session, response, model, context, true);
                }
                try {
                    EntrySaveResult result = entryService.save(command);
                    tokenManager.markCompleted(
                            session,
                            kind,
                            context.getUserId(),
                            pageData.getEntryForm().getSubmissionToken(),
                            rawFormHash,
                            command.getPayloadHash(),
                            result.getRedirectPath(),
                            result.getSuccessMessage());
                    redirectAttributes.addFlashAttribute("successMessage", result.getSuccessMessage());
                    logResult(context.getUserId(), kind, command.getEditId() == null ? "create" : "update", "success");
                    return "redirect:" + result.getRedirectPath();
                } catch (OptimisticLockingFailureException ex) {
                    logResult(context.getUserId(), kind, command.getEditId() == null ? "create" : "update", "stale");
                    pageData.getFieldErrors().putIfAbsent("version", "別の更新が行われたため、再読み込みしてください。");
                    pageData.setFailureMessage("他の更新が行われたため保存できませんでした。再読み込みしてください。");
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    return renderPage(kind, pageData, session, response, model, context, true);
                } catch (EntryConflictException ex) {
                    logResult(context.getUserId(), kind, command.getEditId() == null ? "create" : "update", "conflict");
                    pageData.setFailureMessage(ex.getMessage());
                    if (ex.getMessage() != null && ex.getMessage().contains("再読み込み")) {
                        pageData.getFieldErrors().putIfAbsent("version", ex.getMessage());
                    }
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    return renderPage(kind, pageData, session, response, model, context, true);
                } catch (DataAccessException ex) {
                    logDataAccessFailure(context.getUserId(), kind, command.getEditId() == null ? "create" : "update", ex);
                    pageData.setFailureMessage("データの保存に失敗しました。時間をおいて再度お試しください。");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return renderPage(kind, pageData, session, response, model, context, true);
                }
            }
        } catch (EntryForbiddenException ex) {
            try {
                EntryPageData pageData = entryService.loadPage(kind, context.getUserId(), entryForm.getEditId());
                pageData.setFailureMessage(ex.getMessage());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return renderPage(kind, pageData, session, response, model, context, false);
            } catch (EntryNotFoundException notFoundException) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
            }
        } catch (EntryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
    }

    private void rejectSuppressedOrBindingErrors(BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult == null) {
            return;
        }
        // Spring also binds request headers as candidate fields; only posted parameters count as tampering.
        boolean suppressedParameter = java.util.Arrays.stream(bindingResult.getSuppressedFields())
                .anyMatch(field -> request.getParameterMap().containsKey(field));
        if (suppressedParameter || bindingResult.hasFieldErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        }
    }

    private String renderPage(
            EntryKind kind,
            EntryPageData pageData,
            HttpSession session,
            HttpServletResponse response,
            Model model,
            AuthUserContext context,
            boolean preserveTokens) {
        applyNoStore(response);
        pageData.setCurrentUserDisplayName(context.getDisplayName());
        pageData.setCurrentUserEmail(context.getEmail());
        pageData.setActiveMenu(kind.getActiveMenu());
        if (pageData.getEntryForm() == null) {
            pageData.setEntryForm(new EntryForm());
        }
        if (!preserveTokens
                || isBlank(pageData.getEntryForm().getCsrfToken())
                || isBlank(pageData.getEntryForm().getSubmissionToken())) {
            PreparedTokens tokens = tokenManager.issueTokens(session, kind, context.getUserId());
            pageData.getEntryForm().setCsrfToken(tokens.getCsrfToken());
            pageData.getEntryForm().setSubmissionToken(tokens.getSubmissionToken());
            if (!preserveTokens) {
                pageData.getEntryForm().setConfirmationToken("");
            }
        }
        model.addAttribute("entryKind", pageData.getEntryKind());
        model.addAttribute("entryForm", pageData.getEntryForm());
        model.addAttribute("categories", pageData.getCategories());
        model.addAttribute("items", pageData.getItems());
        model.addAttribute("methods", pageData.getMethods());
        model.addAttribute("apportionmentRate", pageData.getApportionmentRate());
        model.addAttribute("today", pageData.getToday());
        model.addAttribute("fieldErrors", pageData.getFieldErrors());
        model.addAttribute("failureMessage", pageData.getFailureMessage());
        model.addAttribute("successMessage", pageData.getSuccessMessage());
        model.addAttribute("confirmationRequired", pageData.isConfirmationRequired());
        model.addAttribute("activeMenu", pageData.getActiveMenu());
        model.addAttribute("currentUserDisplayName", pageData.getCurrentUserDisplayName());
        model.addAttribute("currentUserEmail", pageData.getCurrentUserEmail());
        model.addAttribute("fiscalYearLabel", pageData.getFiscalYearLabel());
        model.addAttribute("validationErrors", toValidationErrors(pageData.getFieldErrors(), pageData.getFailureMessage()));
        return kind.getViewName();
    }

    private List<String> toValidationErrors(Map<String, String> fieldErrors, String failureMessage) {
        List<String> messages = new ArrayList<>();
        if (failureMessage != null && !failureMessage.isBlank()) {
            messages.add(failureMessage);
        }
        if (fieldErrors != null) {
            messages.addAll(fieldErrors.values());
        }
        return messages;
    }

    private void applyNoStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private AuthUserContext requireContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        Object currentUser = session.getAttribute(AuthSessionAttributes.CURRENT_USER);
        if (currentUser instanceof AuthUserContext context) {
            return context;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    private void logResult(Long userId, EntryKind kind, String action, String result) {
        LOGGER.info("entry userId={} kind={} action={} result={}", userId, kind.getCode(), action, result);
    }

    private void logDataAccessFailure(Long userId, EntryKind kind, String action, DataAccessException ex) {
        LOGGER.warn("entry userId={} kind={} action={} result=data-access-error type={}",
                userId,
                kind.getCode(),
                action,
                ex.getClass().getSimpleName());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String hashRawForm(EntryKind kind, Long userId, EntryForm form) {
        StringBuilder monthlyRowsHash = new StringBuilder();
        if (form.getMonthlyRows() != null) {
            for (MonthlyExpenseRowForm row : form.getMonthlyRows()) {
                if (row == null) {
                    monthlyRowsHash.append("<null-row>|\n");
                    continue;
                }
                monthlyRowsHash.append(nullToEmpty(row.getExpenseType())).append('|')
                        .append(nullToEmpty(row.getCategory())).append('|')
                        .append(nullToEmpty(row.getDate())).append('|')
                        .append(nullToEmpty(row.getAmount())).append('|')
                        .append(nullToEmpty(row.getPaymentMethodId())).append('|')
                        .append(row.isExcludeFromReturn()).append('|')
                        .append(row.isHomeApportionment()).append('|')
                        .append(nullToEmpty(row.getDetails())).append("\n");
            }
        }
        String payload = String.join("|",
                kind.getCode(),
                String.valueOf(userId),
                String.valueOf(form.getEditId()),
                String.valueOf(form.getVersion()),
                nullToEmpty(form.getIncomeType()),
                nullToEmpty(form.getExpenseType()),
                nullToEmpty(form.getCategory()),
                nullToEmpty(form.getDate()),
                nullToEmpty(form.getAmount()),
                nullToEmpty(form.getReceiveMethodId()),
                nullToEmpty(form.getPaymentMethodId()),
                String.valueOf(form.isExcludeFromReturn()),
                String.valueOf(form.isHomeApportionment()),
                nullToEmpty(form.getDetails()),
                nullToEmpty(form.getEntryMode()),
                nullToEmpty(form.getTargetMonth()),
                monthlyRowsHash.toString(),
                nullToEmpty(form.getConfirmationToken()));
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
