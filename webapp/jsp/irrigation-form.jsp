<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map,com.greenfields.model.IrrigationSchedule,com.greenfields.model.Crop,com.greenfields.model.Season" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    IrrigationSchedule schedule = (IrrigationSchedule) request.getAttribute("schedule");
    List<Crop> crops = (List<Crop>) request.getAttribute("crops");
    List<Season> seasons = (List<Season>) request.getAttribute("seasons");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    if (errors == null) errors = java.util.Map.of();
    boolean updating = "update".equals(request.getAttribute("formAction"));
    String selectedCropId = request.getParameter("cropId");
    if (selectedCropId == null && schedule != null && schedule.getSeasonId() > 0 && seasons != null) {
        for (Season season : seasons) if (season.getId() == schedule.getSeasonId()) selectedCropId = String.valueOf(season.getCropId());
    }
%>
<!doctype html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= esc((String) request.getAttribute("pageTitle")) %> | GreenFields</title><link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css"></head>
<body><header class="dashboard-nav"><a class="brand" href="<%= request.getContextPath() %>/dashboard">GreenFields <span>Agri Farm</span></a><div class="nav-account"><a href="<%= request.getContextPath() %>/irrigation">Irrigation schedules</a></div></header>
<main class="page-narrow irrigation-form-page"><div class="page-header"><div><p class="eyebrow">Water management</p><h1><%= esc((String) request.getAttribute("pageTitle")) %></h1></div><a class="button button-secondary" href="<%= request.getContextPath() %>/irrigation">Cancel</a></div>
    <% if (request.getAttribute("errorMessage") != null) { %><div class="notice notice-error"><%= esc((String) request.getAttribute("errorMessage")) %></div><% } %>
    <form class="crop-form fertilizer-form" method="post" action="<%= request.getContextPath() %>/irrigation" novalidate>
        <input type="hidden" name="action" value="<%= updating ? "update" : "create" %>"><% if (updating) { %><input type="hidden" name="id" value="<%= schedule.getId() %>"><% } %>
        <div class="form-grid">
            <label>Crop <span class="required-mark">*</span><select name="cropId" id="formIrrigationCrop" required><option value="">Select crop</option>
                <% if (crops != null) for (Crop crop : crops) { %><option value="<%= crop.getId() %>" <%= String.valueOf(crop.getId()).equals(selectedCropId) ? "selected" : "" %>><%= esc(crop.getLabel()) %></option><% } %>
            </select><% if (errors.containsKey("cropId")) { %><small class="field-error"><%= esc(errors.get("cropId")) %></small><% } %></label>
            <label>Season <span class="required-mark">*</span><select name="seasonId" id="formIrrigationSeason" required><option value="">Select season</option>
                <% if (seasons != null) for (Season season : seasons) { %><option value="<%= season.getId() %>" data-crop="<%= season.getCropId() %>" <%= schedule != null && season.getId() == schedule.getSeasonId() ? "selected" : "" %>><%= esc(season.getSeasonName()) %></option><% } %>
            </select><% if (errors.containsKey("seasonId")) { %><small class="field-error"><%= esc(errors.get("seasonId")) %></small><% } %></label>
            <label>Scheduled date <span class="required-mark">*</span><input type="date" name="scheduledDate" required value="<%= schedule == null || schedule.getScheduledDate() == null ? "" : schedule.getScheduledDate() %>"><% if (errors.containsKey("scheduledDate")) { %><small class="field-error"><%= esc(errors.get("scheduledDate")) %></small><% } %></label>
            <label>Actual date<input type="date" name="actualDate" value="<%= schedule == null || schedule.getActualDate() == null ? "" : schedule.getActualDate() %>"><% if (errors.containsKey("actualDate")) { %><small class="field-error"><%= esc(errors.get("actualDate")) %></small><% } %></label>
            <label>Method <span class="required-mark">*</span><select name="method" required><option value="">Select method</option>
                <% for (String method : List.of("drip", "sprinkler", "flood", "manual")) { %><option value="<%= method %>" <%= schedule != null && method.equals(schedule.getMethod()) ? "selected" : "" %>><%= method.substring(0, 1).toUpperCase() + method.substring(1) %></option><% } %>
            </select><% if (errors.containsKey("method")) { %><small class="field-error"><%= esc(errors.get("method")) %></small><% } %></label>
            <label>Water volume (litres) <span class="required-mark">*</span><input type="number" name="waterVolumeLitres" min="0.01" max="99999999.99" step="0.01" required value="<%= schedule == null || schedule.getWaterVolumeLitres() == 0 ? "" : schedule.getWaterVolumeLitres() %>"><% if (errors.containsKey("waterVolumeLitres")) { %><small class="field-error"><%= esc(errors.get("waterVolumeLitres")) %></small><% } %></label>
            <label>Status <span class="required-mark">*</span><select name="status" required><option value="">Select status</option>
                <% for (String status : List.of("scheduled", "completed", "skipped")) { %><option value="<%= status %>" <%= schedule != null && status.equals(schedule.getScheduleStatus()) ? "selected" : "" %>><%= status.substring(0, 1).toUpperCase() + status.substring(1) %></option><% } %>
            </select><% if (errors.containsKey("status")) { %><small class="field-error"><%= esc(errors.get("status")) %></small><% } %></label>
            <label class="field-wide">Notes<textarea name="notes" rows="4"><%= esc(schedule == null ? null : schedule.getNotes()) %></textarea><% if (errors.containsKey("notes")) { %><small class="field-error"><%= esc(errors.get("notes")) %></small><% } %></label>
        </div>
        <div class="form-actions"><button class="button button-primary" type="submit"><%= updating ? "Save changes" : "Add schedule" %></button><a class="text-link" href="<%= request.getContextPath() %>/irrigation">Cancel</a></div>
    </form>
</main>
<script>
    const cropSelect = document.getElementById('formIrrigationCrop');
    const seasonSelect = document.getElementById('formIrrigationSeason');
    cropSelect.addEventListener('change', () => {
        let selectedVisible = false;
        for (const option of seasonSelect.options) {
            if (!option.value) continue;
            option.hidden = !!cropSelect.value && option.dataset.crop !== cropSelect.value;
            if (option.selected && !option.hidden) selectedVisible = true;
        }
        if (!selectedVisible) seasonSelect.value = '';
    });
    cropSelect.dispatchEvent(new Event('change'));
</script></body></html>
