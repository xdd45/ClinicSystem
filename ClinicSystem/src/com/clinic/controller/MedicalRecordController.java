package com.clinic.controller;

import com.clinic.db.AuditLogger;
import com.clinic.db.DatabaseManager;
import com.clinic.db.SessionManager;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.FileOutputStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MedicalRecordController {

    @FXML private TableView<String[]>           recordTable;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colPatient;
    @FXML private TableColumn<String[], String> colDate;
    @FXML private TableColumn<String[], String> colComplaint;
    @FXML private TableColumn<String[], String> colDiagnosis;
    @FXML private TableColumn<String[], String> colBP;
    @FXML private TableColumn<String[], String> colTemp;

    @FXML private ComboBox<String> patientCombo;
    @FXML private DatePicker       visitDatePicker;
    @FXML private TextField        bpField;
    @FXML private TextField        tempField;
    @FXML private TextField        weightField;
    @FXML private TextField        heightField;
    @FXML private TextArea         complaintArea;
    @FXML private TextArea         diagnosisArea;
    @FXML private TextArea         prescriptionArea;
    @FXML private TextArea         notesArea;
    @FXML private TextField        searchField;
    @FXML private DatePicker       filterDate;
    @FXML private Label            statusLabel;
    @FXML private Label            countLabel;
    @FXML private Label            formTitle;
    @FXML private Button           saveButton;

    private int editingId = -1;
    private Map<String, Integer> patientMap =
        new LinkedHashMap<>();
    private ObservableList<String[]> masterList =
        FXCollections.observableArrayList();
    private ObservableList<String[]> filteredList =
        FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadPatients();
        loadRecords();
        visitDatePicker.setValue(LocalDate.now());

        searchField.textProperty().addListener(
            (obs, old, n) -> applyFilter());
        filterDate.valueProperty().addListener(
            (obs, old, n) -> applyFilter());

        // Role-based access: only Doctor can edit/add/delete medical records
        // Admin can only view (read-only)
        String role = com.clinic.db.SessionManager.getCurrentRole();
        boolean canEdit = "Doctor".equalsIgnoreCase(role);

        if (!canEdit) {
            // Disable all form inputs
            patientCombo.setDisable(true);
            visitDatePicker.setDisable(true);
            bpField.setDisable(true);
            tempField.setDisable(true);
            weightField.setDisable(true);
            heightField.setDisable(true);
            complaintArea.setDisable(true);
            diagnosisArea.setDisable(true);
            prescriptionArea.setDisable(true);
            notesArea.setDisable(true);
            saveButton.setDisable(true);
            saveButton.setVisible(false);

            // Show read-only banner
            formTitle.setText("📋 Medical Records — View Only");
            statusLabel.setText("ℹ️ Only doctors can add or edit medical records.");
            statusLabel.setStyle(
                "-fx-text-fill: #1D4ED8; -fx-font-size: 12px;" +
                "-fx-padding: 6 10; -fx-background-color: #DBEAFE;" +
                "-fx-background-radius: 6;"
            );
        }
    }

    private void setupColumns() {
        colId.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[0]));
        colPatient.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[1]));
        colDate.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[2]));
        colComplaint.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[3]));
        colDiagnosis.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[4]));
        colBP.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[5]));
        colTemp.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[6]));

        recordTable.setItems(filteredList);
        recordTable.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) populateForm(newVal);
            });
    }

    private void loadPatients() {
        patientMap.clear();
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(
                    "SELECT id, full_name FROM patients " +
                    "ORDER BY full_name")
                .executeQuery();
            while (rs.next()) {
                patientMap.put(
                    rs.getString("full_name"),
                    rs.getInt("id")
                );
            }
            patientCombo.setItems(
                FXCollections.observableArrayList(
                    patientMap.keySet()));
        } catch (SQLException e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    private void loadRecords() {
        masterList.clear();
        String sql =
            "SELECT r.id::text, p.full_name, " +
            "r.visit_date::text, " +
            "COALESCE(r.chief_complaint,'—'), " +
            "COALESCE(r.diagnosis,'—'), " +
            "COALESCE(r.blood_pressure,'—'), " +
            "COALESCE(r.temperature::text,'—'), " +
            "COALESCE(r.weight::text,''), " +
            "COALESCE(r.height::text,''), " +
            "COALESCE(r.prescription,''), " +
            "COALESCE(r.notes,''), " +
            "r.patient_id::text " +
            "FROM medical_records r " +
            "JOIN patients p ON r.patient_id = p.id " +
            "ORDER BY r.visit_date DESC";
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            while (rs.next()) {
                String dateStr = rs.getString(3);
                masterList.add(new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    dateStr != null && dateStr.length() >= 10
                        ? dateStr.substring(0, 10) : "—",
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7).equals("—")
                        ? "—" : rs.getString(7) + "°C",
                    rs.getString(8),
                    rs.getString(9),
                    rs.getString(10),
                    rs.getString(11),
                    rs.getString(12)
                });
            }
            applyFilter();
        } catch (SQLException e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    private void applyFilter() {
        String kw   = searchField.getText().trim()
            .toLowerCase();
        LocalDate d = filterDate.getValue();
        filteredList.clear();
        for (String[] row : masterList) {
            boolean matchKw = kw.isEmpty() ||
                row[1].toLowerCase().contains(kw);
            boolean matchDate = d == null ||
                row[2].startsWith(d.toString());
            if (matchKw && matchDate) {
                filteredList.add(row);
            }
        }
        if (countLabel != null) {
            countLabel.setText(
                filteredList.size() + " record(s)");
        }
    }

    @FXML
    private void resetFilter() {
        searchField.clear();
        filterDate.setValue(null);
    }

    @FXML
    private void handleSave() {
        if (patientCombo.getValue() == null) {
            showStatus("Please select a patient.", true);
            return;
        }
        if (visitDatePicker.getValue() == null) {
            showStatus("Visit date is required.", true);
            return;
        }

        String sql = editingId == -1
            ? "INSERT INTO medical_records " +
              "(patient_id, user_id, visit_date, " +
              "blood_pressure, temperature, weight, height, " +
              "chief_complaint, diagnosis, " +
              "prescription, notes) " +
              "VALUES(?,?,?,?,?,?,?,?,?,?,?)"
            : "UPDATE medical_records SET " +
              "patient_id=?, user_id=?, visit_date=?, " +
              "blood_pressure=?, temperature=?, " +
              "weight=?, height=?, chief_complaint=?, " +
              "diagnosis=?, prescription=?, notes=? " +
              "WHERE id=?";

        try {
            PreparedStatement s = DatabaseManager
                .getConnection().prepareStatement(sql);

            s.setInt(1,
                patientMap.get(patientCombo.getValue()));
            s.setInt(2,
                SessionManager.getCurrentUserId());
            s.setDate(3,
                Date.valueOf(visitDatePicker.getValue()));
            s.setString(4, bpField.getText().trim());

            setDouble(s, 5, tempField.getText().trim());
            setDouble(s, 6, weightField.getText().trim());
            setDouble(s, 7, heightField.getText().trim());

            s.setString(8,
                complaintArea.getText().trim());
            s.setString(9,
                diagnosisArea.getText().trim());
            s.setString(10,
                prescriptionArea.getText().trim());
            s.setString(11,
                notesArea.getText().trim());

            if (editingId != -1) s.setInt(12, editingId);

            s.executeUpdate();

            AuditLogger.log(
                editingId == -1
                    ? "ADD MEDICAL RECORD"
                    : "UPDATE MEDICAL RECORD",
                "medical_records",
                editingId,
                "Record for: " + patientCombo.getValue()
            );

            showStatus("✅ Medical record saved!", false);
            clearForm();
            loadRecords();

        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    private void setDouble(PreparedStatement s,
                           int idx,
                           String val)
            throws SQLException {
        if (val == null || val.trim().isEmpty()) {
            s.setNull(idx, Types.DECIMAL);
        } else {
            try {
                s.setDouble(idx,
                    Double.parseDouble(val.trim()));
            } catch (NumberFormatException e) {
                s.setNull(idx, Types.DECIMAL);
            }
        }
    }

    @FXML
    private void handlePrint() {
        String[] row = recordTable.getSelectionModel()
            .getSelectedItem();
        if (row == null) {
            showStatus("Select a record to print.", true);
            return;
        }

        String filename = "MedicalRecord_" +
            row[1].replace(" ", "_") +
            "_" + row[2] + ".pdf";
        String path = System.getProperty("user.home") +
            "/Desktop/" + filename;

        try {
            Document doc = new Document();
            PdfWriter.getInstance(
                doc, new FileOutputStream(path));
            doc.open();

            Font titleFont = new Font(
                Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font headFont  = new Font(
                Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font bodyFont  = new Font(
                Font.FontFamily.HELVETICA, 11);
            Font smallFont = new Font(
                Font.FontFamily.HELVETICA, 9,
                Font.ITALIC);

            doc.add(new Paragraph(
                "BARANGAY HEALTH CENTER", titleFont));
            doc.add(new Paragraph(
                "Medical Record / Clinical Summary",
                smallFont));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                "PATIENT INFORMATION", headFont));
            doc.add(new Paragraph(
                "Name:       " + row[1], bodyFont));
            doc.add(new Paragraph(
                "Visit Date: " + row[2], bodyFont));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                "VITAL SIGNS", headFont));
            doc.add(new Paragraph(
                "Blood Pressure: " + row[5], bodyFont));
            doc.add(new Paragraph(
                "Temperature:    " + row[6], bodyFont));
            doc.add(new Paragraph(
                "Weight:         " +
                (row[7] != null && !row[7].isEmpty()
                    ? row[7] + " kg" : "—"),
                bodyFont));
            doc.add(new Paragraph(
                "Height:         " +
                (row[8] != null && !row[8].isEmpty()
                    ? row[8] + " cm" : "—"),
                bodyFont));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                "CLINICAL NOTES", headFont));
            doc.add(new Paragraph(
                "Chief Complaint: " + row[3], bodyFont));
            doc.add(new Paragraph(
                "Diagnosis:       " + row[4], bodyFont));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                "PRESCRIPTION", headFont));
            doc.add(new Paragraph(
                row[9] != null && !row[9].isEmpty()
                    ? row[9] : "None",
                bodyFont));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                "_______________________________\n" +
                "Attending Physician\nDate: " +
                LocalDate.now().format(
                    DateTimeFormatter.ofPattern(
                        "MMMM d, yyyy")),
                bodyFont));

            doc.close();
            showStatus(
                "✅ PDF saved to Desktop: " + filename,
                false);

        } catch (Exception e) {
            showStatus(
                "❌ PDF Error: " + e.getMessage(), true);
        }
    }

    private void populateForm(String[] row) {
        editingId = Integer.parseInt(row[0]);
        if (formTitle != null)
            formTitle.setText(
                "Edit Record #" + editingId);

        patientCombo.setValue(row[1]);

        try {
            visitDatePicker.setValue(
                LocalDate.parse(row[2]));
        } catch (Exception ignored) {}

        bpField.setText(
            "—".equals(row[5]) ? "" : row[5]);
        tempField.setText(
            row[6].replace("°C", "")
                .equals("—") ? "" :
            row[6].replace("°C", ""));
        weightField.setText(
            row[7] != null ? row[7] : "");
        heightField.setText(
            row[8] != null ? row[8] : "");
        complaintArea.setText(
            "—".equals(row[3]) ? "" : row[3]);
        diagnosisArea.setText(
            "—".equals(row[4]) ? "" : row[4]);
        prescriptionArea.setText(
            row[9] != null ? row[9] : "");
        notesArea.setText(
            row[10] != null ? row[10] : "");

        if (saveButton != null)
            saveButton.setText("💾  Update Record");
    }

    @FXML
    public void clearForm() {
        editingId = -1;
        if (formTitle != null)
            formTitle.setText("New Medical Record");

        patientCombo.setValue(null);
        visitDatePicker.setValue(LocalDate.now());
        bpField.clear();
        tempField.clear();
        weightField.clear();
        heightField.clear();
        complaintArea.clear();
        diagnosisArea.clear();
        prescriptionArea.clear();
        notesArea.clear();
        recordTable.getSelectionModel().clearSelection();

        if (saveButton != null)
            saveButton.setText("💾  Save Record");
        if (statusLabel != null)
            statusLabel.setText("");
    }

    private void showStatus(String msg, boolean err) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill:" +
            (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size:12px;-fx-padding:6 10;" +
            "-fx-background-color:" +
            (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius:6;"
        );
    }
}