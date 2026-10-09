package com.greenfields.servlet;

import com.greenfields.dao.UserDAO;
import com.greenfields.dao.impl.UserDAOImpl;
import com.greenfields.model.User;
import com.greenfields.util.PasswordHasher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LoginServlet - Handles user authentication and session creation.
 *
 * ARCHITECTURE:
 *   JSP (login.jsp) -> LoginServlet (Controller) -> UserDAO -> MySQL (users table)
 *
 * RESPONSIBILITIES:
 *   - GET  : Renders the login form (or redirects to /dashboard if already logged in)
 *   - POST : Validates credentials via UserDAO, establishes an HttpSession on success,
 *            or returns an error message on failure.
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

    private final UserDAO userDAO;

    public LoginServlet() {
        this.userDAO = new UserDAOImpl();
    }

    // Constructor injection for testing
    public LoginServlet(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // If user already has an active session, forward directly to dashboard
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof User) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        // Check if an error code was passed via query parameter (e.g. from AuthFilter)
        String errorCode = request.getParameter("error");
        if ("auth_required".equalsIgnoreCase(errorCode)) {
            request.setAttribute("errorMessage", "Please log in to access this page.");
        }

        String msg = request.getParameter("msg");
        if ("logged_out".equalsIgnoreCase(msg)) {
            request.setAttribute("infoMessage", "You have been successfully logged out.");
        }

        request.getRequestDispatcher("/jsp/login.jsp").forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        // Validate non-empty input
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            request.setAttribute("errorMessage", "Username and password cannot be empty.");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/jsp/login.jsp").forward(request, response);
            return;
        }

        try {
            // Authenticate against database via UserDAO
            User user = userDAO.findByUsername(username);

            if (user != null && PasswordHasher.matches(password, user.getPassword())) {
                if (!PasswordHasher.isHashed(user.getPassword())) {
                    userDAO.updatePassword(user.getId(), PasswordHasher.hash(password.toCharArray()));
                }
                // Keep credentials out of the long-lived session principal.
                User sessionUser = new User(user.getId(), user.getUsername(), null,
                        user.getFullName(), user.getRole(), user.getCreatedAt());
                HttpSession session = request.getSession(true);
                session.setAttribute("user", sessionUser);

                // Redirect to dashboard (PRG pattern: Post-Redirect-Get)
                response.sendRedirect(request.getContextPath() + "/dashboard");
            } else {
                // Authentication FAILED:
                request.setAttribute("errorMessage", "Invalid username or password.");
                request.setAttribute("username", username);
                request.getRequestDispatcher("/jsp/login.jsp").forward(request, response);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login authentication failed while accessing the user store", e);
            request.setAttribute("errorMessage", "Login is temporarily unavailable. Please try again later.");
            request.getRequestDispatcher("/jsp/login.jsp").forward(request, response);
        }
    }
}
