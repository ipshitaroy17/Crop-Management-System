package com.greenfields.interfaces;

/**
 * Reportable - Interface for model classes that can produce a summary report.
 *
 * WHY IT EXISTS:
 *   Both Season and HarvestRecord appear on the Seasonal Reports page.
 *   By implementing this interface, both classes guarantee they can produce
 *   a formatted summary string — useful in JSP pages and report generation.
 *
 * IMPLEMENTED BY: Season, HarvestRecord
 *
 * OOP CONCEPT: Interface — defines a contract without implementation.
 */
public interface Reportable {

    /**
     * Returns a formatted, human-readable summary of this entity.
     * Used on the seasonal reports page and dashboard recent activity.
     *
     * Example (Season):
     *   "Kharif 2025 | Paddy | Block A – North Field | 5.0 acres | Status: completed"
     *
     * Example (HarvestRecord):
     *   "Harvest: 2025-10-10 | Expected: 5000.0 kg | Actual: 4750.0 kg | Achievement: 95.0%"
     */
    String generateSummary();
}
