package org.example.features.profitloss.service;

import org.example.features.profitloss.dto.ProfitLossDetailView;
import org.example.features.profitloss.dto.ProfitLossSearchResult;
import org.example.features.profitloss.form.ProfitLossSearchForm;

public interface ProfitLossService {

    ProfitLossSearchResult search(ProfitLossSearchForm form);

    ProfitLossDetailView getDetail(String recordType, Long id);

    void delete(String recordType, Long id);
}

