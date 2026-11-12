package org.example.integration;

import java.time.LocalDate;

import org.example.features.taxreturn.requirements.FilingRequirements;
import org.example.features.taxreturn.requirements.FilingRequirements.Input;
import org.example.features.taxreturn.requirements.FilingRequirements.Necessity;
import org.example.features.taxreturn.requirements.FilingRequirements.Result;
import org.example.features.taxreturn.requirements.FilingRequirements.Status;

/** 申告方式・控除区分ごとの必要書類と出力前検出を確認する。 */
public class FilingRequirementsHarness {
    private static final LocalDate DEADLINE = LocalDate.of(2026, 3, 16);
    private static final LocalDate BEFORE = LocalDate.of(2026, 2, 1);

    public static void main(String[] args) {
        Result ten = FilingRequirements.evaluate(input("blue", 10, null, true, BEFORE));
        require(ten.getErrors().isEmpty() && ten.getWarnings().isEmpty() && ten.isSatisfied(), "10万円控除の正常系: " + ten.getWarnings());
        require(necessity(ten, "req-simple-books") == Necessity.REQUIRED
                && necessity(ten, "req-journal-ledger") == Necessity.OPTIONAL
                && necessity(ten, "req-balance-sheet") == Necessity.OPTIONAL
                && status(ten, "req-deadline") == Status.NOT_APPLICABLE, "10万円控除の必要書類区分が不正です");

        Result fiftyFive = FilingRequirements.evaluate(input("blue", 55, null, true, BEFORE));
        require(fiftyFive.getWarnings().isEmpty() && fiftyFive.isSatisfied(), "55万円控除の正常系: " + fiftyFive.getWarnings());
        require(necessity(fiftyFive, "req-journal-ledger") == Necessity.REQUIRED
                && necessity(fiftyFive, "req-balance-sheet") == Necessity.REQUIRED
                && necessity(fiftyFive, "req-simple-books") == Necessity.OPTIONAL
                && status(fiftyFive, "req-deadline") == Status.USER_CHECK
                && status(fiftyFive, "req-65-additional") == Status.NOT_APPLICABLE, "55万円控除の必要書類区分が不正です");

        Result late = FilingRequirements.evaluate(input("blue", 55, null, true, DEADLINE.plusDays(1)));
        require(status(late, "req-deadline") == Status.MISSING
                && late.getWarnings().stream().anyMatch(w -> w.contains("10万円控除が上限")), "期限後の55万円控除を検出しません");
        Result onDeadline = FilingRequirements.evaluate(input("blue", 55, null, true, DEADLINE));
        require(status(onDeadline, "req-deadline") == Status.USER_CHECK, "期限当日を期限後と判定しました");
        Result tenLate = FilingRequirements.evaluate(input("blue", 10, null, true, DEADLINE.plusDays(1)));
        require(tenLate.getWarnings().isEmpty(), "10万円控除で期限後警告が出ました");
        Result unknownDeadline = FilingRequirements.evaluate(new Input("blue", 55, null, true,
                true, true, 0, true, true, true, false, null, BEFORE));
        require(status(unknownDeadline, "req-deadline") == Status.USER_CHECK && unknownDeadline.getWarnings().isEmpty(),
                "未確定期限の扱いが不正です");

        Result unconfirmed65 = FilingRequirements.evaluate(input("blue", 65, "UNCONFIRMED", false, BEFORE));
        require(status(unconfirmed65, "req-65-additional") == Status.MISSING
                && status(unconfirmed65, "req-approval") == Status.USER_CHECK
                && unconfirmed65.getWarnings().size() == 2 && unconfirmed65.getErrors().isEmpty(), "65万円控除の未確認を検出しません");
        Result etax = FilingRequirements.evaluate(input("blue", 65, "ETAX", true, BEFORE));
        require(status(etax, "req-65-additional") == Status.USER_CHECK && etax.isSatisfied()
                && etax.getWarnings().stream().anyMatch(w -> w.contains("本アプリでは送信・完了確認できません")), "e-Tax経路の確認事項が不正です");
        Result ebooks = FilingRequirements.evaluate(input("blue", 65, "QUALIFIED_EBOOKS", true, BEFORE));
        require(status(ebooks, "req-65-additional") == Status.MISSING && !ebooks.isSatisfied(), "優良電子帳簿経路を充足扱いしました");

        Result noBooks = FilingRequirements.evaluate(new Input("blue", 55, null, true,
                false, true, 0, false, true, false, false, DEADLINE, BEFORE));
        require(status(noBooks, "req-journal-ledger") == Status.MISSING && status(noBooks, "req-profit-loss") == Status.MISSING
                && status(noBooks, "req-balance-sheet") == Status.USER_CHECK && !noBooks.getWarnings().isEmpty(),
                "帳簿・決算書の不足を検出しません");
        Result unbalanced = FilingRequirements.evaluate(new Input("blue", 55, null, true,
                true, false, 2, true, false, true, true, DEADLINE, BEFORE));
        require(status(unbalanced, "req-journal-ledger") == Status.MISSING && status(unbalanced, "req-balance-sheet") == Status.MISSING
                && status(unbalanced, "req-fixed-asset-ledger") == Status.OK
                && necessity(unbalanced, "req-fixed-asset-ledger") == Necessity.CONDITIONAL, "不整合の検出が不正です");
        Result drafts = FilingRequirements.evaluate(new Input("blue", 55, null, true,
                true, true, 3, true, true, true, false, DEADLINE, BEFORE));
        require(status(drafts, "req-journal-ledger") == Status.USER_CHECK, "未確認仕訳を利用者確認にしていません");

        Result white = FilingRequirements.evaluate(input("white", 65, "ETAX", true, BEFORE));
        require(!white.getErrors().isEmpty() && !white.isSatisfied(), "白色申告の出力を拒否しません");
        Result unknownType = FilingRequirements.evaluate(input(null, 55, null, true, BEFORE));
        require(!unknownType.getErrors().isEmpty(), "申告方式未設定を拒否しません");
        System.out.println("FilingRequirementsHarness OK");
    }

    private static Input input(String type, int deduction, String method, boolean confirmed, LocalDate today) {
        return new Input(type, deduction, method, confirmed, true, true, 0, true, true, true, false, DEADLINE, today);
    }

    private static Status status(Result result, String id) {
        return result.item(id).getStatus();
    }

    private static Necessity necessity(Result result, String id) {
        return result.item(id).getNecessity();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
