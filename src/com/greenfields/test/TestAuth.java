package com.greenfields.test;

import com.greenfields.dao.UserDAO;
import com.greenfields.dao.impl.UserDAOImpl;
import com.greenfields.filter.AuthFilter;
import com.greenfields.model.User;
import com.greenfields.servlet.DashboardServlet;
import com.greenfields.servlet.LoginServlet;
import com.greenfields.servlet.LogoutServlet;
import com.greenfields.util.DBConnection;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TestAuth - Automated Verification for Phase 4: Authentication & Session Management.
 *
 * Verifies all 7 requirements:
 *   1. Valid login (UserDAO check, session creation, redirect to /dashboard)
 *   2. Invalid login (error message returned, forward to /jsp/login.jsp)
 *   3. Session creation & user attribute integrity
 *   4. Access to protected page with active session (AuthFilter allows)
 *   5. Access to protected page without session (AuthFilter blocks & redirects)
 *   6. Logout (session invalidated, redirect to /login)
 *   7. Access to protected page after logout (AuthFilter blocks & redirects)
 */
public class TestAuth {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   GREENFIELDS AGRI FARM - PHASE 4 AUTH TEST SUITE");
        System.out.println("=================================================\n");

        ensureDatabaseReady();

        testValidLogin();
        testInvalidLogin();
        testSessionCreation();
        testAccessProtectedPageWithSession();
        testAccessProtectedPageWithoutSession();
        testLogout();
        testAccessProtectedPageAfterLogout();

        DBConnection.resetConfiguration();

