<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>認証コード入力</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css" />
</head>
<body class="auth-page auth-page-mfa">
<main class="auth-main auth-main-mfa">
    <section class="auth-card auth-card-mfa" aria-labelledby="page-title">
        <div class="auth-card-header auth-card-header-mfa">
            <div class="auth-brand auth-brand-card">
                <img class="auth-brand-mark" src="${pageContext.request.contextPath}/css/auth/auth-brand-bars.svg" alt="" width="52" height="52" />
                <div class="auth-brand-copy">
                    <p class="auth-brand-name">マネタックス</p>
                    <p class="auth-brand-subtitle">確定申告・資産管理</p>
                </div>
            </div>
            <h2 id="page-title">認証コード入力</h2>
            <p id="page-description" class="auth-description">
                <c:out value="${email}" /> に登録されている<br>
                認証アプリの認証コードを入力してください。
            </p>
        </div>

        <form id="mfaForm" class="auth-form" method="post" action="${pageContext.request.contextPath}/mfa-verify" novalidate>
            <!-- タブコントロール -->
            <div class="auth-mfa-tabs" role="tablist">
                <button id="tab-totp" class="auth-mfa-tab" role="tab" aria-selected="true" aria-controls="panel-totp">
                    認証アプリ
                </button>
                <button id="tab-backup" class="auth-mfa-tab" role="tab" aria-selected="false" aria-controls="panel-backup">
                    バックアップコード
                </button>
            </div>

            <!-- TOTP パネル -->
            <div id="panel-totp" class="auth-mfa-panel" role="tabpanel" aria-labelledby="tab-totp" hidden>
                <div class="form-group">
                    <label id="label-code" for="form-code">認証コード <span class="required">*</span></label>
                    <p class="field-hint">Google Authenticator などの認証アプリに表示されている 6 桁のコードを入力してください。</p>
                    <div class="field-shell field-shell-with-icon">
                        <span class="field-icon field-icon-lock" aria-hidden="true"></span>
                        <input id="form-code" name="code" type="text" maxlength="6"
                               placeholder="000000" autocomplete="one-time-code"
                               inputmode="numeric" pattern="[0-9]{6}"
                               aria-describedby="codeError" />
                    </div>
                    <p id="codeError" class="field-error" aria-live="polite" hidden></p>
                </div>
            </div>

            <!-- バックアップコード パネル -->
            <div id="panel-backup" class="auth-mfa-panel" role="tabpanel" aria-labelledby="tab-backup" hidden>
                <div class="form-group">
                    <label id="label-backup" for="form-backup-code">バックアップコード <span class="required">*</span></label>
                    <p class="field-hint">認証アプリにアクセスできない場合、バックアップコードを使用できます。</p>
                    <div class="field-shell field-shell-with-icon">
                        <span class="field-icon field-icon-shield" aria-hidden="true"></span>
                        <input id="form-backup-code" name="backupCode" type="text" maxlength="8"
                               placeholder="XXXXXXXX" autocomplete="off"
                               aria-describedby="backupError" />
                    </div>
                    <p id="backupError" class="field-error" aria-live="polite" hidden></p>
                </div>
                <input type="hidden" name="useBackupCode" id="hidden-use-backup" value="false" />
            </div>

            <div class="button-group">
                <button id="button-verify" type="submit" class="auth-button auth-button-primary">検証</button>
            </div>
        </form>

        <p class="auth-help-text">
            <a id="link-back" class="auth-inline-link" href="${pageContext.request.contextPath}/login">ログイン画面に戻る</a>
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
        const form = document.getElementById('mfaForm');
        const tabTotp = document.getElementById('tab-totp');
        const tabBackup = document.getElementById('tab-backup');
        const panelTotp = document.getElementById('panel-totp');
        const panelBackup = document.getElementById('panel-backup');
        const codeInput = document.getElementById('form-code');
        const backupInput = document.getElementById('form-backup-code');
        const hiddenUseBackup = document.getElementById('hidden-use-backup');
        const notice = document.getElementById('auth-inline-notice');

        // タブ切り替え
        function switchTab(activeTab, inactiveTab, activePanel, inactivePanel) {
            activeTab.setAttribute('aria-selected', 'true');
            inactiveTab.setAttribute('aria-selected', 'false');
            activePanel.hidden = false;
            inactivePanel.hidden = true;
        }

        tabTotp.addEventListener('click', function () {
            switchTab(tabTotp, tabBackup, panelTotp, panelBackup);
            hiddenUseBackup.value = 'false';
            codeInput.focus();
        });

        tabBackup.addEventListener('click', function () {
            switchTab(tabBackup, tabTotp, panelBackup, panelTotp);
            hiddenUseBackup.value = 'true';
            backupInput.focus();
        });

        function setError(input, id, message) {
            const element = document.getElementById(id);
            const hasMessage = Boolean(message);
            element.textContent = message || '';
            element.hidden = !hasMessage;
            element.classList.toggle('is-visible', hasMessage);
            input.setAttribute('aria-invalid', hasMessage ? 'true' : 'false');
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

        document.querySelectorAll('[data-unavailable-message]').forEach(function (link) {
            link.addEventListener('click', function (event) {
                event.preventDefault();
                showNotice(link.getAttribute('data-unavailable-message'));
            });
        });

        form.addEventListener('submit', function (event) {
            let valid = true;
            const useBackup = hiddenUseBackup.value === 'true';

            setError(codeInput, 'codeError', '');
            setError(backupInput, 'backupError', '');

            if (useBackup) {
                const backup = (backupInput.value || '').trim();
                if (!backup) {
                    setError(backupInput, 'backupError', 'バックアップコードを入力してください。');
                    valid = false;
                }
            } else {
                const code = (codeInput.value || '').trim();
                if (!code) {
                    setError(codeInput, 'codeError', '認証コードを入力してください。');
                    valid = false;
                } else if (code.length !== 6 || !/^\d{6}$/.test(code)) {
                    setError(codeInput, 'codeError', '認証コードは 6 桁の数字で入力してください。');
                    valid = false;
                }
            }

            if (!valid) {
                event.preventDefault();
                const firstInvalid = form.querySelector('[aria-invalid="true"]');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
            }
        });

        // 初期フォーカス
        codeInput.focus();
    })();
</script>
<jsp:include page="/WEB-INF/views/Common/app-dialog.jsp">
    <jsp:param name="useValidationErrors" value="false" />
    <jsp:param name="primaryLabel" value="閉じる" />
</jsp:include>
</body>
</html>
