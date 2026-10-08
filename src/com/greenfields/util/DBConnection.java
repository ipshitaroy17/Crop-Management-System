package com.greenfields.util;

import com.greenfields.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection - Utility class for obtaining MySQL JDBC connections.
 *
 * DESIGN:
 *   - Reads credentials from db.properties (on the classpath).
 *   - Provides a single static getConnection() method used by all DAO classes.
 *   - Properties are loaded once when the class is first used (static initializer).
 *   - Callers are responsible for closing the Connection (via try-with-resources).
 *
 * WHY NOT SINGLETON / CONNECTION POOL?
 *   For this academic project, a simple DriverManager-based approach is
 *   appropriate and easy to explain. A production system would use a pool
 *   (e.g., Apache DBCP or HikariCP), but that is outside scope here.
 *
 * NOTE ON CLASS.FORNAME:
 *   MySQL Connector/J 8.x registers its driver automatically via Java's
 *   ServiceLoader mechanism, so Class.forName() is NOT required.
 *   It is mentioned in a comment below for faculty reference only.
 */
public class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;

    // ─── Static initializer — runs once when class is loaded ─────────────
    static {
        loadProperties();
    }

    // Private constructor — this class should never be instantiated
    private DBConnection() { }

    // ─── Load db.properties from classpath ───────────────────────────────
    private static void loadProperties() {
        Properties props = new Properties();

        // getResourceAsStream looks for db.properties in the same location
        // as the compiled .class files (i.e., the out/ or WEB-INF/classes folder)
        try (InputStream in = DBConnection.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (in == null) {
                throw new DatabaseException(
                        "Configuration file '" + PROPERTIES_FILE + "' not found in classpath. "
                        + "Make sure db.properties is copied to the output/classes directory.");
            }

            props.load(in);

            dbUrl      = props.getProperty("db.url");
            dbUsername = props.getProperty("db.username");
            dbPassword = props.getProperty("db.password");

            if (dbUrl == null || dbUsername == null || dbPassword == null) {
                throw new DatabaseException(
                        "One or more required properties (db.url, db.username, db.password) "
                        + "are missing in " + PROPERTIES_FILE);
            }

        } catch (IOException e) {
            throw new DatabaseException("Failed to read " + PROPERTIES_FILE, e);
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────

    /**
     * Returns a new JDBC Connection to greenfields_db.
     *
     * USAGE (always use try-with-resources so the connection is auto-closed):
     *
     *   try (Connection conn = DBConnection.getConnection()) {
     *       // use conn here
     *   }
     *
     * @return a live, open Connection object
     * @throws DatabaseException if the connection cannot be established
     */
    public static Connection getConnection() {
        // Note for faculty:
        // Class.forName("com.mysql.cj.jdbc.Driver") is NOT required for
        // MySQL Connector/J 8.x because the driver self-registers via ServiceLoader.
        // We rely on DriverManager to pick it up automatically.
        try {
            return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Cannot connect to database at: " + dbUrl
                    + " — check that MySQL is running and db.properties is correct.",
                    e,
                    e.getErrorCode());
        }
    }

    /**
     * Allows setting custom DB credentials (useful for unit/integration tests).
     */
    public static void setConfiguration(String url, String username, String password) {
        dbUrl = url;
        dbUsername = username;
        dbPassword = password;
    }

    /**
     * Resets DB credentials back to values from db.properties.
     */
    public static void resetConfiguration() {
        loadProperties();
    }

    /**
     * Quick test: attempts to open and immediately close a connection.
     * Useful for startup checks and the Phase 3 test class.
     *
     * @return true if connection succeeded, false if it failed
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (DatabaseException | SQLException e) {
            System.err.println("[DBConnection] Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
