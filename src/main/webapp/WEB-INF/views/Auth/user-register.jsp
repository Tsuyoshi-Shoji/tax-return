<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ユーザー登録</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css" />
</head>
<body class="auth-page auth-page-register">
<main class="auth-main auth-main-register">
    <section class="auth-card auth-card-register" aria-labelledby="page-title">
        <div class="auth-card-header auth-card-header-register">
            <p id="logo" class="auth-register-wordmark">マネタックス</p>
            <p id="subtitle" class="auth-register-subtitle">確定申告・資産管理</p>
            <div class="auth-register-rule" aria-hidden="true"></div>
            <h1 id="page-title">ユーザー登録</h1>
            <p id="page-description" class="auth-description">アカウントを作成して利用を開始します。</p>
        </div>

        <form id="registerForm" class="auth-form" method="post" action="${pageContext.request.contextPath}/register" novalidate>
            <div class="form-group">
                <label id="label-name" for="form-name">氏名 <span class="required">*</span></label>
                <div class="field-shell">
                    <input id="form-name" type="text" name="name" maxlength="100"
                           value="<c:out value='${registerForm.name}' />" placeholder="例）山田太郎"
                           autocomplete="name" aria-describedby="nameError" required />
                </div>
                <p id="nameError" class="field-error" aria-live="polite" hidden></p>
            </div>

            <div class="form-group">
                <label id="label-address" for="form-address">メールアドレス <span class="required">*</span></label>
                <div class="field-shell">
                    <input id="form-address" type="email" name="email" maxlength="255"
                           value="<c:out value='${registerForm.email}' />"
                           placeholder="例）taro.yamada@example.com" autocomplete="email" inputmode="email"
                           aria-describedby="emailError" required />
                </div>
                <p id="emailError" class="field-error" aria-live="polite" hidden></p>
            </div>

            <div class="form-group">
                <label id="label-password" for="form-password">パスワード <span class="required">*</span></label>
                <div class="field-shell field-shell-password">
                    <input id="form-password" type="password" name="password" minlength="8" maxlength="72"
                           placeholder="半角英数字8文字以上で入力してください" autocomplete="new-password"
                           aria-describedby="passwordError" required />
                    <button id="button-toggle-password" class="password-toggle" type="button"
                            aria-controls="form-password" aria-label="パスワードを表示" aria-pressed="false">
                        <span class="visually-hidden">パスワードの表示を切り替える</span>
                    </button>
                </div>
                <p id="passwordError" class="field-error" aria-live="polite" hidden></p>
            </div>

            <div class="form-group">
                <label id="label-confirm-password" for="form-confirm-password">パスワード（確認） <span class="required">*</span></label>
                <div class="field-shell field-shell-password">
                    <input id="form-confirm-password" type="password" name="confirmPassword" minlength="8" maxlength="72"
                           placeholder="同じパスワードを入力してください" autocomplete="new-password"
                           aria-describedby="confirmPasswordError" required />
                    <button id="button-toggle-confirm-password" class="password-toggle" type="button"
                            aria-controls="form-confirm-password" aria-label="確認用パスワードを表示" aria-pressed="false">
                        <span class="visually-hidden">確認用パスワードの表示を切り替える</span>
                    </button>
                </div>
                <p id="confirmPasswordError" class="field-error" aria-live="polite" hidden></p>
            </div>

            <div class="form-group form-group-terms">
                <div class="terms-row">
                    <label class="check-control check-control-standalone" for="checkbox-terms-agreement">
                        <input id="checkbox-terms-agreement" type="checkbox" name="termsAgreement"
                               aria-label="利用規約・プライバシーポリシーに同意する"
                               aria-describedby="termsError"
                               <c:if test="${registerForm.termsAgreement}">checked</c:if> />
                        <span class="check-indicator" aria-hidden="true"></span>
                    </label>
                    <p class="check-text">
                        <a class="auth-inline-link auth-inline-link-strong" href="#"
                           data-unavailable-message="利用規約は現在準備中です。">利用規約</a>
                        ・
                        <a class="auth-inline-link auth-inline-link-strong" href="#"
                           data-unavailable-message="プライバシーポリシーは現在準備中です。">プライバシーポリシー</a>
                        に同意する
                    </p>
                </div>
                <p id="termsError" class="field-error" aria-live="polite" hidden></p>
            </div>

            <div class="button-group">
                <button id="button-register" type="submit" class="auth-button auth-button-primary">アカウントを作成</button>
            </div>
        </form>

        <div class="auth-divider auth-divider-register" aria-hidden="true"></div>
        <p id="text-login-guide" class="auth-register-footer-copy">すでにアカウントをお持ちの方は <a id="link-to-login" href="${pageContext.request.contextPath}/login">ログイン</a></p>
    </section>

    <div id="auth-inline-notice" class="auth-inline-notice" role="status" aria-live="polite" hidden></div>
