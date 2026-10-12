package org.example.features.home.dto;

public class FiscalYearOptionDto {
    private final Long id;
    private final String label;

    public FiscalYearOptionDto(Long id, String label) {
        this.id = id;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
