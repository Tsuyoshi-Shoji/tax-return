<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="ja_JP" scope="page" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>確定申告 | マネタックス</title>
    <link rel="stylesheet" href="${ctx}/css/style.css?v=20261011c">
    <link rel="stylesheet" href="${ctx}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${ctx}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${ctx}/css/tax-return/tax-return.css?v=20261011c">
    <script src="${ctx}/css/tax-return/tax-return.js?v=20261011c" defer></script>
</head>
<body class="home-page profit-loss-page tax-return-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main profit-loss-main" tabindex="-1"
          data-error-count="${page.errors.size()}" data-warning-count="${page.warnings.size()}">
        <div class="profit-loss-container">
            <nav class="profit-loss-breadcrumb" aria-label="パンくず">
                <ol>
                    <li><a href="${ctx}/home">ホーム</a></li>
                    <li aria-current="page">確定申告</li>
                </ol>
            </nav>

            <div class="tr-title-row">
                <section class="profit-loss-title-area" aria-labelledby="page-title">
                    <h1 id="page-title">確定申告</h1>
                    <p id="page-description">青色申告の申告書作成に必要な各種データの確認・編集・出力ができます。</p>
                </section>
                <div class="tr-controls">
                    <form id="year-form" method="get" action="${ctx}/tax-return" class="tr-control">
                        <label id="label-target-year" for="year-select">対象年度</label>
                        <select id="year-select" name="year">
                            <c:forEach var="opt" items="${page.yearOptions}">
                                <option value="${opt.year}" <c:if test="${opt.year == page.selectedYear}">selected</c:if>><c:out value="${opt.label}" /></option>
                            </c:forEach>
                        </select>
                        <noscript><button type="submit">表示</button></noscript>
                    </form>
                    <form id="deduction-form" method="post" action="${ctx}/tax-return/filing-type?year=${page.selectedYear}" class="tr-control">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <label for="deduction-select">青色申告特別控除</label>
                        <select id="deduction-select" name="deduction" <c:if test="${page.closed}">disabled</c:if>>
                            <option value="65" <c:if test="${page.deduction == 65}">selected</c:if>>65万円</option>
                            <option value="55" <c:if test="${page.deduction == 55}">selected</c:if>>55万円</option>
                            <option value="10" <c:if test="${page.deduction == 10}">selected</c:if>>10万円</option>
                        </select>
                        <noscript><button type="submit">更新</button></noscript>
                    </form>
                    <span id="badge-status" class="tr-status ${page.closed ? 'is-closed' : 'is-draft'}">${page.closed ? '確定済' : '作成中'}</span>
                    <button id="button-error-check" type="button" class="tr-btn tr-btn-outline">エラーチェック</button>
                    <button id="button-pdf-output" type="button" class="tr-btn tr-btn-primary">PDF出力</button>
                </div>
            </div>

            <nav class="tr-tabs" aria-label="確定申告セクション">
                <a id="tab-basic-info" class="tr-tab is-active" href="#section-basic-info">基本情報</a>
                <a id="tab-annual-summary" class="tr-tab" href="#section-annual-summary">年間収支</a>
                <a id="tab-monthly-sales" class="tr-tab" href="#section-monthly">月別売上</a>
                <a id="tab-profit-loss" class="tr-tab" href="#section-profit-loss">損益計算書</a>
                <a id="tab-settlement" class="tr-tab" href="#section-settlement">決算整理</a>
                <a id="tab-balance-sheet" class="tr-tab" href="#section-balance-sheet">貸借対照表</a>
                <a id="tab-journal" class="tr-tab" href="#section-journal">仕訳帳</a>
                <a id="tab-output" class="tr-tab" href="#section-output">出力</a>
            </nav>

            <section id="section-basic-info" class="profit-loss-card tr-section print-section" data-print-groups="return" aria-labelledby="heading-basic-info">
                <div class="tr-section-head">
                    <div>
                        <h2 id="heading-basic-info">基本情報</h2>
                        <p id="text-basic-info-description" class="tr-desc">申告者の基本情報です。内容に誤りがないかご確認ください。</p>
                    </div>
                    <a id="button-edit-basic-info" class="tr-btn tr-btn-soft" href="${ctx}/settings">編集する</a>
                </div>
                <div class="tr-info-grid">
                    <dl class="tr-info">
                        <div><dt id="label-name">氏名</dt><dd id="display-name"><c:out value="${page.basicInfo.name}" default="未設定" /></dd></div>
                        <div><dt id="label-business-name">屋号</dt><dd id="display-business-name"><c:out value="${page.basicInfo.businessName}" default="未設定" /></dd></div>
                        <div><dt>業種</dt><dd id="display-industry"><c:out value="${page.basicInfo.industry}" default="未設定" /></dd></div>
                        <div><dt>電話番号</dt><dd id="display-phone"><c:out value="${page.basicInfo.phone}" default="未設定" /></dd></div>
                    </dl>
                    <dl class="tr-info">
                        <div><dt id="label-address">住所</dt><dd id="display-address"><c:out value="${page.basicInfo.address}" default="未設定" /></dd></div>
                        <div><dt id="label-email">メールアドレス</dt><dd id="display-email"><c:out value="${page.basicInfo.email}" default="未設定" /></dd></div>
                        <div><dt>申告方式</dt><dd id="display-filing-type"><c:out value="${page.basicInfo.filingTypeLabel}" /></dd></div>
                    </dl>
                </div>
            </section>

            <section id="section-annual-summary" class="profit-loss-card tr-section print-section" data-print-groups="return profit-loss" aria-labelledby="heading-annual-summary">
                <h2 id="heading-annual-summary">年間収支サマリー</h2>
                <p id="text-annual-financial-description" class="tr-desc"><c:out value="${page.yearLabel}" />の収支の概要です。</p>
                <div class="tr-cards">
                    <div class="tr-card">
                        <span class="tr-card-icon is-green" aria-hidden="true">↗</span>
                        <div>
                            <div class="tr-card-label">年間売上</div>
                            <div class="tr-card-value"><span id="display-annual-sales"><fmt:formatNumber value="${page.sales}" pattern="#,##0" /></span><small>円</small></div>
                            <c:set var="d" value="${page.salesDiff}" />
                            <div id="display-annual-sales-diff" class="tr-diff ${d.available ? (d.good ? 'is-good' : 'is-bad') : ''}">前年比 <c:out value="${d.amountText}" /><c:if test="${d.available}"> 円（<c:out value="${d.rateText}" />）<span aria-hidden="true">${d.up ? '↗' : '↘'}</span></c:if></div>
                        </div>
                    </div>
                    <div class="tr-card">
                        <span class="tr-card-icon is-red" aria-hidden="true">↘</span>
                        <div>
                            <div class="tr-card-label">年間経費</div>
                            <div class="tr-card-value"><span id="display-annual-expenses"><fmt:formatNumber value="${page.expenses}" pattern="#,##0" /></span><small>円</small></div>
                            <c:set var="d" value="${page.expensesDiff}" />
                            <div id="display-annual-expenses-diff" class="tr-diff ${d.available ? (d.good ? 'is-good' : 'is-bad') : ''}">前年比 <c:out value="${d.amountText}" /><c:if test="${d.available}"> 円（<c:out value="${d.rateText}" />）<span aria-hidden="true">${d.up ? '↗' : '↘'}</span></c:if></div>
                        </div>
                    </div>
                    <div class="tr-card">
                        <span class="tr-card-icon is-purple" aria-hidden="true">▲</span>
                        <div>
                            <div class="tr-card-label">所得</div>
                            <div class="tr-card-value"><span id="display-income-amount"><fmt:formatNumber value="${page.income}" pattern="#,##0" /></span><small>円</small></div>
                            <c:set var="d" value="${page.incomeDiff}" />
                            <div id="display-income-diff" class="tr-diff ${d.available ? (d.good ? 'is-good' : 'is-bad') : ''}">前年比 <c:out value="${d.amountText}" /><c:if test="${d.available}"> 円（<c:out value="${d.rateText}" />）<span aria-hidden="true">${d.up ? '↗' : '↘'}</span></c:if></div>
                        </div>
                    </div>
                    <div class="tr-card">
                        <span class="tr-card-icon is-teal" aria-hidden="true">¥</span>
                        <div>
                            <div class="tr-card-label">青色申告特別控除後所得</div>
                            <div class="tr-card-value"><span id="display-income-after-deduction"><fmt:formatNumber value="${page.incomeAfterDeduction}" pattern="#,##0" /></span><small>円</small></div>
                            <div class="tr-diff">（${page.deduction}万円控除適用後）</div>
                        </div>
                    </div>
                </div>
            </section>

            <section id="section-monthly" class="profit-loss-card tr-section print-section" data-print-groups="return" aria-labelledby="heading-annual-revenue-breakdown">
                <h2 id="heading-annual-revenue-breakdown">年間収支</h2>
                <p id="text-annual-revenue-description" class="tr-desc"><c:out value="${page.yearLabel}" />の月別収支の推移です。</p>
                <div class="tr-table-scroll">
                    <table id="table-monthly-revenue-breakdown" class="tr-table tr-monthly">
                        <thead>
                            <tr><th scope="col"></th>
                                <c:forEach var="m" items="${page.months}"><th scope="col">${m.month}月</th></c:forEach>
                                <th scope="col">年間合計</th></tr>
                        </thead>
                        <tbody>
                            <tr class="row-income"><th scope="row">収益</th>
                                <c:forEach var="m" items="${page.months}"><td><fmt:formatNumber value="${m.income}" pattern="#,##0" /></td></c:forEach>
                                <td class="total"><fmt:formatNumber value="${page.monthsIncomeTotal}" pattern="#,##0" /></td></tr>
                            <tr class="row-expense"><th scope="row">支出</th>
                                <c:forEach var="m" items="${page.months}"><td><fmt:formatNumber value="${m.expense}" pattern="#,##0" /></td></c:forEach>
                                <td class="total"><fmt:formatNumber value="${page.monthsExpenseTotal}" pattern="#,##0" /></td></tr>
                            <tr class="row-profit"><th scope="row">損益</th>
                                <c:forEach var="m" items="${page.months}"><td class="${m.profit.signum() < 0 ? 'is-negative' : ''}"><fmt:formatNumber value="${m.profit}" pattern="#,##0" /></td></c:forEach>
                                <td class="total ${page.income.signum() < 0 ? 'is-negative' : ''}"><fmt:formatNumber value="${page.income}" pattern="#,##0" /></td></tr>
                        </tbody>
                    </table>
                </div>
            </section>

            <div class="tr-two-col">
                <section id="section-profit-loss" class="profit-loss-card tr-section print-section" data-print-groups="return profit-loss" aria-labelledby="heading-profit-loss-statement">
                    <h2 id="heading-profit-loss-statement">損益計算書</h2>
                    <p id="text-profit-loss-description" class="tr-desc"><c:out value="${page.yearLabel}" />の損益計算書の内訳です。</p>
                    <table id="table-profit-loss-statement" class="tr-table tr-pl">
                        <thead><tr><th scope="col">科目</th><th scope="col" class="num">金額（円）</th><th scope="col"><span class="visually-hidden">内訳</span></th></tr></thead>
                        <tbody>
                            <c:forEach var="r" items="${page.plRows}">
                                <tr>
                                    <th scope="row"><c:out value="${r.label}" /></th>
                                    <td class="num"><fmt:formatNumber value="${r.amount}" pattern="#,##0" /></td>
                                    <td class="link"><a class="link-profit-loss-detail" href="${ctx}/profit-loss">内訳 ›</a></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty page.plRows}"><tr><td colspan="3" class="empty">対象年度の損益データはありません。</td></tr></c:if>
                        </tbody>
                        <tfoot><tr><th scope="row">当期純利益</th><td class="num"><fmt:formatNumber value="${page.income}" pattern="#,##0" /></td><td></td></tr></tfoot>
                    </table>
                </section>

                <section id="section-settlement" class="profit-loss-card tr-section" aria-labelledby="heading-settlement-management">
                    <h2 id="heading-settlement-management">決算整理</h2>
                    <p id="text-settlement-description" class="tr-desc">年末の決算整理に関する項目を入力してください。</p>
                    <form id="settlement-form" method="post" action="${ctx}/tax-return/settlement?year=${page.selectedYear}" novalidate>
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <c:set var="s" value="${page.settlement}" />
                        <fieldset class="tr-settlement" <c:if test="${page.closed}">disabled</c:if>>
                            <label class="tr-field"><span>期首棚卸（円）</span><input name="openingInventory" inputmode="numeric" autocomplete="off" value="<c:out value='${s.openingInventory}' />" /></label>
                            <label class="tr-field"><span>期末棚卸（円）</span><input name="closingInventory" inputmode="numeric" autocomplete="off" value="<c:out value='${s.closingInventory}' />" /></label>
                            <label class="tr-field"><span>減価償却費（円）</span><input name="depreciation" inputmode="numeric" autocomplete="off" value="<c:out value='${s.depreciation}' />" /></label>
                            <label class="tr-field"><span>売掛金（円）</span><input name="accountsReceivable" inputmode="numeric" autocomplete="off" value="<c:out value='${s.accountsReceivable}' />" /></label>
                            <label class="tr-field"><span>買掛金（円）</span><input name="accountsPayable" inputmode="numeric" autocomplete="off" value="<c:out value='${s.accountsPayable}' />" /></label>
                            <label class="tr-field"><span>未払金（円）</span><input name="unpaidExpenses" inputmode="numeric" autocomplete="off" value="<c:out value='${s.unpaidExpenses}' />" /></label>
                            <label class="tr-field"><span>未収入金（円）</span><input name="unreceivedIncome" inputmode="numeric" autocomplete="off" value="<c:out value='${s.unreceivedIncome}' />" /></label>
                            <label class="tr-field"><span>期首残高（円）</span><input name="openingBalance" inputmode="numeric" autocomplete="off" value="<c:out value='${s.openingBalance}' />" /></label>
                            <label class="tr-field"><span>期末現預金（円）</span><input name="closingCash" inputmode="numeric" autocomplete="off" value="<c:out value='${s.closingCash}' />" /></label>
                        </fieldset>
                        <div class="tr-actions"><button id="button-save-settlement" type="submit" class="tr-btn tr-btn-primary" <c:if test="${page.closed}">disabled</c:if>>保存する</button></div>
                    </form>
                </section>
            </div>

            <section id="section-balance-sheet" class="profit-loss-card tr-section print-section" data-print-groups="return balance-sheet" aria-labelledby="heading-balance-sheet">
                <h2 id="heading-balance-sheet">貸借対照表</h2>
                <p id="text-balance-sheet-description" class="tr-desc"><c:out value="${page.yearLabel}" />12月31日時点の貸借対照表です。</p>
                <div class="tr-bs">
                    <table id="table-balance-sheet" class="tr-table tr-bs-table">
                        <thead><tr class="is-asset"><th scope="col">資産の部</th><th scope="col" class="num">期首（円）</th><th scope="col" class="num">期末（円）</th></tr></thead>
                        <tbody>
                            <c:forEach var="r" items="${page.assets}"><tr><th scope="row"><c:out value="${r.label}" /></th><td class="num"><fmt:formatNumber value="${r.opening}" pattern="#,##0" /></td><td class="num"><fmt:formatNumber value="${r.closing}" pattern="#,##0" /></td></tr></c:forEach>
                        </tbody>
                        <tfoot><tr class="is-asset"><th scope="row">資産合計</th><td class="num"><fmt:formatNumber value="${page.assetsOpening}" pattern="#,##0" /></td><td class="num"><fmt:formatNumber value="${page.assetsClosing}" pattern="#,##0" /></td></tr></tfoot>
                    </table>
                    <table class="tr-table tr-bs-table">
                        <thead><tr class="is-liability"><th scope="col">負債・資本の部</th><th scope="col" class="num">期首（円）</th><th scope="col" class="num">期末（円）</th></tr></thead>
                        <tbody>
                            <c:forEach var="r" items="${page.liabilities}"><tr><th scope="row"><c:out value="${r.label}" /></th><td class="num"><fmt:formatNumber value="${r.opening}" pattern="#,##0" /></td><td class="num"><fmt:formatNumber value="${r.closing}" pattern="#,##0" /></td></tr></c:forEach>
                        </tbody>
                        <tfoot><tr class="is-liability"><th scope="row">負債・資本合計</th><td class="num"><fmt:formatNumber value="${page.liabilitiesOpening}" pattern="#,##0" /></td><td class="num"><fmt:formatNumber value="${page.liabilitiesClosing}" pattern="#,##0" /></td></tr></tfoot>
                    </table>
                    <div id="check-balance-sheet-confirmed" class="tr-balance ${page.balanced ? 'is-ok' : 'is-ng'}" role="status">
                        <strong>${page.balanced ? '✓ 貸借一致' : '! 貸借不一致'}</strong>
                        <span>${page.balanced ? '資産合計と負債・資本合計が一致しています。' : '資産合計と負債・資本合計が一致していません。'}</span>
                        <c:if test="${not page.balanced}"><span>期末差額 <fmt:formatNumber value="${page.balanceDifference}" pattern="#,##0" />円</span></c:if>
                    </div>
                </div>
            </section>

            <section id="section-journal" class="profit-loss-card tr-section print-section" data-print-groups="journal" aria-labelledby="heading-journal">
                <h2 id="heading-journal">仕訳帳</h2>
                <p id="text-journal-description" class="tr-desc"><c:out value="${page.yearLabel}" />の仕訳帳の一部を表示しています。</p>
                <div class="tr-table-scroll">
                    <table id="table-journal" class="tr-table tr-journal">
                        <thead><tr><th scope="col">日付</th><th scope="col">摘要</th><th scope="col">借方科目</th><th scope="col" class="num">借方金額（円）</th><th scope="col">貸方科目</th><th scope="col" class="num">貸方金額（円）</th></tr></thead>
                        <tbody class="screen-only">
                            <c:forEach var="j" items="${page.journalPreview}">
                                <tr class="${j.continuation ? 'is-continuation' : ''}"><td><c:if test="${not j.continuation}"><c:out value="${j.date}" /></c:if></td><td><c:if test="${not j.continuation}"><c:out value="${j.summary}" /></c:if></td><td><c:out value="${j.debitAccount}" /></td><td class="num"><fmt:formatNumber value="${j.debitAmount}" pattern="#,##0" /></td><td><c:out value="${j.creditAccount}" /></td><td class="num"><c:if test="${j.creditAmount != null}"><fmt:formatNumber value="${j.creditAmount}" pattern="#,##0" /></c:if></td></tr>
                            </c:forEach>
                            <c:if test="${empty page.journalPreview}"><tr><td colspan="6" class="empty">対象年度の仕訳はありません。</td></tr></c:if>
                        </tbody>
                        <tbody class="print-only">
                            <c:forEach var="j" items="${page.journalAll}">
                                <tr class="${j.continuation ? 'is-continuation' : ''}"><td><c:if test="${not j.continuation}"><c:out value="${j.date}" /></c:if></td><td><c:if test="${not j.continuation}"><c:out value="${j.summary}" /></c:if></td><td><c:out value="${j.debitAccount}" /></td><td class="num"><fmt:formatNumber value="${j.debitAmount}" pattern="#,##0" /></td><td><c:out value="${j.creditAccount}" /></td><td class="num"><c:if test="${j.creditAmount != null}"><fmt:formatNumber value="${j.creditAmount}" pattern="#,##0" /></c:if></td></tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </section>

            <section id="section-ledger" class="profit-loss-card tr-section print-section print-only" data-print-groups="ledger" aria-label="総勘定元帳">
                <h2>総勘定元帳</h2>
                <c:forEach var="acc" items="${page.ledger}">
                    <h3 class="tr-ledger-title"><c:out value="${acc.name}" /></h3>
                    <table class="tr-table tr-journal">
                        <thead><tr><th scope="col">日付</th><th scope="col">摘要</th><th scope="col">相手科目</th><th scope="col" class="num">借方（円）</th><th scope="col" class="num">貸方（円）</th></tr></thead>
                        <tbody>
                            <c:forEach var="l" items="${acc.lines}">
                                <tr><td><c:out value="${l.date}" /></td><td><c:out value="${l.summary}" /></td><td><c:out value="${l.counterAccount}" /></td>
                                    <td class="num"><c:if test="${l.debit != null}"><fmt:formatNumber value="${l.debit}" pattern="#,##0" /></c:if></td>
                                    <td class="num"><c:if test="${l.credit != null}"><fmt:formatNumber value="${l.credit}" pattern="#,##0" /></c:if></td></tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:forEach>
            </section>

            <section id="section-checklist" class="profit-loss-card tr-section" aria-labelledby="heading-blue-tax-checklist">
                <h2 id="heading-blue-tax-checklist">青色申告決算書チェック</h2>
                <p id="text-blue-tax-checklist-description" class="tr-desc">申告書の作成に必要な項目のチェック状況です。</p>
                <ul id="checklist-blue-tax-items" class="tr-checks">
                    <c:forEach var="ck" items="${page.checks}">
                        <li id="${ck.id}" class="tr-check ${ck.ok ? 'is-ok' : 'is-warn'}">
                            <span class="tr-check-mark" aria-hidden="true">${ck.ok ? '✓' : '!'}</span>
                            <span><strong><c:out value="${ck.label}" /></strong><small><c:out value="${ck.message}" /></small></span>
                        </li>
                    </c:forEach>
                </ul>
            </section>

            <section id="section-output" class="profit-loss-card tr-section" aria-labelledby="heading-output">
                <h2 id="heading-output">出力</h2>
                <p id="text-output-description" class="tr-desc">作成したデータを各種帳票形式で出力できます。PDFファイルでの出力が可能です。</p>
                <div id="button-pdf-output-group" class="tr-output">
                    <button id="button-pdf-return" type="button" class="tr-btn tr-btn-outline" data-print="return" data-title="青色申告決算書">青色申告決算書PDF</button>
                    <button id="button-pdf-balance-sheet" type="button" class="tr-btn tr-btn-outline" data-print="balance-sheet" data-title="貸借対照表">貸借対照表PDF</button>
                    <button id="button-pdf-profit-loss" type="button" class="tr-btn tr-btn-outline" data-print="profit-loss" data-title="損益計算書">損益計算書PDF</button>
                    <button id="button-pdf-journal" type="button" class="tr-btn tr-btn-outline" data-print="journal" data-title="仕訳帳">仕訳帳PDF</button>
                    <button id="button-pdf-ledger" type="button" class="tr-btn tr-btn-outline" data-print="ledger" data-title="総勘定元帳">総勘定元帳PDF</button>
                </div>
                <p class="tr-desc">PDF出力はブラウザの印刷ダイアログを使用します。送信先で「PDFに保存」を選択してください。</p>
            </section>
        </div>
    </main>