        System.out.println("\n=================================================");
        System.out.printf("   TOTAL RESULTS: %d PASSED  |  %d FAILED%n", passed, failed);
        System.out.println("=================================================");
    }

    private static void ensureDatabaseReady() {
        boolean connected = false;
        try {
            connected = DBConnection.testConnection();
        } catch (Exception ignored) {}

        if (!connected) {
            String testUrl = "jdbc:h2:mem:greenfields_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
            try {
                Connection conn = DriverManager.getConnection(testUrl, "sa", "");
                Statement stmt = conn.createStatement();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader("sql/greenfields_db.sql"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty() ||
                            trimmed.toUpperCase().startsWith("DROP DATABASE") ||
                            trimmed.toUpperCase().startsWith("CREATE DATABASE") ||
                            trimmed.toUpperCase().startsWith("USE ")) {
                            continue;
                        }
                        sb.append(line).append("\n");
                        if (trimmed.endsWith(";")) {
                            String sql = sb.toString().trim();
                            sql = sql.substring(0, sql.length() - 1);
                            if (!sql.isEmpty()) {
                                try { stmt.execute(sql); } catch (Exception ignored) {}
                            }
                            sb.setLength(0);
                        }
                    }
                }
                DBConnection.setConfiguration(testUrl, "sa", "");
            } catch (Exception e) {
                System.err.println("Setup error: " + e.getMessage());
            }
        }
    }

    // ─── Test 1: Valid Login ──────────────────────────────────────────────
    static void testValidLogin() {
        System.out.println("--- Test 1: Valid Login Authentication ---");
        try {
            LoginServlet servlet = new LoginServlet();
            MockContext ctx = new MockContext();
            ctx.parameters.put("username", "admin");
            ctx.parameters.put("password", "admin123");

            servlet.doPost(ctx.request, ctx.response);

            if ("/dashboard".equals(ctx.redirectUrl)) {
                pass("doPost() redirected to /dashboard on valid credentials.");
            } else {
                fail("Expected redirect to /dashboard, got: " + ctx.redirectUrl);
            }

            if (ctx.session != null && ctx.sessionAttributes.get("user") instanceof User user) {
                if ("admin".equals(user.getUsername())) {
                    pass("HttpSession created and user attribute stored (User: " + user.getFullName() + ")");
                } else {
                    fail("Session user username mismatch: " + user.getUsername());
                }
                if (user.getPassword() == null) {
                    pass("Authenticated session principal does not retain the password.");
                } else {
                    fail("Password was retained in the authenticated session principal.");
                }
            } else {
                fail("HttpSession was not created or user attribute missing.");
            }
        } catch (Exception e) {
            fail("Valid login threw exception: " + e.getMessage());
        }
    }

    // ─── Test 2: Invalid Login ────────────────────────────────────────────
    static void testInvalidLogin() {
        System.out.println("\n--- Test 2: Invalid Login Handling ---");
        try {
            LoginServlet servlet = new LoginServlet();
            MockContext ctx = new MockContext();
            ctx.parameters.put("username", "admin");
            ctx.parameters.put("password", "wrongpassword");

            servlet.doPost(ctx.request, ctx.response);

            if (ctx.redirectUrl == null) {
                pass("doPost() did not redirect to dashboard on invalid credentials.");
            } else {
                fail("doPost() unexpectedly redirected to: " + ctx.redirectUrl);
            }

            if ("/jsp/login.jsp".equals(ctx.forwardedPath)) {
                pass("Forwarded back to /jsp/login.jsp.");
            } else {
                fail("Expected forward to /jsp/login.jsp, got: " + ctx.forwardedPath);
            }

            Object err = ctx.requestAttributes.get("errorMessage");
            if (err != null && err.toString().contains("Invalid username or password")) {
                pass("Set expected error message: '" + err + "'");
            } else {
                fail("Error message missing or unexpected: " + err);
            }
        } catch (Exception e) {
            fail("Invalid login threw exception: " + e.getMessage());
        }
    }

    // ─── Test 3: Session Creation & Integrity ─────────────────────────────
    static void testSessionCreation() {
        System.out.println("\n--- Test 3: Session Creation & State Integrity ---");
        try {
            LoginServlet servlet = new LoginServlet();
            MockContext ctx = new MockContext();
            ctx.parameters.put("username", "ravi");
            ctx.parameters.put("password", "ravi123");

            servlet.doPost(ctx.request, ctx.response);

            if (ctx.session != null) {
                User user = (User) ctx.sessionAttributes.get("user");
                if (user != null && "viewer".equals(user.getRole()) && "Ravi Kumar".equals(user.getFullName())) {
                    pass("Session created for staff user Ravi Kumar with role 'viewer'.");
                    if (user.getPassword() == null) {
                        pass("Viewer session principal does not retain the password.");
                    } else {
                        fail("Viewer password was retained in the session principal.");
                    }
                } else {
                    fail("User details incorrect in session.");
                }
            } else {
                fail("Session was not created.");
            }
        } catch (Exception e) {
            fail("Session creation test threw exception: " + e.getMessage());
        }
    }

    // ─── Test 4: Access Protected Page With Session ───────────────────────
    static void testAccessProtectedPageWithSession() {
        System.out.println("\n--- Test 4: Access Protected Page WITH Valid Session ---");
        try {
            AuthFilter filter = new AuthFilter();
            MockContext ctx = new MockContext();
            ctx.requestUri = "/dashboard";
            // Populate active session
            ctx.session = ctx.createSession();
            ctx.sessionAttributes.put("user", new User(1, "admin", "admin123", "Farm Administrator", "admin", null));

            filter.doFilter(ctx.request, ctx.response, ctx.chain);

            if (ctx.chainInvoked) {
                pass("AuthFilter allowed access to /dashboard when user is logged in.");
            } else {
                fail("AuthFilter blocked access despite valid session.");
            }

            if (ctx.redirectUrl == null) {
                pass("No unauthorized redirect occurred.");
            } else {
                fail("Unexpected redirect to: " + ctx.redirectUrl);
            }
        } catch (Exception e) {
            fail("Protected page access test threw exception: " + e.getMessage());
        }
    }

    // ─── Test 5: Access Protected Page Without Session ────────────────────
    static void testAccessProtectedPageWithoutSession() {
        System.out.println("\n--- Test 5: Access Protected Page WITHOUT Session ---");
        try {
            AuthFilter filter = new AuthFilter();
            MockContext ctx = new MockContext();
            ctx.requestUri = "/dashboard";
            ctx.session = null; // No session

            filter.doFilter(ctx.request, ctx.response, ctx.chain);

            if (!ctx.chainInvoked) {
                pass("AuthFilter correctly blocked request to /dashboard without session.");
            } else {
                fail("AuthFilter allowed unauthenticated access to /dashboard!");
            }

            if (ctx.redirectUrl != null && ctx.redirectUrl.contains("/login?error=auth_required")) {
                pass("AuthFilter redirected unauthenticated request to /login?error=auth_required.");
            } else {
                fail("Expected redirect to /login?error=auth_required, got: " + ctx.redirectUrl);
            }
        } catch (Exception e) {
            fail("Unauthenticated access test threw exception: " + e.getMessage());
        }
    }

    // ─── Test 6: Logout ───────────────────────────────────────────────────
    static void testLogout() {
        System.out.println("\n--- Test 6: Logout Functionality ---");
        try {
            LogoutServlet servlet = new LogoutServlet();
            MockContext ctx = new MockContext();
            ctx.session = ctx.createSession();
            ctx.sessionAttributes.put("user", new User(1, "admin", "admin123", "Admin", "admin", null));

            servlet.doGet(ctx.request, ctx.response);

            if (ctx.sessionInvalidated) {
                pass("LogoutServlet invalidated the active HttpSession.");
            } else {
                fail("LogoutServlet failed to invalidate the session.");
            }

            if (ctx.redirectUrl != null && ctx.redirectUrl.contains("/login?msg=logged_out")) {
                pass("Redirected to /login?msg=logged_out.");
            } else {
                fail("Expected redirect to /login?msg=logged_out, got: " + ctx.redirectUrl);
            }
        } catch (Exception e) {
            fail("Logout test threw exception: " + e.getMessage());
        }
    }

    // ─── Test 7: Access After Logout ──────────────────────────────────────
    static void testAccessProtectedPageAfterLogout() {
        System.out.println("\n--- Test 7: Access Protected Page After Logout ---");
        try {
            AuthFilter filter = new AuthFilter();
            MockContext ctx = new MockContext();
            ctx.requestUri = "/dashboard";
            // Simulate invalidated/cleared session
            ctx.session = null;

            filter.doFilter(ctx.request, ctx.response, ctx.chain);

            if (!ctx.chainInvoked) {
                pass("AuthFilter blocked access to /dashboard after logout.");
            } else {
                fail("AuthFilter allowed access after logout!");
            }

            if (ctx.redirectUrl != null && ctx.redirectUrl.contains("/login")) {
                pass("Redirected to login page after logout attempt.");
            } else {
                fail("Expected redirect to login, got: " + ctx.redirectUrl);
            }
        } catch (Exception e) {
            fail("Access after logout test threw exception: " + e.getMessage());
        }
    }

    private static void pass(String msg) {
        System.out.println("  [PASS] " + msg);
        passed++;
    }

    private static void fail(String msg) {
        System.out.println("  [FAIL] " + msg);
        failed++;
    }

    // ─── Mocking Harness via Dynamic Proxies ──────────────────────────────
    static class MockContext {
        Map<String, String> parameters = new HashMap<>();
        Map<String, Object> requestAttributes = new HashMap<>();
        Map<String, Object> sessionAttributes = new HashMap<>();
        String requestUri = "/login";
        String redirectUrl = null;
        String forwardedPath = null;
        boolean sessionInvalidated = false;
        boolean chainInvoked = false;

        HttpSession session = null;
        HttpServletRequest request;
        HttpServletResponse response;
        FilterChain chain;

        MockContext() {
            this.request = (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        switch (name) {
                            case "getParameter" -> {
                                return parameters.get((String) args[0]);
                            }
                            case "setAttribute" -> {
                                requestAttributes.put((String) args[0], args[1]);
                                return null;
                            }
                            case "getAttribute" -> {
                                return requestAttributes.get((String) args[0]);
                            }
                            case "getContextPath" -> {
                                return "";
                            }
                            case "getRequestURI" -> {
                                return requestUri;
                            }
                            case "getSession" -> {
                                boolean create = (args == null || args.length == 0 || (Boolean) args[0]);
                                if (session == null && create) {
                                    session = createSession();
                                }
                                return session;
                            }
                            case "getRequestDispatcher" -> {
                                forwardedPath = (String) args[0];
                                return (RequestDispatcher) Proxy.newProxyInstance(
                                        RequestDispatcher.class.getClassLoader(),
                                        new Class<?>[]{RequestDispatcher.class},
                                        (dProxy, dMethod, dArgs) -> null
                                );
                            }
                            default -> {
                                return defaultValue(method.getReturnType());
                            }
                        }
                    }
            );

            this.response = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> {
                        if ("sendRedirect".equals(method.getName())) {
                            redirectUrl = (String) args[0];
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );

            this.chain = (req, res) -> chainInvoked = true;
        }

        HttpSession createSession() {
            String sid = UUID.randomUUID().toString();
            return (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getId" -> {
                                return sid;
                            }
                            case "setAttribute" -> {
                                sessionAttributes.put((String) args[0], args[1]);
                                return null;
                            }
                            case "getAttribute" -> {
                                if (sessionInvalidated) return null;
                                return sessionAttributes.get((String) args[0]);
                            }
                            case "invalidate" -> {
                                sessionInvalidated = true;
                                sessionAttributes.clear();
                                session = null;
                                return null;
                            }
                            default -> {
                                return defaultValue(method.getReturnType());
                            }
                        }
                    }
            );
        }

        private static Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) return false;
            if (returnType == int.class) return 0;
            if (returnType == long.class) return 0L;
            return null;
        }
    }
}
