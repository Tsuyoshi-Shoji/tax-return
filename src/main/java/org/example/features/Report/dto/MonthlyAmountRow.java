package org.example.features.report.dto;

import java.math.BigDecimal;

public record MonthlyAmountRow(int month, BigDecimal income, BigDecimal expense) {
}

