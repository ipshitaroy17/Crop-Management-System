<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.greenfields.model.User" %>
<%
    User currentUser = (User) request.getAttribute("currentUser");
    if (currentUser == null) {
        currentUser = (User) session.getAttribute("user");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Greenfields Agri Farm — Dashboard</title>
    <style>
        body { font-family: sans-serif; background-color: #F8F7F2; color: #1E3A2B; margin: 0; padding: 24px; }
        .header { display: flex; justify-content: space-between; align-items: center; background: #FFFFFF; padding: 16px 24px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
        h1 { margin: 0; font-size: 20px; color: #1E3A2B; }
        .user-tag { font-size: 14px; color: #475569; }
        .logout-btn { background: #DC2626; color: #FFFFFF; padding: 6px 14px; text-decoration: none; border-radius: 4px; font-weight: bold; font-size: 13px; }
        .content { margin-top: 24px; background: #FFFFFF; padding: 24px; border-radius: 8px; }
    </style>
</head>
<body>
<div class="header">
    <div>
        <h1>🌾 Greenfields Agri Farm Dashboard</h1>
        <div class="user-tag">Logged in as: <strong><%= currentUser != null ? currentUser.getFullName() : "User" %></strong> (<%= currentUser != null ? currentUser.getRole() : "" %>)</div>
    </div>
    <a href="<%= request.getContextPath() %>/logout" class="logout-btn">Logout</a>
</div>

<div class="content">
    <h3>Authentication & Session Active</h3>
    <p>Session ID: <code><%= session.getId() %></code></p>
    <p>Username: <code><%= currentUser != null ? currentUser.getUsername() : "N/A" %></code></p>
    <p>Full dashboard metrics and charts will be connected in Phase 6.</p>
</div>
</body>
</html>
