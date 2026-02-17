<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>申告書作成</title>
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
    <jsp:include page="footer.jsp" />
</body>
</html>
