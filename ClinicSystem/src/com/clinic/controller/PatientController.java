package com.clinic.controller;

import com.clinic.db.AuditLogger;
import com.clinic.db.DatabaseManager;
import com.clinic.model.Patient;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;

public class PatientController {

    @FXML private TableView<Patient>            patientTable;
    @FXML private TableColumn<Patient, Integer> colId;
    @FXML private TableColumn<Patient, String>  colName;
    @FXML private TableColumn<Patient, String>  colAge;
    @FXML private TableColumn<Patient, String>  colGender;
    @FXML private TableColumn<Patient, String>  colContact;
    @FXML private TableColumn<Patient, String>  colEmail;
    @FXML private TableColumn<Patient, String>  colDate;

    @FXML private TextField        nameField;
    @FXML private DatePicker       dobPicker;
    @FXML private ComboBox<String> genderCombo;
    @FXML private TextField        contactField;
    @FXML private TextArea         addressArea;
    @FXML private TextField        emailField;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private TextField        philhealthField;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterGender;
    @FXML private Label            statusLabel;
    @FXML private Label            countLabel;
    @FXML private Label            formTitle;
    @FXML private Button           saveButton;

    private int editingId = -1;
    private ObservableList<Patient> masterList = FXCollections.observableArrayList();
    private ObservableList<Patient> filteredList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupDropdowns();
        setupListeners();
        loadPatients();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Age column — calculated from date of birth
        colAge.setCellValueFactory(data -> {
            LocalDate dob = data.getValue().getDateOfBirth();
            if (dob == null) return new SimpleStringProperty("—");
            int age = Period.between(dob, LocalDate.now()).getYears();
            return new SimpleStringProperty(age + " yrs");
        });

        // Date column — formatted nicely
        colDate.setCellValueFactory(data -> {
            String raw = data.getValue().getCreatedAt();
            if (raw == null) return new SimpleStringProperty("—");
            try {
                return new SimpleStringProperty(raw.substring(0, 10));
            } catch (Exception e) {
                return new SimpleStringProperty(raw);
            }
        });

        patientTable.setItems(filteredList);

