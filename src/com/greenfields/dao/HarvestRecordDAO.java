package com.greenfields.dao;

import com.greenfields.model.HarvestRecord;

import java.util.List;

/**
 * HarvestRecordDAO - Data Access interface for the harvest_records table.
 */
public interface HarvestRecordDAO {

    /** Returns all harvest records ordered by harvest date (newest first). */
    List<HarvestRecord> findAll();

    /**
     * Returns harvest records for a specific season.
     * Primary lookup — used on the Harvest & Yield page.
     *
     * @param seasonId the season_id foreign key
     */
    List<HarvestRecord> findBySeasonId(int seasonId);

    /**
     * Returns harvest records across ALL seasons of a specific crop.
     * Used on the Crop History and Seasonal Reports pages.
     * Internally JOINs harvest_records with seasons on crop_id.
     *
     * @param cropId the crop_id to filter by
     */
    List<HarvestRecord> findByCropId(int cropId);

    /**
     * Returns a single harvest record by its primary key.
     * Returns null if not found.
     */
    HarvestRecord findById(int id);

    /**
     * Inserts a new harvest record and returns the generated harvest_id.
     * actualYieldKg may be null at the time of insert (harvest not yet done).
     *
     * @param record HarvestRecord with seasonId and expectedYieldKg set
     * @return the generated primary key
     */
    int save(HarvestRecord record);

    /**
     * Updates an existing harvest record.
     * Used to record the actual yield after harvest is complete.
     * Uses record.getId() to identify which row to update.
     */
    void update(HarvestRecord record);
}
