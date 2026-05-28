package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.features.auth.AuthSessionAttributes;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addCurrentUserAttributes(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object displayName = session == null ? null : session.getAttribute(AuthSessionAttributes.USER_DISPLAY_NAME);
        Object email = session == null ? null : session.getAttribute(AuthSessionAttributes.USER_EMAIL);
        model.addAttribute("currentUserDisplayName", displayName == null ? "" : displayName);
        model.addAttribute("currentUserEmail", email == null ? "" : email);
    }
}

