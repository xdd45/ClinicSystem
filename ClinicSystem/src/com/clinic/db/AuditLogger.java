package com.clinic.db;

import java.sql.*;

/**
 * AuditLogger — Records every important action in the system.
 * WHY: For a sellable system, you MUST know who did what and when.
 * This protects the clinic from disputes and unauthorized changes.
 */
public class AuditLogger {

    public static void log(String action, String tableName,
                           int recordId, String details) {
        // Run in background so it doesn't slow down the UI
        new Thread(() -> {
            String sql = "INSERT INTO audit_logs " +
                "(user_id, action, table_name, record_id, details) " +
                "VALUES (?,?,?,?,?)";
            try {
                PreparedStatement s = DatabaseManager.getConnection()
                    .prepareStatement(sql);
                s.setInt(1, SessionManager.getCurrentUserId());
                s.setString(2, action);
                s.setString(3, tableName);
                s.setInt(4, recordId);
                s.setString(5, details);
                s.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Audit log error: " + e.getMessage());
            }
        }).start();
    }
}