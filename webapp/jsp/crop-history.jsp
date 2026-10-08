<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Locale,com.greenfields.model.CropHistoryViewModel,com.greenfields.model.Season,com.greenfields.model.Crop,com.greenfields.model.ReportsViewModel.HarvestYieldRow,com.greenfields.model.ReportsViewModel.FertilizerUsageRow,com.greenfields.model.ReportsViewModel.IrrigationUsageRow" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String date(Object value) { return value == null ? "Not recorded" : esc(String.valueOf(value)); }
    private String kg(Number value) { return value == null ? "Pending" : String.format(Locale.ROOT, "%,.2f kg", value.doubleValue()); }
    private String litres(Number value) { return String.format(Locale.ROOT, "%,.2f L", value.doubleValue()); }
%>
<%
    CropHistoryViewModel history = (CropHistoryViewModel) request.getAttribute("history");
    List<Crop> crops = history == null ? List.of() : history.getCrops();
    Crop crop = history == null ? null : history.getCrop();
%>
<!doctype html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Crop History | GreenFields</title><link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css"></head>
<body><header class="dashboard-nav report-nav"><a class="farm-brand" href="<%= request.getContextPath() %>/dashboard"><span class="brand-mark" aria-hidden="true">GF</span><span><strong>GreenFields</strong><small>Agri Farm</small></span></a><div class="nav-account"><a href="<%= request.getContextPath() %>/reports">Reports</a><a class="logout-link" href="<%= request.getContextPath() %>/logout">Log out</a></div></header>
<main class="page-shell report-page">
    <div class="page-header"><div><p class="eyebrow">Historical records</p><h1>Crop history</h1><p class="welcome-copy">A read-only timeline of seasons, inputs, irrigation, and harvests.</p></div><a class="button button-secondary" href="<%= request.getContextPath() %>/reports">Reports</a></div>
    <% if (history != null && history.getErrorMessage() != null) { %><div class="notice notice-error" role="alert"><%= esc(history.getErrorMessage()) %></div><% } %>
    <form class="fertilizer-filter report-filters history-filter" method="get" action="<%= request.getContextPath() %>/crop-history">
        <label>Crop<select name="cropId" required><option value="">Select crop</option>
            <% for (Crop item : crops) { %><option value="<%= item.getId() %>" <%= crop != null && crop.getId() == item.getId() ? "selected" : "" %>><%= esc(item.getLabel()) %></option><% } %>
        </select></label><button class="button button-primary" type="submit">View history</button>
    </form>
    <% if (crop != null) { %>
    <section class="history-section"><div class="section-heading"><h2>Crop information</h2></div><dl class="detail-grid history-crop-details">
        <div><dt>Name</dt><dd><%= esc(crop.getCropName()) %></dd></div><div><dt>Variety</dt><dd><%= esc(crop.getVariety() == null || crop.getVariety().isBlank() ? "Not specified" : crop.getVariety()) %></dd></div>
        <div><dt>Category</dt><dd><%= esc(crop.getCropType()) %></dd></div><div><dt>Growth duration</dt><dd><%= crop.getGrowthDurationDays() %> days</dd></div><div><dt>Status</dt><dd><%= esc(crop.getStatus()) %></dd></div>
    </dl></section>
    <section class="history-section"><div class="section-heading"><h2>Season history</h2><span class="count"><%= history.getSeasons().size() %> season(s)</span></div>
        <% if (history.getSeasons().isEmpty()) { %><div class="empty-state report-empty"><h3>No seasons recorded</h3><p>Season history will appear after seasons are added.</p></div><% } else { %>
        <div class="table-wrap"><table><thead><tr><th>Season</th><th>Field</th><th>Area</th><th>Planting date</th><th>Expected harvest</th><th>Status</th></tr></thead><tbody>
            <% for (Season season : history.getSeasons()) { %><tr><td><%= esc(season.getSeasonName()) %></td><td><%= esc(season.getFieldLocation()) %></td><td><%= String.format(Locale.ROOT, "%.2f acres", season.getAreaAcres()) %></td><td><%= date(season.getPlantingDate()) %></td><td><%= date(season.getExpectedHarvestDate()) %></td><td><%= esc(season.getSeasonStatus()) %></td></tr><% } %>
        </tbody></table></div><% } %>
    </section>
    <section class="history-section"><div class="section-heading"><h2>Fertilizer applications</h2><span class="count"><%= history.getFertilizerApplications().size() %> record(s)</span></div>
        <% if (history.getFertilizerApplications().isEmpty()) { %><p class="empty-copy">No fertilizer records for this crop.</p><% } else { %><div class="table-wrap"><table><thead><tr><th>Season</th><th>Name</th><th>Type</th><th>Quantity</th><th>Date</th></tr></thead><tbody>
            <% for (FertilizerUsageRow row : history.getFertilizerApplications()) { %><tr><td><%= esc(row.getSeasonName()) %></td><td><%= esc(row.getFertilizerName()) %></td><td><%= esc(row.getFertilizerType()) %></td><td><%= kg(row.getQuantityKg()) %></td><td><%= date(row.getApplicationDate()) %></td></tr><% } %>
        </tbody></table></div><% } %>
    </section>
    <section class="history-section"><div class="section-heading"><h2>Irrigation records</h2><span class="count"><%= history.getIrrigationSchedules().size() %> record(s)</span></div>
        <% if (history.getIrrigationSchedules().isEmpty()) { %><p class="empty-copy">No irrigation records for this crop.</p><% } else { %><div class="table-wrap"><table><thead><tr><th>Season</th><th>Scheduled date</th><th>Actual date</th><th>Method</th><th>Volume</th><th>Status</th></tr></thead><tbody>
            <% for (IrrigationUsageRow row : history.getIrrigationSchedules()) { %><tr><td><%= esc(row.getSeasonName()) %></td><td><%= date(row.getScheduledDate()) %></td><td><%= date(row.getActualDate()) %></td><td><%= esc(row.getMethod()) %></td><td><%= litres(row.getWaterVolumeLitres()) %></td><td><%= esc(row.getStatus()) %></td></tr><% } %>
        </tbody></table></div><% } %>
    </section>
    <section class="history-section"><div class="section-heading"><h2>Harvest &amp; yield</h2><span class="count"><%= history.getHarvestRecords().size() %> record(s)</span></div>
        <% if (history.getHarvestRecords().isEmpty()) { %><p class="empty-copy">No harvest records for this crop.</p><% } else { %><div class="table-wrap"><table><thead><tr><th>Season</th><th>Harvest date</th><th>Expected</th><th>Actual</th><th>Achievement</th><th>Status</th><th>Grade</th></tr></thead><tbody>
            <% for (HarvestYieldRow row : history.getHarvestRecords()) { %><tr><td><%= esc(row.getSeasonName()) %></td><td><%= date(row.getHarvestDate()) %></td><td><%= kg(row.getExpectedYieldKg()) %></td><td class="<%= row.getActualYieldKg() == null ? "yield-pending" : "" %>"><%= kg(row.getActualYieldKg()) %></td><td><%= row.getAchievementPercent() == null ? (row.getActualYieldKg() == null ? "Pending" : "N/A") : String.format(Locale.ROOT, "%.1f%%", row.getAchievementPercent()) %></td><td><%= esc(row.getYieldStatus()) %></td><td><%= row.getQualityGrade() == null ? "Not graded" : esc(row.getQualityGrade()) %></td></tr><% } %>
        </tbody></table></div><% } %>
    </section>
    <% } %>
</main></body></html>
