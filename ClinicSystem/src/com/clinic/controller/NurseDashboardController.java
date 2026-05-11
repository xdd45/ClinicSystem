package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import com.clinic.db.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NurseDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label     userLabel;
    @FXML private Label     roleLabel;
    @FXML private Label     pageTitle;
    @FXML private Label     clockLabel;

    @FXML
    public void initialize() {
        userLabel.setText(
            LoginController.getLoggedInUser());
        roleLabel.setText("Nurse / Staff");
        startClock();
        // Show assignments page first
        // since it's most important for nurse
        showMyAssignments();
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
            "Here's your shift summary.");
        sub.setStyle(
            "-fx-font-size:13px;" +
            "-fx-text-fill:#64748B;");

        // Stat cards
        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("📋", "My Pending Assignments",
                getCountForNurse(
                    "SELECT COUNT(*) FROM " +
                    "nurse_assignments " +
                    "WHERE nurse_id=? " +
                    "AND status='Pending'"),
                "#F59E0B"),
            statCard("⏳", "In Progress",
                getCountForNurse(
                    "SELECT COUNT(*) FROM " +
                    "nurse_assignments " +
                    "WHERE nurse_id=? " +
                    "AND status='In Progress'"),
                "#1D4ED8"),
            statCard("✅", "Completed Today",
                getCountForNurse(
                    "SELECT COUNT(*) FROM " +
                    "nurse_assignments " +
                    "WHERE nurse_id=? " +
                    "AND status='Completed' " +
                    "AND DATE(completed_at)=" +
                    "CURRENT_DATE"),
                "#059669"),
            statCard("👥", "Total Patients",
                getCount(
                    "SELECT COUNT(*) FROM patients"),
                "#8B5CF6")
        );

        content.getChildren().addAll(
            greet, sub, cards);

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

    // Count query with nurse_id parameter
    private String getCountForNurse(String sql) {
        try {
            PreparedStatement s =
                DatabaseManager.getConnection()
                    .prepareStatement(sql);
            s.setInt(1,
                SessionManager.getCurrentUserId());
            ResultSet rs = s.executeQuery();
            if (rs.next())
                return String.valueOf(
                    rs.getInt(1));
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return "0";
    }

    // Count query without parameter
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
    private void showMyAssignments() {
        loadPage(
            "/com/clinic/fxml/MyAssignments.fxml",
            "My Assignments");
    }

    @FXML
    private void showPatients() {
        loadPage(
            "/com/clinic/fxml/Patient.fxml",
            "Patients");
    }

    @FXML
    private void showAppointments() {
        loadPage(
            "/com/clinic/fxml/Appointment.fxml",
            "Appointments");
    }

    @FXML
    private void showInventory() {
        loadPage(
            "/com/clinic/fxml/Inventory.fxml",
            "Inventory");
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
                "⚠️ Error loading: " +
                e.getMessage());
            err.setStyle(
                "-fx-text-fill:#DC2626;" +
                "-fx-font-size:13px;");
            contentArea.getChildren().setAll(err);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
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