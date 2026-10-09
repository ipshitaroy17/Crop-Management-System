package com.greenfields.test;

import com.greenfields.util.PasswordHasher;

public final class TestPasswordHasher {

    public static void main(String[] args) {
        char[] password = "Unique test password 21!".toCharArray();
        String hash = PasswordHasher.hash(password);

        check(!hash.contains(new String(password)), "stored value does not contain the password");
        check(hash.length() <= 100, "hash fits the existing database column");
        check(PasswordHasher.isHashed(hash), "hash format is identified");
        check(PasswordHasher.matches(new String(password), hash), "correct password matches");
        check(!PasswordHasher.matches("incorrect", hash), "incorrect password does not match");
        check(PasswordHasher.matches("legacy-local", "legacy-local"), "legacy local credential is recognized");
        check(!PasswordHasher.matches("wrong", "legacy-local"), "incorrect legacy credential does not match");
        System.out.println("Password hashing tests passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
