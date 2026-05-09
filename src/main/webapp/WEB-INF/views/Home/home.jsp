<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Home</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/home/home.css" />
</head>
<body>
    <!-- ヘッダーをインクルード -->
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <!-- メインコンテンツ -->
    <main>
        <h1>Spring MVC 動いてます 🎉</h1>
        <p>メッセージ: ${message}</p>
        <p>サーバー時刻: ${serverTime}</p>
    </main>

    <!-- フッターをインクルード -->
    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>


