<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="ja_JP" scope="page" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>損益詳細 | マネタックス</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss-detail.css?v=20261011c">
    <script src="${pageContext.request.contextPath}/css/profit-loss/profit-loss-detail.js?v=20261011c" defer></script>
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
                    <li><a href="${pageContext.request.contextPath}/profit-loss">損益一覧</a></li>
                    <li aria-current="page">損益詳細</li>
                </ol>
            </nav>

            <section class="profit-loss-card detail-card" aria-labelledby="page-title">
                <h1 id="page-title">損益詳細</h1>
                <p id="page-description">一覧で選択した損益データの詳細を確認できます。</p>
                <c:if test="${not empty detail.lockMessage}">
                    <div class="detail-notice" role="status"><c:out value="${detail.lockMessage}" /></div>
                </c:if>

                <c:set var="kindName" value="${detail.expense ? '支出' : '収益'}" />
                <dl class="detail-list">
                    <dt id="label-category">区分</dt>
                    <dd><span id="display-category" class="detail-field"><c:out value="${detail.categoryLabel}" /></span></dd>

                    <dt id="label-item"><c:out value="${kindName}" />項目</dt>
                    <dd><span id="display-item" class="detail-field"><c:out value="${detail.itemName}" /></span></dd>

                    <dt id="label-date"><c:out value="${kindName}" />発生日</dt>
                    <dd><span id="display-date" class="detail-field"><c:out value="${detail.transactionDateText}" /></span></dd>

                    <dt id="label-amount">金額</dt>
                    <dd class="detail-amount-row">
                        <span id="display-amount" class="detail-field"><fmt:formatNumber value="${detail.amount}" pattern="#,##0" /></span>
                        <span class="detail-unit">円</span>
                    </dd>

                    <dt id="label-payment-method"><c:out value="${detail.methodLabel}" /></dt>
                    <dd><span id="display-payment-method" class="detail-field"><c:out value="${detail.methodName}" /></span></dd>

                    <dt>確定申告対象外</dt>
                    <dd>
                        <label class="detail-check">
                            <input id="checkbox-exclude-from-return" type="checkbox" disabled <c:if test="${detail.excludeFromReturn}">checked</c:if> />
                            この<c:out value="${kindName}" />を確定申告対象外にする
                        </label>
                    </dd>
                </dl>

                <c:if test="${detail.expense and detail.apportionmentApplied}">
                    <section class="detail-apportionment" aria-labelledby="heading-home-apportionment">
                        <h2 id="heading-home-apportionment">家事按分</h2>
                        <div class="detail-apportionment-body">
                            <label class="detail-check">
                                <input id="display-apportionment-applied" type="checkbox" disabled checked /> 家事按分を適用する
                            </label>
                            <p id="text-apportionment-description" class="detail-note">設定画面で保持している固定按分率 <fmt:formatNumber value="${detail.apportionmentRate}" pattern="#,##0.##" />% を使用して経費対象金額を計算します。</p>
                            <div class="detail-sub-row">
                                <span class="detail-sub-label">家事按分率</span>
                                <span id="display-apportionment-rate" class="detail-field"><fmt:formatNumber value="${detail.apportionmentRate}" pattern="#,##0.##" />%</span>
                            </div>
                            <div class="detail-sub-row">
                                <span class="detail-sub-label">経費対象金額</span>
                                <span id="display-deductible-amount" class="detail-field"><fmt:formatNumber value="${detail.deductibleAmount}" pattern="#,##0" />円</span>
                                <span class="detail-note">(<fmt:formatNumber value="${detail.amount}" pattern="#,##0" />円 × <fmt:formatNumber value="${detail.apportionmentRate}" pattern="#,##0.##" />%)</span>
                            </div>
                        </div>
                    </section>
                </c:if>

                <dl class="detail-list detail-list-details">
                    <dt id="label-details"><c:out value="${kindName}" />詳細</dt>
                    <dd><div id="display-details" class="detail-field detail-text"><c:out value="${detail.details}" default="-" /></div></dd>
                </dl>

                <div class="detail-actions">
                    <a class="detail-back" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(returnUrl)}">一覧へ戻る</a>
                    <c:if test="${detail.editable}">
                        <c:url var="editUrl" value="/${detail.recordType}">
                            <c:param name="editId" value="${detail.id}" />
                            <c:param name="searchQuery" value="${returnQuery}" />
                        </c:url>
                        <a id="button-edit" class="btn-primary" href="${editUrl}">編集</a>
                        <form id="delete-form" method="post" action="${pageContext.request.contextPath}/profit-loss/delete" data-confirm-message="このデータを削除します。よろしいですか？">
                            <input type="hidden" name="recordType" value="<c:out value='${detail.recordType}' />" />
                            <input type="hidden" name="id" value="${detail.id}" />
                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                            <input type="hidden" name="returnQuery" value="<c:out value='${returnQuery}' />" />
                            <button id="button-delete" type="submit" class="btn-danger">削除</button>
                        </form>
                    </c:if>
                </div>
            </section>
        </div>
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
<jsp:include page="/WEB-INF/views/Common/app-dialog.jsp" />
</body>
</html>