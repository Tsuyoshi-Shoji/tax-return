<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>設定</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/settings/settings.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <main>
        <div class="container settings-container">
            <div class="page-header">
                <h1>設定</h1>
                <p class="page-subtitle">ユーザー情報と支出登録時の家事按分設定を管理します。</p>
            </div>

            <c:if test="${not empty successMessage}">
                <div class="success-message" role="status">
                    <c:out value="${successMessage}" />
                </div>
            </c:if>

            <c:if test="${not empty failureMessage}">
                <div class="error-messages" role="alert">
                    <p class="error-title"><c:out value="${failureMessage}" /></p>
                    <c:if test="${not empty validationErrors}">
                        <ul>
                            <c:forEach var="error" items="${validationErrors}">
                                <li><c:out value="${error}" /></li>
                            </c:forEach>
                        </ul>
                    </c:if>
                </div>
            </c:if>

            <section class="settings-section" aria-labelledby="profileSettingsTitle">
                <div class="section-header">
                    <div>
                        <h2 id="profileSettingsTitle">プロフィール</h2>
                        <p>ユーザー名と登録メールアドレスを編集します。</p>
                    </div>
                    <span class="status-badge"><c:out value="${setting.accountStatus}" /></span>
                </div>

                <form class="settings-form" method="post" action="${pageContext.request.contextPath}/settings/profile" novalidate data-settings-form="profile">
                    <c:if test="${not empty _csrf}">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    </c:if>
                    <div class="form-group">
                        <label for="username">ユーザー名 <span class="required">*</span></label>
                        <input type="text" id="username" name="username" maxlength="100" value="<c:out value='${setting.username}' />" autocomplete="name" required />
                        <p id="usernameError" class="field-error" aria-live="polite"></p>
                    </div>
                    <div class="form-group">
                        <label for="email">登録メールアドレス <span class="required">*</span></label>
                        <input type="email" id="email" name="email" maxlength="255" value="<c:out value='${setting.email}' />" autocomplete="email" required />
                        <p id="emailError" class="field-error" aria-live="polite"></p>
                    </div>
                    <div class="button-group">
                        <button type="submit" class="btn-primary">プロフィールを保存</button>
                    </div>
                </form>
            </section>

            <section class="settings-section" aria-labelledby="passwordSettingsTitle">
                <div class="section-header">
                    <div>
                        <h2 id="passwordSettingsTitle">パスワード変更</h2>
                        <p>パスワードは画面に表示せず、サーバー側で安全に処理します。</p>
                    </div>
                </div>

                <form class="settings-form" method="post" action="${pageContext.request.contextPath}/settings/password" novalidate data-settings-form="password">
                    <c:if test="${not empty _csrf}">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    </c:if>
                    <div class="form-group">
                        <label for="currentPassword">現在のパスワード <span class="required">*</span></label>
                        <input type="password" id="currentPassword" name="currentPassword" autocomplete="current-password" required />
                        <p id="currentPasswordError" class="field-error" aria-live="polite"></p>
                    </div>
                    <div class="form-group">
                        <label for="newPassword">新しいパスワード <span class="required">*</span></label>
                        <input type="password" id="newPassword" name="newPassword" minlength="8" maxlength="72" autocomplete="new-password" required />
                        <p id="newPasswordError" class="field-error" aria-live="polite"></p>
                    </div>
                    <div class="form-group">
                        <label for="confirmPassword">新しいパスワード（確認） <span class="required">*</span></label>
                        <input type="password" id="confirmPassword" name="confirmPassword" minlength="8" maxlength="72" autocomplete="new-password" required />
                        <p id="confirmPasswordError" class="field-error" aria-live="polite"></p>
                    </div>
                    <div class="button-group">
                        <button type="submit" class="btn-primary">パスワードを変更</button>
                    </div>
                </form>
            </section>

            <section class="settings-section" aria-labelledby="businessUseRatioTitle">
                <div class="section-header">
                    <div>
                        <h2 id="businessUseRatioTitle">家事按分の設定</h2>
                        <p>支出登録画面で家事按分チェックを選択した場合に使用する固定割合です。</p>
                    </div>
                </div>

                <form class="settings-form ratio-form" method="post" action="${pageContext.request.contextPath}/settings/business-use-ratio" novalidate data-settings-form="ratio">
                    <c:if test="${not empty _csrf}">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    </c:if>
                    <div class="form-group ratio-input-group">
                        <label for="defaultBusinessUseRatio">家事按分率 <span class="required">*</span></label>
                        <div class="ratio-input-wrapper">
                            <input type="number" id="defaultBusinessUseRatio" name="defaultBusinessUseRatio" min="0" max="100" step="0.01" inputmode="decimal" value="<c:out value='${setting.defaultBusinessUseRatio}' />" required />
                            <span>%</span>
                        </div>
                        <p id="defaultBusinessUseRatioError" class="field-error" aria-live="polite"></p>
                    </div>
                    <div class="ratio-preview" aria-live="polite">
                        <p class="preview-title">支出登録画面への反映例</p>
                        <dl>
                            <div>
                                <dt>支出金額</dt>
                                <dd>10,000円</dd>
                            </div>
                            <div>
                                <dt>経費対象金額</dt>
                                <dd id="ratioPreviewAmount">10,000円</dd>
                            </div>
                        </dl>
                        <p class="preview-note">保存後、支出登録画面の家事按分計算に即時反映されます。</p>
                    </div>
                    <div class="button-group">
                        <button type="submit" class="btn-primary">家事按分率を保存</button>
                    </div>
                </form>
            </section>

            <section class="settings-section danger-section" aria-labelledby="accountActionTitle">
                <div class="section-header">
                    <div>
                        <h2 id="accountActionTitle">アカウント操作</h2>
                        <p>ユーザーの無効化・削除を行います。削除操作は復元できない前提で扱ってください。</p>
                    </div>
                </div>

                <div class="danger-actions">
                    <form class="danger-form" method="post" action="${pageContext.request.contextPath}/settings/disable" data-settings-form="disable">
                        <c:if test="${not empty _csrf}">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        </c:if>
                        <h3>ユーザーの無効化</h3>
                        <p>無効化されたユーザーは、認証実装後にログインできない状態として扱います。データは削除しません。</p>
                        <label for="disableConfirmation">確認のため「無効化」と入力してください</label>
                        <input type="text" id="disableConfirmation" name="confirmationText" autocomplete="off" />
                        <p id="disableConfirmationError" class="field-error" aria-live="polite"></p>
                        <button type="submit" class="btn-warning">ユーザーを無効化</button>
                    </form>

                    <form class="danger-form delete-form" method="post" action="${pageContext.request.contextPath}/settings/delete" data-settings-form="delete">
                        <c:if test="${not empty _csrf}">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        </c:if>
                        <h3>ユーザーの削除</h3>
                        <p>削除されたユーザーのデータは復元できません。実運用では会計・税務データへの影響を確認してから実行します。</p>
                        <label for="deleteConfirmation">確認のため「削除」と入力してください</label>
                        <input type="text" id="deleteConfirmation" name="confirmationText" autocomplete="off" />
                        <p id="deleteConfirmationError" class="field-error" aria-live="polite"></p>
                        <button type="submit" class="btn-danger">ユーザーを削除</button>
                    </form>
                </div>
            </section>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />

    <script>
        (function () {
            'use strict';

            const ratioInput = document.getElementById('defaultBusinessUseRatio');
            const ratioPreviewAmount = document.getElementById('ratioPreviewAmount');

            function setError(id, message) {
                const element = document.getElementById(id + 'Error');
                if (!element) {
                    return;
                }
                element.textContent = message || '';
                element.classList.toggle('is-visible', Boolean(message));
            }

            function updateRatioPreview() {
                const ratio = Number(ratioInput.value);
                const amount = Number.isFinite(ratio) ? Math.floor(10000 * ratio / 100) : 0;
                ratioPreviewAmount.textContent = amount.toLocaleString('ja-JP') + '円';
            }

            function validateProfile(form) {
                const username = form.username.value.trim();
                const email = form.email.value.trim();
                let valid = true;
                setError('username', '');
                setError('email', '');
                if (username.length < 1 || username.length > 100) {
                    setError('username', 'ユーザー名は1〜100文字で入力してください。');
                    valid = false;
                }
                if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
                    setError('email', 'メールアドレスは正しい形式で入力してください。');
                    valid = false;
                }
                return valid;
            }

            function validatePassword(form) {
                const currentPassword = form.currentPassword.value;
                const newPassword = form.newPassword.value;
                const confirmPassword = form.confirmPassword.value;
                let valid = true;
                setError('currentPassword', '');
                setError('newPassword', '');
                setError('confirmPassword', '');
                if (!currentPassword) {
                    setError('currentPassword', '現在のパスワードを入力してください。');
                    valid = false;
                }
                if (newPassword.length < 8 || newPassword.length > 72) {
                    setError('newPassword', '新しいパスワードは8〜72文字で入力してください。');
                    valid = false;
                }
                if (newPassword !== confirmPassword) {
                    setError('confirmPassword', '新しいパスワードと確認用パスワードが一致しません。');
                    valid = false;
                }
                return valid;
            }

            function validateRatio() {
                const ratio = Number(ratioInput.value);
                setError('defaultBusinessUseRatio', '');
                if (!Number.isFinite(ratio) || ratio < 0 || ratio > 100) {
                    setError('defaultBusinessUseRatio', '家事按分率は0〜100%の範囲で入力してください。');
                    return false;
                }
                return true;
            }

            function validateConfirmation(form, expectedText, fieldId) {
                const value = form.confirmationText.value.trim();
                setError(fieldId, '');
                if (value !== expectedText) {
                    setError(fieldId, '確認欄に「' + expectedText + '」と入力してください。');
                    return false;
                }
                return window.confirm('この操作を実行してもよろしいですか？');
            }

            document.querySelectorAll('[data-settings-form]').forEach(form => {
                form.addEventListener('submit', event => {
                    const formType = form.getAttribute('data-settings-form');
                    let valid = true;
                    if (formType === 'profile') {
                        valid = validateProfile(form);
                    } else if (formType === 'password') {
                        valid = validatePassword(form);
                    } else if (formType === 'ratio') {
                        valid = validateRatio();
                    } else if (formType === 'disable') {
                        valid = validateConfirmation(form, '無効化', 'disableConfirmation');
                    } else if (formType === 'delete') {
                        valid = validateConfirmation(form, '削除', 'deleteConfirmation');
                    }

                    if (!valid) {
                        event.preventDefault();
                    }
                });
            });

            ratioInput.addEventListener('input', () => {
                setError('defaultBusinessUseRatio', '');
                updateRatioPreview();
            });

            updateRatioPreview();
        })();
    </script>
</body>
</html>
