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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NurseDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label     userLabel;
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
        pageTitle.setText("Nurse Dashboard");
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 4;");

        Label greet = new Label(
            "Welcome, " +
            LoginController.getLoggedInUser() + "! 👋");
        greet.setStyle(
            "-fx-font-size:20px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#0F172A;");

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("👥", "Total Patients",
                getCount("SELECT COUNT(*) FROM patients"),
                "#F59E0B"),
            statCard("📅", "Today's Appointments",
                getCount(
                    "SELECT COUNT(*) FROM appointments " +
                    "WHERE appointment_date=CURRENT_DATE"),
                "#1D4ED8"),
            statCard("💊", "Low Stock Items",
                getCount(
                    "SELECT COUNT(*) FROM inventory " +
                    "WHERE quantity<=reorder_level"),
                "#EF4444")
        );

        content.getChildren().addAll(greet, cards);
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
        Label iconL = new Label(icon);
        iconL.setStyle("-fx-font-size:20px;");
        Label titleL = new Label(title);
        titleL.setStyle(
            "-fx-font-size:12px;" +
            "-fx-text-fill:#64748B;");
        Label valueL = new Label(value);
        valueL.setStyle(
            "-fx-font-size:32px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:" + color + ";");
        card.getChildren().addAll(
            new HBox(8, iconL, titleL), valueL);
        return card;
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
            "Patients");
    }
    @FXML private void showAppointments() {
        loadPage("/com/clinic/fxml/Appointment.fxml",
            "Appointments");
    }
    @FXML private void showInventory() {
        loadPage("/com/clinic/fxml/Inventory.fxml",
            "Inventory");
    }

    private void loadPage(String path, String title) {
        pageTitle.setText(title);
        try {
            Parent page = FXMLLoader.load(
                getClass().getResource(path));
            contentArea.getChildren().setAll(page);
        } catch (Exception e) {
            contentArea.getChildren().setAll(
                new Label("⚠️ Error: " + e.getMessage()));
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}