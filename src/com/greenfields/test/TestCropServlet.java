package com.greenfields.test;

import com.greenfields.dao.CropDAO;
import com.greenfields.model.Crop;
import com.greenfields.model.User;
import com.greenfields.servlet.CropServlet;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCropServlet {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        FakeCropDAO dao = new FakeCropDAO();
        dao.rows.add(new Crop(1, "Paddy", "Grain", "IR-64", "Rice", 120, "active"));
        CropServlet servlet = new CropServlet(dao);

        MockContext viewerList = new MockContext("viewer");
        servlet.doGet(viewerList.request, viewerList.response);
        check(viewerList.forwardedPath.equals("/jsp/crops.jsp")
                && ((List<?>) viewerList.attributes.get("crops")).size() == 1,
                "Authenticated viewer can load crop list");

        MockContext viewerDetails = new MockContext("viewer");
        viewerDetails.parameters.put("action", "details");
        viewerDetails.parameters.put("id", "1");
        servlet.doGet(viewerDetails.request, viewerDetails.response);
        check(viewerDetails.forwardedPath.equals("/jsp/crop-details.jsp"),
                "Authenticated viewer can load crop details");

        MockContext viewerAdd = new MockContext("viewer");
        viewerAdd.parameters.put("action", "new");
        servlet.doGet(viewerAdd.request, viewerAdd.response);
        check(viewerAdd.errorStatus == HttpServletResponse.SC_FORBIDDEN,
                "Viewer cannot open the add form");

        MockContext viewerEdit = new MockContext("viewer");
        viewerEdit.parameters.put("action", "edit");
        viewerEdit.parameters.put("id", "1");
        servlet.doGet(viewerEdit.request, viewerEdit.response);
        check(viewerEdit.errorStatus == HttpServletResponse.SC_FORBIDDEN,
                "Viewer cannot open the edit form");

        MockContext viewerCreate = new MockContext("viewer");
        viewerCreate.parameters.put("action", "create");
        servlet.doPost(viewerCreate.request, viewerCreate.response);
        check(viewerCreate.errorStatus == HttpServletResponse.SC_FORBIDDEN && dao.saveCount == 0,
                "Viewer cannot create a crop by posting directly");

        MockContext viewerDelete = new MockContext("viewer");
        viewerDelete.parameters.put("action", "delete");
        viewerDelete.parameters.put("id", "1");
        servlet.doPost(viewerDelete.request, viewerDelete.response);
        check(viewerDelete.errorStatus == HttpServletResponse.SC_FORBIDDEN && dao.deleteCount == 0,
                "Viewer cannot delete a crop by posting directly");

        MockContext invalidCreate = new MockContext("admin");
        invalidCreate.parameters.put("action", "create");
        invalidCreate.parameters.put("cropName", " ");
        invalidCreate.parameters.put("cropType", "Grain");
        invalidCreate.parameters.put("growthDurationDays", "0");
        invalidCreate.parameters.put("status", "active");
        servlet.doPost(invalidCreate.request, invalidCreate.response);
        check(dao.saveCount == 0 && invalidCreate.forwardedPath.equals("/jsp/crop-form.jsp")
                && invalidCreate.attributes.containsKey("errors"),
                "Invalid crop is rejected with field errors before persistence");

        MockContext create = new MockContext("admin");
        create.parameters.put("action", "create");
        create.parameters.put("cropName", "Maize");
        create.parameters.put("cropType", "Grain");
        create.parameters.put("variety", "Sweet Corn");
        create.parameters.put("growthDurationDays", "90");
        create.parameters.put("status", "active");
        servlet.doPost(create.request, create.response);
        check(dao.saveCount == 1 && create.redirectUrl.endsWith("/crops?message=created"),
                "Admin can add a valid crop");

        MockContext edit = new MockContext("admin");
        edit.parameters.put("action", "update");
        edit.parameters.put("id", "1");
        edit.parameters.put("cropName", "Paddy Improved");
        edit.parameters.put("cropType", "Grain");
        edit.parameters.put("growthDurationDays", "125");
        edit.parameters.put("status", "active");
        servlet.doPost(edit.request, edit.response);
        check(dao.updateCount == 1 && "Paddy Improved".equals(dao.findById(1).getCropName()),
                "Admin can edit an existing crop");

        MockContext getDelete = new MockContext("admin");
        getDelete.parameters.put("action", "delete");
        getDelete.parameters.put("id", "1");
        servlet.doGet(getDelete.request, getDelete.response);
        check(getDelete.errorStatus == HttpServletResponse.SC_METHOD_NOT_ALLOWED
                && dao.deleteCount == 0, "Delete is not available through GET");

        MockContext delete = new MockContext("admin");
        delete.parameters.put("action", "delete");
        delete.parameters.put("id", "1");
        servlet.doPost(delete.request, delete.response);
        check(dao.deleteCount == 1 && dao.findById(1) == null,
                "Admin can delete through POST");

        System.out.printf("Phase 5 servlet checks: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
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

    private static final class FakeCropDAO implements CropDAO {
        private final List<Crop> rows = new ArrayList<>();
        private int nextId = 2;
        private int saveCount;
        private int updateCount;
        private int deleteCount;

        public List<Crop> findAll() { return new ArrayList<>(rows); }
        public Crop findById(int id) { return rows.stream().filter(c -> c.getId() == id).findFirst().orElse(null); }
        public int save(Crop crop) { crop.setId(nextId++); rows.add(crop); saveCount++; return crop.getId(); }
        public void update(Crop crop) {
            Crop existing = findById(crop.getId());
            if (existing != null) {
                rows.set(rows.indexOf(existing), crop);
                updateCount++;
            }
        }
        public void delete(int id) { rows.removeIf(c -> c.getId() == id); deleteCount++; }
    }

    private static final class MockContext {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final User user;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private String forwardedPath;
        private String redirectUrl;
        private int errorStatus;

        private MockContext(String role) {
            user = new User();
            user.setRole(role);
            HttpSession session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(), new Class<?>[]{HttpSession.class},
                    (proxy, method, args) -> "getAttribute".equals(method.getName())
                            && "user".equals(args[0]) ? user : defaultValue(method.getReturnType()));
            request = (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getParameter" -> parameters.get(args[0]);
                        case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
                        case "getAttribute" -> attributes.get(args[0]);
                        case "getContextPath" -> "/greenfields";
                        case "getSession" -> session;
                        case "getRequestDispatcher" -> {
                            forwardedPath = (String) args[0];
                            yield Proxy.newProxyInstance(RequestDispatcher.class.getClassLoader(),
                                    new Class<?>[]{RequestDispatcher.class}, (p, m, a) -> null);
                        }
                        default -> defaultValue(method.getReturnType());
                    });
            response = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> {
                        if ("sendRedirect".equals(method.getName())) redirectUrl = (String) args[0];
                        if ("sendError".equals(method.getName())) errorStatus = (int) args[0];
                        return defaultValue(method.getReturnType());
                    });
        }

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            return null;
        }
    }
}
