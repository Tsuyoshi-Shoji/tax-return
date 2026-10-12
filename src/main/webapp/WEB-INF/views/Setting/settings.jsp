<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>各種設定 | マネタックス</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/setting/settings.css?v=20261011c">
    <script src="${pageContext.request.contextPath}/css/setting/settings.js?v=20261011c" defer></script>
</head>
<body class="home-page profit-loss-page settings-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main profit-loss-main" tabindex="-1">
        <div class="profit-loss-container">
            <nav class="profit-loss-breadcrumb" aria-label="パンくず">
                <ol>
                    <li><a href="${pageContext.request.contextPath}/home">ホーム</a></li>
                    <li aria-current="page">各種設定</li>
                </ol>
            </nav>

            <section class="profit-loss-title-area" aria-labelledby="page-title">
                <h1 id="page-title">各種設定</h1>
                <p id="page-description">アカウント情報や各種設定の確認・変更を行います。</p>
            </section>

            <section class="profit-loss-card settings-card" aria-labelledby="heading-profile">
                <h2 id="heading-profile">プロフィール設定</h2>
                <p class="settings-desc">ユーザー情報の確認・変更ができます。</p>
                
                <form id="profile-form" method="post" action="${pageContext.request.contextPath}/settings/profile" novalidate>
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                    <div class="settings-grid-3">
                        <div class="settings-field">
                            <label id="label-username" for="display-username">ユーザー名</label>
                            <input id="display-username" name="displayName" type="text" maxlength="100" autocomplete="name" value="<c:out value='${profileForm.displayName}' />" />
                        </div>
                        <div class="settings-field">
                            <label id="label-email" for="display-email">登録メールアドレス</label>
                            <input id="display-email" type="email" value="<c:out value='${view.email}' />" readonly />
                        </div>
                        <div class="settings-field">
                            <span id="label-status" class="settings-label">アカウントステータス</span>
                            <span id="display-status" class="status-badge"><c:out value="${view.statusLabel}" /></span>
                            <small class="settings-hint">現在、このアカウントは有効です。</small>
                        </div>
                    </div>
                    <div class="settings-actions"><button id="button-edit-profile" type="submit" class="btn-outline">プロフィール変更</button></div>
                </form>
            </section>

            <section class="profit-loss-card settings-card" aria-labelledby="heading-password">
                <h2 id="heading-password">パスワードの変更</h2>
                <p class="settings-desc">セキュリティのため、定期的なパスワードの変更をおすすめします。</p>
                
                <form id="password-form" method="post" action="${pageContext.request.contextPath}/settings/password" novalidate>
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                    <div class="settings-grid-pw">
                        <div class="settings-field">
                            <label id="label-current-password" for="input-current-password">現在のパスワード</label>
                            <div class="pw-wrap"><input id="input-current-password" name="currentPassword" type="password" autocomplete="current-password" placeholder="現在のパスワードを入力" />
                                <button id="toggle-current-password" type="button" class="pw-toggle" data-target="input-current-password" aria-label="パスワードを表示" aria-pressed="false">表示</button></div>
                        </div>
                        <div class="settings-field">
                            <label id="label-new-password" for="input-new-password">新しいパスワード</label>
                            <div class="pw-wrap"><input id="input-new-password" name="newPassword" type="password" autocomplete="new-password" maxlength="72" placeholder="新しいパスワードを入力" />
                                <button id="toggle-new-password" type="button" class="pw-toggle" data-target="input-new-password" aria-label="パスワードを表示" aria-pressed="false">表示</button></div>
                            <small class="settings-hint">8〜72文字で入力してください。</small>
                        </div>
                        <div class="settings-field">
                            <label id="label-confirm-password" for="input-confirm-password">新しいパスワード（再入力）</label>
                            <div class="pw-wrap"><input id="input-confirm-password" name="confirmPassword" type="password" autocomplete="new-password" maxlength="72" placeholder="新しいパスワードを再入力" />
                                <button id="toggle-confirm-password" type="button" class="pw-toggle" data-target="input-confirm-password" aria-label="パスワードを表示" aria-pressed="false">表示</button></div>
                        </div>
                        <div class="settings-field settings-submit"><button id="button-update-password" type="submit" class="btn-primary">パスワードを変更</button></div>
                    </div>
                </form>
            </section>

            <section class="profit-loss-card settings-card" aria-labelledby="heading-home-apportionment">
                <h2 id="heading-home-apportionment">家事按分の設定</h2>
                <p id="text-apportionment-description" class="settings-desc">設定した家事按分率は、支出登録画面の計算に反映されます。</p>
                
                <form id="ratio-form" method="post" action="${pageContext.request.contextPath}/settings/business-use-ratio" novalidate>
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                    <div class="settings-grid-ratio">
                        <div>
                            <label id="label-apportionment-rate" class="settings-label" for="input-apportionment-rate">家事按分率</label>
                            <div class="unit-wrap"><input id="input-apportionment-rate" name="ratio" type="text" inputmode="numeric" maxlength="3" placeholder="例）30" value="<c:out value='${ratioForm.ratio}' />" /><span class="unit">%</span></div>
                            <small id="ratio-error" class="settings-error-inline" role="alert"></small>
                            <small class="settings-hint">0〜100の整数で入力してください。</small>
                            <div class="settings-actions settings-actions-left"><button id="button-save-apportionment" type="submit" class="btn-primary">家事按分率を保存</button></div>
                        </div>
                        <div class="example-box">
                            <h3 id="text-apportionment-example">※ 支出登録時の反映例</h3>
                            <p id="display-example-amount">支出金額: 10,000円</p>
                            <p id="display-example-calculation">経費対象金額: <strong id="example-result">-</strong>円</p>
                        </div>
                    </div>
                </form>
            </section>

            <section class="profit-loss-card settings-card" aria-labelledby="heading-account-operations">
                <h2 id="heading-account-operations">アカウント操作</h2>
                <p class="settings-desc">アカウントの無効化または削除を行います。これらの操作は取り消すことができませんのでご注意ください。</p>
                <div class="settings-grid-2">
                    <form id="disable-form" class="danger-box" method="post" action="${pageContext.request.contextPath}/settings/disable" novalidate data-expected="無効化" data-confirm-message="アカウントを無効化します。ログインできなくなります。よろしいですか？">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <h3>アカウントの無効化</h3>
                        <p id="text-disable-description" class="settings-desc">このアカウントを無効化します。ログインできなくなります。</p>
                        
                        <label id="label-disable-confirmation" for="input-disable-confirmation">『無効化』と入力してください</label>
                        <input id="input-disable-confirmation" name="confirmText" type="text" autocomplete="off" placeholder="無効化" />
                        <label for="input-disable-password">現在のパスワード</label>
                        <input id="input-disable-password" name="currentPassword" type="password" autocomplete="current-password" />
                        <small class="settings-error-inline form-error" role="alert"></small>
                        <button id="button-disable-account" type="submit" class="btn-warning">アカウントを無効化</button>
                    </form>
                    <form id="delete-form" class="danger-box" method="post" action="${pageContext.request.contextPath}/settings/delete" novalidate data-expected="削除" data-confirm-message="アカウントを削除します。この操作は取り消せません。よろしいですか？">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <h3>アカウントの削除</h3>
                        <p id="text-delete-description" class="settings-desc">このアカウントと全てのデータを完全に削除します。この操作は取り消せません。</p>
                        
                        <label id="label-delete-confirmation" for="input-delete-confirmation">『削除』と入力してください</label>
                        <input id="input-delete-confirmation" name="confirmText" type="text" autocomplete="off" placeholder="削除" />
                        <label for="input-delete-password">現在のパスワード</label>
                        <input id="input-delete-password" name="currentPassword" type="password" autocomplete="current-password" />
                        <small class="settings-error-inline form-error" role="alert"></small>
                        <button id="button-delete-account" type="submit" class="btn-danger">アカウントを削除</button>
                    </form>
                </div>
            </section>
        </div>
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
<jsp:include page="/WEB-INF/views/Common/app-dialog.jsp" />
</body>
</html>