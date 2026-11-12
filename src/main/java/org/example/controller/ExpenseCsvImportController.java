package org.example.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryForbiddenException;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.entry.csv.ExpenseCsvImportPreparation;
import org.example.features.entry.csv.ExpenseCsvParser;
import org.example.features.entry.service.EntrySaveResult;
import org.example.features.entry.service.EntryService;
import org.example.features.entry.service.ValidatedEntryCommand;
import org.example.features.entry.session.EntrySessionTokenManager;
import org.example.features.entry.session.EntrySessionTokenManager.SubmissionDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

/** 支出登録画面の CSV フォーマット出力と CSV 一括登録。 */
@Controller
public class ExpenseCsvImportController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpenseCsvImportController.class);
    private static final String TEMPLATE_FILE_NAME = "expense-import-template.csv";
    private static final String EXPENSE_PATH = "/expense";
    private static final int MAX_DISPLAYED_ERRORS = 30;

    private final EntryService entryService;
    private final EntrySessionTokenManager tokenManager;

    public ExpenseCsvImportController(EntryService entryService, EntrySessionTokenManager tokenManager) {
        this.entryService = entryService;
        this.tokenManager = tokenManager;
    }

    @GetMapping("/expense/import/template")
    public void downloadTemplate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requireContext(request);
        byte[] content = ExpenseCsvParser.templateBytes();
        response.setContentType("text/csv; charset=UTF-8");
        response.setContentLength(content.length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + TEMPLATE_FILE_NAME + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-store");
        response.getOutputStream().write(content);
    }

    @PostMapping("/expense/import")
    public String importCsv(
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            @RequestParam(value = "submissionToken", required = false) String submissionToken,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) throws IOException {
        AuthUserContext context = requireContext(request);
        HttpSession session = request.getSession();
        Long userId = context.getUserId();
        try {
            tokenManager.validateCsrf(session, EntryKind.EXPENSE, userId, csrfToken);
            tokenManager.validateSubmissionToken(session, EntryKind.EXPENSE, userId, submissionToken);
        } catch (EntryForbiddenException ex) {
            logResult(userId, "forbidden", 0);
            return fail(redirectAttributes, ex.getMessage(), List.of());
        }

        String fileError = validateFile(file);
        if (fileError != null) {
            logResult(userId, "invalid-file", 0);
            return fail(redirectAttributes, fileError, List.of());
        }
        byte[] content = file.getBytes();
        String rawHash = sha256(userId + "|" + Base64.getEncoder().encodeToString(sha256Bytes(content)));
        try {
            SubmissionDecision completed = tokenManager.checkCompletedReplay(
                    session, EntryKind.EXPENSE, userId, submissionToken, rawHash);
            if (completed.isReplay()) {
                redirectAttributes.addFlashAttribute("successMessage", completed.getSuccessMessage());
                return "redirect:" + completed.getRedirectPath();
            }

            ExpenseCsvParser.Result parsed = ExpenseCsvParser.parse(content);
            if (parsed.hasErrors()) {
                logResult(userId, "invalid-format", 0);
                return fail(redirectAttributes, "CSVファイルの形式に誤りがあります。", parsed.errors());
            }
            ExpenseCsvImportPreparation prepared = entryService.prepareExpenseCsvImport(userId, parsed.rows());
            if (!prepared.isValid()) {
                logResult(userId, "invalid-rows", parsed.rows().size());
                return fail(redirectAttributes, "入力内容に誤りがあるため登録しませんでした。修正して再度インポートしてください。",
                        prepared.errors());
            }

            ValidatedEntryCommand command = prepared.command();
            synchronized (tokenManager.getSessionMonitor(session)) {
                SubmissionDecision decision = tokenManager.evaluateSubmission(
                        session, EntryKind.EXPENSE, userId, submissionToken, command.getPayloadHash(), false, null);
                if (decision.isReplay()) {
                    redirectAttributes.addFlashAttribute("successMessage", decision.getSuccessMessage());
                    return "redirect:" + decision.getRedirectPath();
                }
                EntrySaveResult result = entryService.save(command);
                String successMessage = "CSVから" + result.getSuccessMessage();
                tokenManager.markCompleted(session, EntryKind.EXPENSE, userId, submissionToken,
                        rawHash, command.getPayloadHash(), EXPENSE_PATH, successMessage);
                logResult(userId, "success", command.getMonthlyEntries().size());
                redirectAttributes.addFlashAttribute("successMessage", successMessage);
                return "redirect:" + EXPENSE_PATH;
            }
        } catch (EntryForbiddenException ex) {
            logResult(userId, "forbidden", 0);
            return fail(redirectAttributes, ex.getMessage(), List.of());
        } catch (OptimisticLockingFailureException ex) {
            logResult(userId, "stale", 0);
            return fail(redirectAttributes, "他の更新が行われたため登録できませんでした。再度インポートしてください。", List.of());
        } catch (EntryConflictException | EntryNotFoundException ex) {
            logResult(userId, "conflict", 0);
            return fail(redirectAttributes, ex.getMessage(), List.of());
        } catch (DataAccessException ex) {
            logDataAccessFailure(userId, ex);
            return fail(redirectAttributes, "データの保存に失敗しました。時間をおいて再度お試しください。", List.of());
        }
    }

    @ExceptionHandler(MultipartException.class)
    public String handleMultipartFailure(MultipartException ex, HttpServletRequest request) {
        LOGGER.warn("entry kind=expense action=csv-import result=multipart-error");
        var flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("flashError", "ファイルを受け付けられませんでした。1MB以下のCSVファイルを選択してください。");
        flash.put("csvImportOpen", Boolean.TRUE);
        return "redirect:" + EXPENSE_PATH;
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "CSVファイルを選択してください。";
        }
        if (file.getSize() > ExpenseCsvParser.MAX_BYTES) {
            return "CSVファイルは1MB以下にしてください。";
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return "拡張子が .csv のファイルを選択してください。";
        }
        return null;
    }

    private String fail(RedirectAttributes redirectAttributes, String message, List<String> details) {
        List<String> displayed = new ArrayList<>(details.subList(0, Math.min(details.size(), MAX_DISPLAYED_ERRORS)));
        if (details.size() > MAX_DISPLAYED_ERRORS) {
            displayed.add("ほか" + (details.size() - MAX_DISPLAYED_ERRORS) + "件のエラーがあります。");
        }
        redirectAttributes.addFlashAttribute("flashError", message);
        redirectAttributes.addFlashAttribute("formErrors", displayed);
        redirectAttributes.addFlashAttribute("csvImportOpen", Boolean.TRUE);
        return "redirect:" + EXPENSE_PATH;
    }

    private AuthUserContext requireContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthSessionAttributes.CURRENT_USER) instanceof AuthUserContext context) {
            return context;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    private void logResult(Long userId, String result, int rows) {
        LOGGER.info("entry userId={} kind=expense action=csv-import result={} rows={}", userId, result, rows);
    }

    private void logDataAccessFailure(Long userId, DataAccessException ex) {
        Throwable cause = ex.getMostSpecificCause();
        SQLException sqlException = cause instanceof SQLException sql ? sql : null;
        LOGGER.warn("entry userId={} kind=expense action=csv-import result=data-access-error type={} causeType={} sqlState={} vendorCode={}",
                userId,
                ex.getClass().getSimpleName(),
                cause.getClass().getSimpleName(),
                sqlException == null ? "n/a" : sqlException.getSQLState(),
                sqlException == null ? "n/a" : sqlException.getErrorCode());
    }

    private static byte[] sha256Bytes(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String sha256(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256Bytes(value.getBytes(StandardCharsets.UTF_8)));
    }
}
