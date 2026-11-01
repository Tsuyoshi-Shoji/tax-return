<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>各種設定 | マネタックス</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261016a">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/setting/settings.css?v=20261101a">
    <script src="${pageContext.request.contextPath}/css/setting/settings.js?v=20261101a" defer></script>
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

            <section class="profit-loss-card settings-card" aria-labelledby="heading-tax-basic-info">
                <h2 id="heading-tax-basic-info">確定申告の基本情報</h2>
                <p class="settings-desc">確定申告書（第一表・第二表）や消費税の計算に使う基本情報を設定できます。</p>

                <c:if test="${failedSection eq 'taxBasicInfo' and not empty formErrors}">
                    <div class="settings-errors" role="alert" aria-live="polite">
                        <ul>
                            <c:forEach var="message" items="${formErrors}">
                                <li><c:out value="${message}" /></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>

                <form id="tax-basic-info-form" method="post" action="${pageContext.request.contextPath}/settings/tax-basic-info" novalidate>
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                    <div class="settings-grid-2">
                        <div class="settings-field">
                            <label id="label-business-name" for="input-business-name">屋号</label>
                            <input id="input-business-name" name="businessName" type="text" maxlength="200" autocomplete="organization" value="<c:out value='${taxBasicInfoForm.businessName}' />" />
                        </div>
                        <div class="settings-field">
                            <label id="label-industry" for="input-industry">業種</label>
                            <input id="input-industry" name="industry" type="text" maxlength="100" autocomplete="organization-title" value="<c:out value='${taxBasicInfoForm.industry}' />" />
                        </div>
                        <div class="settings-field">
                            <label id="label-phone" for="input-phone">電話番号</label>
                            <input id="input-phone" name="phone" type="text" maxlength="30" autocomplete="tel" inputmode="tel" value="<c:out value='${taxBasicInfoForm.phone}' />" />
                            <small class="settings-hint">数字と + - ( ) を使用できます。</small>
                        </div>
                        <div class="settings-field">
                            <label id="label-address" for="input-address">住所</label>
                            <input id="input-address" name="address" type="text" maxlength="255" autocomplete="street-address" value="<c:out value='${taxBasicInfoForm.address}' />" />
                        </div>
                        <div class="settings-field">
                            <label id="label-name-kana" for="input-name-kana">フリガナ</label>
                            <input id="input-name-kana" name="nameKana" type="text" maxlength="100" autocomplete="off" placeholder="例）ヤマダ　タロウ" value="<c:out value='${taxBasicInfoForm.nameKana}' />" />
                            <small class="settings-hint">全角カタカナで入力してください（姓と名の間はスペース）。</small>
                        </div>
                        <div class="settings-field">
                            <label id="label-birth-date" for="input-birth-date">生年月日</label>
                            <input id="input-birth-date" name="birthDate" type="date" min="1900-01-02" autocomplete="off" value="<c:out value='${taxBasicInfoForm.birthDate}' />" />
                        </div>
                        <div class="settings-field">
                            <label id="label-tax-office" for="input-tax-office">提出先税務署</label>
                            <input id="input-tax-office" name="taxOffice" type="text" maxlength="100" autocomplete="off" placeholder="例）渋谷税務署" value="<c:out value='${taxBasicInfoForm.taxOffice}' />" />
                        </div>
                        <div class="settings-field">
                            <label id="label-household-head-name" for="input-household-head-name">世帯主の氏名</label>
                            <input id="input-household-head-name" name="householdHeadName" type="text" maxlength="100" autocomplete="off" value="<c:out value='${taxBasicInfoForm.householdHeadName}' />" />
                        </div>
                        <div class="settings-field">
                            <label id="label-household-head-relation" for="input-household-head-relation">世帯主との続柄</label>
                            <input id="input-household-head-relation" name="householdHeadRelation" type="text" maxlength="30" autocomplete="off" placeholder="例）本人" value="<c:out value='${taxBasicInfoForm.householdHeadRelation}' />" />
                        </div>
                    </div>

                    <fieldset class="settings-subgroup" aria-describedby="text-consumption-tax-note">
                        <legend id="heading-consumption-tax">消費税</legend>
                        <div class="settings-grid-2">
                            <div class="settings-field">
                                <label id="label-consumption-tax-status" for="select-consumption-tax-status">消費税の区分</label>
                                <select id="select-consumption-tax-status" name="consumptionTaxStatus">
                                    <option value="EXEMPT" <c:if test="${taxBasicInfoForm.consumptionTaxStatus ne 'SIMPLIFIED'}">selected</c:if>>免税事業者</option>
                                    <option value="SIMPLIFIED" <c:if test="${taxBasicInfoForm.consumptionTaxStatus eq 'SIMPLIFIED'}">selected</c:if>>課税事業者（簡易課税）</option>
                                </select>
                            </div>
                            <div class="settings-field">
                                <label id="label-simplified-business-type" for="select-simplified-business-type">簡易課税の事業区分</label>
                                <select id="select-simplified-business-type" name="simplifiedBusinessType">
                                    <option value="">選択してください</option>
                                    <option value="1" <c:if test="${taxBasicInfoForm.simplifiedBusinessType eq '1'}">selected</c:if>>第1種 卸売業（みなし仕入率90%）</option>
                                    <option value="2" <c:if test="${taxBasicInfoForm.simplifiedBusinessType eq '2'}">selected</c:if>>第2種 小売業等（みなし仕入率80%）</option>
                                    <option value="3" <c:if test="${taxBasicInfoForm.simplifiedBusinessType eq '3'}">selected</c:if>>第3種 製造業等（みなし仕入率70%）</option>
                                    <option value="4" <c:if test="${taxBasicInfoForm.simplifiedBusinessType eq '4'}">selected</c:if>>第4種 その他の事業（みなし仕入率60%）</option>
                                    <option value="5" <c:if test="${taxBasicInfoForm.simplifiedBusinessType eq '5'}">selected</c:if>>第5種 サービス業等（みなし仕入率50%）</option>
                                    <option value="6" <c:if test="${taxBasicInfoForm.simplifiedBusinessType eq '6'}">selected</c:if>>第6種 不動産業（みなし仕入率40%）</option>
                                </select>
                                <small class="settings-hint">課税事業者（簡易課税）の場合に選択してください。</small>
                            </div>
                        </div>
                        <small id="text-consumption-tax-note" class="settings-hint">※ 本則課税・2割特例には対応していません。簡易課税を選ぶ場合は、所轄税務署へ「消費税簡易課税制度選択届出書」を提出済みである必要があります。</small>
                    </fieldset>
                    <div class="settings-actions"><button id="button-save-tax-basic-info" type="submit" class="btn-primary">基本情報を保存</button></div>
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
