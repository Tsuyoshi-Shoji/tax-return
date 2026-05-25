package org.example.features.home.service;

import java.util.List;
import java.util.Map;

public interface HomeService {
    String getWelcomeMessage();
    String getCurrentTime();

    // README仕様の実装
    List<Map<String, Object>> getRecentTransactions(int limit);
    Map<String, Object> getAnnualProfitLoss();
    Map<String, Object> getAnnualProfitLoss(boolean includeTaxExcluded);
    Map<String, Object> getMonthlyProfitLoss();
    Map<String, Object> getMonthlyProfitLoss(boolean includeTaxExcluded);
    long getDaysUntilTaxDeadline();
    boolean isApproachingDeadline();
}
