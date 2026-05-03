package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import com.clinic.db.SessionManager;
import com.clinic.db.AuditLogger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;

import org.mindrot.jbcrypt.BCrypt;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    // ✅ Keep this for Dashboard compatibility
    private static String loggedInUser;
    private static String loggedInRole;

    public static String getLoggedInUser() {
        return loggedInUser;
    }

    public static String getLoggedInRole() {
        return loggedInRole;
    }

    @FXML
    private void handleLogin() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter username and password.", true);
            return;
        }

        String sql = "SELECT * FROM users WHERE username = ?";

        try {
            PreparedStatement stmt = DatabaseManager.getConnection()
                    .prepareStatement(sql);
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                // ✅ FIXED: use correct column name
                String storedHash = rs.getString("password_hash");

                // ✅ BCrypt check (correct)
                if (BCrypt.checkpw(password, storedHash)) {

                    // ✅ Session system
                    SessionManager.login(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role")
                    );

                    // ✅ Old system (for Dashboard labels)
                    loggedInUser = rs.getString("full_name");
                    loggedInRole = rs.getString("role");

                    // ✅ Audit log (safe)
                    try {
                        AuditLogger.log(
                                "LOGIN",
                                "users",
                                rs.getInt("id"),
                                "User logged in successfully"
                        );
                    } catch (Exception e) {
                        System.out.println("Audit skipped: " + e.getMessage());
                    }

                    openDashboard();

                } else {
                    showStatus("Invalid password.", true);
                }

            } else {
                showStatus("User not found.", true);
            }

        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/clinic/fxml/Dashboard.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Clinic System Dashboard");

        } catch (Exception e) {
            showStatus("Failed to open dashboard: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(
                "-fx-text-fill: " + (isError ? "#DC2626" : "#059669") + ";"
        );
    }
}