package com.greenfields.servlet;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.FertilizerApplicationDAO;
import com.greenfields.dao.HarvestRecordDAO;
import com.greenfields.dao.IrrigationScheduleDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.dao.impl.CropDAOImpl;
import com.greenfields.dao.impl.FertilizerApplicationDAOImpl;
import com.greenfields.dao.impl.HarvestRecordDAOImpl;
import com.greenfields.dao.impl.IrrigationScheduleDAOImpl;
import com.greenfields.dao.impl.SeasonDAOImpl;
import com.greenfields.model.Crop;
import com.greenfields.model.ReportsViewModel;
import com.greenfields.model.Season;
import com.greenfields.service.FarmReportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "ReportsServlet", urlPatterns = {"/reports"})
public class ReportsServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ReportsServlet.class.getName());
    private final CropDAO cropDAO;
    private final SeasonDAO seasonDAO;
    private final FarmReportService reportService;

    public ReportsServlet() {
        this(new CropDAOImpl(), new SeasonDAOImpl(), new FertilizerApplicationDAOImpl(),
                new IrrigationScheduleDAOImpl(), new HarvestRecordDAOImpl());
    }

    public ReportsServlet(CropDAO cropDAO, SeasonDAO seasonDAO, FertilizerApplicationDAO fertilizerDAO,
                          IrrigationScheduleDAO irrigationDAO, HarvestRecordDAO harvestDAO) {
        this.cropDAO = cropDAO;
        this.seasonDAO = seasonDAO;
        this.reportService = new FarmReportService(cropDAO, seasonDAO, fertilizerDAO, irrigationDAO, harvestDAO);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Crop> crops = List.of();
        List<Season> seasons = List.of();
        int cropId = 0;
        int seasonId = 0;
        String error = null;
        ReportsViewModel model = emptyModel(crops, seasons, 0, 0, null);
        try {
            crops = cropDAO.findAll();
            seasons = seasonDAO.findAll();
            cropId = parseFilter(request.getParameter("cropId"), "crop");
            seasonId = parseFilter(request.getParameter("seasonId"), "season");
            model = reportService.buildReport(crops, seasons, cropId, seasonId);
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
            model = emptyModel(crops, seasons, cropId, seasonId, error);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to generate farm report", e);
            error = "Report data is temporarily unavailable. Please try again later.";
            model = emptyModel(crops, seasons, cropId, seasonId, error);
        }
        request.setAttribute("reports", model);
        request.setAttribute("errorMessage", error);
        request.getRequestDispatcher("/jsp/reports.jsp").forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private static int parseFilter(String value, String label) {
        if (value == null || value.isBlank()) return 0;
        try {
            int id = Integer.parseInt(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Choose a valid " + label + " filter.");
    }

    private static ReportsViewModel emptyModel(List<Crop> crops, List<Season> seasons,
                                               int cropId, int seasonId, String error) {
        return new ReportsViewModel(crops, seasons, cropId, seasonId, null, null, error,
                0, null, 0, 0, 0, List.of(), List.of(), List.of(), List.of());
    }
}
