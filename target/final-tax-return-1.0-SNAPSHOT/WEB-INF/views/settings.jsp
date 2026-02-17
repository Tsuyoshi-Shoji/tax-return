<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>設定</title>
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
        <h1>設定</h1>
        <p>アプリケーションの各種設定をここから行います。</p>

        <h2>設定可能な項目：</h2>
        <ul>
            <li><strong>アカウント設定</strong>
                <ul>
                    <li>ユーザー名の変更</li>
                    <li>メールアドレスの変更</li>
                    <li>パスワードの変更</li>
                </ul>
            </li>
            <li><strong>事業情報設定</strong>
                <ul>
                    <li>事業名</li>
                    <li>所在地</li>
                    <li>事業形態</li>
                    <li>開業日</li>
                </ul>
            </li>
            <li><strong>表示設定</strong>
                <ul>
                    <li>言語の選択</li>
                    <li>テーマの選択</li>
                    <li>通知設定</li>
                </ul>
            </li>
            <li><strong>その他</strong>
                <ul>
                    <li>データのエクスポート</li>
                    <li>データのバックアップ</li>
                    <li>アカウントの削除</li>
                </ul>
            </li>
        </ul>
    </main>

    <!-- フッターをインクルード -->
    <jsp:include page="footer.jsp" />
</body>
</html>
