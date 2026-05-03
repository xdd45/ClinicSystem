package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import javafx.animation.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.util.Duration;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label     userLabel;
    @FXML private Label     roleLabel;
    @FXML private Label     pageTitle;
    @FXML private Label     dateLabel;
    @FXML private Label     clockLabel;

    @FXML
    public void initialize() {
        userLabel.setText(LoginController.getLoggedInUser());
        roleLabel.setText(LoginController.getLoggedInRole());
        startClock();
        showDashboard();
    }

    // Live clock that updates every second
    private void startClock() {
        dateLabel.setText(LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        Timeline clock = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                clockLabel.setText(LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
            })
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    @FXML
    public void showDashboard() {
        pageTitle.setText("Dashboard");

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 4;");

        // Greeting
        String hour = String.valueOf(LocalTime.now().getHour());
        String greeting = Integer.parseInt(hour) < 12 ? "Good Morning" :
                          Integer.parseInt(hour) < 18 ? "Good Afternoon" : "Good Evening";
        Label greet = new Label(greeting + ", " + LoginController.getLoggedInUser() + "! 👋");
        greet.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label sub = new Label("Here's what's happening at the clinic today.");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        // Stat Cards Row 1
        HBox row1 = new HBox(16);
        row1.getChildren().addAll(
            statCard("👥", "Total Patients",
                getCount("SELECT COUNT(*) FROM patients"), "#3B82F6", "card-blue"),
            statCard("📅", "Today's Appointments",
                getCount("SELECT COUNT(*) FROM appointments WHERE appointment_date = CURRENT_DATE"),
                "#10B981", "card-green"),
            statCard("⏳", "Pending Appointments",
                getCount("SELECT COUNT(*) FROM appointments WHERE status='Scheduled' AND appointment_date >= CURRENT_DATE"),
                "#F59E0B", "card-orange"),
            statCard("✅", "Completed Today",
                getCount("SELECT COUNT(*) FROM appointments WHERE status='Completed' AND appointment_date = CURRENT_DATE"),
                "#8B5CF6", "card-purple")
        );

        // Today's Appointments Table
        Label apptTitle = new Label("📅  Today's Appointments");
        apptTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TableView<String[]> apptTable = buildTodayTable();

        // Recent Patients
        Label recentTitle = new Label("👥  Recently Added Patients");
        recentTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TableView<String[]> recentTable = buildRecentPatientsTable();

        content.getChildren().addAll(
            greet, sub, row1,
            apptTitle, apptTable,
            recentTitle, recentTable
        );

        contentArea.getChildren().setAll(new javafx.scene.control.ScrollPane(content) {{
            setFitToWidth(true);
            setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        }});
    }

    private VBox statCard(String icon, String title, String value, String color, String styleClass) {
        VBox card = new VBox(8);
        card.setStyle(
            "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),12,0,0,4);" +
            "-fx-border-color: " + color + "; -fx-border-width: 0 0 0 4;" +
            "-fx-border-radius: 0 12 12 0; -fx-min-width: 180;"
        );
        card.setPrefWidth(200);

        HBox top = new HBox(8);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
        top.getChildren().addAll(iconLabel, titleLabel);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(top, valueLabel);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]> buildTodayTable() {
        TableView<String[]> table = new TableView<>();
        table.setPrefHeight(200);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),12,0,0,4);");

        TableColumn<String[], String> colTime   = col("Time", 0, 120);
        TableColumn<String[], String> colPat    = col("Patient", 1, 200);
        TableColumn<String[], String> colReason = col("Reason", 2, 200);
        TableColumn<String[], String> colStatus = col("Status", 3, 120);
        table.getColumns().addAll(colTime, colPat, colReason, colStatus);

        try {
            String sql = "SELECT a.appointment_time, p.full_name, a.reason, a.status " +
                         "FROM appointments a JOIN patients p ON a.patient_id = p.id " +
                         "WHERE a.appointment_date = CURRENT_DATE ORDER BY a.appointment_time";
            ResultSet rs = DatabaseManager.getConnection().prepareStatement(sql).executeQuery();
            while (rs.next()) {
                table.getItems().add(new String[]{
                    rs.getString(1), rs.getString(2),
                    rs.getString(3), rs.getString(4)
                });
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }

        if (table.getItems().isEmpty()) {
            table.setPlaceholder(new Label("No appointments scheduled for today."));
        }
        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]> buildRecentPatientsTable() {
        TableView<String[]> table = new TableView<>();
        table.setPrefHeight(200);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),12,0,0,4);");

        TableColumn<String[], String> colName    = col("Name", 0, 200);
        TableColumn<String[], String> colGender  = col("Gender", 1, 100);
        TableColumn<String[], String> colContact = col("Contact", 2, 150);
        TableColumn<String[], String> colDate    = col("Added On", 3, 150);
        table.getColumns().addAll(colName, colGender, colContact, colDate);

        try {
            String sql = "SELECT full_name, gender, contact_number, created_at " +
                         "FROM patients ORDER BY created_at DESC LIMIT 10";
            ResultSet rs = DatabaseManager.getConnection().prepareStatement(sql).executeQuery();
            while (rs.next()) {
                table.getItems().add(new String[]{
                    rs.getString(1), rs.getString(2),
                    rs.getString(3) != null ? rs.getString(3) : "—",
                    rs.getTimestamp(4).toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                });
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return table;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<T, String> col(String title, int idx, int width) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                ((String[]) data.getValue())[idx]
            )
        );
        return col;
    }

    private String getCount(String sql) {
        try {
            ResultSet rs = DatabaseManager.getConnection().prepareStatement(sql).executeQuery();
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return "0";
    }

    // Navigation methods
    @FXML private void showPatients()      { loadPage("/com/clinic/fxml/Patient.fxml",      "Patients"); }
    @FXML private void showAppointments() { loadPage("/com/clinic/fxml/Appointment.fxml",   "Appointments"); }
    @FXML private void showMedicalRecords(){ loadPage("/com/clinic/fxml/MedicalRecord.fxml","Medical Records"); }
    @FXML private void showInventory()    { loadPage("/com/clinic/fxml/Inventory.fxml",     "Inventory"); }
    @FXML private void showBilling()      { loadPage("/com/clinic/fxml/Billing.fxml",       "Billing"); }
    @FXML private void showReports()      { loadPage("/com/clinic/fxml/Reports.fxml",       "Reports"); }
    @FXML private void showUsers()        { loadPage("/com/clinic/fxml/UserManagement.fxml","User Management"); }
    @FXML private void showSettings()     { loadPage("/com/clinic/fxml/Settings.fxml",      "Settings"); }

    private void loadPage(String path, String title) {
        pageTitle.setText(title);
        try {
            Parent page = FXMLLoader.load(getClass().getResource(path));
            contentArea.getChildren().setAll(page);
        } catch (Exception e) {
            Label err = new Label("⚠️ Page coming soon: " + title);
            err.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748B;");
            contentArea.getChildren().setAll(err);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(
                getClass().getResource("/com/clinic/fxml/Login.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(login, 900, 580));
            stage.setTitle("BHC System — Login");
        } catch (Exception e) { e.printStackTrace(); }
    }
}