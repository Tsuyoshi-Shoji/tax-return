<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/header/header.css" />

<header class="app-header">
    <button id="hamburgerBtn" onclick="toggleMenu()">☰</button>
    <div class="header-spacer"></div>
    <div class="header-user-area">
        <c:if test="${not empty currentUserDisplayName}">
            <div class="header-user-info">
                <span class="header-user-name"><c:out value="${currentUserDisplayName}" /></span>
                <span class="header-user-email"><c:out value="${currentUserEmail}" /></span>
            </div>
        </c:if>
        <c:if test="${not empty currentUserEmail}">
            <form method="post" action="${pageContext.request.contextPath}/logout" class="logout-form">
                <c:if test="${not empty _csrf}">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                </c:if>
                <button type="submit" class="logout-button">ログアウト</button>
            </form>
        </c:if>
    </div>
</header>

<!-- オーバーレイメニュー背景 -->
<div id="menuOverlay" onclick="toggleMenu()"></div>

<!-- ドロップダウンメニュー（左側、縦一杯） -->
<nav id="menuPanel" aria-hidden="true">
    <a href="${pageContext.request.contextPath}/">ホーム</a>
    <a href="${pageContext.request.contextPath}/return-form">申告書作成</a>
    <a href="${pageContext.request.contextPath}/income">収益登録</a>
    <a href="${pageContext.request.contextPath}/expense">支出登録</a>
    <a href="${pageContext.request.contextPath}/profit-loss">損益一覧</a>
    <a href="${pageContext.request.contextPath}/report">損益レポート</a>
    <a href="${pageContext.request.contextPath}/settings">設定</a>
</nav>

<script>
    let menuOpen = false;
    function toggleMenu() {
        const menuPanel = document.getElementById('menuPanel');
        const menuOverlay = document.getElementById('menuOverlay');

        if (!menuOpen) {
            menuPanel.classList.remove('close');
            menuPanel.classList.add('open');
            menuPanel.style.transform = 'translateX(0)';
            menuPanel.setAttribute('aria-hidden', 'false');
            menuOverlay.style.display = 'block';
            menuOpen = true;
        } else {
            menuPanel.classList.remove('open');
            menuPanel.classList.add('close');
            menuPanel.style.transform = 'translateX(-250px)';
            menuPanel.setAttribute('aria-hidden', 'true');
            menuOverlay.style.display = 'none';
            menuOpen = false;
        }
    }
</script>

