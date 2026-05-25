<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>収益登録</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/income/income.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />
    <main>
        <div class="container income-container">
            <div class="page-header">
                <h1>収益登録</h1>
                <p class="page-subtitle">収益内容を入力して登録します。</p>
            </div>

            <form id="incomeForm" method="post" action="${pageContext.request.contextPath}/income" novalidate>
                <c:if test="${not empty _csrf}">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                </c:if>
                <input type="hidden" id="incomeId" name="id" value="<c:out value='${not empty incomeForm.id ? incomeForm.id : incomeEdit.id}' />" />
                <input type="hidden" name="returnTo" value="<c:out value='${not empty incomeForm.returnTo ? incomeForm.returnTo : param.returnTo}' />" />
                <div class="form-group full">
                    <span class="group-label">区分 <span class="required">*</span></span>
                    <div class="radio-group" role="radiogroup" aria-label="収益区分">
                        <label class="radio-item" for="typeBusiness">
                            <input type="radio" id="typeBusiness" name="incomeType" value="business" />
                            <span>事業収入</span>
                        </label>
                        <label class="radio-item" for="typeInvestment">
                            <input type="radio" id="typeInvestment" name="incomeType" value="investment" />
                            <span>金融・投資収益</span>
                        </label>
                        <label class="radio-item" for="typeTemporary">
                            <input type="radio" id="typeTemporary" name="incomeType" value="temporary" />
                            <span>一時収益</span>
                        </label>
                        <label class="radio-item" for="typeNontaxable">
                            <input type="radio" id="typeNontaxable" name="incomeType" value="nontaxable" />
                            <span>非課税・対象外収益</span>
                        </label>
                    </div>
                    <p id="incomeTypeError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group full">
                    <label for="category">項目 <span class="required">*</span></label>
                    <select id="category" name="category" required disabled>
                        <option value="">区分を選択してください</option>
                    </select>
                    <p id="categoryError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group">
                    <label for="date">収益発生日 <span class="required">*</span></label>
                    <input type="date" id="date" name="date" value="<c:out value='${not empty incomeForm.date ? incomeForm.date : incomeEdit.date}' />" required />
                    <p id="dateError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group">
                    <label for="amount">金額（円） <span class="required">*</span></label>
                    <div class="amount-wrapper">
                        <span aria-hidden="true">¥</span>
                        <input type="number" id="amount" name="amount" min="1" step="1" inputmode="numeric" placeholder="例: 12000" value="<c:out value='${not empty incomeForm.amount ? incomeForm.amount : incomeEdit.amount}' />" required />
                    </div>
                    <p id="amountError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group full">
                    <span class="group-label">受取方法 <span class="required">*</span></span>
                    <div class="radio-group" role="radiogroup" aria-label="受取方法">
                        <label class="radio-item" for="receiveMethod1">
                            <input type="radio" id="receiveMethod1" name="receiveMethodId" value="1" />
                            <span>現金</span>
                        </label>
                        <label class="radio-item" for="receiveMethod2">
                            <input type="radio" id="receiveMethod2" name="receiveMethodId" value="2" />
                            <span>クレジットカード</span>
                        </label>
                        <label class="radio-item" for="receiveMethod3">
                            <input type="radio" id="receiveMethod3" name="receiveMethodId" value="3" />
                            <span>電子決済</span>
                        </label>
                        <label class="radio-item" for="receiveMethod4">
                            <input type="radio" id="receiveMethod4" name="receiveMethodId" value="4" />
                            <span>銀行振込</span>
                        </label>
                        <label class="radio-item" for="receiveMethod5">
                            <input type="radio" id="receiveMethod5" name="receiveMethodId" value="5" />
                            <span>その他</span>
                        </label>
                    </div>
                    <p id="receiveMethodIdError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="form-group full">
                    <label class="checkbox-item" for="businessTartgetExcluded">
                        <input type="checkbox" id="businessTartgetExcluded" name="businessTartget" value="false" />
                        <span>申告対象外（損益計算から除外）として登録する</span>
                    </label>
                </div>

                <div class="textarea-group">
                    <label for="details">収益詳細 <span class="required">*</span></label>
                    <textarea id="details" name="details" rows="4" maxlength="255" placeholder="収益の内容を1〜255文字で入力してください" required><c:out value='${not empty incomeForm.details ? incomeForm.details : incomeEdit.details}' /></textarea>
                    <div class="char-count"><span id="detailsCount"><c:out value='${not empty incomeForm.details ? fn:length(incomeForm.details) : (not empty incomeEdit.details ? fn:length(incomeEdit.details) : 0)}' /></span>/255</div>
                    <p id="detailsError" class="error-message" aria-live="polite"></p>
                </div>

                <div class="button-group">
                    <button type="button" class="btn-secondary" id="clearButton">クリア</button>
                    <button type="submit" class="btn-primary"><c:out value='${empty incomeEdit.id ? "登録" : "更新"}' /></button>
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
            <h2 id="highAmountModalTitle">高額収益の確認</h2>
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
                business: [
                    { value: '1', label: '商品売上' },
                    { value: '2', label: 'サービス売上' },
                    { value: '3', label: '制作売上' },
                    { value: '4', label: '開発売上(業務委託)' },
                    { value: '5', label: '開発売上(請負)' },
                    { value: '6', label: 'コンサル売上' },
                    { value: '7', label: '広告収益' },
                    { value: '8', label: 'アフィリエイト' },
                    { value: '9', label: 'YouTube収益' },
                    { value: '10', label: 'SNS収益' },
                    { value: '11', label: 'ライセンス収益' },
                    { value: '12', label: 'サブスク収益' },
                    { value: '13', label: 'システム利用料' },
                    { value: '14', label: '手数料収益' },
                    { value: '15', label: 'イベント売上' },
                    { value: '16', label: 'その他事業収益' }
                ],
                investment: [
                    { value: '17', label: '株式配当' },
                    { value: '18', label: '売却益' },
                    { value: '19', label: 'FX収益' },
                    { value: '20', label: '仮想通貨利益' },
                    { value: '21', label: 'ステーキング' },
                    { value: '22', label: '投資信託収益' },
                    { value: '23', label: '銀行利息' },
                    { value: '24', label: '外貨利息' },
                    { value: '25', label: 'その他金融・投資収益' }
                ],
                temporary: [
                    { value: '26', label: '不用品売却' },
                    { value: '27', label: '臨時収入' },
                    { value: '28', label: '保険金' },
                    { value: '29', label: 'お祝い金' },
                    { value: '30', label: '懸賞' },
                    { value: '31', label: '返戻金' },
                    { value: '32', label: 'その他一時収益' }
                ],
                nontaxable: [
                    { value: '33', label: '借入金' },
                    { value: '34', label: '資本投入費' },
                    { value: '35', label: '建て替え金' },
                    { value: '36', label: '預かり金' },
                    { value: '37', label: '振替' },
                    { value: '38', label: '補助・給付金' },
                    { value: '39', label: 'その他非課税・対象外収益' }
                ]
            };

            const form = document.getElementById('incomeForm');
            const categorySelect = document.getElementById('category');
            const dateInput = document.getElementById('date');
            const amountInput = document.getElementById('amount');
            const detailsInput = document.getElementById('details');
            const detailsCount = document.getElementById('detailsCount');
            const clearButton = document.getElementById('clearButton');
            const confirmHighAmountButton = document.getElementById('confirmHighAmountButton');
            const initialIncomeType = '<c:out value="${not empty incomeForm.incomeType ? incomeForm.incomeType : incomeEdit.incomeType}" />';
            const initialCategory = '<c:out value="${not empty incomeForm.category ? incomeForm.category : incomeEdit.subcategoryId}" />';
            const initialReceiveMethodId = '<c:out value="${not empty incomeForm.receiveMethodId ? incomeForm.receiveMethodId : incomeEdit.receiveMethodId}" />';
            const initialBusinessTartget = '<c:out value="${not empty incomeForm.businessTartget ? incomeForm.businessTartget : incomeEdit.businessTartget}" />';

            function getSelectedType() {
                const selected = document.querySelector('input[name="incomeType"]:checked');
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
                ['incomeType', 'category', 'date', 'amount', 'details', 'receiveMethodId'].forEach(id => setError(id, ''));
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
                const receiveMethodValue = document.querySelector('input[name="receiveMethodId"]:checked');

                if (!type) {
                    setError('incomeType', '区分を選択してください。');
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
                    setError('date', '収益発生日を選択してください。');
                    errors.push('収益発生日を選択してください。');
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

                if (!detailsValue) {
                    setError('details', '収益詳細を入力してください。');
                    errors.push('収益詳細を入力してください。');
                } else if (detailsValue.length < 1 || detailsValue.length > 255) {
                    setError('details', '収益詳細は1〜255文字で入力してください。');
                    errors.push('収益詳細は1〜255文字で入力してください。');
                } else if (containsInvalidDetailsCharacter(detailsValue)) {
                    setError('details', '収益詳細に使用できない文字が含まれています。');
                    errors.push('収益詳細に使用できない文字が含まれています。');
                }

                if (!receiveMethodValue) {
                    setError('receiveMethodId', '受取方法を選択してください。');
                    errors.push('受取方法を選択してください。');
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

            document.querySelectorAll('input[name="incomeType"]').forEach(radio => {
                radio.addEventListener('change', () => {
                    populateCategories(getSelectedType());
                    setError('incomeType', '');
                    setError('category', '');
                });
            });

            categorySelect.addEventListener('change', () => setError('category', ''));
            dateInput.addEventListener('change', () => setError('date', ''));
            amountInput.addEventListener('input', () => setError('amount', ''));
            document.querySelectorAll('input[name="receiveMethodId"]').forEach(radio => {
                radio.addEventListener('change', () => setError('receiveMethodId', ''));
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
            });

            form.addEventListener('submit', event => {
                event.preventDefault();
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

            if (initialIncomeType) {
                const typeRadio = document.querySelector('input[name="incomeType"][value="' + initialIncomeType + '"]');
                if (typeRadio) {
                    typeRadio.checked = true;
                }
                populateCategories(initialIncomeType);
                if (initialCategory) {
                    categorySelect.value = initialCategory;
                }
            } else {
                populateCategories('');
            }
            if (initialReceiveMethodId) {
                const receiveRadio = document.querySelector('input[name="receiveMethodId"][value="' + initialReceiveMethodId + '"]');
                if (receiveRadio) {
                    receiveRadio.checked = true;
                }
            }
            if (initialBusinessTartget === 'false') {
                const businessTartgetExcludedCheckbox = document.getElementById('businessTartgetExcluded');
                if (businessTartgetExcludedCheckbox) {
                    businessTartgetExcludedCheckbox.checked = true;
                }
            }
        })();
    </script>
</body>
</html>

