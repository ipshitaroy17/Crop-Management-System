package com.greenfields.dao.impl;

import com.greenfields.dao.FertilizerApplicationDAO;
import com.greenfields.exception.DatabaseException;
import com.greenfields.model.FertilizerApplication;
import com.greenfields.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FertilizerApplicationDAOImpl - JDBC implementation of FertilizerApplicationDAO.
 * CRUD operations against the fertilizer_applications table.
 */
public class FertilizerApplicationDAOImpl implements FertilizerApplicationDAO {

    // ─── SQL Queries ──────────────────────────────────────────────────────
    private static final String COLUMNS =
            "fertilizer_id, season_id, fertilizer_name, fertilizer_type, "
            + "quantity_kg, application_date, applied_by, notes";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLUMNS + " FROM fertilizer_applications "
            + "ORDER BY application_date DESC";

    private static final String SQL_FIND_BY_SEASON =
            "SELECT " + COLUMNS + " FROM fertilizer_applications "
            + "WHERE season_id = ? ORDER BY application_date";

    // JOIN through seasons to filter by crop
    private static final String SQL_FIND_BY_CROP =
            "SELECT fa.fertilizer_id, fa.season_id, fa.fertilizer_name, fa.fertilizer_type, "
            + "fa.quantity_kg, fa.application_date, fa.applied_by, fa.notes "
            + "FROM fertilizer_applications fa "
            + "INNER JOIN seasons s ON fa.season_id = s.season_id "
            + "WHERE s.crop_id = ? ORDER BY fa.application_date DESC";

    private static final String SQL_INSERT =
            "INSERT INTO fertilizer_applications "
            + "(season_id, fertilizer_name, fertilizer_type, quantity_kg, "
            + "application_date, applied_by, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE fertilizer_applications SET season_id=?, fertilizer_name=?, "
            + "fertilizer_type=?, quantity_kg=?, application_date=?, "
            + "applied_by=?, notes=? WHERE fertilizer_id=?";

    private static final String SQL_DELETE =
            "DELETE FROM fertilizer_applications WHERE fertilizer_id=?";

    // ─── Public DAO Methods ───────────────────────────────────────────────

    @Override
    public List<FertilizerApplication> findAll() {
        List<FertilizerApplication> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all fertilizer records", e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public List<FertilizerApplication> findBySeasonId(int seasonId) {
        List<FertilizerApplication> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_SEASON)) {

            ps.setInt(1, seasonId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error finding fertilizer records for season_id: " + seasonId, e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public List<FertilizerApplication> findByCropId(int cropId) {
        List<FertilizerApplication> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CROP)) {

            ps.setInt(1, cropId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error finding fertilizer records for crop_id: " + cropId, e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public int save(FertilizerApplication fa) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, fa.getSeasonId());
            ps.setString(2, fa.getFertilizerName());
            ps.setString(3, fa.getFertilizerType());
            ps.setDouble(4, fa.getQuantityKg());
            ps.setDate(5, fa.getApplicationDate() != null
                    ? Date.valueOf(fa.getApplicationDate()) : null);
            ps.setString(6, fa.getAppliedBy());
            ps.setString(7, fa.getNotes());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error saving fertilizer application: " + fa.getFertilizerName(), e, e.getErrorCode());
        }
        throw new DatabaseException("Save failed: no generated key returned for fertilizer application.");
    }

    @Override
    public void update(FertilizerApplication fa) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, fa.getSeasonId());
            ps.setString(2, fa.getFertilizerName());
            ps.setString(3, fa.getFertilizerType());
            ps.setDouble(4, fa.getQuantityKg());
            ps.setDate(5, fa.getApplicationDate() != null
                    ? Date.valueOf(fa.getApplicationDate()) : null);
            ps.setString(6, fa.getAppliedBy());
            ps.setString(7, fa.getNotes());
            ps.setInt(8, fa.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error updating fertilizer id: " + fa.getId(), e, e.getErrorCode());
        }
    }

    @Override
    public void delete(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error deleting fertilizer application id: " + id, e, e.getErrorCode());
        }
    }

    // ─── Private helper: maps one ResultSet row → FertilizerApplication ──
    private FertilizerApplication mapRow(ResultSet rs) throws SQLException {
        java.sql.Date appDateSql = rs.getDate("application_date");

        return new FertilizerApplication(
                rs.getInt("fertilizer_id"),
                rs.getInt("season_id"),
                rs.getString("fertilizer_name"),
                rs.getString("fertilizer_type"),
                rs.getDouble("quantity_kg"),
                appDateSql != null ? appDateSql.toLocalDate() : null,
                rs.getString("applied_by"),
                rs.getString("notes")
        );
    }
}
