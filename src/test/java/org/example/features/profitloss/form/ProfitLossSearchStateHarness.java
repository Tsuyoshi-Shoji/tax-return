package org.example.features.profitloss.form;

import java.util.List;

public final class ProfitLossSearchStateHarness {
    public static void main(String[] args) {
        ProfitLossSearchForm form = new ProfitLossSearchForm();
        form.setDateFrom("2025-01-01");
        form.setDateTo("2025-12-31");
        form.setTypes(List.of("income"));
        form.setTaxTarget("hide");
        form.setItems(List.of("income:12"));
        form.setAmountFrom("1000");
        form.setAmountTo("50000");
        form.setIncomePage(2);
        form.setExpensePage(3);

        String query = ProfitLossSearchState.toQuery(form);
        require(!query.isEmpty(), "Search criteria were not serialized");
        require(query.equals(ProfitLossSearchState.normalizeQuery(query)), "Search criteria did not survive a round trip");

        String sanitized = ProfitLossSearchState.normalizeQuery(query + "&returnUrl=https%3A%2F%2Fevil.example&types=invalid");
        require(query.equals(sanitized), "Unsupported return-query values were retained");

        System.out.println("Profit-loss search state harness passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
