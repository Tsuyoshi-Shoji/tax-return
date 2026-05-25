<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>支出登録</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/expense/expense.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />
    <main>
        <div class="container expense-container">
            <div class="page-header">
                <h1>支出登録</h1>
                <p class="page-subtitle">支出内容を入力して登録します。</p>
            </div>

            <form id="expenseForm" method="post" action="${pageContext.request.contextPath}/expense" novalidate>
                <c:if test="${not empty _csrf}">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                </c:if>
                <input type="hidden" id="expenseId" name="id" value="<c:out value='${not empty expenseForm.id ? expenseForm.id : expenseEdit.id}' />" />
                <input type="hidden" name="returnTo" value="<c:out value='${not empty expenseForm.returnTo ? expenseForm.returnTo : param.returnTo}' />" />
                <div class="form-group full">
                    <span class="group-label">区分 <span class="required">*</span></span>
                    <div class="radio-group" role="radiogroup" aria-label="支出区分">
                        <label class="radio-item" for="typeExpense">
                            <input type="radio" id="typeExpense" name="expenseType" value="expense" />
                            <span>経費</span>
                        </label>
                        <label class="radio-item" for="typePublic">
                            <input type="radio" id="typePublic" name="expenseType" value="public" />
                            <span>公的負担</span>
                        </label>
                        <label class="radio-item" for="typePrivate">
                            <input type="radio" id="typePrivate" name="expenseType" value="private" />
                            <span>私的利用</span>
                        </label>
                    </div>
                    <p id="expenseTypeError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group full">
                    <label for="category">項目 <span class="required">*</span></label>
                    <select id="category" name="category" required disabled>
                        <option value="">区分を選択してください</option>
                    </select>
                    <p id="categoryError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group">
                    <label for="date">支出発生日 <span class="required">*</span></label>
                    <input type="date" id="date" name="date" value="<c:out value='${not empty expenseForm.date ? expenseForm.date : expenseEdit.date}' />" required />
                    <p id="dateError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group">
                    <label for="amount">金額（円） <span class="required">*</span></label>
                    <div class="amount-wrapper">
                        <span aria-hidden="true">¥</span>
                        <input type="number" id="amount" name="amount" min="1" step="1" inputmode="numeric" placeholder="例: 12000" value="<c:out value='${not empty expenseForm.amount ? expenseForm.amount : expenseEdit.amount}' />" required />
                    </div>
                    <p id="amountError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group full">
                    <span class="group-label">支払方法 <span class="required">*</span></span>
                    <div class="radio-group" role="radiogroup" aria-label="支払方法">
                        <label class="radio-item" for="paymentMethod1">
                            <input type="radio" id="paymentMethod1" name="paymentMethodId" value="1" />
                            <span>現金</span>
                        </label>
                        <label class="radio-item" for="paymentMethod2">
                            <input type="radio" id="paymentMethod2" name="paymentMethodId" value="2" />
                            <span>クレジットカード</span>
                        </label>
                        <label class="radio-item" for="paymentMethod3">
                            <input type="radio" id="paymentMethod3" name="paymentMethodId" value="3" />
                            <span>電子決済</span>
                        </label>
                        <label class="radio-item" for="paymentMethod4">
                            <input type="radio" id="paymentMethod4" name="paymentMethodId" value="4" />
                            <span>銀行振込</span>
                        </label>
                        <label class="radio-item" for="paymentMethod5">
                            <input type="radio" id="paymentMethod5" name="paymentMethodId" value="5" />
                            <span>その他</span>
                        </label>
                    </div>
                    <p id="paymentMethodIdError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group full apportionment-group">
                    <input type="hidden" id="defaultBusinessUseRatio" name="defaultBusinessUseRatio" value="${defaultBusinessUseRatio}" />
                    <input type="hidden" id="deductibleAmount" name="deductibleAmount" value="" />
                    <c:set var="homeApportionmentChecked" value="${(not empty expenseForm && expenseForm.homeApportionment) || (empty expenseForm && expenseEdit.homeApportionment)}" />
                    <label class="checkbox-item" for="homeApportionment">
                        <input type="checkbox" id="homeApportionment" name="homeApportionment" value="true" ${homeApportionmentChecked ? 'checked="checked"' : ''} />
                        <span>家事按分を適用する</span>
                    </label>
                    <div class="apportionment-panel" id="apportionmentPanel" aria-live="polite">
                        <p class="apportionment-note">
                            設定画面で保持している固定按分率
                            <strong><span id="businessUseRatioLabel"><c:out value="${defaultBusinessUseRatio}" /></span>%</strong>
                            を使用して経費対象金額を計算します。
                        </p>
                        <dl class="apportionment-summary">
                            <div>
                                <dt>支出テーブル保存金額</dt>
                                <dd id="apportionmentAmountLabel">0円</dd>
                            </div>
                            <div>
                                <dt>経費対象金額</dt>
                                <dd id="deductibleAmountLabel">0円</dd>
                            </div>
                        </dl>
                    </div>
                    <p id="apportionmentError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="textarea-group">
                    <label for="details">支出詳細 <span class="required">*</span></label>
                    <textarea id="details" name="details" rows="4" maxlength="255" placeholder="支出の内容を1〜255文字で入力してください" required><c:out value='${not empty expenseForm.details ? expenseForm.details : expenseEdit.details}' /></textarea>
                    <div class="char-count"><span id="detailsCount"><c:out value='${not empty expenseForm.details ? fn:length(expenseForm.details) : (not empty expenseEdit.details ? fn:length(expenseEdit.details) : 0)}' /></span>/255</div>
                    <p id="detailsError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="button-group">
                    <button type="button" class="btn-secondary" id="clearButton">クリア</button>
                    <button type="submit" class="btn-primary"><c:out value='${empty expenseEdit.id ? "登録" : "更新"}' /></button>
                </div>
            </form>
        </div>
    </main>

    <div class="modal" id="validationModal" aria-hidden="true" role="dialog" aria-modal="true" aria-labelledby="validationModalTitle">
        <div class="modal-content modal-error">
            <h2 id="validationModalTitle">入力エラー</h2>
            <p id="validationModalMessage">入力内容に誤りがあります。各項目を確認してください。</p>
            <div class="modal-actions">
                <button type="button" class="btn-secondary" data-close-modal="validationModal">閉じる</button>
            </div>
        </div>
    </div>

    <div class="modal" id="highAmountModal" aria-hidden="true" role="dialog" aria-modal="true" aria-labelledby="highAmountModalTitle">
        <div class="modal-content modal-warning">
            <h2 id="highAmountModalTitle">高額支出の確認</h2>
            <p id="highAmountModalMessage">金額が100,000,000円以上です。内容に誤りがないか確認してください。</p>
            <div class="modal-actions">
                <button type="button" class="btn-secondary" data-close-modal="highAmountModal">キャンセル</button>
                <button type="button" class="btn-primary" id="confirmHighAmountButton">確認して送信</button>
            </div>
        </div>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="modal is-open" id="serverSuccessModal" aria-hidden="false" role="dialog" aria-modal="true" aria-labelledby="serverSuccessModalTitle">
            <div class="modal-content modal-success">
                <h2 id="serverSuccessModalTitle">登録完了</h2>
                <p><c:out value="${successMessage}" /></p>
                <div class="modal-actions">
                    <button type="button" class="btn-primary" data-close-modal="serverSuccessModal">閉じる</button>
                </div>
            </div>
        </div>
    </c:if>

    <c:if test="${not empty failureMessage}">
        <div class="modal is-open" id="serverFailureModal" aria-hidden="false" role="dialog" aria-modal="true" aria-labelledby="serverFailureModalTitle">
            <div class="modal-content modal-error">
                <h2 id="serverFailureModalTitle">登録失敗</h2>
                <p><c:out value="${failureMessage}" /></p>
                <div class="modal-actions">
                    <button type="button" class="btn-secondary" data-close-modal="serverFailureModal">閉じる</button>
                </div>
            </div>
        </div>
    </c:if>

    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />

    <script>
        (function () {
            'use strict';

            const categoryMasters = {
                expense: [
                    { value: '1', label: '通信・IT利用料' },
                    { value: '2', label: '設備・機材' },
                    { value: '3', label: '外注費' },
                    { value: '4', label: '広告宣伝費' },
                    { value: '5', label: '旅費交通費' },
                    { value: '6', label: '会議費' },
                    { value: '7', label: '接待交際費' },
                    { value: '8', label: '地代家賃' },
                    { value: '9', label: '水道光熱費' },
                    { value: '10', label: 'スペース利用料' },
                    { value: '11', label: '学習・教材費' },
                    { value: '12', label: '手数料' },
                    { value: '13', label: '保険（事業保険・賠償責任保険）' },
                    { value: '14', label: '税務相談費' },
                    { value: '15', label: '法務相談費' },
                    { value: '16', label: '消耗品費' },
                    { value: '17', label: '雑費' }
                ],
                public: [
                    { value: '18', label: '所得税' },
                    { value: '19', label: '住民税' },
                    { value: '20', label: '個人事業税' },
                    { value: '21', label: '固定資産税' },
                    { value: '22', label: '自動車税' },
                    { value: '23', label: '消費税' },
                    { value: '24', label: '国民健康保険' },
                    { value: '25', label: '国民年金' },
                    { value: '26', label: '介護保険' },
                    { value: '27', label: '証明書発行手数料' },
                    { value: '28', label: '行政手続料' },
                    { value: '29', label: '許可申請費' },
                    { value: '30', label: 'その他公的支出（罰金・延滞税、各種協会費など）' }
                ],
                private: [
                    { value: '31', label: '生活費' },
                    { value: '32', label: '趣味・娯楽' },
                    { value: '33', label: '教育費' },
                    { value: '34', label: '医療費' },
                    { value: '35', label: '交通費' },
                    { value: '36', label: '交際費' },
                    { value: '37', label: 'その他私費(雑費・貯金・投資など)' }
                ]
            };

            const form = document.getElementById('expenseForm');
            const categorySelect = document.getElementById('category');
            const dateInput = document.getElementById('date');
            const amountInput = document.getElementById('amount');
            const detailsInput = document.getElementById('details');
            const detailsCount = document.getElementById('detailsCount');
            const clearButton = document.getElementById('clearButton');
            const confirmHighAmountButton = document.getElementById('confirmHighAmountButton');
            const homeApportionmentInput = document.getElementById('homeApportionment');
            const initialExpenseType = '<c:out value="${not empty expenseForm.expenseType ? expenseForm.expenseType : expenseEdit.expenseType}" />';
            const initialCategory = '<c:out value="${not empty expenseForm.category ? expenseForm.category : expenseEdit.subcategoryId}" />';
            const initialPaymentMethodId = '<c:out value="${not empty expenseForm.paymentMethodId ? expenseForm.paymentMethodId : expenseEdit.paymentMethodId}" />';
            const defaultBusinessUseRatioInput = document.getElementById('defaultBusinessUseRatio');
            const deductibleAmountInput = document.getElementById('deductibleAmount');
            const businessUseRatioLabel = document.getElementById('businessUseRatioLabel');
            const apportionmentAmountLabel = document.getElementById('apportionmentAmountLabel');
            const deductibleAmountLabel = document.getElementById('deductibleAmountLabel');
            const apportionmentPanel = document.getElementById('apportionmentPanel');

            const defaultBusinessUseRatio = parseBusinessUseRatio(defaultBusinessUseRatioInput.value);
            businessUseRatioLabel.textContent = formatRatio(defaultBusinessUseRatio);

            function getSelectedType() {
                const selected = document.querySelector('input[name="expenseType"]:checked');
                return selected ? selected.value : '';
            }

            function populateCategories(type) {
                categorySelect.innerHTML = '';

                const initialOption = document.createElement('option');
                initialOption.value = '';
                initialOption.textContent = type ? '-- 選択してください --' : '区分を選択してください';
                categorySelect.appendChild(initialOption);

                if (!type || !categoryMasters[type]) {
                    categorySelect.disabled = true;
                    return;
                }

                categoryMasters[type].forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.value;
                    option.textContent = item.label;
                    categorySelect.appendChild(option);
                });

                categorySelect.disabled = false;
            }

            function setError(id, message) {
                const element = document.getElementById(id + 'Error');
                if (!element) {
                    return;
                }
                element.textContent = message || '';
                element.classList.toggle('is-visible', Boolean(message));
            }

            function clearErrors() {
                ['expenseType', 'category', 'date', 'amount', 'details', 'apportionment', 'paymentMethodId'].forEach(id => setError(id, ''));
            }

            function parseBusinessUseRatio(value) {
                const ratio = Number(value);
                if (!Number.isFinite(ratio) || ratio < 0 || ratio > 100) {
                    return 100;
                }
                return ratio;
            }

            function formatRatio(value) {
                return value.toLocaleString('ja-JP', {
                    minimumFractionDigits: 0,
                    maximumFractionDigits: 2
                });
            }

            function parseAmount(value) {
                if (!/^\d+$/.test(value || '')) {
                    return 0;
                }
                return Number(value);
            }

            function formatYen(value) {
                return Math.max(0, Number(value || 0)).toLocaleString('ja-JP') + '円';
            }

            function calculateDeductibleAmount(amount, shouldApplyApportionment) {
                if (!shouldApplyApportionment) {
                    return amount;
                }
                return Math.floor(amount * defaultBusinessUseRatio / 100);
            }

            function updateApportionmentPreview() {
                const amount = parseAmount(amountInput.value.trim());
                const shouldApplyApportionment = homeApportionmentInput.checked;
                const deductibleAmount = calculateDeductibleAmount(amount, shouldApplyApportionment);

                deductibleAmountInput.value = amount > 0 ? String(deductibleAmount) : '';
                apportionmentAmountLabel.textContent = formatYen(amount);
                deductibleAmountLabel.textContent = formatYen(deductibleAmount);
                apportionmentPanel.classList.toggle('is-active', shouldApplyApportionment);
            }

            function isValidCategory(type, category) {
                return Boolean(type && category && categoryMasters[type] && categoryMasters[type].some(item => item.value === category));
            }

            function isValidDateValue(value) {
                if (!value) {
                    return false;
                }
                const datePattern = /^\d{4}-\d{2}-\d{2}$/;
                if (!datePattern.test(value)) {
                    return false;
                }
                const parts = value.split('-').map(Number);
                const year = parts[0];
                const month = parts[1];
                const day = parts[2];
                const selectedDate = new Date(year, month - 1, day);
                if (Number.isNaN(selectedDate.getTime())) {
                    return false;
                }
                return selectedDate.getFullYear() === year
                    && selectedDate.getMonth() === month - 1
                    && selectedDate.getDate() === day;
            }

            function containsInvalidDetailsCharacter(value) {
                return /[<>"'`\\]/.test(value);
            }

            function validateForm() {
                clearErrors();
                const errors = [];
                const type = getSelectedType();
                const category = categorySelect.value;
                const dateValue = dateInput.value;
                const amountValue = amountInput.value.trim();
                const detailsValue = detailsInput.value.trim();
                const paymentMethodValue = document.querySelector('input[name="paymentMethodId"]:checked');

                if (!type) {
                    setError('expenseType', '区分を選択してください。');
                    errors.push('区分を選択してください。');
                }

                if (!category) {
                    setError('category', '項目を選択してください。');
                    errors.push('項目を選択してください。');
                } else if (!isValidCategory(type, category)) {
                    setError('category', '選択された項目がマスタに存在しません。');
                    errors.push('選択された項目がマスタに存在しません。');
                }

                if (!dateValue) {
                    setError('date', '支出発生日を選択してください。');
                    errors.push('支出発生日を選択してください。');
                } else if (!isValidDateValue(dateValue)) {
                    setError('date', '存在する日付を選択してください。');
                    errors.push('存在する日付を選択してください。');
                } else {
                    const dateParts = dateValue.split('-').map(Number);
                    const selectedDate = new Date(dateParts[0], dateParts[1] - 1, dateParts[2]);
                    const today = new Date();
                    today.setHours(0, 0, 0, 0);
                    if (selectedDate > today) {
                        setError('date', '登録日より未来の日付は選択できません。');
                        errors.push('登録日より未来の日付は選択できません。');
                    }
                }

                if (!amountValue) {
                    setError('amount', '金額を入力してください。');
                    errors.push('金額を入力してください。');
                } else if (!/^\d+$/.test(amountValue)) {
                    setError('amount', '金額は数値のみで入力してください。');
                    errors.push('金額は数値のみで入力してください。');
                } else if (Number(amountValue) < 1) {
                    setError('amount', '金額は1円以上で入力してください。');
                    errors.push('金額は1円以上で入力してください。');
                }

                if (!Number.isFinite(defaultBusinessUseRatio) || defaultBusinessUseRatio < 0 || defaultBusinessUseRatio > 100) {
                    setError('apportionment', '設定されている家事按分率が不正です。設定画面で0〜100%の範囲に修正してください。');
                    errors.push('設定されている家事按分率が不正です。');
                }

                if (!paymentMethodValue) {
                    setError('paymentMethodId', '支払方法を選択してください。');
                    errors.push('支払方法を選択してください。');
                }

                if (!detailsValue) {
                    setError('details', '支出詳細を入力してください。');
                    errors.push('支出詳細を入力してください。');
                } else if (detailsValue.length < 1 || detailsValue.length > 255) {
                    setError('details', '支出詳細は1〜255文字で入力してください。');
                    errors.push('支出詳細は1〜255文字で入力してください。');
                } else if (containsInvalidDetailsCharacter(detailsValue)) {
                    setError('details', '支出詳細に使用できない文字が含まれています。');
                    errors.push('支出詳細に使用できない文字が含まれています。');
                }

                return errors;
            }

            function openModal(id) {
                const modal = document.getElementById(id);
                if (modal) {
                    modal.classList.add('is-open');
                    modal.setAttribute('aria-hidden', 'false');
                }
            }

            function closeModal(id) {
                const modal = document.getElementById(id);
                if (modal) {
                    modal.classList.remove('is-open');
                    modal.setAttribute('aria-hidden', 'true');
                }
            }

            function submitWithoutValidationLoop() {
                HTMLFormElement.prototype.submit.call(form);
            }

            document.querySelectorAll('input[name="expenseType"]').forEach(radio => {
                radio.addEventListener('change', () => {
                    populateCategories(getSelectedType());
                    setError('expenseType', '');
                    setError('category', '');
                });
            });

            categorySelect.addEventListener('change', () => setError('category', ''));
            dateInput.addEventListener('change', () => setError('date', ''));
            document.querySelectorAll('input[name="paymentMethodId"]').forEach(radio => {
                radio.addEventListener('change', () => setError('paymentMethodId', ''));
            });
            amountInput.addEventListener('input', () => {
                setError('amount', '');
                updateApportionmentPreview();
            });
            homeApportionmentInput.addEventListener('change', () => {
                setError('apportionment', '');
                updateApportionmentPreview();
            });
            detailsInput.addEventListener('input', () => {
                detailsCount.textContent = String(detailsInput.value.length);
                setError('details', '');
            });

            clearButton.addEventListener('click', () => {
                form.reset();
                populateCategories('');
                clearErrors();
                detailsCount.textContent = '0';
                updateApportionmentPreview();
            });

            form.addEventListener('submit', event => {
                event.preventDefault();
                updateApportionmentPreview();
                const errors = validateForm();
                if (errors.length > 0) {
                    document.getElementById('validationModalMessage').textContent = errors[0];
                    openModal('validationModal');
                    return;
                }

                if (Number(amountInput.value) >= 100000000) {
                    document.getElementById('highAmountModalMessage').textContent = '金額が' + Number(amountInput.value).toLocaleString('ja-JP') + '円です。内容に誤りがないか確認してください。';
                    openModal('highAmountModal');
                    return;
                }

                submitWithoutValidationLoop();
            });

            confirmHighAmountButton.addEventListener('click', () => {
                closeModal('highAmountModal');
                updateApportionmentPreview();
                submitWithoutValidationLoop();
            });

            document.querySelectorAll('[data-close-modal]').forEach(button => {
                button.addEventListener('click', () => closeModal(button.getAttribute('data-close-modal')));
            });

            document.querySelectorAll('.modal').forEach(modal => {
                modal.addEventListener('click', event => {
                    if (event.target === modal) {
                        closeModal(modal.id);
                    }
                });
            });

            if (initialExpenseType) {
                const typeRadio = document.querySelector('input[name="expenseType"][value="' + initialExpenseType + '"]');
                if (typeRadio) {
                    typeRadio.checked = true;
                }
                populateCategories(initialExpenseType);
                if (initialCategory) {
                    categorySelect.value = initialCategory;
                }
            } else {
                populateCategories('');
            }
            if (initialPaymentMethodId) {
                const paymentRadio = document.querySelector('input[name="paymentMethodId"][value="' + initialPaymentMethodId + '"]');
                if (paymentRadio) {
                    paymentRadio.checked = true;
                }
            }
            updateApportionmentPreview();
        })();
    </script>
</body>
</html>


