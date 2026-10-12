<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="ja_JP" scope="page" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>損益一覧 | マネタックス</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss-detail.css?v=20261011c">
    <script src="${pageContext.request.contextPath}/css/profit-loss/profit-loss.js?v=20261011c" defer></script>
</head>
<body class="home-page profit-loss-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main profit-loss-main" tabindex="-1">
        <div class="profit-loss-container">
            <nav class="profit-loss-breadcrumb" aria-label="パンくず">
                <ol>
                    <li><a href="${pageContext.request.contextPath}/home">ホーム</a></li>
                    <li aria-current="page">損益一覧</li>
                </ol>
            </nav>

            <section class="profit-loss-title-area" aria-labelledby="page-title">
                <h1 id="page-title">損益一覧</h1>
                <p id="page-description">収益・支出の一覧を確認できます。期間や条件で絞り込んで検索することができます。</p>
            </section>

            <section class="profit-loss-card search-card" aria-labelledby="search-heading">
                <h2 id="search-heading">検索条件</h2>
                <c:if test="${not empty page.validationErrors}">
                    <div id="search-error-panel" class="search-errors" role="alert" aria-live="polite">
                        <ul>
                            <c:forEach var="message" items="${page.validationErrors}">
                                <li><c:out value="${message}" /></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
                <form id="profit-loss-search-form" method="get" action="${pageContext.request.contextPath}/profit-loss" novalidate>
                    <input type="hidden" id="income-page" name="incomePage" value="${page.incomeCurrentPage}" />
                    <input type="hidden" id="expense-page" name="expensePage" value="${page.expenseCurrentPage}" />
                    <div class="search-rows">
                        <div class="search-row search-row-top">
                            <div class="field-group">
                                <label id="label-date-from" for="date-from">発生日From</label>
                                <input id="date-from" name="dateFrom" type="date" value="<c:out value='${page.form.dateFrom}' />" />
                            </div>
                            <div class="field-group">
                                <label id="label-date-to" for="date-to">発生日To</label>
                                <input id="date-to" name="dateTo" type="date" value="<c:out value='${page.form.dateTo}' />" />
                            </div>
                            <div class="field-group">
                                <span id="label-types" class="group-label">区分</span>
                                <div class="inline-options">
                                    <label class="check-option"><input type="checkbox" name="types" value="income" <c:if test="${page.incomeTypeSelected}">checked</c:if> />収益</label>
                                    <label class="check-option"><input type="checkbox" name="types" value="expense" <c:if test="${page.expenseTypeSelected}">checked</c:if> />支出</label>
                                </div>
                            </div>
                            <div class="field-group">
                                <span id="label-tax-target" class="group-label">申告対象外データ</span>
                                <div class="inline-options">
                                    <label class="radio-option"><input type="radio" name="taxTarget" value="show" <c:if test="${page.form.taxTarget ne 'hide'}">checked</c:if> />表示する</label>
                                    <label class="radio-option"><input type="radio" name="taxTarget" value="hide" <c:if test="${page.form.taxTarget eq 'hide'}">checked</c:if> />表示しない</label>
                                </div>
                            </div>
                        </div>
                        <div class="search-row search-row-bottom">
                            <div class="field-group">
                                <label id="label-items" for="button-items-toggle">項目</label>
                                <div id="item-selector" class="item-selector">
                                    <button id="button-items-toggle" class="item-selector-toggle" type="button" aria-haspopup="listbox" aria-expanded="false" aria-controls="items-dropdown">
                                        <span id="items-selected-label">すべて</span>
                                        <span class="ui-icon icon-chevron-down" aria-hidden="true"></span>
                                    </button>
                                    <div id="items-dropdown" class="items-dropdown" hidden>
                                        <div class="items-dropdown-search-wrap">
                                            <input id="items-search" class="items-search" type="text" placeholder="項目を検索" autocomplete="off" />
                                        </div>
                                        <ul id="items-options" class="items-options" role="listbox" aria-multiselectable="true">
                                            <c:forEach var="option" items="${page.itemOptions}">
                                                <li class="items-option-row" data-value="${option.value}" data-group="${option.group}">
                                                    <label class="items-option-label">
                                                        <input class="items-option-checkbox" type="checkbox" value="${option.value}" />
                                                        <span><c:out value="${option.label}" /></span>
                                                    </label>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </div>
                                    <div id="items-hidden-container">
                                        <c:forEach var="selectedItem" items="${page.form.items}">
                                            <input type="hidden" name="items" value="${selectedItem}" />
                                        </c:forEach>
                                    </div>
                                </div>
                            </div>
                            <div class="field-group">
                                <label id="label-amount-from" for="amount-from">金額From</label>
                                <input id="amount-from" name="amountFrom" type="text" inputmode="numeric" placeholder="例）10000" value="<c:out value='${page.form.amountFrom}' />" />
                            </div>
                            <div class="field-group">
                                <label id="label-amount-to" for="amount-to">金額To</label>
                                <input id="amount-to" name="amountTo" type="text" inputmode="numeric" placeholder="例）100000" value="<c:out value='${page.form.amountTo}' />" />
                            </div>
                            <div class="field-group actions-cell">
                                <button id="button-clear-conditions" class="button-secondary" type="button">条件クリア</button>
                                <button id="button-search" class="button-primary" type="submit">検索</button>
                            </div>
                        </div>
                    </div>
                </form>
            </section>

            <section class="summary-grid" aria-labelledby="heading-profit-loss-summary">
                <h2 id="heading-profit-loss-summary" class="visually-hidden">損益集計</h2>
                <article class="profit-loss-card summary-card">
                    <div class="summary-icon summary-icon-blue"><span class="ui-icon icon-bars" aria-hidden="true"></span></div>
                    <div>
                        <h3>損益累計</h3>
                        <p class="summary-amount"><c:if test="${page.totalProfit gt 0}">+</c:if><fmt:formatNumber value="${page.totalProfit}" pattern="#,##0" /> <small>円</small></p>
                        <p class="summary-sub">収益 <fmt:formatNumber value="${page.summaryIncome}" pattern="#,##0" /> 円 − 支出 <fmt:formatNumber value="${page.summaryExpense}" pattern="#,##0" /> 円</p>
                    </div>
                </article>
                <article class="profit-loss-card summary-card">
                    <div class="summary-icon summary-icon-green"><span class="ui-icon icon-trend" aria-hidden="true"></span></div>
                    <div>
                        <h3>収益累計</h3>
                        <p class="summary-amount summary-income"><fmt:formatNumber value="${page.totalIncome}" pattern="#,##0" /> <small>円</small></p>
                    </div>
                </article>
                <article class="profit-loss-card summary-card">
                    <div class="summary-icon summary-icon-red"><span class="ui-icon icon-trend" aria-hidden="true"></span></div>
                    <div>
                        <h3>支出累計</h3>
                        <p class="summary-amount summary-expense"><fmt:formatNumber value="${page.totalExpense}" pattern="#,##0" /> <small>円</small></p>
                        <c:if test="${page.totalOwnerDraw gt 0}"><p class="summary-sub">家事按分の事業主貸 <fmt:formatNumber value="${page.totalOwnerDraw}" pattern="#,##0" /> 円（経費対象外）</p></c:if>
                    </div>
                </article>
            </section>

            <section class="profit-loss-card list-card" aria-labelledby="heading-income-list">
                <div class="list-header">
                    <h2 id="heading-income-list">収益一覧</h2>
                    <div id="pagination-income" class="pagination-area">
                        <span>全<c:out value="${page.incomeTotalCount}" />件 / 1ページ10件</span>
                        <button class="page-nav" type="button" data-target-page="income-page" data-page="${page.incomeCurrentPage - 1}" <c:if test="${page.incomeCurrentPage le 1}">disabled</c:if>>‹</button>
                        <c:forEach var="pageNo" items="${page.incomePageNumbers}">
                            <button class="page-index ${pageNo eq page.incomeCurrentPage ? 'is-active' : ''}" type="button" data-target-page="income-page" data-page="${pageNo}"><c:out value="${pageNo}" /></button>
                        </c:forEach>
                        <button class="page-nav" type="button" data-target-page="income-page" data-page="${page.incomeCurrentPage + 1}" <c:if test="${page.incomeCurrentPage ge page.incomeTotalPages}">disabled</c:if>>›</button>
                    </div>
                </div>
                <div class="table-wrap">
                    <table id="table-income-list" class="list-table">
                        <thead>
                        <tr>
                            <th>発生日</th>
                            <th>区分</th>
                            <th>項目</th>
                            <th>詳細</th>
                            <th class="align-right">金額</th>
                            <th>申告対象</th>
                            <th>詳細</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="row" items="${page.incomeRows}">
                            <tr>
                                <td><c:out value="${row.dateLabel}" /></td>
                                <td><span class="type-pill type-income"><c:out value="${row.type}" /></span></td>
                                <td><c:out value="${row.item}" /></td>
                                <td><c:out value="${row.details}" default="-" /></td>
                                <td class="align-right amount-income"><fmt:formatNumber value="${row.amount}" pattern="#,##0" /> 円</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${row.taxTarget}"><span class="target-pill target-yes">対象</span></c:when>
                                        <c:otherwise><span class="target-pill target-no">対象外</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><a class="detail-link" href="${pageContext.request.contextPath}/profit-loss/detail?recordType=${row.recordType}&id=${row.id}">詳細</a></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty page.incomeRows}">
                            <tr><td colspan="7" class="empty-row">該当する収益データはありません。</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="profit-loss-card list-card" aria-labelledby="heading-expense-list">
                <div class="list-header">
                    <h2 id="heading-expense-list">支出一覧</h2>
                    <div id="pagination-expense" class="pagination-area">
                        <span>全<c:out value="${page.expenseTotalCount}" />件 / 1ページ10件</span>
                        <button class="page-nav" type="button" data-target-page="expense-page" data-page="${page.expenseCurrentPage - 1}" <c:if test="${page.expenseCurrentPage le 1}">disabled</c:if>>‹</button>
                        <c:forEach var="pageNo" items="${page.expensePageNumbers}">
                            <button class="page-index ${pageNo eq page.expenseCurrentPage ? 'is-active' : ''}" type="button" data-target-page="expense-page" data-page="${pageNo}"><c:out value="${pageNo}" /></button>
                        </c:forEach>
                        <button class="page-nav" type="button" data-target-page="expense-page" data-page="${page.expenseCurrentPage + 1}" <c:if test="${page.expenseCurrentPage ge page.expenseTotalPages}">disabled</c:if>>›</button>
                    </div>
                </div>
                <div class="table-wrap">
                    <table id="table-expense-list" class="list-table">
                        <thead>
                        <tr>
                            <th>発生日</th>
                            <th>区分</th>
                            <th>項目</th>
                            <th>詳細</th>
                            <th class="align-right">金額</th>
                            <th>申告対象</th>
                            <th>詳細</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="row" items="${page.expenseRows}">
                            <tr>
                                <td><c:out value="${row.dateLabel}" /></td>
                                <td><span class="type-pill type-expense"><c:out value="${row.type}" /></span></td>
                                <td><c:out value="${row.item}" /></td>
                                <td><c:out value="${row.details}" default="-" /></td>
                                <td class="align-right amount-expense"><fmt:formatNumber value="${row.amount}" pattern="#,##0" /> 円</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${row.taxTarget}"><span class="target-pill target-yes">対象</span></c:when>
                                        <c:otherwise><span class="target-pill target-no">対象外</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><a class="detail-link" href="${pageContext.request.contextPath}/profit-loss/detail?recordType=${row.recordType}&id=${row.id}">詳細</a></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty page.expenseRows}">
                            <tr><td colspan="7" class="empty-row">該当する支出データはありません。</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
<jsp:include page="/WEB-INF/views/Common/app-dialog.jsp" />
</body>
</html>
