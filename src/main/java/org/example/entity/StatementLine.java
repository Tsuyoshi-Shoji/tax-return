package org.example.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "statement_line", uniqueConstraints = @UniqueConstraint(
        name = "uk_statement_line", columnNames = {"form_code", "line_code", "effective_from_year"}))
public class StatementLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_code", nullable = false, length = 50)
    private String formCode;

    @Column(name = "line_code", nullable = false, length = 50)
    private String lineCode;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "line_label", nullable = false, length = 100)
    private String lineLabel;

    /** INPUT（科目集計）/ FREE（任意記載欄）/ CALC（計算欄） */
    @Column(name = "line_type", nullable = false, length = 20)
    private String lineType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "effective_from_year", nullable = false)
    private Integer effectiveFromYear;

    @Column(name = "effective_to_year")
    private Integer effectiveToYear;

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

    public Long getId() {
        return id;
    }

    public String getFormCode() {
        return formCode;
    }

    public void setFormCode(String formCode) {
        this.formCode = formCode;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getLineLabel() {
        return lineLabel;
    }

    public void setLineLabel(String lineLabel) {
        this.lineLabel = lineLabel;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer getEffectiveFromYear() {
        return effectiveFromYear;
    }

    public void setEffectiveFromYear(Integer effectiveFromYear) {
        this.effectiveFromYear = effectiveFromYear;
    }

    public Integer getEffectiveToYear() {
        return effectiveToYear;
    }

    public void setEffectiveToYear(Integer effectiveToYear) {
        this.effectiveToYear = effectiveToYear;
    }
}
