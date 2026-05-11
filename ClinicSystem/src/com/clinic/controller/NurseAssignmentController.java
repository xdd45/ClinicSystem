package com.clinic.controller;

import com.clinic.db.AuditLogger;
import com.clinic.db.DatabaseManager;
import com.clinic.db.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class NurseAssignmentController {

    @FXML private TableView<String[]>           assignmentTable;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colNurse;
    @FXML private TableColumn<String[], String> colPatient;
    @FXML private TableColumn<String[], String> colType;
    @FXML private TableColumn<String[], String> colPriority;
    @FXML private TableColumn<String[], String> colTask;
    @FXML private TableColumn<String[], String> colStatus;
    @FXML private TableColumn<String[], String> colDate;

    @FXML private ComboBox<String> nurseCombo;
    @FXML private ComboBox<String> patientCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> appointmentCombo;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterNurse;
    @FXML private TextArea         taskArea;
    @FXML private TextArea         notesArea;
    @FXML private Label            statusLabel;
    @FXML private Label            countLabel;
    @FXML private Label            formTitle;
    @FXML private Button           saveButton;

    private int editingId = -1;
    private Map<String, Integer> nurseMap       = new LinkedHashMap<>();
    private Map<String, Integer> patientMap     = new LinkedHashMap<>();
    private Map<String, Integer> appointmentMap = new LinkedHashMap<>();

    private ObservableList<String[]> masterList   = FXCollections.observableArrayList();
    private ObservableList<String[]> filteredList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        setupDropdowns();
        setupListeners();
        loadNurses();
        loadPatients();
        loadAssignments();
    }

    private void setupColumns() {
        colId.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[0]));
        colNurse.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[1]));
        colPatient.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[2]));
        colType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[3]));
        colPriority.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[4]));
        colTask.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[5]));
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[6]));
        colDate.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[7]));

        // Color code status column
        colStatus.setCellFactory(col ->
            new TableCell<String[], String>() {
                @Override
                protected void updateItem(
                        String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    setText(s);
                    if ("Pending".equals(s)) {
                        setStyle("-fx-text-fill:#D97706;" +
                            "-fx-font-weight:bold;");
                    } else if ("In Progress".equals(s)) {
                        setStyle("-fx-text-fill:#1D4ED8;" +
                            "-fx-font-weight:bold;");
                    } else if ("Completed".equals(s)) {
                        setStyle("-fx-text-fill:#059669;" +
                            "-fx-font-weight:bold;");
                    } else if ("Cancelled".equals(s)) {
                        setStyle("-fx-text-fill:#DC2626;" +
                            "-fx-font-weight:bold;");
                    } else {
                        setStyle("");
                    }
                }
            });

        // Color code priority column
        colPriority.setCellFactory(col ->
            new TableCell<String[], String>() {
                @Override
                protected void updateItem(
                        String p, boolean empty) {
                    super.updateItem(p, empty);
                    if (empty || p == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    setText(p);
                    if ("Urgent".equals(p)) {
                        setStyle("-fx-text-fill:#DC2626;" +
                            "-fx-font-weight:bold;");
                    } else if ("High".equals(p)) {
                        setStyle("-fx-text-fill:#D97706;" +
                            "-fx-font-weight:bold;");
                    } else {
                        setStyle("-fx-text-fill:#059669;");
                    }
                }
            });

        assignmentTable.setItems(filteredList);
        assignmentTable.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, o, n) -> {
                if (n != null) populateForm(n);
            });
    }

    private void setupDropdowns() {
        typeCombo.setItems(
            FXCollections.observableArrayList(
                "Appointment", "General Care",
                "Medication", "Monitoring",
                "Post-Op Care", "Other"
            ));
        typeCombo.setValue("General Care");

        priorityCombo.setItems(
            FXCollections.observableArrayList(
                "Normal", "High", "Urgent"
            ));
        priorityCombo.setValue("Normal");

        filterStatus.setItems(
            FXCollections.observableArrayList(
                "All", "Pending", "In Progress",
                "Completed", "Cancelled"
            ));
        filterStatus.setValue("All");
    }

    private void setupListeners() {
        filterStatus.valueProperty().addListener(
            (obs, o, n) -> applyFilter());
        filterNurse.valueProperty().addListener(
            (obs, o, n) -> applyFilter());

        // When patient is selected,
        // load their appointments
        patientCombo.valueProperty().addListener(
            (obs, o, n) -> {
                if (n != null) {
                    loadAppointmentsForPatient(
                        patientMap.get(n));
                }
            });
    }

    private void loadNurses() {
        nurseMap.clear();
        try {
            ResultSet rs = DatabaseManager
                .getConnection().prepareStatement(
                    "SELECT id, full_name FROM users " +
                    "WHERE role = 'Nurse' " +
                    "AND (is_active = true " +
                    "OR is_active IS NULL) " +
                    "ORDER BY full_name")
                .executeQuery();
            while (rs.next()) {
                nurseMap.put(
                    rs.getString("full_name"),
                    rs.getInt("id"));
            }
            nurseCombo.setItems(
                FXCollections.observableArrayList(
                    nurseMap.keySet()));

            // Also populate filter nurse combo
            ObservableList<String> nurseFilter =
                FXCollections.observableArrayList(
                    "All Nurses");
            nurseFilter.addAll(nurseMap.keySet());
            filterNurse.setItems(nurseFilter);
            filterNurse.setValue("All Nurses");

        } catch (SQLException e) {
            showStatus("Error loading nurses: " +
                e.getMessage(), true);
        }
    }

    private void loadPatients() {
        patientMap.clear();
        try {
            ResultSet rs = DatabaseManager
                .getConnection().prepareStatement(
                    "SELECT id, full_name FROM patients " +
                    "ORDER BY full_name")
                .executeQuery();
            while (rs.next()) {
                patientMap.put(
                    rs.getString("full_name"),
                    rs.getInt("id"));
            }
            patientCombo.setItems(
                FXCollections.observableArrayList(
                    patientMap.keySet()));
        } catch (SQLException e) {
            showStatus("Error loading patients: " +
                e.getMessage(), true);
        }
    }

    private void loadAppointmentsForPatient(
            int patientId) {
        appointmentMap.clear();
        appointmentCombo.getItems().clear();
        appointmentCombo.getItems().add(
            "No appointment link");

        try {
            PreparedStatement s =
                DatabaseManager.getConnection()
                    .prepareStatement(
                        "SELECT id, appointment_date::text, " +
                        "appointment_time::text, " +
                        "COALESCE(reason,'Visit') " +
                        "FROM appointments " +
                        "WHERE patient_id=? " +
                        "AND status='Scheduled' " +
                        "ORDER BY appointment_date");
            s.setInt(1, patientId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                String date = rs.getString(2);
                String time = rs.getString(3);
                String reason = rs.getString(4);
                String label =
                    (date != null && date.length() >= 10
                        ? date.substring(0, 10) : "—") +
                    " " +
                    (time != null && time.length() >= 5
                        ? time.substring(0, 5) : "") +
                    " — " + reason;
                appointmentMap.put(
                    label, rs.getInt(1));
                appointmentCombo.getItems().add(label);
            }
            appointmentCombo.setValue(
                "No appointment link");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private void loadAssignments() {
        masterList.clear();
        String sql =
            "SELECT na.id::text, " +
            "un.full_name AS nurse_name, " +
            "p.full_name AS patient_name, " +
            "na.assignment_type, " +
            "na.priority, " +
            "COALESCE(na.task_description,'—'), " +
            "na.status, " +
            "na.assigned_at::text " +
            "FROM nurse_assignments na " +
            "JOIN users un ON na.nurse_id = un.id " +
            "JOIN patients p ON na.patient_id = p.id " +
            "ORDER BY " +
            "CASE na.status " +
            "WHEN 'Pending' THEN 1 " +
            "WHEN 'In Progress' THEN 2 " +
            "WHEN 'Completed' THEN 3 " +
            "ELSE 4 END, " +
            "CASE na.priority " +
            "WHEN 'Urgent' THEN 1 " +
            "WHEN 'High' THEN 2 " +
            "ELSE 3 END, " +
            "na.assigned_at DESC";

        try {
            ResultSet rs = DatabaseManager
                .getConnection()
                .prepareStatement(sql)
                .executeQuery();
            while (rs.next()) {
                String date = rs.getString(8);
                masterList.add(new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    date != null && date.length() >= 10
                        ? date.substring(0, 10) : "—"
                });
            }
            applyFilter();
        } catch (SQLException e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    private void applyFilter() {
        String status = filterStatus.getValue();
        String nurse  = filterNurse.getValue();
        filteredList.clear();
        for (String[] row : masterList) {
            boolean matchStatus =
                "All".equals(status) ||
                status.equals(row[6]);
            boolean matchNurse =
                "All Nurses".equals(nurse) ||
                nurse == null ||
                nurse.equals(row[1]);
            if (matchStatus && matchNurse) {
                filteredList.add(row);
            }
        }
        countLabel.setText(
            filteredList.size() + " assignment(s)");
    }

    @FXML private void resetFilter() {
        filterStatus.setValue("All");
        filterNurse.setValue("All Nurses");
    }

    @FXML
    private void handleSave() {
        if (nurseCombo.getValue() == null) {
            showStatus("Please select a nurse.", true);
            return;
        }
        if (patientCombo.getValue() == null) {
            showStatus("Please select a patient.", true);
            return;
        }
        if (taskArea.getText().trim().isEmpty()) {
            showStatus(
                "Task description is required.", true);
            return;
        }

        String sql = editingId == -1
            ? "INSERT INTO nurse_assignments " +
              "(nurse_id, doctor_id, patient_id, " +
              "appointment_id, assignment_type, " +
              "task_description, priority, " +
              "status, notes) " +
              "VALUES (?,?,?,?,?,?,?,?,?)"
            : "UPDATE nurse_assignments SET " +
              "nurse_id=?, doctor_id=?, patient_id=?, " +
              "appointment_id=?, assignment_type=?, " +
              "task_description=?, priority=?, " +
              "status=?, notes=? WHERE id=?";

        try {
            PreparedStatement s =
                DatabaseManager.getConnection()
                    .prepareStatement(sql);

            s.setInt(1,
                nurseMap.get(nurseCombo.getValue()));
            s.setInt(2,
                SessionManager.getCurrentUserId());
            s.setInt(3,
                patientMap.get(patientCombo.getValue()));

            // Appointment link (optional)
            String apptVal =
                appointmentCombo.getValue();
            if (apptVal != null &&
                    appointmentMap.containsKey(apptVal)) {
                s.setInt(4,
                    appointmentMap.get(apptVal));
            } else {
                s.setNull(4, Types.INTEGER);
            }

            s.setString(5, typeCombo.getValue());
            s.setString(6,
                taskArea.getText().trim());
            s.setString(7,
                priorityCombo.getValue());
            s.setString(8, "Pending");
            s.setString(9,
                notesArea.getText().trim());

            if (editingId != -1)
                s.setInt(10, editingId);

            s.executeUpdate();

            AuditLogger.log(
                editingId == -1
                    ? "ASSIGN NURSE"
                    : "UPDATE ASSIGNMENT",
                "nurse_assignments",
                editingId,
                "Nurse: " + nurseCombo.getValue() +
                " → Patient: " + patientCombo.getValue()
            );

            showStatus(
                "✅ Nurse assigned successfully! " +
                nurseCombo.getValue() +
                " has been notified.", false);
            clearForm();
            loadAssignments();

        } catch (SQLException e) {
            showStatus("❌ Error: " +
                e.getMessage(), true);
        }
    }

    @FXML
    private void cancelAssignment() {
        String[] row = assignmentTable
            .getSelectionModel().getSelectedItem();
        if (row == null) {
            showStatus(
                "Select an assignment to cancel.",
                true);
            return;
        }

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Cancel this assignment?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(
            "Cancel Assignment for " + row[2]);

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    PreparedStatement s =
                        DatabaseManager.getConnection()
                            .prepareStatement(
                                "UPDATE nurse_assignments " +
                                "SET status='Cancelled' " +
                                "WHERE id=?");
                    s.setInt(1,
                        Integer.parseInt(row[0]));
                    s.executeUpdate();
                    showStatus(
                        "Assignment cancelled.", false);
                    clearForm();
                    loadAssignments();
                } catch (SQLException e) {
                    showStatus("❌ Error: " +
                        e.getMessage(), true);
                }
            }
        });
    }

    private void populateForm(String[] row) {
        editingId = Integer.parseInt(row[0]);
        formTitle.setText(
            "Assignment #" + editingId);
        nurseCombo.setValue(row[1]);
        patientCombo.setValue(row[2]);
        typeCombo.setValue(row[3]);
        priorityCombo.setValue(row[4]);
        taskArea.setText(
            "—".equals(row[5]) ? "" : row[5]);
        saveButton.setText(
            "💾  Update Assignment");
    }

    @FXML
    public void clearForm() {
        editingId = -1;
        formTitle.setText("New Assignment");
        nurseCombo.setValue(null);
        patientCombo.setValue(null);
        typeCombo.setValue("General Care");
        appointmentCombo.getItems().clear();
        appointmentCombo.getItems().add(
            "No appointment link");
        appointmentCombo.setValue(
            "No appointment link");
        priorityCombo.setValue("Normal");
        taskArea.clear();
        notesArea.clear();
        assignmentTable.getSelectionModel()
            .clearSelection();
        saveButton.setText("📋  Assign Nurse");
        statusLabel.setText("");
    }

    private void showStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill:" +
            (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size:12px;" +
            "-fx-padding:6 10;" +
            "-fx-background-color:" +
            (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius:6;"
        );
    }
}