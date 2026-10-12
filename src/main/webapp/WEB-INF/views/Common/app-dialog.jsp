<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="adComplete" value="${not empty successMessage ? successMessage : flashMessage}" />
<c:set var="adError" value="${not empty failureMessage ? failureMessage : flashError}" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/app-dialog.css?v=20261011a">
<script src="${pageContext.request.contextPath}/css/common/app-dialog.js?v=20261011a"></script>
<div id="app-dialog-server" hidden
     data-complete-title="${fn:escapeXml(param.completeTitle)}"
     data-error-title="${fn:escapeXml(empty param.errorTitle ? '処理に失敗しました' : param.errorTitle)}"
     data-alert-title="入力内容をご確認ください"
     data-alert-message="未確認の項目があります。内容をご確認のうえ、再度お進みください。"
     data-primary-label="${fn:escapeXml(param.primaryLabel)}"
     data-primary-href="${fn:escapeXml(param.primaryHref)}"
     data-secondary-label="${fn:escapeXml(param.secondaryLabel)}">
    <c:if test="${not empty adComplete}"><p data-role="complete-message"><c:out value="${adComplete}" /></p></c:if>
    <c:if test="${not empty adError}"><p data-role="error-message"><c:out value="${adError}" /></p></c:if>
    <c:forEach var="m" items="${formErrors}"><span data-role="error-detail"><c:out value="${m}" /></span></c:forEach>
    <c:if test="${param.useValidationErrors eq 'true'}">
        <c:forEach var="m" items="${validationErrors}"><span data-role="error-detail"><c:out value="${m}" /></span></c:forEach>
    </c:if>
    <c:if test="${empty adError}">
        <c:forEach var="m" items="${fieldErrors}"><span data-role="alert-detail"><c:out value="${m.value}" /></span></c:forEach>
    </c:if>
</div>
