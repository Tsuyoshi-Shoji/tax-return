package org.example.features.profitloss.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProfitLossListRow(
        Long id,
        LocalDate date,
        String type,
        String item,
        BigDecimal amount) {
}

