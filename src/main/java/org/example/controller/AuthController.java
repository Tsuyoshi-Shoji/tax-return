package org.example.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.auth.dto.AuthOperationResult;
import org.example.features.auth.form.LoginForm;
import org.example.features.auth.form.UserRegisterForm;
import org.example.features.auth.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String showLogin(
            Model model,
            HttpServletRequest request,
            @ModelAttribute("successMessage") String successMessage) {
        if (request.getSession(false) != null
                && request.getSession(false).getAttribute(AuthSessionAttributes.CURRENT_USER) != null) {
            return "redirect:/home";
        }
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        if (successMessage != null && !successMessage.isBlank()) {
            model.addAttribute("successMessage", successMessage);
        }
        return "Auth/login";
    }

    @PostMapping("/login")
    public String login(
            @ModelAttribute("loginForm") LoginForm loginForm,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthOperationResult result = authService.login(loginForm);
        if (!result.isSuccess()) {
            response.setStatus(result.getStatusCode());
            applyFailure(model, "loginForm", loginForm, result.getMessage(), result.getValidationErrors());
            return "Auth/login";
        }
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession session = request.getSession(true);
        AuthUserContext context = result.getUserContext();
        session.setAttribute(AuthSessionAttributes.CURRENT_USER, context);
        int timeout = loginForm.isRememberMe() ? 60 * 60 * 24 * 14 : 60 * 30;
        session.setMaxInactiveInterval(timeout);
        return "redirect:/home";
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new UserRegisterForm());
        }
        return "Auth/user-register";
    }

    @PostMapping("/register")
    public String register(
            @ModelAttribute("registerForm") UserRegisterForm registerForm,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        AuthOperationResult result = authService.register(registerForm);
        if (!result.isSuccess()) {
            response.setStatus(result.getStatusCode());
            applyFailure(model, "registerForm", registerForm, result.getMessage(), result.getValidationErrors());
            return "Auth/user-register";
        }
        redirectAttributes.addFlashAttribute("successMessage", "ユーザー登録が完了しました。ログインしてください。");
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    private void applyFailure(Model model, String formKey, Object form, String message, List<String> errors) {
        model.addAttribute(formKey, form);
        model.addAttribute("failureMessage", message);
        model.addAttribute("validationErrors", errors);
    }
}
