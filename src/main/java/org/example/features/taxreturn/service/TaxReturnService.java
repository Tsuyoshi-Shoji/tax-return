package org.example.features.taxreturn.service;

import java.util.List;

import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.form.SettlementForm;

public interface TaxReturnService {
    TaxReturnPageData load(Long appUserId, Integer year, SettlementForm overrideForm);

    List<String> saveSettlement(Long appUserId, Integer year, SettlementForm form);

    List<String> updateDeduction(Long appUserId, Integer year, String deduction);
}
