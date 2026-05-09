<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>損益レポート</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/report/report.css" />
</head>
<body>
    <!-- ヘッダーをインクルード -->
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <!-- メインコンテンツ -->
    <main>
        <h1>損益レポート</h1>
        <p>登録された収益と経費から自動的に作成される損益レポートです。</p>

        <h2>損益計算書（概要）</h2>
        <table border="1" style="width: 100%; max-width: 600px; margin-top: 20px;">
            <tr>
                <td><strong>総収益</strong></td>
                <td>¥0</td>
            </tr>
            <tr>
                <td><strong>総経費</strong></td>
                <td>¥0</td>
            </tr>
            <tr style="background-color: #f0f0f0;">
                <td><strong>利益/損失</strong></td>
                <td><strong>¥0</strong></td>
            </tr>
        </table>

        <h2>詳細レポート：</h2>
        <ul>
            <li>月別の収支レポート</li>
            <li>経費カテゴリ別の内訳</li>
            <li>収益源別の分析</li>
            <li>利益率の推移</li>
            <li>グラフによる可視化</li>
        </ul>
    </main>

    <!-- フッターをインクルード -->
    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>


