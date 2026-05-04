package com.clinic.db;

public class SessionManager {

    private static int    currentUserId   = 0;
    private static String currentUsername = "";
    private static String currentFullName = "";
    private static String currentRole     = "";
    private static int    currentPatientId = 0;

    public static void login(int id, String username,
                             String fullName,
                             String role) {
        currentUserId   = id;
        currentUsername = username;
        currentFullName = fullName;
        currentRole     = role;
    }

    public static void logout() {
        currentUserId    = 0;
        currentUsername  = "";
        currentFullName  = "";
        currentRole      = "";
        currentPatientId = 0;
    }

    public static int    getCurrentUserId()   {
        return currentUserId;
    }
    public static String getCurrentUsername() {
        return currentUsername;
    }
    public static String getCurrentFullName() {
        return currentFullName;
    }
    public static String getCurrentRole() {
        return currentRole;
    }
    public static int getPatientId() {
        return currentPatientId;
    }
    public static void setPatientId(int id) {
        currentPatientId = id;
    }

    public static boolean isAdmin() {
        return "Admin".equalsIgnoreCase(currentRole);
    }
    public static boolean isDoctor() {
        return "Doctor".equalsIgnoreCase(currentRole);
    }
    public static boolean isNurse() {
        return "Nurse".equalsIgnoreCase(currentRole);
    }
    public static boolean isPatient() {
        return "Patient".equalsIgnoreCase(currentRole);
    }
}