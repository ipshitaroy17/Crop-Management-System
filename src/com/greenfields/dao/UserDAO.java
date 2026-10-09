package com.greenfields.dao;

import com.greenfields.model.User;

/**
 * UserDAO - Data Access interface for the users table.
 *
 * Only the read operations needed for session-based login are defined here.
 * User management (add/edit/delete) is out of scope for this academic project.
 */
public interface UserDAO {

    /**
     * Find a user by their username (used during login).
     * Returns null if no user with that username exists.
     *
     * @param username the login username to search for
     * @return matching User object, or null
     */
    User findByUsername(String username);

    /**
     * Find a user by their primary key.
     * Used to reload session user data after login.
     *
     * @param id the user_id primary key
     * @return matching User object, or null
     */
    User findById(int id);

    /** Replace a legacy password value with its hashed form after successful login. */
    void updatePassword(int id, String password);
}
