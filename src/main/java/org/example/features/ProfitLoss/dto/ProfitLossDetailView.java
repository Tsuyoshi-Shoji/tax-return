package org.example.features.profitloss.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProfitLossDetailView(
        Long id,
        String recordType,
        LocalDate date,
        String type,
        String item,
        BigDecimal amount,
        String description) {
}

