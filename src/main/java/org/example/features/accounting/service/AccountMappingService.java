package org.example.features.accounting.service;

import org.example.features.accounting.AccountMapping;

public interface AccountMappingService {
    /** 勘定科目・様式欄・品目対応の未登録分を投入する（冪等）。登録済みの行は変更しない。 */
    void ensureMasterData();

    /** 未確定年度の仕訳明細で科目コード未設定の行に、マスタの科目コードを付与する（冪等）。 */
    int backfillJournalAccountCodes();

    AccountMapping snapshot();
}
