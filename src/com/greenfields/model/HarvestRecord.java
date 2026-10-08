package com.greenfields.model;

import com.greenfields.interfaces.Reportable;

import java.time.LocalDate;

/**
 * HarvestRecord - Represents the harvest event and yield data for a season.
 *
 * DATABASE TABLE: harvest_records
 * EXTENDS: BaseEntity     → provides int id (maps to harvest_id)
 * IMPLEMENTS: Reportable  → can generate a yield summary for the reports page
 *
 * FIELDS:
 *   seasonId        → harvest_records.season_id         (FK → seasons)
 *   harvestDate     → harvest_records.harvest_date      (DATE)
 *   expectedYieldKg → harvest_records.expected_yield_kg (DECIMAL 10,2, NOT NULL)
 *   actualYieldKg   → harvest_records.actual_yield_kg   (DECIMAL 10,2, nullable)
 *   qualityGrade    → harvest_records.quality_grade     "A"/"B"/"C"/"reject" (nullable)
 *   remarks         → harvest_records.remarks           (TEXT)
 *   recordedBy      → harvest_records.recorded_by       (VARCHAR 100)
 *
 * NOTE ON YIELD FIELDS:
 *   - expectedYieldKg is always set when creating a harvest record (pre-harvest estimate).
 *   - actualYieldKg is initially null (harvest not yet done). It is updated after harvest.
 *   - Both fields live in the same table row → single-table expected-vs-actual query.
 *
 * MEANINGFUL METHODS:
 *   isHarvestComplete()          → checks if actualYieldKg has been recorded
 *   getYieldAchievementPercent() → (actual / expected) * 100, rounded to 1 decimal
 *   getYieldDifferenceKg()       → actual - expected (positive = surplus, negative = deficit)
 *   generateSummary()            → Reportable implementation for the reports page
 */
public class HarvestRecord extends BaseEntity implements Reportable {

    private int       seasonId;
    private LocalDate harvestDate;
    private double    expectedYieldKg;
    private Double    actualYieldKg;    // Double (object) to allow null (harvest pending)
    private String    qualityGrade;     // "A", "B", "C", "reject" — null if pending
    private String    remarks;
    private String    recordedBy;

    // ─── Constructors ─────────────────────────────────────────

    /** Default constructor */
    public HarvestRecord() {
    }

    /**
     * Full constructor — used when loading a harvest record from the database.
     *
     * @param actualYieldKg  pass null if harvest is not yet completed
     * @param qualityGrade   pass null if grading is not yet done
     */
    public HarvestRecord(int id, int seasonId, LocalDate harvestDate,
                         double expectedYieldKg, Double actualYieldKg,
                         String qualityGrade, String remarks, String recordedBy) {
        super(id);
        this.seasonId        = seasonId;
        this.harvestDate     = harvestDate;
        this.expectedYieldKg = expectedYieldKg;
        this.actualYieldKg   = actualYieldKg;
        this.qualityGrade    = qualityGrade;
        this.remarks         = remarks;
        this.recordedBy      = recordedBy;
    }

    // ─── Getters and Setters ──────────────────────────────────

    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }

    public LocalDate getHarvestDate() { return harvestDate; }
    public void setHarvestDate(LocalDate harvestDate) { this.harvestDate = harvestDate; }

    public double getExpectedYieldKg() { return expectedYieldKg; }
    public void setExpectedYieldKg(double expectedYieldKg) {
        this.expectedYieldKg = expectedYieldKg;
    }

    public Double getActualYieldKg() { return actualYieldKg; }
    public void setActualYieldKg(Double actualYieldKg) { this.actualYieldKg = actualYieldKg; }

    public String getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    // ─── Meaningful Methods ───────────────────────────────────

    /**
     * Returns true if the actual yield has been recorded (harvest is done).
     * The Dashboard uses this to distinguish pending vs completed harvests.
     */
    public boolean isHarvestComplete() {
        return actualYieldKg != null;
    }

    /**
     * Returns the yield achievement as a percentage of the expected yield.
     *
     * Formula: (actual / expected) * 100
     * Returns 0.0 if actual yield has not been recorded yet.
     *
     * Example:
     *   expected = 5000 kg, actual = 4750 kg → 95.0%
     *   expected = 2400 kg, actual = 2580 kg → 107.5% (exceeded expectation)
     *
     * Used on the Dashboard expected-vs-actual chart and the Harvest page.
     */
    public double getYieldAchievementPercent() {
        if (actualYieldKg == null || expectedYieldKg == 0) return 0.0;
        return Math.round((actualYieldKg / expectedYieldKg) * 1000.0) / 10.0;
    }

    /**
     * Returns the difference between actual and expected yield in kg.
     * Positive → surplus. Negative → deficit.
     * Returns 0.0 if harvest is not yet complete.
     *
     * Example: actual 4750 - expected 5000 = -250.0 kg
     */
    public double getYieldDifferenceKg() {
        if (actualYieldKg == null) return 0.0;
        return actualYieldKg - expectedYieldKg;
    }

    // ─── Reportable Implementation ────────────────────────────

    /**
     * Generates a formatted yield summary for the Seasonal Reports page.
     *
     * Example outputs:
     *   "Harvest: 2025-10-10 | Expected: 5000.0 kg | Actual: 4750.0 kg | Achievement: 95.0% | Grade: A"
     *   "Harvest: 2026-01-28 | Expected: 3600.0 kg | Actual: Pending"
     */
    @Override
    public String generateSummary() {
        if (!isHarvestComplete()) {
            return "Harvest: " + harvestDate
                    + " | Expected: " + expectedYieldKg + " kg"
                    + " | Actual: Pending";
        }
        return "Harvest: " + harvestDate
                + " | Expected: " + expectedYieldKg + " kg"
                + " | Actual: " + actualYieldKg + " kg"
                + " | Achievement: " + getYieldAchievementPercent() + "%"
                + " | Grade: " + qualityGrade;
    }

    // ─── describe() — required by BaseEntity ─────────────────

    @Override
    public String describe() {
        return "Harvest on " + harvestDate
                + " | Expected: " + expectedYieldKg + " kg";
    }
}
