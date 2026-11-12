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
    <title>仕訳帳の確認 | マネタックス</title>
    <link rel="stylesheet" href="${ctx}/css/style.css?v=20261016a">
    <link rel="stylesheet" href="${ctx}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${ctx}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${ctx}/css/tax-return/tax-return.css?v=20261108a">
    <link rel="stylesheet" href="${ctx}/css/tax-return/journal-review.css?v=20261113a">
</head>
<body class="home-page profit-loss-page tax-return-page journal-review-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main profit-loss-main" tabindex="-1">
        <div class="profit-loss-container">
            <nav class="profit-loss-breadcrumb" aria-label="パンくず">
                <ol><li><a href="${ctx}/home">ホーム</a></li><li><a href="${ctx}/tax-return?year=${review.year}">確定申告</a></li><li aria-current="page">仕訳帳の確認</li></ol>
            </nav>
            <section class="profit-loss-title-area" aria-labelledby="page-title">
                <h1 id="page-title">仕訳帳の確認</h1>
                <p>日付・摘要・借方・貸方・金額を証憑と照合し、仕訳ごとに確認済みにしてください。</p>
            </section>
            <c:if test="${not empty reviewMessage}"><p class="journal-success" role="status"><c:out value="${reviewMessage}" /></p></c:if>
            <c:if test="${not empty reviewError}"><p class="journal-error" role="alert"><c:out value="${reviewError}" /></p></c:if>
            <section class="profit-loss-card tr-section" aria-label="仕訳帳一覧">
                <form method="get" action="${ctx}/tax-return/journal" class="journal-filters">
                    <label class="tr-field"><span>対象年度</span>
                        <select name="year"><c:forEach var="opt" items="${review.yearOptions}">
                            <option value="${opt.year}" <c:if test="${opt.year == review.year}">selected</c:if>><c:out value="${opt.label}" /></option>
                        </c:forEach></select>
                    </label>
                    <label class="tr-field"><span>確認状態</span>
                        <select name="status">
                            <option value="all" ${review.status == 'all' ? 'selected' : ''}>すべて</option>
                            <option value="draft" ${review.status == 'draft' ? 'selected' : ''}>未確認</option>
                            <option value="reviewed" ${review.status == 'reviewed' ? 'selected' : ''}>確認済み</option>
                        </select>
                    </label>
                    <button type="submit" class="tr-btn tr-btn-outline">表示</button>
                </form>
                <p id="journal-count">未確認 <strong><c:out value="${review.draftCount}" /></strong>件 ／ 表示条件に一致 <c:out value="${review.totalCount}" />件（1ページ20件）</p>
                <p class="tr-desc">「確認済み」は内容の確認記録であり、年度の締めや申告の提出ではありません。収支を修正すると該当の仕訳は未確認に戻ります。決算整理・固定資産などの自動生成仕訳も、再計算で内容が変わると再確認が必要です。</p>
                <c:if test="${review.closed}"><p class="journal-error">締め済み年度のため閲覧のみです。変更する場合は確定申告画面から理由を付けて締めを取消してください。</p></c:if>
                <c:if test="${review.missingCount > 0}"><p class="journal-error" role="alert">仕訳が未生成の未確認取引が<c:out value="${review.missingCount}" />件あります。これらも確定申告画面の未確認件数に含まれます。収支の元データを確認・修正してください。</p></c:if>
                <div class="tr-table-scroll">
                    <table class="tr-table journal-review-table">
                        <thead><tr><th scope="col">日付・摘要</th><th scope="col">借貸</th><th scope="col">科目</th><th scope="col" class="num">金額（円）</th><th scope="col">明細摘要</th><th scope="col">状態・操作</th></tr></thead>
                        <c:forEach var="entry" items="${review.entries}">
                            <tbody id="journal-${entry.id}">
                                <tr class="journal-heading">
                                    <th scope="rowgroup">
                                        <c:out value="${entry.date}" /><br><c:out value="${entry.summary}" />
                                        <br><small>仕訳番号 <c:out value="${entry.id}" /></small>
                                    </th>
                                    <td colspan="4"></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${entry.reviewed}"><strong class="journal-success">確認済み</strong></c:when>
                                            <c:otherwise><strong>未確認</strong></c:otherwise>
                                        </c:choose>
                                        <c:if test="${not empty entry.error}"><p class="journal-error"><c:out value="${entry.error}" /></p></c:if>
                                        <c:if test="${not review.closed and not entry.reviewed and empty entry.error}">
                                            <form method="post" action="${ctx}/tax-return/journal/confirm" class="journal-confirm">
                                                <input type="hidden" name="year" value="${review.year}">
                                                <input type="hidden" name="status" value="<c:out value='${review.status}' />">
                                                <input type="hidden" name="page" value="${review.pageNumber}">
                                                <input type="hidden" name="id" value="${entry.id}">
                                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                                <input type="hidden" name="reviewToken" value="<c:out value='${reviewToken}' />">
                                                <label><input type="checkbox" name="confirmed" value="true" required>この仕訳の内容を確認した</label>
                                                <button type="submit" class="tr-btn tr-btn-primary">確認済みにする</button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                                <c:forEach var="line" items="${entry.lines}">
                                    <tr><td></td><td><c:out value="${line.side == 'D' ? '借方' : (line.side == 'C' ? '貸方' : '不正')}" /></td><td><c:out value="${line.accountName}" /><br><small><c:out value="${line.accountCode}" /></small></td><td class="num"><fmt:formatNumber value="${line.amount}" pattern="#,##0.##" /></td><td><c:out value="${line.memo}" /></td><td></td></tr>
                                </c:forEach>
                            </tbody>
                        </c:forEach>
                        <c:if test="${empty review.entries}"><tbody><tr><td colspan="6" class="empty">表示条件に一致する仕訳はありません。</td></tr></tbody></c:if>
                    </table>
                </div>
                <nav class="journal-pagination" aria-label="仕訳帳のページ切替">
                    <c:if test="${review.pageNumber > 0}"><a class="tr-btn tr-btn-outline" href="${ctx}/tax-return/journal?year=${review.year}&amp;status=${review.status}&amp;page=${review.pageNumber - 1}">前の20件</a></c:if>
                    <span><c:out value="${review.totalPages == 0 ? 0 : review.pageNumber + 1}" /> / <c:out value="${review.totalPages}" />ページ</span>
                    <c:if test="${review.pageNumber + 1 < review.totalPages}"><a class="tr-btn tr-btn-outline" href="${ctx}/tax-return/journal?year=${review.year}&amp;status=${review.status}&amp;page=${review.pageNumber + 1}">次の20件</a></c:if>
                </nav>
                <a class="tr-btn tr-btn-outline" href="${ctx}/tax-return?year=${review.year}#section-journal">確定申告画面に戻る</a>
            </section>
        </div>
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>
