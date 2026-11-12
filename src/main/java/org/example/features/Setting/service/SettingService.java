package org.example.features.setting.service;

import java.util.List;
import java.util.Set;

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

public interface SettingService {
    SettingView load(Long appUserId);

    /** 非表示に設定された任意セクション（必須セクションは含まない）。 */
    Set<TaxReturnSection> loadHiddenTaxReturnSections(Long appUserId);

    List<TaxReturnSectionOption> loadTaxReturnSectionOptions(Long appUserId);

    SettingOperationResult updateTaxReturnSections(Long appUserId, TaxReturnSectionForm form);

    SettingOperationResult updateProfile(Long appUserId, ProfileForm form);

    SettingOperationResult changePassword(Long appUserId, PasswordForm form);

    SettingOperationResult updateTaxBasicInfo(Long appUserId, TaxBasicInfoForm form);

    SettingOperationResult updateBusinessUseRatio(Long appUserId, BusinessUseRatioForm form);

    SettingOperationResult disableAccount(Long appUserId, AccountActionForm form);

    SettingOperationResult deleteAccount(Long appUserId, AccountActionForm form);
}
