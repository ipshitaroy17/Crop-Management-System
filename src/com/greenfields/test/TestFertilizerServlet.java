package com.greenfields.test;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.FertilizerApplicationDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.dao.impl.CropDAOImpl;
import com.greenfields.dao.impl.FertilizerApplicationDAOImpl;
import com.greenfields.dao.impl.SeasonDAOImpl;
import com.greenfields.filter.AuthFilter;
import com.greenfields.model.Crop;
import com.greenfields.model.FertilizerApplication;
import com.greenfields.model.Season;
import com.greenfields.model.User;
import com.greenfields.servlet.FertilizerServlet;
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

public class TestFertilizerServlet {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Fixture fixture = new Fixture();
        FertilizerServlet servlet = fixture.servlet();

        Context list = new Context("viewer");
        servlet.doGet(list.request, list.response);
        check("/jsp/fertilizers.jsp".equals(list.forwarded) && ((List<?>) list.attributes.get("applications")).size() == 2,
                "Authenticated user can list fertilizer applications with crop/season labels");
        Context cropFilter = new Context("viewer");
        cropFilter.parameters.put("cropId", "1");
        servlet.doGet(cropFilter.request, cropFilter.response);
        check(((List<?>) cropFilter.attributes.get("applications")).size() == 1,
                "Crop filter returns only applications for that crop");
        Context seasonFilter = new Context("viewer");
        seasonFilter.parameters.put("seasonId", "2");
        servlet.doGet(seasonFilter.request, seasonFilter.response);
        check(((List<?>) seasonFilter.attributes.get("applications")).size() == 1,
                "Season filter returns only applications for that season");
        Context combinedFilter = new Context("viewer");
        combinedFilter.parameters.put("cropId", "1"); combinedFilter.parameters.put("seasonId", "2");
        servlet.doGet(combinedFilter.request, combinedFilter.response);
        check(((List<?>) combinedFilter.attributes.get("applications")).isEmpty(),
                "Combined crop and season filter cannot cross crop-season relationship");

        Context viewerAdd = new Context("viewer");
        viewerAdd.parameters.put("action", "new"); servlet.doGet(viewerAdd.request, viewerAdd.response);
        check(viewerAdd.status == 403, "Viewer cannot open admin add form");
        Context viewerEdit = new Context("viewer"); viewerEdit.parameters.put("action", "edit"); viewerEdit.parameters.put("id", "1");
        servlet.doGet(viewerEdit.request, viewerEdit.response);
        check(viewerEdit.status == 403, "Viewer cannot open admin edit form");
        Context viewerPost = new Context("viewer"); viewerPost.parameters.put("action", "create");
        servlet.doPost(viewerPost.request, viewerPost.response);
        check(viewerPost.status == 403 && fixture.fertilizers.size() == 2, "Viewer cannot add by direct POST");
        Context viewerUpdate = validPost("viewer", "update"); viewerUpdate.parameters.put("id", "1");
        servlet.doPost(viewerUpdate.request, viewerUpdate.response);
        Context viewerDelete = new Context("viewer"); viewerDelete.parameters.put("action", "delete"); viewerDelete.parameters.put("id", "1");
        servlet.doPost(viewerDelete.request, viewerDelete.response);
        check(viewerUpdate.status == 403 && viewerDelete.status == 403 && fixture.fertilizers.size() == 2,
                "Viewer cannot edit or delete by direct POST");

        Context invalid = validPost("admin", "create");
        invalid.parameters.put("quantityKg", "-2");
        servlet.doPost(invalid.request, invalid.response);
        check(fixture.fertilizers.size() == 2 && invalid.attributes.containsKey("errors"),
                "Non-positive quantity is rejected before persistence");
        Context missingRequired = validPost("admin", "create"); missingRequired.parameters.remove("fertilizerType");
        servlet.doPost(missingRequired.request, missingRequired.response);
        check(fixture.fertilizers.size() == 2 && missingRequired.attributes.containsKey("errors"),
                "Missing required fertilizer type is reported as a validation error");
        Context invalidDate = validPost("admin", "create"); invalidDate.parameters.put("applicationDate", "yesterday");
        servlet.doPost(invalidDate.request, invalidDate.response);
        check(fixture.fertilizers.size() == 2 && invalidDate.attributes.containsKey("errors"),
                "Malformed application date is rejected");
        Context invalidRelation = validPost("admin", "create"); invalidRelation.parameters.put("cropId", "1");
        invalidRelation.parameters.put("seasonId", "2"); servlet.doPost(invalidRelation.request, invalidRelation.response);
        check(fixture.fertilizers.size() == 2 && invalidRelation.attributes.containsKey("errors"),
                "Mismatched crop and season is rejected server-side");

