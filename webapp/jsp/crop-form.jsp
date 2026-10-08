<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map,com.greenfields.model.Crop" %>
<%!
    private String escapeHtml(Object value) {
        if (value == null) return "";
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    Crop crop = (Crop) request.getAttribute("crop");
    String formAction = (String) request.getAttribute("formAction");
    String pageTitle = (String) request.getAttribute("pageTitle");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    String errorMessage = (String) request.getAttribute("errorMessage");
    boolean editing = "update".equals(formAction);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= escapeHtml(pageTitle) %> | GreenFields</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css">
</head>
<body>
<main class="page-shell page-narrow">
    <header class="page-header">
        <div>
            <a class="back-link" href="<%= request.getContextPath() %>/crops">Crop catalog</a>
            <h1><%= escapeHtml(pageTitle) %></h1>
            <p class="subheading">Maintain the crop profile and growing period.</p>
        </div>
    </header>

    <% if (errorMessage != null) { %><div class="notice notice-error" role="alert"><%= escapeHtml(errorMessage) %></div><% } %>
    <% if (errors != null && errors.containsKey("id")) { %><div class="notice notice-error" role="alert"><%= escapeHtml(errors.get("id")) %></div><% } %>
    <form class="crop-form detail-panel" action="<%= request.getContextPath() %>/crops" method="post">
        <input type="hidden" name="action" value="<%= editing ? "update" : "create" %>">
        <% if (editing) { %><input type="hidden" name="id" value="<%= crop.getId() %>"><% } %>

        <div class="form-grid">
            <div class="field">
                <label for="cropName">Crop name <span aria-hidden="true">*</span></label>
                <input id="cropName" name="cropName" type="text" maxlength="100" required value="<%= escapeHtml(crop.getCropName()) %>" aria-describedby="cropName-error">
                <% if (errors != null && errors.containsKey("cropName")) { %><span class="field-error" id="cropName-error"><%= escapeHtml(errors.get("cropName")) %></span><% } %>
            </div>
            <div class="field">
                <label for="cropType">Crop type <span aria-hidden="true">*</span></label>
                <input id="cropType" name="cropType" type="text" maxlength="50" required value="<%= escapeHtml(crop.getCropType()) %>" aria-describedby="cropType-error">
                <% if (errors != null && errors.containsKey("cropType")) { %><span class="field-error" id="cropType-error"><%= escapeHtml(errors.get("cropType")) %></span><% } %>
            </div>
            <div class="field">
                <label for="variety">Variety</label>
                <input id="variety" name="variety" type="text" maxlength="100" value="<%= escapeHtml(crop.getVariety()) %>" aria-describedby="variety-error">
                <% if (errors != null && errors.containsKey("variety")) { %><span class="field-error" id="variety-error"><%= escapeHtml(errors.get("variety")) %></span><% } %>
            </div>
            <div class="field">
                <label for="growthDurationDays">Growth period (days) <span aria-hidden="true">*</span></label>
                <input id="growthDurationDays" name="growthDurationDays" type="number" min="1" step="1" required value="<%= crop.getGrowthDurationDays() > 0 ? crop.getGrowthDurationDays() : "" %>" aria-describedby="growthDurationDays-error">
                <% if (errors != null && errors.containsKey("growthDurationDays")) { %><span class="field-error" id="growthDurationDays-error"><%= escapeHtml(errors.get("growthDurationDays")) %></span><% } %>
            </div>
            <div class="field">
                <label for="status">Status <span aria-hidden="true">*</span></label>
                <select id="status" name="status" required aria-describedby="status-error">
                    <option value="active" <%= "active".equals(crop.getStatus()) ? "selected" : "" %>>Active</option>
                    <option value="inactive" <%= "inactive".equals(crop.getStatus()) ? "selected" : "" %>>Inactive</option>
                </select>
                <% if (errors != null && errors.containsKey("status")) { %><span class="field-error" id="status-error"><%= escapeHtml(errors.get("status")) %></span><% } %>
            </div>
            <div class="field field-wide">
                <label for="description">Description</label>
                <textarea id="description" name="description" rows="5"><%= escapeHtml(crop.getDescription()) %></textarea>
                <% if (errors != null && errors.containsKey("description")) { %><span class="field-error"><%= escapeHtml(errors.get("description")) %></span><% } %>
            </div>
        </div>
        <div class="form-actions">
            <a class="button button-secondary" href="<%= request.getContextPath() %>/crops">Cancel</a>
            <button class="button button-primary" type="submit"><%= editing ? "Save changes" : "Add crop" %></button>
        </div>
    </form>
</main>
</body>
</html>