</main>

<script>
    (function () {
        const form = document.getElementById('registerForm');
        const nameInput = document.getElementById('form-name');
        const emailInput = document.getElementById('form-address');
        const passwordInput = document.getElementById('form-password');
        const confirmInput = document.getElementById('form-confirm-password');
        const termsInput = document.getElementById('checkbox-terms-agreement');
        const notice = document.getElementById('auth-inline-notice');
        const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

        function setError(input, id, message) {
            const element = document.getElementById(id);
            const hasMessage = Boolean(message);
            element.textContent = message || '';
            element.hidden = !hasMessage;
            element.classList.toggle('is-visible', hasMessage);
            if (input) {
                input.setAttribute('aria-invalid', hasMessage ? 'true' : 'false');
            }
        }

        function bindToggle(buttonId, inputId, showLabel, hideLabel) {
            const button = document.getElementById(buttonId);
            const input = document.getElementById(inputId);
            button.addEventListener('click', function () {
                const masked = input.type === 'password';
                input.type = masked ? 'text' : 'password';
                button.setAttribute('aria-pressed', masked ? 'true' : 'false');
                button.setAttribute('aria-label', masked ? hideLabel : showLabel);
                button.classList.toggle('is-revealed', masked);
            });
        }

        function showNotice(message) {
            notice.textContent = message;
            notice.hidden = false;
            notice.classList.add('is-visible');
            window.clearTimeout(showNotice.timerId);
            showNotice.timerId = window.setTimeout(function () {
                notice.classList.remove('is-visible');
                notice.hidden = true;
            }, 3200);
        }

        bindToggle('button-toggle-password', 'form-password', 'パスワードを表示', 'パスワードを隠す');
        bindToggle('button-toggle-confirm-password', 'form-confirm-password', '確認用パスワードを表示', '確認用パスワードを隠す');

        document.querySelectorAll('[data-unavailable-message]').forEach(function (link) {
            link.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                showNotice(link.getAttribute('data-unavailable-message'));
            });
        });

        form.addEventListener('submit', function (event) {
            let valid = true;
            const name = (nameInput.value || '').trim();
            const email = (emailInput.value || '').trim();
            const password = passwordInput.value || '';
            const confirmPassword = confirmInput.value || '';

            setError(nameInput, 'nameError', '');
            setError(emailInput, 'emailError', '');
            setError(passwordInput, 'passwordError', '');
            setError(confirmInput, 'confirmPasswordError', '');
            setError(termsInput, 'termsError', '');

            if (!name) {
                setError(nameInput, 'nameError', '氏名を入力してください。');
                valid = false;
            } else if (name.length > 100) {
                setError(nameInput, 'nameError', '氏名は100文字以内で入力してください。');
                valid = false;
            }

            if (!email) {
                setError(emailInput, 'emailError', 'メールアドレスを入力してください。');
                valid = false;
            } else if (!emailPattern.test(email)) {
                setError(emailInput, 'emailError', 'メールアドレスは正しい形式で入力してください。');
                valid = false;
            }

            if (!password) {
                setError(passwordInput, 'passwordError', 'パスワードを入力してください。');
                valid = false;
            } else if (password.length < 8 || password.length > 72) {
                setError(passwordInput, 'passwordError', 'パスワードは8〜72文字で入力してください。');
                valid = false;
            }

            if (!confirmPassword) {
                setError(confirmInput, 'confirmPasswordError', '確認用パスワードを入力してください。');
                valid = false;
            } else if (confirmPassword !== password) {
                setError(confirmInput, 'confirmPasswordError', 'パスワードが異なります。確認してください。');
                valid = false;
            }

            if (!termsInput.checked) {
                setError(termsInput, 'termsError', '利用規約・プライバシーポリシーへの同意が必要です。');
                valid = false;
            }

            if (!valid) {
                event.preventDefault();
                const firstInvalid = form.querySelector('[aria-invalid="true"]');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
            }
        });
    })();
</script>
<jsp:include page="/WEB-INF/views/Common/app-dialog.jsp">
    <jsp:param name="useValidationErrors" value="true" />
    <jsp:param name="primaryLabel" value="閉じる" />
</jsp:include>
</body>
</html>
