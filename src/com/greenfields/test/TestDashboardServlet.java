package com.greenfields.test;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.FertilizerApplicationDAO;
import com.greenfields.dao.HarvestRecordDAO;
import com.greenfields.dao.IrrigationScheduleDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.model.Crop;
import com.greenfields.model.DashboardViewModel;
import com.greenfields.model.FertilizerApplication;
import com.greenfields.model.HarvestRecord;
import com.greenfields.model.IrrigationSchedule;
import com.greenfields.model.Season;
import com.greenfields.model.User;
import com.greenfields.servlet.DashboardServlet;
import com.greenfields.util.DBConnection;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Proxy;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TestDashboardServlet {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        SampleData sample = new SampleData();
        DashboardServlet servlet = sample.servlet();
        MockContext ctx = new MockContext("Farm Administrator", "admin");
        servlet.doGet(ctx.request, ctx.response);

        DashboardViewModel dashboard = (DashboardViewModel) ctx.attributes.get("dashboard");
        check(ctx.forwardedPath.equals("/jsp/dashboard.jsp") && "Farm Administrator".equals(ctx.attributes.get("currentUserName")),
                "Authenticated dashboard request forwards with the signed-in user's name");
        check(dashboard.getTotalCrops() == 3, "Total Crops is calculated from crop DAO records");
        check(dashboard.getActiveSeasons() == 2, "Active Seasons counts only active season records");
        check(close(dashboard.getFertilizerUsedKg(), 6.0), "Total Fertilizer Used sums DAO quantities");
        check(close(dashboard.getExpectedYieldKg(), 350.0), "Expected Yield includes completed and pending harvest records");

        DashboardViewModel.CropPerformance paddy = findPerformance(dashboard, "Paddy");
        DashboardViewModel.CropPerformance maize = findPerformance(dashboard, "Maize");
        DashboardViewModel.CropPerformance tomato = findPerformance(dashboard, "Tomato");
        check(paddy.hasActualYield() && close(paddy.getActualYieldKg(), 90.0),
                "Expected versus actual yield shows recorded Paddy yield");
        check(maize.getActualYieldKg() == null && maize.getPendingHarvests() == 1,
                "NULL actual yield remains pending and is not treated as zero");
        check(close(paddy.getAchievementPercent(), 90.0) && close(tomato.getAchievementPercent(), 150.0),
                "Crop performance uses HarvestRecord achievement calculations");
        check(dashboard.getRecentActivities().size() == 6
                        && "Harvest".equals(dashboard.getRecentActivities().get(0).getCategory()),
                "Recent activities combine real events and sort newest first");
        check(Boolean.TRUE.equals(ctx.attributes.get("currentUserIsAdmin")),
                "Admin quick-action visibility is provided without exposing the User object");
        check(!ctx.attributes.containsKey("currentUser"),
                "Dashboard request attributes do not expose the password-bearing User model");

        SampleData empty = new SampleData();
        empty.crops.clear();
        empty.seasons.clear();
        empty.fertilizers.clear();
        empty.irrigation.clear();
        empty.harvests.clear();
        MockContext emptyContext = new MockContext("Farm Viewer", "viewer");
        empty.servlet().doGet(emptyContext.request, emptyContext.response);
        DashboardViewModel emptyDashboard = (DashboardViewModel) emptyContext.attributes.get("dashboard");
        check(emptyDashboard.getTotalCrops() == 0 && emptyDashboard.getTotalSeasons() == 0
                        && emptyDashboard.getActiveSeasons() == 0
                        && emptyDashboard.getFertilizerRecordCount() == 0
                        && emptyDashboard.getHarvestRecordCount() == 0
                        && emptyDashboard.getRecentActivities().isEmpty(),
                "Empty database collections produce empty dashboard states");

        CropDAO failingCropDAO = new CropDAO() {
            public List<Crop> findAll() { throw new IllegalStateException("private database detail"); }
            public Crop findById(int id) { return null; }
            public int save(Crop crop) { return 0; }
            public void update(Crop crop) { }
            public void delete(int id) { }
        };
        MockContext failedContext = new MockContext("Farm Viewer", "viewer");
        new DashboardServlet(failingCropDAO, empty.seasonDAO(), empty.fertilizerDAO(),
                empty.irrigationDAO(), empty.harvestDAO()).doGet(failedContext.request, failedContext.response);
        check(failedContext.attributes.get("dashboard") == null
                        && failedContext.attributes.get("dashboardError") != null
                        && !failedContext.attributes.get("dashboardError").toString().contains("private database detail"),
                "Database failure shows a safe user-facing state");

        testJdbcDashboardIntegration();

        System.out.printf("Phase 6 dashboard checks: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static DashboardViewModel.CropPerformance findPerformance(DashboardViewModel dashboard, String name) {
        return dashboard.getCropPerformance().stream()
                .filter(item -> name.equals(item.getCropName())).findFirst().orElseThrow();
    }

    private static boolean close(Double actual, double expected) {
        return actual != null && close(actual.doubleValue(), expected);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.0001;
    }

    private static void testJdbcDashboardIntegration() throws Exception {
        String url = "jdbc:h2:mem:greenfields_dashboard_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
        DBConnection.setConfiguration(url, "sa", "");
        try {
            initializeDatabase(url);
            MockContext ctx = new MockContext("Farm Administrator", "admin");
            new DashboardServlet().doGet(ctx.request, ctx.response);
            DashboardViewModel data = (DashboardViewModel) ctx.attributes.get("dashboard");
            check(data.getTotalCrops() == 3 && data.getActiveSeasons() == 1,
                    "JDBC dashboard reads crop and active-season totals from the SQL seed data");
            check(close(data.getFertilizerUsedKg(), 392.0)
                            && close(data.getExpectedYieldKg(), 11_000.0),
                    "JDBC dashboard sums fertilizer and expected yield from SQL records");
            check(close(findPerformance(data, "Paddy").getActualYieldKg(), 4_750.0)
                            && findPerformance(data, "Maize").getActualYieldKg() == null
                            && findPerformance(data, "Maize").getPendingHarvests() == 1,
                    "JDBC dashboard preserves recorded yield and nullable pending yield");
            check("28 Jan 2026".equals(data.getRecentActivities().get(0).getDateLabel()),
                    "JDBC recent activity includes the latest seeded harvest record");
        } finally {
            DBConnection.resetConfiguration();
        }
    }

    private static void initializeDatabase(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             BufferedReader reader = Files.newBufferedReader(Path.of("sql/greenfields_db.sql"), StandardCharsets.UTF_8)) {
            StringBuilder sql = new StringBuilder();
            String line;
            boolean skippingDatabaseStatement = false;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                String upper = trimmed.toUpperCase(Locale.ROOT);
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                if (skippingDatabaseStatement) {
                    if (trimmed.endsWith(";")) skippingDatabaseStatement = false;
                    continue;
                }
                if (upper.startsWith("DROP DATABASE") || upper.startsWith("USE ")) continue;
                if (upper.startsWith("CREATE DATABASE")) {
                    skippingDatabaseStatement = !trimmed.endsWith(";");
                    continue;
                }
                sql.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    String command = sql.toString().trim();
                    statement.execute(command.substring(0, command.length() - 1));
                    sql.setLength(0);
                }
            }
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + description);
        } else {
            failed++;
            System.out.println("[FAIL] " + description);
        }
    }

    private static final class SampleData {
        private final List<Crop> crops = new ArrayList<>(List.of(
                new Crop(1, "Paddy", "Grain", "IR-64", "", 120, "active"),
                new Crop(2, "Maize", "Grain", "Sweet Corn", "", 90, "active"),
                new Crop(3, "Tomato", "Vegetable", "Roma VF", "", 75, "active")));
        private final List<Season> seasons = new ArrayList<>(List.of(
                new Season(1, 1, "Kharif", "Block A", 5, null, null, "active", ""),
                new Season(2, 2, "Rabi", "Block B", 3, null, null, "planned", ""),
                new Season(3, 3, "Summer", "Greenhouse", 1, null, null, "active", "")));
        private final List<FertilizerApplication> fertilizers = new ArrayList<>(List.of(
                new FertilizerApplication(1, 1, "Urea", "chemical", 2.5, LocalDate.of(2026, 3, 2), "Ravi", ""),
                new FertilizerApplication(2, 2, "Compost", "organic", 3.5, LocalDate.of(2026, 4, 2), "Priya", "")));
        private final List<IrrigationSchedule> irrigation = new ArrayList<>(List.of(
                new IrrigationSchedule(1, 1, LocalDate.of(2026, 5, 2), null, "drip", 100, "scheduled", ""),
                new IrrigationSchedule(2, 2, LocalDate.of(2026, 6, 2), null, "flood", 100, "completed", "")));
        private final List<HarvestRecord> harvests = new ArrayList<>(List.of(
                new HarvestRecord(1, 1, LocalDate.of(2026, 8, 2), 100, 90.0, "A", "", "Ravi"),
                new HarvestRecord(2, 2, LocalDate.of(2026, 7, 2), 200, null, null, "", "Ravi"),
                new HarvestRecord(3, 3, LocalDate.of(2026, 9, 2), 50, 75.0, "A", "", "Priya")));

        private DashboardServlet servlet() {
            return new DashboardServlet(cropDAO(), seasonDAO(), fertilizerDAO(), irrigationDAO(), harvestDAO());
        }

        private CropDAO cropDAO() {
            return new CropDAO() {
                public List<Crop> findAll() { return crops; }
                public Crop findById(int id) { return null; }
                public int save(Crop crop) { return 0; }
                public void update(Crop crop) { }
                public void delete(int id) { }
            };
        }

        private SeasonDAO seasonDAO() {
            return new SeasonDAO() {
                public List<Season> findAll() { return seasons; }
                public Season findById(int id) { return null; }
                public List<Season> findByCropId(int id) { return List.of(); }
                public int save(Season season) { return 0; }
                public void update(Season season) { }
            };
        }

        private FertilizerApplicationDAO fertilizerDAO() {
            return new FertilizerApplicationDAO() {
                public List<FertilizerApplication> findAll() { return fertilizers; }
                public List<FertilizerApplication> findBySeasonId(int id) { return List.of(); }
                public List<FertilizerApplication> findByCropId(int id) { return List.of(); }
                public int save(FertilizerApplication record) { return 0; }
                public void update(FertilizerApplication record) { }
                public void delete(int id) { }
            };
        }

        private IrrigationScheduleDAO irrigationDAO() {
            return new IrrigationScheduleDAO() {
                public List<IrrigationSchedule> findAll() { return irrigation; }
                public List<IrrigationSchedule> findBySeasonId(int id) { return List.of(); }
                public List<IrrigationSchedule> findByCropId(int id) { return List.of(); }
                public int save(IrrigationSchedule record) { return 0; }
                public void update(IrrigationSchedule record) { }
                public void delete(int id) { }
            };
        }

        private HarvestRecordDAO harvestDAO() {
            return new HarvestRecordDAO() {
                public List<HarvestRecord> findAll() { return harvests; }
                public List<HarvestRecord> findBySeasonId(int id) { return List.of(); }
                public List<HarvestRecord> findByCropId(int id) { return List.of(); }
                public HarvestRecord findById(int id) { return null; }
                public int save(HarvestRecord record) { return 0; }
                public void update(HarvestRecord record) { }
            };
        }
    }

    private static final class MockContext {
        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private String forwardedPath;

        private MockContext(String fullName, String role) {
            User user = new User(1, "farm-user", "secret-must-not-be-rendered", fullName, role, null);
            HttpSession session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(), new Class<?>[]{HttpSession.class},
                    (proxy, method, args) -> "getAttribute".equals(method.getName())
                            && "user".equals(args[0]) ? user : defaultValue(method.getReturnType()));
            request = (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getSession" -> session;
                        case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
                        case "getAttribute" -> attributes.get(args[0]);
                        case "getRequestDispatcher" -> {
                            forwardedPath = (String) args[0];
                            yield Proxy.newProxyInstance(RequestDispatcher.class.getClassLoader(),
                                    new Class<?>[]{RequestDispatcher.class}, (p, m, a) -> null);
                        }
                        default -> defaultValue(method.getReturnType());
                    });
            response = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> defaultValue(method.getReturnType()));
        }

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            return null;
        }
    }
}
