package org.example.features.setting.form;

import java.util.ArrayList;
import java.util.List;

/** 確定申告画面のセクション表示設定。チェックされた（表示する）任意セクションのコードを受け取る。 */
public class TaxReturnSectionForm {
    private List<String> visibleSections = new ArrayList<>();
    private String csrfToken;

    public List<String> getVisibleSections() { return visibleSections; }
    public void setVisibleSections(List<String> visibleSections) { this.visibleSections = visibleSections; }
    public String getCsrfToken() { return csrfToken; }
    public void setCsrfToken(String csrfToken) { this.csrfToken = csrfToken; }
}
