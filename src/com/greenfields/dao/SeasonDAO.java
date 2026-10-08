package com.greenfields.dao;

import com.greenfields.model.Season;

import java.util.List;

/**
 * SeasonDAO - Data Access interface for the seasons table.
 */
public interface SeasonDAO {

    /** Returns all seasons ordered by planting date (newest first). */
    List<Season> findAll();

    /**
     * Returns a single season by its primary key.
     * Returns null if not found.
     */
    Season findById(int id);

    /**
     * Returns all seasons that belong to a specific crop.
     * Used when the user clicks on a crop to view its history.
     *
     * @param cropId the crop_id foreign key
     */
    List<Season> findByCropId(int cropId);

    /**
     * Inserts a new season and returns the generated season_id.
     *
     * @param season Season object with all fields set except id
     * @return the generated primary key (season_id)
     */
    int save(Season season);

    /**
     * Updates an existing season record.
     * Uses season.getId() to identify which row to update.
     */
    void update(Season season);
}
