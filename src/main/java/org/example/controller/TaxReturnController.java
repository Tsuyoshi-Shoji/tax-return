package org.example.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.example.features.taxreturn.export.TaxReturnPdfRenderer;
import org.example.features.taxreturn.export.TaxReturnPdfRenderer.PdfCapacityExceededException;
import org.example.features.taxreturn.export.TaxReturnPdfRenderer.PdfContentUnavailableException;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.common.session.FormCsrfTokenManager;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.setting.TaxReturnSection;
import org.example.features.setting.TaxReturnSectionVisibility;
import org.example.features.setting.service.SettingService;
import org.example.features.taxreturn.export.EtaxAssistCsv;
import org.example.features.taxreturn.form.SettlementForm;
import org.example.features.taxreturn.form.TaxReturnForms.DeductionForm;
import org.example.features.taxreturn.form.TaxReturnForms.DependentForm;
import org.example.features.taxreturn.form.TaxReturnForms.FixedAssetForm;
import org.example.features.taxreturn.form.TaxReturnForms.DepreciationRulesForm;
import org.example.features.taxreturn.form.TaxReturnForms.FixedAssetEventForm;
import org.example.features.taxreturn.form.TaxReturnForms.EmployeeSalaryForm;
import org.example.features.taxreturn.form.TaxReturnForms.HouseholdConsumptionForm;
import org.example.features.taxreturn.form.TaxReturnForms.ReturnInputForm;
import org.example.features.taxreturn.input.TaxReturnInputService;
import org.example.features.taxreturn.closing.ClosingPageData;
import org.example.features.taxreturn.closing.YearClosingService;
import org.example.features.taxreturn.retention.RetentionPageData;
import org.example.features.taxreturn.retention.RetentionRecordForm;
import org.example.features.taxreturn.retention.RetentionSearch;
import org.example.features.taxreturn.retention.RetentionService;
import org.example.features.taxreturn.evidence.EvidenceFileService;
import org.example.features.taxreturn.evidence.EvidenceFileService.AuthorizationStart;
import org.example.features.taxreturn.evidence.EvidenceFileService.EvidenceDownload;
import org.example.features.taxreturn.evidence.EvidenceFileService.EvidenceException;
import org.example.features.taxreturn.evidence.EvidenceUploadValidator;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.example.features.taxreturn.service.TaxReturnService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

