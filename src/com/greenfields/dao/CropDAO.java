package com.greenfields.dao;

import com.greenfields.model.Crop;

import java.util.List;

/**
 * CropDAO - Data Access interface for the crops table.
 * Full CRUD operations for the Crop Management module.
 */
public interface CropDAO {

    /**
     * Returns all crops ordered by crop name.
     * Used in the Crop Management list page and season dropdowns.
     */
    List<Crop> findAll();

    /**
     * Returns a single crop by its primary key.
     * Returns null if not found.
     */
    Crop findById(int id);

    /**
     * Inserts a new crop record and returns the generated crop_id.
     *
     * @param crop a Crop object with all fields populated except id
     * @return the generated primary key (crop_id) assigned by MySQL
     */
    int save(Crop crop);

    /**
     * Updates an existing crop record.
     * Uses crop.getId() to identify which row to update.
     *
     * @param crop a Crop object with id and all updated fields
     */
    void update(Crop crop);

    /**
     * Deletes a crop record by primary key.
     * Note: will fail with a DatabaseException if seasons reference this crop
     * (FK constraint: ON DELETE RESTRICT).
     *
     * @param id the crop_id to delete
     */
    void delete(int id);
}
