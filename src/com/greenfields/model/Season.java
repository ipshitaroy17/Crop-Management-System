package com.greenfields.model;

import com.greenfields.interfaces.Reportable;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Season - Represents one growing season for a specific crop on the farm.
 *
 * DATABASE TABLE: seasons
 * EXTENDS: BaseEntity     → provides int id (maps to season_id)
 * IMPLEMENTS: Reportable  → can generate a text summary for the reports page
 *
 * FIELDS:
 *   cropId               → seasons.crop_id              (FK → crops.crop_id)
 *   seasonName           → seasons.season_name          e.g. "Kharif 2025"
 *   fieldLocation        → seasons.field_location        e.g. "Block A – North Field"
 *   areaAcres            → seasons.area_acres           (DECIMAL 8,2)
 *   plantingDate         → seasons.planting_date        (DATE)
 *   expectedHarvestDate  → seasons.expected_harvest_date(DATE)
 *   seasonStatus         → seasons.season_status         e.g. "active", "completed"
 *   notes                → seasons.notes                (TEXT)
 *
 * NOTE: cropId stores the FK integer. The actual Crop object is loaded separately
 *       by the DAO when needed (to keep the model layer simple).
 *
 * MEANINGFUL METHODS:
 *   isActive()             → checks if season_status is 'active'
 *   getDaysUntilHarvest()  → computes days remaining from today to expected harvest
 *   generateSummary()      → Reportable implementation for report page
 */
public class Season extends BaseEntity implements Reportable {

    private int       cropId;
    private String    seasonName;
    private String    fieldLocation;
    private double    areaAcres;
    private LocalDate plantingDate;
    private LocalDate expectedHarvestDate;
    private String    seasonStatus;    // "planned", "active", "completed", "failed"
    private String    notes;

    // ─── Constructors ─────────────────────────────────────────

    /** Default constructor */
    public Season() {
    }

    /**
     * Full constructor — used when loading a season record from the database.
     */
    public Season(int id, int cropId, String seasonName, String fieldLocation,
                  double areaAcres, LocalDate plantingDate,
                  LocalDate expectedHarvestDate, String seasonStatus, String notes) {
        super(id);
        this.cropId              = cropId;
        this.seasonName          = seasonName;
        this.fieldLocation       = fieldLocation;
        this.areaAcres           = areaAcres;
        this.plantingDate        = plantingDate;
        this.expectedHarvestDate = expectedHarvestDate;
        this.seasonStatus        = seasonStatus;
        this.notes               = notes;
    }

    // ─── Getters and Setters ──────────────────────────────────

    public int getCropId() { return cropId; }
    public void setCropId(int cropId) { this.cropId = cropId; }

    public String getSeasonName() { return seasonName; }
    public void setSeasonName(String seasonName) { this.seasonName = seasonName; }

    public String getFieldLocation() { return fieldLocation; }
    public void setFieldLocation(String fieldLocation) { this.fieldLocation = fieldLocation; }

    public double getAreaAcres() { return areaAcres; }
    public void setAreaAcres(double areaAcres) { this.areaAcres = areaAcres; }

    public LocalDate getPlantingDate() { return plantingDate; }
    public void setPlantingDate(LocalDate plantingDate) { this.plantingDate = plantingDate; }

    public LocalDate getExpectedHarvestDate() { return expectedHarvestDate; }
    public void setExpectedHarvestDate(LocalDate expectedHarvestDate) {
        this.expectedHarvestDate = expectedHarvestDate;
    }

    public String getSeasonStatus() { return seasonStatus; }
    public void setSeasonStatus(String seasonStatus) { this.seasonStatus = seasonStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // ─── Meaningful Methods ───────────────────────────────────

    /**
     * Returns true if this season is currently active.
     * Used on the Dashboard to count active seasons.
     */
    public boolean isActive() {
        return "active".equalsIgnoreCase(this.seasonStatus);
    }

    /**
     * Returns the number of days remaining from today until the expected harvest date.
     * Returns a negative number if the harvest date has passed.
     * Returns 0 if expectedHarvestDate is null.
     *
     * Used on the Season details page to show a countdown.
     */
    public long getDaysUntilHarvest() {
        if (expectedHarvestDate == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), expectedHarvestDate);
    }

    // ─── Reportable implementation ────────────────────────────

    /**
     * Generates a summary string for the seasonal reports page.
     * Example output:
     *   "Kharif 2025 | Block A – North Field | 5.0 acres | Status: completed"
     */
    @Override
    public String generateSummary() {
        return seasonName
                + " | " + fieldLocation
                + " | " + areaAcres + " acres"
                + " | Status: " + seasonStatus;
    }

    // ─── describe() — required by BaseEntity ─────────────────

    @Override
    public String describe() {
        return seasonName + " | " + fieldLocation;
    }
}
