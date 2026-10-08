package com.greenfields.test;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.IrrigationScheduleDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.dao.impl.CropDAOImpl;
import com.greenfields.dao.impl.IrrigationScheduleDAOImpl;
import com.greenfields.dao.impl.SeasonDAOImpl;
import com.greenfields.exception.DatabaseException;
import com.greenfields.filter.AuthFilter;
import com.greenfields.model.Crop;
import com.greenfields.model.IrrigationSchedule;
import com.greenfields.model.Season;
import com.greenfields.model.User;
import com.greenfields.servlet.IrrigationServlet;
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

public class TestIrrigationServlet {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Fixture fixture = new Fixture();
        IrrigationServlet servlet = fixture.servlet();

        Context list = new Context("viewer"); servlet.doGet(list.request, list.response);
        check("/jsp/irrigation.jsp".equals(list.forwarded)
                        && ((List<?>) list.attributes.get("schedules")).size() == 2,
                "Authenticated viewer can list schedules with crop and season names");
        Context cropFilter = new Context("viewer"); cropFilter.parameters.put("cropId", "1");
        servlet.doGet(cropFilter.request, cropFilter.response);
        check(((List<?>) cropFilter.attributes.get("schedules")).size() == 1,
                "Crop filter returns only that crop's irrigation schedules");
        Context seasonFilter = new Context("viewer"); seasonFilter.parameters.put("seasonId", "2");
        servlet.doGet(seasonFilter.request, seasonFilter.response);
        check(((List<?>) seasonFilter.attributes.get("schedules")).size() == 1,
                "Season filter returns only that season's irrigation schedules");
        Context mismatchFilter = new Context("viewer"); mismatchFilter.parameters.put("cropId", "1"); mismatchFilter.parameters.put("seasonId", "2");
        servlet.doGet(mismatchFilter.request, mismatchFilter.response);
        check(((List<?>) mismatchFilter.attributes.get("schedules")).isEmpty(),
                "Combined crop and season filters enforce their relationship");

        testUnauthenticatedFilter();

        Context viewerNew = new Context("viewer"); viewerNew.parameters.put("action", "new"); servlet.doGet(viewerNew.request, viewerNew.response);
        Context viewerEditForm = new Context("viewer"); viewerEditForm.parameters.put("action", "edit"); viewerEditForm.parameters.put("id", "1");
        servlet.doGet(viewerEditForm.request, viewerEditForm.response);
        check(viewerNew.status == 403 && viewerEditForm.status == 403, "Viewer cannot open admin add/edit forms");
        Context viewerCreate = validPost("viewer", "create"); servlet.doPost(viewerCreate.request, viewerCreate.response);
        Context viewerUpdate = validPost("viewer", "update"); viewerUpdate.parameters.put("id", "1"); servlet.doPost(viewerUpdate.request, viewerUpdate.response);
        Context viewerDelete = new Context("viewer"); viewerDelete.parameters.put("action", "delete"); viewerDelete.parameters.put("id", "1"); servlet.doPost(viewerDelete.request, viewerDelete.response);
        check(viewerCreate.status == 403 && viewerUpdate.status == 403 && viewerDelete.status == 403
                        && fixture.schedules.size() == 2,
                "Non-admin cannot add, edit, or delete by direct POST");

        Context missingDate = validPost("admin", "create"); missingDate.parameters.remove("scheduledDate");
        servlet.doPost(missingDate.request, missingDate.response);
        check(noWrite(fixture, missingDate), "Missing scheduled date is rejected server-side");
        Context invalidDate = validPost("admin", "create"); invalidDate.parameters.put("scheduledDate", "tomorrow");
        servlet.doPost(invalidDate.request, invalidDate.response);
        check(noWrite(fixture, invalidDate), "Malformed scheduled date is rejected server-side");
        Context invalidActualDate = validPost("admin", "create"); invalidActualDate.parameters.put("actualDate", "not-a-date");
        servlet.doPost(invalidActualDate.request, invalidActualDate.response);
        check(noWrite(fixture, invalidActualDate), "Malformed optional actual date is rejected");
        Context invalidMethod = validPost("admin", "create"); invalidMethod.parameters.put("method", "hose");
        servlet.doPost(invalidMethod.request, invalidMethod.response);
        check(noWrite(fixture, invalidMethod), "Unsupported irrigation method is rejected");
        Context invalidStatus = validPost("admin", "create"); invalidStatus.parameters.put("status", "pending");
        servlet.doPost(invalidStatus.request, invalidStatus.response);
        check(noWrite(fixture, invalidStatus), "Unsupported irrigation status is rejected");
        Context invalidVolume = validPost("admin", "create"); invalidVolume.parameters.put("waterVolumeLitres", "-3");
        servlet.doPost(invalidVolume.request, invalidVolume.response);
        check(noWrite(fixture, invalidVolume), "Non-positive water volume is rejected");
        Context invalidCrop = validPost("admin", "create"); invalidCrop.parameters.put("cropId", "999");
        servlet.doPost(invalidCrop.request, invalidCrop.response);
        check(noWrite(fixture, invalidCrop), "Unknown crop is rejected before persistence");
        Context invalidSeason = validPost("admin", "create"); invalidSeason.parameters.put("seasonId", "999");
        servlet.doPost(invalidSeason.request, invalidSeason.response);
        check(noWrite(fixture, invalidSeason), "Unknown season is rejected before persistence");
        Context invalidRelation = validPost("admin", "create"); invalidRelation.parameters.put("cropId", "1"); invalidRelation.parameters.put("seasonId", "2");
        servlet.doPost(invalidRelation.request, invalidRelation.response);
        check(noWrite(fixture, invalidRelation), "Season from another crop is rejected");

