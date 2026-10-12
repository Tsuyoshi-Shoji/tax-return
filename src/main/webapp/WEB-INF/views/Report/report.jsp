<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="ja_JP" scope="page" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>レポート | マネタックス</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/report/report.css?v=20261011c">
    <script src="${pageContext.request.contextPath}/css/report/report.js?v=20261011c" defer></script>
</head>
<body class="home-page profit-loss-page report-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main profit-loss-main" tabindex="-1">
        <div class="profit-loss-container">
            <nav class="profit-loss-breadcrumb" aria-label="パンくず">
                <ol>
                    <li><a href="${pageContext.request.contextPath}/home">ホーム</a></li>
                    <li aria-current="page">レポート</li>
                </ol>
            </nav>

            <div class="report-title-row">
                <section class="profit-loss-title-area" aria-labelledby="page-title">
                    <h1 id="page-title">レポート</h1>
                    <p id="page-description">選択した年度の月別の収支・損益の内訳を確認できます。申告対象の有無に応じた2種類のレポートを表示します。</p>
                </section>
                <form id="year-form" class="report-controls" method="get" action="${pageContext.request.contextPath}/report">
                    <label id="label-year-select" for="year-select">対象年度</label>
                    <select id="year-select" name="year">
                        <c:forEach var="opt" items="${page.yearOptions}">
                            <option value="${opt.year}" <c:if test="${opt.year == page.selectedYear}">selected</c:if>><c:out value="${opt.label}" /></option>
                        </c:forEach>
                    </select>
                    <noscript><button type="submit">表示</button></noscript>
                    <button id="button-pdf-output" type="button" class="btn-primary report-pdf" data-print-title="損益レポート_${page.selectedYear}年度">PDF出力</button>
                </form>
            </div>

            <c:set var="section" value="${page.allData}" />
            <c:set var="suffix" value="all" />
            <c:set var="idHeading" value="heading-all-data-report" />
            <c:set var="sectionTitle" value="全データレポート（申告対象外を含む）" />
            <c:set var="sectionDesc" value="事業収支、配当金、雑収入など、すべての収支データを含めたレポートです。" />
            <%@ include file="/WEB-INF/views/Report/_annual-section.jspf" %>

            <c:set var="section" value="${page.business}" />
            <c:set var="suffix" value="business" />
            <c:set var="idHeading" value="heading-business-report" />
            <c:set var="sectionTitle" value="事業収支レポート（申告対象のみ）" />
            <c:set var="sectionDesc" value="確定申告の対象となる事業収支データのみを集計したレポートです。" />
            <%@ include file="/WEB-INF/views/Report/_annual-section.jspf" %>
        </div>
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>