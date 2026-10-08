package com.greenfields.test;

import com.greenfields.filter.AuthFilter;
import com.greenfields.model.User;
import com.greenfields.model.CropHistoryViewModel;
import com.greenfields.model.ReportsViewModel;
import com.greenfields.servlet.CropHistoryServlet;
import com.greenfields.servlet.ReportsServlet;
import com.greenfields.util.DBConnection;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class TestReportsServlet {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        String url = "jdbc:h2:mem:greenfields_reports_phase10;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        DBConnection.setConfiguration(url, "sa", "");
        try {
            initialize(url);
            ReportsServlet reports = new ReportsServlet();
            Context all = new Context();
            reports.doGet(all.request, all.response);
            ReportsViewModel data = (ReportsViewModel) all.attributes.get("reports");
            check("/jsp/reports.jsp".equals(all.forwarded) && data.getCrops().size() == 2,
                    "Authenticated all-crop/all-season report loads filter options");
            check(data.getHarvestRecords().size() == 3 && data.getSeasonalSummaries().size() == 3,
                    "Report contains harvest and seasonal records from JDBC DAOs");
            check(data.getFertilizerUsage().size() == 3 && data.getFertilizerTotalKg() == 7.5,
                    "Fertilizer usage and numeric total use DAO data");
            check(data.getIrrigationUsage().size() == 2 && data.getIrrigationCount() == 2,
                    "Irrigation records and schedule count use DAO data");
            check(data.getExpectedYieldTotalKg() == 300.0 && data.getActualYieldTotalKg() == 90.0,
                    "Expected yield and recorded actual yield totals are calculated");

            Context crop = new Context(); crop.parameters.put("cropId", "1"); reports.doGet(crop.request, crop.response);
            check(((ReportsViewModel) crop.attributes.get("reports")).getHarvestRecords().size() == 2,
                    "Crop filter includes all seasons for the crop");
            Context season = new Context(); season.parameters.put("seasonId", "2"); reports.doGet(season.request, season.response);
            check(((ReportsViewModel) season.attributes.get("reports")).getHarvestRecords().size() == 1,
                    "Season filter includes its harvest records");
            Context combined = new Context(); combined.parameters.put("cropId", "1"); combined.parameters.put("seasonId", "2");
            reports.doGet(combined.request, combined.response);
            check(((ReportsViewModel) combined.attributes.get("reports")).getHarvestRecords().size() == 1,
                    "Combined crop and season filter returns matching records");
            Context mismatch = new Context(); mismatch.parameters.put("cropId", "2"); mismatch.parameters.put("seasonId", "1");
            reports.doGet(mismatch.request, mismatch.response);
            check(((ReportsViewModel) mismatch.attributes.get("reports")).getErrorMessage().contains("does not belong"),
                    "Mismatched crop/season IDs are rejected safely");
            Context invalid = new Context(); invalid.parameters.put("cropId", "999"); reports.doGet(invalid.request, invalid.response);
            check(((ReportsViewModel) invalid.attributes.get("reports")).getErrorMessage().contains("does not exist"),
                    "Nonexistent crop filter is rejected safely");
            Context invalidSeason = new Context(); invalidSeason.parameters.put("seasonId", "abc"); reports.doGet(invalidSeason.request, invalidSeason.response);
            check(((ReportsViewModel) invalidSeason.attributes.get("reports")).getErrorMessage().contains("valid season"),
                    "Malformed season filter is rejected safely");
            Context nonexistentSeason = new Context(); nonexistentSeason.parameters.put("seasonId", "999");
            reports.doGet(nonexistentSeason.request, nonexistentSeason.response);
            check(((ReportsViewModel) nonexistentSeason.attributes.get("reports")).getErrorMessage().contains("does not exist"),
                    "Nonexistent season filter is rejected safely");

            ReportsViewModel.HarvestYieldRow pending = data.getHarvestRecords().stream()
                    .filter(row -> row.getActualYieldKg() == null).findFirst().orElseThrow();
            ReportsViewModel.HarvestYieldRow zero = data.getHarvestRecords().stream()
                    .filter(row -> row.getExpectedYieldKg() == 0).findFirst().orElseThrow();
            ReportsViewModel.HarvestYieldRow complete = data.getHarvestRecords().stream()
                    .filter(row -> row.getRecordId() == 1).findFirst().orElseThrow();
            check("Pending".equals(pending.getYieldStatus()) && pending.getAchievementPercent() == null,
                    "NULL actual yield is pending and has no achievement percentage");
            check("N/A (zero expected yield)".equals(zero.getYieldStatus()) && zero.getAchievementPercent() == null,
                    "Zero expected yield is safe and does not divide by zero");
            check(complete.getAchievementPercent() == 90.0 && "Below Expected".equals(complete.getYieldStatus()),
                    "Achievement calculation and performance status are correct");

            CropHistoryServlet historyServlet = new CropHistoryServlet();
            Context historyContext = new Context(); historyContext.parameters.put("cropId", "1");
            historyServlet.doGet(historyContext.request, historyContext.response);
            CropHistoryViewModel history = (CropHistoryViewModel) historyContext.attributes.get("history");
            check("/jsp/crop-history.jsp".equals(historyContext.forwarded) && history.getSeasons().size() == 2,
                    "Crop history loads multiple seasons for one crop");
            check(history.getFertilizerApplications().size() == 2 && history.getIrrigationSchedules().size() == 1
                            && history.getHarvestRecords().size() == 2,
                    "Crop history combines fertilizer, irrigation, and harvest records");
            Context missingHistory = new Context(); missingHistory.parameters.put("cropId", "987");
            historyServlet.doGet(missingHistory.request, missingHistory.response);
            check(((CropHistoryViewModel) missingHistory.attributes.get("history")).getErrorMessage() != null,
                    "Nonexistent crop history displays a safe validation message");
            Context noHistoryId = new Context(); historyServlet.doGet(noHistoryId.request, noHistoryId.response);
            check(((CropHistoryViewModel) noHistoryId.attributes.get("history")).getCrop() == null,
                    "Crop history selection page loads without a crop filter");

            Context readOnlyReport = new Context(); reports.doPost(readOnlyReport.request, readOnlyReport.response);
            Context readOnlyHistory = new Context(); historyServlet.doPost(readOnlyHistory.request, readOnlyHistory.response);
            check(readOnlyReport.status == 405 && readOnlyHistory.status == 405,
                    "Report and history routes reject modification requests");
            check(!all.attributes.containsKey("currentUser") && !all.attributes.containsKey("password"),
                    "Report response does not expose session user or password data");
            checkAuthenticated("/greenfields/reports");
            checkAuthenticated("/greenfields/crop-history");
            checkUnauthenticated("/greenfields/reports");
            checkUnauthenticated("/greenfields/crop-history");

            try (Connection connection = DriverManager.getConnection(url, "sa", ""); Statement s = connection.createStatement()) {
                s.execute("DELETE FROM harvest_records"); s.execute("DELETE FROM fertilizer_applications");
                s.execute("DELETE FROM irrigation_schedules");
            }
            Context empty = new Context(); reports.doGet(empty.request, empty.response);
            ReportsViewModel emptyReport = (ReportsViewModel) empty.attributes.get("reports");
            check(emptyReport.getHarvestRecords().isEmpty() && emptyReport.getFertilizerUsage().isEmpty()
                            && emptyReport.getIrrigationUsage().isEmpty(),
                    "Empty report sources return usable empty collections");
            Context emptyHistory = new Context(); emptyHistory.parameters.put("cropId", "1");
            historyServlet.doGet(emptyHistory.request, emptyHistory.response);
            check(((CropHistoryViewModel) emptyHistory.attributes.get("history")).getHarvestRecords().isEmpty(),
                    "Crop history handles crops with no historical activity");
        } finally {
            DBConnection.resetConfiguration();
        }
        System.out.printf("Phase 10 reports checks: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static void initialize(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", ""); Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE crops (crop_id INT PRIMARY KEY, crop_name VARCHAR(100), crop_type VARCHAR(50), variety VARCHAR(100), description CLOB, growth_duration_days INT, status VARCHAR(20))");
            s.execute("CREATE TABLE seasons (season_id INT PRIMARY KEY, crop_id INT, season_name VARCHAR(100), field_location VARCHAR(100), area_acres DECIMAL(8,2), planting_date DATE, expected_harvest_date DATE, season_status VARCHAR(20), notes CLOB)");
            s.execute("CREATE TABLE fertilizer_applications (fertilizer_id INT PRIMARY KEY, season_id INT, fertilizer_name VARCHAR(100), fertilizer_type VARCHAR(30), quantity_kg DECIMAL(10,2), application_date DATE, applied_by VARCHAR(100), notes CLOB)");
            s.execute("CREATE TABLE irrigation_schedules (irrigation_id INT PRIMARY KEY, season_id INT, scheduled_date DATE, actual_date DATE, method VARCHAR(40), water_volume_litres DECIMAL(12,2), status VARCHAR(20), notes CLOB)");
            s.execute("CREATE TABLE harvest_records (harvest_id INT PRIMARY KEY, season_id INT, harvest_date DATE, expected_yield_kg DECIMAL(10,2), actual_yield_kg DECIMAL(10,2), quality_grade VARCHAR(10), remarks CLOB, recorded_by VARCHAR(100))");
            s.execute("INSERT INTO crops VALUES (1,'Paddy','Grain','IR-64','',120,'active'),(2,'Maize','Grain','Sweet Corn','',90,'active')");
            s.execute("INSERT INTO seasons VALUES (1,1,'Kharif 2025','Block A',5,'2025-06-15','2025-10-13','completed',''),(2,1,'Rabi 2026','Block A',3,'2026-11-01','2027-03-01','planned',''),(3,2,'Maize 2026','Block B',2,'2026-06-01','2026-09-01','active','')");
            s.execute("INSERT INTO fertilizer_applications VALUES (1,1,'Urea','chemical',2.5,'2025-07-01','Ravi',''),(2,2,'Compost','organic',4,'2026-11-02','Asha',''),(3,3,'NPK','chemical',1,'2026-06-03','Ravi','')");
            s.execute("INSERT INTO irrigation_schedules VALUES (1,1,'2025-07-03',NULL,'flood',100,'completed',''),(2,3,'2026-06-04',NULL,'drip',50,'scheduled','')");
            s.execute("INSERT INTO harvest_records VALUES (1,1,'2025-10-10',100,90,'A','','Ravi'),(2,2,'2027-02-28',200,NULL,NULL,'pending','Asha'),(3,3,'2026-08-30',0,0,NULL,'zero expected','Ravi')");
        }
    }

    private static void checkUnauthenticated(String uri) throws Exception {
        Map<String, Object> state = new HashMap<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                    case "getContextPath" -> "/greenfields"; case "getRequestURI" -> uri; case "getSession" -> null;
                    default -> defaultValue(m.getReturnType());
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (p, m, a) -> {
                    if ("sendRedirect".equals(m.getName())) state.put("redirect", a[0]);
                    return defaultValue(m.getReturnType());
                });
        FilterChain chain = (FilterChain) Proxy.newProxyInstance(FilterChain.class.getClassLoader(),
                new Class<?>[]{FilterChain.class}, (p, m, a) -> { state.put("called", true); return null; });
        new AuthFilter().doFilter(request, response, chain);
        check("/greenfields/login?error=auth_required".equals(state.get("redirect")) && !state.containsKey("called"),
                "AuthFilter rejects unauthenticated " + uri.substring(uri.lastIndexOf('/') + 1) + " access");
    }

    private static void checkAuthenticated(String uri) throws Exception {
        Map<String, Object> state = new HashMap<>();
        User user = new User(7, "viewer", "do-not-expose", "Farm Viewer", "viewer", null);
        HttpSession session = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class}, (p, m, a) -> "getAttribute".equals(m.getName())
                        && "user".equals(a[0]) ? user : defaultValue(m.getReturnType()));
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                    case "getContextPath" -> "/greenfields"; case "getRequestURI" -> uri; case "getSession" -> session;
                    default -> defaultValue(m.getReturnType());
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (p, m, a) -> defaultValue(m.getReturnType()));
        FilterChain chain = (FilterChain) Proxy.newProxyInstance(FilterChain.class.getClassLoader(),
                new Class<?>[]{FilterChain.class}, (p, m, a) -> { state.put("called", true); return null; });
        new AuthFilter().doFilter(request, response, chain);
        check(Boolean.TRUE.equals(state.get("called")),
                "AuthFilter allows authenticated " + uri.substring(uri.lastIndexOf('/') + 1) + " access");
    }

    private static void check(boolean condition, String message) {
        if (condition) { passed++; System.out.println("[PASS] " + message); }
        else { failed++; System.out.println("[FAIL] " + message); }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static final class Context {
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, String> parameters = new HashMap<>();
        private String forwarded;
        private int status;
        private final HttpServletRequest request;
        private final HttpServletResponse response;

        private Context() {
            HttpSession session = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class}, (p, m, a) -> defaultValue(m.getReturnType()));
            request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                        case "getParameter" -> parameters.get(a[0]);
                        case "getSession" -> session;
                        case "setAttribute" -> { attributes.put((String) a[0], a[1]); yield null; }
                        case "getAttribute" -> attributes.get(a[0]);
                        case "getRequestDispatcher" -> {
                            forwarded = (String) a[0];
                            yield Proxy.newProxyInstance(RequestDispatcher.class.getClassLoader(),
                                    new Class<?>[]{RequestDispatcher.class}, (q, n, b) -> null);
                        }
                        default -> defaultValue(m.getReturnType());
                    });
            response = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class}, (p, m, a) -> {
                        if ("sendError".equals(m.getName())) status = (Integer) a[0];
                        return defaultValue(m.getReturnType());
                    });
        }
    }
}
