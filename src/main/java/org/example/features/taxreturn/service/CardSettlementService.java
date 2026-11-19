package org.example.features.taxreturn.service;

import java.util.List;

import org.example.features.taxreturn.dto.CardSettlementPageData;
import org.example.features.taxreturn.form.CardSettlementForm;

public interface CardSettlementService {
    CardSettlementPageData load(Long appUserId, int year);

    List<String> save(Long appUserId, Integer year, CardSettlementForm form);
}

