package org.example.controller;

import java.util.List;

import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.common.session.FormCsrfTokenManager;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.export.EtaxAssistCsv;
import org.example.features.taxreturn.form.SettlementForm;
import org.example.features.taxreturn.form.TaxReturnForms.DeductionForm;
import org.example.features.taxreturn.form.TaxReturnForms.DependentForm;
import org.example.features.taxreturn.form.TaxReturnForms.FixedAssetForm;
import org.example.features.taxreturn.form.TaxReturnForms.ReturnInputForm;
import org.example.features.taxreturn.input.TaxReturnInputService;
import org.example.features.taxreturn.service.TaxReturnService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class TaxReturnController {
    private static final String CSRF_SCOPE = "tax-return";

    private final TaxReturnService taxReturnService;
    private final FormCsrfTokenManager csrfTokenManager;
    private final TaxReturnInputService inputService;

    public TaxReturnController(TaxReturnService taxReturnService, FormCsrfTokenManager csrfTokenManager,
            TaxReturnInputService inputService) {
        this.taxReturnService = taxReturnService;
        this.csrfTokenManager = csrfTokenManager;
        this.inputService = inputService;
    }

    @InitBinder("settlementForm")
    void bindSettlement(WebDataBinder binder) {
        binder.setAllowedFields("openingInventory", "closingInventory", "accountsReceivable",
                "accountsPayable", "unpaidExpenses", "unreceivedIncome", "openingBalance", "closingCash", "csrfToken");
    }

    @InitBinder("fixedAssetForm")
    void bindFixedAsset(WebDataBinder binder) {
        binder.setAllowedFields("assetName", "accountCode", "acquisitionDate", "serviceStartDate", "acquisitionCost",
                "usefulLife", "depreciationMethod", "businessRatio", "acquisitionFunding", "notes", "csrfToken");
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

    @InitBinder("deductionForm")
    void bindDeduction(WebDataBinder binder) {
        binder.setAllowedFields("deductionType", "paidDate", "paidAmount", "reimbursedAmount", "payer", "notes", "csrfToken");
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
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"tax-return_etax-assist_" + page.getSelectedYear() + ".csv\"");
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
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, csrfToken);
        List<String> errors = call(() -> taxReturnService.updateDeduction(context.getUserId(), year, deduction));
        if (errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", "青色申告特別控除を更新しました。");
            return redirect(year);
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return render(request, response, model, year, null, errors);
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
}
