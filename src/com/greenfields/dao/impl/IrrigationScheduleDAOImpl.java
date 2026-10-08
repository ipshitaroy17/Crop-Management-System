package com.greenfields.dao.impl;

import com.greenfields.dao.IrrigationScheduleDAO;
import com.greenfields.exception.DatabaseException;
import com.greenfields.model.IrrigationSchedule;
import com.greenfields.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * IrrigationScheduleDAOImpl - JDBC implementation of IrrigationScheduleDAO.
 * CRUD operations against the irrigation_schedules table.
 */
public class IrrigationScheduleDAOImpl implements IrrigationScheduleDAO {

    // ─── SQL Queries ──────────────────────────────────────────────────────
    private static final String COLUMNS =
            "irrigation_id, season_id, scheduled_date, actual_date, method, "
            + "water_volume_litres, status, notes";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLUMNS + " FROM irrigation_schedules "
            + "ORDER BY scheduled_date DESC";

    private static final String SQL_FIND_BY_SEASON =
            "SELECT " + COLUMNS + " FROM irrigation_schedules "
            + "WHERE season_id = ? ORDER BY scheduled_date";

    // JOIN through seasons to filter by crop
    private static final String SQL_FIND_BY_CROP =
            "SELECT irr.irrigation_id, irr.season_id, irr.scheduled_date, irr.actual_date, "
            + "irr.method, irr.water_volume_litres, irr.status, irr.notes "
            + "FROM irrigation_schedules irr "
            + "INNER JOIN seasons s ON irr.season_id = s.season_id "
            + "WHERE s.crop_id = ? ORDER BY irr.scheduled_date DESC";

    private static final String SQL_INSERT =
            "INSERT INTO irrigation_schedules "
            + "(season_id, scheduled_date, actual_date, method, water_volume_litres, status, notes) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE irrigation_schedules SET season_id=?, scheduled_date=?, actual_date=?, "
            + "method=?, water_volume_litres=?, status=?, notes=? "
            + "WHERE irrigation_id=?";

    private static final String SQL_DELETE =
            "DELETE FROM irrigation_schedules WHERE irrigation_id=?";

    // ─── Public DAO Methods ───────────────────────────────────────────────

    @Override
    public List<IrrigationSchedule> findAll() {
        List<IrrigationSchedule> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all irrigation schedules", e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public List<IrrigationSchedule> findBySeasonId(int seasonId) {
        List<IrrigationSchedule> list = new ArrayList<>();

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
                    "Error finding irrigation schedules for season_id: " + seasonId, e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public List<IrrigationSchedule> findByCropId(int cropId) {
        List<IrrigationSchedule> list = new ArrayList<>();

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
                    "Error finding irrigation schedules for crop_id: " + cropId, e, e.getErrorCode());
        }
        return list;
    }

    @Override
    public int save(IrrigationSchedule schedule) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, schedule.getSeasonId());
            ps.setDate(2, schedule.getScheduledDate() != null
                    ? Date.valueOf(schedule.getScheduledDate()) : null);

            // actual_date is nullable (null when event is not yet completed)
            if (schedule.getActualDate() != null) {
                ps.setDate(3, Date.valueOf(schedule.getActualDate()));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setString(4, schedule.getMethod());
            ps.setDouble(5, schedule.getWaterVolumeLitres());
            ps.setString(6, schedule.getScheduleStatus());
            ps.setString(7, schedule.getNotes());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving irrigation schedule", e, e.getErrorCode());
        }
        throw new DatabaseException("Save failed: no generated key returned for irrigation schedule.");
    }

    @Override
    public void update(IrrigationSchedule schedule) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, schedule.getSeasonId());
            ps.setDate(2, schedule.getScheduledDate() != null
                    ? Date.valueOf(schedule.getScheduledDate()) : null);

            if (schedule.getActualDate() != null) {
                ps.setDate(3, Date.valueOf(schedule.getActualDate()));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setString(4, schedule.getMethod());
            ps.setDouble(5, schedule.getWaterVolumeLitres());
            ps.setString(6, schedule.getScheduleStatus());
            ps.setString(7, schedule.getNotes());
            ps.setInt(8, schedule.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error updating irrigation schedule id: " + schedule.getId(), e, e.getErrorCode());
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
                    "Error deleting irrigation schedule id: " + id, e, e.getErrorCode());
        }
    }

    // ─── Private helper: maps one ResultSet row → IrrigationSchedule ─────
    private IrrigationSchedule mapRow(ResultSet rs) throws SQLException {
        java.sql.Date scheduledDateSql = rs.getDate("scheduled_date");
        java.sql.Date actualDateSql    = rs.getDate("actual_date");

        return new IrrigationSchedule(
                rs.getInt("irrigation_id"),
                rs.getInt("season_id"),
                scheduledDateSql != null ? scheduledDateSql.toLocalDate() : null,
                actualDateSql    != null ? actualDateSql.toLocalDate()    : null,
                rs.getString("method"),
                rs.getDouble("water_volume_litres"),
                rs.getString("status"),
                rs.getString("notes")
        );
    }
}
