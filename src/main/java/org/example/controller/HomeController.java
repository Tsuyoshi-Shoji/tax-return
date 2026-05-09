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
        model.addAttribute("headerTitle", "ホーム");
        return "Home/home";
    }

    @GetMapping("/return-form")
    public String returnForm(Model model) {
        model.addAttribute("headerTitle", "申告書作成");
        return "ReturnForm/return-form";
    }

    @GetMapping("/income")
    public String income(Model model) {
        model.addAttribute("headerTitle", "収益登録");
        return "Income/income";
    }

    @GetMapping("/expense")
    public String expense(Model model) {
        model.addAttribute("headerTitle", "経費登録");
        return "Expense/expense";
    }

    @GetMapping("/profit-loss")
    public String profitLoss(Model model) {
        model.addAttribute("headerTitle", "損益一覧");
        return "ProfitLoss/profit-loss";
    }

    @GetMapping("/report")
    public String report(Model model) {
        model.addAttribute("headerTitle", "損益レポート");
        return "Report/report";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("headerTitle", "設定");
        return "Setting/settings";
    }
}