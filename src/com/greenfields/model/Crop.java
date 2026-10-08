package com.greenfields.model;

/**
 * Crop - Represents a crop grown on Greenfields Agri Farm.
 *
 * DATABASE TABLE: crops
 * EXTENDS: BaseEntity  → provides int id (maps to crop_id)
 *
 * FIELDS:
 *   cropName           → crops.crop_name            (VARCHAR 100) e.g. Paddy
 *   cropType           → crops.crop_type             (VARCHAR 50)  e.g. Grain, Vegetable
 *   variety            → crops.variety               (VARCHAR 100) e.g. IR-64
 *   description        → crops.description           (TEXT)
 *   growthDurationDays → crops.growth_duration_days  (INT)
 *   status             → crops.status                (ENUM: 'active'/'inactive')
 *
 * MEANINGFUL METHODS:
 *   isActive()  → used in Crop Management page to filter active crops
 *   getLabel()  → returns "Paddy (IR-64)" for use in dropdowns and reports
 */
public class Crop extends BaseEntity {

    private String cropName;
    private String cropType;
    private String variety;
    private String description;
    private int    growthDurationDays;
    private String status;          // "active" or "inactive"

    // ─── Constructors ─────────────────────────────────────────

    /** Default constructor */
    public Crop() {
    }

    /**
     * Full constructor — used when loading a crop record from the database.
     *
     * @param id                 crop_id from DB
     * @param cropName           name of the crop (Paddy, Maize, Tomato)
     * @param cropType           type category (Grain, Vegetable)
     * @param variety            specific variety name
     * @param description        detailed description
     * @param growthDurationDays days from planting to harvest
     * @param status             "active" or "inactive"
     */
    public Crop(int id, String cropName, String cropType, String variety,
                String description, int growthDurationDays, String status) {
        super(id);
        this.cropName           = cropName;
        this.cropType           = cropType;
        this.variety            = variety;
        this.description        = description;
        this.growthDurationDays = growthDurationDays;
        this.status             = status;
    }

    // ─── Getters and Setters ──────────────────────────────────

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getGrowthDurationDays() { return growthDurationDays; }
    public void setGrowthDurationDays(int growthDurationDays) {
        this.growthDurationDays = growthDurationDays;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // ─── Meaningful Methods ───────────────────────────────────

    /**
     * Returns true if this crop is currently active.
     * Used to filter the dropdown list when creating a new season.
     */
    public boolean isActive() {
        return "active".equalsIgnoreCase(this.status);
    }

    /**
     * Returns a display label combining crop name and variety.
     * e.g. "Paddy (IR-64)" — used in dropdowns and report headings.
     */
    public String getLabel() {
        if (variety != null && !variety.isBlank()) {
            return cropName + " (" + variety + ")";
        }
        return cropName;
    }

    // ─── describe() — required by BaseEntity ─────────────────

    @Override
    public String describe() {
        return getLabel() + " – " + cropType
                + " | Growth: " + growthDurationDays + " days";
    }
}
