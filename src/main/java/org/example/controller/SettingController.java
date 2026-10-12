package org.example.controller;

import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.common.session.FormCsrfTokenManager;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.setting.dto.SettingOperationResult;
import org.example.features.setting.form.AccountActionForm;
import org.example.features.setting.form.BusinessUseRatioForm;
import org.example.features.setting.form.PasswordForm;
import org.example.features.setting.form.ProfileForm;
import org.example.features.setting.service.SettingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class SettingController {
    private static final String CSRF_SCOPE = "settings";

    private final SettingService settingService;
    private final FormCsrfTokenManager csrfTokenManager;

    public SettingController(SettingService settingService, FormCsrfTokenManager csrfTokenManager) {
        this.settingService = settingService;
        this.csrfTokenManager = csrfTokenManager;
    }

    @InitBinder("profileForm")
    void bindProfile(WebDataBinder binder) {
        binder.setAllowedFields("displayName", "csrfToken");
    }

    @InitBinder("passwordForm")
    void bindPassword(WebDataBinder binder) {
        binder.setAllowedFields("currentPassword", "newPassword", "confirmPassword", "csrfToken");
    }

    @InitBinder("ratioForm")
    void bindRatio(WebDataBinder binder) {
        binder.setAllowedFields("ratio", "csrfToken");
    }

    @InitBinder({"disableForm", "deleteForm"})
    void bindAccountAction(WebDataBinder binder) {
        binder.setAllowedFields("confirmText", "currentPassword", "csrfToken");
    }

    @GetMapping("/settings")
    public String index(HttpServletRequest request, HttpServletResponse response, Model model) {
        return render(request, response, model, null, null);
    }

    @PostMapping("/settings/profile")
    public String profile(
            @ModelAttribute("profileForm") ProfileForm form,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        SettingOperationResult result = call(() -> settingService.updateProfile(context.getUserId(), form));
        if (result.isSuccess()) {
            request.getSession().setAttribute(AuthSessionAttributes.CURRENT_USER,
                    new AuthUserContext(context.getUserId(), result.getUpdatedDisplayName(), context.getEmail()));
            return success(redirectAttributes, result);
        }
        form.setCsrfToken(null);
        return render(request, response, model, "profile", result);
    }

    @PostMapping("/settings/password")
    public String password(
            @ModelAttribute("passwordForm") PasswordForm form,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        SettingOperationResult result = call(() -> settingService.changePassword(context.getUserId(), form));
        if (result.isSuccess()) {
            request.changeSessionId();
            return success(redirectAttributes, result);
        }
        model.addAttribute("passwordForm", new PasswordForm());
        return render(request, response, model, "password", result);
    }

    @PostMapping("/settings/business-use-ratio")
    public String businessUseRatio(
            @ModelAttribute("ratioForm") BusinessUseRatioForm form,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        SettingOperationResult result = call(() -> settingService.updateBusinessUseRatio(context.getUserId(), form));
        if (result.isSuccess()) {
            return success(redirectAttributes, result);
        }
        return render(request, response, model, "ratio", result);
    }

    @PostMapping("/settings/disable")
    public String disable(
            @ModelAttribute("disableForm") AccountActionForm form,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        SettingOperationResult result = call(() -> settingService.disableAccount(context.getUserId(), form));
        if (result.isSuccess()) {
            return endSession(request, redirectAttributes);
        }
        model.addAttribute("disableForm", new AccountActionForm());
        return render(request, response, model, "disable", result);
    }

    @PostMapping("/settings/delete")
    public String delete(
            @ModelAttribute("deleteForm") AccountActionForm form,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        AuthUserContext context = authorize(request, form.getCsrfToken());
        SettingOperationResult result = call(() -> settingService.deleteAccount(context.getUserId(), form));
        if (result.isSuccess()) {
            return endSession(request, redirectAttributes);
        }
        model.addAttribute("deleteForm", new AccountActionForm());
        return render(request, response, model, "delete", result);
    }

    private String render(
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            String failedSection,
            SettingOperationResult result) {
        AuthUserContext context = currentUser(request);
        var view = call(() -> settingService.load(context.getUserId()));
        model.addAttribute("view", view);
        if (!model.containsAttribute("profileForm")) {
            ProfileForm profileForm = new ProfileForm();
            profileForm.setDisplayName(view.getDisplayName());
            model.addAttribute("profileForm", profileForm);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordForm());
        }
        if (!model.containsAttribute("ratioForm")) {
            BusinessUseRatioForm ratioForm = new BusinessUseRatioForm();
            ratioForm.setRatio(view.getApportionmentRate().toPlainString());
            model.addAttribute("ratioForm", ratioForm);
        }
        if (!model.containsAttribute("disableForm")) {
            model.addAttribute("disableForm", new AccountActionForm());
        }
        if (!model.containsAttribute("deleteForm")) {
            model.addAttribute("deleteForm", new AccountActionForm());
        }
        if (failedSection != null && result != null) {
            model.addAttribute("failedSection", failedSection);
            model.addAttribute("formErrors", result.getErrors());
        }
        model.addAttribute("csrfToken", csrfTokenManager.issue(request.getSession(), CSRF_SCOPE));
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("currentUserDisplayName", context.getDisplayName());
        model.addAttribute("currentUserEmail", context.getEmail());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
        return "Setting/settings";
    }

    private String success(RedirectAttributes redirectAttributes, SettingOperationResult result) {
        redirectAttributes.addFlashAttribute("flashMessage", result.getMessage());
        return "redirect:/settings";
    }

    private String endSession(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
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