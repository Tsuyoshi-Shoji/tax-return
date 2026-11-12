<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="ja_JP" scope="page" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>確定申告 | マネタックス</title>
    <link rel="stylesheet" href="${ctx}/css/style.css?v=20261016a">
    <link rel="stylesheet" href="${ctx}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${ctx}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${ctx}/css/tax-return/tax-return.css?v=20261108a">
    <script src="${ctx}/css/tax-return/tax-return.js?v=20261101b" defer></script>
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
                    <span id="badge-status" class="tr-status ${page.closed ? 'is-closed' : 'is-draft'}">${page.closed ? '確定済' : '作成中'}</span>
                    <button id="button-error-check" type="button" class="tr-btn tr-btn-outline">エラーチェック</button>
                    <button id="button-pdf-output" type="button" class="tr-btn tr-btn-primary">PDF出力</button>
                </div>
            </div>

            <section id="section-deduction-requirements" class="profit-loss-card tr-section" aria-labelledby="heading-deduction-requirements">
                <h2 id="heading-deduction-requirements">青色申告特別控除の区分・要件確認</h2>
                <p class="tr-desc">控除区分は利用者が選択し、対象年分の要件を確認してください。この選択は適用可否の判定ではありません。</p>
                <form id="deduction-form" method="post" action="${ctx}/tax-return/filing-type?year=${page.selectedYear}">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                    <fieldset class="tr-deduction-fields" <c:if test="${page.closed}">disabled</c:if>>
                        <label class="tr-field">
                            <span>控除額</span>
                            <select id="deduction-select" name="deduction">
                                <option value="10" <c:if test="${page.deduction == 10}">selected</c:if>>青色申告特別控除 10万円</option>
                                <option value="55" <c:if test="${page.deduction == 55}">selected</c:if>>青色申告特別控除 55万円</option>
                                <option value="65" <c:if test="${page.deduction == 65}">selected</c:if>>青色申告特別控除 65万円</option>
                            </select>
                        </label>
                        <p id="deduction-description-10" class="tr-desc" data-deduction-description="10">10万円控除は、55万円・65万円控除の要件に該当しない青色申告者向けです。帳簿・申告要件は対象年分の公式資料をご確認ください。</p>
                        <p id="deduction-description-55" class="tr-desc" data-deduction-description="55">55万円控除には正規の簿記（一般に複式簿記）による記帳、貸借対照表・損益計算書等の添付と期限内提出が必要です。</p>
                        <div id="deduction-65-options" <c:if test="${page.deduction != 65}">hidden</c:if>>
                            <label class="tr-field">
                                <span>65万円控除の追加要件</span>
                                <select id="deduction-65-method" name="deduction65Method">
                                    <option value="UNCONFIRMED" <c:if test="${page.blueDeduction65Method == 'UNCONFIRMED'}">selected</c:if>>未確認</option>
                                    <option value="ETAX" <c:if test="${page.blueDeduction65Method == 'ETAX'}">selected</c:if>>e-Taxで期限内に申告書等を送信する</option>
                                    <option value="QUALIFIED_EBOOKS" <c:if test="${page.blueDeduction65Method == 'QUALIFIED_EBOOKS'}">selected</c:if>>優良な電子帳簿の要件・届出を満たす</option>
                                </select>
                            </label>
                            <p id="deduction-description-65" class="tr-desc">65万円控除は55万円控除の要件に加え、上記いずれかの追加要件が必要です。本アプリはe-Tax送信も優良な電子帳簿の要件判定も行いません。</p>
                        </div>
                        <label class="tr-deduction-confirm">
                            <input id="deduction-requirements-confirmed" type="checkbox" name="requirementsConfirmed" value="true"
                                   <c:if test="${page.blueDeductionRequirementsConfirmed}">checked</c:if> />
                            <span>対象年分の青色申告承認と、選択した控除区分の要件を公式資料等で確認しました。</span>
                        </label>
                    </fieldset>
                    <div class="tr-deduction-verification">
                        <strong>アプリで確認できる範囲</strong>
                        <p>仕訳の借貸一致、帳簿から集計した貸借対照表の一致、未確認仕訳などを検査します。これらの結果は、青色申告の承認、控除要件、電子帳簿保存法への適合、申告書の提出を証明するものではありません。</p>
                    </div>
                    <div class="tr-actions"><button id="button-save-deduction" type="submit" class="tr-btn tr-btn-primary" <c:if test="${page.closed}">disabled</c:if>>区分・確認状態を保存</button></div>
                </form>
                <c:if test="${not empty page.filingRequirements}">
                    <div id="filing-requirements" class="tr-filing-requirements">
                        <h3 id="heading-filing-requirements">必要な帳簿・決算書・提出条件（<c:out value="${page.filingRequirements.modeLabel}" />）</h3>
                        <p class="tr-desc">保存済みの申告方式・控除区分に応じて、出力前に不足と利用者確認事項を検出します。「不足」「利用者確認」がある場合、PDF・CSVの出力前に確認を求めます（白色申告など対象外の方式は出力できません）。</p>
                        <div class="tr-table-scroll">
                            <table id="table-filing-requirements" class="tr-table">
                                <thead><tr><th scope="col">区分</th><th scope="col">書類・条件</th><th scope="col">要否</th><th scope="col">状態</th><th scope="col">内容</th></tr></thead>
                                <tbody>
                                <c:forEach var="req" items="${page.filingRequirements.items}">
                                    <tr id="${req.id}" data-status="${req.status}">
                                        <td><c:out value="${req.category.label}" /></td>
                                        <th scope="row"><c:out value="${req.name}" /></th>
                                        <td><c:out value="${req.necessity.label}" /></td>
                                        <td><strong><c:out value="${req.status.label}" /></strong></td>
                                        <td><c:out value="${req.message}" /></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </c:if>
            </section>

            <c:if test="${sections.show['retention']}">
            <section id="section-retention" class="profit-loss-card tr-section" aria-labelledby="heading-retention">
                <h2 id="heading-retention">年分別の保存管理台帳</h2>
                <c:if test="${sections.forced['retention']}"><p class="tr-section-forced" role="note">設定で非表示にしていますが、この年分に登録データがあるため表示しています。</p></c:if>
                <p class="tr-desc"><c:out value="${page.yearLabel}" />の取引・帳簿・証憑について、保存起算日・期限・確認根拠を登録します<c:if test="${retention.driveStatus.enabled}">。領収書・請求書等の証憑ファイルは利用者自身の Google Drive に保管します</c:if>。書類種別を選ぶと、申告方式（<c:out value="${retention.blueFiling ? '青色申告' : '白色申告等'}" />）・年分・作成受領日に応じた保存期限をサーバーで計算します。</p>
                <div id="retention-rule-guidance" class="tr-deduction-verification" role="note">
                    <strong>保存期限ルール（確認日 <c:out value="${retention.ruleCheckedOn}" />）</strong>
                    <p>所得税法施行規則63条（青色）・102条（白色）、消費税法施行令50条1項に基づき、帳簿は年分の翌年3月16日、書類は作成・受領日の属する年の翌年3月16日（消費税は翌年3月1日）を起算日として、該当する期間のうち最も遅い日を期限とします。前々年分所得300万円以下の5年特例など期限を短くする特例は自動適用しません。この年分の最低保存期限は <strong id="retention-minimum-deadline"><c:out value="${retention.minimumDeadline}" /></strong> で、これより前の削除はできません。税務調査・更正の請求・紛争等で必要な記録は「保全」すると期限経過後も削除できなくなります。計算結果は法令適合を保証しないため、対象年分の国税庁等の公式資料で確認してください。</p>
                    <c:if test="${not retention.ruleYearSupported}"><p id="retention-rule-unsupported"><strong class="tr-retention-alert">この年分はルールの確認対象外です（<c:out value="${page.selectedYear}" />年分）。手入力で公式資料に基づく期限を登録してください。</strong></p></c:if>
                </div>
                <c:if test="${not retention.driveStatus.enabled}">
                <p id="evidence-upload-disabled" class="tr-evidence-drive">領収書・請求書等の証憑ファイル（画像・PDF）のアップロードは今後提供予定のため、現在は停止しています。原本は各自で保管し、この台帳で保存期限を管理してください。</p>
                </c:if>
                <c:if test="${retention.driveStatus.enabled}">
                <div class="tr-deduction-verification" role="note">
                    <strong>電子保存の適合判定は未実装です</strong>
                    <p>証憑ファイルは Google Drive（drive.file 権限：このアプリが作成したファイルのみ）に保管し、登録時のハッシュ値で取得時に改変を検知します。タイムスタンプ付与・訂正削除履歴・検索要件などの電子帳簿保存法への適合は判定・保証しません。ファイルのウイルス検査は行わないため、信頼できる取得元のファイルのみをアップロードしてください。</p>
                </div>
                <div id="evidence-drive-panel" class="tr-evidence-drive">
                    <c:choose>
                        <c:when test="${not retention.driveStatus.configured}">
                            <p id="evidence-drive-unconfigured">Google Drive 連携は未設定です（管理者による OAuth クライアント・暗号鍵の設定が必要です）。台帳メタデータの登録・検索・期限管理は利用できます。</p>
                        </c:when>
                        <c:when test="${retention.driveStatus.connected}">
                            <p id="evidence-drive-connected">Google Drive と連携済みです（連携日時：<c:out value="${retention.driveStatus.connectedAt}" />）。アップロードしたファイルは Drive の「確定申告アプリ 証憑」フォルダーに保存されます。Drive 上で削除・移動すると、保存期限内でも失われます。</p>
                            <form method="post" action="${ctx}/tax-return/evidence/google/disconnect?year=${page.selectedYear}" data-confirm="Google Drive 連携を解除しますか？保管済みファイルは Drive に残りますが、再連携するまでアプリから取得できません。">
                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                <button id="button-drive-disconnect" type="submit" class="tr-btn">連携を解除</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p id="evidence-drive-disconnected">証憑ファイルを保管するには、Google Drive と連携してください。要求する権限は、このアプリが作成したファイルへのアクセスのみです。</p>
                            <form method="post" action="${ctx}/tax-return/evidence/google/connect?year=${page.selectedYear}">
                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                <button id="button-drive-connect" type="submit" class="tr-btn tr-btn-primary">Google Drive と連携</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
                </c:if>
                <form id="retention-record-form" method="post" action="${ctx}/tax-return/retention?year=${page.selectedYear}">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                    <div class="tr-retention-form">
                        <label class="tr-field">
                            <span>記録区分</span>
                            <select name="recordKind" required>
                                <option value="TRANSACTION">取引</option>
                                <option value="BOOK">帳簿</option>
                                <option value="EVIDENCE">証憑</option>
                            </select>
                        </label>
                        <label class="tr-field">
                            <span>書類種別</span>
                            <input name="documentType" maxlength="100" required placeholder="例：領収書、仕訳帳" />
                        </label>
                        <label class="tr-field">
                            <span>名称</span>
                            <input name="title" maxlength="200" required />
                        </label>
                        <label class="tr-field">
                            <span>関連仕訳</span>
                            <select name="journalEntryId">
                                <option value="">帳簿全体／仕訳なし</option>
                                <c:forEach var="entry" items="${retention.journalEntries}">
                                    <option value="${entry.id}"><c:out value="${entry.entryDate}" /> — <c:out value="${entry.summary}" /></option>
                                </c:forEach>
                            </select>
                        </label>
                        <label class="tr-field">
                            <span>保存期限ルール</span>
                            <select id="retention-rule-code" name="ruleCode">
                                <c:forEach var="category" items="${retention.ruleCategories}">
                                    <option value="${category.code}" ${retention.ruleYearSupported ? '' : 'disabled'}><c:out value="${category.book ? '帳簿' : '書類'}" />：<c:out value="${category.label}" /></option>
                                </c:forEach>
                                <option value="MANUAL" ${retention.ruleYearSupported ? '' : 'selected'}>手入力（下の起算日・期限・根拠を使用）</option>
                            </select>
                        </label>
                        <label class="tr-field">
                            <span>作成・受領日（書類のみ）</span>
                            <input type="date" name="documentDate" />
                        </label>
                        <label class="tr-field tr-retention-consumption">
                            <input type="checkbox" name="consumptionTax" value="true" />
                            <span>消費税の仕入税額控除に使用（7年を確保）</span>
                        </label>
                        <label class="tr-field">
                            <span>保存起算日（手入力のみ）</span>
                            <input type="date" name="retentionStartDate" />
                        </label>
                        <label class="tr-field">
                            <span>保存期限（手入力のみ）</span>
                            <input type="date" name="retentionDeadline" />
                        </label>
                        <label class="tr-field tr-retention-basis">
                            <span>期限の根拠（手入力のみ）</span>
                            <input name="legalBasis" maxlength="500" placeholder="対象年分・資料名・該当箇所など" />
                        </label>
                    </div>
                    <div class="tr-actions"><button id="button-add-retention-record" type="submit" class="tr-btn tr-btn-primary">台帳に登録</button></div>
                </form>
                <form id="retention-search-form" method="get" action="${ctx}/tax-return#section-retention" class="tr-retention-search">
                    <input type="hidden" name="year" value="${page.selectedYear}" />
                    <label class="tr-field">
                        <span>キーワード</span>
                        <input name="rq" maxlength="100" value="<c:out value='${retention.search.keyword}' />" placeholder="書類種別・名称・根拠・仕訳摘要・ファイル名" />
                    </label>
                    <label class="tr-field">
                        <span>区分</span>
                        <select name="rkind">
                            <option value="">すべて</option>
                            <option value="TRANSACTION" ${retention.search.recordKind == 'TRANSACTION' ? 'selected' : ''}>取引</option>
                            <option value="BOOK" ${retention.search.recordKind == 'BOOK' ? 'selected' : ''}>帳簿</option>
                            <option value="EVIDENCE" ${retention.search.recordKind == 'EVIDENCE' ? 'selected' : ''}>証憑</option>
                        </select>
                    </label>
                    <label class="tr-field">
                        <span>状態</span>
                        <select name="rstatus">
                            <option value="ALL">すべて</option>
                            <option value="ACTIVE" ${retention.search.status == 'ACTIVE' ? 'selected' : ''}>保存期間中</option>
                            <option value="EXPIRED" ${retention.search.status == 'EXPIRED' ? 'selected' : ''}>保存期限経過</option>
                            <option value="DUE_SOON" ${retention.search.status == 'DUE_SOON' ? 'selected' : ''}>期限間近（<c:out value="${retention.dueSoonDays}" />日以内）</option>
                            <option value="HELD" ${retention.search.status == 'HELD' ? 'selected' : ''}>保全中</option>
                            <option value="MANUAL_RULE" ${retention.search.status == 'MANUAL_RULE' ? 'selected' : ''}>手入力（ルール未適用）</option>
                            <option value="WITH_FILE" ${retention.search.status == 'WITH_FILE' ? 'selected' : ''}>ファイルあり</option>
                            <option value="WITHOUT_FILE" ${retention.search.status == 'WITHOUT_FILE' ? 'selected' : ''}>ファイルなし</option>
                            <option value="FILE_ALERT" ${retention.search.status == 'FILE_ALERT' ? 'selected' : ''}>ファイル要確認</option>
                        </select>
                    </label>
                    <div class="tr-actions"><button id="button-search-retention" type="submit" class="tr-btn">検索</button></div>
                </form>
                <p id="retention-summary" class="tr-desc">全<c:out value="${retention.totalCount}" />件中<c:out value="${retention.records.size()}" />件を表示。保存期限経過：<c:out value="${retention.expiredCount}" />件／期限間近（<c:out value="${retention.dueSoonDays}" />日以内）：<c:out value="${retention.dueSoonCount}" />件／保全中：<c:out value="${retention.heldCount}" />件<c:if test="${retention.manualRuleCount > 0}">／手入力（ルール未適用）：<c:out value="${retention.manualRuleCount}" />件</c:if><c:if test="${retention.fileAlertCount > 0}">／<strong class="tr-retention-alert">Drive 上のファイル要確認：<c:out value="${retention.fileAlertCount}" />件</strong></c:if></p>
                <div class="tr-table-scroll">
                    <table id="table-retention-records" class="tr-table">
                        <thead><tr><th scope="col">区分</th><th scope="col">書類種別・名称</th><th scope="col">関連仕訳</th><th scope="col">起算日</th><th scope="col">保存期限・案内</th><th scope="col">ルール・根拠</th><th scope="col">証憑ファイル</th><th scope="col">操作</th></tr></thead>
                        <tbody>
                            <c:forEach var="record" items="${retention.records}">
                                <tr>
                                    <td>
                                        <c:choose>
                                            <c:when test="${record.recordKind == 'TRANSACTION'}">取引</c:when>
                                            <c:when test="${record.recordKind == 'BOOK'}">帳簿</c:when>
                                            <c:otherwise>証憑</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><c:out value="${record.documentType}" /> — <c:out value="${record.title}" /></td>
                                    <td><c:out value="${record.journalSummary}" default="年度全体" /></td>
                                    <td><c:out value="${record.retentionStartDate}" /></td>
                                    <td><c:out value="${record.retentionDeadline}" />
                                        <c:if test="${record.guidance.effectiveDeadline ne record.retentionDeadline}"><br /><small>最低保存期限 <c:out value="${record.guidance.effectiveDeadline}" /> まで削除不可</small></c:if>
                                        <br />
                                        <c:choose>
                                            <c:when test="${record.deadlineStatus == 'HELD'}"><strong class="tr-retention-alert">保全中（削除不可）</strong><br /><small><c:out value="${record.guidance.legalHoldReason}" />（<c:out value="${record.guidance.legalHoldAt}" />）</small></c:when>
                                            <c:when test="${record.deadlineStatus == 'EXPIRED'}"><span class="tr-retention-expired">期限経過（削除可能）</span></c:when>
                                            <c:when test="${record.deadlineStatus == 'DUE_SOON'}"><strong class="tr-retention-alert">期限まで残り<c:out value="${record.guidance.daysRemaining}" />日</strong></c:when>
                                            <c:otherwise><small>残り<c:out value="${record.guidance.daysRemaining}" />日</small></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><small><c:out value="${record.guidance.ruleLabel}" /><c:if test="${not empty record.guidance.documentDate}">（作成・受領 <c:out value="${record.guidance.documentDate}" />）</c:if><c:if test="${record.guidance.consumptionTaxApplies}">・消費税</c:if></small><br /><c:out value="${record.legalBasis}" /><c:if test="${not record.guidance.ruleApplied}"><br /><small>公式資料で期限を再確認してください。</small></c:if></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${record.hasFile}">
                                                <c:choose>
                                                    <c:when test="${retention.driveStatus.enabled}"><a href="${ctx}/tax-return/retention/${record.id}/file?year=${page.selectedYear}"><c:out value="${record.file.originalFileName}" /></a></c:when>
                                                    <c:otherwise><c:out value="${record.file.originalFileName}" />（保管機能停止中のため取得不可）</c:otherwise>
                                                </c:choose>
                                                <br /><small><c:out value="${record.file.sizeLabel}" />・登録 <c:out value="${record.file.uploadedAt}" /></small>
                                                <c:choose>
                                                    <c:when test="${record.file.verificationStatus == 'OK'}"><br /><small>Drive 確認済（<c:out value="${record.file.lastVerifiedAt}" />）</small></c:when>
                                                    <c:when test="${record.file.verificationStatus == 'MISSING'}"><br /><strong class="tr-retention-alert">Drive 上に見つかりません</strong></c:when>
                                                    <c:when test="${record.file.verificationStatus == 'TRASHED'}"><br /><strong class="tr-retention-alert">Drive のゴミ箱にあります</strong></c:when>
                                                    <c:when test="${record.file.verificationStatus == 'MISMATCH'}"><br /><strong class="tr-retention-alert">登録時と内容が不一致</strong></c:when>
                                                </c:choose>
                                            </c:when>
                                            <c:when test="${retention.driveStatus.connected}">
                                                <form method="post" enctype="multipart/form-data" action="${ctx}/tax-return/retention/${record.id}/file?year=${page.selectedYear}" class="tr-evidence-upload">
                                                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                                    <input type="file" name="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" required aria-label="証憑ファイル（PDF・JPEG・PNG、10MB以下）" />
                                                    <button type="submit" class="tr-btn">アップロード</button>
                                                </form>
                                            </c:when>
                                            <c:otherwise>なし</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:if test="${record.hasFile and retention.driveStatus.connected}">
                                            <form method="post" action="${ctx}/tax-return/retention/${record.id}/verify?year=${page.selectedYear}">
                                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                                <button type="submit" class="tr-btn">Drive 確認</button>
                                            </form>
                                        </c:if>
                                        <c:choose>
                                            <c:when test="${record.guidance.legalHold}">
                                                <form method="post" action="${ctx}/tax-return/retention/${record.id}/hold/release?year=${page.selectedYear}" data-confirm="保全を解除しますか？解除後、保存期限を経過していれば削除できるようになります。">
                                                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                                    <button type="submit" class="tr-btn">保全を解除</button>
                                                </form>
                                            </c:when>
                                            <c:otherwise>
                                                <form method="post" action="${ctx}/tax-return/retention/${record.id}/hold?year=${page.selectedYear}" class="tr-retention-hold">
                                                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                                    <input name="reason" maxlength="200" required aria-label="保全の理由" placeholder="保全理由（例：税務調査対応）" />
                                                    <button type="submit" class="tr-btn">保全</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                        <c:choose>
                                            <c:when test="${record.deletable}">
                                                <form method="post" action="${ctx}/tax-return/retention/${record.id}/delete?year=${page.selectedYear}" data-confirm="保存期限を経過した台帳記録<c:if test="${record.hasFile}">と Google Drive 上の証憑ファイル</c:if>を削除します。元に戻せません。">
                                                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                                    <button type="submit" class="tr-btn tr-btn-danger">削除</button>
                                                </form>
                                            </c:when>
                                            <c:when test="${record.guidance.legalHold}"><small>保全中は削除不可</small></c:when>
                                            <c:otherwise><small>期限経過後に削除可</small></c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty retention.records}">
                                <tr><td colspan="8" class="empty"><c:choose><c:when test="${retention.search.filtered}">検索条件に一致する記録はありません。</c:when><c:otherwise>この年分の保存管理台帳は未登録です。</c:otherwise></c:choose></td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
            </c:if>

            <nav class="tr-tabs" aria-label="確定申告セクション">
                <a id="tab-basic-info" class="tr-tab is-active" href="#section-basic-info">基本情報</a>
                <a id="tab-annual-summary" class="tr-tab" href="#section-annual-summary">年間収支</a>
                <a id="tab-monthly-sales" class="tr-tab" href="#section-monthly">月別売上</a>
                <a id="tab-profit-loss" class="tr-tab" href="#section-profit-loss">損益計算書</a>
                <a id="tab-settlement" class="tr-tab" href="#section-settlement">決算整理</a>
                <a id="tab-blue-statement" class="tr-tab" href="#section-blue-statement">決算書様式</a>
                <c:if test="${sections.show['fixed-assets']}"><a id="tab-fixed-assets" class="tr-tab" href="#section-fixed-assets">固定資産</a></c:if>
                <a id="tab-balance-sheet" class="tr-tab" href="#section-balance-sheet">貸借対照表</a>
                <a id="tab-journal" class="tr-tab" href="#section-journal">仕訳帳</a>
                <a id="tab-deductions" class="tr-tab" href="#section-deductions">所得控除</a>
                <a id="tab-tax" class="tr-tab" href="#section-tax">税額計算</a>
                <c:if test="${sections.show['consumption']}"><a id="tab-consumption" class="tr-tab" href="#section-consumption">消費税</a></c:if>
                <a id="tab-output" class="tr-tab" href="#section-output">出力</a>
                <a id="tab-closing" class="tr-tab" href="#section-closing">締め・履歴</a>
                <a id="link-section-settings" class="tr-tabs-settings-link" href="${ctx}/settings#tax-return-sections">表示セクションの設定</a>
            </nav>

            <section id="section-basic-info" class="profit-loss-card tr-section" aria-labelledby="heading-basic-info">
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

            <section id="section-annual-summary" class="profit-loss-card tr-section print-section" data-print-groups="profit-loss" aria-labelledby="heading-annual-summary">
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

            <section id="section-monthly" class="profit-loss-card tr-section" aria-labelledby="heading-annual-revenue-breakdown">
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
                <section id="section-profit-loss" class="profit-loss-card tr-section print-section" data-print-groups="profit-loss" aria-labelledby="heading-profit-loss-statement">
                    <h2 id="heading-profit-loss-statement">損益計算書</h2>
                    <p id="text-profit-loss-description" class="tr-desc"><c:out value="${page.yearLabel}" />の損益計算書の勘定科目別内訳です。</p>
                    <div class="tr-table-scroll">
                        <table id="table-profit-loss-statement" class="tr-table tr-pl">
                            <thead><tr><th scope="col">勘定科目</th><th scope="col" class="num">金額（円）</th><th scope="col"><span class="visually-hidden">内訳</span></th></tr></thead>
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
                    </div>
                </section>

                <section id="section-settlement" class="profit-loss-card tr-section" aria-labelledby="heading-settlement-management">
                    <h2 id="heading-settlement-management">決算整理</h2>
                    <p id="text-settlement-description" class="tr-desc">年末の決算整理に関する項目を入力してください。売掛金・未収入金・買掛金・未払金は12月31日時点の残高を入力すると、帳簿残高との差額を普通預金で決済したものとして決算整理仕訳を作成します。</p>
                    <form id="settlement-form" method="post" action="${ctx}/tax-return/settlement?year=${page.selectedYear}" novalidate>
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <c:set var="s" value="${page.settlement}" />
                        <div class="tr-settlement-scroll">
                            <fieldset class="tr-settlement" <c:if test="${page.closed}">disabled</c:if>>
                                <label class="tr-field"><span>期首棚卸（円）</span><input name="openingInventory" inputmode="numeric" autocomplete="off" value="<c:out value='${s.openingInventory}' />" /></label>
                                <label class="tr-field"><span>期末棚卸（円）</span><input name="closingInventory" inputmode="numeric" autocomplete="off" value="<c:out value='${s.closingInventory}' />" /></label>
                                <label class="tr-field"><span>減価償却費（円）</span><output id="display-register-depreciation" class="tr-field-output"><fmt:formatNumber value="${page.extras.depreciationBusinessTotal}" pattern="#,##0" />（固定資産台帳から自動計算）</output></label>
                                <label class="tr-field"><span>売掛金（円）</span><input name="accountsReceivable" inputmode="numeric" autocomplete="off" value="<c:out value='${s.accountsReceivable}' />" /></label>
                                <label class="tr-field"><span>買掛金（円）</span><input name="accountsPayable" inputmode="numeric" autocomplete="off" value="<c:out value='${s.accountsPayable}' />" /></label>
                                <label class="tr-field"><span>未払金（円）</span><input name="unpaidExpenses" inputmode="numeric" autocomplete="off" value="<c:out value='${s.unpaidExpenses}' />" /></label>
                                <label class="tr-field"><span>未収入金（円）</span><input name="unreceivedIncome" inputmode="numeric" autocomplete="off" value="<c:out value='${s.unreceivedIncome}' />" /></label>
                                <label class="tr-field"><span>期首残高（円）</span><input name="openingBalance" inputmode="numeric" autocomplete="off" value="<c:out value='${s.openingBalance}' />" <c:if test="${page.balanceCarriedForward}">disabled title="前年の期末残高を繰り越すため入力不要です"</c:if> /></label>
                                <label class="tr-field"><span>期末現預金（円）</span><input name="closingCash" inputmode="numeric" autocomplete="off" value="<c:out value='${s.closingCash}' />" /></label>
                            </fieldset>
                        </div>
                        <div class="tr-actions"><button id="button-save-settlement" type="submit" class="tr-btn tr-btn-primary" <c:if test="${page.closed}">disabled</c:if>>保存する</button></div>
                    </form>
                </section>
            </div>

            <section id="section-blue-statement" class="profit-loss-card tr-section print-section" data-print-groups="profit-loss" aria-labelledby="heading-blue-statement">
                <h2 id="heading-blue-statement">青色申告決算書（損益計算書）</h2>
                <p id="text-blue-statement-description" class="tr-desc">勘定科目を青色申告決算書（一般用）1ページ目の各欄へ集約した金額です。欄番号・様式は提出前に国税庁の最新様式でご確認ください。</p>
                <c:choose>
                    <c:when test="${empty page.statementRows}">
                        <p class="tr-desc">対象年度の決算書様式が登録されていません。</p>
                    </c:when>
                    <c:otherwise>
                        <div class="tr-statement-grid">
                            <div class="tr-table-scroll">
                                <table id="table-blue-statement-main" class="tr-table tr-statement">
                                    <thead><tr><th scope="col" class="no">欄</th><th scope="col">科目</th><th scope="col" class="num">金額（円）</th><th scope="col">集計元</th></tr></thead>
                                    <tbody>
                                        <c:forEach var="r" items="${page.statementMainRows}">
                                            <tr id="statement-line-${r.number}" class="${r.total ? 'is-total' : (r.calculated ? 'is-calc' : '')}">
                                                <td class="no"><c:out value="${r.numberLabel}" /></td>
                                                <th scope="row"><c:out value="${r.label}" /></th>
                                                <td class="num ${r.amount.signum() < 0 ? 'is-negative' : ''}"><fmt:formatNumber value="${r.amount}" pattern="#,##0" /></td>
                                                <td class="src"><c:out value="${r.accounts}" /></td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                            <div class="tr-table-scroll">
                                <table id="table-blue-statement-income" class="tr-table tr-statement">
                                    <thead><tr><th scope="col" class="no">欄</th><th scope="col">科目</th><th scope="col" class="num">金額（円）</th><th scope="col">集計元</th></tr></thead>
                                    <tbody>
                                        <c:forEach var="r" items="${page.statementIncomeRows}">
                                            <tr id="statement-line-${r.number}" class="${r.total ? 'is-total' : (r.calculated ? 'is-calc' : '')}">
                                                <td class="no"><c:out value="${r.numberLabel}" /></td>
                                                <th scope="row"><c:out value="${r.label}" /></th>
                                                <td class="num ${r.amount.signum() < 0 ? 'is-negative' : ''}"><fmt:formatNumber value="${r.amount}" pattern="#,##0" /></td>
                                                <td class="src"><c:out value="${r.accounts}" /></td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <c:if test="${sections.show['fixed-assets']}">
            <%@ include file="/WEB-INF/views/TaxReturn/_fixed-assets.jspf" %>
            </c:if>

            <c:if test="${sections.show['household-consumption']}">
            <section id="section-household-consumption" class="profit-loss-card tr-section" aria-labelledby="heading-household-consumption">
                <h2 id="heading-household-consumption">家事消費等</h2>
                <c:if test="${sections.forced['household-consumption']}"><p class="tr-section-forced" role="note">設定で非表示にしていますが、この年分に登録データがあるため表示しています。</p></c:if>
                <p class="tr-desc">棚卸資産を家事用に消費・贈与した場合等の、利用者が入力した年分別金額です。登録内容は売上高に加算して仕訳・決算書へ反映します。金額の税務上の評価は利用者が確認してください。</p>
                <div class="tr-table-scroll">
                    <table id="table-household-consumption" class="tr-table tr-statement">
                        <thead><tr><th>取引日</th><th>内容</th><th class="num">金額（円）</th><th><span class="visually-hidden">操作</span></th></tr></thead>
                        <tbody>
                            <c:forEach var="h" items="${page.extras.householdConsumptionRows}">
                                <tr><td><c:out value="${h.date}" /></td><th><c:out value="${h.details}" /></th><td class="num"><fmt:formatNumber value="${h.amount}" pattern="#,##0" /></td>
                                    <td><c:if test="${h.deletable}"><form method="post" action="${ctx}/tax-return/household-consumption/delete?year=${page.selectedYear}" class="tr-inline-form" data-confirm="この家事消費等を削除しますか？">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" /><input type="hidden" name="id" value="${h.id}" />
                                        <button type="submit" class="tr-btn tr-btn-soft tr-btn-small">削除</button>
                                    </form></c:if></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty page.extras.householdConsumptionRows}"><tr><td colspan="4" class="empty">登録されている明細はありません。</td></tr></c:if>
                        </tbody>
                        <tfoot><tr class="is-total"><th scope="row" colspan="2">年計</th><td class="num"><fmt:formatNumber value="${page.extras.householdConsumptionTotal}" pattern="#,##0" /></td><td></td></tr></tfoot>
                    </table>
                </div>
                <c:if test="${not page.closed}">
                    <form id="household-consumption-form" method="post" action="${ctx}/tax-return/household-consumption?year=${page.selectedYear}" novalidate>
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <div class="tr-settlement">
                            <label class="tr-field"><span>取引日</span><input type="date" name="transactionDate" required /></label>
                            <label class="tr-field"><span>内容</span><input name="details" maxlength="100" required /></label>
                            <label class="tr-field"><span>金額（円）</span><input name="amount" inputmode="numeric" required /></label>
                        </div>
                        <div class="tr-actions"><button type="submit" class="tr-btn tr-btn-primary">家事消費等を登録</button></div>
                    </form>
                </c:if>
            </section>
            </c:if>

            <c:if test="${sections.show['salary-details']}">
            <section id="section-salary-details" class="profit-loss-card tr-section" aria-labelledby="heading-salary-details">
                <h2 id="heading-salary-details">給料賃金・専従者給与の個人別内訳</h2>
                <c:if test="${sections.forced['salary-details']}"><p class="tr-section-forced" role="note">設定で非表示にしていますが、この年分に登録データがあるため表示しています。</p></c:if>
                <p class="tr-desc">個人別内訳の登録・出力と、給与仕訳の科目別合計との照合を行います。青色事業専従者の届出・要件や給与額の適正性は本機能では判定しません。</p>
                <div class="tr-table-scroll">
                    <table id="table-salary-details" class="tr-table tr-statement">
                        <thead><tr><th>区分</th><th>氏名</th><th>職務</th><th>従事期間</th><th>支給期</th><th class="num">支給額（円）</th><th><span class="visually-hidden">操作</span></th></tr></thead>
                        <tbody>
                            <c:forEach var="p" items="${page.extras.employeeSalaryRows}">
                                <tr><td><c:out value="${p.salaryKindLabel}" /></td><th><c:out value="${p.personName}" /></th><td><c:out value="${p.jobDescription}" /></td>
                                    <td><c:out value="${p.workPeriodStart}" />〜<c:out value="${p.workPeriodEnd}" /></td><td><c:out value="${p.paymentPeriod}" /></td>
                                    <td class="num"><fmt:formatNumber value="${p.amount}" pattern="#,##0" /></td>
                                    <td><c:if test="${p.deletable}"><form method="post" action="${ctx}/tax-return/salary-details/delete?year=${page.selectedYear}" class="tr-inline-form" data-confirm="この給与内訳を削除しますか？">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" /><input type="hidden" name="id" value="${p.id}" />
                                        <button type="submit" class="tr-btn tr-btn-soft tr-btn-small">削除</button>
                                    </form></c:if></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty page.extras.employeeSalaryRows}"><tr><td colspan="7" class="empty">登録されている個人別内訳はありません。</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${not page.closed}">
                    <form id="employee-salary-form" method="post" action="${ctx}/tax-return/salary-details?year=${page.selectedYear}" novalidate>
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <div class="tr-settlement">
                            <label class="tr-field"><span>区分</span><select name="salaryKind"><option value="SALARIES">給料賃金</option><option value="DEDICATED_SALARY">青色事業専従者給与</option></select></label>
                            <label class="tr-field"><span>氏名</span><input name="personName" maxlength="100" required /></label>
                            <label class="tr-field"><span>職務</span><input name="jobDescription" maxlength="100" required /></label>
                            <label class="tr-field"><span>従事期間（開始）</span><input type="date" name="workPeriodStart" required /></label>
                            <label class="tr-field"><span>従事期間（終了）</span><input type="date" name="workPeriodEnd" required /></label>
                            <label class="tr-field"><span>支給期</span><input name="paymentPeriod" maxlength="30" placeholder="例：毎月末日" required /></label>
                            <label class="tr-field"><span>年分支給額（円）</span><input name="amount" inputmode="numeric" required /></label>
                        </div>
                        <div class="tr-actions"><button type="submit" class="tr-btn tr-btn-primary">個人別内訳を登録</button></div>
                    </form>
                </c:if>
            </section>
            </c:if>

            <section id="section-balance-sheet" class="profit-loss-card tr-section print-section" data-print-groups="balance-sheet" aria-labelledby="heading-balance-sheet">
                <h2 id="heading-balance-sheet">貸借対照表</h2>
                <p id="text-balance-sheet-description" class="tr-desc"><c:out value="${page.yearLabel}" />12月31日時点の貸借対照表です。仕訳の残高から作成し、<c:choose><c:when test="${page.balanceCarriedForward}">期首残高は前年の期末残高を繰り越しています（元入金＝期首の資産合計−負債合計）。</c:when><c:otherwise>期首残高は決算整理の「期首残高（普通預金）」「期首棚卸」と固定資産台帳から作成しています。</c:otherwise></c:choose></p>
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
                <p class="screen-only"><a id="button-journal-review" class="tr-btn tr-btn-primary" href="${ctx}/tax-return/journal?year=${page.selectedYear}">仕訳帳の全件表示・確認</a></p>
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

            <%@ include file="/WEB-INF/views/TaxReturn/_deductions.jspf" %>
            <%@ include file="/WEB-INF/views/TaxReturn/_tax-calculation.jspf" %>
            <c:if test="${sections.show['consumption']}">
            <%@ include file="/WEB-INF/views/TaxReturn/_consumption.jspf" %>
            </c:if>
            <%@ include file="/WEB-INF/views/TaxReturn/_blue-return-print.jspf" %>

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
                <p id="text-output-description" class="tr-desc">作成したデータをサーバーでPDF化してダウンロードできます。帳票PDFを一括ダウンロードすることもできます。</p>
                <div id="button-pdf-output-group" class="tr-output">
                    <a id="button-pdf-return" class="tr-btn tr-btn-outline" href="${ctx}/tax-return/pdf/return?year=${page.selectedYear}" data-export="pdf" data-confirm-warning="true">青色申告決算書PDF</a>
                    <a id="button-pdf-balance-sheet" class="tr-btn tr-btn-outline" href="${ctx}/tax-return/pdf/balance-sheet?year=${page.selectedYear}" data-export="pdf" data-confirm-warning="true">貸借対照表PDF</a>
                    <a id="button-pdf-profit-loss" class="tr-btn tr-btn-outline" href="${ctx}/tax-return/pdf/profit-loss?year=${page.selectedYear}" data-export="pdf" data-confirm-warning="true">損益計算書PDF</a>
                    <a id="button-pdf-journal" class="tr-btn tr-btn-outline" href="${ctx}/tax-return/pdf/journal?year=${page.selectedYear}" data-export="pdf" data-confirm-warning="true">仕訳帳PDF</a>
                    <a id="button-pdf-ledger" class="tr-btn tr-btn-outline" href="${ctx}/tax-return/pdf/ledger?year=${page.selectedYear}" data-export="pdf" data-confirm-warning="true">総勘定元帳PDF</a>
                    <c:if test="${not empty page.extras.tax}"><a id="button-pdf-final-return" class="tr-btn tr-btn-outline" href="${ctx}/tax-return/pdf/final-return?year=${page.selectedYear}" data-export="pdf" data-confirm-warning="true">確定申告書（第一表・第二表）PDF</a></c:if>
                    <c:if test="${not empty page.extras.consumption}"><a id="button-pdf-consumption" class="tr-btn tr-btn-outline" href="${ctx}/tax-return/pdf/consumption?year=${page.selectedYear}" data-export="pdf" data-confirm-warning="true">消費税計算書PDF</a></c:if>
                    <a id="button-pdf-all" class="tr-btn tr-btn-primary" href="${ctx}/tax-return/export/pdfs.zip?year=${page.selectedYear}" data-export="pdf" data-confirm-warning="true">帳票PDF一括ダウンロード（ZIP）</a>
                    <a id="button-export-etax-csv" class="tr-btn tr-btn-outline" href="${ctx}/tax-return/export/etax-csv?year=${page.selectedYear}" data-export="etax-csv">e-Tax入力用CSV</a>
                </div>
                <form id="books-export-form" method="get" action="${ctx}/tax-return/export/books-csv" class="tr-output">
                    <input type="hidden" name="year" value="${page.selectedYear}" />
                    <label class="tr-field">
                        <span>期間開始</span>
                        <input type="date" name="fromDate" />
                    </label>
                    <label class="tr-field">
                        <span>期間終了</span>
                        <input type="date" name="toDate" />
                    </label>
                    <label class="tr-field">
                        <span>勘定科目</span>
                        <select name="accountCode">
                            <option value="">すべて</option>
                            <c:forEach var="account" items="${bookAccountOptions}">
                                <option value="<c:out value='${account.code}' />"><c:out value="${account.name}" />（<c:out value="${account.code}" />）</option>
                            </c:forEach>
                        </select>
                    </label>
                    <button id="button-export-books-csv" type="submit" class="tr-btn tr-btn-outline" data-export="books-csv">帳簿一括CSV</button>
                    <button id="button-export-fixed-assets-csv" type="submit" class="tr-btn tr-btn-outline"
                            formaction="${ctx}/tax-return/export/fixed-assets-csv" data-export="fixed-assets-csv">固定資産台帳CSV</button>
                </form>
                <p class="tr-desc">PDFはサーバーで生成してダウンロードします。大量の仕訳・元帳明細は出力上限があります。帳票は国税庁様式の記載項目に沿った転記用で、公式の提出用紙ではありません。</p>
                <p class="tr-desc">e-Tax入力用CSVは、確定申告書等作成コーナー・e-Taxソフトへ入力する際に参照する欄別の一覧です（e-Taxへ直接取り込むデータ（XTX形式）ではありません）。</p>
                <p class="tr-desc">帳簿CSVは選択年度内の期間・勘定科目を指定できます。仕訳帳・総勘定元帳・簡易記帳・該当する補助簿を収録し、期間を一部に絞ると年度の期首・期末残高は除外されます。固定資産台帳の償却額・残高は選択年分全体です。</p>
                <p class="tr-desc">CSVは機械可読の確認用資料で、公式様式・提出データではなく、税務・電子保存要件への適合は保証しません。固定資産台帳CSVには取得・供用開始・当年償却・償却累計・期末残高と指定期間内の登録イベントを収録します。</p>
            </section>

            <section id="section-closing" class="profit-loss-card tr-section" aria-labelledby="heading-closing">
                <h2 id="heading-closing">年度締め・訂正履歴・出力記録</h2>
                <p class="tr-desc">年度を締めると、仕訳・決算整理・控除区分などの変更ができなくなり、締め時点の集計値と会計データの指紋（SHA-256）を締め版として記録します。締め後に訂正する場合は理由を記録して締めを取消し、訂正後に訂正理由を添えて再度締めます。履歴は削除できません。</p>
                <div id="closing-status" class="tr-closing-status ${closing.closed ? 'is-closed' : 'is-open'}" role="status">
                    <c:choose>
                        <c:when test="${closing.closed and not empty closing.currentVersion}"><strong>締め済み（第<c:out value="${closing.currentVersion}" />版）</strong>・締め日時 <c:out value="${closing.closedAt}" /></c:when>
                        <c:when test="${closing.closed}"><strong>締め済み（締め記録なし）</strong></c:when>
                        <c:otherwise><strong>未締め</strong></c:otherwise>
                    </c:choose>
                    <small>現在のデータ指紋 <code><c:out value="${closing.currentFingerprint}" /></code>・計算ルール版 <c:out value="${closing.calcRuleVersion}" />・様式参照版 <c:out value="${closing.formEdition}" /></small>
                </div>
                <c:choose>
                    <c:when test="${closing.closed}">
                        <c:if test="${not empty closing.blockingClosedYears}">
                            <p class="tr-desc">翌年以降の締め済み年度（<c:forEach var="y" items="${closing.blockingClosedYears}" varStatus="s"><c:out value="${y}" /><c:if test="${not s.last}">・</c:if></c:forEach>）があるため、先にそちらの締めを取消す必要があります。</p>
                        </c:if>
                        <form id="closing-reopen-form" method="post" action="${ctx}/tax-return/reopen?year=${page.selectedYear}" class="tr-closing-form" data-confirm-label="締めを取消す" data-confirm="締めを取消して年度を編集可能にしますか？取消の理由は履歴に残り、この締め版で出力した帳票は旧版として扱われます。">
                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                            <label class="tr-field">
                                <span>締めの取消理由（必須・200文字以内）</span>
                                <input name="reason" maxlength="200" required placeholder="例：売上計上漏れの訂正のため" />
                            </label>
                            <button id="button-reopen-year" type="submit" class="tr-btn">締めを取消（訂正する）</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${not empty closing.blockingOpenYears}">
                            <p class="tr-desc">前年以前の未締め年度（<c:forEach var="y" items="${closing.blockingOpenYears}" varStatus="s"><c:out value="${y}" /><c:if test="${not s.last}">・</c:if></c:forEach>）を先に締めてください。</p>
                        </c:if>
                        <form id="closing-close-form" method="post" action="${ctx}/tax-return/close?year=${page.selectedYear}" class="tr-closing-form" data-confirm-label="締める" data-confirm="年度を締めますか？締め後は取消するまで変更できません。">
                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                            <c:if test="${not empty page.warnings}">
                                <label class="tr-check-inline"><input type="checkbox" name="warningsConfirmed" value="true" />警告（<c:out value="${page.warnings.size()}" />件）の内容を確認した</label>
                            </c:if>
                            <c:if test="${closing.correctionRequired}">
                                <label class="tr-field">
                                    <span>訂正理由（再締めでは必須・200文字以内）</span>
                                    <input name="reason" maxlength="200" required placeholder="例：売上計上漏れ 30,000円を追加" />
                                </label>
                            </c:if>
                            <button id="button-close-year" type="submit" class="tr-btn tr-btn-primary" ${empty page.errors ? '' : 'disabled'}>年度を締める</button>
                            <c:if test="${not empty page.errors}"><small>エラーを解消すると締められます。</small></c:if>
                        </form>
                    </c:otherwise>
                </c:choose>

                <h3 id="heading-closing-history">訂正履歴</h3>
                <div class="tr-table-wrap">
                    <table id="table-closing-history" class="tr-table">
                        <thead>
                            <tr><th scope="col">日時</th><th scope="col">操作</th><th scope="col">版</th><th scope="col">理由</th><th scope="col" class="num">売上</th><th scope="col" class="num">所得</th><th scope="col" class="num">仕訳行</th><th scope="col">変更内容</th><th scope="col">指紋・ルール</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="event" items="${closing.events}">
                                <tr>
                                    <td><c:out value="${event.occurredAt}" /></td>
                                    <td><c:out value="${event.eventLabel}" /></td>
                                    <td>第<c:out value="${event.closeVersion}" />版</td>
                                    <td><c:out value="${empty event.reason ? '—' : event.reason}" /></td>
                                    <td class="num"><fmt:formatNumber value="${event.sales}" maxFractionDigits="0" /></td>
                                    <td class="num"><fmt:formatNumber value="${event.income}" maxFractionDigits="0" /></td>
                                    <td class="num"><c:out value="${event.journalLineCount}" /></td>
                                    <td><c:out value="${event.changeSummary}" /></td>
                                    <td><code><c:out value="${event.shortFingerprint}" /></code><br /><small><c:out value="${event.calcRuleVersion}" />／<c:out value="${event.formEdition}" /></small></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty closing.events}">
                                <tr><td colspan="9" class="empty">締め・取消の履歴はありません。</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <h3 id="heading-output-records">出力帳票の生成記録</h3>
                <p class="tr-desc">PDF・CSVをダウンロードするたびに、生成日時・計算ルール版・様式参照版・年度状態・ファイルのSHA-256を記録します（ファイル本体は保存しません）。手元のファイルのハッシュ値と照合できます。誤って配布した出力などは理由を添えて取消（無効化）できます。直近<c:out value="${closing.outputs.size()}" />件／全<c:out value="${closing.outputCount}" />件を表示しています。</p>
                <div class="tr-table-wrap">
                    <table id="table-output-records" class="tr-table">
                        <thead>
                            <tr><th scope="col">生成日時</th><th scope="col">帳票</th><th scope="col">年度状態</th><th scope="col">計算ルール／様式</th><th scope="col">SHA-256</th><th scope="col">状態</th><th scope="col">操作</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="output" items="${closing.outputs}">
                                <tr>
                                    <td><c:out value="${output.generatedAt}" /></td>
                                    <td><c:out value="${output.outputLabel}" /><br /><small><c:out value="${output.fileName}" /></small></td>
                                    <td><c:out value="${output.stateLabel}" /></td>
                                    <td><c:out value="${output.calcRuleVersion}" /><br /><small><c:out value="${output.formEdition}" /></small></td>
                                    <td><code title="<c:out value='${output.fileSha256}' />"><c:out value="${output.shortSha256}" /></code></td>
                                    <td>
                                        <strong class="${output.status == 'CURRENT' and output.matchesCurrent ? '' : 'tr-retention-alert'}"><c:out value="${output.statusLabel}" /></strong>
                                        <c:if test="${output.status == 'CANCELLED'}"><br /><small><c:out value="${output.cancelledAt}" />：<c:out value="${output.cancelReason}" /></small></c:if>
                                    </td>
                                    <td>
                                        <c:if test="${output.cancellable}">
                                            <form method="post" action="${ctx}/tax-return/outputs/${output.id}/cancel?year=${page.selectedYear}" class="tr-retention-hold">
                                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                                                <input name="reason" maxlength="200" required aria-label="出力記録の取消理由" placeholder="取消理由" />
                                                <button type="submit" class="tr-btn">取消</button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty closing.outputs}">
                                <tr><td colspan="7" class="empty">この年分の出力記録はありません。</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
                <p class="tr-desc">締めは本アプリ内の変更制御であり、締め後の訂正記録が税務・電子帳簿保存法の訂正削除履歴の要件に適合することは保証しません。様式参照版は本アプリが参照した様式の識別子で、国税庁の公式な版番号ではありません。</p>
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
    <p><a class="tr-btn tr-btn-outline" href="${ctx}/tax-return/journal?year=${page.selectedYear}&amp;status=draft">未確認の仕訳を確認する</a></p>
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
