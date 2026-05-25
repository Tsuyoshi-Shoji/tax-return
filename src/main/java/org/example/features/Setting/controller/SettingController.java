package org.example.features.setting.controller;

import org.example.features.setting.dto.SettingOperationResult;
import org.example.features.setting.form.AccountActionForm;
import org.example.features.setting.form.BusinessUseRatioForm;
import org.example.features.setting.form.PasswordForm;
import org.example.features.setting.form.ProfileForm;
import org.example.features.setting.service.SettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("headerTitle", "設定");
        model.addAttribute("setting", settingService.getCurrentSetting());
        return "Setting/settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(
            @ModelAttribute ProfileForm form,
            RedirectAttributes redirectAttributes) {
        addResultAttributes(settingService.updateProfile(form), redirectAttributes);
        return "redirect:/settings";
    }

    @PostMapping("/settings/password")
    public String changePassword(
            @ModelAttribute PasswordForm form,
            RedirectAttributes redirectAttributes) {
        addResultAttributes(settingService.changePassword(form), redirectAttributes);
        return "redirect:/settings";
    }

    @PostMapping("/settings/business-use-ratio")
    public String updateBusinessUseRatio(
            @ModelAttribute BusinessUseRatioForm form,
            RedirectAttributes redirectAttributes) {
        addResultAttributes(settingService.updateBusinessUseRatio(form), redirectAttributes);
        return "redirect:/settings";
    }

    @PostMapping("/settings/disable")
    public String disableAccount(
            @ModelAttribute AccountActionForm form,
            RedirectAttributes redirectAttributes) {
        addResultAttributes(settingService.disableAccount(form), redirectAttributes);
        return "redirect:/settings";
    }

    @PostMapping("/settings/delete")
    public String deleteAccount(
            @ModelAttribute AccountActionForm form,
            RedirectAttributes redirectAttributes) {
        addResultAttributes(settingService.deleteAccount(form), redirectAttributes);
        return "redirect:/settings";
    }

    private void addResultAttributes(
            SettingOperationResult result,
            RedirectAttributes redirectAttributes) {
        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("failureMessage", result.getFirstErrorMessage());
            redirectAttributes.addFlashAttribute("validationErrors", result.getErrors());
        }
    }
}

