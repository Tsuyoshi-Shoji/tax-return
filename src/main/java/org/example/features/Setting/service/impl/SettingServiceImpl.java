package org.example.features.setting.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.UserSetting;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.setting.dto.SettingOperationResult;
import org.example.features.setting.dto.SettingView;
import org.example.features.setting.form.AccountActionForm;
import org.example.features.setting.form.BusinessUseRatioForm;
import org.example.features.setting.form.PasswordForm;
import org.example.features.setting.form.ProfileForm;
import org.example.features.setting.form.TaxBasicInfoForm;
import org.example.features.setting.service.SettingService;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.UserSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingServiceImpl implements SettingService {
    private static final Logger log = LoggerFactory.getLogger(SettingServiceImpl.class);
    private static final Pattern RATIO_PATTERN = Pattern.compile("^[0-9]{1,3}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{0,30}$");
    private static final String STATUS_ACTIVE = "active";
    private static final String PASSWORD_MISMATCH = "現在のパスワードが正しくありません。";

    private final AppUserRepository appUserRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingServiceImpl(
            AppUserRepository appUserRepository,
            BusinessProfileRepository businessProfileRepository,
            UserSettingRepository userSettingRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.userSettingRepository = userSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public SettingView load(Long appUserId) {
        AppUser user = findUser(appUserId);
        BusinessProfile profile = findProfile(appUserId);
        BigDecimal rate = userSettingRepository.findByAppUserId(appUserId)
                .map(UserSetting::getHomeApportionmentRate)
                .orElse(BigDecimal.ZERO);
        return new SettingView(
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                profile.getBusinessName(),
                profile.getIndustry(),
                user.getPhone(),
                user.getAddress(),
                rate.setScale(0, java.math.RoundingMode.DOWN));
    }

    @Override
    @Transactional
    public SettingOperationResult updateProfile(Long appUserId, ProfileForm form) {
        String name = form == null || form.getDisplayName() == null ? "" : form.getDisplayName().trim();
        if (name.isEmpty() || name.length() > 100) {
            return SettingOperationResult.invalid(List.of("ユーザー名は1〜100文字で入力してください。"));
        }
        AppUser user = findActiveUser(appUserId);
        user.setDisplayName(name);
        appUserRepository.save(user);
        log.info("Profile updated. userId={}", appUserId);
        return SettingOperationResult.success("プロフィールを更新しました。", name);
    }

    @Override
    @Transactional
    public SettingOperationResult changePassword(Long appUserId, PasswordForm form) {
        String current = form == null ? null : form.getCurrentPassword();
        String next = form == null ? null : form.getNewPassword();
        String confirm = form == null ? null : form.getConfirmPassword();
        List<String> errors = new java.util.ArrayList<>();
        if (current == null || current.isEmpty()) {
            errors.add("現在のパスワードを入力してください。");
        }
        if (next == null || next.length() < 8 || next.length() > 72 || next.getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.add("新しいパスワードは8〜72文字で入力してください。");
        } else if (!next.equals(confirm)) {
            errors.add("新しいパスワードと確認用パスワードが一致しません。");
        }
        if (!errors.isEmpty()) {
            return SettingOperationResult.invalid(errors);
        }
        AppUser user = findActiveUser(appUserId);
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            return SettingOperationResult.invalid(List.of(PASSWORD_MISMATCH));
        }
        user.setPasswordHash(passwordEncoder.encode(next));
        appUserRepository.save(user);
        log.info("Password changed. userId={}", appUserId);
        return SettingOperationResult.success("パスワードを変更しました。");
    }

    @Override
    @Transactional
    public SettingOperationResult updateTaxBasicInfo(Long appUserId, TaxBasicInfoForm form) {
        String businessName = normalizeOptional(form == null ? null : form.getBusinessName());
        String industry = normalizeOptional(form == null ? null : form.getIndustry());
        String phone = normalizeOptional(form == null ? null : form.getPhone());
        String address = normalizeOptional(form == null ? null : form.getAddress());

        List<String> errors = new java.util.ArrayList<>();
        if (businessName != null && businessName.length() > 200) {
            errors.add("屋号は200文字以内で入力してください。");
        }
        if (industry != null && industry.length() > 100) {
            errors.add("業種は100文字以内で入力してください。");
        }
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add("電話番号は30文字以内で、数字と +()- のみ使用できます。");
        }
        if (address != null && address.length() > 255) {
            errors.add("住所は255文字以内で入力してください。");
        }
        if (!errors.isEmpty()) {
            return SettingOperationResult.invalid(errors);
        }

        AppUser user = findActiveUser(appUserId);
        BusinessProfile profile = findProfileForUpdate(appUserId);
        user.setPhone(phone);
        user.setAddress(address);
        profile.setBusinessName(businessName);
        profile.setIndustry(industry);
        appUserRepository.save(user);
        businessProfileRepository.save(profile);
        log.info("Tax basic info updated. userId={}", appUserId);
        return SettingOperationResult.success("確定申告の基本情報を更新しました。");
    }

    @Override
    @Transactional
    public SettingOperationResult updateBusinessUseRatio(Long appUserId, BusinessUseRatioForm form) {
        String raw = form == null || form.getRatio() == null ? "" : form.getRatio().trim();
        if (!RATIO_PATTERN.matcher(raw).matches() || Integer.parseInt(raw) > 100) {
            return SettingOperationResult.invalid(List.of("家事按分率は0〜100%の範囲で入力してください。"));
        }
        AppUser user = findActiveUser(appUserId);
        UserSetting setting = userSettingRepository.findByAppUserId(appUserId).orElseGet(() -> {
            UserSetting created = new UserSetting();
            created.setAppUser(user);
            return created;
        });
        setting.setHomeApportionmentRate(new BigDecimal(raw).setScale(2));
        userSettingRepository.save(setting);
        log.info("Apportionment rate updated. userId={}", appUserId);
        return SettingOperationResult.success("家事按分率を保存しました。");
    }

    @Override
    @Transactional
    public SettingOperationResult disableAccount(Long appUserId, AccountActionForm form) {
        return changeAccountStatus(appUserId, form, "無効化", "disabled", "アカウントを無効化しました。");
    }

    @Override
    @Transactional
    public SettingOperationResult deleteAccount(Long appUserId, AccountActionForm form) {
        // 保存義務の確認前に物理削除しないため、ログイン不可の削除済み状態にとどめる。
        return changeAccountStatus(appUserId, form, "削除", "deleted", "アカウントを削除しました。");
    }

    private SettingOperationResult changeAccountStatus(
            Long appUserId, AccountActionForm form, String expectedText, String newStatus, String message) {
        String confirmText = form == null || form.getConfirmText() == null ? "" : form.getConfirmText().trim();
        if (!expectedText.equals(confirmText)) {
            return SettingOperationResult.invalid(List.of("確認欄に「" + expectedText + "」と入力してください。"));
        }
        String password = form.getCurrentPassword();
        if (password == null || password.isEmpty()) {
            return SettingOperationResult.invalid(List.of("現在のパスワードを入力してください。"));
        }
        AppUser user = findActiveUser(appUserId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return SettingOperationResult.invalid(List.of(PASSWORD_MISMATCH));
        }
        user.setStatus(newStatus);
        appUserRepository.save(user);
        log.info("Account status changed. userId={}, status={}", appUserId, newStatus);
        return SettingOperationResult.success(message);
    }

    private AppUser findUser(Long appUserId) {
        return appUserRepository.findById(appUserId)
                .orElseThrow(() -> new EntryNotFoundException("User not found"));
    }

    private AppUser findActiveUser(Long appUserId) {
        AppUser user = findUser(appUserId);
        if (!STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new EntryNotFoundException("User not found");
        }
        return user;
    }

    private BusinessProfile findProfile(Long appUserId) {
        return businessProfileRepository.findByAppUserId(appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Business profile not found"));
    }

    private BusinessProfile findProfileForUpdate(Long appUserId) {
        return businessProfileRepository.findByAppUserIdForUpdate(appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Business profile not found"));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
