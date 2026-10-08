<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.greenfields.model.IrrigationScheduleView,com.greenfields.model.Crop,com.greenfields.model.Season,com.greenfields.model.User" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    List<IrrigationScheduleView> schedules = (List<IrrigationScheduleView>) request.getAttribute("schedules");
    List<Crop> crops = (List<Crop>) request.getAttribute("crops");
    List<Season> seasons = (List<Season>) request.getAttribute("seasons");
    User user = (User) request.getAttribute("currentUser");
    boolean admin = user != null && user.isAdmin();
    Integer selectedCrop = (Integer) request.getAttribute("selectedCropId");
    Integer selectedSeason = (Integer) request.getAttribute("selectedSeasonId");
%>
<!doctype html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Irrigation Management | GreenFields</title><link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css"></head>
<body><header class="dashboard-nav"><a class="brand" href="<%= request.getContextPath() %>/dashboard">GreenFields <span>Agri Farm</span></a>
    <div class="nav-account"><span class="user-greeting"><%= esc(user == null ? "" : user.getFullName()) %></span><a href="<%= request.getContextPath() %>/dashboard">Dashboard</a><a href="<%= request.getContextPath() %>/logout">Sign out</a></div></header>
<main class="page-shell irrigation-page">
    <div class="page-header"><div><p class="eyebrow">Water management</p><h1>Irrigation schedules</h1><p class="welcome-copy">Plan and track watering across crop seasons.</p></div>
        <% if (admin) { %><a class="button button-primary" href="<%= request.getContextPath() %>/irrigation?action=new">Add schedule</a><% } %></div>
    <% if (request.getAttribute("message") != null) { %><div class="notice notice-success"><%= esc((String) request.getAttribute("message")) %></div><% } %>
    <% if (request.getAttribute("errorMessage") != null) { %><div class="notice notice-error"><%= esc((String) request.getAttribute("errorMessage")) %></div><% } %>
    <form class="fertilizer-filter" method="get" action="<%= request.getContextPath() %>/irrigation">
        <label>Crop<select name="cropId" id="irrigationFilterCrop"><option value="">All crops</option>
            <% if (crops != null) for (Crop crop : crops) { %><option value="<%= crop.getId() %>" <%= selectedCrop != null && selectedCrop == crop.getId() ? "selected" : "" %>><%= esc(crop.getLabel()) %></option><% } %>
        </select></label>
        <label>Season<select name="seasonId" id="irrigationFilterSeason"><option value="">All seasons</option>
            <% if (seasons != null) for (Season season : seasons) { %><option value="<%= season.getId() %>" data-crop="<%= season.getCropId() %>" <%= selectedSeason != null && selectedSeason == season.getId() ? "selected" : "" %>><%= esc(season.getSeasonName()) %></option><% } %>
        </select></label>
        <button class="button button-secondary" type="submit">Filter</button><a class="text-link" href="<%= request.getContextPath() %>/irrigation">Clear</a>
    </form>
    <div class="table-wrap fertilizer-table-wrap"><table><thead><tr><th>Crop / season</th><th>Scheduled</th><th>Method</th><th>Water volume</th><th>Status</th><th>Actual date</th><th>Notes</th><% if (admin) { %><th>Actions</th><% } %></tr></thead>
        <tbody>
        <% if (schedules != null && !schedules.isEmpty()) { for (IrrigationScheduleView view : schedules) { var schedule = view.getSchedule(); %>
            <tr><td><strong><%= esc(view.getCropName()) %></strong><small class="table-subline"><%= esc(view.getSeasonName()) %></small></td>
                <td><%= schedule.getScheduledDate() == null ? "—" : schedule.getScheduledDate() %></td>
                <td><%= esc(schedule.getMethodLabel()) %></td>
                <td><%= String.format(java.util.Locale.ROOT, "%.2f L", schedule.getWaterVolumeLitres()) %></td>
                <td><span class="status status-<%= esc(schedule.getScheduleStatus()) %>"><%= esc(schedule.getScheduleStatus()) %></span>
                    <% if (schedule.isOverdue()) { %><small class="overdue-label">Overdue</small><% } %></td>
                <td><%= schedule.getActualDate() == null ? "Not recorded" : schedule.getActualDate() %></td><td class="notes-cell"><%= esc(schedule.getNotes()) %></td>
                <% if (admin) { %><td class="row-actions"><a href="<%= request.getContextPath() %>/irrigation?action=edit&amp;id=<%= schedule.getId() %>">Edit</a>
                    <form method="post" action="<%= request.getContextPath() %>/irrigation" onsubmit="return confirm('Delete this irrigation schedule?');"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="<%= schedule.getId() %>"><button class="link-button danger-link" type="submit">Delete</button></form></td><% } %>
            </tr>
        <% } } else { %><tr><td colspan="<%= admin ? 8 : 7 %>" class="empty-table-cell">No irrigation schedules match this view.</td></tr><% } %>
        </tbody></table></div>
</main>
<script>
    const cropFilter = document.getElementById('irrigationFilterCrop');
    const seasonFilter = document.getElementById('irrigationFilterSeason');
    cropFilter.addEventListener('change', () => {
        for (const option of seasonFilter.options) {
            if (!option.value) continue;
            option.hidden = !!cropFilter.value && option.dataset.crop !== cropFilter.value;
            if (option.hidden && option.selected) seasonFilter.value = '';
        }
    });
    cropFilter.dispatchEvent(new Event('change'));
</script></body></html>