        Context create = validPost("admin", "create"); servlet.doPost(create.request, create.response);
        check(fixture.schedules.size() == 3 && create.redirect.endsWith("/irrigation?message=created"), "Admin adds a valid schedule");
        Context edit = validPost("admin", "update"); edit.parameters.put("id", "1");
        edit.parameters.put("method", "sprinkler"); edit.parameters.put("waterVolumeLitres", "82.50");
        servlet.doPost(edit.request, edit.response);
        check("sprinkler".equals(fixture.find(1).getMethod()) && edit.redirect.endsWith("/irrigation?message=updated"),
                "Admin edits an existing schedule while preserving its record ID");
        Context getDelete = new Context("admin"); getDelete.parameters.put("action", "delete"); getDelete.parameters.put("id", "1");
        servlet.doGet(getDelete.request, getDelete.response);
        check(getDelete.status == 405 && fixture.find(1) != null, "GET cannot delete an irrigation schedule");
        Context delete = new Context("admin"); delete.parameters.put("action", "delete"); delete.parameters.put("id", "1");
        servlet.doPost(delete.request, delete.response);
        check(fixture.find(1) == null && delete.redirect.endsWith("/irrigation?message=deleted"), "Admin deletes a schedule through POST");

        testDatabaseFailureIsSafe();
        testH2Persistence();
        System.out.printf("Phase 8 irrigation checks: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static boolean noWrite(Fixture fixture, Context ctx) {
        return fixture.schedules.size() == 2 && ctx.forwarded.equals("/jsp/irrigation-form.jsp")
                && ctx.attributes.containsKey("errors");
    }

    private static Context validPost(String role, String action) {
        Context context = new Context(role);
        context.parameters.put("action", action); context.parameters.put("cropId", "1"); context.parameters.put("seasonId", "1");
        context.parameters.put("scheduledDate", "2026-10-10"); context.parameters.put("method", "drip");
        context.parameters.put("waterVolumeLitres", "60.25"); context.parameters.put("status", "scheduled");
        context.parameters.put("notes", "Morning field cycle");
        return context;
    }

