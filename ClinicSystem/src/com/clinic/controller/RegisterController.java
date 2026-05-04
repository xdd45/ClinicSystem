package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private TextField     roleField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         statusLabel;
    @FXML private Label         usernameStatus;
    @FXML private Label         roleTitle;
    @FXML private Label         roleSubLabel;
    @FXML private Label         roleIcon;
    @FXML private Circle        roleCircle;
    @FXML private VBox          roleInfoBox;
    @FXML private Button        registerButton;

    @FXML
    public void initialize() {
        setupRoleUI();
        setupUsernameChecker();
    }

    private void setupRoleUI() {
        String role = LoginController.getSelectedRole();
        roleField.setText(role);

        switch (role) {
            case "Admin" -> {
                roleIcon.setText("👑");
                roleCircle.setFill(
                    Color.web("#1D4ED8"));
                roleTitle.setText(
                    "Admin Registration");
                roleSubLabel.setText(
                    "Full system access account");
                registerButton.setStyle(
                    registerButton.getStyle()
                        .replace("#1D4ED8", "#1D4ED8"));
                addInfo("✓ Full system access");
                addInfo("✓ Manage all users");
                addInfo("✓ View all reports");
                addInfo("✓ Configure settings");
            }
            case "Doctor" -> {
                roleIcon.setText("👨‍⚕️");
                roleCircle.setFill(
                    Color.web("#059669"));
                roleTitle.setText(
                    "Doctor Registration");
                roleSubLabel.setText(
                    "Medical professional account");
                registerButton.setStyle(
                    registerButton.getStyle()
                        .replace(
                            "#1D4ED8", "#059669"));
                addInfo("✓ Access patient records");
                addInfo("✓ Write medical records");
                addInfo("✓ Manage appointments");
                addInfo("✓ Issue prescriptions");
            }
            case "Nurse" -> {
                roleIcon.setText("👩‍⚕️");
                roleCircle.setFill(
                    Color.web("#F59E0B"));
                roleTitle.setText(
                    "Nurse / Staff Registration");
                roleSubLabel.setText(
                    "Healthcare staff account");
                registerButton.setStyle(
                    registerButton.getStyle()
                        .replace(
                            "#1D4ED8", "#D97706"));
                addInfo("✓ Manage patients");
                addInfo("✓ Schedule appointments");
                addInfo("✓ View inventory");
                addInfo("✓ Basic records access");
            }
            case "Patient" -> {
                roleIcon.setText("🧑");
                roleCircle.setFill(
                    Color.web("#8B5CF6"));
                roleTitle.setText(
                    "Patient Registration");
                roleSubLabel.setText(
                    "Personal health portal account");
                registerButton.setStyle(
                    registerButton.getStyle()
                        .replace(
                            "#1D4ED8", "#8B5CF6"));
                addInfo("✓ View your appointments");
                addInfo("✓ View your medical records");
                addInfo("✓ View your billing history");
                addInfo("✓ Personal health timeline");
            }
        }
    }

    private void addInfo(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-text-fill: #CBD5E1;" +
            "-fx-font-size: 12px;");
        roleInfoBox.getChildren().add(lbl);
    }

    private void setupUsernameChecker() {
        // Live username availability check
        usernameField.textProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal.trim().isEmpty()) {
                    usernameStatus.setText("");
                    return;
                }
                if (newVal.contains(" ")) {
                    usernameStatus.setText(
                        "❌ No spaces allowed");
                    usernameStatus.setStyle(
                        "-fx-text-fill: #DC2626;");
                    return;
                }
                checkUsernameAvailability(
                    newVal.trim());
            });
    }

    private void checkUsernameAvailability(
            String username) {
        new Thread(() -> {
            try {
                PreparedStatement s =
                    DatabaseManager.getConnection()
                        .prepareStatement(
                            "SELECT COUNT(*) FROM users " +
                            "WHERE username = ?");
                s.setString(1, username);
                ResultSet rs = s.executeQuery();
                boolean taken =
                    rs.next() && rs.getInt(1) > 0;

                javafx.application.Platform.runLater(
                    () -> {
                        if (taken) {
                            usernameStatus.setText(
                                "❌ Username already taken");
                            usernameStatus.setStyle(
                                "-fx-text-fill: #DC2626;" +
                                "-fx-font-size: 11px;");
                        } else {
                            usernameStatus.setText(
                                "✅ Username available");
                            usernameStatus.setStyle(
                                "-fx-text-fill: #059669;" +
                                "-fx-font-size: 11px;");
                        }
                    });
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleRegister() {
        // Clear previous status
        statusLabel.setText("");

        // --- VALIDATION ---
        String fullName =
            fullNameField.getText().trim();
        String username =
            usernameField.getText().trim();
        String email =
            emailField.getText().trim();
        String phone =
            phoneField.getText().trim();
        String password =
            passwordField.getText().trim();
        String confirm =
            confirmField.getText().trim();
        String role =
            LoginController.getSelectedRole();

        // Required fields
        if (fullName.isEmpty()) {
            showStatus(
                "❌ Full name is required.", true);
            return;
        }
        if (username.isEmpty()) {
            showStatus(
                "❌ Username is required.", true);
            return;
        }
        if (username.contains(" ")) {
            showStatus(
                "❌ Username cannot contain spaces.",
                true);
            return;
        }
        if (password.isEmpty()) {
            showStatus(
                "❌ Password is required.", true);
            return;
        }
        if (password.length() < 6) {
            showStatus(
                "❌ Password must be at least " +
                "6 characters.", true);
            return;
        }
        if (!password.equals(confirm)) {
            showStatus(
                "❌ Passwords do not match!", true);
            return;
        }

        // Email format check
        if (!email.isEmpty() &&
                !email.matches(
                    "^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showStatus(
                "❌ Invalid email format.", true);
            return;
        }

        // Phone format check
        if (!phone.isEmpty() &&
                !phone.matches(
                    "^[0-9+\\-\\s]{7,15}$")) {
            showStatus(
                "❌ Invalid phone number format.",
                true);
            return;
        }

        // Disable button while saving
        registerButton.setDisable(true);
        registerButton.setText("Creating account...");

        try {
            // Final check — username must be unique
            PreparedStatement check =
                DatabaseManager.getConnection()
                    .prepareStatement(
                        "SELECT COUNT(*) FROM users " +
                        "WHERE username = ?");
            check.setString(1, username);
            ResultSet checkRs = check.executeQuery();
            if (checkRs.next() &&
                    checkRs.getInt(1) > 0) {
                showStatus(
                    "❌ Username is already taken. " +
                    "Please choose another.", true);
                registerButton.setDisable(false);
                registerButton.setText(
                    "✅  Create Account");
                return;
            }

            // Hash the password
            String hash = BCrypt.hashpw(
                password, BCrypt.gensalt(10));

            // Insert into database
            String sql =
                "INSERT INTO users " +
                "(full_name, username, password_hash, " +
                "role, email, phone, is_active) " +
                "VALUES (?,?,?,?,?,?,?)";

            PreparedStatement s =
                DatabaseManager.getConnection()
                    .prepareStatement(sql);
            s.setString(1, fullName);
            s.setString(2, username);
            s.setString(3, hash);
            s.setString(4, role);
            s.setString(5,
                email.isEmpty() ? null : email);
            s.setString(6,
                phone.isEmpty() ? null : phone);

            // Admins are active by default
            // Others need admin approval
            s.setBoolean(7,
                role.equals("Admin") ||
                role.equals("Patient"));

            s.executeUpdate();

            // Success!
            showSuccessAndRedirect(
                fullName, username, role);

        } catch (SQLException e) {
            showStatus(
                "❌ Registration failed: " +
                e.getMessage(), true);
        } finally {
            registerButton.setDisable(false);
            registerButton.setText(
                "✅  Create Account");
        }
    }

    private void showSuccessAndRedirect(
            String name, String username, String role) {

        String message = role.equals("Admin") ||
            role.equals("Patient")
            ? "✅ Account created successfully!\n\n" +
              "You can now sign in with:\n" +
              "Username: " + username
            : "✅ Account created successfully!\n\n" +
              "Your account is pending admin activation.\n" +
              "Please contact your administrator to " +
              "activate your account.";

        showStatus(message, false);

        // Go back to login after 3 seconds
        javafx.animation.Timeline timer =
            new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.seconds(3),
                    e -> goBackToLogin()
                )
            );
        timer.play();
    }

    @FXML
    private void goBackToLogin() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/Login.fxml"));
            Stage stage = (Stage) registerButton
                .getScene().getWindow();
            stage.setScene(new Scene(root, 900, 580));
            stage.setTitle(
                "BHC System — " +
                LoginController.getSelectedRole() +
                " Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill:" +
            (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size:12px;" +
            "-fx-padding:8 12;" +
            "-fx-background-color:" +
            (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius:6;"
        );
    }
}