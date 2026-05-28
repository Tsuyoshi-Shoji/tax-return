package org.example.features.auth.service.impl;

import org.example.entity.User;
import org.example.entity.UserSetting;
import org.example.features.auth.dto.AuthOperationResult;
import org.example.features.auth.form.LoginForm;
import org.example.features.auth.form.UserRegisterForm;
import org.example.features.auth.service.AuthService;
import org.example.repository.UserRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final int LOCK_THRESHOLD = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, UserSettingRepository userSettingRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSettingRepository = userSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthOperationResult login(LoginForm form) {
        List<String> errors = new ArrayList<>();
        String email = normalizeEmail(form.getEmail());
        String password = trimToEmpty(form.getPassword());

        if (email.isEmpty()) {
            errors.add("メールアドレスを入力してください。");
        } else if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("メールアドレスは正しい形式で入力してください。");
        }

        if (password.isEmpty()) {
            errors.add("パスワードを入力してください。");
        } else if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            errors.add("パスワードは8〜72文字で入力してください。");
        }

        if (!errors.isEmpty()) {
            return AuthOperationResult.failure(errors);
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return AuthOperationResult.failure(List.of("メールアドレスまたはパスワードが正しくありません。"));
        }

        User user = userOptional.get();
        if (!"ACTIVE".equalsIgnoreCase(trimToEmpty(user.getStatus()))) {
            return AuthOperationResult.failure(List.of("このアカウントは現在利用できません。"));
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            return AuthOperationResult.failure(List.of("ログイン失敗が続いたため一時的にロックされています。時間をおいて再試行してください。"));
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int currentFailures = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
            int nextFailures = currentFailures + 1;
            user.setFailedLoginCount(nextFailures);
            if (nextFailures >= LOCK_THRESHOLD) {
                user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                user.setFailedLoginCount(0);
            }
            return AuthOperationResult.failure(List.of("メールアドレスまたはパスワードが正しくありません。"));
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);

        String displayName = trimToEmpty(user.getUsername());
        if (displayName.isEmpty()) {
            displayName = user.getEmail();
        }
        return AuthOperationResult.loginSuccess(user.getId(), user.getEmail(), displayName);
    }

    @Override
    @Transactional
    public AuthOperationResult register(UserRegisterForm form) {
        List<String> errors = new ArrayList<>();
        String email = normalizeEmail(form.getEmail());
        String password = trimToEmpty(form.getPassword());
        String confirmPassword = trimToEmpty(form.getConfirmPassword());

        if (email.isEmpty()) {
            errors.add("メールアドレスを入力してください。");
        } else if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("メールアドレスは正しい形式で入力してください。");
        } else if (userRepository.existsByEmail(email)) {
            errors.add("このメールアドレスは既に利用されています。");
        }

        if (password.isEmpty()) {
            errors.add("パスワードを入力してください。");
        } else if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            errors.add("パスワードは8〜72文字で入力してください。");
        }

        if (confirmPassword.isEmpty()) {
            errors.add("確認用パスワードを入力してください。");
        } else if (!password.equals(confirmPassword)) {
            errors.add("パスワードが異なります。確認してください。");
        }

        if (!errors.isEmpty()) {
            return AuthOperationResult.failure(errors);
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(defaultUsernameFromEmail(email));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus("ACTIVE");
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);

        User savedUser = userRepository.save(user);

        UserSetting userSetting = new UserSetting();
        userSetting.setUser(savedUser);
        userSetting.setDefaultBusinessUseRatio(new BigDecimal("100.00"));
        userSettingRepository.save(userSetting);

        return AuthOperationResult.success("ユーザー登録が完了しました。ログインしてください。");
    }

    private String normalizeEmail(String value) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private String defaultUsernameFromEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "";
        }
        String candidate = email.substring(0, at).trim();
        if (candidate.length() > 100) {
            return candidate.substring(0, 100);
        }
        return candidate;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

