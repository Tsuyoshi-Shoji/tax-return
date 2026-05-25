package org.example.features.setting.dto;

import java.math.BigDecimal;

public class SettingView {

    private final String username;
    private final String email;
    private final BigDecimal defaultBusinessUseRatio;
    private final String accountStatus;

    public SettingView(String username, String email, BigDecimal defaultBusinessUseRatio, String accountStatus) {
        this.username = username;
        this.email = email;
        this.defaultBusinessUseRatio = defaultBusinessUseRatio;
        this.accountStatus = accountStatus;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public BigDecimal getDefaultBusinessUseRatio() {
        return defaultBusinessUseRatio;
    }

    public String getAccountStatus() {
        return accountStatus;
    }
}

