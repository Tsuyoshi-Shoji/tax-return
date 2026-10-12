package org.example.features.profitloss.dto;

public class ProfitLossItemOption {
    private final String value;
    private final String label;
    private final String group;

    public ProfitLossItemOption(String value, String label, String group) {
        this.value = value;
        this.label = label;
        this.group = group;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getGroup() {
        return group;
    }
}
