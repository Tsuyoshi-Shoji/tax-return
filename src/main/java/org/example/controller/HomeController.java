package org.example.controller;

import org.example.features.home.service.HomeService;
import org.example.features.expense.dto.ExpenseEditView;
import org.example.features.expense.dto.ExpenseRegistrationResult;
import org.example.features.expense.form.ExpenseForm;
import org.example.features.expense.service.ExpenseService;
import org.example.features.income.dto.IncomeEditView;
import org.example.features.income.dto.IncomeRegistrationResult;
import org.example.features.income.form.IncomeForm;
import org.example.features.income.service.IncomeService;
import org.example.features.profitloss.dto.ProfitLossDetailView;
import org.example.features.profitloss.dto.ProfitLossSearchResult;
import org.example.features.profitloss.form.ProfitLossSearchForm;
import org.example.features.profitloss.service.ProfitLossService;
import org.example.features.report.dto.ReportDetailView;
import org.example.features.report.dto.ReportView;
import org.example.features.report.service.ReportService;
import org.example.features.returnform.dto.ReturnFormView;
import org.example.features.returnform.service.ReturnFormService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

@Controller
public class HomeController {

    private final HomeService homeService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final ProfitLossService profitLossService;
    private final ReportService reportService;
    private final ReturnFormService returnFormService;

    public HomeController(
            HomeService homeService,
            ExpenseService expenseService,
            IncomeService incomeService,
            ProfitLossService profitLossService,
            ReportService reportService,
            ReturnFormService returnFormService) {
        this.homeService = homeService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.profitLossService = profitLossService;
        this.reportService = reportService;
        this.returnFormService = returnFormService;
    }

    @GetMapping({"/home", "", "/"})
    public String home(Model model) {
        model.addAttribute("message", homeService.getWelcomeMessage());
        model.addAttribute("serverTime", homeService.getCurrentTime());
        model.addAttribute("headerTitle", "ホーム");

        // README仕様のデータを追加
        model.addAttribute("recentTransactions", homeService.getRecentTransactions(5));
        model.addAttribute("annualAllProfitLoss", homeService.getAnnualProfitLoss(true));
        model.addAttribute("monthlyAllProfitLoss", homeService.getMonthlyProfitLoss(true));
        model.addAttribute("annualBusinessProfitLoss", homeService.getAnnualProfitLoss(false));
        model.addAttribute("monthlyBusinessProfitLoss", homeService.getMonthlyProfitLoss(false));
        model.addAttribute("daysUntilDeadline", homeService.getDaysUntilTaxDeadline());
        model.addAttribute("isApproachingDeadline", homeService.isApproachingDeadline());

        return "Home/home";
    }

    @GetMapping("/return-form")
    public String returnForm(Model model) {
        model.addAttribute("headerTitle", "申告書作成");
        ReturnFormView returnFormView = returnFormService.getCurrentYearSummary();
        model.addAttribute("returnFormView", returnFormView);
        return "ReturnForm/return-form";
    }

    @GetMapping("/income")
    public String income(
            Model model,
            @RequestParam(name = "editId", required = false) Long editId) {
        addIncomeViewAttributes(model, editId);
        return "Income/income";
    }

    @GetMapping("/expense")
    public String expense(
            Model model,
            @RequestParam(name = "editId", required = false) Long editId) {
        addExpenseViewAttributes(model, editId);
        return "Expense/expense";
    }

    @PostMapping("/expense")
    public String registerExpense(@ModelAttribute ExpenseForm form, Model model, RedirectAttributes redirectAttributes) {
        ExpenseRegistrationResult result = expenseService.registerExpense(form);

        if (result.isSuccess()) {
            if (form.getId() != null && "detail".equals(form.getReturnTo())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "支出を更新しました。金額: " + NumberFormat.getNumberInstance(Locale.JAPAN).format(result.getAmount()) + "円");
                return "redirect:/profit-loss/detail?recordType=expense&id=" + form.getId();
            }
            addExpenseViewAttributes(model, null);
            NumberFormat yenFormat = NumberFormat.getNumberInstance(Locale.JAPAN);
            model.addAttribute(
                    "successMessage",
                    (form.getId() == null ? "支出を登録しました。金額: " : "支出を更新しました。金額: ")
                            + yenFormat.format(result.getAmount())
                            + "円、経費対象金額: "
                            + yenFormat.format(result.getDeductibleAmount())
                            + "円");
        } else {
            addExpenseViewAttributes(model, null);
            model.addAttribute("failureMessage", result.getFirstErrorMessage());
            model.addAttribute("validationErrors", result.getErrors());
            model.addAttribute("expenseForm", form);
        }

