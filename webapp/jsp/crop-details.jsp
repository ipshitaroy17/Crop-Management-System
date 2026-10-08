<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.greenfields.model.Crop,com.greenfields.model.User" %>
<%!
    private String escapeHtml(Object value) {
        if (value == null) return "";
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    Crop crop = (Crop) request.getAttribute("crop");
    User currentUser = (User) request.getAttribute("currentUser");
    boolean isAdmin = currentUser != null && currentUser.isAdmin();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= escapeHtml(crop.getCropName()) %> | GreenFields</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css">
</head>
<body>
<main class="page-shell page-narrow">
    <header class="page-header">
        <div>
            <a class="back-link" href="<%= request.getContextPath() %>/crops">Crop catalog</a>
            <h1><%= escapeHtml(crop.getCropName()) %></h1>
            <p class="subheading"><%= escapeHtml(crop.getCropType()) %><% if (crop.getVariety() != null && !crop.getVariety().isBlank()) { %> · <%= escapeHtml(crop.getVariety()) %><% } %></p>
        </div>
        <% if (isAdmin) { %>
            <a class="button button-primary" href="<%= request.getContextPath() %>/crops?action=edit&amp;id=<%= crop.getId() %>">Edit crop</a>
        <% } %>
    </header>

    <section class="detail-panel" aria-label="Crop details">
        <dl class="detail-grid">
            <div><dt>Status</dt><dd><span class="status status-<%= escapeHtml(crop.getStatus()) %>"><%= escapeHtml(crop.getStatus()) %></span></dd></div>
            <div><dt>Growth period</dt><dd><%= crop.getGrowthDurationDays() %> days</dd></div>
            <div class="detail-wide"><dt>Variety</dt><dd><%= crop.getVariety() == null || crop.getVariety().isBlank() ? "Not specified" : escapeHtml(crop.getVariety()) %></dd></div>
            <div class="detail-wide"><dt>Description</dt><dd class="description"><%= crop.getDescription() == null || crop.getDescription().isBlank() ? "No description provided." : escapeHtml(crop.getDescription()) %></dd></div>
        </dl>
    </section>
</main>
</body>
</html>
