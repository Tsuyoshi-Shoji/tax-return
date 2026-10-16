package org.example.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.common.session.FormCsrfTokenManager;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.profitloss.dto.ProfitLossDetailDto;
import org.example.features.profitloss.dto.ProfitLossPageData;
import org.example.features.profitloss.form.ProfitLossSearchForm;
import org.example.features.profitloss.form.ProfitLossSearchState;
import org.example.features.profitloss.service.ProfitLossService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ProfitLossController {
    private static final String CSRF_SCOPE = "profit-loss-delete";

    private final ProfitLossService profitLossService;
    private final FormCsrfTokenManager csrfTokenManager;

    public ProfitLossController(ProfitLossService profitLossService, FormCsrfTokenManager csrfTokenManager) {
        this.profitLossService = profitLossService;
        this.csrfTokenManager = csrfTokenManager;
    }

    @GetMapping("/profit-loss")
    public String index(
            @ModelAttribute("searchForm") ProfitLossSearchForm searchForm,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        AuthUserContext context = (AuthUserContext) request.getSession()
                .getAttribute(AuthSessionAttributes.CURRENT_USER);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        try {
            ProfitLossPageData page = profitLossService.loadPage(context.getUserId(), searchForm);
            model.addAttribute("page", page);
            model.addAttribute("activeMenu", "profit-loss");
            model.addAttribute("currentUserDisplayName", context.getDisplayName());
            model.addAttribute("currentUserEmail", context.getEmail());
            model.addAttribute("returnQuery", ProfitLossSearchState.toQuery(page.getForm()));
            applyNoStore(response);
            return "ProfitLoss/profit-loss";
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    @GetMapping("/profit-loss/detail")
    public String detail(
            @RequestParam("recordType") String recordType,
            @RequestParam("id") Long id,
            @RequestParam(value = "returnQuery", required = false) String returnQuery,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        AuthUserContext context = currentUser(request);
        try {
            ProfitLossDetailDto detail = profitLossService.loadDetail(context.getUserId(), recordType, id);
            model.addAttribute("detail", detail);
            model.addAttribute("csrfToken", csrfTokenManager.issue(request.getSession(), CSRF_SCOPE));
            model.addAttribute("returnQuery", ProfitLossSearchState.normalizeQuery(returnQuery));
            model.addAttribute("returnUrl", "/profit-loss" + querySuffix(returnQuery));
            model.addAttribute("activeMenu", "profit-loss");
            model.addAttribute("currentUserDisplayName", context.getDisplayName());
            model.addAttribute("currentUserEmail", context.getEmail());
            applyNoStore(response);
            return "ProfitLoss/profit-loss-detail";
        } catch (EntryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        }
    }

    @PostMapping("/profit-loss/delete")
    public String delete(
            @RequestParam("recordType") String recordType,
            @RequestParam("id") Long id,
            @RequestParam(value = "csrfToken", required = false) String csrfToken,
            @RequestParam(value = "returnQuery", required = false) String returnQuery,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = currentUser(request);
        if (!csrfTokenManager.isValid(request.getSession(), CSRF_SCOPE, csrfToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        try {
            profitLossService.delete(context.getUserId(), recordType, id);
            redirectAttributes.addFlashAttribute("flashMessage", "データを削除しました。");
            return "redirect:/profit-loss" + querySuffix(returnQuery);
        } catch (EntryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        } catch (EntryConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/profit-loss/detail?recordType=" + ("income".equals(recordType) ? "income" : "expense")
                    + "&id=" + id + returnQueryParameter(returnQuery);
        }
    }

    private String querySuffix(String query) {
        String normalized = ProfitLossSearchState.normalizeQuery(query);
        return normalized.isEmpty() ? "" : "?" + normalized;
    }

    private String returnQueryParameter(String query) {
        String normalized = ProfitLossSearchState.normalizeQuery(query);
        return normalized.isEmpty() ? "" : "&returnQuery=" + URLEncoder.encode(normalized, StandardCharsets.UTF_8);
    }

    private AuthUserContext currentUser(HttpServletRequest request) {
        AuthUserContext context = (AuthUserContext) request.getSession()
                .getAttribute(AuthSessionAttributes.CURRENT_USER);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return context;
    }

    private void applyNoStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
    }
}
