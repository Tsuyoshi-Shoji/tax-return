<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="currentMenu" value="${empty activeMenu ? 'home' : activeMenu}" />
<nav id="menuPanel" class="home-sidebar" aria-label="メインナビゲーション">
    <ul class="sidebar-list">
        <li>
            <a id="nav-home" class="nav-item ${currentMenu eq 'home' ? 'is-active' : ''}" href="${pageContext.request.contextPath}/home" <c:if test="${currentMenu eq 'home'}">aria-current="page"</c:if>>
                <span class="ui-icon icon-home" aria-hidden="true"></span><span>ホーム</span>
            </a>
        </li>
        <li>
            <a id="nav-return" class="nav-item ${currentMenu eq 'tax-return' ? 'is-active' : ''}" href="${pageContext.request.contextPath}/tax-return" <c:if test="${currentMenu eq 'tax-return'}">aria-current="page"</c:if>>
                <span class="ui-icon icon-document" aria-hidden="true"></span><span>確定申告</span>
            </a>
        </li>
        <li>
            <a id="nav-income" class="nav-item ${currentMenu eq 'income' ? 'is-active' : ''}" href="${pageContext.request.contextPath}/income" <c:if test="${currentMenu eq 'income'}">aria-current="page"</c:if>>
                <span class="ui-icon icon-plus" aria-hidden="true"></span><span>収益登録</span>
            </a>
        </li>
        <li>
            <a id="nav-expense" class="nav-item ${currentMenu eq 'expense' ? 'is-active' : ''}" href="${pageContext.request.contextPath}/expense" <c:if test="${currentMenu eq 'expense'}">aria-current="page"</c:if>>
                <span class="ui-icon icon-minus" aria-hidden="true"></span><span>支出登録</span>
            </a>
        </li>
        <li>
            <a id="nav-profit-loss" class="nav-item ${currentMenu eq 'profit-loss' ? 'is-active' : ''}" href="${pageContext.request.contextPath}/profit-loss" <c:if test="${currentMenu eq 'profit-loss'}">aria-current="page"</c:if>>
                <span class="ui-icon icon-bars" aria-hidden="true"></span><span>損益一覧</span>
            </a>
        </li>
        <li>
            <a id="nav-report" class="nav-item ${currentMenu eq 'report' ? 'is-active' : ''}" href="${pageContext.request.contextPath}/report" <c:if test="${currentMenu eq 'report'}">aria-current="page"</c:if>>
                <span class="ui-icon icon-report" aria-hidden="true"></span><span>レポート</span>
            </a>
        </li>
        <li>
            <a id="nav-settings" class="nav-item ${currentMenu eq 'settings' ? 'is-active' : ''}" href="${pageContext.request.contextPath}/settings" <c:if test="${currentMenu eq 'settings'}">aria-current="page"</c:if>>
                <span class="ui-icon icon-settings" aria-hidden="true"></span><span>各種設定</span>
            </a>
        </li>
    </ul>
</nav>
