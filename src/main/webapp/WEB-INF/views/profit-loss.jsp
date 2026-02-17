<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>損益一覧</title>
    <style>
        body {
            margin: 0;
            padding: 60px 0 60px 0;
            font-family: Arial, sans-serif;
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }
        main {
            flex: 1;
            padding: 20px;
            background-color: #f5f5f5;
            width: 100%;
            box-sizing: border-box;
        }
        .container {
            max-width: 900px;
            margin: 0 auto;
            padding: 20px;
            background-color: white;
            border-radius: 8px;
        }
        h1 {
            text-align: center;
            margin: 20px 0 20px 0;
            color: #333;
        }
        .search-container {
            max-width: 900px;
            margin: 0 auto 20px auto;
            padding: 20px;
            background-color: white;
            border-radius: 8px;
        }
        .search-form {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }
        .form-group {
            display: flex;
            flex-direction: column;
        }
        .form-group label {
            font-weight: bold;
            font-size: 14px;
            margin-bottom: 5px;
            color: #333;
        }
        .form-group input,
        .form-group select {
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
            font-family: Arial, sans-serif;
            height: 36px;
            box-sizing: border-box;
        }
        .form-group input[type="date"] {
            height: 36px;
        }
        .custom-select {
            position: relative;
            display: inline-block;
            width: 100%;
        }
        .select-display {
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
            font-family: Arial, sans-serif;
            background-color: white;
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
            min-height: 38px;
        }
        .select-display::after {
            content: "▼";
            font-size: 12px;
            color: #333;
        }
        .select-options {
            position: absolute;
            top: 100%;
            left: 0;
            right: 0;
            border: 1px solid #ddd;
            border-radius: 4px;
            background-color: white;
            max-height: 200px;
            overflow-y: auto;
            z-index: 10;
            display: none;
            margin-top: 2px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .select-options.open {
            display: block;
        }
        .option-item {
            padding: 10px;
            border-bottom: 1px solid #f0f0f0;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .option-item:last-child {
            border-bottom: none;
        }
        .option-item:hover {
            background-color: #f9f9f9;
        }
        .option-item input[type="checkbox"] {
            cursor: pointer;
        }
        .option-item label {
            margin: 0;
            cursor: pointer;
            flex: 1;
        }
        .selected-items {
            display: flex;
            flex-wrap: wrap;
            gap: 5px;
            flex: 1;
        }
        .selected-item {
            background-color: #e0e0e0;
            padding: 2px 8px;
            border-radius: 3px;
            font-size: 13px;
            display: inline-block;
        }
        .checkbox-group {
            display: flex;
            gap: 15px;
            align-items: center;
        }
        .checkbox-group label {
            display: flex;
            align-items: center;
            gap: 5px;
            font-weight: bold;
            font-size: 14px;
            margin: 0;
            cursor: pointer;
        }
        .checkbox-group input[type="checkbox"] {
            cursor: pointer;
        }
        .search-button-group {
            grid-column: 1 / -1;
            display: flex;
            gap: 10px;
            justify-content: flex-end;
        }
        .search-button-group button {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-weight: bold;
            font-size: 14px;
        }
        .search-button {
            background-color: #4CAF50;
            color: white;
        }
        .search-button:hover {
            background-color: #45a049;
        }
        .reset-button {
            background-color: #f0f0f0;
            color: #333;
            border: 1px solid #ddd;
        }
        .reset-button:hover {
            background-color: #e0e0e0;
        }
        h1 {
            text-align: center;
            margin: 40px 0 30px 0;
        }
        .summary {
            display: grid;
            grid-template-columns: 1fr 1fr 1fr;
            gap: 15px;
            margin-bottom: 30px;
        }
        .summary-card {
            background-color: white;
            padding: 15px;
            border-radius: 4px;
            text-align: center;
            border: 1px solid #ddd;
        }
        .summary-card h3 {
            margin: 0 0 10px 0;
            color: #666;
        }
        .summary-card .amount {
            font-size: 20px;
            font-weight: bold;
            color: #333;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        table th {
            background-color: #f0f0f0;
            padding: 10px;
            text-align: left;
            border: 1px solid #ddd;
        }
        table td {
            padding: 10px;
            border: 1px solid #ddd;
        }
        table tr:hover {
            background-color: #f5f5f5;
        }
        /* モーダルスタイル */
        .modal {
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.5);
            backdrop-filter: blur(2px);
        }
        .modal.show {
            display: flex;
            align-items: center;
            justify-content: center;
            animation: fadeIn 0.3s ease-in-out;
        }
        @keyframes fadeIn {
            from {
                opacity: 0;
            }
            to {
                opacity: 1;
            }
        }
        @keyframes slideUp {
            from {
                transform: translateY(30px);
                opacity: 0;
            }
            to {
                transform: translateY(0);
                opacity: 1;
            }
        }
        .modal-content {
            background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
            padding: 40px 35px;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.05);
            text-align: center;
            max-width: 420px;
            animation: slideUp 0.3s ease-in-out;
            border-top: 4px solid #d32f2f;
        }
        .modal-content h2 {
            color: #d32f2f;
            margin: 0 0 10px 0;
            font-size: 24px;
            font-weight: 600;
            text-align: left;
        }
        .modal-content p {
            color: #555;
            margin: 0 0 25px 0;
            font-size: 15px;
            line-height: 1.6;
        }
        .modal-button {
            background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
            color: white;
            padding: 12px 40px;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-weight: 600;
            font-size: 15px;
            transition: all 0.3s ease;
            box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
        }
        .modal-button:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(76, 175, 80, 0.4);
        }
        .modal-button:active {
            transform: translateY(0);
        }
    </style>
</head>
<body>
    <jsp:include page="header.jsp" />

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

    <jsp:include page="footer.jsp" />

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