        Context create = validPost("admin", "create"); servlet.doPost(create.request, create.response);
        check(fixture.fertilizers.size() == 3 && create.redirect.endsWith("/fertilizers?message=created"),
                "Admin can add a valid application");
        Context edit = validPost("admin", "update"); edit.parameters.put("id", "1");
        edit.parameters.put("fertilizerName", "Urea Plus"); servlet.doPost(edit.request, edit.response);
        check("Urea Plus".equals(fixture.find(1).getFertilizerName())
                        && edit.redirect.endsWith("/fertilizers?message=updated"), "Admin can edit an existing application");
        Context getDelete = new Context("admin"); getDelete.parameters.put("action", "delete"); getDelete.parameters.put("id", "1");
        servlet.doGet(getDelete.request, getDelete.response);
        check(getDelete.status == 405 && fixture.fertilizers.size() == 3, "Delete is rejected through GET");
        Context delete = new Context("admin"); delete.parameters.put("action", "delete"); delete.parameters.put("id", "1");
        servlet.doPost(delete.request, delete.response);
        check(fixture.find(1) == null && delete.redirect.endsWith("/fertilizers?message=deleted"), "Admin can delete through POST");

        testUnauthenticatedFilter();
        testH2Persistence();
        System.out.printf("Phase 7 fertilizer checks: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static Context validPost(String role, String action) {
        Context context = new Context(role);
        context.parameters.put("action", action); context.parameters.put("cropId", "1");
        context.parameters.put("seasonId", "1"); context.parameters.put("fertilizerName", "Urea");
        context.parameters.put("fertilizerType", "chemical"); context.parameters.put("quantityKg", "4.25");
        context.parameters.put("applicationDate", "2026-09-01"); context.parameters.put("appliedBy", "Farm team");
        context.parameters.put("notes", "Field test");
        return context;
    }

