<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>損益詳細</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <main>
        <div class="container profit-loss-container">
            <div class="page-header">
                <h1>損益詳細</h1>
                <p class="page-subtitle">登録内容の確認、編集、削除を行うための詳細画面です。</p>
            </div>

            <section class="detail-card" aria-labelledby="detailTitle">
                <div class="section-heading">
                    <h2 id="detailTitle">詳細情報</h2>
                    <span class="status-note">編集画面遷移と論理削除に対応しています。</span>
                </div>

                <dl class="detail-list">
                    <div class="detail-row">
                        <dt>種別</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${detail.recordType == 'income'}">収益</c:when>
                                <c:when test="${detail.recordType == 'expense'}">支出</c:when>
                                <c:when test="${param.recordType == 'income'}">収益</c:when>
                                <c:when test="${param.recordType == 'expense'}">支出</c:when>
                                <c:otherwise>未指定</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                    <div class="detail-row">
                        <dt>発生日</dt>
                        <dd><c:out value="${empty detail.date ? '-' : detail.date}" /></dd>
                    </div>
                    <div class="detail-row">
                        <dt>区分</dt>
                        <dd><c:out value="${empty detail.type ? '-' : detail.type}" /></dd>
                    </div>
                    <div class="detail-row">
                        <dt>項目</dt>
                        <dd><c:out value="${empty detail.item ? '-' : detail.item}" /></dd>
                    </div>
                    <div class="detail-row">
                        <dt>金額</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${not empty detail.amount}">¥<fmt:formatNumber value="${detail.amount}" pattern="#,##0" /></c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                    <div class="detail-row detail-row-wide">
                        <dt>詳細</dt>
                        <dd><c:out value="${empty detail.description ? '-' : detail.description}" /></dd>
                    </div>
                </dl>

                <div class="detail-actions">
                    <a class="btn-secondary" href="${pageContext.request.contextPath}/profit-loss">一覧へ戻る</a>
                    <c:choose>
                        <c:when test="${detail.recordType == 'income'}">
                            <a class="btn-secondary" href="${pageContext.request.contextPath}/income?editId=${detail.id}&amp;returnTo=detail">編集</a>
                        </c:when>
                        <c:otherwise>
                            <a class="btn-secondary" href="${pageContext.request.contextPath}/expense?editId=${detail.id}&amp;returnTo=detail">編集</a>
                        </c:otherwise>
                    </c:choose>
                    <form method="post" action="${pageContext.request.contextPath}/profit-loss/delete" id="deleteForm">
                        <c:if test="${not empty _csrf}">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        </c:if>
                        <input type="hidden" name="recordType" value="<c:out value='${detail.recordType}' />" />
                        <input type="hidden" name="id" value="<c:out value='${detail.id}' />" />
                        <button type="submit" class="danger-button" id="deleteButton">削除</button>
                    </form>
                </div>
            </section>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />

    <script>
        (function () {
            'use strict';

            const deleteForm = document.getElementById('deleteForm');
            deleteForm.addEventListener('submit', function (event) {
                if (!window.confirm('このデータを削除します。よろしいですか？')) {
                    event.preventDefault();
                }
            });
        })();
    </script>
</body>
</html>
