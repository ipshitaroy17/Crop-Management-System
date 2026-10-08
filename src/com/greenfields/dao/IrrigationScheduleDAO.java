package com.greenfields.dao;

import com.greenfields.model.IrrigationSchedule;

import java.util.List;

/**
 * IrrigationScheduleDAO - Data Access interface for the irrigation_schedules table.
 */
public interface IrrigationScheduleDAO {

    /** Returns all irrigation schedules ordered by scheduled date (newest first). */
    List<IrrigationSchedule> findAll();

    /**
     * Returns irrigation schedules for a specific season.
     * Primary lookup — used on the Irrigation Schedule page.
     *
     * @param seasonId the season_id foreign key
     */
    List<IrrigationSchedule> findBySeasonId(int seasonId);

    /**
     * Returns irrigation schedules across ALL seasons of a specific crop.
     * Used on the Crop History page.
     * Internally JOINs irrigation_schedules with seasons on crop_id.
     *
     * @param cropId the crop_id to filter by
     */
    List<IrrigationSchedule> findByCropId(int cropId);

    /**
     * Inserts a new irrigation schedule and returns the generated irrigation_id.
     *
     * @param schedule IrrigationSchedule object with seasonId and all fields set
     * @return the generated primary key
     */
    int save(IrrigationSchedule schedule);

    /**
     * Updates an existing irrigation schedule.
     * Uses schedule.getId() to identify which row to update.
     */
    void update(IrrigationSchedule schedule);

    /**
     * Deletes an irrigation schedule by primary key.
     *
     * @param id the irrigation_id to delete
     */
    void delete(int id);
}
