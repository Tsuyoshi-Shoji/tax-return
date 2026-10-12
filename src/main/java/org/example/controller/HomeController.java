package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.home.dto.HomeViewDto;
import org.example.features.home.service.HomeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(
            @RequestParam(value = "fiscalYearId", required = false) Long fiscalYearId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        AuthUserContext context = (AuthUserContext) request.getSession()
                .getAttribute(AuthSessionAttributes.CURRENT_USER);
        try {
            HomeViewDto view = homeService.getHomeView(context.getUserId(), fiscalYearId);
            model.addAttribute("homeView", view);
            model.addAttribute("activeMenu", "home");
            model.addAttribute("currentUserDisplayName", context.getDisplayName());
            model.addAttribute("currentUserEmail", context.getEmail());
            return "Home/home";
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        } catch (IllegalStateException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            model.addAttribute("failureMessage", "ホーム画面の表示に必要な初期データが不足しています。");
            model.addAttribute("activeMenu", "home");
            return "Home/home";
        }
    }
}
