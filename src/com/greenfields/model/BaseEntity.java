package com.greenfields.model;

/**
 * BaseEntity - Abstract base class for all database-backed model classes.
 *
 * WHY IT EXISTS:
 *   Every table in greenfields_db has a primary key (INT AUTO_INCREMENT).
 *   Rather than repeating the 'id' field in all 6 model classes, we define
 *   it once here. This is genuine reuse, not forced inheritance.
 *
 * WHICH CLASSES EXTEND IT:
 *   User, Crop, Season, FertilizerApplication, IrrigationSchedule, HarvestRecord
 *
 * WHAT IT PROVIDES:
 *   - int id  → maps to the PK column of each table
 *   - getId() / setId()  → used by all DAO classes
 *   - abstract describe() → forces each subclass to provide a human-readable
 *     one-line description, useful for toString(), logging, and debugging.
 *
 * OOP CONCEPTS DEMONSTRATED:
 *   - Abstraction  : class is abstract; cannot be instantiated directly
 *   - Inheritance  : all model classes extend this
 *   - Polymorphism : describe() has different behavior in each subclass
 */
public abstract class BaseEntity {

    // Maps to the primary key column (e.g., crop_id, season_id) of each table
    private int id;

    // ─── Constructors ─────────────────────────────────────────

    /** Default constructor (used when creating a new record before DB insert) */
    public BaseEntity() {
    }

    /** Constructor with id (used when loading an existing record from DB) */
    public BaseEntity(int id) {
        this.id = id;
    }

    // ─── Getter / Setter ─────────────────────────────────────

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // ─── Abstract method ──────────────────────────────────────

    /**
     * Returns a one-line human-readable description of this entity.
     * Each subclass MUST implement this method (polymorphism).
     * Used in toString() and useful for logging/debugging.
     *
     * Example outputs:
     *   Crop         → "Paddy (IR-64) – Grain"
     *   Season       → "Kharif 2025 | Block A – North Field"
     *   HarvestRecord→ "Harvest on 2025-10-10 | Expected: 5000.0 kg"
     */
    public abstract String describe();

    // ─── toString ────────────────────────────────────────────

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + " #" + id + "] " + describe();
    }
}
