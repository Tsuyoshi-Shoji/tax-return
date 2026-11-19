package org.example.features.taxreturn.service;

import java.util.List;

import org.example.features.taxreturn.dto.CardSettlementPageData;
import org.example.features.taxreturn.form.CardSettlementForm;

public interface CardSettlementService {
    CardSettlementPageData load(Long appUserId, int year);

    List<String> save(Long appUserId, Integer year, CardSettlementForm form);

    /**
     * 登録済みの引き落とし日（支払日）を修正する。修正した引き落とし仕訳は未確認に戻る。
     *
     * @param currentDate 画面表示時の引き落とし日。表示後に変更されていた場合は修正しない。
     */
    List<String> updateSettlementDate(Long appUserId, Integer year, Long settlementTransactionId,
            String currentDate, String settlementDate);
}

