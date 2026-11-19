package org.example.features.taxreturn.form;

import java.util.ArrayList;
import java.util.List;

public class CardSettlementForm {
    private String settlementDate;
    private String accountCode = "BANK_DEPOSIT";
    private List<String> sourceTransactionIds = new ArrayList<>();
    private String feeAmount = "0";
    private String memo;
    private String idempotencyKey;
    private String csrfToken;

    public String getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public List<String> getSourceTransactionIds() {
        return sourceTransactionIds;
    }

    public void setSourceTransactionIds(List<String> sourceTransactionIds) {
        this.sourceTransactionIds = sourceTransactionIds == null ? new ArrayList<>() : sourceTransactionIds;
    }

    public String getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(String feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }
}