</div>

<dialog id="dialog-check" class="tr-dialog" aria-labelledby="dialog-check-title">
    <h2 id="dialog-check-title">エラーチェック結果</h2>
    <c:if test="${empty page.errors and empty page.warnings}"><p class="tr-dialog-ok">問題は見つかりませんでした。</p></c:if>
    <c:if test="${not empty page.errors}">
        <h3 class="is-error">エラー（出力前に修正が必要です）</h3>
        <ul><c:forEach var="m" items="${page.errors}"><li><c:out value="${m}" /></li></c:forEach></ul>
    </c:if>
    <c:if test="${not empty page.warnings}">
        <h3 class="is-warn">警告</h3>
        <ul><c:forEach var="m" items="${page.warnings}"><li><c:out value="${m}" /></li></c:forEach></ul>
    </c:if>
    <form method="dialog"><button class="tr-btn tr-btn-primary" type="submit">閉じる</button></form>
</dialog>
<dialog id="dialog-alert" class="tr-dialog" aria-labelledby="dialog-alert-title">
    <h2 id="dialog-alert-title">出力できません</h2>
    <p>エラーがあるためPDFを出力できません。エラーチェックの内容をご確認ください。</p>
    <ul><c:forEach var="m" items="${page.errors}"><li><c:out value="${m}" /></li></c:forEach></ul>
    <form method="dialog"><button class="tr-btn tr-btn-primary" type="submit">閉じる</button></form>
</dialog>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
<jsp:include page="/WEB-INF/views/Common/app-dialog.jsp" />
</body>
</html>
