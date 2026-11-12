package org.example.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "fixed_asset_event")
public class FixedAssetEvent {
    public static final String BUSINESS_RATIO_CHANGE = "BUSINESS_RATIO_CHANGE";
    public static final String SALE = "SALE";
    public static final String DISPOSAL = "DISPOSAL";
    public static final String INHERITANCE = "INHERITANCE";
    public static final String GIFT = "GIFT";
    public static final String METHOD_CHANGE = "METHOD_CHANGE";
    public static final String BUSINESS_CESSATION = "BUSINESS_CESSATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_asset_id", nullable = false)
    private FixedAsset fixedAsset;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "business_ratio", precision = 5, scale = 2)
    private BigDecimal businessRatio;

    @Column(name = "proceeds", precision = 15, scale = 0)
    private BigDecimal proceeds;

    @Column(name = "opening_book_value", precision = 15, scale = 0)
    private BigDecimal openingBookValue;

    @Column(name = "new_depreciation_method", length = 30)
    private String newDepreciationMethod;

    @Column(name = "history_confirmed", nullable = false)
    private boolean historyConfirmed;

    @Column(name = "notes", length = 255)
    private String notes;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public FixedAsset getFixedAsset() { return fixedAsset; }
    public void setFixedAsset(FixedAsset fixedAsset) { this.fixedAsset = fixedAsset; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public BigDecimal getBusinessRatio() { return businessRatio; }
    public void setBusinessRatio(BigDecimal businessRatio) { this.businessRatio = businessRatio; }
    public BigDecimal getProceeds() { return proceeds; }
    public void setProceeds(BigDecimal proceeds) { this.proceeds = proceeds; }
    public BigDecimal getOpeningBookValue() { return openingBookValue; }
    public void setOpeningBookValue(BigDecimal openingBookValue) { this.openingBookValue = openingBookValue; }
    public String getNewDepreciationMethod() { return newDepreciationMethod; }
    public void setNewDepreciationMethod(String newDepreciationMethod) { this.newDepreciationMethod = newDepreciationMethod; }
    public boolean isHistoryConfirmed() { return historyConfirmed; }
    public void setHistoryConfirmed(boolean historyConfirmed) { this.historyConfirmed = historyConfirmed; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
