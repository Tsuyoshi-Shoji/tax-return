<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>損益一覧</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/profit-loss/profit-loss.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <main>
        <!-- 検索条件タイトル -->
        <h1>検索条件</h1>

        <!-- 検索条件container -->
        <div class="search-container">
            <form class="search-form" method="get">
                <!-- 日付From -->
                <div class="form-group">
                    <label for="dateFrom">日付From</label>
                    <input type="date" id="dateFrom" name="dateFrom" />
                </div>

                <!-- 日付To -->
                <div class="form-group">
                    <label for="dateTo">日付To</label>
                    <input type="date" id="dateTo" name="dateTo" />
                </div>

                <!-- 収益・費用チェックボックス -->
                <div class="form-group" style="grid-column: 1 / -1;">
                    <label style="margin-bottom: 10px;">区分</label>
                    <div class="checkbox-group">
                        <label>
                            <input type="checkbox" name="revenue" id="revenue" />
                            収益
                        </label>
                        <label>
                            <input type="checkbox" name="expense" id="expense" />
                            費用
                        </label>
                    </div>
                </div>

                <!-- 勘定項目プルダウン -->
                <div class="form-group">
                    <label for="accountItem">勘定項目</label>
                    <div class="custom-select" id="customSelect">
                        <div class="select-display">
                            <div class="selected-items" id="selectedItems">
                                <span style="color: #999;">-- 選択してください --</span>
                            </div>
                        </div>
                        <div class="select-options" id="selectOptions">
                            <div class="option-item">
                                <input type="checkbox" id="opt-sales" value="sales" />
                                <label for="opt-sales">売上高</label>
                            </div>
                            <div class="option-item">
                                <input type="checkbox" id="opt-service" value="service" />
                                <label for="opt-service">サービス売上</label>
                            </div>
                            <div class="option-item">
                                <input type="checkbox" id="opt-other" value="other" />
                                <label for="opt-other">その他</label>
                            </div>
                        </div>
                    </div>
                    <input type="hidden" id="accountItem" name="accountItem" />
                </div>

                <!-- 検索ボタンとリセットボタン -->
                <div class="search-button-group">
                    <button type="submit" class="search-button">検索</button>
                    <button type="reset" class="reset-button">リセット</button>
                </div>
            </form>
        </div>

        <!-- 損益一覧タイトル -->
        <h1>損益一覧</h1>

        <!-- 結果container -->
        <div class="container">

            <div class="summary">
                <div class="summary-card">
                    <h3>総収益</h3>
                    <div class="amount">¥0</div>
                </div>
                <div class="summary-card">
                    <h3>総費用</h3>
                    <div class="amount">¥0</div>
                </div>
                <div class="summary-card">
                    <h3>利益</h3>
                    <div class="amount">¥0</div>
                </div>
            </div>

            <h2>収益一覧</h2>
            <table>
                <thead>
                    <tr>
                        <th>日付</th>
                        <th>種類</th>
                        <th>詳細</th>
                        <th>金額</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td colspan="4" style="text-align: center; color: #999;">データなし</td>
                    </tr>
                </tbody>
            </table>

            <h2>費用一覧</h2>
            <table>
                <thead>
                    <tr>
                        <th>日付</th>
                        <th>種類</th>
                        <th>詳細</th>
                        <th>金額</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td colspan="4" style="text-align: center; color: #999;">データなし</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </main>

    <!-- エラーモーダル -->
    <div id="errorModal" class="modal">
        <div class="modal-content">
            <h2>入力エラー</h2>
            <p id="errorMessage">未来の日付を選択してください</p>
            <button class="modal-button" onclick="closeErrorModal()">OK</button>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />

    <script>
        // エラーモーダルの制御
        const errorModal = document.getElementById('errorModal');
        const errorMessage = document.getElementById('errorMessage');

        function showErrorModal(message) {
            errorMessage.textContent = message;
            errorModal.classList.add('show');
        }

        function closeErrorModal() {
            errorModal.classList.remove('show');
        }

        // モーダルの外側をクリックして閉じる
        errorModal.addEventListener('click', function(e) {
            if (e.target === errorModal) {
                closeErrorModal();
            }
        });

        // 日付の検証
        const dateFromInput = document.getElementById('dateFrom');
        const dateToInput = document.getElementById('dateTo');
        const searchForm = document.querySelector('.search-form');

        // 日付Toが変更されたときに検証
        dateToInput.addEventListener('change', function() {
            validateDates();
        });

        // 日付Fromが変更されたときに検証
        dateFromInput.addEventListener('change', function() {
            validateDates();
        });

        function validateDates() {
            const dateFrom = dateFromInput.value;
            const dateTo = dateToInput.value;

            // 両方の日付が入力されている場合のみ検証
            if (dateFrom && dateTo) {
                const fromDate = new Date(dateFrom);
                const toDate = new Date(dateTo);

                if (toDate < fromDate) {
                    showErrorModal('未来の日付を選択してください');
                    // エラー時は日付Toをクリア
                    dateToInput.value = '';
                }
            }
        }

        // フォーム送信時の検証
        searchForm.addEventListener('submit', function(e) {
            const dateFrom = dateFromInput.value;
            const dateTo = dateToInput.value;

            // 両方の日付が入力されている場合のみ検証
            if (dateFrom && dateTo) {
                const fromDate = new Date(dateFrom);
                const toDate = new Date(dateTo);

                if (toDate < fromDate) {
                    e.preventDefault();
                    showErrorModal('未来の日付を選択してください');
                    return false;
                }
            }
        });

        // カスタムプルダウンの機能
        const customSelect = document.getElementById('customSelect');
        const selectDisplay = customSelect.querySelector('.select-display');
        const selectOptions = document.getElementById('selectOptions');
        const selectedItems = document.getElementById('selectedItems');
        const checkboxes = selectOptions.querySelectorAll('input[type="checkbox"]');
        const hiddenInput = document.getElementById('accountItem');

        // プルダウンの表示/非表示を切り替え
        selectDisplay.addEventListener('click', function(e) {
            e.stopPropagation();
            selectOptions.classList.toggle('open');
        });

        // チェックボックスの変更を監視
        checkboxes.forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                updateSelectedItems();
                updateHiddenInput();
            });
        });

        // 選択状態を表示
        function updateSelectedItems() {
            const selected = Array.from(checkboxes)
                .filter(cb => cb.checked)
                .map(cb => cb.parentElement.querySelector('label').textContent.trim());

            selectedItems.innerHTML = '';

            if (selected.length === 0) {
                selectedItems.innerHTML = '<span style="color: #999;">-- 選択してください --</span>';
            } else {
                selected.forEach(item => {
                    const tag = document.createElement('span');
                    tag.className = 'selected-item';
                    tag.textContent = item;
                    selectedItems.appendChild(tag);
                });
            }
        }

        // 隠しフィールドに値を設定
        function updateHiddenInput() {
            const selectedValues = Array.from(checkboxes)
                .filter(cb => cb.checked)
                .map(cb => cb.value);
            hiddenInput.value = selectedValues.join(',');
        }

        // ページ外をクリックしたときプルダウンを閉じる
        document.addEventListener('click', function() {
            selectOptions.classList.remove('open');
        });

        // プルダウン内をクリックしたとき、プルダウンを開いたままにする
        selectOptions.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    </script>
</body>
</html>


