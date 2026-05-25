package org.example.features.income.service;

import org.example.features.income.dto.IncomeEditView;
import org.example.features.income.dto.IncomeRegistrationResult;
import org.example.features.income.form.IncomeForm;

public interface IncomeService {

    IncomeRegistrationResult registerIncome(IncomeForm form);

    IncomeEditView getIncomeEditView(Long id);
}

