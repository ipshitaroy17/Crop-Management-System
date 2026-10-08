<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.greenfields.model.FertilizerApplicationView,com.greenfields.model.Crop,com.greenfields.model.Season,com.greenfields.model.User" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    List<FertilizerApplicationView> applications = (List<FertilizerApplicationView>) request.getAttribute("applications");
    List<Crop> crops = (List<Crop>) request.getAttribute("crops");
    List<Season> seasons = (List<Season>) request.getAttribute("seasons");
    User user = (User) request.getAttribute("currentUser");
    boolean admin = user != null && user.isAdmin();
    Integer selectedCrop = (Integer) request.getAttribute("selectedCropId");
    Integer selectedSeason = (Integer) request.getAttribute("selectedSeasonId");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Fertilizer Management | GreenFields</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css">
</head>
<body>
<header class="dashboard-nav"><a class="brand" href="<%= request.getContextPath() %>/dashboard">GreenFields <span>Agri Farm</span></a>
    <div class="nav-account"><span class="user-greeting"><%= esc(user == null ? "" : user.getFullName()) %></span>
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a><a href="<%= request.getContextPath() %>/logout">Sign out</a></div>
</header>
<main class="page-shell fertilizer-page">
    <div class="page-header"><div><p class="eyebrow">Farm inputs</p><h1>Fertilizer management</h1><p class="welcome-copy">Applications recorded against crop seasons.</p></div>
        <% if (admin) { %><a class="button button-primary" href="<%= request.getContextPath() %>/fertilizers?action=new">Add application</a><% } %>
    </div>
    <% if (request.getAttribute("message") != null) { %><div class="notice success-notice"><%= esc((String) request.getAttribute("message")) %></div><% } %>
    <% if (request.getAttribute("errorMessage") != null) { %><div class="notice error-notice"><%= esc((String) request.getAttribute("errorMessage")) %></div><% } %>

    <form class="fertilizer-filter" method="get" action="<%= request.getContextPath() %>/fertilizers">
        <label>Crop<select name="cropId" id="filterCrop"><option value="">All crops</option>
            <% if (crops != null) for (Crop crop : crops) { %><option value="<%= crop.getId() %>" <%= selectedCrop != null && selectedCrop == crop.getId() ? "selected" : "" %>><%= esc(crop.getLabel()) %></option><% } %>
        </select></label>
        <label>Season<select name="seasonId" id="filterSeason"><option value="">All seasons</option>
            <% if (seasons != null) for (Season season : seasons) { %><option value="<%= season.getId() %>" data-crop="<%= season.getCropId() %>" <%= selectedSeason != null && selectedSeason == season.getId() ? "selected" : "" %>><%= esc(season.getSeasonName()) %></option><% } %>
        </select></label>
        <button class="button button-secondary" type="submit">Filter</button><a class="text-link" href="<%= request.getContextPath() %>/fertilizers">Clear</a>
    </form>

    <div class="table-wrap fertilizer-table-wrap"><table><thead><tr><th>Crop / season</th><th>Fertilizer</th><th>Quantity</th><th>Application date</th><th>Applied by</th><th>Notes</th><% if (admin) { %><th>Actions</th><% } %></tr></thead>
        <tbody>
        <% if (applications != null && !applications.isEmpty()) { for (FertilizerApplicationView view : applications) { var app = view.getApplication(); %>
            <tr><td><strong><%= esc(view.getCropName()) %></strong><small class="table-subline"><%= esc(view.getSeasonName()) %></small></td>
                <td><strong><%= esc(app.getFertilizerName()) %></strong><small class="type-label type-<%= esc(app.getFertilizerType()) %>"><%= esc(app.getFertilizerType()) %></small></td>
                <td><%= String.format(java.util.Locale.ROOT, "%.2f kg", app.getQuantityKg()) %></td>
                <td><%= app.getApplicationDate() == null ? "—" : app.getApplicationDate() %></td><td><%= esc(app.getAppliedBy()) %></td>
                <td class="notes-cell"><%= esc(app.getNotes()) %></td>
                <% if (admin) { %><td class="row-actions"><a href="<%= request.getContextPath() %>/fertilizers?action=edit&amp;id=<%= app.getId() %>" aria-label="Edit application">Edit</a>
                    <form method="post" action="<%= request.getContextPath() %>/fertilizers" onsubmit="return confirm('Delete this fertilizer application?');"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="<%= app.getId() %>"><button class="link-button danger-link" type="submit">Delete</button></form></td><% } %>
            </tr>
        <% } } else { %><tr><td colspan="<%= admin ? 7 : 6 %>" class="empty-table-cell">No fertilizer applications match this view.</td></tr><% } %>
        </tbody></table></div>
</main>
<script>
    const cropFilter = document.getElementById('filterCrop');
    const seasonFilter = document.getElementById('filterSeason');
    cropFilter.addEventListener('change', () => {
        const crop = cropFilter.value;
        for (const option of seasonFilter.options) {
            if (!option.value) continue;
            option.hidden = !!crop && option.dataset.crop !== crop;
            if (option.hidden && option.selected) seasonFilter.value = '';
        }
    });
    cropFilter.dispatchEvent(new Event('change'));
</script>
</body></html>
