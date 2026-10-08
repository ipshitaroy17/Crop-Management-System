<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Locale,com.greenfields.model.ReportsViewModel,com.greenfields.model.ReportsViewModel.SeasonSummaryRow,com.greenfields.model.ReportsViewModel.HarvestYieldRow,com.greenfields.model.ReportsViewModel.FertilizerUsageRow,com.greenfields.model.ReportsViewModel.IrrigationUsageRow,com.greenfields.model.Crop,com.greenfields.model.Season" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String kg(Number value) {
        return value == null ? "Pending" : String.format(Locale.ROOT, "%,.2f kg", value.doubleValue());
    }
    private String litres(Number value) {
        return String.format(Locale.ROOT, "%,.2f L", value.doubleValue());
    }
    private String date(String value) { return value == null ? "Not recorded" : esc(value); }
%>
<%
    ReportsViewModel report = (ReportsViewModel) request.getAttribute("reports");
    List<Crop> crops = report == null ? List.of() : report.getCrops();
    List<Season> seasons = report == null ? List.of() : report.getSeasons();
    boolean hasHarvests = report != null && !report.getHarvestRecords().isEmpty();
%>
<!doctype html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Farm Reports | GreenFields</title><link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css"></head>
<body><header class="dashboard-nav report-nav"><a class="farm-brand" href="<%= request.getContextPath() %>/dashboard"><span class="brand-mark" aria-hidden="true">GF</span><span><strong>GreenFields</strong><small>Agri Farm</small></span></a>
    <div class="nav-account"><a href="<%= request.getContextPath() %>/crop-history">Crop history</a><a class="logout-link" href="<%= request.getContextPath() %>/logout">Log out</a></div></header>
