package org.example.features.setting.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.UserSetting;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.setting.TaxReturnSection;
import org.example.features.setting.dto.SettingOperationResult;
import org.example.features.setting.dto.SettingView;
import org.example.features.setting.dto.TaxReturnSectionOption;
import org.example.features.setting.form.AccountActionForm;
import org.example.features.setting.form.BusinessUseRatioForm;
import org.example.features.setting.form.PasswordForm;
import org.example.features.setting.form.ProfileForm;
import org.example.features.setting.form.TaxBasicInfoForm;
import org.example.features.setting.form.TaxReturnSectionForm;
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
    private static final Pattern KANA_PATTERN = Pattern.compile("^[\\u30A1-\\u30FC]+(\\u3000[\\u30A1-\\u30FC]+)*$");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s\\u3000]+");
    private static final Pattern DATE_PATTERN = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");
    private static final Pattern BUSINESS_TYPE_PATTERN = Pattern.compile("^[1-6]$");
    private static final LocalDate MIN_BIRTH_DATE = LocalDate.of(1900, 1, 1);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Tokyo");
    private static final String CONSUMPTION_TAX_EXEMPT = "EXEMPT";
    private static final String CONSUMPTION_TAX_SIMPLIFIED = "SIMPLIFIED";
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
                rate.setScale(0, java.math.RoundingMode.DOWN),
                user.getNameKana(),
                user.getBirthDate(),
                profile.getTaxOffice(),
                profile.getHouseholdHeadName(),
                profile.getHouseholdHeadRelation(),
                profile.getConsumptionTaxStatus(),
                profile.getSimplifiedBusinessType());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<TaxReturnSection> loadHiddenTaxReturnSections(Long appUserId) {
        return userSettingRepository.findByAppUserId(appUserId)
                .map(setting -> TaxReturnSection.parseHidden(setting.getHiddenTaxReturnSections()))
                .orElseGet(() -> EnumSet.noneOf(TaxReturnSection.class));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxReturnSectionOption> loadTaxReturnSectionOptions(Long appUserId) {
        Set<TaxReturnSection> hidden = loadHiddenTaxReturnSections(appUserId);
        return java.util.Arrays.stream(TaxReturnSection.values())
                .map(section -> new TaxReturnSectionOption(section, !hidden.contains(section)))
                .toList();
    }

    @Override
    @Transactional
    public SettingOperationResult updateTaxReturnSections(Long appUserId, TaxReturnSectionForm form) {
        List<String> submitted = form == null || form.getVisibleSections() == null ? List.of() : form.getVisibleSections();
        if (submitted.size() > TaxReturnSection.values().length) {
            return SettingOperationResult.invalid(List.of("表示するセクションの指定が正しくありません。"));
        }
        Set<TaxReturnSection> visible = EnumSet.noneOf(TaxReturnSection.class);
        for (String code : submitted) {
            var section = TaxReturnSection.fromCode(code == null ? "" : code.strip());
            if (section.isEmpty()) {
                return SettingOperationResult.invalid(List.of("表示するセクションの指定が正しくありません。"));
            }
            visible.add(section.get());
        }
        // 必須セクションはチェックの有無にかかわらず表示する（画面では非活性のため送信されない）。
        Set<TaxReturnSection> hidden = EnumSet.noneOf(TaxReturnSection.class);
        for (TaxReturnSection section : TaxReturnSection.values()) {
            if (!section.isRequired() && !visible.contains(section)) {
                hidden.add(section);
            }
        }
        AppUser user = findActiveUser(appUserId);
        UserSetting setting = userSettingRepository.findByAppUserId(appUserId).orElseGet(() -> {
            UserSetting created = new UserSetting();
            created.setAppUser(user);
            created.setHomeApportionmentRate(BigDecimal.ZERO.setScale(2));
            return created;
        });
        setting.setHiddenTaxReturnSections(TaxReturnSection.formatHidden(hidden));
        userSettingRepository.save(setting);
        log.info("Tax return section visibility updated. userId={}, hiddenCount={}", appUserId, hidden.size());
        return SettingOperationResult.success("確定申告画面の表示設定を保存しました。");
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
        String nameKana = normalizeKana(form == null ? null : form.getNameKana());
        String rawBirthDate = normalizeOptional(form == null ? null : form.getBirthDate());
        String taxOffice = normalizeOptional(form == null ? null : form.getTaxOffice());
        String householdHeadName = normalizeOptional(form == null ? null : form.getHouseholdHeadName());
        String householdHeadRelation = normalizeOptional(form == null ? null : form.getHouseholdHeadRelation());
        String consumptionTaxStatus = normalizeOptional(form == null ? null : form.getConsumptionTaxStatus());
        String rawBusinessType = normalizeOptional(form == null ? null : form.getSimplifiedBusinessType());

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
        if (nameKana != null && (nameKana.length() > 100 || !KANA_PATTERN.matcher(nameKana).matches())) {
            errors.add("フリガナは100文字以内で、全角カタカナとスペースのみ使用できます。");
        }
        LocalDate birthDate = null;
        if (rawBirthDate != null) {
            birthDate = parseBirthDate(rawBirthDate);
            if (birthDate == null) {
                errors.add("生年月日は1900年1月2日から昨日までの日付を入力してください。");
            }
        }
        if (taxOffice != null && taxOffice.length() > 100) {
            errors.add("提出先税務署は100文字以内で入力してください。");
        }
        if (householdHeadName != null && householdHeadName.length() > 100) {
            errors.add("世帯主の氏名は100文字以内で入力してください。");
        }
        if (householdHeadRelation != null && householdHeadRelation.length() > 30) {
            errors.add("世帯主との続柄は30文字以内で入力してください。");
        }
        Integer simplifiedBusinessType = null;
        if (!CONSUMPTION_TAX_EXEMPT.equals(consumptionTaxStatus)
                && !CONSUMPTION_TAX_SIMPLIFIED.equals(consumptionTaxStatus)) {
            errors.add("消費税の区分を選択してください。");
        } else if (CONSUMPTION_TAX_SIMPLIFIED.equals(consumptionTaxStatus)) {
            if (rawBusinessType == null || !BUSINESS_TYPE_PATTERN.matcher(rawBusinessType).matches()) {
                errors.add("簡易課税の事業区分（第1種〜第6種）を選択してください。");
            } else {
                simplifiedBusinessType = Integer.valueOf(rawBusinessType);
            }
        }
        if (!errors.isEmpty()) {
            return SettingOperationResult.invalid(errors);
        }

        AppUser user = findActiveUser(appUserId);
        BusinessProfile profile = findProfileForUpdate(appUserId);
        user.setPhone(phone);
        user.setAddress(address);
        user.setNameKana(nameKana);
        user.setBirthDate(birthDate);
        profile.setBusinessName(businessName);
        profile.setIndustry(industry);
        profile.setTaxOffice(taxOffice);
        profile.setHouseholdHeadName(householdHeadName);
        profile.setHouseholdHeadRelation(householdHeadRelation);
        profile.setConsumptionTaxStatus(consumptionTaxStatus);
        profile.setSimplifiedBusinessType(simplifiedBusinessType);
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
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeKana(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : WHITESPACE_PATTERN.matcher(normalized).replaceAll("\u3000");
    }

    private LocalDate parseBirthDate(String value) {
        if (!DATE_PATTERN.matcher(value).matches()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value);
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            return date.isAfter(MIN_BIRTH_DATE) && date.isBefore(today) ? date : null;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
