<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Home</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: Arial, sans-serif;
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }
        main {
            flex: 1;
            max-width: 1200px;
            margin: 0 auto;
            width: 100%;
            box-sizing: border-box;
            background-color: #f5f5f5;
            /* ヘッダーとフッターの高さ分を引いた領域を確保し、その中だけスクロール */
            height: calc(100vh - var(--header-height) - var(--footer-height));
            overflow: auto;
        }
    </style>
</head>
<body>
    <!-- ヘッダーをインクルード -->
    <jsp:include page="header.jsp" />

    <!-- メインコンテンツ -->
    <main>
        <h1>Spring MVC 動いてます 🎉</h1>
        <p>メッセージ: ${message}</p>
        <p>サーバー時刻: ${serverTime}</p>
    </main>

    <!-- フッターをインクルード -->
    <jsp:include page="footer.jsp" />
</body>
</html>
