package com.greenfields.dao.impl;

import com.greenfields.dao.SeasonDAO;
import com.greenfields.exception.DatabaseException;
import com.greenfields.model.Season;
import com.greenfields.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SeasonDAOImpl - JDBC implementation of SeasonDAO.
 * CRUD operations against the seasons table.
 */
public class SeasonDAOImpl implements SeasonDAO {

    // ─── SQL Queries ──────────────────────────────────────────────────────
    private static final String SQL_FIND_ALL =
            "SELECT season_id, crop_id, season_name, field_location, area_acres, "
            + "planting_date, expected_harvest_date, season_status, notes "
            + "FROM seasons ORDER BY planting_date DESC";

    private static final String SQL_FIND_BY_ID =
            "SELECT season_id, crop_id, season_name, field_location, area_acres, "
            + "planting_date, expected_harvest_date, season_status, notes "
            + "FROM seasons WHERE season_id = ?";

    private static final String SQL_FIND_BY_CROP_ID =
            "SELECT season_id, crop_id, season_name, field_location, area_acres, "
            + "planting_date, expected_harvest_date, season_status, notes "
            + "FROM seasons WHERE crop_id = ? ORDER BY planting_date DESC";

    private static final String SQL_INSERT =
            "INSERT INTO seasons (crop_id, season_name, field_location, area_acres, "
            + "planting_date, expected_harvest_date, season_status, notes) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE seasons SET crop_id=?, season_name=?, field_location=?, area_acres=?, "
            + "planting_date=?, expected_harvest_date=?, season_status=?, notes=? "
            + "WHERE season_id=?";

    // ─── Public DAO Methods ───────────────────────────────────────────────

    @Override
    public List<Season> findAll() {
        List<Season> seasons = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                seasons.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all seasons", e, e.getErrorCode());
        }
        return seasons;
    }

    @Override
    public Season findById(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding season by id: " + id, e, e.getErrorCode());
        }
        return null;
    }

    @Override
    public List<Season> findByCropId(int cropId) {
        List<Season> seasons = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CROP_ID)) {

            ps.setInt(1, cropId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seasons.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error finding seasons for crop_id: " + cropId, e, e.getErrorCode());
        }
        return seasons;
    }

    @Override
    public int save(Season season) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, season.getCropId());
            ps.setString(2, season.getSeasonName());
            ps.setString(3, season.getFieldLocation());
            ps.setDouble(4, season.getAreaAcres());

            // Convert LocalDate → java.sql.Date for JDBC
            ps.setDate(5, season.getPlantingDate() != null
                    ? Date.valueOf(season.getPlantingDate()) : null);
            ps.setDate(6, season.getExpectedHarvestDate() != null
                    ? Date.valueOf(season.getExpectedHarvestDate()) : null);

            ps.setString(7, season.getSeasonStatus());
            ps.setString(8, season.getNotes());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving season: " + season.getSeasonName(), e, e.getErrorCode());
        }
        throw new DatabaseException("Save failed: no generated key returned for season.");
    }

    @Override
    public void update(Season season) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, season.getCropId());
            ps.setString(2, season.getSeasonName());
            ps.setString(3, season.getFieldLocation());
            ps.setDouble(4, season.getAreaAcres());
            ps.setDate(5, season.getPlantingDate() != null
                    ? Date.valueOf(season.getPlantingDate()) : null);
            ps.setDate(6, season.getExpectedHarvestDate() != null
                    ? Date.valueOf(season.getExpectedHarvestDate()) : null);
            ps.setString(7, season.getSeasonStatus());
            ps.setString(8, season.getNotes());
            ps.setInt(9, season.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating season id: " + season.getId(), e, e.getErrorCode());
        }
    }

    // ─── Private helper: maps one ResultSet row → Season object ──────────
    private Season mapRow(ResultSet rs) throws SQLException {
        // Handle potentially null DATE columns
        java.sql.Date plantingDateSql      = rs.getDate("planting_date");
        java.sql.Date expectedHarvestDateSql = rs.getDate("expected_harvest_date");

        return new Season(
                rs.getInt("season_id"),
                rs.getInt("crop_id"),
                rs.getString("season_name"),
                rs.getString("field_location"),
                rs.getDouble("area_acres"),
                plantingDateSql      != null ? plantingDateSql.toLocalDate()       : null,
                expectedHarvestDateSql != null ? expectedHarvestDateSql.toLocalDate() : null,
                rs.getString("season_status"),
                rs.getString("notes")
        );
    }
}