@Controller
public class TaxReturnController {
    private static final String CSRF_SCOPE = "tax-return";
    private static final String OAUTH_PENDING = TaxReturnController.class.getName() + ".evidenceOAuth";
    private static final long OAUTH_TTL_MILLIS = 10 * 60 * 1000L;
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final int MAX_PDF_BOOK_ENTRIES = 10000;
    private static final int MAX_PDF_ARCHIVE_BYTES = 50 * 1024 * 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxReturnController.class);
    private static final List<PdfReport> PDF_REPORTS = List.of(
            new PdfReport("return", "青色申告決算書", "blue-return"),
            new PdfReport("balance-sheet", "貸借対照表", "balance-sheet"),
            new PdfReport("profit-loss", "損益計算書", "profit-loss"),
            new PdfReport("journal", "仕訳帳", "journal"),
            new PdfReport("ledger", "総勘定元帳", "ledger"),
            new PdfReport("final-return", "確定申告書（第一表・第二表）", "final-return"),
            new PdfReport("consumption", "消費税計算書（簡易課税）", "consumption"));

    private final TaxReturnService taxReturnService;
    private final FormCsrfTokenManager csrfTokenManager;
    private final TaxReturnInputService inputService;
    private final RetentionService retentionService;
    private final EvidenceFileService evidenceFileService;
    private final YearClosingService yearClosingService;
    private final SettingService settingService;
    private final TaxReturnPdfRenderer pdfRenderer = new TaxReturnPdfRenderer();

    public TaxReturnController(TaxReturnService taxReturnService, FormCsrfTokenManager csrfTokenManager,
            TaxReturnInputService inputService, RetentionService retentionService,
            EvidenceFileService evidenceFileService, YearClosingService yearClosingService,
            SettingService settingService) {
        this.taxReturnService = taxReturnService;
        this.csrfTokenManager = csrfTokenManager;
        this.inputService = inputService;
        this.retentionService = retentionService;
        this.evidenceFileService = evidenceFileService;
        this.yearClosingService = yearClosingService;
        this.settingService = settingService;
    }

    @InitBinder("settlementForm")
    void bindSettlement(WebDataBinder binder) {
        binder.setAllowedFields("openingInventory", "closingInventory", "accountsReceivable",
                "accountsPayable", "unpaidExpenses", "unreceivedIncome", "openingBalance", "closingCash", "csrfToken");
    }

    @InitBinder("fixedAssetForm")
    void bindFixedAsset(WebDataBinder binder) {
        binder.setAllowedFields("assetName", "accountCode", "acquisitionDate", "serviceStartDate", "acquisitionCost",
                "usefulLife", "lifeBasis", "usedElapsedMonths", "usedRequirementsConfirmed", "privateUseConversion",
                "rateTransitionConfirmed", "depreciationMethod", "businessRatio", "acquisitionFunding", "notes", "csrfToken");
    }

    @InitBinder("depreciationRulesForm")
    void bindDepreciationRules(WebDataBinder binder) {
        binder.setAllowedFields("methodBasis", "scopeConfirmed", "specialConfirmed", "businessMonths", "evidence", "csrfToken");
    }

    @PostMapping("/tax-return/assets/rules")
    public String saveDepreciationRules(@RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("depreciationRulesForm") DepreciationRulesForm form,
            HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        return finish(call(() -> inputService.saveDepreciationRules(context.getUserId(), year, form)),
                "対象年分の償却要件を利用者確認として保存しました。法的適格性の証明ではありません。",
                year, request, response, model, redirectAttributes);
    }

    @InitBinder("fixedAssetEventForm")
    void bindFixedAssetEvent(WebDataBinder binder) {
        binder.setAllowedFields("assetId", "eventType", "effectiveDate", "businessRatio", "proceeds", "notes", "csrfToken");
    }

    @InitBinder("returnInputForm")
    void bindReturnInput(WebDataBinder binder) {
        binder.setAllowedFields("salaryRevenue", "salaryWithholding", "salaryPayerName", "salaryPayerAddress",
                "miscRevenue", "miscExpense", "miscWithholding", "miscPayerName", "prepaidTax", "selfDisability",
                "widowStatus", "workingStudent", "hasSpouse", "spouseName", "spouseBirthDate", "spouseIncome",
                "spouseDisability", "spouseCohabiting", "basePeriodTaxableSales", "csrfToken");
    }

    @InitBinder("dependentForm")
    void bindDependent(WebDataBinder binder) {
        binder.setAllowedFields("dependentName", "relationship", "birthDate", "income", "cohabiting", "disability", "csrfToken");
    }

    @InitBinder("householdConsumptionForm")
    void bindHouseholdConsumption(WebDataBinder binder) {
        binder.setAllowedFields("transactionDate", "details", "amount", "csrfToken");
    }

    @InitBinder("employeeSalaryForm")
    void bindEmployeeSalary(WebDataBinder binder) {
        binder.setAllowedFields("salaryKind", "personName", "jobDescription", "workPeriodStart",
                "workPeriodEnd", "paymentPeriod", "amount", "csrfToken");
    }

    @InitBinder("deductionForm")
    void bindDeduction(WebDataBinder binder) {
        binder.setAllowedFields("deductionType", "paidDate", "paidAmount", "reimbursedAmount", "payer", "notes", "csrfToken");
    }

    @InitBinder("retentionRecordForm")
    void bindRetentionRecord(WebDataBinder binder) {
        binder.setAllowedFields("recordKind", "documentType", "title", "journalEntryId",
                "retentionStartDate", "retentionDeadline", "legalBasis", "csrfToken");
    }

    @PostMapping("/tax-return/assets")
    public String addAsset(@RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("fixedAssetForm") FixedAssetForm form, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        return finish(call(() -> inputService.addFixedAsset(context.getUserId(), year, form)),
                "固定資産を登録しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/assets/delete")
    public String deleteAsset(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "csrfToken", required = false) String csrfToken, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        return finish(call(() -> inputService.deleteFixedAsset(context.getUserId(), year, id)),
                "固定資産を削除しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/assets/events")
    public String addAssetEvent(@RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("fixedAssetEventForm") FixedAssetEventForm form, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        return finish(call(() -> inputService.addFixedAssetEvent(context.getUserId(), year, form)),
                "固定資産イベントを登録し、未確定年度の仕訳を再計算しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/assets/events/delete")
    public String deleteAssetEvent(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "csrfToken", required = false) String csrfToken, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        return finish(call(() -> inputService.deleteFixedAssetEvent(context.getUserId(), year, id)),
                "固定資産イベントを削除し、未確定年度の仕訳を再計算しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/household-consumption")
    public String addHouseholdConsumption(@RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("householdConsumptionForm") HouseholdConsumptionForm form, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        return finish(call(() -> inputService.addHouseholdConsumption(context.getUserId(), year, form)),
                "家事消費等を登録し、売上として仕訳に反映しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/household-consumption/delete")
    public String deleteHouseholdConsumption(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "csrfToken", required = false) String csrfToken, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        return finish(call(() -> inputService.deleteHouseholdConsumption(context.getUserId(), year, id)),
                "家事消費等を削除しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/salary-details")
    public String addEmployeeSalaryDetail(@RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("employeeSalaryForm") EmployeeSalaryForm form, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        return finish(call(() -> inputService.addEmployeeSalaryDetail(context.getUserId(), year, form)),
                "給与の個人別内訳を登録しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/salary-details/delete")
    public String deleteEmployeeSalaryDetail(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "csrfToken", required = false) String csrfToken, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        return finish(call(() -> inputService.deleteEmployeeSalaryDetail(context.getUserId(), year, id)),
                "給与の個人別内訳を削除しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/return-input")
    public String saveReturnInput(@RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("returnInputForm") ReturnInputForm form, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        return finish(call(() -> inputService.saveReturnInput(context.getUserId(), year, form)),
                "申告情報を保存しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/dependents")
    public String addDependent(@RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("dependentForm") DependentForm form, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        return finish(call(() -> inputService.addDependent(context.getUserId(), year, form)),
                "扶養親族を登録しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/dependents/delete")
    public String deleteDependent(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "csrfToken", required = false) String csrfToken, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        return finish(call(() -> inputService.deleteDependent(context.getUserId(), year, id)),
                "扶養親族を削除しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/deductions")
    public String addDeductionRecord(@RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("deductionForm") DeductionForm form, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        return finish(call(() -> inputService.addDeduction(context.getUserId(), year, form)),
                "所得控除を登録しました。証憑を確認後「確認済み」にしてください。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/deductions/status")
    public String updateDeductionStatus(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "csrfToken", required = false) String csrfToken, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        return finish(call(() -> inputService.updateDeductionStatus(context.getUserId(), year, id, status)),
                "所得控除の状態を更新しました。", year, request, response, model, redirectAttributes);
    }

    @PostMapping("/tax-return/deductions/delete")
    public String deleteDeductionRecord(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "csrfToken", required = false) String csrfToken, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        return finish(call(() -> inputService.deleteDeduction(context.getUserId(), year, id)),
                "所得控除を削除しました。", year, request, response, model, redirectAttributes);
    }

    @GetMapping("/tax-return/export/etax-csv")
    public String exportEtaxCsv(@RequestParam(value = "year", required = false) Integer year,
            HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes)
            throws java.io.IOException {
        AuthUserContext context = currentUser(request);
        TaxReturnPageData page = call(() -> taxReturnService.load(context.getUserId(), year, null));
        if (!page.getErrors().isEmpty()) {
            List<String> messages = new java.util.ArrayList<>();
            messages.add("エラーがあるため出力できません。エラーチェックの内容をご確認ください。");
            messages.addAll(page.getErrors());
            redirectAttributes.addFlashAttribute("formErrors", messages);
            return redirect(page.getSelectedYear());
        }

        byte[] body = EtaxAssistCsv.build(page).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fileName = "tax-return_etax-assist_" + page.getSelectedYear() + ".csv";
        yearClosingService.recordOutput(context.getUserId(), page, "csv:etax", fileName, body,
                LocalDateTime.now(TOKYO));
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
        return null;
    }

    @GetMapping("/tax-return/export/books-csv")
    public String exportBooksCsv(@RequestParam("year") Integer year, HttpServletRequest request,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            HttpServletResponse response, RedirectAttributes redirectAttributes) throws java.io.IOException {
        AuthUserContext context = currentUser(request);
        TaxReturnService.BooksExport export = call(() -> taxReturnService.exportBooks(context.getUserId(), year,
                fromDate, toDate, accountCode));
        return writeBooksCsv(export, "books_", "csv:books", context.getUserId(), response, redirectAttributes);
    }

    @GetMapping("/tax-return/export/fixed-assets-csv")
    public String exportFixedAssetsCsv(@RequestParam("year") Integer year, HttpServletRequest request,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            HttpServletResponse response, RedirectAttributes redirectAttributes) throws java.io.IOException {
        AuthUserContext context = currentUser(request);
        TaxReturnService.BooksExport export = call(() -> taxReturnService.exportFixedAssetLedger(context.getUserId(),
                year, fromDate, toDate, accountCode));
        return writeBooksCsv(export, "fixed-assets_", "csv:fixed-assets", context.getUserId(), response,
                redirectAttributes);
    }

    @GetMapping("/tax-return/pdf/{type}")
    public String exportPdf(@PathVariable String type, @RequestParam("year") Integer year,
            @RequestParam(value = "confirmed", defaultValue = "false") boolean confirmed,
            HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes)
            throws IOException {
        AuthUserContext context = currentUser(request);
        PdfReport report = findPdfReport(type);
        if (report == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        }
        RenderedTaxReturnPage rendered = renderPdfPage(request, response, year, context.getUserId());
        TaxReturnPageData page = rendered.page();
        if (!isPdfReportAvailable(report, page)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        }
        String blocked = blockPdfExport(page, confirmed, context, report.type(), redirectAttributes);
        if (blocked != null) {
            return blocked;
        }
        Instant generatedAt = Instant.now();
        TaxReturnPdfRenderer.OutputStamp stamp = stamp(rendered.closing(), generatedAt);
        byte[] body = createPdf(rendered.html(), page, report, context.getUserId(), stamp);
        String fileName = "tax-return_" + report.fileName() + "_" + page.getSelectedYear() + ".pdf";
        yearClosingService.recordOutput(context.getUserId(), page, "pdf:" + report.type(), fileName, body,
                LocalDateTime.ofInstant(generatedAt, TOKYO));
        writePdfDownload(body, fileName, "application/pdf", response, context.getUserId(), page.getSelectedYear(),
                report.type());
        return null;
    }

    @GetMapping("/tax-return/export/pdfs.zip")
    public String exportPdfsZip(@RequestParam("year") Integer year,
            @RequestParam(value = "confirmed", defaultValue = "false") boolean confirmed,
            HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes)
            throws IOException {
        AuthUserContext context = currentUser(request);
        RenderedTaxReturnPage rendered = renderPdfPage(request, response, year, context.getUserId());
        TaxReturnPageData page = rendered.page();
        String blocked = blockPdfExport(page, confirmed, context, "all", redirectAttributes);
        if (blocked != null) {
            return blocked;
        }
        Instant generatedAt = Instant.now();
        TaxReturnPdfRenderer.OutputStamp stamp = stamp(rendered.closing(), generatedAt);
        BoundedOutputStream archive = new BoundedOutputStream(MAX_PDF_ARCHIVE_BYTES);
        try (ZipOutputStream zip = new ZipOutputStream(archive)) {
            for (PdfReport report : PDF_REPORTS) {
                if (!isPdfReportAvailable(report, page)) {
                    continue;
                }
                byte[] pdf = createPdf(rendered.html(), page, report, context.getUserId(), stamp);
                zip.putNextEntry(new ZipEntry("tax-return_" + report.fileName() + "_"
                        + page.getSelectedYear() + ".pdf"));
                zip.write(pdf);
                zip.closeEntry();
            }
        } catch (PdfArchiveLimitExceededException ex) {
            LOGGER.info("tax-return.pdf.export userId={} year={} type=all result=archive-size-limit",
                    context.getUserId(), page.getSelectedYear());
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "PDF一括ダウンロードの上限を超えています。帳票を個別にダウンロードしてください。");
        }
        byte[] zipBody = archive.toByteArray();
        String zipName = "tax-return_pdfs_" + page.getSelectedYear() + ".zip";
        yearClosingService.recordOutput(context.getUserId(), page, "pdf:all", zipName, zipBody,
                LocalDateTime.ofInstant(generatedAt, TOKYO));
        writePdfDownload(zipBody, zipName, "application/zip", response, context.getUserId(), page.getSelectedYear(),
                "all");
        return null;
    }

    private RenderedTaxReturnPage renderPdfPage(HttpServletRequest request, HttpServletResponse response, Integer year,
            Long userId) {
        ExtendedModelMap model = new ExtendedModelMap();
        render(request, response, model, year, null, null);
        TaxReturnPageData page = (TaxReturnPageData) model.get("page");
        if (page.getErrors().isEmpty()) {
            ensurePdfEntryLimit(page);
        }
        model.asMap().forEach(request::setAttribute);

        StringWriter html = new StringWriter();
        CapturingResponse capture = new CapturingResponse(response, html);
        try {
            request.getRequestDispatcher("/WEB-INF/views/TaxReturn/tax-return.jsp").include(request, capture);
        } catch (IOException | jakarta.servlet.ServletException ex) {
            LOGGER.warn("tax-return.pdf.export userId={} year={} result=view-render-failed", userId, year);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF generation failed");
        }
        return new RenderedTaxReturnPage(html.toString(), page, (ClosingPageData) model.get("closing"));
    }

    private String blockPdfExport(TaxReturnPageData page, boolean confirmed, AuthUserContext context, String type,
            RedirectAttributes redirectAttributes) {
        if (!page.getErrors().isEmpty()) {
            List<String> messages = new ArrayList<>();
            messages.add("エラーがあるためPDFを出力できません。エラーチェックの内容をご確認ください。");
            messages.addAll(page.getErrors());
            redirectAttributes.addFlashAttribute("formErrors", messages);
            LOGGER.info("tax-return.pdf.export userId={} year={} type={} result=rejected-errors",
                    context.getUserId(), page.getSelectedYear(), type);
            return redirect(page.getSelectedYear());
        }
        if (!page.getWarnings().isEmpty() && !confirmed) {
            redirectAttributes.addFlashAttribute("formErrors",
                    List.of("警告があります。画面で内容を確認してからPDFを出力してください。"));
            LOGGER.info("tax-return.pdf.export userId={} year={} type={} result=confirmation-required",
                    context.getUserId(), page.getSelectedYear(), type);
            return redirect(page.getSelectedYear());
        }
        return null;
    }

    private void ensurePdfEntryLimit(TaxReturnPageData page) {
        long ledgerEntries = page.getLedger().stream().mapToLong(account -> account.getLines().size()).sum();
        if (page.getJournalAll().size() > MAX_PDF_BOOK_ENTRIES || ledgerEntries > MAX_PDF_BOOK_ENTRIES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "PDF明細の上限を超えています。帳簿CSVをご利用ください。");
        }
    }

    private PdfReport findPdfReport(String type) {
        return PDF_REPORTS.stream().filter(report -> report.type().equals(type)).findFirst().orElse(null);
    }

    private boolean isPdfReportAvailable(PdfReport report, TaxReturnPageData page) {
        if ("final-return".equals(report.type())) {
            return page.getExtras().getTax() != null;
        }
        if ("consumption".equals(report.type())) {
            return page.getExtras().getConsumption() != null;
        }
        return true;
    }

    private static TaxReturnPdfRenderer.OutputStamp stamp(ClosingPageData closing, Instant generatedAt) {
        String state = closing.isClosed()
                ? (closing.getCurrentVersion() == null ? "締め済み（締め記録なし）" : "締め第" + closing.getCurrentVersion() + "版")
                : "未締め";
        return new TaxReturnPdfRenderer.OutputStamp(generatedAt, closing.getFormEdition(), state,
                closing.getCurrentFingerprint());
    }

    private byte[] createPdf(String html, TaxReturnPageData page, PdfReport report, Long userId,
            TaxReturnPdfRenderer.OutputStamp stamp) {
        try {
            return pdfRenderer.render(html, report.type(), page.getSelectedYear(), report.title(), stamp);
        } catch (PdfCapacityExceededException ex) {
            LOGGER.info("tax-return.pdf.export userId={} year={} type={} result=capacity-rejected",
                    userId, page.getSelectedYear(), report.type());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PDF生成が混み合っています。");
        } catch (PdfContentUnavailableException ex) {
            LOGGER.warn("tax-return.pdf.export userId={} year={} type={} result=content-unavailable",
                    userId, page.getSelectedYear(), report.type());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF generation failed");
        } catch (IOException | com.lowagie.text.DocumentException ex) {
            LOGGER.warn("tax-return.pdf.export userId={} year={} type={} result=generation-failed",
                    userId, page.getSelectedYear(), report.type());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF generation failed");
        }
    }

    private void writePdfDownload(byte[] body, String filename, String contentType, HttpServletResponse response,
            Long userId, int year, String type) throws IOException {
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(body.length);
        try {
            response.getOutputStream().write(body);
            response.flushBuffer();
        } catch (IOException ex) {
            LOGGER.warn("tax-return.pdf.export userId={} year={} type={} result=transfer-failed", userId, year, type);
            throw ex;
        }
        LOGGER.info("tax-return.pdf.export userId={} year={} type={} result=generated", userId, year, type);
    }

    private String writeBooksCsv(TaxReturnService.BooksExport export, String filenamePrefix, String outputType,
            Long userId, HttpServletResponse response, RedirectAttributes redirectAttributes)
            throws java.io.IOException {
        TaxReturnPageData page = export.page();
        if (!page.getErrors().isEmpty()) {
            List<String> messages = new java.util.ArrayList<>();
            messages.add("エラーがあるため出力できません。エラーチェックの内容をご確認ください。");
            messages.addAll(page.getErrors());
            redirectAttributes.addFlashAttribute("formErrors", messages);
            return redirect(page.getSelectedYear());
        }
        byte[] body = export.csv().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fileName = filenamePrefix + page.getSelectedYear() + ".csv";
        yearClosingService.recordOutput(userId, page, outputType, fileName, body, LocalDateTime.now(TOKYO));
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
        return null;
    }

    private String finish(List<String> errors, String successMessage, Integer year, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        if (errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", successMessage);
            return redirect(year);
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return render(request, response, model, year, null, errors);
    }

    @GetMapping("/tax-return")
    public String index(
            @RequestParam(value = "year", required = false) Integer year,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        return render(request, response, model, year, null, null);
    }

    @PostMapping("/tax-return/settlement")
    public String saveSettlement(
            @RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("settlementForm") SettlementForm form,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        List<String> errors = call(() -> taxReturnService.saveSettlement(context.getUserId(), year, form));
        if (errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", "決算整理を保存しました。");
            return redirect(year);
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return render(request, response, model, year, form, errors);
    }

    @PostMapping("/tax-return/filing-type")
    public String updateDeduction(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "deduction", required = false) String deduction,
            @RequestParam(value = "requirementsConfirmed", required = false, defaultValue = "false") boolean requirementsConfirmed,
            @RequestParam(value = "deduction65Method", required = false) String deduction65Method,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        List<String> errors = call(() -> taxReturnService.updateDeduction(context.getUserId(), year, deduction,
                requirementsConfirmed, deduction65Method));
        if (errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", "青色申告特別控除を更新しました。");
            return redirect(year);
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return render(request, response, model, year, null, errors);
    }

    @PostMapping("/tax-return/retention")
    public String addRetentionRecord(
            @RequestParam(value = "year", required = false) Integer year,
            @ModelAttribute("retentionRecordForm") RetentionRecordForm form,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        List<String> errors = call(() -> retentionService.add(context.getUserId(), year, form));
        if (errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", "保存管理台帳に登録しました。");
        } else {
            redirectAttributes.addFlashAttribute("formErrors", errors);
        }
        return redirect(year);
    }

    @PostMapping("/tax-return/evidence/google/connect")
    public String connectGoogleDrive(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        try {
            AuthorizationStart start = evidenceFileService.beginAuthorization();
            request.getSession().setAttribute(OAUTH_PENDING, new PendingAuthorization(context.getUserId(),
                    start.state(), start.codeVerifier(), year, System.currentTimeMillis() + OAUTH_TTL_MILLIS));
            return "redirect:" + start.url();
        } catch (EvidenceException ex) {
            redirectAttributes.addFlashAttribute("formErrors", List.of(ex.getMessage()));
            return retentionRedirect(year);
        }
    }

    @GetMapping("/tax-return/evidence/google/callback")
    public String googleDriveCallback(@RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = currentUser(request);
        Object attribute = request.getSession().getAttribute(OAUTH_PENDING);
        request.getSession().removeAttribute(OAUTH_PENDING);
        if (!(attribute instanceof PendingAuthorization pending) || state == null
                || !pending.userId().equals(context.getUserId())
                || System.currentTimeMillis() > pending.expiresAt()
                || !MessageDigest.isEqual(pending.state().getBytes(StandardCharsets.UTF_8),
                        state.getBytes(StandardCharsets.UTF_8))) {
            LOGGER.warn("evidence oauth callback rejected user={}", context.getUserId());
            redirectAttributes.addFlashAttribute("formErrors",
                    List.of("Google Drive 連携の確認に失敗しました。もう一度「Google Drive と連携」から操作してください。"));
            return "redirect:/tax-return#section-retention";
        }
        List<String> errors;
        if (error != null) {
            errors = List.of("Google Drive 連携がキャンセルされました。");
        } else {
            try {
                errors = evidenceFileService.completeAuthorization(context.getUserId(), code, pending.codeVerifier());
            } catch (EvidenceException ex) {
                errors = List.of(ex.getMessage());
            }
        }
        flash(redirectAttributes, errors, "Google Drive と連携しました。");
        return retentionRedirect(pending.year());
    }

    @PostMapping("/tax-return/evidence/google/disconnect")
    public String disconnectGoogleDrive(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        flash(redirectAttributes, evidenceFileService.disconnect(context.getUserId()),
                "Google Drive 連携を解除しました。保管済みファイルは Google Drive に残ります。");
        return retentionRedirect(year);
    }

    @PostMapping("/tax-return/retention/{id}/file")
    public String uploadEvidenceFile(@PathVariable("id") Long id,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request, RedirectAttributes redirectAttributes) throws IOException {
        AuthUserContext context = authorize(request, csrfToken);
        List<String> errors;
        if (file == null || file.isEmpty()) {
            errors = List.of("ファイルを選択してください。");
        } else if (file.getSize() > EvidenceUploadValidator.MAX_BYTES) {
            errors = List.of("ファイルサイズは10MB以下にしてください。");
        } else {
            byte[] content = file.getBytes();
            errors = evidence(() -> evidenceFileService.upload(context.getUserId(), id,
                    file.getOriginalFilename(), content));
        }
        flash(redirectAttributes, errors, "証憑ファイルを Google Drive に保管しました。");
        return retentionRedirect(year);
    }

    @GetMapping("/tax-return/retention/{id}/file")
    public String downloadEvidenceFile(@PathVariable("id") Long id,
            @RequestParam(value = "year", required = false) Integer year,
            HttpServletRequest request, HttpServletResponse response,
            RedirectAttributes redirectAttributes) throws IOException {
        AuthUserContext context = currentUser(request);
        EvidenceDownload download;
        try {
            download = call(() -> evidenceFileService.download(context.getUserId(), id));
        } catch (EvidenceException ex) {
            redirectAttributes.addFlashAttribute("formErrors", List.of(ex.getMessage()));
            return retentionRedirect(year);
        }
        response.setContentType(download.mimeType());
        response.setContentLength(download.content().length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + download.fileName() + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", "default-src 'none'; sandbox");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.getOutputStream().write(download.content());
        return null;
    }

    @PostMapping("/tax-return/retention/{id}/verify")
    public String verifyEvidenceFile(@PathVariable("id") Long id,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        flash(redirectAttributes, evidence(() -> evidenceFileService.verify(context.getUserId(), id)),
                "Google Drive 上の証憑ファイルを確認しました（存在・サイズ一致）。");
        return retentionRedirect(year);
    }

    @PostMapping("/tax-return/retention/{id}/delete")
    public String deleteRetentionRecord(@PathVariable("id") Long id,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        flash(redirectAttributes, evidence(() -> evidenceFileService.deleteExpired(context.getUserId(), id,
                LocalDate.now(TOKYO))), "保存期限を経過した台帳記録と証憑ファイルを削除しました。");
        return retentionRedirect(year);
    }

    @PostMapping("/tax-return/retention/{id}/hold")
    public String placeRetentionHold(@PathVariable("id") Long id,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        flash(redirectAttributes, call(() -> retentionService.placeLegalHold(context.getUserId(), id, reason)),
                "台帳記録を保全しました。保全中は保存期限を経過しても削除できません。");
        return retentionRedirect(year);
    }

    @PostMapping("/tax-return/retention/{id}/hold/release")
    public String releaseRetentionHold(@PathVariable("id") Long id,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        flash(redirectAttributes, call(() -> retentionService.releaseLegalHold(context.getUserId(), id)),
                "保全を解除しました。");
        return retentionRedirect(year);
    }

    @PostMapping("/tax-return/close")
    public String closeYear(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "warningsConfirmed", defaultValue = "false") boolean warningsConfirmed,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        flash(redirectAttributes, call(() -> yearClosingService.close(context.getUserId(), year, warningsConfirmed,
                reason, LocalDateTime.now(TOKYO))), "年度を締めました。締め後の変更には締めの取消（理由の記録）が必要です。");
        return closingRedirect(year);
    }

    @PostMapping("/tax-return/reopen")
    public String reopenYear(@RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        flash(redirectAttributes, call(() -> yearClosingService.reopen(context.getUserId(), year, reason,
                LocalDateTime.now(TOKYO))),
                "締めを取消しました。訂正後に再度締めてください。この締め版で出力した帳票は旧版として記録されています。");
        return closingRedirect(year);
    }

    @PostMapping("/tax-return/outputs/{id}/cancel")
    public String cancelOutputRecord(@PathVariable("id") Long id,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        flash(redirectAttributes, call(() -> yearClosingService.cancelOutput(context.getUserId(), id, reason,
                LocalDateTime.now(TOKYO))), "出力記録を取消しました。");
        return closingRedirect(year);
    }

    @ExceptionHandler(MultipartException.class)
    public String handleMultipartFailure(MultipartException ex, HttpServletRequest request) {
        LOGGER.warn("evidence upload rejected: multipart error");
        RequestContextUtils.getOutputFlashMap(request).put("formErrors",
                List.of("ファイルを受け付けられませんでした。10MB以下の PDF・JPEG・PNG を選択してください。"));
        return "redirect:/tax-return#section-retention";
    }

    private List<String> evidence(java.util.function.Supplier<List<String>> action) {
        try {
            return call(action);
        } catch (EvidenceException ex) {
            return List.of(ex.getMessage());
        }
    }

    private void flash(RedirectAttributes redirectAttributes, List<String> errors, String successMessage) {
        if (errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", successMessage);
        } else {
            redirectAttributes.addFlashAttribute("formErrors", errors);
        }
    }

    private String closingRedirect(Integer year) {
        return redirect(year) + "#section-closing";
    }

    private String retentionRedirect(Integer year) {
        return redirect(year) + "#section-retention";
    }

    private String render(
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            Integer year,
            SettlementForm overrideForm,
            List<String> formErrors) {
        AuthUserContext context = currentUser(request);
        TaxReturnPageData page = call(() -> taxReturnService.load(context.getUserId(), year, overrideForm));
        model.addAttribute("page", page);
        model.addAttribute("bookAccountOptions", taxReturnService.availableBookAccounts());
        RetentionSearch retentionSearch = new RetentionSearch(request.getParameter("rq"),
                request.getParameter("rkind"), request.getParameter("rstatus"));
        RetentionPageData retention = call(() -> retentionService.search(context.getUserId(), page.getSelectedYear(),
                retentionSearch, LocalDate.now(TOKYO)));
        retention.setDriveStatus(evidenceFileService.status(context.getUserId()));
        model.addAttribute("retention", retention);
        model.addAttribute("closing", call(() -> yearClosingService.view(context.getUserId(), page)));
        model.addAttribute("sections", TaxReturnSectionVisibility.resolve(
                settingService.loadHiddenTaxReturnSections(context.getUserId()), sectionsInUse(page, retention)));
        if (formErrors != null) {
            model.addAttribute("formErrors", formErrors);
        }
        model.addAttribute("csrfToken", csrfTokenManager.issue(request.getSession(), CSRF_SCOPE));
        model.addAttribute("activeMenu", "tax-return");
        model.addAttribute("currentUserDisplayName", context.getDisplayName());
        model.addAttribute("currentUserEmail", context.getEmail());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
        return "TaxReturn/tax-return";
    }

    /** 非表示設定でも表示を続ける（その年分に登録データ・適用がある）任意セクション。 */
    private static Set<TaxReturnSection> sectionsInUse(TaxReturnPageData page, RetentionPageData retention) {
        Set<TaxReturnSection> inUse = EnumSet.noneOf(TaxReturnSection.class);
        var extras = page.getExtras();
        if (extras != null) {
            if (extras.getAssets() != null && !extras.getAssets().isEmpty()) {
                inUse.add(TaxReturnSection.FIXED_ASSETS);
            }
            if (extras.getHouseholdConsumptionRows() != null && !extras.getHouseholdConsumptionRows().isEmpty()) {
                inUse.add(TaxReturnSection.HOUSEHOLD_CONSUMPTION);
            }
            if (extras.getEmployeeSalaryRows() != null && !extras.getEmployeeSalaryRows().isEmpty()) {
                inUse.add(TaxReturnSection.SALARY_DETAILS);
            }
            if (extras.isConsumptionTaxable()) {
                inUse.add(TaxReturnSection.CONSUMPTION);
            }
        }
        if (retention != null && (retention.getTotalCount() > 0
                || (retention.getSearch() != null && retention.getSearch().isFiltered()))) {
            inUse.add(TaxReturnSection.RETENTION);
        }
        return inUse;
    }

    private String redirect(Integer year) {
        return year == null ? "redirect:/tax-return" : "redirect:/tax-return?year=" + year;
    }

    private AuthUserContext authorize(HttpServletRequest request, String csrfToken) {
        AuthUserContext context = currentUser(request);
        if (!csrfTokenManager.isValid(request.getSession(), CSRF_SCOPE, csrfToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return context;
    }

    private AuthUserContext currentUser(HttpServletRequest request) {
        AuthUserContext context = (AuthUserContext) request.getSession()
                .getAttribute(AuthSessionAttributes.CURRENT_USER);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return context;
    }

    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (EntryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        }
    }

    private record PendingAuthorization(Long userId, String state, String codeVerifier, Integer year,
            long expiresAt) implements java.io.Serializable {
        @Override
        public String toString() {
            return "PendingAuthorization[]";
        }
    }

    private record PdfReport(String type, String title, String fileName) {
    }

    private record RenderedTaxReturnPage(String html, TaxReturnPageData page, ClosingPageData closing) {
    }

    private static final class CapturingResponse extends HttpServletResponseWrapper {
        private final PrintWriter writer;

        private CapturingResponse(HttpServletResponse response, StringWriter buffer) {
            super(response);
            writer = new PrintWriter(buffer);
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            throw new IllegalStateException("JSP rendering requires character output");
        }

        @Override
        public String getCharacterEncoding() {
            return StandardCharsets.UTF_8.name();
        }

        @Override
        public void setContentType(String contentType) {
        }

        @Override
        public void setCharacterEncoding(String charset) {
        }

        @Override
        public void flushBuffer() {
            writer.flush();
        }

        @Override
        public boolean isCommitted() {
            return false;
        }
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final int maximumBytes;

        private BoundedOutputStream(int maximumBytes) {
            this.maximumBytes = maximumBytes;
        }

        @Override
        public void write(int value) throws IOException {
            ensureCapacity(1);
            buffer.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            ensureCapacity(length);
            buffer.write(bytes, offset, length);
        }

        private void ensureCapacity(int additionalBytes) throws PdfArchiveLimitExceededException {
            if (additionalBytes < 0 || buffer.size() > maximumBytes - additionalBytes) {
                throw new PdfArchiveLimitExceededException();
            }
        }

        private byte[] toByteArray() {
            return buffer.toByteArray();
        }
    }

    private static final class PdfArchiveLimitExceededException extends IOException {
        private static final long serialVersionUID = 1L;
    }
}
