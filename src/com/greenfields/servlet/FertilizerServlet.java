package com.greenfields.servlet;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.FertilizerApplicationDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.dao.impl.CropDAOImpl;
import com.greenfields.dao.impl.FertilizerApplicationDAOImpl;
import com.greenfields.dao.impl.SeasonDAOImpl;
import com.greenfields.exception.DatabaseException;
import com.greenfields.model.Crop;
import com.greenfields.model.FertilizerApplication;
import com.greenfields.model.FertilizerApplicationView;
import com.greenfields.model.Season;
import com.greenfields.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "FertilizerServlet", urlPatterns = {"/fertilizers"})
public class FertilizerServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(FertilizerServlet.class.getName());
    private final FertilizerApplicationDAO fertilizerDAO;
    private final CropDAO cropDAO;
    private final SeasonDAO seasonDAO;

    public FertilizerServlet() {
        this(new FertilizerApplicationDAOImpl(), new CropDAOImpl(), new SeasonDAOImpl());
    }

    public FertilizerServlet(FertilizerApplicationDAO fertilizerDAO, CropDAO cropDAO, SeasonDAO seasonDAO) {
        this.fertilizerDAO = fertilizerDAO;
        this.cropDAO = cropDAO;
        this.seasonDAO = seasonDAO;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null || action.isBlank() || "list".equals(action)) {
            showList(request, response);
        } else if ("new".equals(action)) {
            if (requireAdmin(request, response)) showForm(request, response, new FertilizerApplication(), "create");
        } else if ("edit".equals(action)) {
            if (!requireAdmin(request, response)) return;
            FertilizerApplication application = findApplication(request, response);
            if (application != null) showForm(request, response, application, "update");
        } else if ("delete".equals(action)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (!requireAdmin(request, response)) return;
        if ("create".equals(action) || "update".equals(action)) {
            save(request, response, "update".equals(action));
        } else if ("delete".equals(action)) {
            delete(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            List<Crop> crops = cropDAO.findAll();
            List<Season> seasons = seasonDAO.findAll();
            int cropId = parseId(request.getParameter("cropId"));
            int seasonId = parseId(request.getParameter("seasonId"));
            Map<Integer, Crop> cropById = new LinkedHashMap<>();
            Map<Integer, Season> seasonById = new LinkedHashMap<>();
            crops.forEach(c -> cropById.put(c.getId(), c));
            seasons.forEach(s -> seasonById.put(s.getId(), s));

            List<FertilizerApplication> rows;
            if (seasonId > 0) rows = fertilizerDAO.findBySeasonId(seasonId);
            else if (cropId > 0) rows = fertilizerDAO.findByCropId(cropId);
            else rows = fertilizerDAO.findAll();
            if (cropId > 0 && seasonId > 0) {
                Season selectedSeason = seasonById.get(seasonId);
                rows = selectedSeason == null || selectedSeason.getCropId() != cropId
                        ? List.of() : rows.stream().filter(a -> a.getSeasonId() == seasonId).toList();
            }

            List<FertilizerApplicationView> views = new ArrayList<>();
            for (FertilizerApplication row : rows) {
                Season season = seasonById.get(row.getSeasonId());
                Crop crop = season == null ? null : cropById.get(season.getCropId());
                views.add(new FertilizerApplicationView(row,
                        crop == null ? "Unavailable crop" : crop.getLabel(),
                        season == null ? "Unavailable season" : season.getSeasonName()));
            }
            request.setAttribute("applications", views);
            request.setAttribute("crops", crops);
            request.setAttribute("seasons", seasons);
            request.setAttribute("selectedCropId", cropId);
            request.setAttribute("selectedSeasonId", seasonId);
            request.setAttribute("currentUser", currentUser(request));
            request.setAttribute("message", messageFor(request.getParameter("message")));
            request.getRequestDispatcher("/jsp/fertilizers.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load fertilizer applications", e);
            request.setAttribute("applications", List.of());
            request.setAttribute("crops", List.of());
            request.setAttribute("seasons", List.of());
            request.setAttribute("currentUser", currentUser(request));
            request.setAttribute("errorMessage", "Fertilizer records could not be loaded. Please try again.");
            request.getRequestDispatcher("/jsp/fertilizers.jsp").forward(request, response);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          FertilizerApplication application, String formAction) throws ServletException, IOException {
        try {
            request.setAttribute("application", application);
            request.setAttribute("crops", cropDAO.findAll());
            request.setAttribute("seasons", seasonDAO.findAll());
            request.setAttribute("formAction", formAction);
            request.setAttribute("pageTitle", "create".equals(formAction)
                    ? "Add fertilizer application" : "Edit fertilizer application");
            request.getRequestDispatcher("/jsp/fertilizer-form.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load fertilizer form data", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response, boolean updating)
            throws ServletException, IOException {
        FertilizerApplication application = new FertilizerApplication();
        Map<String, String> errors = new LinkedHashMap<>();
        application.setFertilizerName(trim(request.getParameter("fertilizerName")));
        application.setFertilizerType(trim(request.getParameter("fertilizerType")));
        application.setAppliedBy(trim(request.getParameter("appliedBy")));
        application.setNotes(trim(request.getParameter("notes")));

        requiredLength("fertilizerName", "Fertilizer name", application.getFertilizerName(), 100, errors);
        if (application.getFertilizerType() == null
                || !List.of("chemical", "organic", "bio").contains(application.getFertilizerType())) {
            errors.put("fertilizerType", "Choose a valid fertilizer type.");
        }
        if (application.getAppliedBy() != null && application.getAppliedBy().length() > 100) {
            errors.put("appliedBy", "Applied by must be 100 characters or fewer.");
        }
        if (application.getNotes() != null
                && application.getNotes().getBytes(StandardCharsets.UTF_8).length > 65_535) {
            errors.put("notes", "Notes must be 65,535 UTF-8 bytes or fewer.");
        }
        try {
            double quantity = Double.parseDouble(trim(request.getParameter("quantityKg")));
            if (!Double.isFinite(quantity) || quantity <= 0 || quantity > 999_999.99
                    || BigDecimalScale(request.getParameter("quantityKg")) > 2) {
                errors.put("quantityKg", "Enter a positive quantity with up to two decimal places.");
            } else application.setQuantityKg(quantity);
        } catch (RuntimeException e) {
            errors.put("quantityKg", "Enter a valid quantity.");
        }
        try {
            String date = trim(request.getParameter("applicationDate"));
            if (date == null) throw new DateTimeParseException("missing", "", 0);
            application.setApplicationDate(LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            errors.put("applicationDate", "Enter a valid application date.");
        }

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
                else application.setSeasonId(seasonId);
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Unable to validate fertilizer crop/season", e);
                request.setAttribute("errorMessage", "The crop and season could not be verified. Please try again.");
                forwardForm(request, response, application, updating, Map.of());
                return;
            }
        }

        int id = updating ? parseId(request.getParameter("id")) : 0;
        if (updating && id == 0) errors.put("id", "The fertilizer record identifier is invalid.");
        FertilizerApplication existing = updating && id > 0 ? findApplicationQuietly(id) : null;
        if (updating && id > 0 && existing == null) errors.put("id", "This fertilizer record no longer exists.");
        application.setId(id);
        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("errorMessage", "Please correct the highlighted fields.");
            forwardForm(request, response, application, updating, errors);
            return;
        }
        try {
            if (updating) fertilizerDAO.update(application);
            else fertilizerDAO.save(application);
            response.sendRedirect(request.getContextPath() + "/fertilizers?message="
                    + (updating ? "updated" : "created"));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to persist fertilizer application", e);
            request.setAttribute("errorMessage", "The fertilizer application could not be saved. Verify the crop and season still exist, then try again.");
            forwardForm(request, response, application, updating, Map.of());
        }
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response,
                             FertilizerApplication application, boolean updating, Map<String, String> errors)
            throws ServletException, IOException {
        request.setAttribute("application", application);
        request.setAttribute("formAction", updating ? "update" : "create");
        request.setAttribute("pageTitle", updating ? "Edit fertilizer application" : "Add fertilizer application");
        request.setAttribute("errors", errors);
        try {
            request.setAttribute("crops", cropDAO.findAll());
            request.setAttribute("seasons", seasonDAO.findAll());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to reload fertilizer form data", e);
            request.setAttribute("crops", List.of());
            request.setAttribute("seasons", List.of());
        }
        request.getRequestDispatcher("/jsp/fertilizer-form.jsp").forward(request, response);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id == 0) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }
        try {
            if (findApplicationQuietly(id) == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            fertilizerDAO.delete(id);
            response.sendRedirect(request.getContextPath() + "/fertilizers?message=deleted");
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to delete fertilizer application " + id, e);
            response.sendRedirect(request.getContextPath() + "/fertilizers?message=delete_failed");
        }
    }

    private FertilizerApplication findApplication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id == 0) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); return null; }
        try {
            FertilizerApplication row = findApplicationQuietly(id);
            if (row == null) response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return row;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load fertilizer application " + id, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    private FertilizerApplication findApplicationQuietly(int id) {
        return fertilizerDAO.findAll().stream().filter(row -> row.getId() == id).findFirst().orElse(null);
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
            case "created" -> "Fertilizer application added successfully.";
            case "updated" -> "Fertilizer application updated successfully.";
            case "deleted" -> "Fertilizer application deleted successfully.";
            case "delete_failed" -> "The fertilizer application could not be deleted. Please refresh and try again.";
            default -> null;
        };
    }

    private static void requiredLength(String field, String label, String value, int max, Map<String, String> errors) {
        if (value == null) errors.put(field, label + " is required.");
        else if (value.length() > max) errors.put(field, label + " must be " + max + " characters or fewer.");
    }

    private static int BigDecimalScale(String value) {
        try { return new java.math.BigDecimal(value.trim()).scale(); }
        catch (RuntimeException e) { return Integer.MAX_VALUE; }
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
