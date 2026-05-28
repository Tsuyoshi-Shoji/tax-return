<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ユーザー新規登録</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css" />
</head>
<body>
<main class="auth-main">
    <div class="auth-card">
        <h1>ユーザー新規登録</h1>
        <p class="auth-description">メールアドレスとパスワードを入力してアカウントを作成します。</p>

        <c:if test="${not empty failureMessage}">
            <div class="auth-message auth-message-error" role="alert">
                <p><c:out value="${failureMessage}" /></p>
                <c:if test="${not empty validationErrors}">
                    <ul>
                        <c:forEach var="error" items="${validationErrors}">
                            <li><c:out value="${error}" /></li>
                        </c:forEach>
                    </ul>
                </c:if>
            </div>
        </c:if>

        <form id="registerForm" method="post" action="${pageContext.request.contextPath}/register" novalidate>
            <c:if test="${not empty _csrf}">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            </c:if>

            <div class="form-group">
                <label for="email">メールアドレス <span class="required">*</span></label>
                <input
                        type="email"
                        id="email"
                        name="email"
                        maxlength="255"
                        value="<c:out value='${registerForm.email}' />"
                        autocomplete="email"
                        required />
                <p id="emailError" class="field-error" aria-live="polite"></p>
            </div>

            <div class="form-group">
                <label for="password">パスワード <span class="required">*</span></label>
                <input
                        type="password"
                        id="password"
                        name="password"
                        minlength="8"
                        maxlength="72"
                        autocomplete="new-password"
                        required />
                <p id="passwordError" class="field-error" aria-live="polite"></p>
            </div>

            <div class="form-group">
                <label for="confirmPassword">パスワード（確認） <span class="required">*</span></label>
                <input
                        type="password"
                        id="confirmPassword"
                        name="confirmPassword"
                        minlength="8"
                        maxlength="72"
                        autocomplete="new-password"
                        required />
                <p id="confirmPasswordError" class="field-error" aria-live="polite"></p>
            </div>

            <div class="button-group">
                <button type="submit" class="btn-primary">新規登録</button>
                <a class="btn-secondary" href="${pageContext.request.contextPath}/login">ログインへ</a>
            </div>
        </form>
    </div>
</main>

<script>
    (function () {
        'use strict';

        const form = document.getElementById('registerForm');
        const emailInput = document.getElementById('email');
        const passwordInput = document.getElementById('password');
        const confirmInput = document.getElementById('confirmPassword');
        const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

        function setError(id, message) {
            const element = document.getElementById(id + 'Error');
            if (!element) {
                return;
            }
            element.textContent = message || '';
            element.classList.toggle('is-visible', Boolean(message));
        }

        function validateForm() {
            let valid = true;
            const email = (emailInput.value || '').trim();
            const password = passwordInput.value || '';
            const confirm = confirmInput.value || '';

            setError('email', '');
            setError('password', '');
            setError('confirmPassword', '');

            if (!email) {
                setError('email', 'メールアドレスを入力してください。');
                valid = false;
            } else if (!emailPattern.test(email)) {
                setError('email', 'メールアドレスは正しい形式で入力してください。');
                valid = false;
            }

            if (!password) {
                setError('password', 'パスワードを入力してください。');
                valid = false;
            } else if (password.length < 8 || password.length > 72) {
                setError('password', 'パスワードは8〜72文字で入力してください。');
                valid = false;
            }

            if (!confirm) {
                setError('confirmPassword', '確認用パスワードを入力してください。');
                valid = false;
            } else if (password !== confirm) {
                setError('confirmPassword', 'パスワードが異なります。確認してください。');
                valid = false;
            }

            return valid;
        }

        emailInput.addEventListener('input', () => setError('email', ''));
        passwordInput.addEventListener('input', () => setError('password', ''));
        confirmInput.addEventListener('input', () => setError('confirmPassword', ''));

        form.addEventListener('submit', event => {
            if (!validateForm()) {
                event.preventDefault();
            }
        });
    })();
</script>
</body>
</html>

