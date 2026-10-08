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
import com.greenfields.model.CropHistoryViewModel;
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

@WebServlet(name = "CropHistoryServlet", urlPatterns = {"/crop-history"})
public class CropHistoryServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CropHistoryServlet.class.getName());
    private final CropDAO cropDAO;
    private final FarmReportService reportService;

    public CropHistoryServlet() {
        this(new CropDAOImpl(), new SeasonDAOImpl(), new FertilizerApplicationDAOImpl(),
                new IrrigationScheduleDAOImpl(), new HarvestRecordDAOImpl());
    }

    public CropHistoryServlet(CropDAO cropDAO, SeasonDAO seasonDAO, FertilizerApplicationDAO fertilizerDAO,
                              IrrigationScheduleDAO irrigationDAO, HarvestRecordDAO harvestDAO) {
        this.cropDAO = cropDAO;
        this.reportService = new FarmReportService(cropDAO, seasonDAO, fertilizerDAO, irrigationDAO, harvestDAO);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Crop> crops = List.of();
        CropHistoryViewModel model = new CropHistoryViewModel(crops, null, null, List.of(), List.of(), List.of(), List.of());
        try {
            crops = cropDAO.findAll();
            String rawId = request.getParameter("cropId");
            if (rawId != null && !rawId.isBlank()) {
                int cropId;
                try {
                    cropId = Integer.parseInt(rawId);
                    if (cropId <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    model = new CropHistoryViewModel(crops, null, "Choose a valid crop.", List.of(), List.of(), List.of(), List.of());
                    request.setAttribute("history", model);
                    request.getRequestDispatcher("/jsp/crop-history.jsp").forward(request, response);
                    return;
                }
                model = reportService.buildCropHistory(crops, cropId);
            } else {
                model = new CropHistoryViewModel(crops, null, null, List.of(), List.of(), List.of(), List.of());
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load crop history", e);
            model = new CropHistoryViewModel(crops, null,
                    "Crop history is temporarily unavailable. Please try again later.", List.of(), List.of(), List.of(), List.of());
        }
        request.setAttribute("history", model);
        request.getRequestDispatcher("/jsp/crop-history.jsp").forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
