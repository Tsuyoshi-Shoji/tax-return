package org.example.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addCurrentUserAttributes(Model model) {
        model.addAttribute("currentUserDisplayName", "");
        model.addAttribute("currentUserEmail", "");
    }
}

