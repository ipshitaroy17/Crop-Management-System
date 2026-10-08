<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map,com.greenfields.model.FertilizerApplication,com.greenfields.model.Crop,com.greenfields.model.Season" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    FertilizerApplication app = (FertilizerApplication) request.getAttribute("application");
    List<Crop> crops = (List<Crop>) request.getAttribute("crops");
    List<Season> seasons = (List<Season>) request.getAttribute("seasons");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    if (errors == null) errors = java.util.Map.of();
    String formAction = (String) request.getAttribute("formAction");
    boolean updating = "update".equals(formAction);
    String selectedCropId = request.getParameter("cropId");
    if (selectedCropId == null && app != null && app.getSeasonId() > 0 && seasons != null) {
        for (Season season : seasons) if (season.getId() == app.getSeasonId()) selectedCropId = String.valueOf(season.getCropId());
    }
%>
<!doctype html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= esc((String) request.getAttribute("pageTitle")) %> | GreenFields</title><link rel="stylesheet" href="<%= request.getContextPath() %>/css/greenfields.css"></head>
<body><header class="dashboard-nav"><a class="brand" href="<%= request.getContextPath() %>/dashboard">GreenFields <span>Agri Farm</span></a><div class="nav-account"><a href="<%= request.getContextPath() %>/fertilizers">Fertilizer applications</a></div></header>
<main class="page-narrow fertilizer-form-page"><div class="page-header"><div><p class="eyebrow">Farm inputs</p><h1><%= esc((String) request.getAttribute("pageTitle")) %></h1></div><a class="button button-secondary" href="<%= request.getContextPath() %>/fertilizers">Cancel</a></div>
    <% if (request.getAttribute("errorMessage") != null) { %><div class="notice error-notice"><%= esc((String) request.getAttribute("errorMessage")) %></div><% } %>
    <form class="crop-form fertilizer-form" method="post" action="<%= request.getContextPath() %>/fertilizers" novalidate>
        <input type="hidden" name="action" value="<%= updating ? "update" : "create" %>"><% if (updating) { %><input type="hidden" name="id" value="<%= app.getId() %>"><% } %>
        <div class="form-grid">
            <label>Crop <span class="required-mark">*</span><select name="cropId" id="formCrop" required><option value="">Select crop</option>
                <% if (crops != null) for (Crop crop : crops) { %><option value="<%= crop.getId() %>" <%= String.valueOf(crop.getId()).equals(selectedCropId) ? "selected" : "" %>><%= esc(crop.getLabel()) %></option><% } %>
            </select><% if (errors.containsKey("cropId")) { %><small class="field-error"><%= esc(errors.get("cropId")) %></small><% } %></label>
            <label>Season <span class="required-mark">*</span><select name="seasonId" id="formSeason" required><option value="">Select season</option>
                <% if (seasons != null) for (Season season : seasons) { %><option value="<%= season.getId() %>" data-crop="<%= season.getCropId() %>" <%= app != null && season.getId() == app.getSeasonId() ? "selected" : "" %>><%= esc(season.getSeasonName()) %></option><% } %>
            </select><% if (errors.containsKey("seasonId")) { %><small class="field-error"><%= esc(errors.get("seasonId")) %></small><% } %></label>
            <label>Fertilizer name <span class="required-mark">*</span><input name="fertilizerName" maxlength="100" required value="<%= esc(app == null ? null : app.getFertilizerName()) %>"><% if (errors.containsKey("fertilizerName")) { %><small class="field-error"><%= esc(errors.get("fertilizerName")) %></small><% } %></label>
            <label>Type <span class="required-mark">*</span><select name="fertilizerType" required><option value="">Select type</option>
                <% for (String type : List.of("chemical", "organic", "bio")) { %><option value="<%= type %>" <%= app != null && type.equals(app.getFertilizerType()) ? "selected" : "" %>><%= type.substring(0, 1).toUpperCase() + type.substring(1) %></option><% } %>
            </select><% if (errors.containsKey("fertilizerType")) { %><small class="field-error"><%= esc(errors.get("fertilizerType")) %></small><% } %></label>
            <label>Quantity (kg) <span class="required-mark">*</span><input type="number" name="quantityKg" min="0.01" max="999999.99" step="0.01" required value="<%= app == null || app.getQuantityKg() == 0 ? "" : app.getQuantityKg() %>"><% if (errors.containsKey("quantityKg")) { %><small class="field-error"><%= esc(errors.get("quantityKg")) %></small><% } %></label>
            <label>Application date <span class="required-mark">*</span><input type="date" name="applicationDate" required value="<%= app == null || app.getApplicationDate() == null ? "" : app.getApplicationDate() %>"><% if (errors.containsKey("applicationDate")) { %><small class="field-error"><%= esc(errors.get("applicationDate")) %></small><% } %></label>
            <label>Applied by<input name="appliedBy" maxlength="100" value="<%= esc(app == null ? null : app.getAppliedBy()) %>"><% if (errors.containsKey("appliedBy")) { %><small class="field-error"><%= esc(errors.get("appliedBy")) %></small><% } %></label>
            <label class="field-wide">Notes<textarea name="notes" rows="4"><%= esc(app == null ? null : app.getNotes()) %></textarea><% if (errors.containsKey("notes")) { %><small class="field-error"><%= esc(errors.get("notes")) %></small><% } %></label>
        </div>
        <div class="form-actions"><button class="button button-primary" type="submit"><%= updating ? "Save changes" : "Add application" %></button><a class="text-link" href="<%= request.getContextPath() %>/fertilizers">Cancel</a></div>
    </form>
</main>
<script>
    const cropSelect = document.getElementById('formCrop');
    const seasonSelect = document.getElementById('formSeason');
    cropSelect.addEventListener('change', () => {
        const crop = cropSelect.value;
        let selectedVisible = false;
        for (const option of seasonSelect.options) {
            if (!option.value) continue;
            option.hidden = !!crop && option.dataset.crop !== crop;
            if (option.selected && !option.hidden) selectedVisible = true;
        }
        if (!selectedVisible) seasonSelect.value = '';
    });
    cropSelect.dispatchEvent(new Event('change'));
</script></body></html>
