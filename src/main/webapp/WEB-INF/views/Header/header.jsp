<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/header/header.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/sidebar-mobile-overlay.css?v=20261011d">
<script src="${pageContext.request.contextPath}/css/header/header.js?v=20261011d" defer></script>
<a class="skip-link" href="#main-content">メインコンテンツへ</a>
<header class="app-header">
    <button id="hamburgerBtn" class="icon-button" type="button" aria-label="メニューを開く" aria-controls="menuPanel" aria-expanded="false" hidden><span class="ui-icon icon-menu" aria-hidden="true"></span></button>
    <a class="app-brand" href="${pageContext.request.contextPath}/home" aria-label="マネタックス ホーム">
        <img src="${pageContext.request.contextPath}/css/icons/brand.svg" width="38" height="40" alt="">
        <span><span id="logo">マネタックス</span><span id="subtitle">確定申告・資産管理</span></span>
    </a>
    <div class="header-user-area">
        <details class="header-dropdown notification-dropdown">
            <summary id="icon-notification" class="icon-button" aria-label="通知"><span class="ui-icon icon-bell" aria-hidden="true"></span></summary>
            <div class="header-popover"><strong>通知</strong><p>通知機能は準備中です。</p></div>
        </details>
        <details id="menu-user" class="header-dropdown user-dropdown">
            <summary class="user-menu-trigger">
                <img class="user-avatar" src="${pageContext.request.contextPath}/css/icons/avatar.svg" width="40" height="40" alt="">
                <span class="header-user-name"><c:out value="${currentUserDisplayName}" default="ユーザー" /></span>
                <span class="visually-hidden">ユーザーメニュー</span>
                <span class="ui-icon icon-chevron-down" aria-hidden="true"></span>
            </summary>
            <div class="header-popover">
                <strong><c:out value="${currentUserDisplayName}" default="ユーザー" /></strong>
                <p class="header-user-email"><c:out value="${currentUserEmail}" /></p>
                <p class="profile-status">プロフィール編集は準備中です。</p>
                <form method="post" action="${pageContext.request.contextPath}/logout" class="logout-form">
                    <button type="submit" class="logout-button"><span class="ui-icon icon-logout" aria-hidden="true"></span>ログアウト</button>
                </form>
            </div>
        </details>
    </div>
</header>
