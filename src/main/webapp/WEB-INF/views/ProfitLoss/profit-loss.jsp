<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>損益検索・一覧</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <main>
        <div class="container profit-loss-container">
            <div class="page-header">
                <h1>損益検索・一覧</h1>
                <p class="page-subtitle">収益・支出を条件指定して確認できます。</p>
            </div>

            <c:if test="${not empty successMessage}">
                <div class="success-message" role="status">
                    <c:out value="${successMessage}" />
                </div>
            </c:if>

            <c:if test="${not empty failureMessage}">
                <div class="error-messages" role="alert">
                    <p class="error-title"><c:out value="${failureMessage}" /></p>
                </div>
            </c:if>

            <section class="search-container" aria-labelledby="searchTitle">
                <div class="search-header">
                    <h2 id="searchTitle">検索条件</h2>
                    <p>未指定の条件は全件対象です。</p>
                </div>

                <form id="profitLossSearchForm" class="search-form" method="get" action="${pageContext.request.contextPath}/profit-loss" novalidate>
                    <div class="form-group">
                        <label for="dateFrom">発生日From</label>
                        <input type="date" id="dateFrom" name="dateFrom" value="${param.dateFrom}" />
                        <p id="dateFromError" class="error-message" aria-live="polite"></p>
                    </div>

                    <div class="form-group">
                        <label for="dateTo">発生日To</label>
                        <input type="date" id="dateTo" name="dateTo" value="${param.dateTo}" />
                        <p id="dateToError" class="error-message" aria-live="polite"></p>
                    </div>

                    <div class="form-group full">
                        <span class="group-label">区分</span>
                        <div class="checkbox-group category-type-group" aria-label="区分の複数選択">
                            <label><input type="checkbox" name="types" value="expense" <c:if test="${form.types.contains('expense')}">checked</c:if> /> 経費</label>
                            <label><input type="checkbox" name="types" value="public" <c:if test="${form.types.contains('public')}">checked</c:if> /> 公的負担</label>
                            <label><input type="checkbox" name="types" value="private" <c:if test="${form.types.contains('private')}">checked</c:if> /> 私的利用</label>
                            <label><input type="checkbox" name="types" value="business" <c:if test="${form.types.contains('business')}">checked</c:if> /> 事業収入</label>
                            <label><input type="checkbox" name="types" value="investment" <c:if test="${form.types.contains('investment')}">checked</c:if> /> 金融・投資収益</label>
                            <label><input type="checkbox" name="types" value="temporary" <c:if test="${form.types.contains('temporary')}">checked</c:if> /> 一時収益</label>
                            <label><input type="checkbox" name="types" value="nontaxable" <c:if test="${form.types.contains('nontaxable')}">checked</c:if> /> 非課税・対象外収益</label>
                        </div>
                    </div>

                    <div class="form-group full">
                        <span class="group-label">申告対象外データ</span>
                        <div class="checkbox-group" aria-label="申告対象外データの表示条件">
                            <label><input type="radio" name="showTaxExcluded" value="true" <c:if test="${form.showTaxExcluded}">checked</c:if> /> 表示する</label>
                            <label><input type="radio" name="showTaxExcluded" value="false" <c:if test="${not form.showTaxExcluded}">checked</c:if> /> 表示しない</label>
                        </div>
                        <p class="help-text">編集や確認のため、申告対象外の収益を一覧に含めるか切り替えられます。</p>
                    </div>

                    <div class="form-group full">
                        <label for="itemsDisplay">項目</label>
                        <div class="custom-select" id="itemSelect">
                            <button type="button" class="select-display" id="itemsDisplay" aria-expanded="false" aria-controls="itemOptions">
                                <span class="selected-items" id="selectedItems">全項目</span>
                            </button>
                            <div class="select-options" id="itemOptions"></div>
                        </div>
                        <input type="hidden" id="items" name="items" value="${param.items}" />
                        <p class="help-text">区分を未選択の場合は全区分の項目を表示します。</p>
                    </div>

                    <div class="form-group">
                        <label for="amountFrom">金額From</label>
                        <input type="number" id="amountFrom" name="amountFrom" min="0" step="1" inputmode="numeric" value="${param.amountFrom}" />
                        <p id="amountFromError" class="error-message" aria-live="polite"></p>
                    </div>

                    <div class="form-group">
                        <label for="amountTo">金額To</label>
                        <input type="number" id="amountTo" name="amountTo" min="0" step="1" inputmode="numeric" value="${param.amountTo}" />
                        <p id="amountToError" class="error-message" aria-live="polite"></p>
                    </div>

                    <input type="hidden" name="incomePage" id="incomePage" value="${empty param.incomePage ? '1' : param.incomePage}" />
                    <input type="hidden" name="expensePage" id="expensePage" value="${empty param.expensePage ? '1' : param.expensePage}" />

                    <div class="search-button-group">
                        <button type="button" class="reset-button" id="clearButton">クリア</button>
                        <button type="submit" class="search-button">検索</button>
                    </div>
                </form>
            </section>

            <section class="summary-section" aria-labelledby="summaryTitle">
                <h2 id="summaryTitle" class="summary-title">損益サマリー</h2>
                <div class="summary">
                    <div class="summary-card income-card">
                        <div class="card-content">
                            <h3>総収益金額</h3>
                            <div class="amount">¥<fmt:formatNumber value="${empty totalIncome ? 0 : totalIncome}" pattern="#,##0" /></div>
                        </div>
                    </div>
                    <div class="summary-card expense-card">
                        <div class="card-content">
                            <h3>総支出金額</h3>
                            <div class="amount">¥<fmt:formatNumber value="${empty totalExpense ? 0 : totalExpense}" pattern="#,##0" /></div>
                        </div>
                    </div>
                    <div class="summary-card profit-card">
                        <div class="card-content">
                            <h3>損益金額</h3>
                            <div class="amount">¥<fmt:formatNumber value="${empty profitAmount ? 0 : profitAmount}" pattern="#,##0" /></div>
                        </div>
                    </div>
                </div>
            </section>

            <section class="data-table-section" aria-labelledby="incomeListTitle">
                <div class="section-heading">
                    <h2 id="incomeListTitle">収益一覧</h2>
                    <span>1ページ10件表示</span>
                </div>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr>
                                <th>発生日</th>
                                <th>区分</th>
                                <th>項目</th>
                                <th class="amount-column">金額</th>
                                <th>詳細</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty incomeRows}">
                                    <c:forEach var="row" items="${incomeRows}">
                                        <tr>
                                            <td><c:out value="${row.date}" /></td>
                                            <td><c:out value="${row.type}" /></td>
                                            <td><c:out value="${row.item}" /></td>
                                            <td class="amount-column">¥<fmt:formatNumber value="${row.amount}" pattern="#,##0" /></td>
                                            <td>
                                                <a class="detail-button" href="${pageContext.request.contextPath}/profit-loss/detail?recordType=income&id=${row.id}">詳細</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr><td colspan="5" class="empty-row">収益データはありません。</td></tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
                <nav class="pagination" id="incomePagination" aria-label="収益一覧ページング" data-page-input="incomePage" data-current-page="${empty incomeCurrentPage ? 1 : incomeCurrentPage}" data-total-pages="${empty incomeTotalPages ? 1 : incomeTotalPages}"></nav>
            </section>

            <section class="data-table-section" aria-labelledby="expenseListTitle">
                <div class="section-heading">
                    <h2 id="expenseListTitle">支出一覧</h2>
                    <span>1ページ10件表示</span>
                </div>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr>
                                <th>発生日</th>
                                <th>区分</th>
                                <th>項目</th>
                                <th class="amount-column">金額</th>
                                <th>詳細</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty expenseRows}">
                                    <c:forEach var="row" items="${expenseRows}">
                                        <tr>
                                            <td><c:out value="${row.date}" /></td>
                                            <td><c:out value="${row.type}" /></td>
                                            <td><c:out value="${row.item}" /></td>
                                            <td class="amount-column">¥<fmt:formatNumber value="${row.amount}" pattern="#,##0" /></td>
                                            <td>
                                                <a class="detail-button" href="${pageContext.request.contextPath}/profit-loss/detail?recordType=expense&id=${row.id}">詳細</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr><td colspan="5" class="empty-row">支出データはありません。</td></tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
                <nav class="pagination" id="expensePagination" aria-label="支出一覧ページング" data-page-input="expensePage" data-current-page="${empty expenseCurrentPage ? 1 : expenseCurrentPage}" data-total-pages="${empty expenseTotalPages ? 1 : expenseTotalPages}"></nav>
            </section>
        </div>
    </main>

    <div id="errorModal" class="modal" aria-hidden="true" role="dialog" aria-modal="true" aria-labelledby="errorModalTitle">
        <div class="modal-content modal-error">
            <h2 id="errorModalTitle">入力エラー</h2>
            <p id="errorMessage"></p>
            <div class="modal-actions">
                <button type="button" class="modal-button" id="closeErrorModal">閉じる</button>
            </div>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />

    <script>
        (function () {
            'use strict';

            const itemMaster = {
                expense: ['通信・IT利用料', '設備・機材', '外注費', '広告宣伝費', '旅費交通費', '会議費', '接待交際費', '地代家賃', '水道光熱費', 'スペース利用料', '学習・教材費', '手数料', '保険（事業保険・賠償責任保険）', '税務相談費', '法務相談費', '消耗品費', '雑費'],
                public: ['所得税', '住民税', '個人事業税', '固定資産税', '自動車税', '消費税', '国民健康保険', '国民年金', '介護保険', '証明書発行手数料', '行政手続料', '許可申請費', 'その他公的支出（罰金・延滞税、各種協会費など）'],
                private: ['生活費', '趣味・娯楽', '教育費', '医療費', '交通費', '交際費', 'その他私費(雑費・貯金・投資など)'],
                business: ['商品売上', 'サービス売上', '制作売上', '開発売上(業務委託)', '開発売上(請負)', 'コンサル売上', '広告収益', 'アフィリエイト', 'YouTube収益', 'SNS収益', 'ライセンス収益', 'サブスク収益', 'システム利用料', '手数料収益', 'イベント売上', 'その他事業収益'],
                investment: ['株式配当', '売却益', 'FX収益', '仮想通貨利益', 'ステーキング', '投資信託収益', '銀行利息', '外貨利息', 'その他金融・投資収益'],
                temporary: ['不用品売却', '臨時収入', '保険金', 'お祝い金', '懸賞', '返戻金', 'その他一時収益'],
                nontaxable: ['借入金', '資本投入費', '建て替え金', '預かり金', '振替', '補助・給付金', 'その他非課税・対象外収益']
            };

            const form = document.getElementById('profitLossSearchForm');
            const dateFrom = document.getElementById('dateFrom');
            const dateTo = document.getElementById('dateTo');
            const amountFrom = document.getElementById('amountFrom');
            const amountTo = document.getElementById('amountTo');
            const itemsHidden = document.getElementById('items');
            const itemOptions = document.getElementById('itemOptions');
            const selectedItems = document.getElementById('selectedItems');
            const itemsDisplay = document.getElementById('itemsDisplay');
            const errorModal = document.getElementById('errorModal');
            const errorMessage = document.getElementById('errorMessage');

            function selectedTypes() {
                return Array.from(document.querySelectorAll('input[name="types"]:checked')).map(input => input.value);
            }

            function allTargetTypes() {
                const selected = selectedTypes();
                return selected.length > 0 ? selected : Object.keys(itemMaster);
            }

            function itemValue(type, label) {
                return type + ':' + label;
            }

            function renderItemOptions() {
                const current = new Set((itemsHidden.value || '').split(',').filter(Boolean));
                itemOptions.innerHTML = '';
                allTargetTypes().forEach(type => {
                    const group = document.createElement('div');
                    group.className = 'option-group';
                    const title = document.createElement('strong');
                    title.textContent = typeLabel(type);
                    group.appendChild(title);
                    itemMaster[type].forEach(label => {
                        const value = itemValue(type, label);
                        const option = document.createElement('label');
                        option.className = 'option-item';
                        const checkbox = document.createElement('input');
                        checkbox.type = 'checkbox';
                        checkbox.value = value;
                        checkbox.checked = current.has(value);
                        checkbox.addEventListener('change', updateSelectedItems);
                        option.appendChild(checkbox);
                        option.appendChild(document.createTextNode(label));
                        group.appendChild(option);
                    });
                    itemOptions.appendChild(group);
                });
                updateSelectedItems();
            }

            function typeLabel(type) {
                const labels = {
                    expense: '費用区分',
                    public: '公的負担区分',
                    private: '私的利用区分',
                    business: '事業収入区分',
                    investment: '金融・投資収益区分',
                    temporary: '一時収益区分',
                    nontaxable: '非課税・対象外収益区分'
                };
                return labels[type] || type;
            }

            function updateSelectedItems() {
                const checked = Array.from(itemOptions.querySelectorAll('input[type="checkbox"]:checked'));
                itemsHidden.value = checked.map(input => input.value).join(',');
                selectedItems.textContent = checked.length === 0 ? '全項目' : checked.map(input => input.value.split(':')[1]).join('、');
            }

            function parseLocalDate(value) {
                if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
                    return null;
                }
                const parts = value.split('-').map(Number);
                const date = new Date(parts[0], parts[1] - 1, parts[2]);
                if (date.getFullYear() !== parts[0] || date.getMonth() !== parts[1] - 1 || date.getDate() !== parts[2]) {
                    return null;
                }
                return date;
            }

            function showError(message) {
                errorMessage.textContent = message;
                errorModal.classList.add('show');
                errorModal.setAttribute('aria-hidden', 'false');
            }

            function closeError() {
                errorModal.classList.remove('show');
                errorModal.setAttribute('aria-hidden', 'true');
            }

            function validateSearch() {
                const from = dateFrom.value ? parseLocalDate(dateFrom.value) : null;
                const to = dateTo.value ? parseLocalDate(dateTo.value) : null;
                if (dateFrom.value && !from) {
                    showError('発生日Fromには存在する日付を指定してください。');
                    return false;
                }
                if (dateTo.value && !to) {
                    showError('発生日Toには存在する日付を指定してください。');
                    return false;
                }
                if (from && to && to < from) {
                    showError('発生日Toは発生日From以降の日付を指定してください。');
                    return false;
                }
                if (amountFrom.value && !/^\d+$/.test(amountFrom.value)) {
                    showError('金額Fromは数値で入力してください。');
                    return false;
                }
                if (amountTo.value && !/^\d+$/.test(amountTo.value)) {
                    showError('金額Toは数値で入力してください。');
                    return false;
                }
                if (amountFrom.value && amountTo.value && Number(amountTo.value) < Number(amountFrom.value)) {
                    showError('金額Toは金額From以上を指定してください。');
                    return false;
                }
                return true;
            }

            function renderPagination(element) {
                const total = Number(element.dataset.totalPages || '1');
                const current = Number(element.dataset.currentPage || '1');
                const inputId = element.dataset.pageInput;
                element.innerHTML = '';
                if (total <= 1) {
                    return;
                }

                const pages = [];
                const start = Math.max(1, Math.min(current - 2, total - 4));
                const end = Math.min(total, start + 4);
                if (start > 1) {
                    pages.push(1, '...');
                }
                for (let page = start; page <= end; page++) {
                    pages.push(page);
                }
                if (end < total) {
                    pages.push('...', total);
                }

                addPageButton(element, '前へ', Math.max(1, current - 1), inputId, current === 1);
                pages.forEach(page => {
                    if (page === '...') {
                        const span = document.createElement('span');
                        span.textContent = '...';
                        span.className = 'page-ellipsis';
                        element.appendChild(span);
                    } else {
                        addPageButton(element, String(page), page, inputId, page === current);
                    }
                });
                addPageButton(element, '次へ', Math.min(total, current + 1), inputId, current === total);
            }

            function addPageButton(container, label, page, inputId, disabled) {
                const button = document.createElement('button');
                button.type = 'button';
                button.textContent = label;
                button.disabled = disabled;
                button.className = disabled ? 'page-button is-current' : 'page-button';
                button.addEventListener('click', () => {
                    document.getElementById(inputId).value = String(page);
                    form.submit();
                });
                container.appendChild(button);
            }

            document.querySelectorAll('input[name="types"]').forEach(input => {
                input.addEventListener('change', () => {
                    itemsHidden.value = '';
                    renderItemOptions();
                });
            });

            itemsDisplay.addEventListener('click', event => {
                event.stopPropagation();
                itemOptions.classList.toggle('open');
                itemsDisplay.setAttribute('aria-expanded', itemOptions.classList.contains('open') ? 'true' : 'false');
            });

            itemOptions.addEventListener('click', event => event.stopPropagation());
            document.addEventListener('click', () => {
                itemOptions.classList.remove('open');
                itemsDisplay.setAttribute('aria-expanded', 'false');
            });

            document.getElementById('clearButton').addEventListener('click', () => {
                form.reset();
                itemsHidden.value = '';
                document.getElementById('incomePage').value = '1';
                document.getElementById('expensePage').value = '1';
                renderItemOptions();
                form.submit();
            });

            form.addEventListener('submit', event => {
                if (!validateSearch()) {
                    event.preventDefault();
                }
            });

            document.getElementById('closeErrorModal').addEventListener('click', closeError);
            errorModal.addEventListener('click', event => {
                if (event.target === errorModal) {
                    closeError();
                }
            });

            renderItemOptions();
            document.querySelectorAll('.pagination').forEach(renderPagination);
        })();
    </script>
</body>
</html>


