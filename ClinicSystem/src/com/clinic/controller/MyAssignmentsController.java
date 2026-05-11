package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import com.clinic.db.SessionManager;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyAssignmentsController {

    @FXML private VBox             assignmentCards;
    @FXML private ComboBox<String> filterStatus;
    @FXML private Label            pendingBadge;
    @FXML private Label            countLabel;
    @FXML private Label            statusLabel;

    private List<String[]> allAssignments =
        new ArrayList<>();

    @FXML
    public void initialize() {
        filterStatus.setItems(
            FXCollections.observableArrayList(
                "All", "Pending",
                "In Progress", "Completed",
                "Cancelled"
            ));
        filterStatus.setValue("All");
        filterStatus.valueProperty().addListener(
            (obs, o, n) -> renderCards());

        loadAssignments();
        startAutoRefresh();
    }

    /**
     * Auto refresh every 30 seconds to catch
     * new assignments from doctor in real time.
     */
    private void startAutoRefresh() {
        javafx.animation.Timeline refresh =
            new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.seconds(30),
                    e -> loadAssignments()
                )
            );
        refresh.setCycleCount(
            javafx.animation.Animation.INDEFINITE);
        refresh.play();
    }

    @FXML
    public void loadAssignments() {
        allAssignments.clear();
        int nurseId =
            SessionManager.getCurrentUserId();

        String sql =
            "SELECT na.id::text, " +
            "p.full_name AS patient, " +
            "ud.full_name AS doctor, " +
            "na.assignment_type, " +
            "na.priority, " +
            "COALESCE(na.task_description,'—'), " +
            "na.status, " +
            "na.assigned_at::text, " +
            "COALESCE(na.notes,''), " +
            "COALESCE(a.appointment_date::text,'') " +
            "FROM nurse_assignments na " +
            "JOIN patients p " +
            "ON na.patient_id = p.id " +
            "JOIN users ud " +
            "ON na.doctor_id = ud.id " +
            "LEFT JOIN appointments a " +
            "ON na.appointment_id = a.id " +
            "WHERE na.nurse_id = ? " +
            "ORDER BY " +
            "CASE na.status " +
            "WHEN 'Pending' THEN 1 " +
            "WHEN 'In Progress' THEN 2 " +
            "WHEN 'Completed' THEN 3 " +
            "ELSE 4 END, " +
            "CASE na.priority " +
            "WHEN 'Urgent' THEN 1 " +
            "WHEN 'High' THEN 2 " +
            "ELSE 3 END";

        try {
            PreparedStatement s =
                DatabaseManager.getConnection()
                    .prepareStatement(sql);
            s.setInt(1, nurseId);
            ResultSet rs = s.executeQuery();

            int pendingCount = 0;
            while (rs.next()) {
                String[] row = new String[10];
                for (int i = 0; i < 10; i++) {
                    row[i] = rs.getString(i + 1);
                    if (row[i] == null) row[i] = "";
                }
                allAssignments.add(row);
                if ("Pending".equals(row[6]))
                    pendingCount++;
            }

            final int pc = pendingCount;
            Platform.runLater(() -> {
                renderCards();

                // Show pending badge
                if (pc > 0) {
                    pendingBadge.setText(
                        "⚠️ " + pc + " Pending!");
                    pendingBadge.setVisible(true);

                    // Show popup for new assignments
                    showNewAssignmentAlert(pc);
                } else {
                    pendingBadge.setText("");
                    pendingBadge.setVisible(false);
                }
            });

        } catch (SQLException e) {
            Platform.runLater(() ->
                showStatus("Error: " +
                    e.getMessage(), true));
        }
    }

    private void showNewAssignmentAlert(int count) {
        // Only show popup if there are pending items
        Alert alert = new Alert(
            Alert.AlertType.INFORMATION);
        alert.setTitle("New Assignments");
        alert.setHeaderText(
            "📋 You have " + count +
            " pending assignment(s)!");
        alert.setContentText(
            "The doctor has assigned you " +
            count + " patient(s) to assist.\n" +
            "Please review and accept your assignments.");
        alert.getDialogPane().setStyle(
            "-fx-font-size: 13px;");

        // Non-blocking show
        alert.show();

        // Auto close after 4 seconds
        javafx.animation.Timeline close =
            new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.seconds(4),
                    e -> alert.close()
                )
            );
        close.play();
    }

    private void renderCards() {
        assignmentCards.getChildren().clear();
        String filter = filterStatus.getValue();

        int shown = 0;
        for (String[] row : allAssignments) {
            if (!"All".equals(filter) &&
                    !filter.equals(row[6])) {
                continue;
            }
            assignmentCards.getChildren().add(
                buildCard(row));
            shown++;
        }

        countLabel.setText(shown + " assignment(s)");

        if (shown == 0) {
            Label empty = new Label(
                "All assignments are up to date! ✅");
            empty.setStyle(
                "-fx-font-size:14px;" +
                "-fx-text-fill:#64748B;" +
                "-fx-padding:20;");
            assignmentCards.getChildren().add(empty);
        }
    }

    /**
     * Builds a visual card for each assignment.
     */
    private VBox buildCard(String[] row) {
        // id=0, patient=1, doctor=2, type=3,
        // priority=4, task=5, status=6,
        // date=7, notes=8, apptDate=9

        String status   = row[6];
        String priority = row[4];

        // Card border color based on status
        String borderColor;
        if ("Pending".equals(status)) {
            borderColor = "#F59E0B";
        } else if ("In Progress".equals(status)) {
            borderColor = "#1D4ED8";
        } else if ("Completed".equals(status)) {
            borderColor = "#059669";
        } else {
            borderColor = "#DC2626";
        }

        VBox card = new VBox(10);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 16;" +
            "-fx-effect: dropshadow(gaussian," +
            "rgba(0,0,0,0.07),10,0,0,3);" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 0 0 0 5;" +
            "-fx-border-radius: 0 12 12 0;"
        );

        // TOP ROW — Patient name + Status badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(
            javafx.geometry.Pos.CENTER_LEFT);

        Label patientLbl = new Label(
            "👤  " + row[1]);
        patientLbl.setStyle(
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #0F172A;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Priority badge
        String priorityColor =
            "Urgent".equals(priority) ? "#FEE2E2:#DC2626"
            : "High".equals(priority) ? "#FEF3C7:#D97706"
            : "#D1FAE5:#059669";
        String[] pc = priorityColor.split(":");

        Label priorityBadge = new Label(priority);
        priorityBadge.setStyle(
            "-fx-background-color:" + pc[0] + ";" +
            "-fx-text-fill:" + pc[1] + ";" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 3 10;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;"
        );

        // Status badge
        Label statusBadge = new Label(status);
        statusBadge.setStyle(
            "-fx-background-color:" + borderColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 3 10;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;"
        );

        topRow.getChildren().addAll(
            patientLbl, spacer,
            priorityBadge, statusBadge);

        // INFO ROW
        HBox infoRow = new HBox(20);
        infoRow.setAlignment(
            javafx.geometry.Pos.CENTER_LEFT);
        infoRow.getChildren().addAll(
            infoLabel("👨‍⚕️ Dr:", row[2]),
            infoLabel("📋 Type:", row[3]),
            infoLabel("📅 Assigned:", row[7].length() >= 10
                ? row[7].substring(0, 10) : row[7])
        );

        if (!row[9].isEmpty()) {
            infoRow.getChildren().add(
                infoLabel("🗓️ Appt:", row[9]));
        }

        // TASK
        Label taskTitle = new Label("Task:");
        taskTitle.setStyle(
            "-fx-font-size:11px;" +
            "-fx-text-fill:#64748B;" +
            "-fx-font-weight:bold;");
        Label taskLbl = new Label(row[5]);
        taskLbl.setWrapText(true);
        taskLbl.setStyle(
            "-fx-font-size:13px;" +
            "-fx-text-fill:#334155;");

        // NOTES
        VBox notesBox = new VBox(2);
        if (!row[8].isEmpty()) {
            Label notesTitle = new Label("Notes:");
            notesTitle.setStyle(
                "-fx-font-size:11px;" +
                "-fx-text-fill:#64748B;" +
                "-fx-font-weight:bold;");
            Label notesLbl = new Label(row[8]);
            notesLbl.setWrapText(true);
            notesLbl.setStyle(
                "-fx-font-size:12px;" +
                "-fx-text-fill:#64748B;");
            notesBox.getChildren().addAll(
                notesTitle, notesLbl);
        }

        // ACTION BUTTONS
        HBox actions = new HBox(8);
        actions.setAlignment(
            javafx.geometry.Pos.CENTER_LEFT);

        if ("Pending".equals(status)) {
            Button acceptBtn = new Button(
                "✅  Accept");
            acceptBtn.setStyle(
                "-fx-background-color: #1D4ED8;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 8 16;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-font-weight: bold;");
            acceptBtn.setOnAction(e ->
                updateStatus(row[0], "In Progress",
                    "Assignment accepted!"));
            actions.getChildren().add(acceptBtn);
        }

        if ("In Progress".equals(status)) {
            Button completeBtn = new Button(
                "🏁  Mark Complete");
            completeBtn.setStyle(
                "-fx-background-color: #059669;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 8 16;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-font-weight: bold;");
            completeBtn.setOnAction(e ->
                updateStatus(row[0], "Completed",
                    "Marked as completed!"));
            actions.getChildren().add(completeBtn);
        }

        if ("Pending".equals(status) ||
                "In Progress".equals(status)) {
            Button cancelBtn = new Button(
                "❌  Cancel");
            cancelBtn.setStyle(
                "-fx-background-color: #FEE2E2;" +
                "-fx-text-fill: #DC2626;" +
                "-fx-padding: 8 16;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;");
            cancelBtn.setOnAction(e -> {
                Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Cancel this assignment?",
                    ButtonType.OK,
                    ButtonType.CANCEL);
                confirm.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.OK) {
                        updateStatus(
                            row[0], "Cancelled",
                            "Assignment cancelled.");
                    }
                });
            });
            actions.getChildren().add(cancelBtn);
        }

        if ("Completed".equals(status)) {
            Label doneLabel = new Label(
                "✅ This assignment is complete.");
            doneLabel.setStyle(
                "-fx-text-fill: #059669;" +
                "-fx-font-size: 12px;");
            actions.getChildren().add(doneLabel);
        }

        card.getChildren().addAll(
            topRow, infoRow,
            taskTitle, taskLbl);

        if (!row[8].isEmpty()) {
            card.getChildren().add(notesBox);
        }

        card.getChildren().add(actions);
        return card;
    }

    private Label infoLabel(
            String title, String value) {
        Label lbl = new Label(title + " " + value);
        lbl.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #64748B;");
        return lbl;
    }

    private void updateStatus(
            String id, String newStatus,
            String successMsg) {
        try {
            String sql;
            if ("In Progress".equals(newStatus)) {
                sql = "UPDATE nurse_assignments SET " +
                      "status=?, accepted_at=NOW() " +
                      "WHERE id=?";
            } else if ("Completed".equals(newStatus)) {
                sql = "UPDATE nurse_assignments SET " +
                      "status=?, completed_at=NOW() " +
                      "WHERE id=?";
            } else {
                sql = "UPDATE nurse_assignments SET " +
                      "status=? WHERE id=?";
            }

            PreparedStatement s =
                DatabaseManager.getConnection()
                    .prepareStatement(sql);
            s.setString(1, newStatus);
            s.setInt(2, Integer.parseInt(id));
            s.executeUpdate();

            showStatus("✅ " + successMsg, false);
            loadAssignments();

        } catch (SQLException e) {
            showStatus("❌ Error: " +
                e.getMessage(), true);
        }
    }

    private void showStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill:" +
            (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size:13px;" +
            "-fx-padding:8 12;" +
            "-fx-background-color:" +
            (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius:6;"
        );
    }
}