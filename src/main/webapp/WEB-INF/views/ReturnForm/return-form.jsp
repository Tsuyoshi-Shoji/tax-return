<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>申告書作成</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/return-form/return-form.css" />
</head>
<body>
    <!-- ヘッダーをインクルード -->
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <!-- メインコンテンツ -->
    <main>
        <h1>申告書作成</h1>
        <p>ここで税務申告書を作成できます。</p>
        <p>入力された収益と経費の情報から自動的に申告書を生成します。</p>
        <ul>
            <li>基本情報の入力</li>
            <li>収入額の確認</li>
            <li>経費の確認</li>
            <li>申告内容の確認</li>
            <li>申告書の生成</li>
        </ul>
    </main>

    <!-- フッターをインクルード -->
    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>


