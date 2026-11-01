package org.example.features.taxreturn.export;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.example.features.taxreturn.consumption.ConsumptionTaxCalculator;
import org.example.features.taxreturn.dto.TaxReturnExtras;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.dto.TaxReturnPageData.BsRow;
import org.example.features.taxreturn.dto.TaxReturnPageData.StatementRow;
import org.example.features.taxreturn.incometax.IncomeTaxCalculator;

/**
 * 確定申告書等作成コーナー・e-Taxソフトへの転記を補助する欄別一覧（CSV）を作成する。
 * e-Taxの提出データ（XTX）ではない。表計算ソフトでの数式実行を防ぐため、数値以外の値は先頭文字をエスケープする。
 */
public final class EtaxAssistCsv {
    private static final Pattern NUMBER = Pattern.compile("-?\\d+");
    private static final String BOM = "\uFEFF";

    private EtaxAssistCsv() {
    }

    public static String build(TaxReturnPageData page) {
        TaxReturnExtras x = page.getExtras();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"帳票", "区分", "項目", "金額・内容"});
        String year = page.getYearLabel();
        rows.add(new String[] {"共通", "対象年分", "年分", year});
        rows.add(new String[] {"共通", "申告者", "氏名", x.getFiler().getName()});
        rows.add(new String[] {"共通", "申告者", "フリガナ", x.getFiler().getNameKana()});
        rows.add(new String[] {"共通", "申告者", "生年月日", x.getFiler().getBirthDate()});
        rows.add(new String[] {"共通", "申告者", "住所（納税地）", x.getFiler().getAddress()});
        rows.add(new String[] {"共通", "申告者", "屋号", x.getFiler().getBusinessName()});
        rows.add(new String[] {"共通", "申告者", "職業（業種）", x.getFiler().getIndustry()});
        rows.add(new String[] {"共通", "申告者", "提出先税務署", x.getFiler().getTaxOffice()});
        rows.add(new String[] {"共通", "申告者", "世帯主の氏名", x.getFiler().getHouseholdHeadName()});
        rows.add(new String[] {"共通", "申告者", "世帯主との続柄", x.getFiler().getHouseholdHeadRelation()});

        for (StatementRow r : page.getStatementRows()) {
            rows.add(new String[] {"青色申告決算書", "損益計算書", r.getNumberLabel() + " " + r.getLabel(), amount(r.getAmount())});
        }
        for (TaxReturnExtras.MonthRow m : x.getMonthlySalesPurchases()) {
            rows.add(new String[] {"青色申告決算書", "月別売上（収入）金額", m.getMonth() + "月", amount(m.getSales())});
            rows.add(new String[] {"青色申告決算書", "月別仕入金額", m.getMonth() + "月", amount(m.getPurchases())});
        }
        rows.add(new String[] {"青色申告決算書", "月別売上（収入）金額", "雑収入", amount(x.getMiscIncome())});
        for (TaxReturnExtras.AssetRow a : x.getAssets()) {
            String key = a.getName() + "（" + a.getAccountName() + "・取得" + a.getAcquired() + "）";
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 取得価額", amount(a.getCost())});
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 償却方法", a.getMethodLabel()});
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 耐用年数", a.getUsefulLife() == null ? "" : a.getUsefulLife().toString()});
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 償却率", a.getRate() == null ? "" : a.getRate().toPlainString()});
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 本年中の償却期間", a.getMonths() + "/12"});
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 本年分の償却費合計", amount(a.getDepreciation())});
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 事業専用割合(%)", amount(a.getBusinessRatio())});
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 本年分の必要経費算入額", amount(a.getBusinessPortion())});
            rows.add(new String[] {"青色申告決算書", "減価償却費の計算", key + " 未償却残高（期末残高）", amount(a.getClosing())});
        }
        payees(rows, "地代家賃の内訳", x.getRentRows());
        payees(rows, "利子割引料の内訳", x.getInterestRows());
        payees(rows, "税理士・弁護士等の報酬・料金の内訳", x.getFeeRows());
        for (BsRow r : page.getAssets()) {
            rows.add(new String[] {"青色申告決算書", "貸借対照表（資産の部）", r.getLabel() + " 期首", amount(r.getOpening())});
            rows.add(new String[] {"青色申告決算書", "貸借対照表（資産の部）", r.getLabel() + " 期末", amount(r.getClosing())});
        }
        for (BsRow r : page.getLiabilities()) {
            rows.add(new String[] {"青色申告決算書", "貸借対照表（負債・資本の部）", r.getLabel() + " 期首", amount(r.getOpening())});
            rows.add(new String[] {"青色申告決算書", "貸借対照表（負債・資本の部）", r.getLabel() + " 期末", amount(r.getClosing())});
        }

        IncomeTaxCalculator.Result t = x.getTax();
        if (t != null) {
            String f1 = "確定申告書 第一表";
            rows.add(new String[] {f1, "収入金額等", "事業（営業等）", num(t.getBusinessRevenue())});
            rows.add(new String[] {f1, "収入金額等", "給与", num(t.getSalaryRevenue())});
            rows.add(new String[] {f1, "収入金額等", "雑（業務・その他）", num(t.getMiscRevenue())});
            rows.add(new String[] {f1, "所得金額等", "事業（営業等）", num(t.getBusinessIncome())});
            rows.add(new String[] {f1, "所得金額等", "給与", num(t.getSalaryIncome())});
            rows.add(new String[] {f1, "所得金額等", "雑", num(t.getMiscIncome())});
            rows.add(new String[] {f1, "所得金額等", "合計", num(t.getTotalIncome())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "社会保険料控除", num(t.getSocialInsurance())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "小規模企業共済等掛金控除", num(t.getSmallBusinessMutual())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "生命保険料控除", num(t.getLifeInsurance())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "地震保険料控除", num(t.getEarthquake())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "寡婦、ひとり親控除", num(t.getWidowOrSingleParent())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "勤労学生、障害者控除", num(t.getWorkingStudentOrDisability())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "配偶者控除", num(t.getSpouse())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "配偶者特別控除", num(t.getSpouseSpecial())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "扶養控除", num(t.getDependents())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "特定親族特別控除", num(t.getSpecificRelative())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "基礎控除", num(t.getBasic())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "医療費控除", num(t.getMedical())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "寄附金控除", num(t.getDonation())});
            rows.add(new String[] {f1, "所得から差し引かれる金額", "合計", num(t.getDeductionTotal())});
            rows.add(new String[] {f1, "税金の計算", "課税される所得金額", num(t.getTaxableIncome())});
            rows.add(new String[] {f1, "税金の計算", "上の所得に対する税額", num(t.getIncomeTax())});
            if (t.getFlatRateReductionAvailable() > 0) {
                rows.add(new String[] {f1, "税金の計算", "令和6年分特別税額控除（定額減税）", num(t.getFlatRateReduction())});
            }
            rows.add(new String[] {f1, "税金の計算", "再差引所得税額（基準所得税額）", num(t.getBaseIncomeTax())});
            rows.add(new String[] {f1, "税金の計算", "復興特別所得税額", num(t.getReconstructionTax())});
            rows.add(new String[] {f1, "税金の計算", "所得税及び復興特別所得税の額", num(t.getTotalTax())});
            rows.add(new String[] {f1, "税金の計算", "所得税及び復興特別所得税の源泉徴収税額", num(t.getWithholding())});
            rows.add(new String[] {f1, "税金の計算", "所得税及び復興特別所得税の申告納税額", num(t.getDeclaredTax())});
            rows.add(new String[] {f1, "税金の計算", "予定納税額", num(t.getPrepaidTax())});
            rows.add(new String[] {f1, "税金の計算", "第3期分の税額（納める税金）", num(t.getTaxDue())});
            rows.add(new String[] {f1, "税金の計算", "第3期分の税額（還付される税金）", num(t.getRefund())});
            rows.add(new String[] {f1, "その他", "専従者給与額の合計額", amount(x.getDedicatedSalary())});
            rows.add(new String[] {f1, "その他", "青色申告特別控除額", amount(x.getBlueDeductionApplied())});
            String f2 = "確定申告書 第二表";
            for (TaxReturnExtras.IncomeBreakdownRow r : x.getIncomeBreakdown()) {
                String key = r.getKind() + "／" + r.getItem() + "／" + r.getPayerName()
                        + (r.getPayerAddress() == null ? "" : "（" + r.getPayerAddress() + "）");
                rows.add(new String[] {f2, "所得の内訳", key + " 収入金額", amount(r.getRevenue())});
                rows.add(new String[] {f2, "所得の内訳", key + " 源泉徴収税額", amount(r.getWithholding())});
            }
            for (TaxReturnExtras.LabelAmount r : x.getSocialInsuranceRows()) {
                rows.add(new String[] {f2, "社会保険料控除等に関する事項", r.getLabel(), num(r.getAmount())});
            }
            rows.add(new String[] {f2, "生命保険料控除に関する事項", "新生命保険料", num(t.getLifeGeneralPaid())});
            rows.add(new String[] {f2, "生命保険料控除に関する事項", "介護医療保険料", num(t.getLifeMedicalPaid())});
            rows.add(new String[] {f2, "生命保険料控除に関する事項", "新個人年金保険料", num(t.getLifePensionPaid())});
            rows.add(new String[] {f2, "地震保険料控除に関する事項", "地震保険料", num(t.getEarthquakePaid())});
            for (IncomeTaxCalculator.DependentRow d : t.getDependentRows()) {
                rows.add(new String[] {f2, "配偶者や親族に関する事項", d.getDependent().getDependentName() + "（"
                        + d.getDependent().getRelationship() + "・" + d.getDependent().getBirthDate() + "）", d.getCategory()});
            }
        }

        ConsumptionTaxCalculator.Result ct = x.getConsumption();
        if (ct != null) {
            String f3 = "消費税及び地方消費税の申告書（簡易課税用）";
            rows.add(new String[] {f3, "事業区分", "第" + ct.getBusinessType() + "種事業", ct.getDeemedRate() + "%"});
            rows.add(new String[] {f3, "課税売上高", "税込（10%）", amount(ct.getSales10())});
            rows.add(new String[] {f3, "課税売上高", "税込（8%）", amount(ct.getSales8())});
            rows.add(new String[] {f3, "税額の計算", "課税標準額", amount(ct.getTaxableBase())});
            rows.add(new String[] {f3, "税額の計算", "消費税額", amount(ct.getConsumptionTax())});
            rows.add(new String[] {f3, "税額の計算", "控除対象仕入税額", amount(ct.getDeemedPurchaseTax())});
            rows.add(new String[] {f3, "税額の計算", "差引税額（納付税額）", amount(ct.getNetTax())});
            rows.add(new String[] {f3, "地方消費税", "譲渡割額（納税額）", amount(ct.getLocalTax())});
            rows.add(new String[] {f3, "合計", "消費税及び地方消費税の合計（納付）税額", amount(ct.getTotalTax())});
        }

        StringBuilder sb = new StringBuilder(BOM);
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(cell(row[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private static void payees(List<String[]> rows, String section, List<TaxReturnExtras.PayeeRow> payees) {
        for (TaxReturnExtras.PayeeRow p : payees) {
            String key = p.getName() + (p.getAddress() == null ? "" : "（" + p.getAddress() + "）");
            rows.add(new String[] {"青色申告決算書", section, key + " 本年中の金額", amount(p.getAmount())});
            rows.add(new String[] {"青色申告決算書", section, key + " 必要経費算入額", amount(p.getDeductible())});
        }
    }

    private static String amount(BigDecimal v) {
        return v == null ? "0" : v.setScale(0, java.math.RoundingMode.DOWN).toPlainString();
    }

    private static String num(long v) {
        return Long.toString(v);
    }

    public static String cell(String raw) {
        String value = raw == null ? "" : raw.replace("\r", " ").replace("\n", " ");
        if (!value.isEmpty() && !NUMBER.matcher(value).matches()) {
            char first = value.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t') {
                value = "'" + value;
            }
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
