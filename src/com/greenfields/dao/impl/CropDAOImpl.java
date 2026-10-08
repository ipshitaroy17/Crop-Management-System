package com.greenfields.dao.impl;

import com.greenfields.dao.CropDAO;
import com.greenfields.exception.DatabaseException;
import com.greenfields.model.Crop;
import com.greenfields.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CropDAOImpl - JDBC implementation of CropDAO.
 * Full CRUD operations against the crops table.
 */
public class CropDAOImpl implements CropDAO {

    // ─── SQL Queries ──────────────────────────────────────────────────────
    private static final String SQL_FIND_ALL =
            "SELECT crop_id, crop_name, crop_type, variety, description, "
            + "growth_duration_days, status "
            + "FROM crops ORDER BY crop_name";

    private static final String SQL_FIND_BY_ID =
            "SELECT crop_id, crop_name, crop_type, variety, description, "
            + "growth_duration_days, status "
            + "FROM crops WHERE crop_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO crops (crop_name, crop_type, variety, description, "
            + "growth_duration_days, status) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE crops SET crop_name=?, crop_type=?, variety=?, description=?, "
            + "growth_duration_days=?, status=? WHERE crop_id=?";

    private static final String SQL_DELETE =
            "DELETE FROM crops WHERE crop_id=?";

    // ─── Public DAO Methods ───────────────────────────────────────────────

    @Override
    public List<Crop> findAll() {
        List<Crop> crops = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                crops.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all crops", e, e.getErrorCode());
        }
        return crops;
    }

    @Override
    public Crop findById(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding crop by id: " + id, e, e.getErrorCode());
        }
        return null;
    }

    @Override
    public int save(Crop crop) {
        // Statement.RETURN_GENERATED_KEYS tells JDBC to return the auto-increment id
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, crop.getCropName());
            ps.setString(2, crop.getCropType());
            ps.setString(3, crop.getVariety());
            ps.setString(4, crop.getDescription());
            ps.setInt(5, crop.getGrowthDurationDays());
            ps.setString(6, crop.getStatus());

            ps.executeUpdate();

            // Retrieve the auto-generated crop_id
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving crop: " + crop.getCropName(), e, e.getErrorCode());
        }
        throw new DatabaseException("Save failed: no generated key returned for crop.");
    }

    @Override
    public void update(Crop crop) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, crop.getCropName());
            ps.setString(2, crop.getCropType());
            ps.setString(3, crop.getVariety());
            ps.setString(4, crop.getDescription());
            ps.setInt(5, crop.getGrowthDurationDays());
            ps.setString(6, crop.getStatus());
            ps.setInt(7, crop.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating crop id: " + crop.getId(), e, e.getErrorCode());
        }
    }

    @Override
    public void delete(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            // FK constraint violation (seasons reference this crop) will surface here
            throw new DatabaseException(
                    "Error deleting crop id: " + id
                    + ". Make sure no seasons are linked to this crop before deleting.",
                    e, e.getErrorCode());
        }
    }

    // ─── Private helper: maps one ResultSet row → Crop object ────────────
    private Crop mapRow(ResultSet rs) throws SQLException {
        return new Crop(
                rs.getInt("crop_id"),
                rs.getString("crop_name"),
                rs.getString("crop_type"),
                rs.getString("variety"),
                rs.getString("description"),
                rs.getInt("growth_duration_days"),
                rs.getString("status")
        );
    }
}
