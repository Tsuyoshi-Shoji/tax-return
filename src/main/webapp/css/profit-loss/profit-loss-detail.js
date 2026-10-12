(function () {
    var form = document.getElementById('delete-form');
    if (!form) {
        return;
    }
    window.AppDialog.confirmSubmit(form, function () {
        return {
            title: '削除してもよろしいですか？',
            message: form.getAttribute('data-confirm-message'),
            confirmLabel: '削除する'
        };
    });
})();