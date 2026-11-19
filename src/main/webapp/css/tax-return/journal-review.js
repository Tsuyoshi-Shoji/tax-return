(function () {
    'use strict';

    function init() {
        var form = document.getElementById('journal-bulk-form');
        var selectAll = document.getElementById('journal-select-all');
        if (!form || !selectAll) {
            return;
        }
        var boxes = Array.prototype.slice.call(document.querySelectorAll('input.journal-select'));
        var counter = document.getElementById('journal-selected-count');

        function refresh() {
            var checked = boxes.filter(function (box) { return box.checked; }).length;
            selectAll.checked = boxes.length > 0 && checked === boxes.length;
            selectAll.indeterminate = checked > 0 && checked < boxes.length;
            if (counter) {
                counter.textContent = checked + '件選択中';
            }
        }

        selectAll.addEventListener('change', function () {
            boxes.forEach(function (box) { box.checked = selectAll.checked; });
            refresh();
        });
        boxes.forEach(function (box) { box.addEventListener('change', refresh); });
        form.addEventListener('submit', function (event) {
            if (!boxes.some(function (box) { return box.checked; })) {
                event.preventDefault();
                window.alert('確認済みにする仕訳を選択してください。');
            }
        });
        refresh();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
}());
