package com.clinic.controller;

import com.clinic.db.AuditLogger;
import com.clinic.db.DatabaseManager;
import com.clinic.db.SessionManager;
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

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;
    @FXML private Label         roleTitle;
    @FXML private Label         roleSubtitle;
    @FXML private Label         roleIcon;
    @FXML private Circle        roleCircle;
    @FXML private VBox          accessList;

    // Static role info passed from RoleSelectionController
    private static String selectedRole = "Admin";
    private static String roleColor    = "#1D4ED8";

    // Keep backward compatibility with other controllers
    private static String loggedInUser = "";
    private static String loggedInRole = "";

    // --- Static getters and setters ---
    public static void setSelectedRole(String role) {
        selectedRole = role;
    }
    public static void setRoleColor(String color) {
        roleColor = color;
    }
    public static String getSelectedRole() {
        return selectedRole;
    }
    public static String getLoggedInUser() {
        return loggedInUser;
    }
    public static String getLoggedInRole() {
        return loggedInRole;
    }

    @FXML
    public void initialize() {
        setupRoleUI();
    }

    /**
     * Sets up the left panel UI based on the
     * selected role from RoleSelectionController.
     */
    private void setupRoleUI() {
        // Set circle color
        roleCircle.setFill(Color.web(roleColor));

        // Set login button color to match role
        loginButton.setStyle(
            "-fx-background-color: " + roleColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 14;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;"
        );

        // Set role-specific content on left panel
        switch (selectedRole) {
            case "Admin" -> {
                roleIcon.setText("👑");
                roleTitle.setText("Admin Portal");
                roleSubtitle.setText(
                    "Sign in with your admin credentials");
                addAccessItem("✓ Full system access");
                addAccessItem("✓ User management");
                addAccessItem("✓ Reports & analytics");
                addAccessItem("✓ Clinic settings");
                addAccessItem("✓ Billing & inventory");
            }
            case "Doctor" -> {
                roleIcon.setText("👨‍⚕️");
                roleTitle.setText("Doctor Portal");
                roleSubtitle.setText(
                    "Sign in with your doctor credentials");
                addAccessItem("✓ Patient records");
                addAccessItem("✓ Medical records");
                addAccessItem("✓ Appointments");
                addAccessItem("✓ Prescriptions");
            }
            case "Nurse" -> {
                roleIcon.setText("👩‍⚕️");
                roleTitle.setText("Nurse / Staff Portal");
                roleSubtitle.setText(
                    "Sign in with your staff credentials");
                addAccessItem("✓ Patient management");
                addAccessItem("✓ Appointments");
                addAccessItem("✓ Basic medical records");
                addAccessItem("✓ Inventory view");
            }
            case "Patient" -> {
                roleIcon.setText("🧑");
                roleTitle.setText("Patient Portal");
                roleSubtitle.setText(
                    "Sign in to view your health records");
                addAccessItem("✓ My appointments");
                addAccessItem("✓ My medical records");
                addAccessItem("✓ My billing history");
                addAccessItem("✓ My prescriptions");
            }
        }
    }

    /**
     * Adds a bullet point item to the
     * access list on the left panel.
     */
    private void addAccessItem(String text) {
        Label item = new Label(text);
        item.setStyle(
            "-fx-text-fill: #CBD5E1;" +
            "-fx-font-size: 12px;"
        );
        accessList.getChildren().add(item);
    }

    /**
     * Called when user clicks Sign In button.
     * Verifies credentials and routes to the
     * correct dashboard based on role.
     */
    @FXML
    private void handleLogin() {
        String username =
            usernameField.getText().trim();
        String password =
            passwordField.getText().trim();

        // Basic validation
        if (username.isEmpty() ||
                password.isEmpty()) {
            showError(
                "Please enter your username " +
                "and password.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Signing in...");
        errorLabel.setText("");

        try {
            // Build query based on role
            // Patient uses a special join query
            String sql =
                selectedRole.equals("Patient")
                ? "SELECT u.*, " +
                  "COALESCE(u.patient_id, 0) as pid " +
                  "FROM users u " +
                  "WHERE u.username = ? " +
                  "AND u.role = 'Patient' " +
                  "AND (u.is_active = true " +
                  "OR u.is_active IS NULL)"
                : "SELECT * FROM users " +
                  "WHERE username = ? " +
                  "AND role = ? " +
                  "AND (is_active = true " +
                  "OR is_active IS NULL)";

            PreparedStatement stmt =
                DatabaseManager.getConnection()
                    .prepareStatement(sql);

            stmt.setString(1, username);
            if (!selectedRole.equals("Patient")) {
                stmt.setString(2, selectedRole);
            }

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hash =
                    rs.getString("password_hash");

                if (BCrypt.checkpw(password, hash)) {

                    // Save session info
                    SessionManager.login(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role")
                    );

                    // Keep backward compatibility
                    loggedInUser =
                        rs.getString("full_name");
                    loggedInRole =
                        rs.getString("role");

                    // Save patient ID for patient portal
                    if (selectedRole.equals("Patient")) {
                        try {
                            int pid = rs.getInt("pid");
                            if (pid > 0) {
                                SessionManager
                                    .setPatientId(pid);
                            }
                        } catch (Exception ignored) {}
                    }

                    // Log the login
                    AuditLogger.log(
                        "LOGIN",
                        "users",
                        rs.getInt("id"),
                        selectedRole + " logged in: " +
                        username
                    );

                    openDashboard();

                } else {
                    showError(
                        "Incorrect password. " +
                        "Please try again.");
                }
            } else {
                // Check if account exists but inactive
                PreparedStatement checkInactive =
                    DatabaseManager.getConnection()
                        .prepareStatement(
                            "SELECT COUNT(*) FROM users " +
                            "WHERE username=? " +
                            "AND role=? " +
                            "AND is_active=false");
                checkInactive.setString(1, username);
                checkInactive.setString(
                    2, selectedRole);
                ResultSet inactiveRs =
                    checkInactive.executeQuery();

                if (inactiveRs.next() &&
                        inactiveRs.getInt(1) > 0) {
                    showError(
                        "⚠️ Your account is pending " +
                        "admin activation.\n" +
                        "Please contact your " +
                        "administrator.");
                } else {
                    showError(
                        "No " + selectedRole +
                        " account found with that " +
                        "username.\nPlease check your " +
                        "credentials or register.");
                }
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            showError(
                "Connection error: " + e.getMessage());
        } finally {
            loginButton.setDisable(false);
            loginButton.setText("Sign In →");
        }
    }

    /**
     * Routes to the correct dashboard
     * based on the selected role.
     */
    private void openDashboard() {
        try {
            String fxmlPath;

            switch (selectedRole) {
                case "Doctor" ->
                    fxmlPath =
                        "/com/clinic/fxml/" +
                        "DoctorDashboard.fxml";
                case "Nurse" ->
                    fxmlPath =
                        "/com/clinic/fxml/" +
                        "NurseDashboard.fxml";
                case "Patient" ->
                    fxmlPath =
                        "/com/clinic/fxml/" +
                        "PatientPortal.fxml";
                default ->
                    fxmlPath =
                        "/com/clinic/fxml/" +
                        "Dashboard.fxml";
            }

            Parent root = FXMLLoader.load(
                getClass().getResource(fxmlPath));

            Stage stage = (Stage) loginButton
                .getScene().getWindow();

            stage.setScene(
                new Scene(root, 1200, 720));
            stage.setTitle(
                "BHC System — " +
                selectedRole + " Dashboard");
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.show();

        } catch (Exception e) {
            showError(
                "Failed to open dashboard: " +
                e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the registration page.
     * Called when "Don't have an account?" 
     * is clicked.
     */
    @FXML
    private void goToRegister() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/Register.fxml"));
            Stage stage = (Stage) loginButton
                .getScene().getWindow();
            stage.setScene(
                new Scene(root, 900, 620));
            stage.setTitle(
                "BHC System — Create Account");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Goes back to the role selection screen.
     * Called when "Back to Role Selection" 
     * is clicked.
     */
    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/" +
                    "RoleSelection.fxml"));
            Stage stage = (Stage) loginButton
                .getScene().getWindow();
            stage.setScene(
                new Scene(root, 1000, 620));
            stage.setTitle(
                "BHC System — Select Role");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(
            !message.isEmpty());
        errorLabel.setManaged(
            !message.isEmpty());
    }
}