    private static void testUnauthenticatedFilter() throws Exception {
        Map<String, Object> state = new HashMap<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                    case "getContextPath" -> "/greenfields"; case "getRequestURI" -> "/greenfields/irrigation";
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
                "AuthFilter rejects an unauthenticated /irrigation request");
    }

    private static void testDatabaseFailureIsSafe() throws Exception {
        Fixture fixture = new Fixture();
        IrrigationScheduleDAO failingDAO = fixture.irrigationDAO(true);
        IrrigationServlet servlet = new IrrigationServlet(failingDAO, fixture.cropDAO(), fixture.seasonDAO());
        Context create = validPost("admin", "create"); servlet.doPost(create.request, create.response);
        Object message = create.attributes.get("errorMessage");
        check(message != null && !message.toString().contains("sensitive SQL detail")
                        && "/jsp/irrigation-form.jsp".equals(create.forwarded),
                "Database/foreign-key failure returns a safe form message without SQL details");

        IrrigationScheduleDAO failingDelete = fixture.irrigationDAO(false, true);
        IrrigationServlet deleteServlet = new IrrigationServlet(failingDelete, fixture.cropDAO(), fixture.seasonDAO());
        Context delete = new Context("admin"); delete.parameters.put("action", "delete"); delete.parameters.put("id", "1");
        deleteServlet.doPost(delete.request, delete.response);
        check("/greenfields/irrigation?message=delete_failed".equals(delete.redirect),
                "Database deletion failure redirects with a generic safe status");
    }

    private static void testH2Persistence() throws Exception {
        String url = "jdbc:h2:mem:greenfields_irrigation_phase8;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        DBConnection.setConfiguration(url, "sa", "");
        try {
            try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
                s.execute("CREATE TABLE crops (crop_id INT AUTO_INCREMENT PRIMARY KEY, crop_name VARCHAR(100) NOT NULL, crop_type VARCHAR(50) NOT NULL, variety VARCHAR(100), description CLOB, growth_duration_days INT, status VARCHAR(10) NOT NULL)");
                s.execute("CREATE TABLE seasons (season_id INT AUTO_INCREMENT PRIMARY KEY, crop_id INT NOT NULL REFERENCES crops(crop_id), season_name VARCHAR(100) NOT NULL, field_location VARCHAR(150), area_acres DECIMAL(8,2), planting_date DATE, expected_harvest_date DATE, season_status VARCHAR(10) NOT NULL, notes CLOB)");
                s.execute("CREATE TABLE irrigation_schedules (irrigation_id INT AUTO_INCREMENT PRIMARY KEY, season_id INT NOT NULL REFERENCES seasons(season_id) ON DELETE CASCADE, scheduled_date DATE NOT NULL, actual_date DATE, method VARCHAR(10) NOT NULL, water_volume_litres DECIMAL(10,2), status VARCHAR(10) NOT NULL, notes CLOB)");
                s.execute("INSERT INTO crops(crop_name,crop_type,variety,growth_duration_days,status) VALUES ('Paddy','Grain','IR-64',120,'active'),('Maize','Grain','Sweet Corn',90,'active')");
                s.execute("INSERT INTO seasons(crop_id,season_name,season_status) VALUES (1,'Kharif','active'),(2,'Rabi','planned')");
            }
            IrrigationScheduleDAO irrigation = new IrrigationScheduleDAOImpl();
            IrrigationServlet servlet = new IrrigationServlet(irrigation, new CropDAOImpl(), new SeasonDAOImpl());
            Context create = validPost("admin", "create"); servlet.doPost(create.request, create.response);
            check(irrigation.findAll().size() == 1 && irrigation.findAll().get(0).getSeasonId() == 1,
                    "H2 JDBC integration persists schedule with season foreign key");
            IrrigationSchedule saved = irrigation.findAll().get(0);
            check(saved.getScheduledDate().equals(LocalDate.of(2026, 10, 10)) && saved.getActualDate() == null
                            && Math.abs(saved.getWaterVolumeLitres() - 60.25) < 0.001,
                    "H2 JDBC integration preserves dates, nullable actual date, and water amount");
            Context edit = validPost("admin", "update"); edit.parameters.put("id", String.valueOf(saved.getId()));
            edit.parameters.put("actualDate", "2026-10-11"); edit.parameters.put("status", "completed");
            servlet.doPost(edit.request, edit.response);
            IrrigationSchedule updated = irrigation.findAll().get(0);
            check(updated.getId() == saved.getId() && "completed".equals(updated.getScheduleStatus())
                            && LocalDate.of(2026, 10, 11).equals(updated.getActualDate()),
                    "H2 JDBC integration updates existing row and actual date");
            Context badFk = validPost("admin", "create"); badFk.parameters.put("seasonId", "999");
            servlet.doPost(badFk.request, badFk.response);
            check(irrigation.findAll().size() == 1 && badFk.attributes.containsKey("errors"),
                    "H2 integration rejects invalid season before foreign-key insert");
            Context delete = new Context("admin"); delete.parameters.put("action", "delete"); delete.parameters.put("id", String.valueOf(saved.getId()));
            servlet.doPost(delete.request, delete.response);
            check(irrigation.findAll().isEmpty(), "H2 JDBC integration deletes persisted schedule");
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
                new Season(2, 2, "Rabi", "Block B", 3, null, null, "planned", "")));
        private final List<IrrigationSchedule> schedules = new ArrayList<>(List.of(
                new IrrigationSchedule(1, 1, LocalDate.of(2026, 8, 2), null, "drip", 40, "scheduled", "North field"),
                new IrrigationSchedule(2, 2, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3), "flood", 75, "completed", "")));
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
        private IrrigationScheduleDAO irrigationDAO(boolean failSave) { return irrigationDAO(failSave, false); }
        private IrrigationScheduleDAO irrigationDAO(boolean failSave, boolean failDelete) {
            return new IrrigationScheduleDAO() {
                public List<IrrigationSchedule> findAll() { return new ArrayList<>(schedules); }
                public List<IrrigationSchedule> findBySeasonId(int id) { return schedules.stream().filter(r -> r.getSeasonId() == id).toList(); }
                public List<IrrigationSchedule> findByCropId(int id) { return schedules.stream().filter(r -> seasons.stream().anyMatch(s -> s.getId() == r.getSeasonId() && s.getCropId() == id)).toList(); }
                public int save(IrrigationSchedule schedule) {
                    if (failSave) throw new DatabaseException("sensitive SQL detail");
                    schedule.setId(schedules.stream().mapToInt(IrrigationSchedule::getId).max().orElse(0) + 1);
                    schedules.add(schedule); return schedule.getId();
                }
                public void update(IrrigationSchedule schedule) { schedules.removeIf(r -> r.getId() == schedule.getId()); schedules.add(schedule); }
                public void delete(int id) {
                    if (failDelete) throw new DatabaseException("sensitive SQL detail");
                    schedules.removeIf(r -> r.getId() == id);
                }
            };
        }
        private IrrigationServlet servlet() { return new IrrigationServlet(irrigationDAO(false), cropDAO(), seasonDAO()); }
        private IrrigationSchedule find(int id) { return schedules.stream().filter(r -> r.getId() == id).findFirst().orElse(null); }
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
                        (p, m, a) -> "getAttribute".equals(m.getName()) && "user".equals(a[0] ) ? user : defaultValue(m.getReturnType()));
            }
            HttpSession finalSession = session;
            request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "getParameter" -> parameters.get(a[0]);
                        case "setAttribute" -> { attributes.put((String) a[0], a[1]); yield null; }
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
