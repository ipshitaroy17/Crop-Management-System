<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map,com.greenfields.model.HarvestRecord,com.greenfields.model.Crop,com.greenfields.model.Season" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    HarvestRecord record = (HarvestRecord) request.getAttribute("record");
    List<Crop> crops = (List<Crop>) request.getAttribute("crops");
    List<Season> seasons = (List<Season>) request.getAttribute("seasons");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    if (errors == null) errors = java.util.Map.of();
    boolean updating = "update".equals(request.getAttribute("formAction"));
    String selectedCropId = request.getParameter("cropId");
    if (selectedCropId == null && record != null && record.getSeasonId() > 0 && seasons != null) {
        for (Season season : seasons) if (season.getId() == record.getSeasonId()) selectedCropId = String.valueOf(season.getCropId());
    }
%>
<!doctype html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= esc((String) request.getAttribute("pageTitle")) %> | GreenFields</title><link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css"></head>
<body><header class="dashboard-nav"><a class="farm-brand" href="<%= request.getContextPath() %>/dashboard"><span class="brand-mark" aria-hidden="true">GF</span><span><strong>GreenFields</strong><small>Agri Farm</small></span></a><div class="nav-account"><a href="<%= request.getContextPath() %>/harvest">Harvest &amp; yield</a></div></header>
<main class="page-narrow harvest-form-page"><div class="page-header"><div><p class="eyebrow">Production records</p><h1><%= esc((String) request.getAttribute("pageTitle")) %></h1></div><a class="button button-secondary" href="<%= request.getContextPath() %>/harvest">Cancel</a></div>
    <% if (request.getAttribute("errorMessage") != null) { %><div class="notice notice-error"><%= esc((String) request.getAttribute("errorMessage")) %></div><% } %>
    <form class="crop-form fertilizer-form" method="post" action="<%= request.getContextPath() %>/harvest" novalidate>
        <input type="hidden" name="action" value="<%= updating ? "update" : "create" %>"><% if (updating) { %><input type="hidden" name="id" value="<%= record.getId() %>"><% } %>
        <div class="form-grid">
            <label>Crop <span class="required-mark">*</span><select name="cropId" id="harvestFormCrop" required><option value="">Select crop</option>
                <% if (crops != null) for (Crop crop : crops) { %><option value="<%= crop.getId() %>" <%= String.valueOf(crop.getId()).equals(selectedCropId) ? "selected" : "" %>><%= esc(crop.getLabel()) %></option><% } %>
            </select><% if (errors.containsKey("cropId")) { %><small class="field-error"><%= esc(errors.get("cropId")) %></small><% } %></label>
            <label>Season <span class="required-mark">*</span><select name="seasonId" id="harvestFormSeason" required><option value="">Select season</option>
                <% if (seasons != null) for (Season season : seasons) { %><option value="<%= season.getId() %>" data-crop="<%= season.getCropId() %>" <%= record != null && season.getId() == record.getSeasonId() ? "selected" : "" %>><%= esc(season.getSeasonName()) %></option><% } %>
            </select><% if (errors.containsKey("seasonId")) { %><small class="field-error"><%= esc(errors.get("seasonId")) %></small><% } %></label>
            <label>Harvest date <span class="required-mark">*</span><input type="date" name="harvestDate" required value="<%= record == null || record.getHarvestDate() == null ? "" : record.getHarvestDate() %>"><% if (errors.containsKey("harvestDate")) { %><small class="field-error"><%= esc(errors.get("harvestDate")) %></small><% } %></label>
            <label>Expected yield (kg) <span class="required-mark">*</span><input type="number" name="expectedYieldKg" min="0" max="99999999.99" step="0.01" required value="<%= record == null ? "" : record.getExpectedYieldKg() %>"><% if (errors.containsKey("expectedYieldKg")) { %><small class="field-error"><%= esc(errors.get("expectedYieldKg")) %></small><% } %></label>
            <label>Actual yield (kg)<input type="number" name="actualYieldKg" min="0" max="99999999.99" step="0.01" value="<%= record == null || record.getActualYieldKg() == null ? "" : record.getActualYieldKg() %>"><small class="field-hint">Leave blank while the harvest is pending.</small><% if (errors.containsKey("actualYieldKg")) { %><small class="field-error"><%= esc(errors.get("actualYieldKg")) %></small><% } %></label>
            <label>Quality grade<select name="qualityGrade"><option value="">Not graded</option>
                <% for (String grade : List.of("A", "B", "C", "reject")) { %><option value="<%= grade %>" <%= record != null && grade.equals(record.getQualityGrade()) ? "selected" : "" %>><%= grade.equals("reject") ? "Reject" : grade %></option><% } %>
            </select><% if (errors.containsKey("qualityGrade")) { %><small class="field-error"><%= esc(errors.get("qualityGrade")) %></small><% } %></label>
            <label>Recorded by<input name="recordedBy" maxlength="100" value="<%= esc(record == null ? null : record.getRecordedBy()) %>"><% if (errors.containsKey("recordedBy")) { %><small class="field-error"><%= esc(errors.get("recordedBy")) %></small><% } %></label>
            <label class="field-wide">Remarks<textarea name="remarks" rows="4"><%= esc(record == null ? null : record.getRemarks()) %></textarea><% if (errors.containsKey("remarks")) { %><small class="field-error"><%= esc(errors.get("remarks")) %></small><% } %></label>
        </div>
        <div class="form-actions"><button class="button button-primary" type="submit"><%= updating ? "Save changes" : "Add harvest record" %></button><a class="text-link" href="<%= request.getContextPath() %>/harvest">Cancel</a></div>
    </form>
</main>
<script>
    const cropSelect = document.getElementById('harvestFormCrop');
    const seasonSelect = document.getElementById('harvestFormSeason');
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
