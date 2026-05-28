package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.features.auth.AuthSessionAttributes;
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
    public String login(Model model, HttpSession session) {
        if (isLoggedIn(session)) {
            return "redirect:/";
        }
        model.addAttribute("headerTitle", "ログイン");
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        return "Auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@ModelAttribute LoginForm form, Model model, HttpSession session) {
        AuthOperationResult result = authService.login(form);
        if (!result.isSuccess()) {
            model.addAttribute("headerTitle", "ログイン");
            model.addAttribute("failureMessage", result.getFirstErrorMessage());
            model.addAttribute("validationErrors", result.getErrors());
            model.addAttribute("loginForm", form);
            return "Auth/login";
        }

        session.setAttribute(AuthSessionAttributes.USER_ID, result.getUserId());
        session.setAttribute(AuthSessionAttributes.USER_EMAIL, result.getEmail());
        session.setAttribute(AuthSessionAttributes.USER_DISPLAY_NAME, result.getDisplayName());
        return "redirect:/";
    }

    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        if (isLoggedIn(session)) {
            return "redirect:/";
        }
        model.addAttribute("headerTitle", "新規登録");
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new UserRegisterForm());
        }
        return "Auth/user-register";
    }

    @PostMapping("/register")
    public String registerSubmit(@ModelAttribute("registerForm") UserRegisterForm form, Model model, RedirectAttributes redirectAttributes) {
        AuthOperationResult result = authService.register(form);
        if (!result.isSuccess()) {
            model.addAttribute("headerTitle", "新規登録");
            model.addAttribute("failureMessage", result.getFirstErrorMessage());
            model.addAttribute("validationErrors", result.getErrors());
            return "Auth/user-register";
        }

        redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(AuthSessionAttributes.USER_ID) instanceof Long;
    }
}

