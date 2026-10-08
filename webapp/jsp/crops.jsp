<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List,com.greenfields.model.Crop,com.greenfields.model.User" %>
<%!
    private String escapeHtml(Object value) {
        if (value == null) return "";
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    List<Crop> crops = (List<Crop>) request.getAttribute("crops");
    User currentUser = (User) request.getAttribute("currentUser");
    boolean isAdmin = currentUser != null && currentUser.isAdmin();
    String message = (String) request.getAttribute("message");
    String errorMessage = (String) request.getAttribute("errorMessage");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Crop Catalog | GreenFields</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css">
</head>
<body>
<main class="page-shell">
    <header class="page-header">
        <div>
            <a class="back-link" href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
            <h1>Crop catalog</h1>
            <p class="subheading">Crop profiles grown at GreenFields Agri Farm</p>
        </div>
        <% if (isAdmin) { %>
            <a class="button button-primary" href="<%= request.getContextPath() %>/crops?action=new">Add crop</a>
        <% } %>
    </header>

    <% if (message != null) { %><div class="notice notice-success" role="status"><%= escapeHtml(message) %></div><% } %>
    <% if (errorMessage != null) { %><div class="notice notice-error" role="alert"><%= escapeHtml(errorMessage) %></div><% } %>

    <section aria-labelledby="crop-list-heading">
        <div class="section-heading">
            <h2 id="crop-list-heading">Crops</h2>
            <span class="count"><%= crops == null ? 0 : crops.size() %> total</span>
        </div>
        <% if (crops == null || crops.isEmpty()) { %>
            <div class="empty-state"><h3>No crops found</h3><p>Crop records will appear here once added.</p></div>
        <% } else { %>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Crop</th><th>Type</th><th>Variety</th><th>Growth period</th><th>Status</th><th>Actions</th></tr></thead>
                    <tbody>
                    <% for (Crop crop : crops) { %>
                        <tr>
                            <td><a class="table-link" href="<%= request.getContextPath() %>/crops?action=details&amp;id=<%= crop.getId() %>"><%= escapeHtml(crop.getCropName()) %></a></td>
                            <td><%= escapeHtml(crop.getCropType()) %></td>
                            <td><%= crop.getVariety() == null || crop.getVariety().isBlank() ? "—" : escapeHtml(crop.getVariety()) %></td>
                            <td><%= crop.getGrowthDurationDays() %> days</td>
                            <td><span class="status status-<%= escapeHtml(crop.getStatus()) %>"><%= escapeHtml(crop.getStatus()) %></span></td>
                            <td class="row-actions">
                                <a href="<%= request.getContextPath() %>/crops?action=details&amp;id=<%= crop.getId() %>">View</a>
                                <% if (isAdmin) { %>
                                    <a href="<%= request.getContextPath() %>/crops?action=edit&amp;id=<%= crop.getId() %>">Edit</a>
                                    <form class="inline-form" action="<%= request.getContextPath() %>/crops" method="post" onsubmit="return confirm('Delete this crop?');">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="id" value="<%= crop.getId() %>">
                                        <button class="link-button danger-link" type="submit">Delete</button>
                                    </form>
                                <% } %>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        <% } %>
    </section>
</main>
</body>
</html>
