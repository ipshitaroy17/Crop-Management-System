<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Locale,com.greenfields.model.HarvestRecordView,com.greenfields.model.Crop,com.greenfields.model.Season,com.greenfields.model.User" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String kg(Number value) {
        return value == null ? "Pending" : String.format(Locale.ROOT, "%,.2f kg", value.doubleValue());
    }
%>
<%
    List<HarvestRecordView> records = (List<HarvestRecordView>) request.getAttribute("records");
    List<Crop> crops = (List<Crop>) request.getAttribute("crops");
    List<Season> seasons = (List<Season>) request.getAttribute("seasons");
    User user = (User) request.getAttribute("currentUser");
    boolean admin = user != null && user.isAdmin();
    Integer selectedCrop = (Integer) request.getAttribute("selectedCropId");
    Integer selectedSeason = (Integer) request.getAttribute("selectedSeasonId");
%>
<!doctype html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Harvest &amp; Yield | GreenFields</title><link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css"></head>
<body><header class="dashboard-nav"><a class="farm-brand" href="<%= request.getContextPath() %>/dashboard"><span class="brand-mark" aria-hidden="true">GF</span><span><strong>GreenFields</strong><small>Agri Farm</small></span></a>
    <div class="nav-account"><span class="user-greeting"><%= esc(user == null ? "" : user.getFullName()) %></span><a class="logout-link" href="<%= request.getContextPath() %>/logout">Log out</a></div></header>
<main class="page-shell harvest-page">
    <div class="page-header"><div><p class="eyebrow">Production records</p><h1>Harvest &amp; yield</h1><p class="welcome-copy">Compare expected and recorded crop yields by season.</p></div>
        <% if (admin) { %><a class="button button-primary" href="<%= request.getContextPath() %>/harvest?action=new">Add harvest record</a><% } %></div>
    <% if (request.getAttribute("message") != null) { %><div class="notice notice-success"><%= esc((String) request.getAttribute("message")) %></div><% } %>
    <% if (request.getAttribute("errorMessage") != null) { %><div class="notice notice-error"><%= esc((String) request.getAttribute("errorMessage")) %></div><% } %>
    <form class="fertilizer-filter" method="get" action="<%= request.getContextPath() %>/harvest">
        <label>Crop<select name="cropId" id="harvestFilterCrop"><option value="">All crops</option>
            <% if (crops != null) for (Crop crop : crops) { %><option value="<%= crop.getId() %>" <%= selectedCrop != null && selectedCrop == crop.getId() ? "selected" : "" %>><%= esc(crop.getLabel()) %></option><% } %>
        </select></label>
        <label>Season<select name="seasonId" id="harvestFilterSeason"><option value="">All seasons</option>
            <% if (seasons != null) for (Season season : seasons) { %><option value="<%= season.getId() %>" data-crop="<%= season.getCropId() %>" <%= selectedSeason != null && selectedSeason == season.getId() ? "selected" : "" %>><%= esc(season.getSeasonName()) %></option><% } %>
        </select></label>
        <button class="button button-secondary" type="submit">Filter</button><a class="text-link" href="<%= request.getContextPath() %>/harvest">Clear</a>
    </form>
    <div class="table-wrap fertilizer-table-wrap"><table><thead><tr><th>Crop / season</th><th>Harvest date</th><th>Expected</th><th>Actual</th><th>Achievement</th><th>Yield status</th><th>Grade</th><th>Recorded by</th><th>Remarks</th><% if (admin) { %><th>Actions</th><% } %></tr></thead>
        <tbody>
        <% if (records != null && !records.isEmpty()) { for (HarvestRecordView view : records) { var record = view.getRecord(); %>
            <tr><td><strong><%= esc(view.getCropName()) %></strong><small class="table-subline"><%= esc(view.getSeasonName()) %></small></td>
                <td><%= record.getHarvestDate() == null ? "—" : record.getHarvestDate() %></td>
                <td><%= kg(record.getExpectedYieldKg()) %></td>
                <td class="<%= record.getActualYieldKg() == null ? "yield-pending" : "" %>"><%= kg(record.getActualYieldKg()) %></td>
                <td><%= view.getAchievementPercent() == null ? "—" : String.format(Locale.ROOT, "%.1f%%", view.getAchievementPercent()) %></td>
                <td><span class="yield-status <%= "Pending".equals(view.getYieldStatus()) ? "yield-status-pending" : "" %>"><%= esc(view.getYieldStatus()) %></span></td>
                <td><%= record.getQualityGrade() == null ? "Not graded" : esc(record.getQualityGrade()) %></td>
                <td><%= esc(record.getRecordedBy()) %></td><td class="notes-cell"><%= esc(record.getRemarks()) %></td>
                <% if (admin) { %><td class="row-actions"><a href="<%= request.getContextPath() %>/harvest?action=edit&amp;id=<%= record.getId() %>">Edit</a>
                    <form method="post" action="<%= request.getContextPath() %>/harvest" onsubmit="return confirm('Delete this harvest record?');"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="<%= record.getId() %>"><button class="link-button danger-link" type="submit">Delete</button></form></td><% } %>
            </tr>
        <% } } else { %><tr><td colspan="<%= admin ? 10 : 9 %>" class="empty-table-cell">No harvest records match this view.</td></tr><% } %>
        </tbody></table></div>
</main>
<script>
    const cropFilter = document.getElementById('harvestFilterCrop');
    const seasonFilter = document.getElementById('harvestFilterSeason');
    cropFilter.addEventListener('change', () => {
        for (const option of seasonFilter.options) {
            if (!option.value) continue;
            option.hidden = !!cropFilter.value && option.dataset.crop !== cropFilter.value;
            if (option.hidden && option.selected) seasonFilter.value = '';
        }
    });
    cropFilter.dispatchEvent(new Event('change'));
</script></body></html>
