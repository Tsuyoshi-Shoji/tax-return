package org.example.features.setting.dto;

import java.math.BigDecimal;

public class SettingView {
    private final String displayName;
    private final String email;
    private final String status;
    private final String businessName;
    private final String industry;
    private final String phone;
    private final String address;
    private final BigDecimal apportionmentRate;

    public SettingView(
            String displayName,
            String email,
            String status,
            String businessName,
            String industry,
            String phone,
            String address,
            BigDecimal apportionmentRate) {
        this.displayName = displayName;
        this.email = email;
        this.status = status;
        this.businessName = businessName;
        this.industry = industry;
        this.phone = phone;
        this.address = address;
        this.apportionmentRate = apportionmentRate;
    }

    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public String getStatusLabel() { return "active".equalsIgnoreCase(status) ? "Active" : status; }
    public String getBusinessName() { return businessName; }
    public String getIndustry() { return industry; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public BigDecimal getApportionmentRate() { return apportionmentRate; }
}
