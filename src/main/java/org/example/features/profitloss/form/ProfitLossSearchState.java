package org.example.features.profitloss.form;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.web.util.UriComponentsBuilder;

public final class ProfitLossSearchState {
    private static final int MAX_QUERY_LENGTH = 16_384;
    private static final Pattern ITEM_VALUE_PATTERN = Pattern.compile("^(income|expense):[0-9]{1,19}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[0-9]{1,18}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private ProfitLossSearchState() {
    }

    public static String toQuery(ProfitLossSearchForm form) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        add(builder, "dateFrom", validDate(form.getDateFrom()));
        add(builder, "dateTo", validDate(form.getDateTo()));
        if ("show".equals(form.getTaxTarget()) || "hide".equals(form.getTaxTarget())) {
            add(builder, "taxTarget", form.getTaxTarget());
        }
        if (form.getTypes() != null) {
            form.getTypes().stream()
                    .filter(type -> "income".equals(type) || "expense".equals(type))
                    .distinct()
                    .forEach(type -> builder.queryParam("types", type));
        }
        if (form.getItems() != null) {
            form.getItems().stream()
                    .filter(ProfitLossSearchState::validItem)
                    .distinct()
                    .limit(100)
                    .forEach(item -> builder.queryParam("items", item));
        }
        add(builder, "amountFrom", validAmount(form.getAmountFrom()));
        add(builder, "amountTo", validAmount(form.getAmountTo()));
        add(builder, "incomePage", validPage(form.getIncomePage()));
        add(builder, "expensePage", validPage(form.getExpensePage()));
        return builder.build().encode().getQuery();
    }

    public static String normalizeQuery(String query) {
        if (query == null || query.isBlank() || query.length() > MAX_QUERY_LENGTH) {
            return "";
        }
        ProfitLossSearchForm form = new ProfitLossSearchForm();
        form.setTypes(List.of());
        form.setItems(List.of());
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String name = decode(pair.substring(0, separator));
            String value = decode(pair.substring(separator + 1));
            switch (name) {
                case "dateFrom" -> form.setDateFrom(value);
                case "dateTo" -> form.setDateTo(value);
                case "taxTarget" -> form.setTaxTarget(value);
                case "types" -> {
                    if (("income".equals(value) || "expense".equals(value))
                            && form.getTypes().size() < 2 && !form.getTypes().contains(value)) {
                        form.getTypes().add(value);
                    }
                }
                case "items" -> {
                    if (validItem(value) && form.getItems().size() < 100) {
                        form.getItems().add(value);
                    }
                }
                case "amountFrom" -> form.setAmountFrom(value);
                case "amountTo" -> form.setAmountTo(value);
                case "incomePage" -> form.setIncomePage(parsePage(value));
                case "expensePage" -> form.setExpensePage(parsePage(value));
                default -> {
                }
            }
        }
        return toQuery(form);
    }

    private static void add(UriComponentsBuilder builder, String name, String value) {
        if (value != null) {
            builder.queryParam(name, value);
        }
    }

    private static String validDate(String value) {
        if (value == null || !DATE_PATTERN.matcher(value).matches()) {
            return null;
        }
        try {
            LocalDate.parse(value);
            return value;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String validAmount(String value) {
        return value != null && AMOUNT_PATTERN.matcher(value).matches() ? value : null;
    }

    private static String validPage(Integer value) {
        return value != null && value > 0 && value <= 100_000 ? value.toString() : null;
    }

    private static Integer parsePage(String value) {
        try {
            int page = Integer.parseInt(value);
            return page > 0 && page <= 100_000 ? page : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean validItem(String value) {
        return value != null && ITEM_VALUE_PATTERN.matcher(value).matches();
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
