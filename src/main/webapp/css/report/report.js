(function () {
    var yearSelect = document.getElementById('year-select');
    if (yearSelect && yearSelect.form) {
        yearSelect.addEventListener('change', function () {
            yearSelect.form.submit();
        });
    }

    var monthSelect = document.getElementById('month-select');
    if (monthSelect && monthSelect.form) {
        monthSelect.addEventListener('change', function () {
            monthSelect.form.submit();
        });
    }

    var pdfButton = document.getElementById('button-pdf-output');
    if (pdfButton) {
        pdfButton.addEventListener('click', function () {
            var originalTitle = document.title;
            document.title = pdfButton.getAttribute('data-print-title') || originalTitle;
            window.print();
            document.title = originalTitle;
        });
    }
})();