package org.example.features.income.service.impl;

import org.example.entity.Income;
import org.example.entity.IncomeCategory;
import org.example.entity.IncomeSubcategory;
import org.example.entity.ReceiveMethod;
import org.example.entity.User;
import org.example.features.income.dto.IncomeEditView;
import org.example.features.income.dto.IncomeRegistrationResult;
import org.example.features.income.form.IncomeForm;
import org.example.features.income.service.IncomeService;
import org.example.repository.IncomeCategoryRepository;
import org.example.repository.IncomeRepository;
import org.example.repository.IncomeSubcategoryRepository;
import org.example.repository.ReceiveMethodRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class IncomeServiceImpl implements IncomeService {

    private static final Pattern INVALID_DETAILS_CHARACTER_PATTERN = Pattern.compile("[<>\"'`\\\\]");
    private static final Long DEFAULT_USER_ID = 1L;

    private final IncomeRepository incomeRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final IncomeSubcategoryRepository incomeSubcategoryRepository;
    private final ReceiveMethodRepository receiveMethodRepository;
    private final UserRepository userRepository;

    public IncomeServiceImpl(
            IncomeRepository incomeRepository,
            IncomeCategoryRepository incomeCategoryRepository,
            IncomeSubcategoryRepository incomeSubcategoryRepository,
            ReceiveMethodRepository receiveMethodRepository,
            UserRepository userRepository) {
        this.incomeRepository = incomeRepository;
        this.incomeCategoryRepository = incomeCategoryRepository;
        this.incomeSubcategoryRepository = incomeSubcategoryRepository;
        this.receiveMethodRepository = receiveMethodRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public IncomeRegistrationResult registerIncome(IncomeForm form) {
        IncomeCategory category = resolveCategory(trimToEmpty(form.getIncomeType()));
        List<String> errors = validate(form, category);
        if (!errors.isEmpty()) {
            return IncomeRegistrationResult.failure(errors);
        }

         User currentUser = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Default user not found"));
         IncomeSubcategory subcategory = incomeSubcategoryRepository
                .findByIdAndIncomeCategoryIdAndStatus(parseLong(form.getCategory()), category.getId(), "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("選択された項目がマスタに存在しません。"));
        ReceiveMethod receiveMethod = receiveMethodRepository.findById(parseLong(form.getReceiveMethodId()))
                .orElseThrow(() -> new IllegalArgumentException("受取方法がマスタに存在しません。"));
        Income income = form.getId() == null
                ? new Income()
                : incomeRepository.findByIdAndUserIdAndDeletedFlagFalse(form.getId(), currentUser.getId())
                        .orElseThrow(() -> new IllegalArgumentException("更新対象の収益が見つかりません。"));

        BigDecimal amount = new BigDecimal(trimToEmpty(form.getAmount()));
        income.setUser(currentUser);
        income.setIncomeCategory(category);
        income.setIncomeSubcategory(subcategory);
        income.setReceiveMethod(receiveMethod);
        income.setIncomeDate(LocalDate.parse(trimToEmpty(form.getDate())));
        income.setAmount(amount);
        income.setClientName(null);
        income.setDescription(trimToEmpty(form.getDetails()));
        income.setMemo(null);
        income.setBusinessTartget(parseBusinessTartget(form.getBusinessTartget()));
        income.setDeletedFlag(Boolean.FALSE);
        income.setDeletedAt(null);
        incomeRepository.save(income);

        return IncomeRegistrationResult.success("収益を登録しました。", amount);
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeEditView getIncomeEditView(Long id) {
        User currentUser = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Default user not found"));
        Income income = incomeRepository.findByIdAndUserIdAndDeletedFlagFalse(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("対象の収益が見つかりません。"));
        return new IncomeEditView(
                income.getId(),
                mapIncomeType(income.getIncomeCategory()),
                income.getIncomeSubcategory() == null ? null : income.getIncomeSubcategory().getId(),
                income.getIncomeDate().toString(),
                income.getAmount().toPlainString(),
                income.getDescription(),
                income.getReceiveMethod() == null ? null : income.getReceiveMethod().getId(),
                income.getBusinessTartget());
    }

    private List<String> validate(IncomeForm form, IncomeCategory category) {
        List<String> errors = new ArrayList<>();
        User currentUser = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Default user not found"));

        String incomeType = trimToEmpty(form.getIncomeType());
        String categoryId = trimToEmpty(form.getCategory());
        String date = trimToEmpty(form.getDate());
        String amount = trimToEmpty(form.getAmount());
        String details = trimToEmpty(form.getDetails());
        String receiveMethodId = trimToEmpty(form.getReceiveMethodId());

        if (incomeType.isEmpty()) {
            errors.add("区分を選択してください。");
        } else if (category == null) {
            errors.add("区分が不正です。");
        }

        if (categoryId.isEmpty()) {
            errors.add("項目を選択してください。");
        } else if (!isValidSubcategory(category, parseLongSafely(categoryId))) {
            errors.add("選択された項目がマスタに存在しません。");
        }

        if (receiveMethodId.isEmpty()) {
            errors.add("受取方法を選択してください。");
        } else if (parseLongSafely(receiveMethodId) == null || receiveMethodRepository.findById(parseLong(receiveMethodId)).isEmpty()) {
            errors.add("選択された受取方法がマスタに存在しません。");
        }

        if (date.isEmpty()) {
            errors.add("収益発生日を選択してください。");
        } else {
            validateIncomeDate(date, errors);
        }

        if (amount.isEmpty()) {
            errors.add("金額を入力してください。");
        } else if (!amount.matches("\\d+")) {
            errors.add("金額は数値のみで入力してください。");
        } else if (new BigDecimal(amount).compareTo(BigDecimal.ONE) < 0) {
            errors.add("金額は1円以上で入力してください。");
        }

        if (details.isEmpty()) {
            errors.add("収益詳細を入力してください。");
        } else if (details.length() > 255) {
            errors.add("収益詳細は1〜255文字で入力してください。");
        } else if (containsInvalidDetailsCharacter(details)) {
            errors.add("収益詳細に使用できない文字が含まれています。");
        }

        if (form.getId() != null && incomeRepository.findByIdAndUserIdAndDeletedFlagFalse(form.getId(), currentUser.getId()).isEmpty()) {
            errors.add("更新対象の収益が見つかりません。");
        }

        return errors;
    }

    private boolean isValidSubcategory(IncomeCategory category, Long subcategoryId) {
        if (category == null || subcategoryId == null) {
            return false;
        }
        return incomeSubcategoryRepository.findByIdAndIncomeCategoryIdAndStatus(subcategoryId, category.getId(), "ACTIVE").isPresent();
    }

    private IncomeCategory resolveCategory(String incomeType) {
        Long categoryId = switch (incomeType) {
            case "business" -> 1L;
            case "investment" -> 2L;
            case "temporary" -> 3L;
            case "nontaxable" -> 4L;
            default -> null;
        };
        return categoryId == null ? null : incomeCategoryRepository.findById(categoryId).orElse(null);
    }

    private String mapIncomeType(IncomeCategory category) {
        if (category == null || category.getId() == null) {
            return "business";
        }
        return switch (category.getId().intValue()) {
            case 1 -> "business";
            case 2 -> "investment";
            case 3 -> "temporary";
            case 4 -> "nontaxable";
            default -> category.getCategoryName().toLowerCase(Locale.ROOT);
        };
    }

    private void validateIncomeDate(String date, List<String> errors) {
        try {
            LocalDate incomeDate = LocalDate.parse(date);
            if (incomeDate.isAfter(LocalDate.now())) {
                errors.add("登録日より未来の日付は選択できません。");
            }
        } catch (DateTimeParseException ex) {
            errors.add("存在する日付を選択してください。");
        }
    }

    private boolean containsInvalidDetailsCharacter(String value) {
        return INVALID_DETAILS_CHARACTER_PATTERN.matcher(value).find();
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

    private Boolean parseBusinessTartget(String value) {
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
}

