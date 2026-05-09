<%@ page contentType="text/html;charset=UTF-8" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/header/header.css" />

<header class="app-header">
    <!-- ハンバーガーメニューボタン（左側、上下中央揃え） -->
    <button id="hamburgerBtn" onclick="toggleMenu()">☰</button>

    <!-- タイトル（中央揃え） -->
    <h1>${empty headerTitle ? '収支管理' : headerTitle}</h1>
</header>

<!-- オーバーレイメニュー背景 -->
<div id="menuOverlay" onclick="toggleMenu()"></div>

<!-- ドロップダウンメニュー（左側、縦一杯） -->
<nav id="menuPanel" aria-hidden="true">
    <a href="${pageContext.request.contextPath}/">ホーム</a>
    <a href="${pageContext.request.contextPath}/return-form">申告書作成</a>
    <a href="${pageContext.request.contextPath}/income">収益登録</a>
    <a href="${pageContext.request.contextPath}/expense">経費登録</a>
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

