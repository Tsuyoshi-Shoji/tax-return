package org.example.features.profitloss.service;

import org.example.features.profitloss.dto.ProfitLossDetailDto;
import org.example.features.profitloss.dto.ProfitLossPageData;
import org.example.features.profitloss.form.ProfitLossSearchForm;

public interface ProfitLossService {
    ProfitLossPageData loadPage(Long appUserId, ProfitLossSearchForm requestForm);

    ProfitLossDetailDto loadDetail(Long appUserId, String recordType, Long id);

    void delete(Long appUserId, String recordType, Long id);
}