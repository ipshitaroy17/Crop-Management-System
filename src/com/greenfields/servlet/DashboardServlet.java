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
import com.greenfields.model.DashboardViewModel;
import com.greenfields.model.DashboardViewModel.CropPerformance;
import com.greenfields.model.DashboardViewModel.RecentActivity;
import com.greenfields.model.FertilizerApplication;
import com.greenfields.model.HarvestRecord;
import com.greenfields.model.IrrigationSchedule;
import com.greenfields.model.Season;
import com.greenfields.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());
    private static final DateTimeFormatter ACTIVITY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final int RECENT_ACTIVITY_LIMIT = 6;

    private final CropDAO cropDAO;
    private final SeasonDAO seasonDAO;
    private final FertilizerApplicationDAO fertilizerDAO;
    private final IrrigationScheduleDAO irrigationDAO;
    private final HarvestRecordDAO harvestDAO;

    public DashboardServlet() {
        this(new CropDAOImpl(), new SeasonDAOImpl(), new FertilizerApplicationDAOImpl(),
                new IrrigationScheduleDAOImpl(), new HarvestRecordDAOImpl());
    }

    public DashboardServlet(CropDAO cropDAO, SeasonDAO seasonDAO,
                            FertilizerApplicationDAO fertilizerDAO,
                            IrrigationScheduleDAO irrigationDAO, HarvestRecordDAO harvestDAO) {
        this.cropDAO = cropDAO;
        this.seasonDAO = seasonDAO;
        this.fertilizerDAO = fertilizerDAO;
        this.irrigationDAO = irrigationDAO;
        this.harvestDAO = harvestDAO;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = session == null ? null : (User) session.getAttribute("user");
        request.setAttribute("currentUserName", currentUser == null
                || currentUser.getFullName() == null || currentUser.getFullName().isBlank()
                ? "Farm team" : currentUser.getFullName());
        request.setAttribute("currentUserIsAdmin", currentUser != null && currentUser.isAdmin());

        try {
            List<Crop> crops = cropDAO.findAll();
            List<Season> seasons = seasonDAO.findAll();
            List<FertilizerApplication> fertilizerApplications = fertilizerDAO.findAll();
            List<IrrigationSchedule> irrigationSchedules = irrigationDAO.findAll();
            List<HarvestRecord> harvestRecords = harvestDAO.findAll();
            request.setAttribute("dashboard", buildDashboard(crops, seasons, fertilizerApplications,
                    irrigationSchedules, harvestRecords));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load dashboard data", e);
            request.setAttribute("dashboard", null);
            request.setAttribute("dashboardError",
                    "Dashboard data is temporarily unavailable. Please try again later.");
        }

        request.getRequestDispatcher("/jsp/dashboard.jsp").forward(request, response);
    }

    private DashboardViewModel buildDashboard(List<Crop> crops, List<Season> seasons,
                                              List<FertilizerApplication> fertilizers,
                                              List<IrrigationSchedule> irrigation,
                                              List<HarvestRecord> harvests) {
        Map<Integer, Crop> cropsById = crops.stream()
                .collect(Collectors.toMap(Crop::getId, crop -> crop, (first, ignored) -> first));
        Map<Integer, Season> seasonsById = seasons.stream()
                .collect(Collectors.toMap(Season::getId, season -> season, (first, ignored) -> first));
        Map<Integer, YieldAccumulator> yieldsByCrop = new LinkedHashMap<>();
        crops.forEach(crop -> yieldsByCrop.put(crop.getId(), new YieldAccumulator()));

        double expectedYield = 0;
        for (HarvestRecord harvest : harvests) {
            expectedYield += harvest.getExpectedYieldKg();
            Season season = seasonsById.get(harvest.getSeasonId());
            if (season == null) continue;
            YieldAccumulator accumulator = yieldsByCrop.get(season.getCropId());
            if (accumulator != null) accumulator.add(harvest);
        }

        double fertilizerUsed = fertilizers.stream().mapToDouble(FertilizerApplication::getQuantityKg).sum();
        long activeSeasons = seasons.stream().filter(Season::isActive).count();
        List<CropPerformance> performance = buildCropPerformance(crops, yieldsByCrop);
        List<RecentActivity> activities = buildRecentActivities(fertilizers, irrigation, harvests,
                seasonsById, cropsById);
        return new DashboardViewModel(crops.size(), seasons.size(), activeSeasons,
                fertilizers.size(), fertilizerUsed, expectedYield, harvests.size(), performance, activities);
    }

    private List<CropPerformance> buildCropPerformance(List<Crop> crops,
                                                       Map<Integer, YieldAccumulator> yieldsByCrop) {
        double maximum = yieldsByCrop.values().stream()
                .mapToDouble(YieldAccumulator::getScaleValue).max().orElse(0);
        List<CropPerformance> results = new ArrayList<>();
        for (Crop crop : crops) {
            YieldAccumulator yield = yieldsByCrop.get(crop.getId());
            Double achievement = yield.completedHarvests == 0
                    ? null : yield.achievementTotal / yield.completedHarvests;
            results.add(new CropPerformance(crop.getCropName(), yield.expectedYield,
                    yield.completedHarvests == 0 ? null : yield.actualYield,
                    yield.completedHarvests, yield.pendingHarvests, achievement,
                    barPercent(yield.expectedYield, maximum),
                    yield.completedHarvests == 0 ? 0 : barPercent(yield.actualYield, maximum)));
        }
        return results;
    }

    private List<RecentActivity> buildRecentActivities(List<FertilizerApplication> fertilizers,
                                                       List<IrrigationSchedule> irrigation,
                                                       List<HarvestRecord> harvests,
                                                       Map<Integer, Season> seasonsById,
                                                       Map<Integer, Crop> cropsById) {
        List<ActivityEntry> entries = new ArrayList<>();
        for (FertilizerApplication fertilizer : fertilizers) {
            entries.add(new ActivityEntry(fertilizer.getApplicationDate(), new RecentActivity(
                    "Fertilizer", "Fertilizer applied · " + fertilizer.getFertilizerName(),
                    formatNumber(fertilizer.getQuantityKg()) + " kg · "
                            + safeText(fertilizer.getFertilizerType()) + " · "
                            + seasonLabel(fertilizer.getSeasonId(), seasonsById, cropsById),
                    formatDate(fertilizer.getApplicationDate()))));
        }
        for (IrrigationSchedule schedule : irrigation) {
            entries.add(new ActivityEntry(schedule.getScheduledDate(), new RecentActivity(
                    "Irrigation", "Irrigation · " + schedule.getMethodLabel(),
                    safeText(schedule.getScheduleStatus()) + " · "
                            + seasonLabel(schedule.getSeasonId(), seasonsById, cropsById),
                    formatDate(schedule.getScheduledDate()))));
        }
        for (HarvestRecord harvest : harvests) {
            String result = harvest.isHarvestComplete()
                    ? "Actual " + formatNumber(harvest.getActualYieldKg()) + " kg"
                    : "Harvest pending";
            entries.add(new ActivityEntry(harvest.getHarvestDate(), new RecentActivity(
                    "Harvest", "Harvest record", seasonLabel(harvest.getSeasonId(), seasonsById, cropsById)
                            + " · Expected " + formatNumber(harvest.getExpectedYieldKg()) + " kg · " + result,
                    formatDate(harvest.getHarvestDate()))));
        }
        return entries.stream()
                .sorted(Comparator.comparing(ActivityEntry::date,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_ACTIVITY_LIMIT)
                .map(ActivityEntry::activity)
                .toList();
    }

    private static int barPercent(double value, double maximum) {
        if (maximum <= 0 || value <= 0) return 0;
        return (int) Math.max(1, Math.min(100, Math.round(value / maximum * 100)));
    }

    private static String seasonLabel(int seasonId, Map<Integer, Season> seasonsById,
                                      Map<Integer, Crop> cropsById) {
        Season season = seasonsById.get(seasonId);
        if (season == null) return "Season #" + seasonId;
        Crop crop = cropsById.get(season.getCropId());
        String cropName = crop == null ? "Crop" : crop.getCropName();
        return cropName + " · " + season.getSeasonName();
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "Date unavailable" : ACTIVITY_DATE.format(date);
    }

    private static String formatNumber(Number number) {
        return String.format(Locale.US, "%,.1f", number.doubleValue());
    }

    private static String safeText(String value) {
        return value == null || value.isBlank() ? "Not specified" : value;
    }

    private record ActivityEntry(LocalDate date, RecentActivity activity) { }

    private static final class YieldAccumulator {
        private double expectedYield;
        private double actualYield;
        private double achievementTotal;
        private int completedHarvests;
        private int pendingHarvests;

        private void add(HarvestRecord record) {
            expectedYield += record.getExpectedYieldKg();
            if (record.isHarvestComplete()) {
                actualYield += record.getActualYieldKg();
                achievementTotal += record.getYieldAchievementPercent();
                completedHarvests++;
            } else {
                pendingHarvests++;
            }
        }

        private double getScaleValue() {
            return Math.max(expectedYield, actualYield);
        }
    }
}
