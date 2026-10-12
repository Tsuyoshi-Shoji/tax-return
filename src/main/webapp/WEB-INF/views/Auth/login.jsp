<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ログイン</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css" />
</head>
<body class="auth-page auth-page-login">
<main class="auth-main auth-main-login">
    <section class="auth-hero" aria-labelledby="auth-hero-title">
        <div class="auth-brand auth-brand-hero">
            <img class="auth-brand-mark" src="${pageContext.request.contextPath}/css/auth/auth-brand-bars.svg" alt="" width="56" height="56" />
            <div class="auth-brand-copy">
                <p class="auth-brand-name">マネタックス</p>
                <p class="auth-brand-subtitle">確定申告・資産管理</p>
            </div>
        </div>

        <div class="auth-hero-copy">
            <h1 id="auth-hero-title" class="auth-hero-title">確定申告を、<br>もっとシンプルに。</h1>
            <p class="auth-hero-description">
                マネタックスは、確定申告と資産管理を<br>
                まとめてサポートするクラウドサービスです。<br>
                あなたの大切なお金の管理を、もっとかんたんに、もっと安心に。
            </p>
        </div>

        <div class="auth-hero-visual">
            <img src="${pageContext.request.contextPath}/css/auth/auth-hero-illustration.svg" alt="" width="808" height="604" />
        </div>
    </section>

    <section class="auth-card auth-card-login" aria-labelledby="page-title">
        <div class="auth-card-header auth-card-header-login">
            <div class="auth-brand auth-brand-card">
                <img class="auth-brand-mark" src="${pageContext.request.contextPath}/css/auth/auth-brand-bars.svg" alt="" width="52" height="52" />
                <div class="auth-brand-copy">
                    <p class="auth-brand-name">マネタックス</p>
                    <p class="auth-brand-subtitle">確定申告・資産管理</p>
                </div>
            </div>
            <h2 id="page-title">ログイン</h2>
            <p id="page-description" class="auth-description">マネタックスにログインして、<br>確定申告や資産の管理をはじめましょう。</p>
        </div>

        <form id="loginForm" class="auth-form" method="post" action="${pageContext.request.contextPath}/login" novalidate>
            <div class="form-group">
                <label id="label-address" for="form-address">メールアドレス <span class="required">*</span></label>
                <div class="field-shell field-shell-with-icon">
                    <span class="field-icon field-icon-envelope" aria-hidden="true"></span>
                    <input id="form-address" name="email" type="email" maxlength="255"
                           value="<c:out value='${loginForm.email}' />"
                           placeholder="example@manetax.jp" autocomplete="email" inputmode="email"
                           aria-describedby="emailError" required />
                </div>
                <p id="emailError" class="field-error" aria-live="polite" hidden></p>
            </div>

            <div class="form-group">
                <label id="label-password" for="form-password">パスワード <span class="required">*</span></label>
                <div class="field-shell field-shell-with-icon field-shell-password">
                    <span class="field-icon field-icon-lock" aria-hidden="true"></span>
                    <input id="form-password" name="password" type="password" minlength="8" maxlength="72"
                           placeholder="パスワードを入力してください" autocomplete="current-password"
                           aria-describedby="passwordError" required />
                    <button id="button-toggle-password" class="password-toggle" type="button"
                            aria-controls="form-password" aria-label="パスワードを表示" aria-pressed="false">
                        <span class="visually-hidden">パスワードの表示を切り替える</span>
                    </button>
                </div>
                <p id="passwordError" class="field-error" aria-live="polite" hidden></p>
            </div>

            <div class="auth-options-row">
                <label class="check-control" for="checkbox-remember-me">
                    <input id="checkbox-remember-me" type="checkbox" name="rememberMe"
                           <c:if test="${loginForm.rememberMe}">checked</c:if> />
                    <span class="check-indicator" aria-hidden="true"></span>
                    <span class="check-text">ログイン状態を保持する</span>
                </label>
                <a id="link-forgot-password" class="auth-inline-link" href="#"
                   data-unavailable-message="パスワード再設定は現在準備中です。">パスワードをお忘れですか？</a>
            </div>

            <div class="button-group">
                <button id="button-login" type="submit" class="auth-button auth-button-primary">ログイン</button>
            </div>
        </form>

        <div class="auth-divider" aria-hidden="true">
            <span id="text-or">または</span>
        </div>
        <p id="text-register-guide" class="auth-support-copy">はじめてご利用の方へ</p>
        <p class="auth-link-row">
            <a id="link-getting-started" class="auth-button auth-button-secondary" href="${pageContext.request.contextPath}/register">
                <span>利用開始について</span>
                <span class="button-chevron" aria-hidden="true"></span>
            </a>
        </p>
    </section>

    <footer class="auth-site-footer" aria-label="関連情報">
        <nav class="auth-footer-links">
            <a id="link-terms" href="#" data-unavailable-message="利用規約は現在準備中です。">利用規約</a>
            <a id="link-privacy" href="#" data-unavailable-message="プライバシーポリシーは現在準備中です。">プライバシーポリシー</a>
            <a id="link-contact" href="#" data-unavailable-message="お問い合わせ窓口は現在準備中です。">お問い合わせ</a>
        </nav>
        <p class="auth-footer-copy">© 2024 マネタックス　確定申告・資産管理</p>
    </footer>

    <div id="auth-inline-notice" class="auth-inline-notice" role="status" aria-live="polite" hidden></div>
</main>

<script>
    (function () {
        const form = document.getElementById('loginForm');
        const emailInput = document.getElementById('form-address');
        const passwordInput = document.getElementById('form-password');
        const togglePassword = document.getElementById('button-toggle-password');
        const notice = document.getElementById('auth-inline-notice');
        const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

        function setError(input, id, message) {
            const element = document.getElementById(id);
            const hasMessage = Boolean(message);
            element.textContent = message || '';
            element.hidden = !hasMessage;
            element.classList.toggle('is-visible', hasMessage);
            input.setAttribute('aria-invalid', hasMessage ? 'true' : 'false');
        }

        function togglePasswordVisibility(button, input) {
            const masked = input.type === 'password';
            input.type = masked ? 'text' : 'password';
            button.setAttribute('aria-pressed', masked ? 'true' : 'false');
            button.setAttribute('aria-label', masked ? 'パスワードを隠す' : 'パスワードを表示');
            button.classList.toggle('is-revealed', masked);
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

        togglePassword.addEventListener('click', function () {
            togglePasswordVisibility(togglePassword, passwordInput);
        });

        document.querySelectorAll('[data-unavailable-message]').forEach(function (link) {
            link.addEventListener('click', function (event) {
                event.preventDefault();
                showNotice(link.getAttribute('data-unavailable-message'));
            });
        });

        form.addEventListener('submit', function (event) {
            let valid = true;
            const email = (emailInput.value || '').trim();
            const password = passwordInput.value || '';

            setError(emailInput, 'emailError', '');
            setError(passwordInput, 'passwordError', '');

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
