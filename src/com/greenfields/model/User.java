package com.greenfields.model;

import java.time.LocalDateTime;

/**
 * User - Represents a login user of the Greenfields system.
 *
 * DATABASE TABLE: users
 * EXTENDS: BaseEntity  → provides int id (maps to user_id)
 *
 * FIELDS:
 *   username   → users.username   (VARCHAR 50, UNIQUE)
 *   password   → users.password   (VARCHAR 100)
 *   fullName   → users.full_name  (VARCHAR 100)
 *   role       → users.role       (ENUM: 'admin' or 'viewer')
 *   createdAt  → users.created_at (TIMESTAMP)
 *
 * NOTE: Password is stored as plain text in the demo (matches the SQL script).
 *       In a production system this would be a hashed value.
 *
 * MEANINGFUL METHOD:
 *   isAdmin() → convenience check used in Servlets for access control.
 */
public class User extends BaseEntity {

    private String username;
    private String password;
    private String fullName;
    private String role;           // "admin" or "viewer"
    private LocalDateTime createdAt;

    // ─── Constructors ─────────────────────────────────────────

    /** Default constructor */
    public User() {
    }

    /**
     * Full constructor — used when loading a user record from the database.
     *
     * @param id        user_id from DB
     * @param username  login username
     * @param password  password (plain text for demo)
     * @param fullName  display name
     * @param role      "admin" or "viewer"
     * @param createdAt account creation timestamp
     */
    public User(int id, String username, String password,
                String fullName, String role, LocalDateTime createdAt) {
        super(id);
        this.username  = username;
        this.password  = password;
        this.fullName  = fullName;
        this.role      = role;
        this.createdAt = createdAt;
    }

    // ─── Getters and Setters ──────────────────────────────────

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ─── Meaningful Method ────────────────────────────────────

    /**
     * Returns true if this user has the 'admin' role.
     * Used in Servlets to control access to add/edit/delete operations.
     */
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(this.role);
    }

    // ─── describe() — required by BaseEntity ─────────────────

    @Override
    public String describe() {
        return fullName + " (" + role + ")";
    }
}
