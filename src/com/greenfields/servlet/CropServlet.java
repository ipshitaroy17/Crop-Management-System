package com.greenfields.servlet;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.impl.CropDAOImpl;
import com.greenfields.exception.DatabaseException;
import com.greenfields.model.Crop;
import com.greenfields.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "CropServlet", urlPatterns = {"/crops"})
public class CropServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CropServlet.class.getName());
    private final CropDAO cropDAO;

    public CropServlet() {
        this(new CropDAOImpl());
    }

    public CropServlet(CropDAO cropDAO) {
        this.cropDAO = cropDAO;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = valueOrDefault(request.getParameter("action"), "list");
        switch (action) {
            case "details" -> showDetails(request, response);
            case "new" -> showForm(request, response, null, "create");
            case "edit" -> {
                if (requireAdmin(request, response)) {
                    Crop crop = findCrop(request, response);
                    if (crop != null) showForm(request, response, crop, "update");
                }
            }
            case "delete" -> response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            case "list" -> showList(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("create".equals(action) || "update".equals(action)) {
            if (!requireAdmin(request, response)) return;
            saveCrop(request, response, "update".equals(action));
        } else if ("delete".equals(action)) {
            if (!requireAdmin(request, response)) return;
            deleteCrop(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("currentUser", currentUser(request));
        request.setAttribute("message", messageFor(request.getParameter("message")));
        try {
            request.setAttribute("crops", cropDAO.findAll());
            request.getRequestDispatcher("/jsp/crops.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load crop list", e);
            request.setAttribute("crops", List.of());
            request.setAttribute("errorMessage", "Crop records could not be loaded. Please try again.");
            request.getRequestDispatcher("/jsp/crops.jsp").forward(request, response);
        }
    }

    private void showDetails(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Crop crop = findCrop(request, response);
        if (crop == null) return;
        request.setAttribute("currentUser", currentUser(request));
        request.setAttribute("crop", crop);
        request.getRequestDispatcher("/jsp/crop-details.jsp").forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          Crop crop, String formAction)
            throws ServletException, IOException {
        if (!requireAdmin(request, response)) return;
        request.setAttribute("crop", crop == null ? new Crop(0, "", "", "", "", 0, "active") : crop);
        request.setAttribute("formAction", formAction);
        request.setAttribute("pageTitle", "create".equals(formAction) ? "Add crop" : "Edit crop");
        request.getRequestDispatcher("/jsp/crop-form.jsp").forward(request, response);
    }

    private void saveCrop(HttpServletRequest request, HttpServletResponse response, boolean updating)
            throws ServletException, IOException {
        Crop crop = new Crop();
        Map<String, String> errors = new LinkedHashMap<>();
        crop.setCropName(trimToNull(request.getParameter("cropName")));
        crop.setCropType(trimToNull(request.getParameter("cropType")));
        crop.setVariety(trimToNull(request.getParameter("variety")));
        crop.setDescription(trimToNull(request.getParameter("description")));
        crop.setStatus(trimToNull(request.getParameter("status")));

        validateLength("cropName", "Crop name", crop.getCropName(), 100, true, errors);
        validateLength("cropType", "Crop type", crop.getCropType(), 50, true, errors);
        validateLength("variety", "Variety", crop.getVariety(), 100, false, errors);
        if (crop.getDescription() != null
                && crop.getDescription().getBytes(StandardCharsets.UTF_8).length > 65_535) {
            errors.put("description", "Description must be 65,535 UTF-8 bytes or fewer.");
        }
        String duration = trimToNull(request.getParameter("growthDurationDays"));
        try {
            int days = Integer.parseInt(duration == null ? "" : duration);
            if (days <= 0) errors.put("growthDurationDays", "Enter a positive number of days.");
            else crop.setGrowthDurationDays(days);
        } catch (NumberFormatException e) {
            errors.put("growthDurationDays", "Enter a whole number of days.");
        }
        if (!"active".equals(crop.getStatus()) && !"inactive".equals(crop.getStatus())) {
            errors.put("status", "Choose active or inactive.");
        }

        int id = 0;
        if (updating) {
            id = parseId(request.getParameter("id"));
            if (id <= 0) errors.put("id", "The crop identifier is invalid.");
            else {
                try {
                    if (cropDAO.findById(id) == null) errors.put("id", "This crop no longer exists.");
                } catch (RuntimeException e) {
                    LOGGER.log(Level.SEVERE, "Unable to verify crop id " + id, e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }
        }

        if (!errors.isEmpty()) {
            request.setAttribute("crop", crop);
            request.setAttribute("formAction", updating ? "update" : "create");
            request.setAttribute("pageTitle", updating ? "Edit crop" : "Add crop");
            request.setAttribute("errors", errors);
            request.setAttribute("errorMessage", "Please correct the highlighted fields.");
            request.getRequestDispatcher("/jsp/crop-form.jsp").forward(request, response);
            return;
        }

        try {
            if (updating) {
                crop.setId(id);
                cropDAO.update(crop);
            } else {
                cropDAO.save(crop);
            }
            response.sendRedirect(request.getContextPath() + "/crops?message="
                    + (updating ? "updated" : "created"));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to save crop", e);
            request.setAttribute("crop", crop);
            request.setAttribute("formAction", updating ? "update" : "create");
            request.setAttribute("pageTitle", updating ? "Edit crop" : "Add crop");
            request.setAttribute("errorMessage", "The crop could not be saved. Please try again.");
            request.getRequestDispatcher("/jsp/crop-form.jsp").forward(request, response);
        }
    }

    private void deleteCrop(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (cropDAO.findById(id) == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            cropDAO.delete(id);
            response.sendRedirect(request.getContextPath() + "/crops?message=deleted");
        } catch (DatabaseException e) {
            LOGGER.log(Level.WARNING, "Unable to delete crop id " + id, e);
            response.sendRedirect(request.getContextPath() + "/crops?message=delete_failed");
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting crop id " + id, e);
            response.sendRedirect(request.getContextPath() + "/crops?message=delete_failed");
        }
    }

    private Crop findCrop(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        Crop crop;
        try {
            crop = cropDAO.findById(id);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load crop id " + id, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
        if (crop == null) response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return crop;
    }

    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        User user = currentUser(request);
        if (user == null || !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    private String messageFor(String code) {
        if (code == null) return null;
        return switch (code) {
            case "created" -> "Crop added successfully.";
            case "updated" -> "Crop updated successfully.";
            case "deleted" -> "Crop deleted successfully.";
            case "delete_failed" -> "Crop could not be deleted. It may be linked to one or more seasons.";
            default -> null;
        };
    }

    private static void validateLength(String field, String label, String value, int maxLength,
                                       boolean required, Map<String, String> errors) {
        if (value == null && required) errors.put(field, label + " is required.");
        else if (value != null && value.length() > maxLength) {
            errors.put(field, label + " must be " + maxLength + " characters or fewer.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int parseId(String value) {
        try {
            int id = Integer.parseInt(value);
            return id > 0 ? id : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
