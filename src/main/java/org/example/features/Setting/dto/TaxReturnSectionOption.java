package org.example.features.setting.dto;

import org.example.features.setting.TaxReturnSection;

/** 設定画面のセクション表示設定の1行。 */
public class TaxReturnSectionOption {
    private final TaxReturnSection section;
    private final boolean visible;

    public TaxReturnSectionOption(TaxReturnSection section, boolean visible) {
        this.section = section;
        this.visible = visible;
    }

    public String getCode() { return section.getCode(); }
    public String getLabel() { return section.getLabel(); }
    public String getDescription() { return section.getDescription(); }
    public boolean isRequired() { return section.isRequired(); }
    public boolean isVisible() { return section.isRequired() || visible; }
}
