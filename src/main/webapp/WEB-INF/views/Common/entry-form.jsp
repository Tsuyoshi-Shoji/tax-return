<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="isIncome" value="${entryKind eq 'income'}" />
<c:set var="pageTitle" value="${isIncome ? '収益登録' : '支出登録'}" />
<c:set var="pageDescription" value="${isIncome ? '収益情報を入力して登録します。' : '支出情報を入力して登録します。'}" />
<c:set var="typeFieldName" value="${isIncome ? 'incomeType' : 'expenseType'}" />
<c:set var="methodFieldName" value="${isIncome ? 'receiveMethodId' : 'paymentMethodId'}" />
<c:set var="typeLabel" value="区分" />
<c:set var="itemLabel" value="${isIncome ? '収益項目' : '支出項目'}" />
<c:set var="dateLabel" value="${isIncome ? '収益発生日' : '支出発生日'}" />
<c:set var="methodLabel" value="${isIncome ? '受取方法' : '支払方法'}" />
<c:set var="excludeLabel" value="${isIncome ? 'この収益を確定申告対象外にする' : 'この支出を確定申告対象外にする'}" />
<c:set var="detailsLabel" value="${isIncome ? '収益詳細' : '支出詳細'}" />
<c:set var="detailsRequiredMessage" value="${isIncome ? '収益詳細を入力してください。' : '支出詳細を入力してください。'}" />
<c:set var="detailsLengthMessage" value="${isIncome ? '収益詳細は1〜255文字で入力してください。' : '支出詳細は1〜255文字で入力してください。'}" />
<c:set var="dateRequiredMessage" value="${isIncome ? '収益発生日を選択してください。' : '支出発生日を選択してください。'}" />
<c:set var="methodRequiredMessage" value="${isIncome ? '受取方法を選択してください。' : '支払方法を選択してください。'}" />
<c:set var="selectedType" value="${isIncome ? entryForm.incomeType : entryForm.expenseType}" />
<c:set var="selectedMethod" value="${isIncome ? entryForm.receiveMethodId : entryForm.paymentMethodId}" />
<c:set var="selectedEntryMode" value="${empty entryForm.entryMode ? 'normal' : entryForm.entryMode}" />
<c:set var="defaultType" value="${empty categories ? '' : categories[0].id}" />
<c:set var="defaultMethod" value="${empty methods ? '' : methods[0].id}" />
<fmt:setLocale value="ja_JP" scope="page" />
<div class="entry-container">
    <div class="entry-topbar">
        <nav class="entry-breadcrumb" aria-label="パンくず">
            <ol>
                <li><a href="${pageContext.request.contextPath}/home">ホーム</a></li>
                <li aria-current="page"><c:out value="${pageTitle}" /></li>
            </ol>
        </nav>
        <c:if test="${not isIncome}">
            <section class="entry-mode-switch" aria-labelledby="entry-mode-label">
                <span id="entry-mode-label" class="entry-mode-switch-label">登録モード</span>
                <div class="entry-mode-segment" role="radiogroup" aria-labelledby="entry-mode-label">
                    <label class="entry-mode-option">
                        <input id="entry-mode-normal" class="entry-mode-input" type="radio" name="entryMode" value="normal" form="entry-form" <c:if test="${selectedEntryMode ne 'monthly'}">checked</c:if>>
                        <span class="entry-mode-option-text">通常モード</span>
                    </label>
                    <label class="entry-mode-option">
                        <input id="entry-mode-monthly" class="entry-mode-input" type="radio" name="entryMode" value="monthly" form="entry-form" <c:if test="${selectedEntryMode eq 'monthly'}">checked</c:if>>
                        <span class="entry-mode-option-text">月間一括登録モード</span>
                    </label>
                </div>
                <p id="error-entryMode" class="entry-field-error" data-error-for="entryMode"><c:out value="${fieldErrors.entryMode}" /></p>
            </section>
        </c:if>
    </div>

    <section class="entry-card" aria-labelledby="page-title">
        <div class="entry-card-header">
            <div class="entry-card-heading">
                <div class="entry-card-title-row">
                    <h1 id="page-title"><c:out value="${pageTitle}" /></h1>
                    <c:if test="${not isIncome}">
                        <span id="monthly-mode-badge" class="entry-mode-badge" <c:if test="${selectedEntryMode ne 'monthly'}">hidden</c:if>>月間一括登録モード</span>
                    </c:if>
                </div>
                <p id="page-description" class="entry-page-description" <c:if test="${not isIncome and selectedEntryMode eq 'monthly'}">hidden</c:if>><c:out value="${pageDescription}" /></p>
                <c:if test="${not isIncome}">
                    <p id="monthly-mode-description" class="entry-page-description" <c:if test="${selectedEntryMode ne 'monthly'}">hidden</c:if>>カード利用分を月末にまとめて登録できます。月間の支出を一括入力してください。</p>
                </c:if>
            </div>
            <div class="entry-card-header-side">
                <c:if test="${not empty fiscalYearLabel}">
                    <p class="entry-fiscal-year">対象年度：<c:out value="${fiscalYearLabel}" /></p>
                </c:if>
                <c:if test="${not isIncome}">
                    <button id="button-add-5-rows" class="entry-button entry-button-outline entry-button-add" type="button" aria-controls="monthly-rows" <c:if test="${selectedEntryMode ne 'monthly'}">hidden</c:if>>
                        <span class="entry-button-icon entry-button-icon-plus" aria-hidden="true"></span>行を5件追加
                    </button>
                </c:if>
            </div>
        </div>

        <form id="entry-form"
              class="entry-form"
              method="post"
              action="${pageContext.request.contextPath}/${entryKind}"
              novalidate
              data-entry-kind="${fn:escapeXml(entryKind)}"
              data-today="${fn:escapeXml(today)}"
              data-apportionment-rate="${empty apportionmentRate ? '' : fn:escapeXml(apportionmentRate)}"
              data-default-type="${fn:escapeXml(defaultType)}"
              data-default-method="${fn:escapeXml(defaultMethod)}"
              data-confirmation-required="${confirmationRequired ? 'true' : 'false'}">
            <input type="hidden" name="editId" value="${empty entryForm.editId ? '' : fn:escapeXml(entryForm.editId)}">
            <input type="hidden" name="version" value="${empty entryForm.version ? '' : fn:escapeXml(entryForm.version)}">
            <input type="hidden" name="csrfToken" value="${fn:escapeXml(entryForm.csrfToken)}">
            <input type="hidden" name="submissionToken" value="${fn:escapeXml(entryForm.submissionToken)}">
            <input type="hidden" id="confirmation-token" name="confirmationToken" value="${fn:escapeXml(entryForm.confirmationToken)}">

            <section id="normal-mode-section" class="entry-mode-panel" <c:if test="${not isIncome and selectedEntryMode eq 'monthly'}">hidden</c:if>>
            <div class="entry-grid">
                <div class="entry-row" data-field-group="${fn:escapeXml(typeFieldName)}">
                    <div class="entry-label-wrap">
                        <label id="label-type" for="entry-type-1"><c:out value="${typeLabel}" /> <span class="entry-required" aria-hidden="true">*</span></label>
                    </div>
                    <div class="entry-control-wrap">
                        <div class="entry-radio-group" role="radiogroup" aria-labelledby="label-type" aria-required="true">
                            <c:forEach var="category" items="${categories}" varStatus="status">
                                <label class="entry-radio">
                                    <input id="entry-type-${status.index + 1}"
                                           type="radio"
                                           name="${fn:escapeXml(typeFieldName)}"
                                           value="${fn:escapeXml(category.id)}"
                                           <c:if test="${category.id eq selectedType}">checked</c:if>
                                           <c:if test="${empty selectedType and category.id eq defaultType}">checked</c:if>
                                           aria-describedby="error-${fn:escapeXml(typeFieldName)}">
                                    <span><c:out value="${category.label}" /></span>
                                </label>
                            </c:forEach>
                        </div>
                        <p id="error-${fn:escapeXml(typeFieldName)}" class="entry-field-error" data-error-for="${fn:escapeXml(typeFieldName)}"><c:out value="${isIncome ? fieldErrors.incomeType : fieldErrors.expenseType}" /></p>
                    </div>
                </div>

                <div class="entry-row" data-field-group="category">
                    <div class="entry-label-wrap">
                        <label id="label-category" for="category"><c:out value="${itemLabel}" /> <span class="entry-required" aria-hidden="true">*</span></label>
                    </div>
                    <div class="entry-control-wrap">
                        <select id="category"
                                name="category"
                                aria-required="true"
                                aria-describedby="error-category"
                                <c:if test="${not empty fieldErrors.category}">aria-invalid="true"</c:if>>
                            <option value="">項目を選択してください</option>
                            <c:forEach var="item" items="${items}">
                                <c:if test="${item.group eq (empty selectedType ? defaultType : selectedType)}">
                                    <option value="${fn:escapeXml(item.id)}" <c:if test="${item.id eq entryForm.category}">selected</c:if>><c:out value="${item.label}" /></option>
                                </c:if>
                            </c:forEach>
                        </select>
                        <select id="category-option-source" hidden aria-hidden="true" tabindex="-1">
                            <c:forEach var="item" items="${items}">
                                <option value="${fn:escapeXml(item.id)}"
                                        data-group="${fn:escapeXml(item.group)}"
                                        <c:if test="${item.id eq entryForm.category}">data-selected="true"</c:if>><c:out value="${item.label}" /></option>
                            </c:forEach>
                        </select>
                        <p id="error-category" class="entry-field-error" data-error-for="category"><c:out value="${fieldErrors.category}" /></p>
                    </div>
                </div>

                <div class="entry-row" data-field-group="date">
                    <div class="entry-label-wrap">
                        <label id="label-date" for="date"><c:out value="${dateLabel}" /> <span class="entry-required" aria-hidden="true">*</span></label>
                    </div>
                    <div class="entry-control-wrap">
                        <input id="date"
                               type="date"
                               name="date"
                               value="${fn:escapeXml(entryForm.date)}"
                               max="${fn:escapeXml(today)}"
                               placeholder="年/月/日"
                               aria-required="true"
                               aria-describedby="error-date"
                               <c:if test="${not empty fieldErrors.date}">aria-invalid="true"</c:if>>
                        <p id="error-date" class="entry-field-error" data-error-for="date"><c:out value="${fieldErrors.date}" /></p>
                    </div>
                </div>

                <div class="entry-row" data-field-group="amount">
                    <div class="entry-label-wrap">
                        <label id="label-amount" for="amount">金額 <span class="entry-required" aria-hidden="true">*</span></label>
                    </div>
                    <div class="entry-control-wrap">
                        <div class="entry-amount-wrap">
                            <input id="amount"
                                   type="text"
                                   name="amount"
                                   value="${fn:escapeXml(entryForm.amount)}"
                                   inputmode="numeric"
                                   pattern="[0-9]*"
                                   placeholder="例）100000"
                                   aria-required="true"
                                   aria-describedby="error-amount"
                                   <c:if test="${not empty fieldErrors.amount}">aria-invalid="true"</c:if>>
                            <span class="entry-amount-suffix" aria-hidden="true">円</span>
                        </div>
                        <p id="error-amount" class="entry-field-error" data-error-for="amount"><c:out value="${fieldErrors.amount}" /></p>
                    </div>
                </div>

                <div class="entry-row" data-field-group="${fn:escapeXml(methodFieldName)}">
                    <div class="entry-label-wrap">
                        <label id="label-method" for="method-1"><c:out value="${methodLabel}" /> <span class="entry-required" aria-hidden="true">*</span></label>
                    </div>
                    <div class="entry-control-wrap">
                        <div class="entry-radio-group entry-radio-group-methods" role="radiogroup" aria-labelledby="label-method" aria-required="true">
                            <c:forEach var="method" items="${methods}" varStatus="status">
                                <label class="entry-radio">
                                    <input id="method-${status.index + 1}"
                                           type="radio"
                                           name="${fn:escapeXml(methodFieldName)}"
                                           value="${fn:escapeXml(method.id)}"
                                           <c:if test="${method.id eq selectedMethod}">checked</c:if>
                                           <c:if test="${empty selectedMethod and method.id eq defaultMethod}">checked</c:if>
                                           aria-describedby="error-${fn:escapeXml(methodFieldName)}">
                                    <span><c:out value="${method.label}" /></span>
                                </label>
                            </c:forEach>
                        </div>
                        <p id="error-${fn:escapeXml(methodFieldName)}" class="entry-field-error" data-error-for="${fn:escapeXml(methodFieldName)}"><c:out value="${isIncome ? fieldErrors.receiveMethodId : fieldErrors.paymentMethodId}" /></p>
                    </div>
                </div>

                <div class="entry-row entry-row-check" data-field-group="excludeFromReturn">
                    <div class="entry-label-wrap">
                        <label for="exclude-from-return">確定申告対象</label>
                    </div>
                    <div class="entry-control-wrap">
                        <label class="entry-check">
                            <input id="exclude-from-return" type="checkbox" name="excludeFromReturn" value="true" <c:if test="${entryForm.excludeFromReturn}">checked</c:if>>
                            <span><c:out value="${excludeLabel}" /></span>
                        </label>
                    </div>
                </div>

                <c:if test="${not isIncome}">
                    <div class="entry-apportionment-panel" data-field-group="homeApportionment">
                        <div class="entry-apportionment-head">
                            <h2 id="heading-home-apportionment">家事按分</h2>
                            <label class="entry-check entry-apportionment-toggle">
                                <input id="home-apportionment" type="checkbox" name="homeApportionment" value="true" <c:if test="${entryForm.homeApportionment}">checked</c:if> aria-describedby="error-homeApportionment apportionment-description">
                                <span>家事按分を適用する</span>
                            </label>
                        </div>
                        <p id="apportionment-description" class="entry-apportionment-note">
                            <c:choose>
                                <c:when test="${not empty apportionmentRate}">
                                    設定画面で保持している固定按分率 <strong id="display-apportionment-rate-text"><fmt:formatNumber value="${apportionmentRate}" pattern="0.##" /></strong>% を使って経費対象金額を計算します。
                                </c:when>
                                <c:otherwise>
                                    設定画面で家事按分率を設定すると、経費対象金額を計算できます。
                                </c:otherwise>
                            </c:choose>
                        </p>
                        <div class="entry-apportionment-grid" aria-live="polite">
                            <div class="entry-preview-row">
                                <span class="entry-preview-label">家事按分率</span>
                                <span id="display-apportionment-rate" class="entry-preview-value"><c:choose><c:when test="${not empty apportionmentRate}"><fmt:formatNumber value="${apportionmentRate}" pattern="0.##" />%</c:when><c:otherwise>未設定</c:otherwise></c:choose></span>
                            </div>
                            <div class="entry-preview-row">
                                <span class="entry-preview-label">経費対象金額</span>
                                <span id="display-deductible-amount" class="entry-preview-value">—</span>
                                <span id="display-deductible-formula" class="entry-preview-formula">—</span>
                            </div>
                        </div>
                        <p id="error-homeApportionment" class="entry-field-error" data-error-for="homeApportionment"><c:out value="${fieldErrors.homeApportionment}" /></p>
                    </div>
                </c:if>

                <div class="entry-row entry-row-details" data-field-group="details">
                    <div class="entry-label-wrap">
                        <label id="label-details" for="details"><c:out value="${detailsLabel}" /> <span class="entry-required" aria-hidden="true">*</span></label>
                    </div>
                    <div class="entry-control-wrap">
                        <textarea id="details"
                                  name="details"
                                  rows="4"
                                  placeholder="内容を入力してください"
                                  aria-required="true"
                                  aria-describedby="details-count error-details"
                                  <c:if test="${not empty fieldErrors.details}">aria-invalid="true"</c:if>><c:out value="${entryForm.details}" /></textarea>
                        <div class="entry-details-meta">
                            <p id="details-count" class="entry-counter" aria-live="polite">0 / 255文字</p>
                        </div>
                        <p id="error-details" class="entry-field-error" data-error-for="details"><c:out value="${fieldErrors.details}" /></p>
                    </div>
                </div>
            </div>
            </section>

            <c:if test="${not isIncome}">
                <section id="monthly-mode-section" class="entry-mode-panel entry-monthly-panel" aria-label="月間一括登録" <c:if test="${selectedEntryMode ne 'monthly'}">hidden</c:if>>
                    <div class="entry-monthly-toolbar" data-field-group="targetMonth">
                        <label id="label-target-month" class="entry-monthly-toolbar-label" for="target-month">登録対象月 <span class="entry-required" aria-hidden="true">*</span></label>
                        <input id="target-month"
                               type="month"
                               name="targetMonth"
                               value="${fn:escapeXml(entryForm.targetMonth)}"
                               aria-required="true"
                               aria-describedby="error-targetMonth"
                               <c:if test="${not empty fieldErrors.targetMonth}">aria-invalid="true"</c:if>>
                        <p id="error-targetMonth" class="entry-field-error" data-error-for="targetMonth"><c:out value="${fieldErrors.targetMonth}" /></p>
                    </div>
                    <p class="entry-field-error entry-monthly-rows-error" data-error-for="monthlyRows"><c:out value="${fieldErrors.monthlyRows}" /></p>

                    <c:set var="monthlyRowLastIndex" value="${empty entryForm.monthlyRows ? 4 : fn:length(entryForm.monthlyRows) - 1}" />
                    <div class="entry-monthly-scroll" role="region" aria-label="支出明細（横スクロールできます）" tabindex="0">
                        <div id="monthly-rows" class="entry-monthly-rows" data-items-placeholder="項目を選択してください">
                            <c:forEach var="index" begin="0" end="${monthlyRowLastIndex}">
                                <c:remove var="row" />
                                <c:if test="${not empty entryForm.monthlyRows}">
                                    <c:set var="row" value="${entryForm.monthlyRows[index]}" />
                                </c:if>
                                <c:set var="rowType" value="${empty row.expenseType ? defaultType : row.expenseType}" />
                                <c:set var="rowMethod" value="${empty row.paymentMethodId ? defaultMethod : row.paymentMethodId}" />
                                <c:set var="rowPath" value="monthlyRows[${index}]" />
                                <article class="entry-monthly-row" data-row-index="${index}" aria-label="明細${index + 1}">
                                    <div class="entry-monthly-grid">
                                        <fieldset class="entry-monthly-field entry-monthly-field-type">
                                            <legend class="entry-monthly-label">区分</legend>
                                            <div class="entry-monthly-choices">
                                                <c:forEach var="category" items="${categories}">
                                                    <label class="entry-radio entry-radio-compact">
                                                        <input type="radio"
                                                               class="monthly-expense-type"
                                                               name="${rowPath}.expenseType"
                                                               value="${fn:escapeXml(category.id)}"
                                                               data-error-field="${rowPath}.expenseType"
                                                               <c:if test="${category.id eq rowType}">checked</c:if>>
                                                        <span><c:out value="${category.label}" /></span>
                                                    </label>
                                                </c:forEach>
                                            </div>
                                        </fieldset>
                                        <label class="entry-monthly-field entry-monthly-field-item">
                                            <span class="entry-monthly-label">項目</span>
                                            <select name="${rowPath}.category" class="monthly-category" data-selected="${fn:escapeXml(row.category)}" data-error-field="${rowPath}.category">
                                                <option value="">項目を選択してください</option>
                                                <c:forEach var="item" items="${items}">
                                                    <c:if test="${item.group eq rowType}">
                                                        <option value="${fn:escapeXml(item.id)}" <c:if test="${item.id eq row.category}">selected</c:if>><c:out value="${item.label}" /></option>
                                                    </c:if>
                                                </c:forEach>
                                            </select>
                                        </label>
                                        <label class="entry-monthly-field entry-monthly-field-date">
                                            <span class="entry-monthly-label">支出発生日</span>
                                            <input type="date" name="${rowPath}.date" value="${fn:escapeXml(row.date)}" max="${fn:escapeXml(today)}" data-error-field="${rowPath}.date">
                                        </label>
                                        <label class="entry-monthly-field entry-monthly-field-amount">
                                            <span class="entry-monthly-label">金額</span>
                                            <span class="entry-amount-wrap">
                                                <input type="text" name="${rowPath}.amount" value="${fn:escapeXml(row.amount)}" inputmode="numeric" pattern="[0-9]*" placeholder="例）100000" data-error-field="${rowPath}.amount">
                                                <span class="entry-amount-suffix" aria-hidden="true">円</span>
                                            </span>
                                        </label>
                                        <fieldset class="entry-monthly-field entry-monthly-field-method">
                                            <legend class="entry-monthly-label">支払い方法</legend>
                                            <div class="entry-monthly-choices">
                                                <c:forEach var="method" items="${methods}">
                                                    <label class="entry-radio entry-radio-compact">
                                                        <input type="radio"
                                                               class="monthly-payment-method"
                                                               name="${rowPath}.paymentMethodId"
                                                               value="${fn:escapeXml(method.id)}"
                                                               data-error-field="${rowPath}.paymentMethodId"
                                                               <c:if test="${method.id eq rowMethod}">checked</c:if>>
                                                        <span><c:out value="${method.label}" /></span>
                                                    </label>
                                                </c:forEach>
                                            </div>
                                        </fieldset>
                                        <div class="entry-monthly-field-options">
                                            <label class="entry-check entry-check-compact">
                                                <input type="checkbox" name="${rowPath}.excludeFromReturn" value="true" <c:if test="${row.excludeFromReturn}">checked</c:if>>
                                                <span>申告対象外</span>
                                            </label>
                                            <label class="entry-check entry-check-compact">
                                                <input type="checkbox" name="${rowPath}.homeApportionment" value="true" data-error-field="${rowPath}.homeApportionment" <c:if test="${row.homeApportionment}">checked</c:if>>
                                                <span>家事按分適用</span>
                                            </label>
                                        </div>
                                        <label class="entry-monthly-field-details">
                                            <span class="entry-monthly-label-inline">支出詳細</span>
                                            <input type="text" name="${rowPath}.details" value="${fn:escapeXml(row.details)}" maxlength="255" placeholder="内容を入力してください" data-error-field="${rowPath}.details">
                                        </label>
                                        <button class="entry-button entry-button-muted entry-monthly-remove button-remove-row" id="button-remove-row-${index}" type="button" aria-label="明細${index + 1}を削除">
                                            <span class="entry-button-icon entry-button-icon-trash" aria-hidden="true"></span>削除
                                        </button>
                                        <div class="entry-monthly-errors">
                                            <p class="entry-field-error" data-error-for="${rowPath}.expenseType"></p>
                                            <p class="entry-field-error" data-error-for="${rowPath}.category"></p>
                                            <p class="entry-field-error" data-error-for="${rowPath}.date"></p>
                                            <p class="entry-field-error" data-error-for="${rowPath}.amount"></p>
                                            <p class="entry-field-error" data-error-for="${rowPath}.paymentMethodId"></p>
                                            <p class="entry-field-error" data-error-for="${rowPath}.details"></p>
                                            <p class="entry-field-error" data-error-for="${rowPath}.homeApportionment"></p>
                                        </div>
                                    </div>
                                </article>
                            </c:forEach>
                        </div>
                    </div>

                    <div class="entry-monthly-actions">
                        <p class="entry-monthly-count" aria-live="polite">入力件数 <strong id="monthly-row-count"><c:out value="${monthlyRowLastIndex + 1}" /></strong>件</p>
                        <div class="entry-monthly-buttons">
                            <button id="button-clear-monthly" class="entry-button entry-button-secondary" type="button">クリア</button>
                            <button id="button-register-monthly" class="entry-button entry-button-primary" type="submit">一括登録</button>
                        </div>
                    </div>
                </section>
            </c:if>


            <c:if test="${confirmationRequired}">
                <section class="entry-confirmation-panel" aria-labelledby="confirmation-panel-title">
                    <h2 id="confirmation-panel-title">高額<c:out value="${isIncome ? '収益' : '支出'}" />の確認</h2>
                    <p>100,000,000円以上の<c:out value="${isIncome ? '収益' : '支出'}" />は確認後に送信されます。JavaScript が無効な場合は内容を確認のうえ、下の「確認して送信」を押してください。</p>
                </section>
            </c:if>

            <div id="normal-mode-actions" class="entry-actions" <c:if test="${not isIncome and selectedEntryMode eq 'monthly'}">hidden</c:if>>
                <button id="button-clear" class="entry-button entry-button-secondary" type="button">クリア</button>
                <button id="button-register"
                        class="entry-button entry-button-primary"
                        type="submit"
                        data-default-label="登録"
                        data-confirm-label="確認して送信"><c:out value="${confirmationRequired ? '確認して送信' : '登録'}" /></button>
            </div>
        </form>
    </section>
