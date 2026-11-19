package org.example.features.taxreturn.dto;

import java.math.BigDecimal;
import java.util.List;

public class CardSettlementPageData {
    private final List<Candidate> candidates;
    private final List<AccountOption> accountOptions;
    private final String defaultSettlementDate;
    private final String idempotencyKey;
    private final List<Registered> registered;

    public CardSettlementPageData(List<Candidate> candidates, List<AccountOption> accountOptions,
            String defaultSettlementDate, String idempotencyKey, List<Registered> registered) {
        this.candidates = candidates;
        this.accountOptions = accountOptions;
        this.defaultSettlementDate = defaultSettlementDate;
        this.idempotencyKey = idempotencyKey;
        this.registered = List.copyOf(registered);
    }

    public List<Registered> getRegistered() {
        return registered;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public List<AccountOption> getAccountOptions() {
        return accountOptions;
    }

    public String getDefaultSettlementDate() {
        return defaultSettlementDate;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public static class Candidate {
        private final Long transactionId;
        private final String transactionDate;
        private final String summary;
        private final BigDecimal totalAmount;
        private final BigDecimal settledAmount;
        private final BigDecimal outstandingAmount;

        public Candidate(Long transactionId, String transactionDate, String summary, BigDecimal totalAmount,
                BigDecimal settledAmount, BigDecimal outstandingAmount) {
            this.transactionId = transactionId;
            this.transactionDate = transactionDate;
            this.summary = summary;
            this.totalAmount = totalAmount;
            this.settledAmount = settledAmount;
            this.outstandingAmount = outstandingAmount;
        }

        public Long getTransactionId() {
            return transactionId;
        }

        public String getTransactionDate() {
            return transactionDate;
        }

        public String getSummary() {
            return summary;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public BigDecimal getSettledAmount() {
            return settledAmount;
        }

        public BigDecimal getOutstandingAmount() {
            return outstandingAmount;
        }
    }

    /** 登録済みの引き落とし（支払日の修正対象）。 */
    public record Registered(Long transactionId, String settlementDate, String summary, BigDecimal amount,
            boolean reviewed) {
        public Long getTransactionId() {
            return transactionId;
        }

        public String getSettlementDate() {
            return settlementDate;
        }

        public String getSummary() {
            return summary;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public boolean isReviewed() {
            return reviewed;
        }
    }

    public static class AccountOption {
        private final String code;
        private final String name;

        public AccountOption(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }
}

