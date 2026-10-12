(function () {
    'use strict';

    var main = document.getElementById('main-content');
    var errorCount = parseInt(main.getAttribute('data-error-count') || '0', 10);
    var warningCount = parseInt(main.getAttribute('data-warning-count') || '0', 10);

    var yearSelect = document.getElementById('year-select');
    if (yearSelect) {
        yearSelect.addEventListener('change', function () {
            document.getElementById('year-form').submit();
        });
    }

    var deductionSelect = document.getElementById('deduction-select');
    if (deductionSelect) {
        deductionSelect.addEventListener('change', function () {
            document.getElementById('deduction-form').submit();
        });
    }

    var settlementForm = document.getElementById('settlement-form');
    if (settlementForm) {
        settlementForm.addEventListener('submit', function (event) {
            var invalid = false;
            settlementForm.querySelectorAll('input[type="text"], input:not([type])').forEach(function (input) {
                var normalized = input.value.replace(/[０-９]/g, function (c) {
                    return String.fromCharCode(c.charCodeAt(0) - 0xFEE0);
                }).replace(/[,，\s]/g, '');
                var ok = normalized === '' || /^\d{1,10}$/.test(normalized);
                input.setAttribute('aria-invalid', ok ? 'false' : 'true');
                input.style.outline = ok ? '' : '2px solid #ff4d5d';
                if (!ok) {
                    invalid = true;
                } else {
                    input.value = normalized === '' ? '0' : normalized;
                }
            });
            if (invalid) {
                event.preventDefault();
                window.AppDialog.alert({ message: '金額は0〜9,999,999,999の整数で入力してください。', buttons: [{ label: '内容を確認', primary: true }] });
            }
        });
    }

    var checkDialog = document.getElementById('dialog-check');
    var alertDialog = document.getElementById('dialog-alert');
    var errorCheckButton = document.getElementById('button-error-check');
    if (errorCheckButton && checkDialog) {
        errorCheckButton.addEventListener('click', function () { checkDialog.showModal(); });
    }

    function printSection(type, title, forceWarningOutput) {
        if (errorCount > 0) {
            alertDialog.showModal();
            return;
        }
        if (warningCount > 0 && !forceWarningOutput) {
            window.AppDialog.confirm({
                message: '警告があります。このまま出力しますか？（エラーチェックで内容を確認できます）',
                confirmLabel: '出力する'
            }).then(function (accepted) {
                if (accepted) { printSection(type, title, true); }
            });
            return;
        }
        var originalTitle = document.title;
        document.querySelectorAll('.print-section').forEach(function (section) {
            var groups = (section.getAttribute('data-print-groups') || '').split(/\s+/);
            section.classList.toggle('print-hide', groups.indexOf(type) < 0);
        });
        document.body.classList.add('tr-printing');
        document.title = title + '_' + (yearSelect ? yearSelect.value : '') + '年度';
        window.print();
        document.body.classList.remove('tr-printing');
        document.title = originalTitle;
    }

    document.querySelectorAll('[data-print]').forEach(function (button) {
        button.addEventListener('click', function () {
            printSection(button.getAttribute('data-print'), button.getAttribute('data-title'));
        });
    });

    var pdfButton = document.getElementById('button-pdf-output');
    if (pdfButton) {
        pdfButton.addEventListener('click', function () {
            document.getElementById('section-output').scrollIntoView({ behavior: 'smooth' });
        });
    }

    var tabs = Array.prototype.slice.call(document.querySelectorAll('.tr-tab'));
    tabs.forEach(function (tab) {
        tab.addEventListener('click', function (event) {
            var target = document.querySelector(tab.getAttribute('href'));
            if (target) {
                event.preventDefault();
                target.scrollIntoView({ behavior: 'smooth' });
                setActive(tab);
            }
        });
    });

    function setActive(active) {
        tabs.forEach(function (t) { t.classList.toggle('is-active', t === active); });
    }

    if ('IntersectionObserver' in window) {
        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    var match = tabs.filter(function (t) { return t.getAttribute('href') === '#' + entry.target.id; })[0];
                    if (match) { setActive(match); }
                }
            });
        }, { rootMargin: '-20% 0px -70% 0px' });
        tabs.forEach(function (t) {
            var section = document.querySelector(t.getAttribute('href'));
            if (section) { observer.observe(section); }
        });
    }
})();