    private static void testUnauthenticatedFilter() throws Exception {
        Map<String, Object> state = new HashMap<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                    case "getContextPath" -> "/greenfields"; case "getRequestURI" -> "/greenfields/fertilizers";
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
                "Existing AuthFilter redirects unauthenticated fertilizer requests");
    }

    private static void testH2Persistence() throws Exception {
        String url = "jdbc:h2:mem:greenfields_fertilizer_phase7;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        DBConnection.setConfiguration(url, "sa", "");
        try {
            try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
                s.execute("CREATE TABLE crops (crop_id INT AUTO_INCREMENT PRIMARY KEY, crop_name VARCHAR(100) NOT NULL, crop_type VARCHAR(50) NOT NULL, variety VARCHAR(100), description CLOB, growth_duration_days INT, status VARCHAR(10) NOT NULL)");
                s.execute("CREATE TABLE seasons (season_id INT AUTO_INCREMENT PRIMARY KEY, crop_id INT NOT NULL REFERENCES crops(crop_id), season_name VARCHAR(100) NOT NULL, field_location VARCHAR(150), area_acres DECIMAL(8,2), planting_date DATE, expected_harvest_date DATE, season_status VARCHAR(10) NOT NULL, notes CLOB)");
                s.execute("CREATE TABLE fertilizer_applications (fertilizer_id INT AUTO_INCREMENT PRIMARY KEY, season_id INT NOT NULL REFERENCES seasons(season_id) ON DELETE CASCADE, fertilizer_name VARCHAR(100) NOT NULL, fertilizer_type VARCHAR(10) NOT NULL, quantity_kg DECIMAL(8,2) NOT NULL, application_date DATE NOT NULL, applied_by VARCHAR(100), notes CLOB)");
                s.execute("INSERT INTO crops(crop_name,crop_type,variety,growth_duration_days,status) VALUES ('Paddy','Grain','IR-64',120,'active'),('Maize','Grain','Sweet Corn',90,'active')");
                s.execute("INSERT INTO seasons(crop_id,season_name,season_status) VALUES (1,'Kharif','active'),(2,'Rabi','planned')");
            }
            CropDAO crops = new CropDAOImpl(); SeasonDAO seasons = new SeasonDAOImpl();
            FertilizerApplicationDAO fertilizer = new FertilizerApplicationDAOImpl();
            FertilizerServlet servlet = new FertilizerServlet(fertilizer, crops, seasons);
            Context create = validPost("admin", "create"); servlet.doPost(create.request, create.response);
            check(fertilizer.findAll().size() == 1 && fertilizer.findAll().get(0).getSeasonId() == 1,
                    "H2 JDBC integration persists application with season foreign key");
            Context filtered = new Context("viewer"); filtered.parameters.put("cropId", "1");
            servlet.doGet(filtered.request, filtered.response);
            check(((List<?>) filtered.attributes.get("applications")).size() == 1,
                    "H2 JDBC integration filters persisted application by crop");
            Context edit = validPost("admin", "update"); edit.parameters.put("id", "1");
            edit.parameters.put("fertilizerName", "Compost"); edit.parameters.put("fertilizerType", "organic");
            servlet.doPost(edit.request, edit.response);
            check("Compost".equals(fertilizer.findAll().get(0).getFertilizerName()), "H2 JDBC integration updates stored row");
            Context badRelationship = validPost("admin", "update"); badRelationship.parameters.put("id", "1");
            badRelationship.parameters.put("seasonId", "2"); servlet.doPost(badRelationship.request, badRelationship.response);
            check(fertilizer.findAll().get(0).getSeasonId() == 1 && badRelationship.attributes.containsKey("errors"),
                    "H2 persistence remains unchanged when crop-season foreign-key relationship is invalid");
            Context delete = new Context("admin"); delete.parameters.put("action", "delete"); delete.parameters.put("id", "1");
            servlet.doPost(delete.request, delete.response);
            check(fertilizer.findAll().isEmpty(), "H2 JDBC integration deletes persisted row");
        } finally { DBConnection.resetConfiguration(); }
    }

    private static void check(boolean result, String text) {
        if (result) { passed++; System.out.println("[PASS] " + text); }
        else { failed++; System.out.println("[FAIL] " + text); }
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
        private final List<FertilizerApplication> fertilizers = new ArrayList<>(List.of(
                new FertilizerApplication(1, 1, "Urea", "chemical", 2, LocalDate.of(2026, 8, 2), "Ravi", ""),
                new FertilizerApplication(2, 2, "Compost", "organic", 3, LocalDate.of(2026, 8, 3), "Priya", "")));
        private FertilizerServlet servlet() {
            CropDAO cropDAO = new CropDAO() {
                public List<Crop> findAll() { return crops; }
                public Crop findById(int id) { return crops.stream().filter(c -> c.getId() == id).findFirst().orElse(null); }
                public int save(Crop c) { return 0; } public void update(Crop c) { } public void delete(int id) { }
            };
            SeasonDAO seasonDAO = new SeasonDAO() {
                public List<Season> findAll() { return seasons; }
                public Season findById(int id) { return seasons.stream().filter(s -> s.getId() == id).findFirst().orElse(null); }
                public List<Season> findByCropId(int id) { return seasons.stream().filter(s -> s.getCropId() == id).toList(); }
                public int save(Season s) { return 0; } public void update(Season s) { }
            };
            FertilizerApplicationDAO fertilizerDAO = new FertilizerApplicationDAO() {
                public List<FertilizerApplication> findAll() { return new ArrayList<>(fertilizers); }
                public List<FertilizerApplication> findBySeasonId(int id) { return fertilizers.stream().filter(a -> a.getSeasonId() == id).toList(); }
                public List<FertilizerApplication> findByCropId(int id) { return fertilizers.stream().filter(a -> seasons.stream().anyMatch(s -> s.getId() == a.getSeasonId() && s.getCropId() == id)).toList(); }
                public int save(FertilizerApplication a) { a.setId(fertilizers.stream().mapToInt(FertilizerApplication::getId).max().orElse(0) + 1); fertilizers.add(a); return a.getId(); }
                public void update(FertilizerApplication a) { fertilizers.removeIf(old -> old.getId() == a.getId()); fertilizers.add(a); }
                public void delete(int id) { fertilizers.removeIf(a -> a.getId() == id); }
            };
            return new FertilizerServlet(fertilizerDAO, cropDAO, seasonDAO);
        }
        private FertilizerApplication find(int id) { return fertilizers.stream().filter(a -> a.getId() == id).findFirst().orElse(null); }
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
