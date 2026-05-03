package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsController {

    @FXML private TextField clinicNameField;
    @FXML private TextField taglineField;
    @FXML private TextArea  addressArea;
    @FXML private TextField contactField;
    @FXML private TextField emailField;
    @FXML private TextField doctorNameField;
    @FXML private TextField licenseField;
    @FXML private Label     statusLabel;
    @FXML private Label     dbStatusLabel;

    @FXML
    public void initialize() {
        loadSettings();
        checkDbStatus();
    }

    @FXML
    public void loadSettings() {
        // Safe query — only select columns we know exist
        String sql =
            "SELECT clinic_name, " +
            "COALESCE(address,''), " +
            "COALESCE(contact_number,''), " +
            "COALESCE(email,''), " +
            "COALESCE(doctor_name,''), " +
            "COALESCE(doctor_license,''), " +
            "COALESCE(tagline,'') " +
            "FROM clinic_settings LIMIT 1";

        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();

            if (rs.next()) {
                clinicNameField.setText(
                    rs.getString(1) != null
                        ? rs.getString(1) : "");
                addressArea.setText(rs.getString(2));
                contactField.setText(rs.getString(3));
                emailField.setText(rs.getString(4));
                doctorNameField.setText(rs.getString(5));
                licenseField.setText(rs.getString(6));
                taglineField.setText(rs.getString(7));
            } else {
                // No settings row yet — insert default
                insertDefaultSettings();
                loadSettings();
            }
            showStatus("", false);

        } catch (SQLException e) {
            showStatus("Error loading: " + e.getMessage(), true);
        }
    }

    private void insertDefaultSettings() {
        String sql =
            "INSERT INTO clinic_settings " +
            "(clinic_name, address, contact_number, " +
            "doctor_name, tagline) " +
            "VALUES (?,?,?,?,?)";
        try {
            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement(sql);
            s.setString(1, "Barangay Health Center");
            s.setString(2, "Barangay ___, Philippines");
            s.setString(3, "(000) 000-0000");
            s.setString(4, "Dr. ___");
            s.setString(5, "Serving our community with care");
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println(
                "Insert default settings error: " +
                e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        if (clinicNameField.getText().trim().isEmpty()) {
            showStatus("Clinic name is required.", true);
            return;
        }

        try {
            // Check if row exists
            ResultSet check = DatabaseManager.getConnection()
                .prepareStatement(
                    "SELECT COUNT(*) FROM clinic_settings")
                .executeQuery();

            boolean exists = check.next() &&
                check.getInt(1) > 0;

            String sql = exists
                ? "UPDATE clinic_settings SET " +
                  "clinic_name=?, address=?, " +
                  "contact_number=?, email=?, " +
                  "doctor_name=?, doctor_license=?, tagline=?"
                : "INSERT INTO clinic_settings " +
                  "(clinic_name, address, contact_number, " +
                  "email, doctor_name, doctor_license, tagline)" +
                  " VALUES (?,?,?,?,?,?,?)";

            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement(sql);
            s.setString(1, clinicNameField.getText().trim());
            s.setString(2, addressArea.getText().trim());
            s.setString(3, contactField.getText().trim());
            s.setString(4, emailField.getText().trim());
            s.setString(5, doctorNameField.getText().trim());
            s.setString(6, licenseField.getText().trim());
            s.setString(7, taglineField.getText().trim());
            s.executeUpdate();

            showStatus("✅ Settings saved successfully!", false);

        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    private void checkDbStatus() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn != null && !conn.isClosed()) {
                dbStatusLabel.setText(
                    "🟢 Database Connected");
                dbStatusLabel.setStyle(
                    "-fx-font-size:12px;" +
                    "-fx-text-fill:#059669;" +
                    "-fx-font-weight:bold;");
            }
        } catch (SQLException e) {
            dbStatusLabel.setText(
                "🔴 Database Disconnected");
            dbStatusLabel.setStyle(
                "-fx-font-size:12px;" +
                "-fx-text-fill:#DC2626;" +
                "-fx-font-weight:bold;");
        }
    }

    private void showStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill:" +
            (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size:13px;-fx-padding:8 12;" +
            "-fx-background-color:" +
            (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius:6;"
        );
    }
}