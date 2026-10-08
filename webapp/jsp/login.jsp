<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Greenfields Agri Farm — Login</title>
    <style>
        body { font-family: sans-serif; background-color: #F8F7F2; color: #1E3A2B; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
        .login-card { background: #FFFFFF; padding: 32px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); width: 340px; border: 1px solid #E2E8F0; }
        h2 { margin-top: 0; color: #1E3A2B; }
        .alert-error { background-color: #FEE2E2; color: #991B1B; padding: 10px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; }
        .alert-info { background-color: #DCFCE7; color: #166534; padding: 10px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; }
        .form-group { margin-bottom: 16px; }
        label { display: block; margin-bottom: 6px; font-weight: bold; font-size: 13px; }
        input[type="text"], input[type="password"] { width: 100%; padding: 8px 10px; border: 1px solid #CBD5E1; border-radius: 4px; box-sizing: border-box; }
        button { width: 100%; padding: 10px; background-color: #2D6A4F; color: #FFFFFF; border: none; border-radius: 4px; font-size: 15px; cursor: pointer; font-weight: bold; }
        button:hover { background-color: #1E3A2B; }
        .demo-credentials { margin-top: 20px; font-size: 12px; color: #64748B; background: #F1F5F9; padding: 8px; border-radius: 4px; }
    </style>
</head>
<body>
<div class="login-card">
    <h2>Greenfields Farm</h2>
    <p style="color: #64748B; font-size: 14px; margin-top: -8px;">Crop Monitoring System</p>

    <% if (request.getAttribute("errorMessage") != null) { %>
        <div class="alert-error"><%= request.getAttribute("errorMessage") %></div>
    <% } %>

    <% if (request.getAttribute("infoMessage") != null) { %>
        <div class="alert-info"><%= request.getAttribute("infoMessage") %></div>
    <% } %>

    <form action="<%= request.getContextPath() %>/login" method="post">
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" name="username" value="<%= request.getAttribute("username") != null ? request.getAttribute("username") : "" %>" required autofocus />
        </div>
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password" required />
        </div>
        <button type="submit">Sign In</button>
    </form>

    <div class="demo-credentials">
        <strong>Demo Login:</strong><br>
        Admin: <code>admin</code> / <code>admin123</code><br>
        Staff: <code>ravi</code> / <code>ravi123</code>
    </div>
</div>
</body>
</html>
