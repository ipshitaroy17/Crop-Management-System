<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List,java.util.Locale,com.greenfields.model.DashboardViewModel,com.greenfields.model.DashboardViewModel.CropPerformance,com.greenfields.model.DashboardViewModel.RecentActivity" %>
<%!
    private String escapeHtml(Object value) {
        if (value == null) return "";
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String formatNumber(Number value) {
        return value == null ? "—" : String.format(Locale.US, "%,.1f", value.doubleValue());
    }
%>
<%
    String currentUserName = (String) request.getAttribute("currentUserName");
    boolean currentUserIsAdmin = Boolean.TRUE.equals(request.getAttribute("currentUserIsAdmin"));
    DashboardViewModel dashboard = (DashboardViewModel) request.getAttribute("dashboard");
    String dashboardError = (String) request.getAttribute("dashboardError");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Farm Dashboard | GreenFields Agri Farm</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css">
</head>
<body>
<main class="dashboard-shell">
    <header class="dashboard-nav">
        <a class="farm-brand" href="<%= request.getContextPath() %>/dashboard">
            <span class="brand-mark" aria-hidden="true">GF</span>
            <span><strong>GreenFields</strong><small>Agri Farm</small></span>
        </a>
        <div class="nav-account">
            <span class="user-greeting">Signed in as <strong><%= escapeHtml(currentUserName) %></strong></span>
            <a class="logout-link" href="<%= request.getContextPath() %>/logout">Log out</a>
        </div>
    </header>

    <section class="dashboard-welcome">
        <div>
            <p class="eyebrow">Farm overview</p>
            <h1>Good day, <%= escapeHtml(currentUserName) %></h1>
            <p class="welcome-copy">A current view of crops, field activity, and seasonal yield.</p>
        </div>
        <a class="button button-secondary dashboard-crops-link" href="<%= request.getContextPath() %>/crops">View crop catalog</a>
    </section>

    <% if (dashboardError != null) { %>
        <div class="notice notice-error" role="alert"><%= escapeHtml(dashboardError) %></div>
    <% } %>

    <section class="metric-grid" aria-label="Farm summary metrics">
        <article class="metric-card metric-crops">
            <span class="metric-label">Total crops</span>
            <strong class="metric-value"><%= dashboard == null ? "—" : dashboard.getTotalCrops() %></strong>
            <span class="metric-note"><%= dashboard == null ? "Unavailable" : dashboard.getTotalCrops() == 0 ? "No crop records yet" : "Crop profiles in the catalog" %></span>
        </article>
        <article class="metric-card metric-seasons">
            <span class="metric-label">Active seasons</span>
            <strong class="metric-value"><%= dashboard == null ? "—" : dashboard.getActiveSeasons() %></strong>
            <span class="metric-note"><%= dashboard == null ? "Unavailable" : dashboard.getTotalSeasons() == 0 ? "No season records yet" : dashboard.getActiveSeasons() == 0 ? "No active seasons" : "Currently marked active" %></span>
        </article>
        <article class="metric-card metric-fertilizer">
            <span class="metric-label">Fertilizer used</span>
            <strong class="metric-value"><%= dashboard == null ? "—" : formatNumber(dashboard.getFertilizerUsedKg()) %><% if (dashboard != null) { %><small>kg</small><% } %></strong>
            <span class="metric-note"><%= dashboard == null ? "Unavailable" : dashboard.getFertilizerRecordCount() == 0 ? "No fertilizer records yet" : "Across recorded applications" %></span>
        </article>
        <article class="metric-card metric-yield">
            <span class="metric-label">Expected yield</span>
            <strong class="metric-value"><%= dashboard == null ? "—" : formatNumber(dashboard.getExpectedYieldKg()) %><% if (dashboard != null) { %><small>kg</small><% } %></strong>
            <span class="metric-note"><%= dashboard == null ? "Unavailable" : dashboard.getHarvestRecordCount() == 0 ? "No harvest records yet" : "Includes pending harvests" %></span>
        </article>
    </section>

    <section class="dashboard-panel yield-panel" aria-labelledby="yield-heading">
        <div class="panel-heading">
            <div><p class="eyebrow">Production</p><h2 id="yield-heading">Expected vs actual yield</h2></div>
            <% if (dashboard != null && dashboard.getHarvestRecordCount() > 0) { %>
                <div class="chart-legend"><span><i class="legend-swatch expected-swatch"></i>Expected</span><span><i class="legend-swatch actual-swatch"></i>Actual recorded</span></div>
            <% } %>
        </div>
        <% if (dashboard == null) { %>
            <p class="empty-copy">Yield comparisons are unavailable until database access is restored.</p>
        <% } else if (dashboard.getHarvestRecordCount() == 0) { %>
            <div class="empty-state compact-empty"><h3>No harvest records</h3><p>Expected and actual yield comparisons will appear when harvest records are available.</p></div>
        <% } else { %>
            <div class="yield-chart">
                <% for (CropPerformance item : dashboard.getCropPerformance()) { %>
                    <div class="yield-row">
                        <div class="yield-row-heading">
                            <strong><%= escapeHtml(item.getCropName()) %></strong>
                            <% if (item.getPendingHarvests() > 0) { %><span class="pending-note"><%= item.getPendingHarvests() %> pending</span><% } %>
                        </div>
                        <div class="yield-series">
                            <span class="series-label">Expected</span>
                            <div class="yield-track"><span class="yield-bar expected-bar" style="width: <%= item.getExpectedBarPercent() %>%"></span></div>
                            <span class="series-value"><%= formatNumber(item.getExpectedYieldKg()) %> kg</span>
                        </div>
                        <div class="yield-series">
                            <span class="series-label">Actual</span>
                            <% if (item.hasActualYield()) { %>
                                <div class="yield-track"><span class="yield-bar actual-bar" style="width: <%= item.getActualBarPercent() %>%"></span></div>
                                <span class="series-value"><%= formatNumber(item.getActualYieldKg()) %> kg<% if (item.getPendingHarvests() > 0) { %><small> partial</small><% } %></span>
                            <% } else if (item.getPendingHarvests() > 0) { %>
                                <div class="yield-track pending-track"><span>Pending</span></div>
                                <span class="series-value pending-value">Not recorded</span>
                            <% } else { %>
                                <div class="yield-track pending-track"><span>No record</span></div>
                                <span class="series-value pending-value">No harvest record</span>
                            <% } %>
                        </div>
                    </div>
                <% } %>
            </div>
            <p class="chart-footnote">Actual yield excludes harvests that have not been recorded as complete.</p>
        <% } %>
    </section>

    <div class="dashboard-lower-grid">
        <section class="dashboard-panel performance-panel" aria-labelledby="performance-heading">
            <div class="panel-heading">
                <div><p class="eyebrow">By crop</p><h2 id="performance-heading">Crop performance</h2></div>
            </div>
            <% if (dashboard == null) { %>
                <p class="empty-copy">Performance data is unavailable.</p>
            <% } else if (dashboard.getCropPerformance().isEmpty()) { %>
                <p class="empty-copy">No crop records are available.</p>
            <% } else { %>
                <div class="performance-list">
                    <% for (CropPerformance item : dashboard.getCropPerformance()) { %>
                        <div class="performance-row">
                            <div class="performance-name"><strong><%= escapeHtml(item.getCropName()) %></strong><span><%= item.getCompletedHarvests() %> completed · <%= item.getPendingHarvests() %> pending</span></div>
                            <div class="performance-result">
                                <% if (item.getAchievementPercent() == null && item.getPendingHarvests() > 0) { %>
                                    <span class="performance-pending">Pending</span>
                                <% } else if (item.getAchievementPercent() == null) { %>
                                    <span class="performance-pending">No records</span>
                                <% } else { %>
                                    <strong><%= formatNumber(item.getAchievementPercent()) %>%</strong><span>avg. achievement</span>
                                <% } %>
                            </div>
                        </div>
                    <% } %>
                </div>
            <% } %>
        </section>

        <section class="dashboard-panel activity-panel" aria-labelledby="activity-heading">
            <div class="panel-heading">
                <div><p class="eyebrow">Latest records</p><h2 id="activity-heading">Recent activities</h2></div>
            </div>
            <% if (dashboard == null) { %>
                <p class="empty-copy">Activity data is unavailable.</p>
            <% } else if (dashboard.getRecentActivities().isEmpty()) { %>
                <p class="empty-copy">No farm activities have been recorded yet.</p>
            <% } else { %>
                <ol class="activity-list">
                    <% for (RecentActivity activity : dashboard.getRecentActivities()) { %>
                        <li class="activity-item">
                            <span class="activity-marker" aria-hidden="true"></span>
                            <div class="activity-body">
                                <div class="activity-title-row"><strong><%= escapeHtml(activity.getTitle()) %></strong><time><%= escapeHtml(activity.getDateLabel()) %></time></div>
                                <p><%= escapeHtml(activity.getDescription()) %></p>
                                <span class="activity-category"><%= escapeHtml(activity.getCategory()) %></span>
                            </div>
                        </li>
                    <% } %>
                </ol>
            <% } %>
        </section>
    </div>

    <section class="quick-actions" aria-labelledby="actions-heading">
        <div class="actions-heading"><p class="eyebrow">Navigation</p><h2 id="actions-heading">Quick actions</h2></div>
        <div class="action-list">
            <% if (currentUserIsAdmin) { %>
                <a class="action-link" href="<%= request.getContextPath() %>/crops?action=new"><strong>Add crop</strong><span>Create a crop profile</span></a>
            <% } else { %>
                <span class="action-link action-disabled" aria-disabled="true"><strong>Add crop</strong><span>Admin access required</span></span>
            <% } %>
            <a class="action-link" href="<%= request.getContextPath() %>/crops"><strong>View crops</strong><span>Open crop catalog</span></a>
            <a class="action-link" href="<%= request.getContextPath() %>/fertilizers"><strong>Fertilizer management</strong><span>Review farm inputs</span></a>
            <a class="action-link" href="<%= request.getContextPath() %>/irrigation"><strong>Irrigation</strong><span>Manage watering schedules</span></a>
            <span class="action-link action-disabled" aria-disabled="true"><strong>Harvest &amp; yield</strong><span>Coming soon</span></span>
            <span class="action-link action-disabled" aria-disabled="true"><strong>Reports</strong><span>Coming soon</span></span>
        </div>
    </section>
</main>
</body>
</html>
