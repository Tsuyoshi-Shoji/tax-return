package org.example.features.setting.service;

import org.example.features.setting.dto.SettingOperationResult;
import org.example.features.setting.dto.SettingView;
import org.example.features.setting.form.AccountActionForm;
import org.example.features.setting.form.BusinessUseRatioForm;
import org.example.features.setting.form.PasswordForm;
import org.example.features.setting.form.ProfileForm;
import org.example.features.setting.form.TaxBasicInfoForm;

public interface SettingService {
    SettingView load(Long appUserId);

    SettingOperationResult updateProfile(Long appUserId, ProfileForm form);

    SettingOperationResult changePassword(Long appUserId, PasswordForm form);

    SettingOperationResult updateTaxBasicInfo(Long appUserId, TaxBasicInfoForm form);

    SettingOperationResult updateBusinessUseRatio(Long appUserId, BusinessUseRatioForm form);

    SettingOperationResult disableAccount(Long appUserId, AccountActionForm form);

    SettingOperationResult deleteAccount(Long appUserId, AccountActionForm form);
}
