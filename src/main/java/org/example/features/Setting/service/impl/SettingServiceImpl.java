package org.example.features.setting.service.impl;

import org.example.entity.User;
import org.example.entity.UserSetting;
import org.example.features.setting.dto.SettingOperationResult;
import org.example.features.setting.dto.SettingView;
import org.example.features.setting.form.AccountActionForm;
import org.example.features.setting.form.BusinessUseRatioForm;
import org.example.features.setting.form.PasswordForm;
import org.example.features.setting.form.ProfileForm;
import org.example.features.setting.service.SettingService;
import org.example.repository.ExpenseRepository;
import org.example.repository.IncomeRepository;
import org.example.repository.UserRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class SettingServiceImpl implements SettingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Long DEFAULT_USER_ID = 1L;

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;

    public SettingServiceImpl(
            UserRepository userRepository,
            UserSettingRepository userSettingRepository,
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository) {
        this.userRepository = userRepository;
        this.userSettingRepository = userSettingRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SettingView getCurrentSetting() {
        Optional<User> userOptional = userRepository.findById(DEFAULT_USER_ID);
        if (userOptional.isEmpty()) {
            return new SettingView("", "", ONE_HUNDRED, "ACTIVE");
        }
        User user = userOptional.get();
        UserSetting setting = getOrCreateUserSetting(user);
        return new SettingView(
                user.getUsername() == null ? "" : user.getUsername(),
                user.getEmail(),
                setting.getDefaultBusinessUseRatio(),
                user.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getDefaultBusinessUseRatio() {
        Optional<User> userOptional = userRepository.findById(DEFAULT_USER_ID);
        if (userOptional.isEmpty()) {
            return ONE_HUNDRED;
        }
        User user = userOptional.get();
        return getOrCreateUserSetting(user).getDefaultBusinessUseRatio();
    }

    @Override
    @Transactional
    public SettingOperationResult updateProfile(ProfileForm form) {
        List<String> errors = new ArrayList<>();
        String requestedUsername = trimToEmpty(form.getUsername());
        String requestedEmail = trimToEmpty(form.getEmail());
        User currentUser = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Default user not found"));

        if (requestedUsername.isEmpty() || requestedUsername.length() > 100) {
            errors.add("ユーザー名は1〜100文字で入力してください。");
        }
        if (requestedEmail.isEmpty() || requestedEmail.length() > 255 || !EMAIL_PATTERN.matcher(requestedEmail).matches()) {
            errors.add("メールアドレスは正しい形式で入力してください。");
        }
        if (!requestedEmail.equalsIgnoreCase(currentUser.getEmail())
                && userRepository.existsByEmail(requestedEmail.toLowerCase())) {
            errors.add("このメールアドレスは既に利用されています。");
        }
        if (!errors.isEmpty()) {
            return SettingOperationResult.failure(errors);
        }

        currentUser.setUsername(requestedUsername);
        currentUser.setEmail(requestedEmail.toLowerCase());
        return SettingOperationResult.success("プロフィールを更新しました。");
    }

    @Override
    @Transactional
    public SettingOperationResult changePassword(PasswordForm form) {
        List<String> errors = new ArrayList<>();
        String currentPassword = trimToEmpty(form.getCurrentPassword());
        String newPassword = trimToEmpty(form.getNewPassword());
        String confirmPassword = trimToEmpty(form.getConfirmPassword());
        User currentUser = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Default user not found"));

        if (currentPassword.isEmpty()) {
            errors.add("現在のパスワードを入力してください。");
        }
        if (newPassword.length() < 8 || newPassword.length() > 72) {
            errors.add("新しいパスワードは8〜72文字で入力してください。");
        }
        if (!newPassword.equals(confirmPassword)) {
            errors.add("新しいパスワードと確認用パスワードが一致しません。");
        }
        if (!errors.isEmpty()) {
            return SettingOperationResult.failure(errors);
        }

        currentUser.setPasswordHash(newPassword);
        return SettingOperationResult.success("パスワードを変更しました。");
    }

    @Override
    @Transactional
    public SettingOperationResult updateBusinessUseRatio(BusinessUseRatioForm form) {
        List<String> errors = new ArrayList<>();
        BigDecimal requestedRatio = parseRatio(trimToEmpty(form.getDefaultBusinessUseRatio()), errors);
        if (!errors.isEmpty()) {
            return SettingOperationResult.failure(errors);
        }

        User user = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Default user not found"));
        UserSetting setting = getOrCreateUserSetting(user);
        setting.setDefaultBusinessUseRatio(requestedRatio);
        return SettingOperationResult.success("家事按分率を更新しました。支出登録画面の計算に即時反映されます。");
    }

    @Override
    @Transactional
    public SettingOperationResult disableAccount(AccountActionForm form) {
        if (!"無効化".equals(trimToEmpty(form.getConfirmationText()))) {
            return SettingOperationResult.failure(List.of("確認欄に「無効化」と入力してください。"));
        }
        User currentUser = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Default user not found"));
        currentUser.setStatus("DISABLED");
        currentUser.setLockedUntil(null);
        currentUser.setFailedLoginCount(0);
        return SettingOperationResult.success("ユーザーを無効化しました。次回以降はログインできません。");
    }

    @Override
    @Transactional
    public SettingOperationResult deleteAccount(AccountActionForm form) {
        if (!"削除".equals(trimToEmpty(form.getConfirmationText()))) {
            return SettingOperationResult.failure(List.of("確認欄に「削除」と入力してください。"));
        }
        User currentUser = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Default user not found"));
        Long userId = currentUser.getId();
        expenseRepository.deleteAllByUserId(userId);
        incomeRepository.deleteAllByUserId(userId);
        userSettingRepository.findByUserId(userId).ifPresent(userSettingRepository::delete);
        userRepository.delete(currentUser);
        return SettingOperationResult.success("ユーザーと関連データを削除しました。");
    }

    private UserSetting getOrCreateUserSetting(User user) {
        return userSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSetting setting = new UserSetting();
                    setting.setUser(user);
                    setting.setDefaultBusinessUseRatio(ONE_HUNDRED);
                    return userSettingRepository.save(setting);
                });
    }

    private BigDecimal parseRatio(String value, List<String> errors) {
        if (value.isEmpty()) {
            errors.add("家事按分率を入力してください。");
            return ONE_HUNDRED;
        }
        try {
            BigDecimal ratio = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
            if (ratio.compareTo(ZERO) < 0 || ratio.compareTo(ONE_HUNDRED) > 0) {
                errors.add("家事按分率は0〜100%の範囲で入力してください。");
            }
            return ratio;
        } catch (NumberFormatException e) {
            errors.add("家事按分率は数値で入力してください。");
            return ONE_HUNDRED;
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

