package org.example.controller;

import org.example.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final HomeService homeService;

    @Autowired
    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", homeService.getWelcomeMessage());
        model.addAttribute("serverTime", homeService.getCurrentTime());
        return "home";
    }

    @GetMapping("/return-form")
    public String returnForm() {
        return "return-form";
    }

    @GetMapping("/income")
    public String income() {
        return "income";
    }

    @GetMapping("/expense")
    public String expense() {
        return "expense";
    }

    @GetMapping("/profit-loss")
    public String profitLoss() {
        return "profit-loss";
    }

    @GetMapping("/report")
    public String report() {
        return "report";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}