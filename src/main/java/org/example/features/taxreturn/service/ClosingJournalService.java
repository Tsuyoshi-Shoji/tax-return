package org.example.features.taxreturn.service;

/**
 * 決算整理仕訳（棚卸・売掛金等の期末残高調整）と固定資産台帳による減価償却仕訳を再生成する。
 * 取引・決算整理・固定資産の変更後に、同一トランザクション内で呼び出す。確定済み年度は変更しない。
 */
public interface ClosingJournalService {
    void rebuild(Long fiscalYearId);

    /** 事業者の未確定年度すべてを再生成する（固定資産の追加・削除時など）。 */
    void rebuildOpenYears(Long businessProfileId);
}
