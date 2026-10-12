package org.example.features.setting.dto;

import java.math.BigDecimal;

public class SettingView {
    private final String displayName;
    private final String email;
    private final String status;
    private final BigDecimal apportionmentRate;

    public SettingView(String displayName, String email, String status, BigDecimal apportionmentRate) {
        this.displayName = displayName;
        this.email = email;
        this.status = status;
        this.apportionmentRate = apportionmentRate;
    }

    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public String getStatusLabel() { return "active".equalsIgnoreCase(status) ? "Active" : status; }
    public BigDecimal getApportionmentRate() { return apportionmentRate; }
}