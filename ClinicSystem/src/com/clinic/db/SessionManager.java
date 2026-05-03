package com.clinic.db;

/**
 * SessionManager — Stores the logged-in user's info globally.
 * WHY: Every module needs to know WHO is logged in
 * for audit logging, role-based access, and personalization.
 */
public class SessionManager {

    private static int    currentUserId   = 0;
    private static String currentUsername = "";
    private static String currentFullName = "";
    private static String currentRole     = "";

    public static void login(int id, String username,
                             String fullName, String role) {
        currentUserId   = id;
        currentUsername = username;
        currentFullName = fullName;
        currentRole     = role;
    }

    public static void logout() {
        currentUserId   = 0;
        currentUsername = "";
        currentFullName = "";
        currentRole     = "";
    }

    public static int    getCurrentUserId()   { return currentUserId; }
    public static String getCurrentUsername() { return currentUsername; }
    public static String getCurrentFullName() { return currentFullName; }
    public static String getCurrentRole()     { return currentRole; }

    public static boolean isAdmin() {
        return "Admin".equalsIgnoreCase(currentRole);
    }
    public static boolean isDoctor() {
        return "Doctor".equalsIgnoreCase(currentRole);
    }
}