package com.greenfields.model;

import com.greenfields.interfaces.Schedulable;

import java.time.LocalDate;

/**
 * IrrigationSchedule - Represents one planned or completed irrigation event
 * for a specific season.
 *
 * DATABASE TABLE: irrigation_schedules
 * EXTENDS: BaseEntity     → provides int id (maps to irrigation_id)
 * IMPLEMENTS: Schedulable → provides isOverdue(), getScheduleStatus(), getScheduledDate()
 *
 * FIELDS:
 *   seasonId           → irrigation_schedules.season_id            (FK → seasons)
 *   scheduledDate      → irrigation_schedules.scheduled_date       (DATE)
 *   actualDate         → irrigation_schedules.actual_date          (DATE, nullable)
 *   method             → irrigation_schedules.method               "drip"/"sprinkler"/"flood"/"manual"
 *   waterVolumeLitres  → irrigation_schedules.water_volume_litres  (DECIMAL 10,2)
 *   status             → irrigation_schedules.status               "scheduled"/"completed"/"skipped"
 *   notes              → irrigation_schedules.notes                (TEXT)
 *
 * INTERFACE METHODS:
 *   isOverdue()        → true if scheduled date is in the past AND status is still "scheduled"
 *   getScheduleStatus()→ returns the status field value
 *   getScheduledDate() → returns scheduledDate (required by Schedulable interface)
 *
 * MEANINGFUL METHODS:
 *   isCompleted()  → quick check for the irrigation list page
 *   getMethodLabel() → returns a user-friendly label for the irrigation method
 */
public class IrrigationSchedule extends BaseEntity implements Schedulable {

    private int       seasonId;
    private LocalDate scheduledDate;
    private LocalDate actualDate;       // null until the event is completed
    private String    method;           // "drip", "sprinkler", "flood", "manual"
    private double    waterVolumeLitres;
    private String    status;           // "scheduled", "completed", "skipped"
    private String    notes;

    // ─── Constructors ─────────────────────────────────────────

    /** Default constructor */
    public IrrigationSchedule() {
    }

    /**
     * Full constructor — used when loading an irrigation record from the database.
     */
    public IrrigationSchedule(int id, int seasonId, LocalDate scheduledDate,
                               LocalDate actualDate, String method,
                               double waterVolumeLitres, String status,
                               String notes) {
        super(id);
        this.seasonId          = seasonId;
        this.scheduledDate     = scheduledDate;
        this.actualDate        = actualDate;
        this.method            = method;
        this.waterVolumeLitres = waterVolumeLitres;
        this.status            = status;
        this.notes             = notes;
    }

    // ─── Getters and Setters ──────────────────────────────────

    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }

    public LocalDate getActualDate() { return actualDate; }
    public void setActualDate(LocalDate actualDate) { this.actualDate = actualDate; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public double getWaterVolumeLitres() { return waterVolumeLitres; }
    public void setWaterVolumeLitres(double waterVolumeLitres) {
        this.waterVolumeLitres = waterVolumeLitres;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Setter for status (Schedulable provides the getter via getScheduleStatus())
    public void setStatus(String status) { this.status = status; }

    // Setter for scheduledDate (Schedulable provides the getter)
    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    // ─── Schedulable Interface Implementation ─────────────────

    /**
     * Returns the planned date of this irrigation event.
     * Required by the Schedulable interface.
     */
    @Override
    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    /**
     * Returns the current status of this irrigation event.
     * Required by the Schedulable interface.
     * Possible values: "scheduled", "completed", "skipped"
     */
    @Override
    public String getScheduleStatus() {
        return status;
    }

    /**
     * Returns true if this irrigation event is OVERDUE.
     * Definition: the scheduled date is before today AND status is still "scheduled"
     * (meaning it was never completed or skipped on time).
     *
     * Required by the Schedulable interface.
     * Used on the Dashboard to show an overdue irrigation alert.
     */
    @Override
    public boolean isOverdue() {
        return scheduledDate != null
                && scheduledDate.isBefore(LocalDate.now())
                && "scheduled".equalsIgnoreCase(status);
    }

    // ─── Meaningful Methods ───────────────────────────────────

    /**
     * Returns true if this irrigation event has been completed.
     * Shortcut used in JSP to apply green colour-coding.
     */
    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    /**
     * Returns a capitalised, user-friendly label for the irrigation method.
     * e.g. "drip" → "Drip Irrigation"
     */
    public String getMethodLabel() {
        if (method == null) return "Unknown";
        return switch (method.toLowerCase()) {
            case "drip"      -> "Drip Irrigation";
            case "sprinkler" -> "Sprinkler Irrigation";
            case "flood"     -> "Flood Irrigation";
            case "manual"    -> "Manual Watering";
            default          -> method;
        };
    }

    // ─── describe() — required by BaseEntity ─────────────────

    @Override
    public String describe() {
        return getMethodLabel()
                + " on " + scheduledDate
                + " | " + waterVolumeLitres + " L"
                + " | Status: " + status;
    }
}
