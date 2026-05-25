<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>損益レポート</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/report/report.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <main class="report-main">
        <div class="report-container" id="reportContent">
            <div class="page-header">
                <h1>損益レポート</h1>
                <p class="page-subtitle">全データ版と事業収支版を並べて比較できます。</p>
            </div>

            <section class="period-selector" aria-label="年度切替">
                <form id="yearForm" method="get" action="${pageContext.request.contextPath}/report">
                    <label for="yearSelect">表示年度</label>
                    <select id="yearSelect" name="year"></select>
                </form>
                <button type="button" class="btn-primary" id="pdfButton">PDF出力</button>
            </section>

            <section class="report-section" aria-labelledby="allReportTitle" data-report-section="all">
                <div class="section-heading">
                    <h2 id="allReportTitle">全データレポート</h2>
                    <span data-year-label></span>
                </div>
                <div class="report-table-wrapper">
                    <table class="report-table">
                        <thead>
                            <tr>
                                <th>区分</th>
                                <th><a class="month-link" data-month="1" href="#">1月</a></th>
                                <th><a class="month-link" data-month="2" href="#">2月</a></th>
                                <th><a class="month-link" data-month="3" href="#">3月</a></th>
                                <th><a class="month-link" data-month="4" href="#">4月</a></th>
                                <th><a class="month-link" data-month="5" href="#">5月</a></th>
                                <th><a class="month-link" data-month="6" href="#">6月</a></th>
                                <th><a class="month-link" data-month="7" href="#">7月</a></th>
                                <th><a class="month-link" data-month="8" href="#">8月</a></th>
                                <th><a class="month-link" data-month="9" href="#">9月</a></th>
                                <th><a class="month-link" data-month="10" href="#">10月</a></th>
                                <th><a class="month-link" data-month="11" href="#">11月</a></th>
                                <th><a class="month-link" data-month="12" href="#">12月</a></th>
                                <th>年間合計</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr class="income-row">
                                <th>収益</th>
                                <td data-income-month="1">¥0</td>
                                <td data-income-month="2">¥0</td>
                                <td data-income-month="3">¥0</td>
                                <td data-income-month="4">¥0</td>
                                <td data-income-month="5">¥0</td>
                                <td data-income-month="6">¥0</td>
                                <td data-income-month="7">¥0</td>
                                <td data-income-month="8">¥0</td>
                                <td data-income-month="9">¥0</td>
                                <td data-income-month="10">¥0</td>
                                <td data-income-month="11">¥0</td>
                                <td data-income-month="12">¥0</td>
                                <td id="allTotalIncomeCell">¥0</td>
                            </tr>
                            <tr class="expense-row">
                                <th>支出</th>
                                <td data-expense-month="1">¥0</td>
                                <td data-expense-month="2">¥0</td>
                                <td data-expense-month="3">¥0</td>
                                <td data-expense-month="4">¥0</td>
                                <td data-expense-month="5">¥0</td>
                                <td data-expense-month="6">¥0</td>
                                <td data-expense-month="7">¥0</td>
                                <td data-expense-month="8">¥0</td>
                                <td data-expense-month="9">¥0</td>
                                <td data-expense-month="10">¥0</td>
                                <td data-expense-month="11">¥0</td>
                                <td data-expense-month="12">¥0</td>
                                <td id="allTotalExpenseCell">¥0</td>
                            </tr>
                            <tr class="profit-row">
                                <th>損益</th>
                                <td data-profit-month="1">¥0</td>
                                <td data-profit-month="2">¥0</td>
                                <td data-profit-month="3">¥0</td>
                                <td data-profit-month="4">¥0</td>
                                <td data-profit-month="5">¥0</td>
                                <td data-profit-month="6">¥0</td>
                                <td data-profit-month="7">¥0</td>
                                <td data-profit-month="8">¥0</td>
                                <td data-profit-month="9">¥0</td>
                                <td data-profit-month="10">¥0</td>
                                <td data-profit-month="11">¥0</td>
                                <td data-profit-month="12">¥0</td>
                                <td id="allProfitAmountCell">¥0</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <section class="summary-section" aria-label="全データ年度サマリー">
                    <div class="summary-card income-summary">
                        <h3>当年度 総収益</h3>
                        <p id="allTotalIncomeSummary">¥0</p>
                    </div>
                    <div class="summary-card expense-summary">
                        <h3>当年度 総支出</h3>
                        <p id="allTotalExpenseSummary">¥0</p>
                    </div>
                    <div class="summary-card profit-summary">
                        <h3>当年度 損益</h3>
                        <p id="allProfitAmountSummary">¥0</p>
                    </div>
                </section>

                <div class="section-heading">
                    <h2>月別 損益棒グラフ</h2>
                    <span>収益は青、支出は赤で表示します。</span>
                </div>
                <div class="bar-chart" id="allBarChart" aria-label="全データ月別損益棒グラフ"></div>
            </section>

            <section class="report-section" aria-labelledby="businessReportTitle" data-report-section="business">
                <div class="section-heading">
                    <h2 id="businessReportTitle">事業収支レポート</h2>
                    <span data-year-label></span>
                </div>
                <div class="report-table-wrapper">
                    <table class="report-table">
                        <thead>
                            <tr>
                                <th>区分</th>
                                <th><a class="month-link" data-month="1" href="#">1月</a></th>
                                <th><a class="month-link" data-month="2" href="#">2月</a></th>
                                <th><a class="month-link" data-month="3" href="#">3月</a></th>
                                <th><a class="month-link" data-month="4" href="#">4月</a></th>
                                <th><a class="month-link" data-month="5" href="#">5月</a></th>
                                <th><a class="month-link" data-month="6" href="#">6月</a></th>
                                <th><a class="month-link" data-month="7" href="#">7月</a></th>
                                <th><a class="month-link" data-month="8" href="#">8月</a></th>
                                <th><a class="month-link" data-month="9" href="#">9月</a></th>
                                <th><a class="month-link" data-month="10" href="#">10月</a></th>
                                <th><a class="month-link" data-month="11" href="#">11月</a></th>
                                <th><a class="month-link" data-month="12" href="#">12月</a></th>
                                <th>年間合計</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr class="income-row">
                                <th>収益</th>
                                <td data-income-month="1">¥0</td>
                                <td data-income-month="2">¥0</td>
                                <td data-income-month="3">¥0</td>
                                <td data-income-month="4">¥0</td>
                                <td data-income-month="5">¥0</td>
                                <td data-income-month="6">¥0</td>
                                <td data-income-month="7">¥0</td>
                                <td data-income-month="8">¥0</td>
                                <td data-income-month="9">¥0</td>
                                <td data-income-month="10">¥0</td>
                                <td data-income-month="11">¥0</td>
                                <td data-income-month="12">¥0</td>
                                <td id="businessTotalIncomeCell">¥0</td>
                            </tr>
                            <tr class="expense-row">
                                <th>支出</th>
                                <td data-expense-month="1">¥0</td>
                                <td data-expense-month="2">¥0</td>
                                <td data-expense-month="3">¥0</td>
                                <td data-expense-month="4">¥0</td>
                                <td data-expense-month="5">¥0</td>
                                <td data-expense-month="6">¥0</td>
                                <td data-expense-month="7">¥0</td>
                                <td data-expense-month="8">¥0</td>
                                <td data-expense-month="9">¥0</td>
                                <td data-expense-month="10">¥0</td>
                                <td data-expense-month="11">¥0</td>
                                <td data-expense-month="12">¥0</td>
                                <td id="businessTotalExpenseCell">¥0</td>
                            </tr>
                            <tr class="profit-row">
                                <th>損益</th>
                                <td data-profit-month="1">¥0</td>
                                <td data-profit-month="2">¥0</td>
                                <td data-profit-month="3">¥0</td>
                                <td data-profit-month="4">¥0</td>
                                <td data-profit-month="5">¥0</td>
                                <td data-profit-month="6">¥0</td>
                                <td data-profit-month="7">¥0</td>
                                <td data-profit-month="8">¥0</td>
                                <td data-profit-month="9">¥0</td>
                                <td data-profit-month="10">¥0</td>
                                <td data-profit-month="11">¥0</td>
                                <td data-profit-month="12">¥0</td>
                                <td id="businessProfitAmountCell">¥0</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <section class="summary-section" aria-label="事業収支年度サマリー">
                    <div class="summary-card income-summary">
                        <h3>当年度 総収益</h3>
                        <p id="businessTotalIncomeSummary">¥0</p>
                    </div>
                    <div class="summary-card expense-summary">
                        <h3>当年度 総支出</h3>
                        <p id="businessTotalExpenseSummary">¥0</p>
                    </div>
                    <div class="summary-card profit-summary">
                        <h3>当年度 損益</h3>
                        <p id="businessProfitAmountSummary">¥0</p>
                    </div>
                </section>

                <div class="section-heading">
                    <h2>月別 損益棒グラフ</h2>
                    <span>申告対象外の収益を除いた月別損益です。</span>
                </div>
                <div class="bar-chart" id="businessBarChart" aria-label="事業収支月別損益棒グラフ"></div>
            </section>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />

    <script>
        (function () {
            'use strict';

            const contextPath = '${pageContext.request.contextPath}';
            const selectedYear = Number('${reportViewAll.year}');
            const selectableYears = [<c:forEach var="year" items="${reportViewAll.selectableYears}" varStatus="status">${year}<c:if test="${not status.last}">,</c:if></c:forEach>];
            const monthLabels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
            const reportData = {
                all: [
                    <c:forEach var="row" items="${reportViewAll.monthlyRows}" varStatus="status">
                    { month: ${row.month}, income: Number('${row.income}'), expense: Number('${row.expense}') }<c:if test="${not status.last}">,</c:if>
                    </c:forEach>
                ],
                business: [
                    <c:forEach var="row" items="${reportViewBusiness.monthlyRows}" varStatus="status">
                    { month: ${row.month}, income: Number('${row.income}'), expense: Number('${row.expense}') }<c:if test="${not status.last}">,</c:if>
                    </c:forEach>
                ]
            };
            const reportTotals = {
                all: {
                    income: Number('${reportViewAll.totalIncome}'),
                    expense: Number('${reportViewAll.totalExpense}'),
                    profit: Number('${reportViewAll.profitAmount}')
                },
                business: {
                    income: Number('${reportViewBusiness.totalIncome}'),
                    expense: Number('${reportViewBusiness.totalExpense}'),
                    profit: Number('${reportViewBusiness.profitAmount}')
                }
            };

            function formatCurrency(value) {
                return '¥' + Number(value || 0).toLocaleString('ja-JP');
            }

            function setupYearSelector() {
                const yearSelect = document.getElementById('yearSelect');
                selectableYears.forEach(year => {
                    const option = document.createElement('option');
                    option.value = String(year);
                    option.textContent = year + '年度';
                    option.selected = year === selectedYear;
                    yearSelect.appendChild(option);
                });
                yearSelect.addEventListener('change', () => {
                    document.getElementById('yearForm').submit();
                });
                document.querySelectorAll('[data-year-label]').forEach(label => {
                    label.textContent = selectedYear + '年度';
                });
            }

            function detailUrl(month) {
                return contextPath + '/report/detail?year=' + encodeURIComponent(selectedYear) + '&month=' + encodeURIComponent(month);
            }

            function renderReport(sectionId, key) {
                const section = document.querySelector('[data-report-section="' + sectionId + '"]');
                const rows = reportData[key];
                const totals = reportTotals[key];
                let totalIncome = 0;
                let totalExpense = 0;

                rows.forEach(row => {
                    const profit = row.income - row.expense;
                    totalIncome += row.income;
                    totalExpense += row.expense;
                    section.querySelector('[data-income-month="' + row.month + '"]').textContent = formatCurrency(row.income);
                    section.querySelector('[data-expense-month="' + row.month + '"]').textContent = formatCurrency(row.expense);
                    section.querySelector('[data-profit-month="' + row.month + '"]').textContent = formatCurrency(profit);
                });

                section.querySelectorAll('.month-link').forEach(link => {
                    link.href = detailUrl(link.dataset.month);
                });

                section.querySelector('#' + key + 'TotalIncomeCell').textContent = formatCurrency(totalIncome);
                section.querySelector('#' + key + 'TotalExpenseCell').textContent = formatCurrency(totalExpense);
                section.querySelector('#' + key + 'ProfitAmountCell').textContent = formatCurrency(totalIncome - totalExpense);
                section.querySelector('#' + key + 'TotalIncomeSummary').textContent = formatCurrency(totalIncome);
                section.querySelector('#' + key + 'TotalExpenseSummary').textContent = formatCurrency(totalExpense);
                section.querySelector('#' + key + 'ProfitAmountSummary').textContent = formatCurrency(totalIncome - totalExpense);
                renderChart(section.querySelector('#' + key + 'BarChart'), rows);

                // keep server-calculated totals in sync for validation/debugging
                void totals;
            }

            function renderChart(chart, rows) {
                const maxValue = Math.max(1, ...rows.map(row => Math.max(row.income, row.expense)));
                chart.innerHTML = '';

                rows.forEach(row => {
                    const group = document.createElement('a');
                    group.className = 'bar-group';
                    group.href = detailUrl(row.month);
                    group.setAttribute('aria-label', selectedYear + '年' + row.month + '月の詳細レポート');

                    const bars = document.createElement('div');
                    bars.className = 'bars';

                    const incomeBar = document.createElement('span');
                    incomeBar.className = 'bar income-bar';
                    incomeBar.style.height = Math.max(4, (row.income / maxValue) * 180) + 'px';
                    incomeBar.title = '収益 ' + formatCurrency(row.income);

                    const expenseBar = document.createElement('span');
                    expenseBar.className = 'bar expense-bar';
                    expenseBar.style.height = Math.max(4, (row.expense / maxValue) * 180) + 'px';
                    expenseBar.title = '支出 ' + formatCurrency(row.expense);

                    const label = document.createElement('span');
                    label.className = 'bar-label';
                    label.textContent = monthLabels[row.month - 1];

                    bars.appendChild(incomeBar);
                    bars.appendChild(expenseBar);
                    group.appendChild(bars);
                    group.appendChild(label);
                    chart.appendChild(group);
                });
            }

            function exportPdf() {
                const originalTitle = document.title;
                document.title = '損益レポート_' + selectedYear;
                window.print();
                document.title = originalTitle;
            }

            setupYearSelector();
            renderReport('all', 'all');
            renderReport('business', 'business');
            document.getElementById('pdfButton').addEventListener('click', exportPdf);
        })();
    </script>
</body>
</html>

