package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import com.clinic.db.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PatientPortalController {

    @FXML private StackPane contentArea;
    @FXML private Label     userLabel;
    @FXML private Label     pageTitle;
    @FXML private Label     dateLabel;

    private int patientId = 0;

    @FXML
    public void initialize() {
        userLabel.setText(
            LoginController.getLoggedInUser());
        dateLabel.setText(LocalDate.now().format(
            DateTimeFormatter.ofPattern(
                "EEEE, MMMM d, yyyy")));

        // Get patient ID from session
        patientId = SessionManager.getPatientId();
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        pageTitle.setText("My Health Dashboard");
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 4;");

        Label greet = new Label(
            "Hello, " +
            LoginController.getLoggedInUser() + "! 🌟");
        greet.setStyle(
            "-fx-font-size:20px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#0F172A;");
        Label sub = new Label(
            "Here's a summary of your health records.");
        sub.setStyle(
            "-fx-font-size:13px;" +
            "-fx-text-fill:#64748B;");

        // Summary cards
        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            infoCard("📅", "My Appointments",
                getMyCount(
                    "SELECT COUNT(*) FROM appointments " +
                    "WHERE patient_id=?"),
                "#8B5CF6"),
            infoCard("🩺", "My Visits",
                getMyCount(
                    "SELECT COUNT(*) FROM medical_records " +
                    "WHERE patient_id=?"),
                "#059669"),
            infoCard("💰", "Pending Bills",
                getMyCount(
                    "SELECT COUNT(*) FROM billing " +
                    "WHERE patient_id=? " +
                    "AND payment_status='Unpaid'"),
                "#EF4444")
        );

        // Upcoming appointments
        Label apptTitle = new Label(
            "📅  My Upcoming Appointments");
        apptTitle.setStyle(
            "-fx-font-size:15px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#0F172A;");

        TableView<String[]> apptTable =
            buildMyAppointments();

        content.getChildren().addAll(
            greet, sub, cards, apptTitle, apptTable);

        contentArea.getChildren().setAll(
            new ScrollPane(content) {{
                setFitToWidth(true);
                setStyle(
                    "-fx-background-color:transparent;" +
                    "-fx-background:transparent;");
            }}
        );
    }

    private VBox infoCard(String icon, String title,
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
        Label iconL = new Label(icon + "  " + title);
        iconL.setStyle(
            "-fx-font-size:12px;" +
            "-fx-text-fill:#64748B;");
        Label valueL = new Label(value);
        valueL.setStyle(
            "-fx-font-size:32px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:" + color + ";");
        card.getChildren().addAll(iconL, valueL);
        return card;
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]> buildMyAppointments() {
        TableView<String[]> table = new TableView<>();
        table.setPrefHeight(220);
        table.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian," +
            "rgba(0,0,0,0.06),12,0,0,4);");

        TableColumn<String[], String> c1 =
            makeCol("Date", 0, 130);
        TableColumn<String[], String> c2 =
            makeCol("Time", 1, 100);
        TableColumn<String[], String> c3 =
            makeCol("Reason", 2, 200);
        TableColumn<String[], String> c4 =
            makeCol("Status", 3, 120);
        table.getColumns().addAll(c1, c2, c3, c4);

        if (patientId > 0) {
            try {
                PreparedStatement s =
                    DatabaseManager.getConnection()
                        .prepareStatement(
                            "SELECT appointment_date::text, " +
                            "appointment_time::text, " +
                            "COALESCE(reason,'—'), status " +
                            "FROM appointments " +
                            "WHERE patient_id=? " +
                            "AND appointment_date >= " +
                            "CURRENT_DATE " +
                            "ORDER BY appointment_date, " +
                            "appointment_time " +
                            "LIMIT 10");
                s.setInt(1, patientId);
                ResultSet rs = s.executeQuery();
                while (rs.next()) {
                    String d = rs.getString(1);
                    String t = rs.getString(2);
                    table.getItems().add(new String[]{
                        d != null && d.length() >= 10
                            ? d.substring(0,10) : "—",
                        t != null && t.length() >= 5
                            ? t.substring(0,5) : "—",
                        rs.getString(3),
                        rs.getString(4)
                    });
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }

        if (table.getItems().isEmpty()) {
            table.setPlaceholder(new Label(
                "No upcoming appointments."));
        }
        return table;
    }

    @FXML
    private void showMyAppointments() {
        pageTitle.setText("My Appointments");
        contentArea.getChildren().setAll(
            buildFullTable(
                "SELECT appointment_date::text, " +
                "appointment_time::text, " +
                "COALESCE(reason,'—'), status " +
                "FROM appointments WHERE patient_id=? " +
                "ORDER BY appointment_date DESC",
                new String[]{
                    "Date","Time","Reason","Status"},
                new int[]{130,100,200,120}
            )
        );
    }

    @FXML
    private void showMyRecords() {
        pageTitle.setText("My Medical Records");
        contentArea.getChildren().setAll(
            buildFullTable(
                "SELECT visit_date::text, " +
                "COALESCE(chief_complaint,'—'), " +
                "COALESCE(diagnosis,'—'), " +
                "COALESCE(blood_pressure,'—'), " +
                "COALESCE(prescription,'—') " +
                "FROM medical_records WHERE patient_id=? " +
                "ORDER BY visit_date DESC",
                new String[]{
                    "Date","Complaint",
                    "Diagnosis","BP","Prescription"},
                new int[]{120,160,160,80,180}
            )
        );
    }

    @FXML
    private void showMyBills() {
        pageTitle.setText("My Bills");
        contentArea.getChildren().setAll(
            buildFullTable(
                "SELECT created_at::text, " +
                "COALESCE(description,'—'), " +
                "(amount-discount-philhealth_amount)::text, " +
                "payment_status " +
                "FROM billing WHERE patient_id=? " +
                "ORDER BY created_at DESC",
                new String[]{
                    "Date","Description","Amount","Status"},
                new int[]{120,200,120,120}
            )
        );
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]> buildFullTable(
            String sql, String[] headers, int[] widths) {
        TableView<String[]> table = new TableView<>();
        table.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian," +
            "rgba(0,0,0,0.06),12,0,0,4);");

        for (int i = 0; i < headers.length; i++) {
            table.getColumns().add(
                makeCol(headers[i], i, widths[i]));
        }

        if (patientId > 0) {
            try {
                PreparedStatement s =
                    DatabaseManager.getConnection()
                        .prepareStatement(sql);
                s.setInt(1, patientId);
                ResultSet rs = s.executeQuery();
                while (rs.next()) {
                    String[] row =
                        new String[headers.length];
                    for (int i = 0; i < headers.length;
                            i++) {
                        String val = rs.getString(i + 1);
                        if (i == 0 && val != null
                                && val.length() >= 10) {
                            val = val.substring(0, 10);
                        }
                        row[i] = val != null ? val : "—";
                    }
                    table.getItems().add(row);
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }

        if (table.getItems().isEmpty()) {
            table.setPlaceholder(
                new Label("No records found."));
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

    private String getMyCount(String sql) {
        if (patientId <= 0) return "0";
        try {
            PreparedStatement s =
                DatabaseManager.getConnection()
                    .prepareStatement(sql);
            s.setInt(1, patientId);
            ResultSet rs = s.executeQuery();
            if (rs.next())
                return String.valueOf(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return "0";
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            Parent role = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/RoleSelection.fxml"));
            Stage stage = (Stage) contentArea
                .getScene().getWindow();
            stage.setScene(new Scene(role, 900, 580));
            stage.setTitle("BHC System — Select Role");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}