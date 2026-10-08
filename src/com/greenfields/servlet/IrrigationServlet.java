package com.greenfields.servlet;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.IrrigationScheduleDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.dao.impl.CropDAOImpl;
import com.greenfields.dao.impl.IrrigationScheduleDAOImpl;
import com.greenfields.dao.impl.SeasonDAOImpl;
import com.greenfields.model.Crop;
import com.greenfields.model.IrrigationSchedule;
import com.greenfields.model.IrrigationScheduleView;
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

@WebServlet(name = "IrrigationServlet", urlPatterns = {"/irrigation"})
public class IrrigationServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(IrrigationServlet.class.getName());
    private static final List<String> METHODS = List.of("drip", "sprinkler", "flood", "manual");
    private static final List<String> STATUSES = List.of("scheduled", "completed", "skipped");
    private final IrrigationScheduleDAO irrigationDAO;
    private final CropDAO cropDAO;
    private final SeasonDAO seasonDAO;

    public IrrigationServlet() {
        this(new IrrigationScheduleDAOImpl(), new CropDAOImpl(), new SeasonDAOImpl());
    }

    public IrrigationServlet(IrrigationScheduleDAO irrigationDAO, CropDAO cropDAO, SeasonDAO seasonDAO) {
        this.irrigationDAO = irrigationDAO;
        this.cropDAO = cropDAO;
        this.seasonDAO = seasonDAO;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null || action.isBlank() || "list".equals(action)) {
            showList(request, response);
        } else if ("new".equals(action)) {
            if (requireAdmin(request, response)) showForm(request, response, new IrrigationSchedule(), "create");
        } else if ("edit".equals(action)) {
            if (!requireAdmin(request, response)) return;
            IrrigationSchedule schedule = findSchedule(request, response);
            if (schedule != null) showForm(request, response, schedule, "update");
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
            Map<Integer, Crop> cropById = new LinkedHashMap<>();
            Map<Integer, Season> seasonById = new LinkedHashMap<>();
            crops.forEach(crop -> cropById.put(crop.getId(), crop));
            seasons.forEach(season -> seasonById.put(season.getId(), season));

            List<IrrigationSchedule> rows;
            if (seasonId > 0) rows = irrigationDAO.findBySeasonId(seasonId);
            else if (cropId > 0) rows = irrigationDAO.findByCropId(cropId);
            else rows = irrigationDAO.findAll();
            if (cropId > 0 && seasonId > 0) {
                Season selected = seasonById.get(seasonId);
                rows = selected == null || selected.getCropId() != cropId ? List.of()
                        : rows.stream().filter(row -> row.getSeasonId() == seasonId).toList();
            }

            List<IrrigationScheduleView> views = new ArrayList<>();
            for (IrrigationSchedule row : rows) {
                Season season = seasonById.get(row.getSeasonId());
                Crop crop = season == null ? null : cropById.get(season.getCropId());
                views.add(new IrrigationScheduleView(row,
                        crop == null ? "Unavailable crop" : crop.getLabel(),
                        season == null ? "Unavailable season" : season.getSeasonName()));
            }
            request.setAttribute("schedules", views);
            request.setAttribute("crops", crops);
            request.setAttribute("seasons", seasons);
            request.setAttribute("selectedCropId", cropId);
            request.setAttribute("selectedSeasonId", seasonId);
            request.setAttribute("currentUser", currentUser(request));
            request.setAttribute("message", messageFor(request.getParameter("message")));
            request.getRequestDispatcher("/jsp/irrigation.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load irrigation schedules", e);
            request.setAttribute("schedules", List.of());
            request.setAttribute("crops", List.of());
            request.setAttribute("seasons", List.of());
            request.setAttribute("currentUser", currentUser(request));
            request.setAttribute("errorMessage", "Irrigation schedules could not be loaded. Please try again.");
            request.getRequestDispatcher("/jsp/irrigation.jsp").forward(request, response);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          IrrigationSchedule schedule, String action) throws ServletException, IOException {
        try {
            request.setAttribute("schedule", schedule);
            request.setAttribute("crops", cropDAO.findAll());
            request.setAttribute("seasons", seasonDAO.findAll());
            request.setAttribute("formAction", action);
            request.setAttribute("pageTitle", "create".equals(action) ? "Add irrigation schedule" : "Edit irrigation schedule");
            request.getRequestDispatcher("/jsp/irrigation-form.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load irrigation form data", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response, boolean updating)
            throws ServletException, IOException {
        IrrigationSchedule schedule = new IrrigationSchedule();
        Map<String, String> errors = new LinkedHashMap<>();
        schedule.setMethod(trim(request.getParameter("method")));
        schedule.setStatus(trim(request.getParameter("status")));
        schedule.setNotes(trim(request.getParameter("notes")));

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
                else schedule.setSeasonId(seasonId);
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Unable to validate irrigation crop/season", e);
                request.setAttribute("errorMessage", "The crop and season could not be verified. Please try again.");
                forwardForm(request, response, schedule, updating, Map.of());
                return;
            }
        }

        try {
            String date = trim(request.getParameter("scheduledDate"));
            if (date == null) throw new DateTimeParseException("missing", "", 0);
            schedule.setScheduledDate(LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            errors.put("scheduledDate", "Enter a valid scheduled date.");
        }
        String actualDate = trim(request.getParameter("actualDate"));
        if (actualDate != null) {
            try { schedule.setActualDate(LocalDate.parse(actualDate)); }
            catch (DateTimeParseException e) { errors.put("actualDate", "Enter a valid actual date or leave it blank."); }
        }
        if (schedule.getMethod() == null || !METHODS.contains(schedule.getMethod())) {
            errors.put("method", "Choose a valid irrigation method.");
        }
        if (schedule.getScheduleStatus() == null || !STATUSES.contains(schedule.getScheduleStatus())) {
            errors.put("status", "Choose a valid schedule status.");
        }
        String volumeText = trim(request.getParameter("waterVolumeLitres"));
        if (volumeText == null) {
            errors.put("waterVolumeLitres", "Water volume is required.");
        } else {
            try {
                BigDecimal volume = new BigDecimal(volumeText);
                if (volume.signum() <= 0 || volume.compareTo(new BigDecimal("99999999.99")) > 0 || volume.scale() > 2) {
                    errors.put("waterVolumeLitres", "Enter a positive volume up to 99,999,999.99 litres with at most two decimals.");
                } else schedule.setWaterVolumeLitres(volume.doubleValue());
            } catch (NumberFormatException e) {
                errors.put("waterVolumeLitres", "Enter a valid water volume.");
            }
        }
        if (schedule.getNotes() != null
                && schedule.getNotes().getBytes(StandardCharsets.UTF_8).length > 65_535) {
            errors.put("notes", "Notes must be 65,535 UTF-8 bytes or fewer.");
        }

        int id = updating ? parseId(request.getParameter("id")) : 0;
        if (updating && id == 0) errors.put("id", "The irrigation schedule identifier is invalid.");
        if (updating && id > 0 && findScheduleQuietly(id) == null) errors.put("id", "This irrigation schedule no longer exists.");
        schedule.setId(id);
        if (!errors.isEmpty()) {
            request.setAttribute("errorMessage", "Please correct the highlighted fields.");
            forwardForm(request, response, schedule, updating, errors);
            return;
        }
        try {
            if (updating) irrigationDAO.update(schedule);
            else irrigationDAO.save(schedule);
            response.sendRedirect(request.getContextPath() + "/irrigation?message=" + (updating ? "updated" : "created"));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to save irrigation schedule", e);
            request.setAttribute("errorMessage", "The irrigation schedule could not be saved. Verify the selected crop and season still exist, then try again.");
            forwardForm(request, response, schedule, updating, Map.of());
        }
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response, IrrigationSchedule schedule,
                             boolean updating, Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("schedule", schedule);
        request.setAttribute("formAction", updating ? "update" : "create");
        request.setAttribute("pageTitle", updating ? "Edit irrigation schedule" : "Add irrigation schedule");
        request.setAttribute("errors", errors);
        try {
            request.setAttribute("crops", cropDAO.findAll());
            request.setAttribute("seasons", seasonDAO.findAll());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to reload irrigation form options", e);
            request.setAttribute("crops", List.of());
            request.setAttribute("seasons", List.of());
        }
        request.getRequestDispatcher("/jsp/irrigation-form.jsp").forward(request, response);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id == 0) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }
        try {
            if (findScheduleQuietly(id) == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            irrigationDAO.delete(id);
            response.sendRedirect(request.getContextPath() + "/irrigation?message=deleted");
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to delete irrigation schedule " + id, e);
            response.sendRedirect(request.getContextPath() + "/irrigation?message=delete_failed");
        }
    }

    private IrrigationSchedule findSchedule(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id == 0) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); return null; }
        try {
            IrrigationSchedule schedule = findScheduleQuietly(id);
            if (schedule == null) response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return schedule;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load irrigation schedule " + id, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    private IrrigationSchedule findScheduleQuietly(int id) {
        return irrigationDAO.findAll().stream().filter(row -> row.getId() == id).findFirst().orElse(null);
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
            case "created" -> "Irrigation schedule added successfully.";
            case "updated" -> "Irrigation schedule updated successfully.";
            case "deleted" -> "Irrigation schedule deleted successfully.";
            case "delete_failed" -> "The irrigation schedule could not be deleted. Please refresh and try again.";
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
