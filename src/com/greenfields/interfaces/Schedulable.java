package com.greenfields.interfaces;

import java.time.LocalDate;

/**
 * Schedulable - Interface for model classes that represent a scheduled event.
 *
 * WHY IT EXISTS:
 *   IrrigationSchedule has a scheduled_date and a status (scheduled/completed/skipped).
 *   The Irrigation page needs to highlight overdue schedules and show status.
 *   This interface defines that contract cleanly.
 *
 * IMPLEMENTED BY: IrrigationSchedule
 *
 * OOP CONCEPT: Interface — defines behaviour that a class must guarantee.
 */
public interface Schedulable {

    /**
     * Returns the planned date for this scheduled event.
     * Maps to: irrigation_schedules.scheduled_date
     */
    LocalDate getScheduledDate();

    /**
     * Returns the current status of this scheduled event.
     * Expected values: "scheduled", "completed", "skipped"
     */
    String getScheduleStatus();

    /**
     * Returns true if the scheduled date has passed AND the event is still
     * marked as "scheduled" (i.e., it was not completed or skipped on time).
     * Useful for the dashboard "overdue irrigation" alert.
     */
    boolean isOverdue();
}
