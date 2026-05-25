package org.example.features.expense.service;

import org.example.features.expense.dto.ExpenseRegistrationResult;
import org.example.features.expense.dto.ExpenseEditView;
import org.example.features.expense.form.ExpenseForm;

import java.math.BigDecimal;

public interface ExpenseService {

    BigDecimal getDefaultBusinessUseRatio();

    ExpenseRegistrationResult registerExpense(ExpenseForm form);

    ExpenseEditView getExpenseEditView(Long id);
}

