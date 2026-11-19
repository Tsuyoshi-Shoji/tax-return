<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="ja_JP" scope="page" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>仕訳帳 | マネタックス</title>
    <link rel="stylesheet" href="${ctx}/css/style.css?v=20261016a">
    <link rel="stylesheet" href="${ctx}/css/home/home.css?v=20261120b">
    <link rel="stylesheet" href="${ctx}/css/profit-loss/profit-loss.css?v=20261120b">
    <link rel="stylesheet" href="${ctx}/css/tax-return/tax-return.css?v=20261120b">
    <link rel="stylesheet" href="${ctx}/css/tax-return/journal-review.css?v=20261120c">
    <script src="${ctx}/css/tax-return/journal-review.js?v=20261120a" defer></script>
</head>
<body class="home-page profit-loss-page tax-return-page journal-review-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main profit-loss-main" tabindex="-1">
        <div class="profit-loss-container">
            <nav class="profit-loss-breadcrumb" aria-label="パンくず">
                <ol><li><a href="${ctx}/home">ホーム</a></li><li><a href="${ctx}/tax-return?year=${review.year}">確定申告</a></li><li aria-current="page">仕訳帳</li></ol>
            </nav>
            <section class="profit-loss-title-area" aria-labelledby="page-title">
                <h1 id="page-title">仕訳帳</h1>
                <p>仕訳の確認・生成、カード引き落としの登録・支払日の修正を行います。日付・摘要・借方・貸方・金額を証憑と照合してください。</p>
            </section>
            <c:if test="${not empty reviewMessage}"><p class="journal-success" role="status"><c:out value="${reviewMessage}" /></p></c:if>
            <c:if test="${not empty reviewError}"><p class="journal-error" role="alert"><c:out value="${reviewError}" /></p></c:if>
            <c:if test="${not empty settlementErrors}">
                <ul class="tr-errors" role="alert"><c:forEach var="error" items="${settlementErrors}"><li><c:out value="${error}" /></li></c:forEach></ul>
            </c:if>
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
                <c:if test="${review.missingCount > 0}">
                    <section class="journal-missing" aria-labelledby="journal-missing-title">
                        <h2 id="journal-missing-title">仕訳が未生成の未確認取引（<c:out value="${review.missingCount}" />件）</h2>
                        <p class="journal-error" role="alert">これらも確定申告画面の未確認件数に含まれます。収支の元データを確認して編集するか、元データの内容のまま仕訳を生成してください。生成した仕訳は未確認として一覧に追加されます。</p>
                        <c:if test="${review.missingCount > fn:length(review.missingTransactions)}"><p class="tr-desc">先頭<c:out value="${fn:length(review.missingTransactions)}" />件を表示しています。処理すると残りが表示されます。</p></c:if>
                        <div class="tr-table-scroll">
                            <table class="tr-table journal-missing-table">
                                <thead><tr><th scope="col">日付</th><th scope="col">摘要</th><th scope="col">区分</th><th scope="col" class="num">金額（円）</th><th scope="col">操作</th></tr></thead>
                                <tbody>
                                <c:forEach var="missing" items="${review.missingTransactions}">
                                    <tr id="missing-${missing.transactionId}">
                                        <td><c:out value="${missing.date}" /></td>
                                        <td><c:out value="${missing.summary}" /><br><small>取引番号 <c:out value="${missing.transactionId}" /></small></td>
                                        <td><c:out value="${missing.kind == 'income' ? '売上（収入）' : (missing.kind == 'expense' ? '経費' : '不明')}" /></td>
                                        <td class="num"><fmt:formatNumber value="${missing.amount}" pattern="#,##0.##" /></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${missing.generatable}">
                                                    <a class="tr-btn tr-btn-outline" href="${ctx}/${missing.kind}?editId=${missing.entryId}">元データを編集</a>
                                                    <c:if test="${not review.closed}">
                                                        <form method="post" action="${ctx}/tax-return/journal/generate" class="journal-generate">
                                                            <input type="hidden" name="year" value="${review.year}">
                                                            <input type="hidden" name="status" value="<c:out value='${review.status}' />">
                                                            <input type="hidden" name="page" value="${review.pageNumber}">
                                                            <input type="hidden" name="transactionId" value="${missing.transactionId}">
                                                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                                            <button type="submit" class="tr-btn tr-btn-primary">仕訳を生成</button>
                                                        </form>
                                                    </c:if>
                                                </c:when>
                                                <c:otherwise><span class="journal-error">元データなし（自動生成できません）</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </section>
                </c:if>
                <c:if test="${not review.closed and review.hasConfirmable}">
                    <form id="journal-bulk-form" method="post" action="${ctx}/tax-return/journal/confirm-bulk" class="journal-bulk">
                        <input type="hidden" name="year" value="${review.year}">
                        <input type="hidden" name="status" value="<c:out value='${review.status}' />">
                        <input type="hidden" name="page" value="${review.pageNumber}">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                        <input type="hidden" name="reviewToken" value="<c:out value='${reviewToken}' />">
                        <label><input type="checkbox" id="journal-select-all">このページの未確認仕訳をすべて選択</label>
                        <span id="journal-selected-count" aria-live="polite">0件選択中</span>
                        <label><input type="checkbox" name="confirmed" value="true" required>選択した仕訳の内容をすべて確認した</label>
                        <button type="submit" class="tr-btn tr-btn-primary">選択した仕訳を確認済みにする</button>
                    </form>
                </c:if>
                <div class="tr-table-scroll journal-review-scroll">
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
                                        <c:if test="${entry.sourceType == 'CARD_SETTLEMENT' and not review.closed}"><p><a href="#heading-card-settlement-registered">引き落とし日を修正</a></p></c:if>
                                        <c:if test="${not review.closed and entry.confirmable}">
                                            <label class="journal-select-label"><input type="checkbox" name="ids" value="${entry.id}" form="journal-bulk-form" class="journal-select">一括確認に含める</label>
                                        </c:if>
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
            </section>
            <section id="section-card-settlement" class="profit-loss-card tr-section" aria-labelledby="heading-card-settlement">
                <h2 id="heading-card-settlement">クレジットカード引き落とし登録</h2>
                <p class="tr-desc">クレジットカード利用で計上された未払金を、引き落とし時点で消し込む仕訳を登録します。</p>
                <c:if test="${review.closed}">
                    <p class="tr-desc">締め済み年度のため登録できません。</p>
                </c:if>
                <c:if test="${empty cardSettlement.candidates}">
                    <p class="tr-desc">未消込のクレジットカード取引はありません。</p>
                </c:if>
                <c:if test="${not review.closed and not empty cardSettlement.candidates}">
                    <form id="card-settlement-form" method="post" action="${ctx}/tax-return/journal/card-settlement" novalidate>
                        <input type="hidden" name="year" value="${review.year}" />
                        <input type="hidden" name="status" value="<c:out value='${review.status}' />" />
                        <input type="hidden" name="page" value="${review.pageNumber}" />
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <input type="hidden" name="idempotencyKey" value="<c:out value='${cardSettlement.idempotencyKey}' />" />
                        <div class="tr-settlement">
                            <label class="tr-field"><span>引き落とし日</span><input type="date" name="settlementDate" value="<c:out value='${cardSettlement.defaultSettlementDate}' />" required /></label>
                            <label class="tr-field"><span>引き落とし口座</span>
                                <select name="accountCode" required>
                                    <c:forEach var="account" items="${cardSettlement.accountOptions}">
                                        <option value="<c:out value='${account.code}' />"><c:out value="${account.name}" />（<c:out value="${account.code}" />）</option>
                                    </c:forEach>
                                </select>
                            </label>
                            <label class="tr-field"><span>手数料（円）</span><input name="feeAmount" inputmode="decimal" value="0" /></label>
                            <label class="tr-field"><span>メモ</span><input name="memo" maxlength="255" /></label>
                        </div>
                        <div class="tr-table-scroll">
                            <table class="tr-table">
                                <thead><tr><th scope="col">登録対象</th><th scope="col">取引日</th><th scope="col">摘要</th><th scope="col" class="num">取引金額（円）</th><th scope="col" class="num">消込済（円）</th><th scope="col" class="num">未決済残高（円）</th></tr></thead>
                                <tbody>
                                    <c:forEach var="candidate" items="${cardSettlement.candidates}">
                                        <tr>
                                            <td><label><input type="checkbox" name="sourceTransactionIds" value="${candidate.transactionId}" />選択</label></td>
                                            <td><c:out value="${candidate.transactionDate}" /></td>
                                            <td><c:out value="${candidate.summary}" /></td>
                                            <td class="num"><fmt:formatNumber value="${candidate.totalAmount}" pattern="#,##0.##" /></td>
                                            <td class="num"><fmt:formatNumber value="${candidate.settledAmount}" pattern="#,##0.##" /></td>
                                            <td class="num"><fmt:formatNumber value="${candidate.outstandingAmount}" pattern="#,##0.##" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                        <div class="tr-actions"><button type="submit" class="tr-btn tr-btn-primary">選択した取引を一括で引き落とし登録</button></div>
                    </form>
                </c:if>
                <c:if test="${not empty cardSettlement.registered}">
                    <h3 id="heading-card-settlement-registered">登録済みの引き落とし（支払日の修正）</h3>
                    <p class="tr-desc">引き落とし日を誤って登録した場合は修正できます。修正した引き落とし仕訳は未確認に戻るため、この画面の仕訳一覧で再確認してください。金額・対象取引を誤った場合は、該当の収支を修正してください。</p>
                    <div class="tr-table-scroll">
                        <table class="tr-table" aria-labelledby="heading-card-settlement-registered">
                            <thead><tr><th scope="col">引き落とし日</th><th scope="col">摘要</th><th scope="col" class="num">金額（円）</th><th scope="col">仕訳の状態</th><th scope="col">支払日の修正</th></tr></thead>
                            <tbody>
                                <c:forEach var="settled" items="${cardSettlement.registered}">
                                    <tr id="card-settlement-${settled.transactionId}">
                                        <td><c:out value="${settled.settlementDate}" /></td>
                                        <td><c:out value="${settled.summary}" /></td>
                                        <td class="num"><fmt:formatNumber value="${settled.amount}" pattern="#,##0.##" /></td>
                                        <td><c:out value="${settled.reviewed ? '確認済み' : '未確認'}" /></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${review.closed}">締め済みのため修正不可</c:when>
                                                <c:otherwise>
                                                    <form method="post" action="${ctx}/tax-return/journal/card-settlement/date" class="tr-settlement-date">
                                                        <input type="hidden" name="year" value="${review.year}" />
                                                        <input type="hidden" name="status" value="<c:out value='${review.status}' />" />
                                                        <input type="hidden" name="page" value="${review.pageNumber}" />
                                                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                                        <input type="hidden" name="settlementTransactionId" value="${settled.transactionId}" />
                                                        <input type="hidden" name="currentDate" value="<c:out value='${settled.settlementDate}' />" />
                                                        <label><span class="visually-hidden">新しい引き落とし日</span><input type="date" name="settlementDate" value="<c:out value='${settled.settlementDate}' />" min="${review.year}-01-01" max="${review.year}-12-31" required /></label>
                                                        <button type="submit" class="tr-btn tr-btn-outline">修正</button>
                                                    </form>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:if>
            </section>
            <div class="tr-actions">
                <a class="tr-btn tr-btn-outline" href="${ctx}/tax-return?year=${review.year}#section-journal">確定申告画面に戻る</a>
            </div>
        </div>
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>
