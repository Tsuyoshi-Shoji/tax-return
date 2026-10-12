package org.example.features.auth.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.FiscalYear;
import org.example.entity.UserSetting;
import org.example.features.auth.AuthUserContext;
import org.example.features.auth.dto.AuthOperationResult;
import org.example.features.auth.form.LoginForm;
import org.example.features.auth.form.UserRegisterForm;
import org.example.features.auth.service.AuthService;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final AppUserRepository appUserRepository;
    private final UserSettingRepository userSettingRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            AppUserRepository appUserRepository,
            UserSettingRepository userSettingRepository,
            BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.userSettingRepository = userSettingRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthOperationResult login(LoginForm form) {
        List<String> errors = validateLoginForm(form);
        if (!errors.isEmpty()) {
            return AuthOperationResult.invalid(errors, "入力内容に誤りがあります。");
        }
        String normalizedEmail = normalizeEmail(form.getEmail());
        Optional<AppUser> userOpt = appUserRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            return AuthOperationResult.unauthorized("メールアドレスまたはパスワードが正しくありません。");
        }

        AppUser user = userOpt.get();
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            return AuthOperationResult.unauthorized("メールアドレスまたはパスワードが正しくありません。");
        }
        if (!passwordEncoder.matches(form.getPassword(), user.getPasswordHash())) {
            return AuthOperationResult.unauthorized("メールアドレスまたはパスワードが正しくありません。");
        }

        // MFAが有効な場合は、仮認証状態で返す
        // ※MFA廃止のためコメントアウト
        // if (Boolean.TRUE.equals(user.getMfaEnabled())) {
        //     AuthUserContext context = new AuthUserContext(user.getId(), user.getDisplayName(), user.getEmail());
        //     context.setMfaPending(true);
        //     return AuthOperationResult.success(context, "MFA検証が必要です。");
        // }

        AuthUserContext context = new AuthUserContext(user.getId(), user.getDisplayName(), user.getEmail());
        return AuthOperationResult.success(context, "ログインしました。");
    }

    @Override
    @Transactional
    public AuthOperationResult register(UserRegisterForm form) {
        List<String> errors = validateRegisterForm(form);
        if (!errors.isEmpty()) {
            return AuthOperationResult.invalid(errors, "入力内容に誤りがあります。");
        }

        String normalizedEmail = normalizeEmail(form.getEmail());
        if (appUserRepository.existsByEmail(normalizedEmail)) {
            return AuthOperationResult.duplicateEmail(
                    List.of("メールアドレスはすでに登録されています。"),
                    "メールアドレスはすでに登録されています。");
        }

        AppUser user = new AppUser();
        user.setLoginId(normalizedEmail);
        user.setEmail(normalizedEmail);
        user.setDisplayName(form.getName().trim());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setStatus("active");
        AppUser saved = appUserRepository.save(user);

        UserSetting setting = new UserSetting();
        setting.setAppUser(saved);
        setting.setHomeApportionmentRate(new BigDecimal("0.00"));
        userSettingRepository.save(setting);

        BusinessProfile profile = new BusinessProfile();
        profile.setAppUser(saved);
        profile.setBusinessName(saved.getDisplayName());
        profile.setFilingType("blue");
        BusinessProfile savedProfile = businessProfileRepository.save(profile);

        int year = LocalDate.now().getYear();
        FiscalYear fiscalYear = new FiscalYear();
        fiscalYear.setBusinessProfile(savedProfile);
        fiscalYear.setTaxYear(year);
        fiscalYear.setPeriodStart(LocalDate.of(year, 1, 1));
        fiscalYear.setPeriodEnd(LocalDate.of(year, 12, 31));
        fiscalYear.setStatus("open");
        fiscalYearRepository.save(fiscalYear);

        return AuthOperationResult.success(null, "ユーザー登録が完了しました。ログインしてください。");
    }

    private List<String> validateLoginForm(LoginForm form) {
        List<String> errors = new ArrayList<>();
        String email = form == null ? "" : valueOrEmpty(form.getEmail());
        String password = form == null ? "" : valueOrEmpty(form.getPassword());
        if (email.isBlank()) {
            errors.add("メールアドレスを入力してください。");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("メールアドレスは正しい形式で入力してください。");
        }
        if (password.isBlank()) {
            errors.add("パスワードを入力してください。");
        } else if (password.length() < 8 || password.length() > 72) {
            errors.add("パスワードは8〜72文字で入力してください。");
        }
        return errors;
    }

    private List<String> validateRegisterForm(UserRegisterForm form) {
        List<String> errors = new ArrayList<>();
        String name = form == null ? "" : valueOrEmpty(form.getName());
        String email = form == null ? "" : valueOrEmpty(form.getEmail());
        String password = form == null ? "" : valueOrEmpty(form.getPassword());
        String confirmPassword = form == null ? "" : valueOrEmpty(form.getConfirmPassword());
        boolean termsAgreement = form != null && form.isTermsAgreement();

        if (name.isBlank()) {
            errors.add("氏名を入力してください。");
        } else if (name.length() > 100) {
            errors.add("氏名は100文字以内で入力してください。");
        }
        if (email.isBlank()) {
            errors.add("メールアドレスを入力してください。");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("メールアドレスは正しい形式で入力してください。");
        }
        if (password.isBlank()) {
            errors.add("パスワードを入力してください。");
        } else if (password.length() < 8 || password.length() > 72) {
            errors.add("パスワードは8〜72文字で入力してください。");
        }
        if (confirmPassword.isBlank()) {
            errors.add("確認用パスワードを入力してください。");
        } else if (!password.equals(confirmPassword)) {
            errors.add("パスワードが異なります。確認してください。");
        }
        if (!termsAgreement) {
            errors.add("利用規約・プライバシーポリシーへの同意が必要です。");
        }
        return errors;
    }

    private String normalizeEmail(String email) {
        return valueOrEmpty(email).trim().toLowerCase();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
