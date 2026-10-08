package com.greenfields.dao.impl;

import com.greenfields.dao.HarvestRecordDAO;
import com.greenfields.exception.DatabaseException;
import com.greenfields.model.HarvestRecord;
import com.greenfields.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * HarvestRecordDAOImpl - JDBC implementation of HarvestRecordDAO.
 * CRUD operations against the harvest_records table.
 *
 * KEY NOTE:
 *   actual_yield_kg and quality_grade are nullable columns (harvest may be pending).
 *   We use ps.setNull() when these values are null, and rs.getObject() with a type
 *   hint when reading them back — this correctly handles the null case.
 */
public class HarvestRecordDAOImpl implements HarvestRecordDAO {

    // ─── SQL Queries ──────────────────────────────────────────────────────
    private static final String COLUMNS =
            "harvest_id, season_id, harvest_date, expected_yield_kg, "
            + "actual_yield_kg, quality_grade, remarks, recorded_by";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLUMNS + " FROM harvest_records ORDER BY harvest_date DESC";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLUMNS + " FROM harvest_records WHERE harvest_id = ?";

    private static final String SQL_FIND_BY_SEASON =
            "SELECT " + COLUMNS + " FROM harvest_records "
            + "WHERE season_id = ? ORDER BY harvest_date DESC";

    // JOIN through seasons to filter by crop
    private static final String SQL_FIND_BY_CROP =
            "SELECT hr.harvest_id, hr.season_id, hr.harvest_date, hr.expected_yield_kg, "
            + "hr.actual_yield_kg, hr.quality_grade, hr.remarks, hr.recorded_by "
            + "FROM harvest_records hr "
            + "INNER JOIN seasons s ON hr.season_id = s.season_id "
            + "WHERE s.crop_id = ? ORDER BY hr.harvest_date DESC";

    private static final String SQL_INSERT =
            "INSERT INTO harvest_records "
            + "(season_id, harvest_date, expected_yield_kg, actual_yield_kg, "
            + "quality_grade, remarks, recorded_by) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE harvest_records SET season_id=?, harvest_date=?, expected_yield_kg=?, "
            + "actual_yield_kg=?, quality_grade=?, remarks=?, recorded_by=? "
            + "WHERE harvest_id=?";

    private static final String SQL_DELETE = "DELETE FROM harvest_records WHERE harvest_id=?";

    // ─── Public DAO Methods ───────────────────────────────────────────────

    @Override
    public List<HarvestRecord> findAll() {
        List<HarvestRecord> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all harvest records", e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public HarvestRecord findById(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding harvest record by id: " + id, e, e.getErrorCode());
        }
        return null;
    }

    @Override
    public List<HarvestRecord> findBySeasonId(int seasonId) {
        List<HarvestRecord> list = new ArrayList<>();

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
                    "Error finding harvest records for season_id: " + seasonId, e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public List<HarvestRecord> findByCropId(int cropId) {
        List<HarvestRecord> list = new ArrayList<>();

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
                    "Error finding harvest records for crop_id: " + cropId, e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public int save(HarvestRecord record) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, record.getSeasonId());
            ps.setDate(2, record.getHarvestDate() != null
                    ? Date.valueOf(record.getHarvestDate()) : null);
            ps.setDouble(3, record.getExpectedYieldKg());

            // actual_yield_kg may be null (harvest not yet done)
            if (record.getActualYieldKg() != null) {
                ps.setDouble(4, record.getActualYieldKg());
            } else {
                ps.setNull(4, Types.DECIMAL);
            }

            // quality_grade may be null (grading not yet done)
            if (record.getQualityGrade() != null) {
                ps.setString(5, record.getQualityGrade());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            ps.setString(6, record.getRemarks());
            ps.setString(7, record.getRecordedBy());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving harvest record", e, e.getErrorCode());
        }
        throw new DatabaseException("Save failed: no generated key returned for harvest record.");
    }

    @Override
    public void update(HarvestRecord record) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, record.getSeasonId());
            ps.setDate(2, record.getHarvestDate() != null
                    ? Date.valueOf(record.getHarvestDate()) : null);
            ps.setDouble(3, record.getExpectedYieldKg());

            if (record.getActualYieldKg() != null) {
                ps.setDouble(4, record.getActualYieldKg());
            } else {
                ps.setNull(4, Types.DECIMAL);
            }

            if (record.getQualityGrade() != null) {
                ps.setString(5, record.getQualityGrade());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            ps.setString(6, record.getRemarks());
            ps.setString(7, record.getRecordedBy());
            ps.setInt(8, record.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error updating harvest record id: " + record.getId(), e, e.getErrorCode());
        }
    }

    @Override
    public void delete(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting harvest record id: " + id, e, e.getErrorCode());
        }
    }

    // ─── Private helper: maps one ResultSet row → HarvestRecord object ───
    private HarvestRecord mapRow(ResultSet rs) throws SQLException {
        java.sql.Date harvestDateSql = rs.getDate("harvest_date");

        // Read nullable DECIMAL column: safely convert Number / BigDecimal to Double
        Object actualYieldObj = rs.getObject("actual_yield_kg");
        Double actualYieldKg = (actualYieldObj instanceof Number num) ? num.doubleValue() : null;

        return new HarvestRecord(
                rs.getInt("harvest_id"),
                rs.getInt("season_id"),
                harvestDateSql != null ? harvestDateSql.toLocalDate() : null,
                rs.getDouble("expected_yield_kg"),
                actualYieldKg,                          // may be null
                rs.getString("quality_grade"),          // may be null
                rs.getString("remarks"),
                rs.getString("recorded_by")
        );
    }
}
