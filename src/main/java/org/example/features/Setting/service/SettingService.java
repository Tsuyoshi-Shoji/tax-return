package org.example.features.setting.service;

import org.example.features.setting.dto.SettingOperationResult;
import org.example.features.setting.dto.SettingView;
import org.example.features.setting.form.AccountActionForm;
import org.example.features.setting.form.BusinessUseRatioForm;
import org.example.features.setting.form.PasswordForm;
import org.example.features.setting.form.ProfileForm;

import java.math.BigDecimal;

public interface SettingService {

    SettingView getCurrentSetting();

    BigDecimal getDefaultBusinessUseRatio();

    SettingOperationResult updateProfile(ProfileForm form);

    SettingOperationResult changePassword(PasswordForm form);

    SettingOperationResult updateBusinessUseRatio(BusinessUseRatioForm form);

    SettingOperationResult disableAccount(AccountActionForm form);

    SettingOperationResult deleteAccount(AccountActionForm form);
}

