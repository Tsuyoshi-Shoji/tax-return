<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>支出登録 | マネタックス</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261016a">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css?v=20261010a">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/entry/entry.css?v=20261016a">
    <script src="${pageContext.request.contextPath}/css/entry/entry.js?v=20261010a" defer></script>
</head>
<body class="home-page entry-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main entry-main" tabindex="-1">
        <jsp:include page="/WEB-INF/views/Common/entry-form.jsp" />
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>
