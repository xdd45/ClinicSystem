package com.clinic.controller;

import com.clinic.db.AuditLogger;
import com.clinic.db.DatabaseManager;
import com.clinic.db.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class UserManagementController {

    @FXML private TableView<String[]>           userTable;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colName;
    @FXML private TableColumn<String[], String> colUsername;
    @FXML private TableColumn<String[], String> colRole;
    @FXML private TableColumn<String[], String> colStatus;
    @FXML private TableColumn<String[], String> colCreated;

    @FXML private TextField        nameField;
    @FXML private TextField        usernameField;
    @FXML private PasswordField    passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField        emailField;
    @FXML private TextField        phoneField;
    @FXML private CheckBox         activeCheck;
    @FXML private Label            statusLabel;
    @FXML private Label            formTitle;
    @FXML private Button           saveButton;

    private int editingId = -1;
    private ObservableList<String[]> userList =
        FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Only Admin can access this module
        if (!SessionManager.isAdmin()) {
            showStatus("⛔ Access denied. Admins only.", true);
            nameField.setDisable(true);
            usernameField.setDisable(true);
            passwordField.setDisable(true);
            roleCombo.setDisable(true);
            saveButton.setDisable(true);
        }

        setupColumns();
        roleCombo.setItems(FXCollections.observableArrayList(
            "Admin", "Doctor", "Staff"
        ));
        loadUsers();
    }

    private void setupColumns() {
        colId.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[0]));
        colName.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[1]));
        colUsername.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[2]));
        colRole.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[3]));
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[4]));
        colCreated.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[5]));

        // Color code roles
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null); setStyle(""); return;
                }
                setText(role);
                switch (role) {
                    case "Admin"  ->
                        setStyle("-fx-text-fill:#1D4ED8;" +
                            "-fx-font-weight:bold;");
                    case "Doctor" ->
                        setStyle("-fx-text-fill:#059669;" +
                            "-fx-font-weight:bold;");
                    default ->
                        setStyle("-fx-text-fill:#64748B;");
                }
            }
        });

        // Color code status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null); setStyle(""); return;
                }
                setText(s);
                setStyle("Active".equals(s)
                    ? "-fx-text-fill:#059669;-fx-font-weight:bold;"
                    : "-fx-text-fill:#DC2626;-fx-font-weight:bold;"
                );
            }
        });

        userTable.setItems(userList);
        userTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> {
                if (n != null) populateForm(n);
            });
    }

    private void loadUsers() {
        userList.clear();
        String sql =
            "SELECT id::text, full_name, username, role, " +
            "CASE WHEN is_active THEN 'Active' ELSE 'Inactive' END, " +
            "created_at::text " +
            "FROM users ORDER BY full_name";
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            while (rs.next()) {
                String created = rs.getString(6);
                userList.add(new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    created != null && created.length() >= 10
                        ? created.substring(0, 10) : "—"
                });
            }
        } catch (SQLException e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        // Prevent editing your own role/status
        if (editingId == SessionManager.getCurrentUserId()) {
            showStatus("⚠️ You cannot edit your own account here.",
                true);
            return;
        }

        if (editingId == -1) insertUser();
        else updateUser();
    }

    private void insertUser() {
        if (passwordField.getText().trim().isEmpty()) {
            showStatus("Password is required for new users.", true);
            return;
        }
        if (isUsernameTaken()) {
            showStatus("Username already exists!", true);
            return;
        }

        String hash = BCrypt.hashpw(
            passwordField.getText().trim(),
            BCrypt.gensalt(10)
        );
        String sql =
            "INSERT INTO users " +
            "(full_name, username, password_hash, role, " +
            "email, phone, is_active) " +
            "VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement(sql);
            s.setString(1, nameField.getText().trim());
            s.setString(2, usernameField.getText().trim());
            s.setString(3, hash);
            s.setString(4, roleCombo.getValue());
            s.setString(5, emailField.getText().trim());
            s.setString(6, phoneField.getText().trim());
            s.setBoolean(7, activeCheck.isSelected());
            s.executeUpdate();

            AuditLogger.log("ADD USER", "users", 0,
                "Added user: " + usernameField.getText().trim());

            showStatus("✅ User created successfully!", false);
            clearForm();
            loadUsers();
        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    private void updateUser() {
        String sql =
            "UPDATE users SET full_name=?, username=?, role=?, " +
            "email=?, phone=?, is_active=? WHERE id=?";
        try {
            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement(sql);
            s.setString(1, nameField.getText().trim());
            s.setString(2, usernameField.getText().trim());
            s.setString(3, roleCombo.getValue());
            s.setString(4, emailField.getText().trim());
            s.setString(5, phoneField.getText().trim());
            s.setBoolean(6, activeCheck.isSelected());
            s.setInt(7, editingId);
            s.executeUpdate();

            // Update password only if provided
            if (!passwordField.getText().trim().isEmpty()) {
                String hash = BCrypt.hashpw(
                    passwordField.getText().trim(),
                    BCrypt.gensalt(10)
                );
                PreparedStatement sp = DatabaseManager.getConnection()
                    .prepareStatement(
                        "UPDATE users SET password_hash=? WHERE id=?");
                sp.setString(1, hash);
                sp.setInt(2, editingId);
                sp.executeUpdate();
            }

            AuditLogger.log("UPDATE USER", "users", editingId,
                "Updated user: " + usernameField.getText());

            showStatus("✅ User updated!", false);
            clearForm();
            loadUsers();
        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleResetPassword() {
        if (editingId == -1) {
            showStatus("Select a user first.", true);
            return;
        }

        // Generate a default password
        String newPassword = "bhc" + editingId + "2024";
        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password");
        confirm.setHeaderText("Reset password for this user?");
        confirm.setContentText(
            "New temporary password will be:\n\n" +
            "   " + newPassword + "\n\n" +
            "Please inform the user to change it after login."
        );

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    PreparedStatement s = DatabaseManager.getConnection()
                        .prepareStatement(
                            "UPDATE users SET password_hash=? WHERE id=?");
                    s.setString(1, hash);
                    s.setInt(2, editingId);
                    s.executeUpdate();
                    showStatus("✅ Password reset to: " + newPassword,
                        false);
                } catch (SQLException e) {
                    showStatus("❌ Error: " + e.getMessage(), true);
                }
            }
        });
    }

    private boolean isUsernameTaken() {
        String sql = editingId == -1
            ? "SELECT COUNT(*) FROM users WHERE username=?"
            : "SELECT COUNT(*) FROM users WHERE username=? AND id!=?";
        try {
            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement(sql);
            s.setString(1, usernameField.getText().trim());
            if (editingId != -1) s.setInt(2, editingId);
            ResultSet rs = s.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    private void populateForm(String[] row) {
        editingId = Integer.parseInt(row[0]);
        formTitle.setText("Edit User #" + editingId);
        nameField.setText(row[1]);
        usernameField.setText(row[2]);
        passwordField.clear();
        roleCombo.setValue(row[3]);
        activeCheck.setSelected("Active".equals(row[4]));
        saveButton.setText("💾  Update User");
        statusLabel.setText("");

        // Load email and phone separately
        try {
            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement(
                    "SELECT COALESCE(email,''), COALESCE(phone,'') " +
                    "FROM users WHERE id=?");
            s.setInt(1, editingId);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                emailField.setText(rs.getString(1));
                phoneField.setText(rs.getString(2));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    public void clearForm() {
        editingId = -1;
        formTitle.setText("New User");
        nameField.clear();
        usernameField.clear();
        passwordField.clear();
        roleCombo.setValue(null);
        emailField.clear();
        phoneField.clear();
        activeCheck.setSelected(true);
        userTable.getSelectionModel().clearSelection();
        saveButton.setText("💾  Save User");
        statusLabel.setText("");
    }

    private boolean validate() {
        StringBuilder err = new StringBuilder();
        if (nameField.getText().trim().isEmpty())
            err.append("• Full name is required.\n");
        if (usernameField.getText().trim().isEmpty())
            err.append("• Username is required.\n");
        if (usernameField.getText().trim().contains(" "))
            err.append("• Username cannot have spaces.\n");
        if (roleCombo.getValue() == null)
            err.append("• Please select a role.\n");
        if (err.length() > 0) {
            showStatus(err.toString(), true);
            return false;
        }
        return true;
    }

    private void showStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill:" + (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size:12px;-fx-padding:6 10;" +
            "-fx-background-color:" + (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius:6;"
        );
    }
}