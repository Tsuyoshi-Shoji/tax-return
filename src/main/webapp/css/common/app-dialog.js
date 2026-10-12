(function () {
    'use strict';

    var ICONS = {
        alert: '<svg viewBox="0 0 56 56" aria-hidden="true"><circle cx="28" cy="28" r="26" fill="#ffc61a"/><rect x="25.5" y="14" width="5" height="19" rx="2.5" fill="#1b2250"/><circle cx="28" cy="40" r="3" fill="#1b2250"/></svg>',
        complete: '<svg viewBox="0 0 56 56" aria-hidden="true"><circle cx="28" cy="28" r="26" fill="#12a150"/><path d="M16 29l8 8 16-17" fill="none" stroke="#fff" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        error: '<svg viewBox="0 0 56 56" aria-hidden="true"><circle cx="28" cy="28" r="26" fill="#e8243c"/><path d="M19 19l18 18M37 19L19 37" fill="none" stroke="#fff" stroke-width="5" stroke-linecap="round"/></svg>'
    };

    var DEFAULT_TITLES = { alert: '入力内容をご確認ください', complete: '処理が完了しました', error: '処理に失敗しました' };

    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) { node.className = className; }
        if (text !== undefined && text !== null) { node.textContent = text; }
        return node;
    }

    var counter = 0;

    /**
     * options: type ('alert'|'complete'|'error'), title, message, details (string[]),
     * buttons [{label, primary, href, onClick}], onClose
     */
    function show(options) {
        var type = ICONS[options.type] ? options.type : 'alert';
        var id = 'app-dialog-' + (++counter);
        var dialog = el('dialog', 'app-dialog app-dialog-' + type);
        dialog.setAttribute('aria-labelledby', id + '-title');
        dialog.setAttribute('role', type === 'complete' ? 'dialog' : 'alertdialog');

        var body = el('div', 'app-dialog-body');
        var closeButton = el('button', 'app-dialog-close', '×');
        closeButton.type = 'button';
        closeButton.setAttribute('aria-label', '閉じる');
        body.appendChild(closeButton);

        var icon = el('div', 'app-dialog-icon');
        icon.innerHTML = ICONS[type];
        body.appendChild(icon);

        var title = el('h2', 'app-dialog-title', options.title || DEFAULT_TITLES[type]);
        title.id = id + '-title';
        body.appendChild(title);

        if (options.message) {
            body.appendChild(el('p', 'app-dialog-message', options.message));
        }
        if (options.details && options.details.length) {
            var list = el('ul', 'app-dialog-details');
            options.details.forEach(function (text) { list.appendChild(el('li', null, text)); });
            body.appendChild(list);
        }

        var buttons = options.buttons && options.buttons.length
            ? options.buttons
            : [{ label: '閉じる', primary: true }];
        var actions = el('div', 'app-dialog-actions');
        buttons.forEach(function (def) {
            var button = el('button', 'app-dialog-button' + (def.primary ? ' app-dialog-button-primary' : ''), def.label);
            button.type = 'button';
            button.addEventListener('click', function () {
                dialog.close();
                if (def.onClick) { def.onClick(); }
                if (def.href) { window.location.href = def.href; }
            });
            actions.appendChild(button);
        });
        body.appendChild(actions);
        dialog.appendChild(body);

        closeButton.addEventListener('click', function () { dialog.close(); });
        dialog.addEventListener('close', function () {
            dialog.remove();
            if (options.onClose) { options.onClose(); }
        });
        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) { dialog.close(); }
        });

        document.body.appendChild(dialog);
        dialog.showModal();
        var focusTarget = actions.querySelector('.app-dialog-button-primary') || actions.querySelector('button');
        if (focusTarget) { focusTarget.focus(); }
        return dialog;
    }

    function confirmDialog(options) {
        return new Promise(function (resolve) {
            var settled = false;
            function settle(value) {
                if (!settled) { settled = true; resolve(value); }
            }
            show({
                type: options.type || 'alert',
                title: options.title,
                message: options.message,
                details: options.details,
                buttons: [
                    { label: options.cancelLabel || 'キャンセル', onClick: function () { settle(false); } },
                    { label: options.confirmLabel || '実行する', primary: true, onClick: function () { settle(true); } }
                ],
                onClose: function () { settle(false); }
            });
        });
    }

    // Asks for confirmation on submit, then resubmits once the user accepts.
    function confirmSubmit(form, options) {
        form.addEventListener('submit', function (event) {
            if (event.defaultPrevented || form.dataset.appConfirmed === '1') { return; }
            event.preventDefault();
            confirmDialog(options(form)).then(function (accepted) {
                if (!accepted) { return; }
                form.dataset.appConfirmed = '1';
                HTMLFormElement.prototype.submit.call(form);
            });
        });
    }

    window.AppDialog = {
        show: show,
        alert: function (options) { return show(Object.assign({ type: 'alert' }, options)); },
        error: function (options) { return show(Object.assign({ type: 'error' }, options)); },
        complete: function (options) { return show(Object.assign({ type: 'complete' }, options)); },
        confirm: confirmDialog,
        confirmSubmit: confirmSubmit
    };

    function texts(root, role) {
        return Array.prototype.slice.call(root.querySelectorAll('[data-role="' + role + '"]'))
            .map(function (node) { return node.textContent.trim(); })
            .filter(Boolean);
    }

    function inferCompleteTitle(message) {
        if (/削除/.test(message)) { return '削除が完了しました'; }
        if (/登録/.test(message)) { return '登録が完了しました'; }
        if (/更新|変更|保存/.test(message)) { return '更新が完了しました'; }
        return DEFAULT_TITLES.complete;
    }

    function showServerMessages() {
        var root = document.getElementById('app-dialog-server');
        if (!root) { return; }
        var data = root.dataset;
        var completeMessage = texts(root, 'complete-message')[0];
        var errorMessage = texts(root, 'error-message')[0];
        var errorDetails = texts(root, 'error-detail');
        var alertDetails = texts(root, 'alert-detail');

        if (errorMessage || errorDetails.length) {
            show({
                type: 'error',
                title: data.errorTitle,
                message: errorMessage || errorDetails.shift(),
                details: errorDetails,
                buttons: [{ label: '閉じる', primary: true }]
            });
        } else if (alertDetails.length) {
            show({
                type: 'alert',
                title: data.alertTitle,
                message: data.alertMessage,
                details: alertDetails,
                buttons: [{ label: '内容を確認', primary: true }]
            });
        } else if (completeMessage) {
            var buttons = [];
            if (data.secondaryLabel) { buttons.push({ label: data.secondaryLabel }); }
            buttons.push({ label: data.primaryLabel || '閉じる', primary: true, href: data.primaryHref || '' });
            show({ type: 'complete', title: data.completeTitle || inferCompleteTitle(completeMessage), message: completeMessage, buttons: buttons });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', showServerMessages);
    } else {
        showServerMessages();
    }
})();
