package org.example.features.setting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 確定申告画面での各セクションの表示可否。必須セクションは常に表示し、非表示設定の任意セクションでも
 * 対象年分に登録データ・適用がある場合は記載漏れを防ぐため表示する（forced）。
 */
public final class TaxReturnSectionVisibility {
    private final Map<String, Boolean> show;
    private final Map<String, Boolean> forced;

    private TaxReturnSectionVisibility(Map<String, Boolean> show, Map<String, Boolean> forced) {
        this.show = Collections.unmodifiableMap(show);
        this.forced = Collections.unmodifiableMap(forced);
    }

    public static TaxReturnSectionVisibility resolve(Set<TaxReturnSection> hidden, Set<TaxReturnSection> sectionsInUse) {
        Map<String, Boolean> show = new LinkedHashMap<>();
        Map<String, Boolean> forced = new LinkedHashMap<>();
        for (TaxReturnSection section : TaxReturnSection.values()) {
            boolean hiddenBySetting = !section.isRequired() && hidden.contains(section);
            boolean inUse = sectionsInUse.contains(section);
            show.put(section.getCode(), !hiddenBySetting || inUse);
            forced.put(section.getCode(), hiddenBySetting && inUse);
        }
        return new TaxReturnSectionVisibility(show, forced);
    }

    /** JSP から ${sections.show['fixed-assets']} の形で参照する。 */
    public Map<String, Boolean> getShow() { return show; }

    public Map<String, Boolean> getForced() { return forced; }

    public boolean isShown(TaxReturnSection section) {
        return Boolean.TRUE.equals(show.get(section.getCode()));
    }
}
