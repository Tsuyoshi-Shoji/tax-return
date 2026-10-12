package org.example.controller;

import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.report.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    public String index(
            @RequestParam(value = "year", required = false) Integer year,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        AuthUserContext context = currentUser(request);
        try {
            model.addAttribute("page", reportService.loadReport(context.getUserId(), year));
        } catch (EntryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        }
        prepare(model, response, context);
        return "Report/report";
    }

    @GetMapping("/report/detail")
    public String detail(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        AuthUserContext context = currentUser(request);
        try {
            model.addAttribute("page", reportService.loadDetail(context.getUserId(), year, month));
        } catch (EntryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad Request");
        }
        prepare(model, response, context);
        return "Report/report-detail";
    }

    private void prepare(Model model, HttpServletResponse response, AuthUserContext context) {
        model.addAttribute("activeMenu", "report");
        model.addAttribute("currentUserDisplayName", context.getDisplayName());
        model.addAttribute("currentUserEmail", context.getEmail());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
    }

    private AuthUserContext currentUser(HttpServletRequest request) {
        AuthUserContext context = (AuthUserContext) request.getSession()
                .getAttribute(AuthSessionAttributes.CURRENT_USER);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return context;
    }
}