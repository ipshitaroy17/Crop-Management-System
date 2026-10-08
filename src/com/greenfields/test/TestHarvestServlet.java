package com.greenfields.test;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.HarvestRecordDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.dao.impl.CropDAOImpl;
import com.greenfields.dao.impl.HarvestRecordDAOImpl;
import com.greenfields.dao.impl.SeasonDAOImpl;
import com.greenfields.exception.DatabaseException;
import com.greenfields.filter.AuthFilter;
import com.greenfields.model.Crop;
import com.greenfields.model.HarvestRecord;
import com.greenfields.model.HarvestRecordView;
import com.greenfields.model.Season;
import com.greenfields.model.User;
import com.greenfields.servlet.HarvestServlet;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestHarvestServlet {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Fixture fixture = new Fixture();
        HarvestServlet servlet = fixture.servlet();

        Context list = new Context("viewer"); servlet.doGet(list.request, list.response);
        check("/jsp/harvest.jsp".equals(list.forwarded) && ((List<?>) list.attributes.get("records")).size() == 3,
                "Authenticated user can list harvest records");
        Context cropFilter = new Context("viewer"); cropFilter.parameters.put("cropId", "1"); servlet.doGet(cropFilter.request, cropFilter.response);
        check(((List<?>) cropFilter.attributes.get("records")).size() == 2, "Crop filter returns records for one crop");
        Context seasonFilter = new Context("viewer"); seasonFilter.parameters.put("seasonId", "2"); servlet.doGet(seasonFilter.request, seasonFilter.response);
        check(((List<?>) seasonFilter.attributes.get("records")).size() == 1, "Season filter returns records for one season");
        Context combined = new Context("viewer"); combined.parameters.put("cropId", "2"); combined.parameters.put("seasonId", "2");
        servlet.doGet(combined.request, combined.response);
        check(((List<?>) combined.attributes.get("records")).isEmpty(), "Combined crop/season filter enforces the relationship");
        testUnauthenticatedFilter();

        Context viewerNew = new Context("viewer"); viewerNew.parameters.put("action", "new"); servlet.doGet(viewerNew.request, viewerNew.response);
        Context viewerEditForm = new Context("viewer"); viewerEditForm.parameters.put("action", "edit"); viewerEditForm.parameters.put("id", "1"); servlet.doGet(viewerEditForm.request, viewerEditForm.response);
        check(viewerNew.status == 403 && viewerEditForm.status == 403, "Non-admin cannot open add or edit forms");
        Context viewerCreate = validPost("viewer", "create"); servlet.doPost(viewerCreate.request, viewerCreate.response);
        Context viewerUpdate = validPost("viewer", "update"); viewerUpdate.parameters.put("id", "1"); servlet.doPost(viewerUpdate.request, viewerUpdate.response);
        Context viewerDelete = new Context("viewer"); viewerDelete.parameters.put("action", "delete"); viewerDelete.parameters.put("id", "1"); servlet.doPost(viewerDelete.request, viewerDelete.response);
        check(viewerCreate.status == 403 && viewerUpdate.status == 403 && viewerDelete.status == 403 && fixture.records.size() == 3,
                "Non-admin cannot add, edit, or delete by direct POST");

        validateRejected(servlet, fixture, context -> context.parameters.remove("cropId"), "Missing crop is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.put("cropId", "999"), "Invalid crop is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.remove("seasonId"), "Missing season is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.put("seasonId", "999"), "Invalid season is rejected");
        validateRejected(servlet, fixture, context -> { context.parameters.put("cropId", "1"); context.parameters.put("seasonId", "3"); },
                "Crop-season mismatch is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.remove("harvestDate"), "Missing harvest date is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.put("harvestDate", "not-a-date"), "Invalid harvest date is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.put("expectedYieldKg", "-1"), "Negative expected yield is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.put("expectedYieldKg", "abc"), "Non-numeric expected yield is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.put("actualYieldKg", "-0.01"), "Negative actual yield is rejected");
        validateRejected(servlet, fixture, context -> context.parameters.put("actualYieldKg", "abc"), "Non-numeric actual yield is rejected");

        Context blankActual = validPost("admin", "create"); blankActual.parameters.put("actualYieldKg", "");
        servlet.doPost(blankActual.request, blankActual.response);
        check(fixture.records.size() == 4 && fixture.records.get(3).getActualYieldKg() == null,
                "Blank actual yield is accepted and stored as pending NULL");
        Context zeroExpected = validPost("admin", "create"); zeroExpected.parameters.put("expectedYieldKg", "0"); zeroExpected.parameters.put("actualYieldKg", "0");
        servlet.doPost(zeroExpected.request, zeroExpected.response);
        HarvestRecord zeroRecord = fixture.records.get(4);
        HarvestRecordView zeroView = new HarvestRecordView(zeroRecord, "Paddy", "Kharif");
        check(fixture.records.size() == 5 && zeroView.getAchievementPercent() == null
                        && zeroView.getYieldStatus().startsWith("N/A"),
                "Expected yield zero is accepted and displayed without division by zero");

        Context create = validPost("admin", "create"); create.parameters.put("actualYieldKg", "95.00");
        servlet.doPost(create.request, create.response);
        check(fixture.records.size() == 6 && create.redirect.endsWith("/harvest?message=created"), "Admin can add a harvest record");
        Context edit = validPost("admin", "update"); edit.parameters.put("id", "1");
        edit.parameters.put("expectedYieldKg", "100"); edit.parameters.put("actualYieldKg", "110"); servlet.doPost(edit.request, edit.response);
        check(fixture.find(1).getActualYieldKg() == 110.0 && fixture.find(1).getId() == 1,
                "Admin edits actual yield without changing the existing record ID");
        Context getDelete = new Context("admin"); getDelete.parameters.put("action", "delete"); getDelete.parameters.put("id", "1"); servlet.doGet(getDelete.request, getDelete.response);
        check(getDelete.status == 405 && fixture.find(1) != null, "GET delete is rejected");
        Context delete = new Context("admin"); delete.parameters.put("action", "delete"); delete.parameters.put("id", "1"); servlet.doPost(delete.request, delete.response);
        check(fixture.find(1) == null && delete.redirect.endsWith("/harvest?message=deleted"), "Admin deletes by POST");

        testYieldCalculations();
        testH2Persistence();
        System.out.printf("Phase 9 harvest checks: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static void validateRejected(HarvestServlet servlet, Fixture fixture, java.util.function.Consumer<Context> change, String message) throws Exception {
        Context context = validPost("admin", "create"); change.accept(context);
        servlet.doPost(context.request, context.response);
        check(fixture.records.size() == 3 && context.forwarded.equals("/jsp/harvest-form.jsp")
                && context.attributes.containsKey("errors"), message);
    }

    private static Context validPost(String role, String action) {
        Context context = new Context(role);
        context.parameters.put("action", action); context.parameters.put("cropId", "1"); context.parameters.put("seasonId", "1");
        context.parameters.put("harvestDate", "2026-10-04"); context.parameters.put("expectedYieldKg", "100.00");
        context.parameters.put("actualYieldKg", ""); context.parameters.put("qualityGrade", "");
        context.parameters.put("recordedBy", "Field supervisor"); context.parameters.put("remarks", "First section");
        return context;
    }

    private static void testYieldCalculations() {
        HarvestRecord record = new HarvestRecord(1, 1, LocalDate.now(), 200, 190.0, "A", "", "");
        HarvestRecordView view = new HarvestRecordView(record, "Paddy", "Kharif");
        check(view.getAchievementPercent() == 95.0 && "Below Expected".equals(view.getYieldStatus()),
                "Yield achievement uses HarvestRecord's calculation and labels below expected");
        record.setActualYieldKg(200.0);
        check("On Target".equals(view.getYieldStatus()), "Yield equal to expected is On Target");
        record.setActualYieldKg(220.0);
        check("Above Expected".equals(view.getYieldStatus()), "Yield above expected is Above Expected");
        record.setActualYieldKg(null);
        check(view.getAchievementPercent() == null && "Pending".equals(view.getYieldStatus()),
                "NULL actual yield displays Pending and has no achievement percentage");
        record.setExpectedYieldKg(0); record.setActualYieldKg(0.0);
        check(view.getAchievementPercent() == null && view.getYieldStatus().startsWith("N/A"),
                "Zero expected yield never divides by zero or displays a misleading percentage");
    }

    private static void testUnauthenticatedFilter() throws Exception {
        Map<String, Object> state = new HashMap<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                    case "getContextPath" -> "/greenfields"; case "getRequestURI" -> "/greenfields/harvest";
                    case "getSession" -> null; default -> defaultValue(m.getReturnType());
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
                "AuthFilter rejects unauthenticated /harvest access");
    }

    private static void testH2Persistence() throws Exception {
        String url = "jdbc:h2:mem:greenfields_harvest_phase9;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        DBConnection.setConfiguration(url, "sa", "");
        try {
            try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
                s.execute("CREATE TABLE crops (crop_id INT AUTO_INCREMENT PRIMARY KEY, crop_name VARCHAR(100) NOT NULL, crop_type VARCHAR(50) NOT NULL, variety VARCHAR(100), description CLOB, growth_duration_days INT, status VARCHAR(10) NOT NULL)");
                s.execute("CREATE TABLE seasons (season_id INT AUTO_INCREMENT PRIMARY KEY, crop_id INT NOT NULL REFERENCES crops(crop_id), season_name VARCHAR(100) NOT NULL, field_location VARCHAR(150), area_acres DECIMAL(8,2), planting_date DATE, expected_harvest_date DATE, season_status VARCHAR(10) NOT NULL, notes CLOB)");
                s.execute("CREATE TABLE harvest_records (harvest_id INT AUTO_INCREMENT PRIMARY KEY, season_id INT NOT NULL REFERENCES seasons(season_id) ON DELETE CASCADE, harvest_date DATE NOT NULL, expected_yield_kg DECIMAL(10,2) NOT NULL, actual_yield_kg DECIMAL(10,2), quality_grade VARCHAR(10), remarks CLOB, recorded_by VARCHAR(100))");
                s.execute("INSERT INTO crops(crop_name,crop_type,variety,growth_duration_days,status) VALUES ('Paddy','Grain','IR-64',120,'active'),('Maize','Grain','Sweet Corn',90,'active')");
                s.execute("INSERT INTO seasons(crop_id,season_name,season_status) VALUES (1,'Kharif','active'),(2,'Rabi','planned')");
            }
            HarvestRecordDAO harvest = new HarvestRecordDAOImpl();
            CropDAO crops = new CropDAOImpl(); SeasonDAO seasons = new SeasonDAOImpl();
            HarvestServlet servlet = new HarvestServlet(harvest, crops, seasons);
            Context pending = validPost("admin", "create"); servlet.doPost(pending.request, pending.response);
            HarvestRecord savedPending = harvest.findAll().get(0);
            check(harvest.findAll().size() == 1 && savedPending.getActualYieldKg() == null,
                    "H2 JDBC persists pending NULL actual yield");
            Context stillPending = validPost("admin", "update"); stillPending.parameters.put("id", String.valueOf(savedPending.getId()));
            stillPending.parameters.put("actualYieldKg", ""); servlet.doPost(stillPending.request, stillPending.response);
            check(harvest.findById(savedPending.getId()).getActualYieldKg() == null,
                    "H2 JDBC update preserves NULL actual yield for a pending record");
            Context complete = validPost("admin", "update"); complete.parameters.put("id", String.valueOf(savedPending.getId()));
            complete.parameters.put("actualYieldKg", "92.50"); complete.parameters.put("qualityGrade", "A");
            servlet.doPost(complete.request, complete.response);
            check(harvest.findById(savedPending.getId()).getActualYieldKg() == 92.5
                            && "A".equals(harvest.findById(savedPending.getId()).getQualityGrade()),
                    "H2 JDBC update persists actual yield and quality grade");
            Context zero = validPost("admin", "create"); zero.parameters.put("expectedYieldKg", "0"); zero.parameters.put("actualYieldKg", "0");
            servlet.doPost(zero.request, zero.response);
            check(harvest.findAll().size() == 2 && harvest.findAll().stream().anyMatch(row -> row.getExpectedYieldKg() == 0),
                    "H2 JDBC persists zero expected and actual yields");

            SeasonDAO staleSeason = new SeasonDAO() {
                public List<Season> findAll() { return List.of(new Season(999, 1, "Removed season", "", 1, null, null, "active", "")); }
                public Season findById(int id) { return id == 999 ? findAll().get(0) : null; }
                public List<Season> findByCropId(int id) { return List.of(); }
                public int save(Season season) { return 0; } public void update(Season season) { }
            };
            HarvestServlet foreignKeyServlet = new HarvestServlet(harvest, crops, staleSeason);
            Context fkFailure = validPost("admin", "create"); fkFailure.parameters.put("seasonId", "999");
            foreignKeyServlet.doPost(fkFailure.request, fkFailure.response);
            check(fkFailure.attributes.get("errorMessage") != null
                            && !fkFailure.attributes.get("errorMessage").toString().contains("SQL")
                            && harvest.findAll().size() == 2,
                    "H2 foreign-key failure is caught and shown as a safe user message");

            int savedId = savedPending.getId();
            Context delete = new Context("admin"); delete.parameters.put("action", "delete"); delete.parameters.put("id", String.valueOf(savedId));
            servlet.doPost(delete.request, delete.response);
            check(harvest.findById(savedId) == null, "H2 JDBC delete removes persisted harvest record");
        } finally { DBConnection.resetConfiguration(); }
    }

    private static void check(boolean result, String description) {
        if (result) { passed++; System.out.println("[PASS] " + description); }
        else { failed++; System.out.println("[FAIL] " + description); }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false; if (type == int.class) return 0; if (type == long.class) return 0L;
        return null;
    }

    private static final class Fixture {
        private final List<Crop> crops = new ArrayList<>(List.of(
                new Crop(1, "Paddy", "Grain", "IR-64", "", 120, "active"),
                new Crop(2, "Maize", "Grain", "Sweet Corn", "", 90, "active")));
        private final List<Season> seasons = new ArrayList<>(List.of(
                new Season(1, 1, "Kharif", "Block A", 5, null, null, "active", ""),
                new Season(2, 1, "Rabi", "Block A", 5, null, null, "completed", ""),
                new Season(3, 2, "Winter", "Block B", 3, null, null, "active", "")));
        private final List<HarvestRecord> records = new ArrayList<>(List.of(
                new HarvestRecord(1, 1, LocalDate.of(2026, 7, 2), 100, 95.0, "A", "", "Asha"),
                new HarvestRecord(2, 2, LocalDate.of(2026, 8, 2), 200, null, null, "Pending", ""),
                new HarvestRecord(3, 3, LocalDate.of(2026, 9, 2), 50, 60.0, "B", "", "Ravi")));
        private CropDAO cropDAO() {
            return new CropDAO() {
                public List<Crop> findAll() { return crops; }
                public Crop findById(int id) { return crops.stream().filter(c -> c.getId() == id).findFirst().orElse(null); }
                public int save(Crop crop) { return 0; } public void update(Crop crop) { } public void delete(int id) { }
            };
        }
        private SeasonDAO seasonDAO() {
            return new SeasonDAO() {
                public List<Season> findAll() { return seasons; }
                public Season findById(int id) { return seasons.stream().filter(s -> s.getId() == id).findFirst().orElse(null); }
                public List<Season> findByCropId(int id) { return seasons.stream().filter(s -> s.getCropId() == id).toList(); }
                public int save(Season season) { return 0; } public void update(Season season) { }
            };
        }
        private HarvestRecordDAO harvestDAO(boolean failSave, boolean failDelete) {
            return new HarvestRecordDAO() {
                public List<HarvestRecord> findAll() { return new ArrayList<>(records); }
                public List<HarvestRecord> findBySeasonId(int id) { return records.stream().filter(r -> r.getSeasonId() == id).toList(); }
                public List<HarvestRecord> findByCropId(int id) { return records.stream().filter(r -> seasons.stream().anyMatch(s -> s.getId() == r.getSeasonId() && s.getCropId() == id)).toList(); }
                public HarvestRecord findById(int id) { return records.stream().filter(r -> r.getId() == id).findFirst().orElse(null); }
                public int save(HarvestRecord record) { if (failSave) throw new DatabaseException("sensitive SQL exception"); record.setId(records.stream().mapToInt(HarvestRecord::getId).max().orElse(0) + 1); records.add(record); return record.getId(); }
                public void update(HarvestRecord record) { records.removeIf(old -> old.getId() == record.getId()); records.add(record); }
                public void delete(int id) { if (failDelete) throw new DatabaseException("sensitive SQL exception"); records.removeIf(record -> record.getId() == id); }
            };
        }
        private HarvestServlet servlet() { return new HarvestServlet(harvestDAO(false, false), cropDAO(), seasonDAO()); }
        private HarvestRecord find(int id) { return records.stream().filter(record -> record.getId() == id).findFirst().orElse(null); }
    }

    private static final class Context {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private String forwarded, redirect;
        private int status;
        private Context(String role) {
            HttpSession session = null;
            if (role != null) {
                User user = new User(); user.setRole(role);
                session = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(), new Class<?>[]{HttpSession.class},
                        (p, m, a) -> "getAttribute".equals(m.getName()) && "user".equals(a[0]) ? user : defaultValue(m.getReturnType()));
            }
            HttpSession finalSession = session;
            request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "getParameter" -> parameters.get(a[0]); case "setAttribute" -> { attributes.put((String) a[0], a[1]); yield null; }
                        case "getAttribute" -> attributes.get(a[0]); case "getContextPath" -> "/greenfields";
                        case "getSession" -> finalSession;
                        case "getRequestDispatcher" -> { forwarded = (String) a[0]; yield Proxy.newProxyInstance(RequestDispatcher.class.getClassLoader(), new Class<?>[]{RequestDispatcher.class}, (x, y, z) -> null); }
                        default -> defaultValue(m.getReturnType());
                    });
            response = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                    (p, m, a) -> { if ("sendRedirect".equals(m.getName())) redirect = (String) a[0];
                        if ("sendError".equals(m.getName())) status = (int) a[0]; return defaultValue(m.getReturnType()); });
        }
    }
}