</div>

<dialog id="confirmation-dialog" class="entry-dialog" aria-labelledby="confirmation-dialog-title">
    <form method="dialog" class="entry-dialog-body">
        <div class="entry-dialog-header">
            <h2 id="confirmation-dialog-title">高額<c:out value="${isIncome ? '収益' : '支出'}" />の確認</h2>
            <button class="entry-dialog-close" type="submit" value="cancel" aria-label="閉じる">×</button>
        </div>
        <p>以下の内容で送信します。問題なければ「確認して送信」を押してください。</p>
        <dl class="entry-confirmation-list">
            <div><dt>区分</dt><dd id="confirm-type">—</dd></div>
            <div><dt><c:out value="${itemLabel}" /></dt><dd id="confirm-category">—</dd></div>
            <div><dt><c:out value="${dateLabel}" /></dt><dd id="confirm-date">—</dd></div>
            <div><dt>金額</dt><dd id="confirm-amount">—</dd></div>
            <div><dt><c:out value="${methodLabel}" /></dt><dd id="confirm-method">—</dd></div>
            <div><dt>確定申告対象外</dt><dd id="confirm-exclude">—</dd></div>
            <c:if test="${not isIncome}">
                <div><dt>家事按分</dt><dd id="confirm-apportionment">—</dd></div>
                <div><dt>経費対象金額</dt><dd id="confirm-deductible">—</dd></div>
            </c:if>
            <div><dt><c:out value="${detailsLabel}" /></dt><dd id="confirm-details">—</dd></div>
        </dl>
        <div class="entry-dialog-actions">
            <button id="button-confirm-cancel" class="entry-button entry-button-secondary" type="submit" value="cancel">内容を修正する</button>
            <button id="button-confirm-submit" class="entry-button entry-button-primary" type="button">確認して送信</button>
        </div>
    </form>
</dialog>
<c:set var="entryActionLabel" value="${empty entryForm.editId ? '登録' : '更新'}" />
<jsp:include page="/WEB-INF/views/Common/app-dialog.jsp">
    <jsp:param name="errorTitle" value="${entryActionLabel}に失敗しました" />
    <jsp:param name="secondaryLabel" value="${empty entryForm.editId ? '続けて登録' : '閉じる'}" />
    <jsp:param name="primaryLabel" value="一覧へ" />
    <jsp:param name="primaryHref" value="${pageContext.request.contextPath}/profit-loss" />
</jsp:include>
