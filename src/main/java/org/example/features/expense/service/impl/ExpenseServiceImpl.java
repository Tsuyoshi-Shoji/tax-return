package org.example.features.expense.service.impl;

import org.example.entity.Expense;
import org.example.entity.ExpenseCategory;
import org.example.entity.ExpenseSubcategory;
import org.example.entity.PaymentMethod;
import org.example.entity.User;
import org.example.features.auth.AuthUserContext;
import org.example.features.expense.dto.ExpenseEditView;
import org.example.features.expense.dto.ExpenseRegistrationResult;
import org.example.features.expense.form.ExpenseForm;
import org.example.features.expense.service.ExpenseService;
import org.example.features.setting.service.SettingService;
import org.example.repository.ExpenseCategoryRepository;
import org.example.repository.ExpenseRepository;
import org.example.repository.ExpenseSubcategoryRepository;
import org.example.repository.PaymentMethodRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Pattern INVALID_DETAILS_CHARACTER_PATTERN = Pattern.compile("[<>\"'`\\\\]");

    private final SettingService settingService;
    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseSubcategoryRepository expenseSubcategoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;

    public ExpenseServiceImpl(
            SettingService settingService,
            ExpenseRepository expenseRepository,
            ExpenseCategoryRepository expenseCategoryRepository,
            ExpenseSubcategoryRepository expenseSubcategoryRepository,
            PaymentMethodRepository paymentMethodRepository,
            UserRepository userRepository) {
        this.settingService = settingService;
        this.expenseRepository = expenseRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.expenseSubcategoryRepository = expenseSubcategoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BigDecimal getDefaultBusinessUseRatio() {
        return settingService.getDefaultBusinessUseRatio();
    }

    @Override
    @Transactional
    public ExpenseRegistrationResult registerExpense(ExpenseForm form) {
        BigDecimal businessUseRatio = getDefaultBusinessUseRatio();
        ExpenseCategory category = resolveCategory(trimToEmpty(form.getExpenseType()));
        List<String> errors = validate(form, businessUseRatio, category);
        if (!errors.isEmpty()) {
            return ExpenseRegistrationResult.failure(errors, businessUseRatio);
        }
        if (category == null) {
            return ExpenseRegistrationResult.failure(List.of("区分が不正です。"), businessUseRatio);
         }
         ExpenseCategory resolvedCategory = category;

         User currentUser = getCurrentUser();
         ExpenseSubcategory subcategory = expenseSubcategoryRepository
                .findByIdAndExpenseCategoryIdAndStatus(parseLong(form.getCategory()), resolvedCategory.getId(), "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("選択された項目がマスタに存在しません。"));
        PaymentMethod paymentMethod = paymentMethodRepository.findById(parseLong(form.getPaymentMethodId()))
                .orElseThrow(() -> new IllegalArgumentException("支払方法がマスタに存在しません。"));
        Expense expense = form.getId() == null
                ? new Expense()
                : expenseRepository.findByIdAndUserIdAndDeletedFlagFalse(form.getId(), currentUser.getId())
                        .orElseThrow(() -> new IllegalArgumentException("更新対象の支出が見つかりません。"));

        BigDecimal amount = new BigDecimal(form.getAmount().trim());
        Boolean businessTarget = parseBusinessTarget(form.getBusinessTarget());
        BigDecimal deductibleAmount = calculateDeductibleAmount(amount, form.isHomeApportionment(), businessUseRatio, businessTarget);

        expense.setUser(currentUser);
        expense.setExpenseCategory(resolvedCategory);
        expense.setExpenseSubcategory(subcategory);
        expense.setPaymentMethod(paymentMethod);
        expense.setExpenseDate(LocalDate.parse(trimToEmpty(form.getDate())));
        expense.setAmount(amount);
        expense.setDescription(trimToEmpty(form.getDetails()));
        expense.setMemo(null);
        expense.setBusinessTarget(businessTarget);
        expense.setDeductibleAmount(deductibleAmount);
        expense.setDeletedFlag(Boolean.FALSE);
        expense.setDeletedAt(null);
        expenseRepository.save(expense);

        return ExpenseRegistrationResult.success(amount, deductibleAmount, businessUseRatio);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseEditView getExpenseEditView(Long id) {
        User currentUser = getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUserIdAndDeletedFlagFalse(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("対象の支出が見つかりません。"));
        String expenseType = expense.getExpenseCategory().getExpenseType().toLowerCase(Locale.ROOT);
        boolean homeApportionment = !Boolean.FALSE.equals(expense.getBusinessTarget())
                && expense.getDeductibleAmount() != null
                && expense.getDeductibleAmount().compareTo(BigDecimal.ZERO) > 0
                && expense.getDeductibleAmount().compareTo(expense.getAmount()) < 0;
        return new ExpenseEditView(
                expense.getId(),
                expenseType,
                expense.getExpenseSubcategory() == null ? null : expense.getExpenseSubcategory().getId(),
                expense.getExpenseDate().toString(),
                expense.getAmount().toPlainString(),
                expense.getDescription(),
                expense.getBusinessTarget(),
                homeApportionment,
                expense.getPaymentMethod() == null ? null : expense.getPaymentMethod().getId());
    }

    private List<String> validate(ExpenseForm form, BigDecimal businessUseRatio, ExpenseCategory expenseCategory) {
        List<String> errors = new ArrayList<>();
        User currentUser = getCurrentUser();

        String expenseType = trimToEmpty(form.getExpenseType());
        String categoryId = trimToEmpty(form.getCategory());
        String date = trimToEmpty(form.getDate());
        String amount = trimToEmpty(form.getAmount());
        String details = trimToEmpty(form.getDetails());
        String paymentMethodId = trimToEmpty(form.getPaymentMethodId());
        boolean profitLossTarget = !Boolean.FALSE.equals(parseBusinessTarget(form.getBusinessTarget()));

        if (expenseType.isEmpty()) {
            errors.add("区分を選択してください。");
        } else if (expenseCategory == null) {
            errors.add("区分が不正です。");
        }

        if (categoryId.isEmpty()) {
            errors.add("項目を選択してください。");
        } else if (!isValidSubcategory(expenseCategory, parseLongSafely(categoryId))) {
            errors.add("選択された項目がマスタに存在しません。");
        }

        if (paymentMethodId.isEmpty()) {
            errors.add("支払方法を選択してください。");
        } else if (parseLongSafely(paymentMethodId) == null || paymentMethodRepository.findById(parseLong(paymentMethodId)).isEmpty()) {
            errors.add("選択された支払方法がマスタに存在しません。");
        }

        if (date.isEmpty()) {
            errors.add("支出発生日を選択してください。");
        } else {
            validateExpenseDate(date, errors);
        }

        if (amount.isEmpty()) {
            errors.add("金額を入力してください。");
        } else if (!amount.matches("\\d+")) {
            errors.add("金額は数値のみで入力してください。");
        } else if (new BigDecimal(amount).compareTo(BigDecimal.ONE) < 0) {
            errors.add("金額は1円以上で入力してください。");
        }

        if (details.isEmpty()) {
            errors.add("支出詳細を入力してください。");
        } else if (details.length() > 255) {
            errors.add("支出詳細は1〜255文字で入力してください。");
        } else if (containsInvalidDetailsCharacter(details)) {
            errors.add("支出詳細に使用できない文字が含まれています。");
        }

        if (profitLossTarget && (businessUseRatio.compareTo(BigDecimal.ZERO) < 0 || businessUseRatio.compareTo(ONE_HUNDRED) > 0)) {
            errors.add("設定されている家事按分率が不正です。");
        }

        if (form.getId() != null
                && expenseRepository.findByIdAndUserIdAndDeletedFlagFalse(form.getId(), currentUser.getId()).isEmpty()) {
            errors.add("更新対象の支出が見つかりません。");
        }

        return errors;
    }

    private boolean isValidSubcategory(ExpenseCategory expenseCategory, Long subcategoryId) {
        if (expenseCategory == null || subcategoryId == null) {
            return false;
        }
        return expenseSubcategoryRepository
                .findByIdAndExpenseCategoryIdAndStatus(subcategoryId, expenseCategory.getId(), "ACTIVE")
                .isPresent();
    }

    private ExpenseCategory resolveCategory(String expenseType) {
        String mappedType = switch (expenseType) {
            case "expense", "business" -> "BUSINESS";
            case "public" -> "PUBLIC";
            case "private" -> "PRIVATE";
            default -> "";
        };
        return mappedType.isEmpty()
                ? null
                : expenseCategoryRepository.findByExpenseTypeAndStatus(mappedType, "ACTIVE").orElse(null);
    }

    private void validateExpenseDate(String date, List<String> errors) {
        try {
            LocalDate expenseDate = LocalDate.parse(date);
            if (expenseDate.isAfter(LocalDate.now())) {
                errors.add("登録日より未来の日付は選択できません。");
            }
        } catch (DateTimeParseException e) {
            errors.add("存在する日付を選択してください。");
        }
    }

    private boolean containsInvalidDetailsCharacter(String value) {
        return INVALID_DETAILS_CHARACTER_PATTERN.matcher(value).find();
    }

    private BigDecimal calculateDeductibleAmount(
            BigDecimal amount,
            boolean homeApportionment,
            BigDecimal businessUseRatio,
            Boolean businessTarget) {
        if (Boolean.FALSE.equals(businessTarget)) {
            return BigDecimal.ZERO;
        }
        if (!homeApportionment) {
            // 按分未適用時は満額保存（amountとdeductible_amountを同額にする）。
            return amount;
        }
        return amount.multiply(businessUseRatio).divide(ONE_HUNDRED, 0, RoundingMode.DOWN);
    }

    private Boolean parseBusinessTarget(String value) {
        String normalized = trimToEmpty(value);
        if (normalized.isEmpty()) {
            return Boolean.TRUE;
        }
        if ("true".equalsIgnoreCase(normalized) || "on".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private Long parseLong(String value) {
        return Long.parseLong(value.trim());
    }

    private Long parseLongSafely(String value) {
        try {
            return parseLong(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private User getCurrentUser() {
        Long userId = AuthUserContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Login user is not found in session");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
}