        // Click row to populate form
        patientTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> { if (newVal != null) populateForm(newVal); }
        );
    }

    private void setupDropdowns() {
        genderCombo.setItems(FXCollections.observableArrayList(
            "Male", "Female", "Other"
        ));
        bloodTypeCombo.setItems(FXCollections.observableArrayList(
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown"
        ));
        filterGender.setItems(FXCollections.observableArrayList(
            "All Genders", "Male", "Female", "Other"
        ));
        filterGender.setValue("All Genders");
    }

    private void setupListeners() {
        // Live search as user types
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        filterGender.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void loadPatients() {
        masterList.clear();
        String sql = "SELECT *, created_at::text FROM patients ORDER BY full_name";

        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            while (rs.next()) {
                Patient p = new Patient(
                    rs.getInt("id"),
                    rs.getString("full_name"),
                    rs.getDate("date_of_birth") != null ?
                        rs.getDate("date_of_birth").toLocalDate() : null,
                    rs.getString("gender"),
                    rs.getString("contact_number"),
                    rs.getString("address"),
                    rs.getString("email")
                );
                p.setBloodType(rs.getString("blood_type"));
                p.setPhilhealthNumber(rs.getString("philhealth_number"));
                p.setCreatedAt(rs.getString("created_at"));
                masterList.add(p);
            }
            applyFilter();
        } catch (SQLException e) {
            showStatus("Error loading: " + e.getMessage(), true);
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText().trim().toLowerCase();
        String gender  = filterGender.getValue();

        filteredList.clear();
        masterList.stream()
            .filter(p -> {
                boolean matchesKeyword = keyword.isEmpty()
                    || p.getFullName().toLowerCase().contains(keyword)
                    || (p.getContactNumber() != null && p.getContactNumber().contains(keyword))
                    || (p.getEmail() != null && p.getEmail().toLowerCase().contains(keyword));

                boolean matchesGender = gender == null
                    || gender.equals("All Genders")
                    || gender.equals(p.getGender());

                return matchesKeyword && matchesGender;
            })
            .forEach(filteredList::add);

        countLabel.setText(filteredList.size() + " patient(s) found");
    }

    @FXML
    private void resetSearch() {
        searchField.clear();
        filterGender.setValue("All Genders");
        applyFilter();
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        // First check if philhealth number already exists for another patient
        if (!philhealthField.getText().trim().isEmpty()) {
            if (isDuplicatePhilhealth()) {
                showStatus("PhilHealth number already registered to another patient!", true);
                return;
            }
        }

        if (editingId == -1) insertPatient();
        else updatePatient();
    }

    private void insertPatient() {
        String sql = "INSERT INTO patients " +
            "(full_name, date_of_birth, gender, contact_number, address, email, blood_type, philhealth_number) " +
            "VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql);
            setParams(s);
            s.executeUpdate();

            // Log the action
            AuditLogger.log("ADD PATIENT", "patients", 0,
                "Added patient: " + nameField.getText().trim());

            showStatus("✅ Patient added successfully!", false);
            clearForm();
            loadPatients();
        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    private void updatePatient() {
        String sql = "UPDATE patients SET " +
            "full_name=?, date_of_birth=?, gender=?, contact_number=?, " +
            "address=?, email=?, blood_type=?, philhealth_number=? WHERE id=?";
        try {
            PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql);
            setParams(s);
            s.setInt(9, editingId);
            s.executeUpdate();

            AuditLogger.log("UPDATE PATIENT", "patients", editingId,
                "Updated patient: " + nameField.getText().trim());

            showStatus("✅ Patient updated successfully!", false);
            clearForm();
            loadPatients();
        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    private void setParams(PreparedStatement s) throws SQLException {
        s.setString(1, nameField.getText().trim());
        if (dobPicker.getValue() != null)
            s.setDate(2, Date.valueOf(dobPicker.getValue()));
        else s.setNull(2, Types.DATE);
        s.setString(3, genderCombo.getValue());
        s.setString(4, contactField.getText().trim());
        s.setString(5, addressArea.getText().trim());
        s.setString(6, emailField.getText().trim());
        s.setString(7, bloodTypeCombo.getValue());
        s.setString(8, philhealthField.getText().trim());
    }

    @FXML
    private void handleDelete() {
        Patient p = patientTable.getSelectionModel().getSelectedItem();
        if (p == null) { showStatus("Please select a patient first.", true); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete " + p.getFullName() + "?");
        confirm.setContentText(
            "This will permanently delete all records, appointments,\n" +
            "and medical history for this patient.\n\n" +
            "This action CANNOT be undone!"
        );

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    PreparedStatement s = DatabaseManager.getConnection()
                        .prepareStatement("DELETE FROM patients WHERE id=?");
                    s.setInt(1, p.getId());
                    s.executeUpdate();

                    AuditLogger.log("DELETE PATIENT", "patients", p.getId(),
                        "Deleted patient: " + p.getFullName());

                    showStatus("Patient deleted.", false);
                    clearForm();
                    loadPatients();
                } catch (SQLException e) {
                    showStatus("❌ Error: " + e.getMessage(), true);
                }
            }
        });
    }

    private void populateForm(Patient p) {
        editingId = p.getId();
        formTitle.setText("Edit Patient #" + p.getId());
        nameField.setText(p.getFullName());
        dobPicker.setValue(p.getDateOfBirth());
        genderCombo.setValue(p.getGender());
        contactField.setText(p.getContactNumber() != null ? p.getContactNumber() : "");
        addressArea.setText(p.getAddress() != null ? p.getAddress() : "");
        emailField.setText(p.getEmail() != null ? p.getEmail() : "");
        bloodTypeCombo.setValue(p.getBloodType());
        philhealthField.setText(p.getPhilhealthNumber() != null ? p.getPhilhealthNumber() : "");
        saveButton.setText("💾  Update Patient");
        statusLabel.setText("");
    }

    @FXML
    public void clearForm() {
        editingId = -1;
        formTitle.setText("New Patient");
        nameField.clear();
        dobPicker.setValue(null);
        genderCombo.setValue(null);
        contactField.clear();
        addressArea.clear();
        emailField.clear();
        bloodTypeCombo.setValue(null);
        philhealthField.clear();
        patientTable.getSelectionModel().clearSelection();
        saveButton.setText("💾  Save Patient");
        statusLabel.setText("");
        nameField.requestFocus();
    }

    private boolean validate() {
        StringBuilder err = new StringBuilder();
        if (nameField.getText().trim().isEmpty())
            err.append("• Full name is required.\n");
        if (dobPicker.getValue() == null)
            err.append("• Date of birth is required.\n");
        if (dobPicker.getValue() != null && dobPicker.getValue().isAfter(LocalDate.now()))
            err.append("• Date of birth cannot be in the future.\n");
        if (genderCombo.getValue() == null)
            err.append("• Gender is required.\n");
        if (!contactField.getText().trim().isEmpty() &&
            !contactField.getText().trim().matches("^[0-9+\\-\\s]{7,15}$"))
            err.append("• Contact number format is invalid.\n");
        if (!emailField.getText().trim().isEmpty() &&
            !emailField.getText().trim().matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$"))
            err.append("• Email format is invalid.\n");

        if (err.length() > 0) { showStatus(err.toString(), true); return false; }
        return true;
    }

    private boolean isDuplicatePhilhealth() {
        String num = philhealthField.getText().trim();
        if (num.isEmpty()) return false;
        try {
            String sql = editingId == -1
                ? "SELECT COUNT(*) FROM patients WHERE philhealth_number=?"
                : "SELECT COUNT(*) FROM patients WHERE philhealth_number=? AND id!=?";
            PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql);
            s.setString(1, num);
            if (editingId != -1) s.setInt(2, editingId);
            ResultSet rs = s.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill: " + (isError ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size: 12px; -fx-padding: 6 10;" +
            "-fx-background-color: " + (isError ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius: 6;"
        );
    }
}