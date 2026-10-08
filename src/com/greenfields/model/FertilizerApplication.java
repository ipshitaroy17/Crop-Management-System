package com.greenfields.model;

import java.time.LocalDate;

/**
 * FertilizerApplication - Represents one fertilizer application event
 * for a specific season.
 *
 * DATABASE TABLE: fertilizer_applications
 * EXTENDS: BaseEntity  → provides int id (maps to fertilizer_id)
 *
 * FIELDS:
 *   seasonId        → fertilizer_applications.season_id        (FK → seasons)
 *   fertilizerName  → fertilizer_applications.fertilizer_name  e.g. "Urea"
 *   fertilizerType  → fertilizer_applications.fertilizer_type  "chemical"/"organic"/"bio"
 *   quantityKg      → fertilizer_applications.quantity_kg      (DECIMAL 8,2)
 *   applicationDate → fertilizer_applications.application_date (DATE)
 *   appliedBy       → fertilizer_applications.applied_by       e.g. "Ravi Kumar"
 *   notes           → fertilizer_applications.notes            (TEXT)
 *
 * MEANINGFUL METHOD:
 *   isBioFertilizer()  → returns true if type is "bio" — useful for organic
 *                        farming reports and colour-coding in the UI.
 */
public class FertilizerApplication extends BaseEntity {

    private int       seasonId;
    private String    fertilizerName;
    private String    fertilizerType;    // "chemical", "organic", "bio"
    private double    quantityKg;
    private LocalDate applicationDate;
    private String    appliedBy;
    private String    notes;

    // ─── Constructors ─────────────────────────────────────────

    /** Default constructor */
    public FertilizerApplication() {
    }

    /**
     * Full constructor — used when loading a fertilizer record from the database.
     */
    public FertilizerApplication(int id, int seasonId, String fertilizerName,
                                 String fertilizerType, double quantityKg,
                                 LocalDate applicationDate, String appliedBy,
                                 String notes) {
        super(id);
        this.seasonId        = seasonId;
        this.fertilizerName  = fertilizerName;
        this.fertilizerType  = fertilizerType;
        this.quantityKg      = quantityKg;
        this.applicationDate = applicationDate;
        this.appliedBy       = appliedBy;
        this.notes           = notes;
    }

    // ─── Getters and Setters ──────────────────────────────────

    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }

    public String getFertilizerName() { return fertilizerName; }
    public void setFertilizerName(String fertilizerName) {
        this.fertilizerName = fertilizerName;
    }

    public String getFertilizerType() { return fertilizerType; }
    public void setFertilizerType(String fertilizerType) {
        this.fertilizerType = fertilizerType;
    }

    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }

    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public String getAppliedBy() { return appliedBy; }
    public void setAppliedBy(String appliedBy) { this.appliedBy = appliedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // ─── Meaningful Methods ───────────────────────────────────

    /**
     * Returns true if this fertilizer is of bio type.
     * Used in the Fertilizer list page to apply different styling to bio entries.
     */
    public boolean isBioFertilizer() {
        return "bio".equalsIgnoreCase(this.fertilizerType);
    }

    /**
     * Returns true if this fertilizer is organic or bio (non-chemical).
     * Useful for organic farming certification reports.
     */
    public boolean isNonChemical() {
        return "organic".equalsIgnoreCase(this.fertilizerType)
                || "bio".equalsIgnoreCase(this.fertilizerType);
    }

    // ─── describe() — required by BaseEntity ─────────────────

    @Override
    public String describe() {
        return fertilizerName + " (" + fertilizerType + ")"
                + " | " + quantityKg + " kg"
                + " on " + applicationDate;
    }
}
