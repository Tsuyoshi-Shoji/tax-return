package org.example.controller;

import java.util.List;

import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.common.session.FormCsrfTokenManager;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.form.SettlementForm;
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

    public TaxReturnController(TaxReturnService taxReturnService, FormCsrfTokenManager csrfTokenManager) {
        this.taxReturnService = taxReturnService;
        this.csrfTokenManager = csrfTokenManager;
    }

    @InitBinder("settlementForm")
    void bindSettlement(WebDataBinder binder) {
        binder.setAllowedFields("openingInventory", "closingInventory", "depreciation", "accountsReceivable",
                "accountsPayable", "unpaidExpenses", "unreceivedIncome", "openingBalance", "closingCash", "csrfToken");
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
