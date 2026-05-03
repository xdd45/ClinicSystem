package com.clinic.controller;

import com.clinic.db.AuditLogger;
import com.clinic.db.DatabaseManager;
import com.clinic.model.Appointment;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AppointmentController {

    @FXML private TableView<Appointment>            apptTable;
    @FXML private TableColumn<Appointment, Integer> colId;
    @FXML private TableColumn<Appointment, String>  colPatient;
    @FXML private TableColumn<Appointment, String>  colDate;
    @FXML private TableColumn<Appointment, String>  colTime;
    @FXML private TableColumn<Appointment, String>  colReason;
    @FXML private TableColumn<Appointment, String>  colStatus;

    @FXML private ComboBox<String> patientCombo;
    @FXML private DatePicker       datePicker;
    @FXML private ComboBox<String> timeCombo;
    @FXML private TextField        reasonField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea         notesArea;
    @FXML private TextField        searchField;
    @FXML private DatePicker       filterDate;
    @FXML private ComboBox<String> filterStatus;
    @FXML private Label            statusLabel;
    @FXML private Label            countLabel;
    @FXML private Label            formTitle;
    @FXML private Button           saveButton;

    private int editingId = -1;
    private ObservableList<Appointment> masterList   = FXCollections.observableArrayList();
    private ObservableList<Appointment> filteredList = FXCollections.observableArrayList();

    // Maps patient name to ID for the combo box
    private java.util.Map<String, Integer> patientMap = new java.util.LinkedHashMap<>();

    @FXML
    public void initialize() {
        setupColumns();
        setupDropdowns();
        setupListeners();
        loadPatients();
        loadAppointments();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));

        // Format date nicely
        colDate.setCellValueFactory(data -> {
            LocalDate d = data.getValue().getAppointmentDate();
            if (d == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(
                d.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        });

        // Format time as 12-hour
        colTime.setCellValueFactory(data -> {
            LocalTime t = data.getValue().getAppointmentTime();
            if (t == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(
                t.format(DateTimeFormatter.ofPattern("hh:mm a")));
        });

        // Color-coded status
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                switch (status) {
                    case "Scheduled"  -> setStyle("-fx-text-fill: #1D4ED8; -fx-font-weight: bold;");
                    case "Completed"  -> setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    case "Cancelled"  -> setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    default           -> setStyle("");
                }
            }
        });

        apptTable.setItems(filteredList);
        apptTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, o, n) -> { if (n != null) populateForm(n); }
        );
    }

    private void setupDropdowns() {
        // Time slots every 30 minutes
        ObservableList<String> times = FXCollections.observableArrayList();
        for (int h = 7; h <= 17; h++) {
            times.add(String.format("%02d:00", h));
            times.add(String.format("%02d:30", h));
        }
        timeCombo.setItems(times);

        statusCombo.setItems(FXCollections.observableArrayList(
            "Scheduled", "Completed", "Cancelled"
        ));
        statusCombo.setValue("Scheduled");

        filterStatus.setItems(FXCollections.observableArrayList(
            "All", "Scheduled", "Completed", "Cancelled"
        ));
        filterStatus.setValue("All");
    }

    private void setupListeners() {
        searchField.textProperty().addListener((o, old, n) -> applyFilter());
        filterDate.valueProperty().addListener((o, old, n) -> applyFilter());
        filterStatus.valueProperty().addListener((o, old, n) -> applyFilter());
    }

    private void loadPatients() {
        patientMap.clear();
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement("SELECT id, full_name FROM patients ORDER BY full_name")
                .executeQuery();
            while (rs.next()) {
                patientMap.put(rs.getString("full_name"), rs.getInt("id"));
            }
            patientCombo.setItems(
                FXCollections.observableArrayList(patientMap.keySet())
            );
        } catch (SQLException e) {
            showStatus("Error loading patients: " + e.getMessage(), true);
        }
    }

    private void loadAppointments() {
        masterList.clear();
        String sql =
            "SELECT a.id, a.patient_id, p.full_name, a.user_id, " +
            "a.appointment_date, a.appointment_time, a.reason, a.status, a.notes " +
            "FROM appointments a JOIN patients p ON a.patient_id = p.id " +
            "ORDER BY a.appointment_date DESC, a.appointment_time";
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            while (rs.next()) {
                masterList.add(new Appointment(
                    rs.getInt("id"),
                    rs.getInt("patient_id"),
                    rs.getString("full_name"),
                    rs.getInt("user_id"),
                    rs.getDate("appointment_date") != null ?
                        rs.getDate("appointment_date").toLocalDate() : null,
                    rs.getTime("appointment_time") != null ?
                        rs.getTime("appointment_time").toLocalTime() : null,
                    rs.getString("reason"),
                    rs.getString("status"),
                    rs.getString("notes")
                ));
            }
            applyFilter();
        } catch (SQLException e) {
            showStatus("Error loading: " + e.getMessage(), true);
        }
    }

    private void applyFilter() {
        String kw     = searchField.getText().trim().toLowerCase();
        LocalDate date = filterDate.getValue();
        String status  = filterStatus.getValue();

        filteredList.clear();
        masterList.stream()
            .filter(a ->
                (kw.isEmpty() || a.getPatientName().toLowerCase().contains(kw)) &&
                (date == null || date.equals(a.getAppointmentDate())) &&
                ("All".equals(status) || status.equals(a.getStatus()))
            )
            .forEach(filteredList::add);

        countLabel.setText(filteredList.size() + " appointment(s)");
    }

    @FXML private void resetFilter() {
        searchField.clear();
        filterDate.setValue(null);
        filterStatus.setValue("All");
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        // Check for time conflicts
        if (hasTimeConflict()) {
            showStatus("⚠️ Time slot is already booked! Please choose another time.", true);
            return;
        }

        if (editingId == -1) insertAppointment();
        else updateAppointment();
    }

    private void insertAppointment() {
        String sql = "INSERT INTO appointments " +
            "(patient_id, user_id, appointment_date, appointment_time, reason, status, notes) " +
            "VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql);
            setParams(s);
            s.executeUpdate();

            AuditLogger.log("ADD APPOINTMENT", "appointments", 0,
                "Appointment for: " + patientCombo.getValue());

            showStatus("✅ Appointment scheduled!", false);
            clearForm();
            loadAppointments();
        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    private void updateAppointment() {
        String sql = "UPDATE appointments SET " +
            "patient_id=?, user_id=?, appointment_date=?, " +
            "appointment_time=?, reason=?, status=?, notes=? WHERE id=?";
        try {
            PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql);
            setParams(s);
            s.setInt(8, editingId);
            s.executeUpdate();

            AuditLogger.log("UPDATE APPOINTMENT", "appointments", editingId,
                "Updated appointment #" + editingId);

            showStatus("✅ Appointment updated!", false);
            clearForm();
            loadAppointments();
        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    private void setParams(PreparedStatement s) throws SQLException {
        s.setInt(1, patientMap.get(patientCombo.getValue()));
        s.setInt(2, com.clinic.db.SessionManager.getCurrentUserId());
        s.setDate(3, Date.valueOf(datePicker.getValue()));
        s.setTime(4, Time.valueOf(
            LocalTime.parse(timeCombo.getValue(),
                DateTimeFormatter.ofPattern("HH:mm"))
        ));
        s.setString(5, reasonField.getText().trim());
        s.setString(6, statusCombo.getValue());
        s.setString(7, notesArea.getText().trim());
    }

    @FXML private void markComplete()   { updateStatus("Completed"); }
    @FXML private void markCancelled()  { updateStatus("Cancelled"); }

    private void updateStatus(String status) {
        Appointment a = apptTable.getSelectionModel().getSelectedItem();
        if (a == null) { showStatus("Select an appointment first.", true); return; }
        try {
            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement("UPDATE appointments SET status=? WHERE id=?");
            s.setString(1, status);
            s.setInt(2, a.getId());
            s.executeUpdate();
            showStatus("Status updated to: " + status, false);
            clearForm();
            loadAppointments();
        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    private boolean hasTimeConflict() {
        if (datePicker.getValue() == null || timeCombo.getValue() == null) return false;
        try {
            String sql = editingId == -1
                ? "SELECT COUNT(*) FROM appointments WHERE appointment_date=? AND appointment_time=? AND status='Scheduled'"
                : "SELECT COUNT(*) FROM appointments WHERE appointment_date=? AND appointment_time=? AND status='Scheduled' AND id!=?";
            PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql);
            s.setDate(1, Date.valueOf(datePicker.getValue()));
            s.setTime(2, Time.valueOf(LocalTime.parse(timeCombo.getValue(),
                DateTimeFormatter.ofPattern("HH:mm"))));
            if (editingId != -1) s.setInt(3, editingId);
            ResultSet rs = s.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) { return false; }
    }

    private void populateForm(Appointment a) {
        editingId = a.getId();
        formTitle.setText("Edit Appointment #" + a.getId());
        patientCombo.setValue(a.getPatientName());
        datePicker.setValue(a.getAppointmentDate());
        if (a.getAppointmentTime() != null)
            timeCombo.setValue(a.getAppointmentTime()
                .format(DateTimeFormatter.ofPattern("HH:mm")));
        reasonField.setText(a.getReason() != null ? a.getReason() : "");
        statusCombo.setValue(a.getStatus());
        notesArea.setText(a.getNotes() != null ? a.getNotes() : "");
        saveButton.setText("💾  Update Appointment");
    }

    @FXML
    public void clearForm() {
        editingId = -1;
        formTitle.setText("New Appointment");
        patientCombo.setValue(null);
        datePicker.setValue(LocalDate.now());
        timeCombo.setValue(null);
        reasonField.clear();
        statusCombo.setValue("Scheduled");
        notesArea.clear();
        apptTable.getSelectionModel().clearSelection();
        saveButton.setText("💾  Save Appointment");
        statusLabel.setText("");
    }

    private boolean validate() {
        StringBuilder err = new StringBuilder();
        if (patientCombo.getValue() == null) err.append("• Please select a patient.\n");
        if (datePicker.getValue() == null) err.append("• Date is required.\n");
        if (datePicker.getValue() != null && datePicker.getValue().isBefore(LocalDate.now())
            && editingId == -1)
            err.append("• Cannot schedule appointment in the past.\n");
        if (timeCombo.getValue() == null) err.append("• Please select a time slot.\n");
        if (err.length() > 0) { showStatus(err.toString(), true); return false; }
        return true;
    }

    private void showStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill: " + (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size: 12px; -fx-padding: 6 10;" +
            "-fx-background-color: " + (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius: 6;"
        );
    }
}