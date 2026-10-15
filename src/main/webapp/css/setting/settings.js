(function () {
    function byId(id) { return document.getElementById(id); }

    document.querySelectorAll('.pw-toggle').forEach(function (button) {
        button.addEventListener('click', function () {
            var input = byId(button.getAttribute('data-target'));
            if (!input) { return; }
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            button.textContent = show ? '非表示' : '表示';
            button.setAttribute('aria-pressed', show ? 'true' : 'false');
            button.setAttribute('aria-label', show ? 'パスワードを隠す' : 'パスワードを表示');
        });
    });

    function showErrors(form, messages) {
        var holder = form.querySelector('.form-error');
        if (!holder) {
            holder = form.querySelector('.settings-error-inline');
        }
        if (holder) { holder.textContent = messages.join(' '); }
        return messages.length === 0;
    }

    var profileForm = byId('profile-form');
    if (profileForm) {
        profileForm.addEventListener('submit', function (event) {
            var name = byId('display-username').value.trim();
            if (name.length < 1 || name.length > 100) {
                event.preventDefault();
                window.AppDialog.alert({ message: 'ユーザー名は1〜100文字で入力してください。', buttons: [{ label: '内容を確認', primary: true }] });
            }
        });
    }

    var passwordForm = byId('password-form');
    if (passwordForm) {
        passwordForm.addEventListener('submit', function (event) {
            var messages = [];
            var current = byId('input-current-password').value;
            var next = byId('input-new-password').value;
            var confirm = byId('input-confirm-password').value;
            if (!current) { messages.push('現在のパスワードを入力してください。'); }
            if (next.length < 8 || next.length > 72) {
                messages.push('新しいパスワードは8〜72文字で入力してください。');
            } else if (next !== confirm) {
                messages.push('新しいパスワードと確認用パスワードが一致しません。');
            }
            if (messages.length) {
                event.preventDefault();
                window.AppDialog.alert({ message: '入力内容をご確認ください。', details: messages, buttons: [{ label: '内容を確認', primary: true }] });
            }
        });
    }

    var taxBasicInfoForm = byId('tax-basic-info-form');
    if (taxBasicInfoForm) {
        taxBasicInfoForm.addEventListener('submit', function (event) {
            var messages = [];
            var businessName = byId('input-business-name').value.trim();
            var industry = byId('input-industry').value.trim();
            var phone = byId('input-phone').value.trim();
            var address = byId('input-address').value.trim();
            if (businessName.length > 200) { messages.push('屋号は200文字以内で入力してください。'); }
            if (industry.length > 100) { messages.push('業種は100文字以内で入力してください。'); }
            if (phone && !/^[0-9+()\-\s]{1,30}$/.test(phone)) {
                messages.push('電話番号は30文字以内で、数字と +()- のみ使用できます。');
            }
            if (address.length > 255) { messages.push('住所は255文字以内で入力してください。'); }
            if (messages.length) {
                event.preventDefault();
                window.AppDialog.alert({ message: '入力内容をご確認ください。', details: messages, buttons: [{ label: '内容を確認', primary: true }] });
            }
        });
    }

    var ratioInput = byId('input-apportionment-rate');
    var ratioError = byId('ratio-error');
    var result = byId('example-result');
    function validRatio(value) {
        return /^[0-9]{1,3}$/.test(value) && Number(value) <= 100;
    }
    function updatePreview() {
        var value = ratioInput.value.trim();
        if (ratioError) { ratioError.textContent = ''; }
        if (!validRatio(value)) {
            result.textContent = '-';
            return;
        }
        var deductible = 10000 - Math.floor(10000 * Number(value) / 100);
        result.textContent = deductible.toLocaleString('ja-JP');
    }
    if (ratioInput && result) {
        ratioInput.addEventListener('input', updatePreview);
        updatePreview();
        byId('ratio-form').addEventListener('submit', function (event) {
            if (!validRatio(ratioInput.value.trim())) {
                event.preventDefault();
                ratioError.textContent = '家事按分率は0〜100%の範囲で入力してください。';
            }
        });
    }

    ['disable-form', 'delete-form'].forEach(function (id) {
        var form = byId(id);
        if (!form) { return; }
        form.addEventListener('submit', function (event) {
            var expected = form.getAttribute('data-expected');
            var text = form.querySelector('input[name="confirmText"]').value.trim();
            if (text !== expected) {
                event.preventDefault();
                showErrors(form, ['確認欄に「' + expected + '」と入力してください。']);
                return;
            }
            showErrors(form, []);
        });
        window.AppDialog.confirmSubmit(form, function () {
            return { message: form.getAttribute('data-confirm-message'), confirmLabel: '実行する' };
        });
    });
})();
