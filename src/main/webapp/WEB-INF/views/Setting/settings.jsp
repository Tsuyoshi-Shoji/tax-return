<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>設定</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/settings/settings.css" />
</head>
<body>
    <!-- ヘッダーをインクルード -->
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

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
    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>


