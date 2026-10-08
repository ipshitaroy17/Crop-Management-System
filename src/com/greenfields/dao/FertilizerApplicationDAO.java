package com.greenfields.dao;

import com.greenfields.model.FertilizerApplication;

import java.util.List;

/**
 * FertilizerApplicationDAO - Data Access interface for the fertilizer_applications table.
 */
public interface FertilizerApplicationDAO {

    /** Returns all fertilizer records ordered by application date (newest first). */
    List<FertilizerApplication> findAll();

    /**
     * Returns fertilizer records for a specific season.
     * Primary lookup — used on the Fertilizer Management page.
     *
     * @param seasonId the season_id foreign key
     */
    List<FertilizerApplication> findBySeasonId(int seasonId);

    /**
     * Returns fertilizer records across ALL seasons of a specific crop.
     * Used on the Crop History page to show the full fertilizer history.
     * Internally JOINs fertilizer_applications with seasons on crop_id.
     *
     * @param cropId the crop_id to filter by
     */
    List<FertilizerApplication> findByCropId(int cropId);

    /**
     * Inserts a new fertilizer record and returns the generated fertilizer_id.
     *
     * @param fa FertilizerApplication object with seasonId and all fields set
     * @return the generated primary key
     */
    int save(FertilizerApplication fa);

    /**
     * Updates an existing fertilizer record.
     * Uses fa.getId() to identify which row to update.
     */
    void update(FertilizerApplication fa);

    /**
     * Deletes a fertilizer record by primary key.
     *
     * @param id the fertilizer_id to delete
     */
    void delete(int id);
}
