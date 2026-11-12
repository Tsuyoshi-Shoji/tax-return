package org.example.features.taxreturn.closing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 年度締め・訂正履歴・出力記録の画面表示データ。 */
public class ClosingPageData {
    private final boolean closed;
    private final Integer currentVersion;
    private final LocalDateTime closedAt;
    private final String currentFingerprint;
    private final String calcRuleVersion;
    private final String formEdition;
    private final List<String> blockingOpenYears;
    private final List<String> blockingClosedYears;
    private final List<EventView> events;
    private final List<OutputView> outputs;
    private final long outputCount;

    public ClosingPageData(boolean closed, Integer currentVersion, LocalDateTime closedAt, String currentFingerprint,
            String calcRuleVersion, String formEdition, List<String> blockingOpenYears,
            List<String> blockingClosedYears, List<EventView> events, List<OutputView> outputs, long outputCount) {
        this.closed = closed;
        this.currentVersion = currentVersion;
        this.closedAt = closedAt;
        this.currentFingerprint = currentFingerprint;
        this.calcRuleVersion = calcRuleVersion;
        this.formEdition = formEdition;
        this.blockingOpenYears = blockingOpenYears;
        this.blockingClosedYears = blockingClosedYears;
        this.events = events;
        this.outputs = outputs;
        this.outputCount = outputCount;
    }

    public boolean isClosed() { return closed; }
    public Integer getCurrentVersion() { return currentVersion; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getCurrentFingerprint() { return currentFingerprint; }
    public String getCalcRuleVersion() { return calcRuleVersion; }
    public String getFormEdition() { return formEdition; }
    public List<String> getBlockingOpenYears() { return blockingOpenYears; }
    public List<String> getBlockingClosedYears() { return blockingClosedYears; }
    public List<EventView> getEvents() { return events; }
    public List<OutputView> getOutputs() { return outputs; }
    public long getOutputCount() { return outputCount; }
    /** 再締め時に訂正理由が必要か（過去に締めたことがある年度）。 */
    public boolean isCorrectionRequired() { return events.stream().anyMatch(EventView::isClose); }

    public record EventView(String eventType, int closeVersion, LocalDateTime occurredAt, String reason,
            BigDecimal sales, BigDecimal expenses, BigDecimal income, BigDecimal incomeAfterDeduction,
            int deduction, BigDecimal assetsClosing, int journalLineCount, BigDecimal debitTotal,
            String fingerprint, String calcRuleVersion, String formEdition, String changeSummary) {
        public boolean isClose() { return "CLOSE".equals(eventType); }
        public String getEventLabel() { return isClose() ? "締め" : "締めの取消"; }
        public String getEventType() { return eventType; }
        public int getCloseVersion() { return closeVersion; }
        public LocalDateTime getOccurredAt() { return occurredAt; }
        public String getReason() { return reason; }
        public BigDecimal getSales() { return sales; }
        public BigDecimal getExpenses() { return expenses; }
        public BigDecimal getIncome() { return income; }
        public BigDecimal getIncomeAfterDeduction() { return incomeAfterDeduction; }
        public int getDeduction() { return deduction; }
        public BigDecimal getAssetsClosing() { return assetsClosing; }
        public int getJournalLineCount() { return journalLineCount; }
        public BigDecimal getDebitTotal() { return debitTotal; }
        public String getShortFingerprint() { return fingerprint.substring(0, 12); }
        public String getCalcRuleVersion() { return calcRuleVersion; }
        public String getFormEdition() { return formEdition; }
        public String getChangeSummary() { return changeSummary; }
    }

    public record OutputView(Long id, String outputType, String outputLabel, String fileName, LocalDateTime generatedAt,
            String calcRuleVersion, String formEdition, String yearStatus, Integer closeVersion, String fileSha256,
            long fileSize, String status, boolean matchesCurrent, String cancelReason, LocalDateTime cancelledAt) {
        public Long getId() { return id; }
        public String getOutputType() { return outputType; }
        public String getOutputLabel() { return outputLabel; }
        public String getFileName() { return fileName; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public String getCalcRuleVersion() { return calcRuleVersion; }
        public String getFormEdition() { return formEdition; }
        public String getYearStatus() { return yearStatus; }
        public Integer getCloseVersion() { return closeVersion; }
        public String getShortSha256() { return fileSha256.substring(0, 12); }
        public String getFileSha256() { return fileSha256; }
        public long getFileSize() { return fileSize; }
        public String getStatus() { return status; }
        public boolean isMatchesCurrent() { return matchesCurrent; }
        public String getCancelReason() { return cancelReason; }
        public LocalDateTime getCancelledAt() { return cancelledAt; }
        public boolean isCancellable() { return !"CANCELLED".equals(status); }
        public String getStatusLabel() {
            return switch (status) {
                case "CANCELLED" -> "取消";
                case "SUPERSEDED" -> "旧版（締め取消済み）";
                default -> matchesCurrent ? "現行" : "現データと不一致";
            };
        }
        public String getStateLabel() {
            return "closed".equals(yearStatus) ? "締め第" + closeVersion + "版" : "未締め";
        }
    }
}