<main class="page-shell report-page">
    <div class="page-header"><div><p class="eyebrow">Production records</p><h1>Farm reports</h1><p class="welcome-copy">Seasonal yield, farm inputs, and irrigation records.</p></div><div class="report-actions"><a class="button button-secondary" href="<%= request.getContextPath() %>/crop-history">Crop history</a><button class="button button-secondary print-button" type="button" onclick="window.print()">Print report</button></div></div>
    <% if (request.getAttribute("errorMessage") != null) { %><div class="notice notice-error" role="alert"><%= esc((String) request.getAttribute("errorMessage")) %></div><% } %>
    <form class="fertilizer-filter report-filters" method="get" action="<%= request.getContextPath() %>/reports">
        <label>Crop<select name="cropId" id="reportCrop"><option value="">All crops</option>
            <% for (Crop crop : crops) { %><option value="<%= crop.getId() %>" <%= report != null && report.getSelectedCropId() == crop.getId() ? "selected" : "" %>><%= esc(crop.getLabel()) %></option><% } %>
        </select></label>
        <label>Season<select name="seasonId" id="reportSeason"><option value="">All seasons</option>
            <% for (Season season : seasons) { %><option value="<%= season.getId() %>" data-crop="<%= season.getCropId() %>" <%= report != null && report.getSelectedSeasonId() == season.getId() ? "selected" : "" %>><%= esc(season.getSeasonName()) %></option><% } %>
        </select></label>
        <button class="button button-primary" type="submit">Generate report</button><a class="text-link" href="<%= request.getContextPath() %>/reports">Clear</a>
    </form>
    <% if (report != null) { %>
    <p class="report-scope"><strong>Scope:</strong> <%= esc(report.getSelectedCropName() == null ? "All crops" : report.getSelectedCropName()) %> · <%= esc(report.getSelectedSeasonName() == null ? "All seasons" : report.getSelectedSeasonName()) %></p>
    <section class="metric-grid report-metrics" aria-label="Report summary">
        <article class="metric-card"><span class="metric-label">Expected yield</span><strong class="metric-value"><%= String.format(Locale.ROOT, "%,.2f", report.getExpectedYieldTotalKg()) %><small>kg</small></strong><span class="metric-note">Across matching harvest records</span></article>
        <article class="metric-card metric-yield"><span class="metric-label">Actual yield recorded</span><strong class="metric-value"><%= report.getActualYieldTotalKg() == null ? "Pending" : String.format(Locale.ROOT, "%,.2f", report.getActualYieldTotalKg()) %><% if (report.getActualYieldTotalKg() != null) { %><small>kg</small><% } %></strong><span class="metric-note"><%= report.getPendingHarvestCount() %> pending harvest record(s)</span></article>
        <article class="metric-card metric-fertilizer"><span class="metric-label">Fertilizer applied</span><strong class="metric-value"><%= String.format(Locale.ROOT, "%,.2f", report.getFertilizerTotalKg()) %><small>kg</small></strong><span class="metric-note"><%= report.getFertilizerUsage().size() %> recorded application(s)</span></article>
        <article class="metric-card metric-seasons"><span class="metric-label">Irrigation schedules</span><strong class="metric-value"><%= report.getIrrigationCount() %></strong><span class="metric-note">Matching schedule records</span></article>
    </section>

    <section class="report-section"><div class="section-heading"><h2>Seasonal production summary</h2><span class="count"><%= report.getSeasonalSummaries().size() %> season(s)</span></div>
        <% if (report.getSeasonalSummaries().isEmpty()) { %><div class="empty-state report-empty"><h3>No seasonal yield records</h3><p>Harvest totals appear when records exist for the selected filters.</p></div><% } else { %>
        <div class="table-wrap"><table><thead><tr><th>Crop / season</th><th>Planting</th><th>Expected harvest</th><th>Harvest records</th><th>Expected yield</th><th>Actual recorded</th><th>Pending</th></tr></thead><tbody>
            <% for (SeasonSummaryRow row : report.getSeasonalSummaries()) { %><tr><td><strong><%= esc(row.getCropName()) %></strong><small class="table-subline"><%= esc(row.getSeasonName()) %></small></td><td><%= date(row.getPlantingDate()) %></td><td><%= date(row.getExpectedHarvestDate()) %></td><td><%= row.getHarvestCount() %></td><td><%= kg(row.getExpectedYieldKg()) %></td><td><%= kg(row.getActualYieldKg()) %></td><td><%= row.getPendingCount() %></td></tr><% } %>
        </tbody></table></div><% } %>
    </section>

    <section class="report-section"><div class="section-heading"><h2>Crop performance &amp; harvest records</h2><span class="count"><%= report.getHarvestRecords().size() %> record(s)</span></div>
        <% if (!hasHarvests) { %><div class="empty-state report-empty"><h3>No harvest records</h3><p>Yield comparisons will appear here when harvests are recorded.</p></div><% } else { %>
        <div class="table-wrap"><table><thead><tr><th>Crop</th><th>Variety</th><th>Season</th><th>Harvest date</th><th>Expected</th><th>Actual</th><th>Achievement</th><th>Status</th><th>Grade</th></tr></thead><tbody>
            <% for (HarvestYieldRow row : report.getHarvestRecords()) { %><tr><td><%= esc(row.getCropName()) %></td><td><%= esc(row.getVariety()) %></td><td><%= esc(row.getSeasonName()) %></td><td><%= date(row.getHarvestDate()) %></td><td><%= kg(row.getExpectedYieldKg()) %></td><td class="<%= row.getActualYieldKg() == null ? "yield-pending" : "" %>"><%= kg(row.getActualYieldKg()) %></td><td><%= row.getAchievementPercent() == null ? (row.getActualYieldKg() == null ? "Pending" : "N/A") : String.format(Locale.ROOT, "%.1f%%", row.getAchievementPercent()) %></td><td><%= esc(row.getYieldStatus()) %></td><td><%= row.getQualityGrade() == null ? "Not graded" : esc(row.getQualityGrade()) %></td></tr><% } %>
        </tbody></table></div><% } %>
    </section>

    <section class="report-section"><div class="section-heading"><h2>Fertilizer usage</h2><span class="count"><%= report.getFertilizerUsage().size() %> application(s)</span></div>
        <% if (report.getFertilizerUsage().isEmpty()) { %><div class="empty-state report-empty"><h3>No fertilizer applications</h3><p>No fertilizer records match the current report scope.</p></div><% } else { %>
        <div class="table-wrap"><table><thead><tr><th>Crop</th><th>Season</th><th>Fertilizer</th><th>Type</th><th>Quantity</th><th>Application date</th></tr></thead><tbody>
            <% for (FertilizerUsageRow row : report.getFertilizerUsage()) { %><tr><td><%= esc(row.getCropName()) %></td><td><%= esc(row.getSeasonName()) %></td><td><%= esc(row.getFertilizerName()) %></td><td><%= esc(row.getFertilizerType()) %></td><td><%= kg(row.getQuantityKg()) %></td><td><%= date(row.getApplicationDate()) %></td></tr><% } %>
        </tbody></table></div><% } %>
    </section>

    <section class="report-section"><div class="section-heading"><h2>Irrigation summary</h2><span class="count"><%= report.getIrrigationUsage().size() %> schedule(s)</span></div>
        <% if (report.getIrrigationUsage().isEmpty()) { %><div class="empty-state report-empty"><h3>No irrigation records</h3><p>No irrigation schedules match the current report scope.</p></div><% } else { %>
        <div class="table-wrap"><table><thead><tr><th>Crop</th><th>Season</th><th>Scheduled date</th><th>Actual date</th><th>Method</th><th>Water volume</th><th>Status</th></tr></thead><tbody>
            <% for (IrrigationUsageRow row : report.getIrrigationUsage()) { %><tr><td><%= esc(row.getCropName()) %></td><td><%= esc(row.getSeasonName()) %></td><td><%= date(row.getScheduledDate()) %></td><td><%= date(row.getActualDate()) %></td><td><%= esc(row.getMethod()) %></td><td><%= litres(row.getWaterVolumeLitres()) %></td><td><%= esc(row.getStatus()) %></td></tr><% } %>
        </tbody></table></div><% } %>
    </section>
    <p class="report-footnote">Achievement is calculated per harvest using the recorded actual and expected yield. Pending actual yields are not treated as zero; zero expected yield has no defined achievement percentage.</p>
    <% } %>
</main>
<script>
    const cropSelect = document.getElementById('reportCrop');
    const seasonSelect = document.getElementById('reportSeason');
    cropSelect.addEventListener('change', () => {
        for (const option of seasonSelect.options) {
            if (!option.value) continue;
            option.hidden = !!cropSelect.value && option.dataset.crop !== cropSelect.value;
            if (option.hidden && option.selected) seasonSelect.value = '';
        }
    });
    cropSelect.dispatchEvent(new Event('change'));
</script></body></html>
