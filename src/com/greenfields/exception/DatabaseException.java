package com.greenfields.exception;

/**
 * DatabaseException - Custom unchecked exception for JDBC / database errors.
 *
 * WHY IT EXISTS:
 *   Java's SQLException is a checked exception. Every DAO method that uses JDBC
 *   would normally need to declare "throws SQLException" in its signature.
 *   By wrapping it in this RuntimeException subclass, DAO methods stay clean
 *   and callers (Servlets) can handle database errors in one place.
 *
 * OOP CONCEPT: Exception handling — custom exception hierarchy.
 *
 * USAGE IN VIVA:
 *   "We wrap SQLException inside DatabaseException so that our Servlet layer
 *    does not need to know about low-level JDBC details. This is encapsulation
 *    applied to error handling."
 */
public class DatabaseException extends RuntimeException {

    // Stores the original SQL error code (0 if not from a SQLException)
    private final int sqlErrorCode;

    // ─── Constructors ─────────────────────────────────────────

    /** Wrap a message only */
    public DatabaseException(String message) {
        super(message);
        this.sqlErrorCode = 0;
    }

    /** Wrap a message and the original cause (e.g., a SQLException) */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.sqlErrorCode = 0;
    }

    /** Wrap a message, original cause, and the SQL vendor error code */
    public DatabaseException(String message, Throwable cause, int sqlErrorCode) {
        super(message, cause);
        this.sqlErrorCode = sqlErrorCode;
    }

    // ─── Getter ───────────────────────────────────────────────

    public int getSqlErrorCode() {
        return sqlErrorCode;
    }
}