        return "Expense/expense";
    }

    @PostMapping("/income")
    public String registerIncome(@ModelAttribute IncomeForm form, Model model, RedirectAttributes redirectAttributes) {
        IncomeRegistrationResult result = incomeService.registerIncome(form);

        if (result.isSuccess()) {
            if (form.getId() != null && "detail".equals(form.getReturnTo())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "収益を更新しました。金額: " + NumberFormat.getNumberInstance(Locale.JAPAN).format(result.getAmount()) + "円");
                return "redirect:/profit-loss/detail?recordType=income&id=" + form.getId();
            }
            addIncomeViewAttributes(model, null);
            NumberFormat yenFormat = NumberFormat.getNumberInstance(Locale.JAPAN);
            model.addAttribute(
                    "successMessage",
                    (form.getId() == null ? "収益を登録しました。金額: " : "収益を更新しました。金額: ")
                            + yenFormat.format(result.getAmount())
                            + "円");
        } else {
            addIncomeViewAttributes(model, null);
            model.addAttribute("failureMessage", result.getFirstErrorMessage());
            model.addAttribute("validationErrors", result.getErrors());
            model.addAttribute("incomeForm", form);
        }

        return "Income/income";
    }

    private void addExpenseViewAttributes(Model model, Long editId) {
        model.addAttribute("headerTitle", "支出登録");
        model.addAttribute("defaultBusinessUseRatio", expenseService.getDefaultBusinessUseRatio());
        if (editId != null) {
            ExpenseEditView editView = expenseService.getExpenseEditView(editId);
            model.addAttribute("expenseEdit", editView);
        }
    }

    private void addIncomeViewAttributes(Model model, Long editId) {
        model.addAttribute("headerTitle", "収益登録");
        if (editId != null) {
            IncomeEditView editView = incomeService.getIncomeEditView(editId);
            model.addAttribute("incomeEdit", editView);
        }
    }

    @GetMapping("/profit-loss")
    public String profitLoss(@ModelAttribute ProfitLossSearchForm form, Model model) {
        model.addAttribute("headerTitle", "損益一覧");
        model.addAttribute("form", form);
        ProfitLossSearchResult result = profitLossService.search(form);
        model.addAttribute("totalIncome", result.totalIncome());
        model.addAttribute("totalExpense", result.totalExpense());
        model.addAttribute("profitAmount", result.profitAmount());
        model.addAttribute("incomeRows", result.incomeRows());
        model.addAttribute("expenseRows", result.expenseRows());
        model.addAttribute("incomeCurrentPage", result.incomeCurrentPage());
        model.addAttribute("incomeTotalPages", result.incomeTotalPages());
        model.addAttribute("expenseCurrentPage", result.expenseCurrentPage());
        model.addAttribute("expenseTotalPages", result.expenseTotalPages());
        return "ProfitLoss/profit-loss";
    }

    @GetMapping("/profit-loss/detail")
    public String profitLossDetail(
            @RequestParam("recordType") String recordType,
            @RequestParam("id") Long id,
            Model model) {
        model.addAttribute("headerTitle", "損益詳細");
        ProfitLossDetailView detail = profitLossService.getDetail(recordType, id);
        model.addAttribute("detail", detail);
        return "ProfitLoss/profit-loss-detail";
    }

    @PostMapping("/profit-loss/delete")
    public String deleteProfitLoss(
            @RequestParam("recordType") String recordType,
            @RequestParam("id") Long id,
            RedirectAttributes redirectAttributes) {
        profitLossService.delete(recordType, id);
        redirectAttributes.addFlashAttribute("successMessage", "対象データを削除しました。");
        return "redirect:/profit-loss";
    }

    @GetMapping("/report")
    public String report(
            @RequestParam(name = "year", required = false) Integer year,
            Model model) {
        model.addAttribute("headerTitle", "損益レポート");
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        model.addAttribute("reportViewAll", reportService.getYearlyReport(targetYear, true));
        model.addAttribute("reportViewBusiness", reportService.getYearlyReport(targetYear, false));
        return "Report/report";
    }

    @GetMapping("/report/detail")
    public String reportDetail(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            Model model) {
        model.addAttribute("headerTitle", "損益詳細レポート");
        model.addAttribute("reportDetailViewAll", reportService.getMonthlyReport(year, month, true));
        model.addAttribute("reportDetailViewBusiness", reportService.getMonthlyReport(year, month, false));
        return "Report/report-detail";
    }

}
