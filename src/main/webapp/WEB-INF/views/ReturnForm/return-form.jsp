<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>申告書作成</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/return-form/return-form.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <main>
        <div class="container">
            <div class="page-header">
                <h1>申告書作成</h1>
                <p class="page-subtitle">当年の収益・経費集計をもとに、申告準備に必要な基礎金額を確認します。</p>
            </div>

            <section class="section">
                <div class="section-header">
                    <h2><c:out value="${returnFormView.year}" />年 集計サマリー</h2>
                </div>
                <table class="form-table">
                    <tbody>
                        <tr>
                            <td class="label">総収益</td>
                            <td class="value">¥<fmt:formatNumber value="${returnFormView.totalIncome}" pattern="#,##0" /></td>
                        </tr>
                        <tr>
                            <td class="label">経費対象金額</td>
                            <td class="value">¥<fmt:formatNumber value="${returnFormView.deductibleExpense}" pattern="#,##0" /></td>
                        </tr>
                        <tr>
                            <td class="label">経費対象外支出</td>
                            <td class="value">¥<fmt:formatNumber value="${returnFormView.nonDeductibleExpense}" pattern="#,##0" /></td>
                        </tr>
                        <tr class="total">
                            <td class="label">課税対象の概算利益</td>
                            <td class="value">¥<fmt:formatNumber value="${returnFormView.taxableProfit}" pattern="#,##0" /></td>
                        </tr>
                    </tbody>
                </table>
            </section>

            <section class="section">
                <div class="section-header">
                    <h2>次のアクション</h2>
                </div>
                <ul>
                    <li>収益・支出の未登録があれば、先に入力してください。</li>
                    <li>損益一覧で明細を確認し、不要なデータは修正または削除してください。</li>
                    <li>損益レポートで月次推移を確認し、申告前のチェックに活用してください。</li>
                </ul>
                <div style="display:flex; gap:16px; flex-wrap:wrap; margin-top:16px;">
                    <a class="btn-secondary" href="${pageContext.request.contextPath}/income">収益登録へ</a>
                    <a class="btn-secondary" href="${pageContext.request.contextPath}/expense">支出登録へ</a>
                    <a class="btn-primary" href="${pageContext.request.contextPath}/report">損益レポートへ</a>
                </div>
            </section>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>

