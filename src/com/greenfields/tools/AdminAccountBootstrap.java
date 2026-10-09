package com.greenfields.tools;

import com.greenfields.util.DBConnection;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

/** One-time interactive administrator creation for a newly initialized database. */
public final class AdminAccountBootstrap {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,50}");

    private AdminAccountBootstrap() { }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Run this command in an interactive terminal so password input stays hidden.");
            System.exit(2);
        }

        int exitCode = run(console);
        if (exitCode != 0) System.exit(exitCode);
    }

    private static int run(Console console) {
        char[] firstPassword = null;
        char[] confirmation = null;
        try {
            String username = console.readLine("New administrator username: ");
            if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
                System.err.println("Username must be 3-50 characters: letters, numbers, dot, underscore, or hyphen.");
                return 2;
            }

            String fullName = console.readLine("Administrator full name: ");
            if (fullName == null || fullName.isBlank() || fullName.length() > 100) {
                System.err.println("A full name of 1-100 characters is required.");
                return 2;
            }

            firstPassword = console.readPassword("Unique password (at least 16 characters): ");
            confirmation = console.readPassword("Confirm password: ");
            if (firstPassword == null || confirmation == null
                    || firstPassword.length < 16 || firstPassword.length > 100
                    || !Arrays.equals(firstPassword, confirmation)) {
                System.err.println("Passwords must match and contain 16-100 characters.");
                return 2;
            }

            createAccount(username, fullName, new String(firstPassword));
            console.printf("Administrator account created.%n");
            return 0;
        } catch (SQLException e) {
            System.err.println("Administrator account was not created. Verify the database and schema; details were not logged.");
            return 1;
        } catch (RuntimeException e) {
            System.err.println("Administrator account was not created. Verify the database configuration; details were not logged.");
            return 1;
        } finally {
            if (firstPassword != null) Arrays.fill(firstPassword, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }

    private static void createAccount(String username, String fullName, String password)
            throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lookup = connection.prepareStatement(
                        "SELECT 1 FROM users WHERE username = ?")) {
                    lookup.setString(1, username);
                    try (ResultSet result = lookup.executeQuery()) {
                        if (result.next()) {
                            connection.rollback();
                            throw new SQLException("Username already exists.");
                        }
                    }
                }

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, 'admin')")) {
                    insert.setString(1, username);
                    insert.setString(2, password);
                    insert.setString(3, fullName);
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
