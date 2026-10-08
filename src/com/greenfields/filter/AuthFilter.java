package com.greenfields.filter;

import com.greenfields.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * AuthFilter - Intercepts HTTP requests to protect authenticated routes.
 *
 * PURPOSE:
 *   Ensures that only users with an active HttpSession containing a valid
 *   "user" attribute can access protected resources like /dashboard, /crops,
 *   /seasons, /fertilizers, /irrigation, /harvest, /reports, etc.
 *
 * HOW IT WORKS:
 *   1. Identifies if the requested path is public (e.g. /login, static CSS/JS).
 *   2. If public -> passes request through chain.
 *   3. If protected -> checks session:
 *      - If session exists and has "user" attribute -> passes through.
 *      - Otherwise -> redirects to /login with an error parameter.
 *
 * OOP / DESIGN PRINCIPLES:
 *   - Interceptor pattern (Servlet Filter specification).
 *   - Centralized security check (DRY principle — no duplicate checks in each servlet).
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String contextPath = httpRequest.getContextPath();
        String uri = httpRequest.getRequestURI();

        // Extract relative path within the web application
        String path = uri.substring(contextPath.length());

        // Check if resource is public
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Check for active session without creating a new one
        HttpSession session = httpRequest.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("user") instanceof User);

        if (isLoggedIn) {
            // User is authenticated; allow access
            chain.doFilter(request, response);
        } else {
            // User is not authenticated; redirect to login
            httpResponse.sendRedirect(contextPath + "/login?error=auth_required");
        }
    }

    /**
     * Determines whether a given request path is publicly accessible without login.
     */
    public boolean isPublicPath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return true;
        }
        return path.startsWith("/login")
                || path.startsWith("/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".ico");
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
