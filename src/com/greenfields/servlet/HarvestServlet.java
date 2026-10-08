package com.greenfields.servlet;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.HarvestRecordDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.dao.impl.CropDAOImpl;
import com.greenfields.dao.impl.HarvestRecordDAOImpl;
import com.greenfields.dao.impl.SeasonDAOImpl;
import com.greenfields.model.Crop;
import com.greenfields.model.HarvestRecord;
import com.greenfields.model.HarvestRecordView;
import com.greenfields.model.Season;
import com.greenfields.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "HarvestServlet", urlPatterns = {"/harvest"})
public class HarvestServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(HarvestServlet.class.getName());
    private static final List<String> GRADES = List.of("A", "B", "C", "reject");
    private static final BigDecimal MAX_YIELD = new BigDecimal("99999999.99");
    private final HarvestRecordDAO harvestDAO;
    private final CropDAO cropDAO;
    private final SeasonDAO seasonDAO;

    public HarvestServlet() {
        this(new HarvestRecordDAOImpl(), new CropDAOImpl(), new SeasonDAOImpl());
    }

    public HarvestServlet(HarvestRecordDAO harvestDAO, CropDAO cropDAO, SeasonDAO seasonDAO) {
        this.harvestDAO = harvestDAO;
        this.cropDAO = cropDAO;
        this.seasonDAO = seasonDAO;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null || action.isBlank() || "list".equals(action)) {
            showList(request, response);
        } else if ("new".equals(action)) {
            if (requireAdmin(request, response)) showForm(request, response, new HarvestRecord(), "create");
        } else if ("edit".equals(action)) {
            if (!requireAdmin(request, response)) return;
            HarvestRecord record = findRecord(request, response);
            if (record != null) showForm(request, response, record, "update");
        } else if ("delete".equals(action)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireAdmin(request, response)) return;
        String action = request.getParameter("action");
        if ("create".equals(action) || "update".equals(action)) save(request, response, "update".equals(action));
        else if ("delete".equals(action)) delete(request, response);
        else response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            List<Crop> crops = cropDAO.findAll();
            List<Season> seasons = seasonDAO.findAll();
            int cropId = parseId(request.getParameter("cropId"));
            int seasonId = parseId(request.getParameter("seasonId"));
            Map<Integer, Crop> cropsById = new LinkedHashMap<>();
            Map<Integer, Season> seasonsById = new LinkedHashMap<>();
            crops.forEach(crop -> cropsById.put(crop.getId(), crop));
            seasons.forEach(season -> seasonsById.put(season.getId(), season));

            List<HarvestRecord> records;
            if (seasonId > 0) records = harvestDAO.findBySeasonId(seasonId);
            else if (cropId > 0) records = harvestDAO.findByCropId(cropId);
            else records = harvestDAO.findAll();
            if (cropId > 0 && seasonId > 0) {
                Season selected = seasonsById.get(seasonId);
                records = selected == null || selected.getCropId() != cropId ? List.of()
                        : records.stream().filter(record -> record.getSeasonId() == seasonId).toList();
            }

            List<HarvestRecordView> views = new ArrayList<>();
            for (HarvestRecord record : records) {
                Season season = seasonsById.get(record.getSeasonId());
                Crop crop = season == null ? null : cropsById.get(season.getCropId());
                views.add(new HarvestRecordView(record,
                        crop == null ? "Unavailable crop" : crop.getLabel(),
                        season == null ? "Unavailable season" : season.getSeasonName()));
            }
            request.setAttribute("records", views);
            request.setAttribute("crops", crops);
            request.setAttribute("seasons", seasons);
            request.setAttribute("selectedCropId", cropId);
            request.setAttribute("selectedSeasonId", seasonId);
            request.setAttribute("currentUser", currentUser(request));
            request.setAttribute("message", messageFor(request.getParameter("message")));
            request.getRequestDispatcher("/jsp/harvest.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load harvest records", e);
            request.setAttribute("records", List.of());
            request.setAttribute("crops", List.of());
            request.setAttribute("seasons", List.of());
            request.setAttribute("currentUser", currentUser(request));
            request.setAttribute("errorMessage", "Harvest records could not be loaded. Please try again.");
            request.getRequestDispatcher("/jsp/harvest.jsp").forward(request, response);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          HarvestRecord record, String action) throws ServletException, IOException {
        try {
            request.setAttribute("record", record);
            request.setAttribute("crops", cropDAO.findAll());
            request.setAttribute("seasons", seasonDAO.findAll());
            request.setAttribute("formAction", action);
            request.setAttribute("pageTitle", "create".equals(action) ? "Add harvest record" : "Edit harvest record");
            request.getRequestDispatcher("/jsp/harvest-form.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load harvest form data", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response, boolean updating)
            throws ServletException, IOException {
        HarvestRecord record = new HarvestRecord();
        Map<String, String> errors = new LinkedHashMap<>();
        record.setQualityGrade(trim(request.getParameter("qualityGrade")));
        record.setRemarks(trim(request.getParameter("remarks")));
        record.setRecordedBy(trim(request.getParameter("recordedBy")));

        int cropId = parseId(request.getParameter("cropId"));
        int seasonId = parseId(request.getParameter("seasonId"));
        if (cropId == 0) errors.put("cropId", "Choose a crop.");
        if (seasonId == 0) errors.put("seasonId", "Choose a season.");
        if (cropId > 0 && seasonId > 0) {
            try {
                Crop crop = cropDAO.findById(cropId);
                Season season = seasonDAO.findById(seasonId);
                if (crop == null) errors.put("cropId", "The selected crop does not exist.");
                if (season == null) errors.put("seasonId", "The selected season does not exist.");
                else if (season.getCropId() != cropId) errors.put("seasonId", "Choose a season belonging to the selected crop.");
                else record.setSeasonId(seasonId);
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Unable to validate harvest crop/season", e);
                request.setAttribute("errorMessage", "The crop and season could not be verified. Please try again.");
                forwardForm(request, response, record, updating, Map.of());
                return;
            }
        }

        try {
            String date = trim(request.getParameter("harvestDate"));
            if (date == null) throw new DateTimeParseException("missing", "", 0);
            record.setHarvestDate(LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            errors.put("harvestDate", "Enter a valid harvest date.");
        }
        record.setExpectedYieldKg(parseYield(request.getParameter("expectedYieldKg"), "expectedYieldKg", "Expected yield", false, errors));
        String actual = trim(request.getParameter("actualYieldKg"));
        record.setActualYieldKg(actual == null ? null : parseYield(actual, "actualYieldKg", "Actual yield", true, errors));
        if (record.getQualityGrade() != null && !GRADES.contains(record.getQualityGrade())) {
            errors.put("qualityGrade", "Choose a valid quality grade.");
        }
        if (record.getRemarks() != null && record.getRemarks().getBytes(StandardCharsets.UTF_8).length > 65_535) {
            errors.put("remarks", "Remarks must be 65,535 UTF-8 bytes or fewer.");
        }
        if (record.getRecordedBy() != null && record.getRecordedBy().length() > 100) {
            errors.put("recordedBy", "Recorded by must be 100 characters or fewer.");
        }

        int id = updating ? parseId(request.getParameter("id")) : 0;
        if (updating && id == 0) errors.put("id", "The harvest record identifier is invalid.");
        if (updating && id > 0) {
            try {
                if (harvestDAO.findById(id) == null) errors.put("id", "This harvest record no longer exists.");
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Unable to verify harvest record " + id, e);
                request.setAttribute("errorMessage", "The harvest record could not be verified. Please try again.");
                forwardForm(request, response, record, true, errors);
                return;
            }
        }
        record.setId(id);
        if (!errors.isEmpty()) {
            request.setAttribute("errorMessage", "Please correct the highlighted fields.");
            forwardForm(request, response, record, updating, errors);
            return;
        }
        try {
            if (updating) harvestDAO.update(record);
            else harvestDAO.save(record);
            response.sendRedirect(request.getContextPath() + "/harvest?message=" + (updating ? "updated" : "created"));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to save harvest record", e);
            request.setAttribute("errorMessage", "The harvest record could not be saved. Verify the selected crop and season still exist, then try again.");
            forwardForm(request, response, record, updating, Map.of());
        }
    }

    private double parseYield(String raw, String field, String label, boolean actual, Map<String, String> errors) {
        String value = trim(raw);
        if (value == null) {
            if (!actual) errors.put(field, label + " is required.");
            return 0;
        }
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.signum() < 0 || amount.compareTo(MAX_YIELD) > 0 || amount.scale() > 2) {
                errors.put(field, label + " must be between 0 and 99,999,999.99 kg with at most two decimals.");
                return 0;
            }
            return amount.doubleValue();
        } catch (NumberFormatException e) {
            errors.put(field, label + " must be a valid number.");
            return 0;
        }
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response, HarvestRecord record,
                             boolean updating, Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("record", record);
        request.setAttribute("formAction", updating ? "update" : "create");
        request.setAttribute("pageTitle", updating ? "Edit harvest record" : "Add harvest record");
        request.setAttribute("errors", errors);
        try {
            request.setAttribute("crops", cropDAO.findAll());
            request.setAttribute("seasons", seasonDAO.findAll());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to reload harvest form options", e);
            request.setAttribute("crops", List.of());
            request.setAttribute("seasons", List.of());
        }
        request.getRequestDispatcher("/jsp/harvest-form.jsp").forward(request, response);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id == 0) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }
        try {
            if (harvestDAO.findById(id) == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            harvestDAO.delete(id);
            response.sendRedirect(request.getContextPath() + "/harvest?message=deleted");
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to delete harvest record " + id, e);
            response.sendRedirect(request.getContextPath() + "/harvest?message=delete_failed");
        }
    }

    private HarvestRecord findRecord(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id == 0) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); return null; }
        try {
            HarvestRecord record = harvestDAO.findById(id);
            if (record == null) response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return record;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load harvest record " + id, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = currentUser(request);
        if (user == null || !user.isAdmin()) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return false; }
        return true;
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    private String messageFor(String code) {
        if (code == null) return null;
        return switch (code) {
            case "created" -> "Harvest record added successfully.";
            case "updated" -> "Harvest record updated successfully.";
            case "deleted" -> "Harvest record deleted successfully.";
            case "delete_failed" -> "The harvest record could not be deleted. Please refresh and try again.";
            default -> null;
        };
    }

    private static String trim(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private static int parseId(String value) {
        try { int id = Integer.parseInt(value); return id > 0 ? id : 0; }
        catch (RuntimeException e) { return 0; }
    }
}
