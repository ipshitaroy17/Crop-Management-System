package com.greenfields.servlet;

import com.greenfields.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * DashboardServlet - Controller for the main application dashboard.
 *
 * Protected by AuthFilter: can only be reached if an active user session exists.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;

        // Set user info attribute for view rendering
        request.setAttribute("currentUser", currentUser);

        // Forward to dashboard view
        request.getRequestDispatcher("/jsp/dashboard.jsp").forward(request, response);
    }
}
