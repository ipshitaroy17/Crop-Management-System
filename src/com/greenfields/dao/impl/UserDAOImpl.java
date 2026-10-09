package com.greenfields.dao.impl;

import com.greenfields.dao.UserDAO;
import com.greenfields.exception.DatabaseException;
import com.greenfields.model.User;
import com.greenfields.util.DBConnection;

import java.sql.*;

/**
 * UserDAOImpl - JDBC implementation of UserDAO.
 * Handles read access to the users table for session-based login.
 */
public class UserDAOImpl implements UserDAO {

    // ─── SQL Queries ──────────────────────────────────────────────────────
    private static final String SQL_FIND_BY_USERNAME =
            "SELECT user_id, username, password, full_name, role, created_at "
            + "FROM users WHERE username = ?";

    private static final String SQL_FIND_BY_ID =
            "SELECT user_id, username, password, full_name, role, created_at "
            + "FROM users WHERE user_id = ?";

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE users SET password = ? WHERE user_id = ?";

    // ─── Public DAO Methods ───────────────────────────────────────────────

    @Override
    public User findByUsername(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error finding user by username: " + username, e, e.getErrorCode());
        }
        return null;    // user not found
    }

    @Override
    public User findById(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error finding user by id: " + id, e, e.getErrorCode());
        }
        return null;
    }

    @Override
    public void updatePassword(int id, String password) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, password);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user password", e, e.getErrorCode());
        }
    }

    // ─── Private helper: maps one ResultSet row → User object ────────────
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }
}
