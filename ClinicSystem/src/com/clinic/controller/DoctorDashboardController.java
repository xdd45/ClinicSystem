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

public class DoctorDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label     userLabel;
    @FXML private Label     roleLabel;
    @FXML private Label     pageTitle;
    @FXML private Label     clockLabel;

    @FXML
    public void initialize() {
        userLabel.setText(
            LoginController.getLoggedInUser());
        startClock();
        showDashboard();
    }

    private void startClock() {
        Timeline clock = new Timeline(
            new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(
                    LocalTime.now().format(
                        DateTimeFormatter.ofPattern(
                            "hh:mm:ss a")))));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    @FXML
    public void showDashboard() {
        pageTitle.setText("Doctor Dashboard");

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 4;");

        String hour = String.valueOf(
            LocalTime.now().getHour());
        String greeting =
            Integer.parseInt(hour) < 12
                ? "Good Morning"
                : Integer.parseInt(hour) < 18
                    ? "Good Afternoon"
                    : "Good Evening";

        Label greet = new Label(
            greeting + ", " +
            LoginController.getLoggedInUser() + "! 👋");
        greet.setStyle(
            "-fx-font-size:20px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#0F172A;");

        Label sub = new Label(
            "Here are your patients and appointments.");
        sub.setStyle(
            "-fx-font-size:13px;" +
            "-fx-text-fill:#64748B;");

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("👥", "My Patients",
                getCount(
                    "SELECT COUNT(*) FROM patients"),
                "#059669"),
            statCard("📅", "Today's Appointments",
                getCount(
                    "SELECT COUNT(*) FROM appointments " +
                    "WHERE appointment_date = CURRENT_DATE " +
                    "AND status = 'Scheduled'"),
                "#1D4ED8"),
            statCard("✅", "Completed Today",
                getCount(
                    "SELECT COUNT(*) FROM appointments " +
                    "WHERE appointment_date = CURRENT_DATE " +
                    "AND status = 'Completed'"),
                "#8B5CF6")
        );

        // Today's patient list
        Label todayTitle = new Label(
            "📅  Today's Patients");
        todayTitle.setStyle(
            "-fx-font-size:15px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#0F172A;");

        TableView<String[]> table =
            buildTodayTable();

        content.getChildren().addAll(
            greet, sub, cards, todayTitle, table);

        contentArea.getChildren().setAll(
            new ScrollPane(content) {{
                setFitToWidth(true);
                setStyle(
                    "-fx-background-color:transparent;" +
                    "-fx-background:transparent;");
            }}
        );
    }

    private VBox statCard(String icon, String title,
                          String value, String color) {
        VBox card = new VBox(8);
        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-padding:20;" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian," +
            "rgba(0,0,0,0.06),12,0,0,4);" +
            "-fx-border-color:" + color + ";" +
            "-fx-border-width:0 0 0 4;" +
            "-fx-border-radius:0 12 12 0;" +
            "-fx-min-width:180;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconL = new Label(icon);
        iconL.setStyle("-fx-font-size:20px;");
        Label titleL = new Label(title);
        titleL.setStyle(
            "-fx-font-size:12px;" +
            "-fx-text-fill:#64748B;" +
            "-fx-font-weight:bold;");
        top.getChildren().addAll(iconL, titleL);

        Label valueL = new Label(value);
        valueL.setStyle(
            "-fx-font-size:32px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:" + color + ";");

        card.getChildren().addAll(top, valueL);
        return card;
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]> buildTodayTable() {
        TableView<String[]> table = new TableView<>();
        table.setPrefHeight(250);
        table.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian," +
            "rgba(0,0,0,0.06),12,0,0,4);");

        TableColumn<String[], String> c1 =
            makeCol("Time", 0, 100);
        TableColumn<String[], String> c2 =
            makeCol("Patient", 1, 200);
        TableColumn<String[], String> c3 =
            makeCol("Reason", 2, 200);
        TableColumn<String[], String> c4 =
            makeCol("Status", 3, 120);
        table.getColumns().addAll(c1, c2, c3, c4);

        try {
            String sql =
                "SELECT a.appointment_time::text, " +
                "p.full_name, " +
                "COALESCE(a.reason,'—'), a.status " +
                "FROM appointments a " +
                "JOIN patients p ON a.patient_id=p.id " +
                "WHERE a.appointment_date=CURRENT_DATE " +
                "ORDER BY a.appointment_time";
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            while (rs.next()) {
                String t = rs.getString(1);
                table.getItems().add(new String[]{
                    t != null && t.length() >= 5
                        ? t.substring(0,5) : "—",
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4)
                });
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        if (table.getItems().isEmpty()) {
            table.setPlaceholder(new Label(
                "No appointments scheduled for today."));
        }
        return table;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<T, String> makeCol(
            String title, int idx, int width) {
        TableColumn<T, String> col =
            new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                ((String[]) d.getValue())[idx]));
        return col;
    }

    private String getCount(String sql) {
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            if (rs.next())
                return String.valueOf(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return "0";
    }

    @FXML private void showPatients() {
        loadPage("/com/clinic/fxml/Patient.fxml",
            "My Patients");
    }
    @FXML private void showAppointments() {
        loadPage("/com/clinic/fxml/Appointment.fxml",
            "Appointments");
    }
    @FXML private void showMedicalRecords() {
        loadPage("/com/clinic/fxml/MedicalRecord.fxml",
            "Medical Records");
    }
    @FXML private void showPatientHistory() {
        loadPage("/com/clinic/fxml/PatientHistory.fxml",
            "Patient History");
    }

    private void loadPage(String path, String title) {
        pageTitle.setText(title);
        try {
            Parent page = FXMLLoader.load(
                getClass().getResource(path));
            contentArea.getChildren().setAll(page);
        } catch (Exception e) {
            contentArea.getChildren().setAll(
                new Label("⚠️ Error: " +
                    e.getMessage()));
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent role = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/RoleSelection.fxml"));
            Stage stage = (Stage) contentArea
                .getScene().getWindow();
            stage.setScene(new Scene(role, 900, 580));
            stage.setTitle(
                "BHC System — Select Role");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}