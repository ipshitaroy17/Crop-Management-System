package com.greenfields.test;

import com.greenfields.exception.DatabaseException;
import com.greenfields.util.DBConnection;

/** Verifies an unconfigured deployment fails through the normal DB error path. */
public final class TestDBConnectionMissingConfiguration {

    private TestDBConnectionMissingConfiguration() { }

    public static void main(String[] args) {
        try {
            DBConnection.getConnection();
            if (args.length > 0 && "--expect-env-config".equals(args[0])) {
                throw new AssertionError("Expected the test endpoint to reject its connection.");
            }
            throw new AssertionError("An unconfigured database unexpectedly connected.");
        } catch (DatabaseException e) {
            boolean environmentConfigured = args.length > 0 && "--expect-env-config".equals(args[0]);
            String message = e.getMessage();
            if (environmentConfigured) {
                if (message == null || !message.startsWith("Cannot connect to database at:")) {
                    throw new AssertionError("Environment variables were not used for DB configuration.", e);
                }
                System.out.println("MYSQL environment configuration was accepted without db.properties.");
                return;
            }
            if (message == null || !message.contains("Database configuration")) {
                throw new AssertionError("Expected a controlled configuration error.", e);
            }
            System.out.println("Missing DB configuration is reported without class initialization failure.");
        }
    }
}
