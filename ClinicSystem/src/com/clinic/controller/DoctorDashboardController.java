package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.ResultSet;
import java.sql.SQLException;
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
        roleLabel.setText("Doctor");
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

        // Greeting
        int hour = LocalTime.now().getHour();
        String greeting =
            hour < 12 ? "Good Morning"
            : hour < 18 ? "Good Afternoon"
            : "Good Evening";

        Label greet = new Label(
            greeting + ", " +
            LoginController.getLoggedInUser() +
            "! 👋");
        greet.setStyle(
            "-fx-font-size:20px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#0F172A;");

        Label sub = new Label(
            "Here are your patients and appointments.");
        sub.setStyle(
            "-fx-font-size:13px;" +
            "-fx-text-fill:#64748B;");

        // Stat cards
        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("👥", "Total Patients",
                getCount(
                    "SELECT COUNT(*) FROM patients"),
                "#059669"),
            statCard("📅", "Today's Appointments",
                getCount(
                    "SELECT COUNT(*) FROM appointments " +
                    "WHERE appointment_date = " +
                    "CURRENT_DATE " +
                    "AND status = 'Scheduled'"),
                "#1D4ED8"),
            statCard("✅", "Completed Today",
                getCount(
                    "SELECT COUNT(*) FROM appointments " +
                    "WHERE appointment_date = " +
                    "CURRENT_DATE " +
                    "AND status = 'Completed'"),
                "#8B5CF6"),
            statCard("👩‍⚕️", "Active Assignments",
                getCount(
                    "SELECT COUNT(*) FROM " +
                    "nurse_assignments " +
                    "WHERE status IN " +
                    "('Pending','In Progress')"),
                "#F59E0B")
        );

        // Today's appointments table
        Label apptTitle = new Label(
            "📅  Today's Patients");
        apptTitle.setStyle(
            "-fx-font-size:15px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#0F172A;");

        TableView<String[]> todayTable =
            buildTodayTable();

        // Nurse assignments summary
        Label assignTitle = new Label(
            "👩‍⚕️  Recent Nurse Assignments");
        assignTitle.setStyle(
            "-fx-font-size:15px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#0F172A;");

        TableView<String[]> assignTable =
            buildAssignmentSummaryTable();

        content.getChildren().addAll(
            greet, sub, cards,
            apptTitle, todayTable,
            assignTitle, assignTable
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;");

        contentArea.getChildren().setAll(scroll);
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
            "-fx-min-width:160;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(
            javafx.geometry.Pos.CENTER_LEFT);
        Label iconL = new Label(icon);
        iconL.setStyle("-fx-font-size:18px;");
        Label titleL = new Label(title);
        titleL.setStyle(
            "-fx-font-size:11px;" +
            "-fx-text-fill:#64748B;" +
            "-fx-font-weight:bold;");
        top.getChildren().addAll(iconL, titleL);

        Label valueL = new Label(value);
        valueL.setStyle(
            "-fx-font-size:30px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:" + color + ";");

        card.getChildren().addAll(top, valueL);
        return card;
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]> buildTodayTable() {
        TableView<String[]> table =
            new TableView<>();
        table.setPrefHeight(200);
        table.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian," +
            "rgba(0,0,0,0.06),12,0,0,4);");

        table.getColumns().addAll(
            makeCol("Time",    0, 100),
            makeCol("Patient", 1, 200),
            makeCol("Reason",  2, 200),
            makeCol("Status",  3, 120)
        );

        try {
            String sql =
                "SELECT a.appointment_time::text, " +
                "p.full_name, " +
                "COALESCE(a.reason,'—'), " +
                "a.status " +
                "FROM appointments a " +
                "JOIN patients p " +
                "ON a.patient_id = p.id " +
                "WHERE a.appointment_date = " +
                "CURRENT_DATE " +
                "ORDER BY a.appointment_time";
            ResultSet rs = DatabaseManager
                .getConnection()
                .prepareStatement(sql)
                .executeQuery();
            while (rs.next()) {
                String t = rs.getString(1);
                table.getItems().add(new String[]{
                    t != null && t.length() >= 5
                        ? t.substring(0, 5) : "—",
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
                "No appointments today."));
        }
        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]>
            buildAssignmentSummaryTable() {
        TableView<String[]> table =
            new TableView<>();
        table.setPrefHeight(180);
        table.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian," +
            "rgba(0,0,0,0.06),12,0,0,4);");

        table.getColumns().addAll(
            makeCol("Nurse",   0, 160),
            makeCol("Patient", 1, 160),
            makeCol("Task",    2, 200),
            makeCol("Status",  3, 110),
            makeCol("Date",    4, 110)
        );

        try {
            String sql =
                "SELECT un.full_name, " +
                "p.full_name, " +
                "COALESCE(" +
                "na.task_description,'—'), " +
                "na.status, " +
                "na.assigned_at::text " +
                "FROM nurse_assignments na " +
                "JOIN users un " +
                "ON na.nurse_id = un.id " +
                "JOIN patients p " +
                "ON na.patient_id = p.id " +
                "ORDER BY na.assigned_at DESC " +
                "LIMIT 8";
            ResultSet rs = DatabaseManager
                .getConnection()
                .prepareStatement(sql)
                .executeQuery();
            while (rs.next()) {
                String date = rs.getString(5);
                table.getItems().add(new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    date != null &&
                        date.length() >= 10
                        ? date.substring(0, 10)
                        : "—"
                });
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        if (table.getItems().isEmpty()) {
            table.setPlaceholder(new Label(
                "No nurse assignments yet."));
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
            new SimpleStringProperty(
                ((String[]) d.getValue())[idx]));
        return col;
    }

    private String getCount(String sql) {
        try {
            ResultSet rs = DatabaseManager
                .getConnection()
                .prepareStatement(sql)
                .executeQuery();
            if (rs.next())
                return String.valueOf(
                    rs.getInt(1));
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return "0";
    }

    // --- Navigation Methods ---

    @FXML
    private void showPatients() {
        loadPage(
            "/com/clinic/fxml/Patient.fxml",
            "My Patients");
    }

    @FXML
    private void showAppointments() {
        loadPage(
            "/com/clinic/fxml/Appointment.fxml",
            "Appointments");
    }

    @FXML
    private void showMedicalRecords() {
        loadPage(
            "/com/clinic/fxml/MedicalRecord.fxml",
            "Medical Records");
    }

    @FXML
    private void showPatientHistory() {
        loadPage(
            "/com/clinic/fxml/PatientHistory.fxml",
            "Patient History");
    }

    @FXML
    private void showNurseAssignment() {
        loadPage(
            "/com/clinic/fxml/NurseAssignment.fxml",
            "Assign Nurses");
    }

    private void loadPage(
            String path, String title) {
        pageTitle.setText(title);
        try {
            Parent page = FXMLLoader.load(
                getClass().getResource(path));
            contentArea.getChildren().setAll(page);
        } catch (Exception e) {
            Label err = new Label(
                "⚠️ Error loading page: " +
                e.getMessage());
            err.setStyle(
                "-fx-text-fill:#DC2626;" +
                "-fx-font-size:14px;");
            contentArea.getChildren().setAll(err);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent role = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/" +
                    "RoleSelection.fxml"));
            Stage stage = (Stage) contentArea
                .getScene().getWindow();
            stage.setScene(
                new Scene(role, 900, 580));
            stage.setTitle(
                "BHC System — Select Role");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}