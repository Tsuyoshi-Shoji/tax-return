<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>経費登録</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/expense/expense.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />
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
    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>


