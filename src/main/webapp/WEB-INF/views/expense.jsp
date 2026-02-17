<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>経費登録</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: Arial, sans-serif;
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }
        main {
            flex: 1;
            padding: 20px;
            display: flex;
            flex-direction: column;
            align-items: center;
            background-color: #f5f5f5;
            width: 100%;
            box-sizing: border-box;
        }
        .container {
            width: 100%;
            max-width: 600px;
            padding: 30px;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            box-sizing: border-box;
        }
        h1 {
            text-align: center;
            margin: 40px 0 30px 0;
        }
        .form-group {
            margin-bottom: 20px;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
            align-items: center;
        }
        .form-group.full {
            grid-column: 1 / -1;
        }
        .form-group label {
            font-weight: bold;
            font-size: 14px;
        }
        .form-group label .required {
            color: red;
            margin-left: 2px;
        }
        .form-group input,
        .form-group select,
        .form-group textarea {
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
            font-family: Arial, sans-serif;
        }
        .form-group.full input,
        .form-group.full select,
        .form-group.full textarea {
            grid-column: 1 / -1;
        }
        .textarea-group {
            grid-column: 1 / -1;
        }
        .textarea-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            font-size: 14px;
        }
        .textarea-group textarea {
            width: 100%;
            box-sizing: border-box;
            resize: vertical;
        }
        .button-group {
            margin-top: 30px;
            display: flex;
            justify-content: flex-end;
        }
        button {
            background-color: #4CAF50;
            color: white;
            padding: 12px 30px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-weight: bold;
        }
        button:hover {
            background-color: #45a049;
        }
        /* number input のspinner非表示 */
        input[type="number"] {
            -moz-appearance: textfield;
        }
        input[type="number"]::-webkit-outer-spin-button,
        input[type="number"]::-webkit-inner-spin-button {
            -webkit-appearance: none;
            margin: 0;
        }
        .amount-wrapper {
            display: flex;
            align-items: center;
            border: 1px solid #ddd;
            border-radius: 4px;
            overflow: hidden;
        }
        .amount-wrapper span {
            padding: 10px 10px;
            background-color: #f5f5f5;
            border-right: 1px solid #ddd;
            font-weight: bold;
            color: #333;
        }
        .amount-wrapper input[type="number"] {
            border: none;
            flex: 1;
            padding: 10px;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <jsp:include page="header.jsp" />
    <main>
        <h1>経費登録</h1>
        <div class="container">
            <form method="post" action="/expense">
                <div class="form-group full">
                    <label for="category">勘定項目 <span class="required">*</span></label>
                    <select id="category" name="category" required>
                        <option value="">-- 選択してください --</option>
                        <option value="rent">賃料</option>
                        <option value="salary">給与</option>
                        <option value="supplies">事務用品</option>
                        <option value="other">その他</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="date">日付 <span class="required">*</span></label>
                    <input type="date" id="date" name="date" required />
                </div>
                <div class="form-group">
                    <label for="amount">金額 <span class="required">*</span></label>
                    <div class="amount-wrapper">
                        <span>¥</span>
                        <input type="number" id="amount" name="amount" required />
                    </div>
                </div>
                <div class="textarea-group">
                    <label for="note">但し書き</label>
                    <textarea id="note" name="note" rows="4"></textarea>
                </div>
                <div class="button-group">
                    <button type="submit">登録</button>
                </div>
            </form>
        </div>
    </main>
    <jsp:include page="footer.jsp" />
</body>
</html>
