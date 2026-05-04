package com.clinic.controller;

import com.clinic.db.AuditLogger;
import com.clinic.db.DatabaseManager;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class BillingController {

    @FXML private TableView<String[]>                    billingTable;
    @FXML private TableColumn<String[], String>          colId;
    @FXML private TableColumn<String[], String>          colPatient;
    @FXML private TableColumn<String[], String>          colDesc;
    @FXML private TableColumn<String[], String>          colAmount;
    @FXML private TableColumn<String[], String>          colPhil;
    @FXML private TableColumn<String[], String>          colTotal;
    @FXML private TableColumn<String[], String>          colStatus;
    @FXML private TableColumn<String[], String>          colDate;

    @FXML private ComboBox<String>  patientCombo;
    @FXML private TextField         descField;
    @FXML private TextField         amountField;
    @FXML private TextField         discountField;
    @FXML private CheckBox          philhealthCheck;
    @FXML private VBox              philhealthBox;
    @FXML private TextField         philhealthAmountField;
    @FXML private ComboBox<String>  statusCombo;
    @FXML private ComboBox<String>  methodCombo;
    @FXML private ComboBox<String>  filterStatus;
    @FXML private TextField         searchField;
    @FXML private Label             totalLabel;
    @FXML private Label             statusLabel;
    @FXML private Label             totalUnpaidLabel;
    @FXML private Label             formTitle;
    @FXML private Button            saveButton;

    private int editingId = -1;
    private Map<String, Integer> patientMap = new LinkedHashMap<>();
    private ObservableList<String[]> masterList =
        FXCollections.observableArrayList();
    private ObservableList<String[]> filteredList =
        FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        setupDropdowns();
        setupListeners();
        loadPatients();
        loadBilling();
        philhealthBox.setVisible(false);
        philhealthBox.setManaged(false);
    }

    private void setupColumns() {
        colId.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[0]));
        colPatient.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[1]));
        colDesc.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[2]));
        colAmount.setCellValueFactory(d ->
            new SimpleStringProperty("₱" + d.getValue()[3]));
        colPhil.setCellValueFactory(d ->
            new SimpleStringProperty(
                "true".equals(d.getValue()[5])
                    ? "₱" + d.getValue()[6] : "None"));
        colTotal.setCellValueFactory(d ->
            new SimpleStringProperty("₱" + d.getValue()[7]));
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[8]));
        colDate.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue()[9]));

        colStatus.setCellFactory(col ->
            new TableCell<String[], String>() {
                @Override
                protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) {
                        setText(null); setStyle(""); return;
                    }
                    setText(s);
                    if ("Paid".equals(s)) {
                        setStyle("-fx-text-fill:#059669;" +
                            "-fx-font-weight:bold;");
                    } else if ("Unpaid".equals(s)) {
                        setStyle("-fx-text-fill:#DC2626;" +
                            "-fx-font-weight:bold;");
                    } else if ("Partial".equals(s)) {
                        setStyle("-fx-text-fill:#D97706;" +
                            "-fx-font-weight:bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        );

        billingTable.setItems(filteredList);
        billingTable.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) populateForm(newVal);
            });
    }

    private void setupDropdowns() {
        statusCombo.setItems(FXCollections.observableArrayList(
            "Unpid", "Pid", "MedjoPid"
        ));
        statusCombo.setValue("Unpaid");

        methodCombo.setItems(FXCollections.observableArrayList(
            "Cash", "GCash", "PayMaya",
            "Bank Transfer", "PhilHealth", "Free/Charity"
        ));

        filterStatus.setItems(FXCollections.observableArrayList(
            "All", "Unpaid", "Paid", "Partial"
        ));
        filterStatus.setValue("All");
    }

    private void setupListeners() {
        searchField.textProperty().addListener(
            (obs, oldVal, newVal) -> applyFilter());
        filterStatus.valueProperty().addListener(
            (obs, oldVal, newVal) -> applyFilter());

        amountField.textProperty().addListener(
            (obs, oldVal, newVal) -> updateTotal());
        discountField.textProperty().addListener(
            (obs, oldVal, newVal) -> updateTotal());
        philhealthAmountField.textProperty().addListener(
            (obs, oldVal, newVal) -> updateTotal());
    }

    private void updateTotal() {
        try {
            double amount   = toDouble(amountField.getText());
            double discount = toDouble(discountField.getText());
            double phil     = philhealthCheck.isSelected()
                ? toDouble(philhealthAmountField.getText()) : 0.0;
            double total = Math.max(0.0,
                amount - discount - phil);
            totalLabel.setText(
                String.format("TOTAL DUE: \u20B1 %.2f", total));
        } catch (Exception e) {
            totalLabel.setText("TOTAL DUE: \u20B1 0.00");
        }
    }

    @FXML
    private void onPhilhealthToggle() {
        boolean checked = philhealthCheck.isSelected();
        philhealthBox.setVisible(checked);
        philhealthBox.setManaged(checked);
        updateTotal();
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
            showStatus("Error loading patients: " +
                e.getMessage(), true);
        }
    }

    private void loadBilling() {
        masterList.clear();
        double totalUnpaid = 0.0;

        String sql =
            "SELECT b.id::text, p.full_name, " +
            "COALESCE(b.description,'—'), " +
            "b.amount::text, " +
            "b.discount::text, " +
            "b.philhealth_covered::text, " +
            "b.philhealth_amount::text, " +
            "(b.amount - b.discount - " +
            " b.philhealth_amount)::text, " +
            "b.payment_status, " +
            "b.created_at::text, " +
            "COALESCE(b.payment_method,'—'), " +
            "COALESCE(b.notes,'') " +
            "FROM billing b " +
            "JOIN patients p ON b.patient_id = p.id " +
            "ORDER BY b.created_at DESC";

        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();

            while (rs.next()) {
                String created = rs.getString(10);
                masterList.add(new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9),
                    (created != null && created.length() >= 10)
                        ? created.substring(0, 10) : "—",
                    rs.getString(11),
                    rs.getString(12)
                });

                String status = rs.getString(9);
                if ("Unpaid".equals(status) ||
                        "Partial".equals(status)) {
                    totalUnpaid += toDouble(rs.getString(8));
                }
            }

            applyFilter();

            if (totalUnpaid > 0) {
                totalUnpaidLabel.setText(
                    String.format(
                        "\u26A0\uFE0F Unpaid: \u20B1%.2f",
                        totalUnpaid));
                totalUnpaidLabel.setVisible(true);
            } else {
                totalUnpaidLabel.setText("");
                totalUnpaidLabel.setVisible(false);
            }

        } catch (SQLException e) {
            showStatus("Error loading: " + e.getMessage(), true);
        }
    }

    private void applyFilter() {
        String kw     = searchField.getText().trim().toLowerCase();
        String status = filterStatus.getValue();
        filteredList.clear();
        for (String[] row : masterList) {
            boolean matchKw = kw.isEmpty() ||
                row[1].toLowerCase().contains(kw);
            boolean matchStatus = "All".equals(status) ||
                status.equals(row[8]);
            if (matchKw && matchStatus) {
                filteredList.add(row);
            }
        }
    }

    @FXML
    private void resetFilter() {
        searchField.clear();
        filterStatus.setValue("All");
    }

    @FXML
    private void showUnpaid() {
        filteredList.clear();
        for (String[] row : masterList) {
            if ("Unpaid".equals(row[8]) ||
                    "Partial".equals(row[8])) {
                filteredList.add(row);
            }
        }
    }

    @FXML
    private void handleSave() {
        if (patientCombo.getValue() == null) {
            showStatus("Please select a patient.", true);
            return;
        }
        if (amountField.getText().trim().isEmpty()) {
            showStatus("Amount is required.", true);
            return;
        }
        try {
            double amt = Double.parseDouble(
                amountField.getText().trim());
            if (amt < 0) {
                showStatus("Amount cannot be negative.", true);
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Amount must be a valid number.", true);
            return;
        }

        String sql = editingId == -1
            ? "INSERT INTO billing " +
              "(patient_id, description, amount, discount, " +
              "philhealth_covered, philhealth_amount, " +
              "payment_status, payment_method, notes) " +
              "VALUES (?,?,?,?,?,?,?,?,?)"
            : "UPDATE billing SET " +
              "patient_id=?, description=?, amount=?, " +
              "discount=?, philhealth_covered=?, " +
              "philhealth_amount=?, payment_status=?, " +
              "payment_method=?, notes=? WHERE id=?";

        try {
            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement(sql);
            s.setInt(1,
                patientMap.get(patientCombo.getValue()));
            s.setString(2,
                descField.getText().trim());
            s.setDouble(3,
                toDouble(amountField.getText()));
            s.setDouble(4,
                toDouble(discountField.getText()));
            s.setBoolean(5,
                philhealthCheck.isSelected());
            s.setDouble(6,
                philhealthCheck.isSelected()
                    ? toDouble(philhealthAmountField.getText())
                    : 0.0);
            s.setString(7, statusCombo.getValue());
            s.setString(8, methodCombo.getValue());
            s.setString(9, "");
            if (editingId != -1) s.setInt(10, editingId);

            s.executeUpdate();

            AuditLogger.log(
                editingId == -1 ? "ADD BILL" : "UPDATE BILL",
                "billing",
                editingId,
                "Bill for: " + patientCombo.getValue()
            );

            showStatus("✅ Bill saved successfully!", false);
            clearForm();
            loadBilling();

        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void printReceipt() {
        String[] row =
            billingTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showStatus("Select a bill to print receipt.", true);
            return;
        }

        String path = System.getProperty("user.home") +
            "/Desktop/Receipt_" +
            row[1].replace(" ", "_") + "_" +
            row[9] + ".pdf";

        try {
            String clinicName = "Barangay Health Center";
            String address    = "";
            String contact    = "";

            try {
                ResultSet cs = DatabaseManager.getConnection()
                    .prepareStatement(
                        "SELECT clinic_name, address, " +
                        "contact_number " +
                        "FROM clinic_settings LIMIT 1")
                    .executeQuery();
                if (cs.next()) {
                    if (cs.getString(1) != null)
                        clinicName = cs.getString(1);
                    if (cs.getString(2) != null)
                        address = cs.getString(2);
                    if (cs.getString(3) != null)
                        contact = cs.getString(3);
                }
            } catch (Exception ignored) {}

            Document doc = new Document(PageSize.A5);
            PdfWriter.getInstance(
                doc, new FileOutputStream(path));
            doc.open();

            Font titleFont = new Font(
                Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font subFont = new Font(
                Font.FontFamily.HELVETICA, 9,
                Font.ITALIC, BaseColor.GRAY);
            Font headerFont = new Font(
                Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font bodyFont = new Font(
                Font.FontFamily.HELVETICA, 10);
            Font totalFont = new Font(
                Font.FontFamily.HELVETICA, 13, Font.BOLD,
                new BaseColor(5, 150, 105));

            Paragraph title =
                new Paragraph(clinicName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph sub = new Paragraph(
                address + "\n" + contact, subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            doc.add(sub);

            doc.add(new Paragraph(
                "--------------------------------", subFont));
            doc.add(Chunk.NEWLINE);

            Paragraph receiptTitle =
                new Paragraph("OFFICIAL RECEIPT", headerFont);
            receiptTitle.setAlignment(Element.ALIGN_CENTER);
            doc.add(receiptTitle);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                "Receipt #:   " + row[0], bodyFont));
            doc.add(new Paragraph(
                "Patient:     " + row[1], bodyFont));
            doc.add(new Paragraph(
                "Date:        " + row[9], bodyFont));
            doc.add(new Paragraph(
                "Description: " + row[2], bodyFont));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                "--------------------------------", subFont));
            doc.add(new Paragraph(
                "Gross Amount:  PHP " + row[3], bodyFont));
            doc.add(new Paragraph(
                "Discount:      PHP " + row[4], bodyFont));

            if ("true".equals(row[5])) {
                doc.add(new Paragraph(
                    "PhilHealth:    PHP " + row[6], bodyFont));
            }

            doc.add(new Paragraph(
                "--------------------------------", subFont));

            Paragraph totalPara = new Paragraph(
                "TOTAL DUE:     PHP " + row[7], totalFont);
            doc.add(totalPara);

            doc.add(new Paragraph(
                "Status: " + row[8], headerFont));
            if (!"—".equals(row[10])) {
                doc.add(new Paragraph(
                    "Method: " + row[10], bodyFont));
            }
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                "--------------------------------", subFont));
            doc.add(new Paragraph(
                "\n\n___________________________\n" +
                "       Received By\n\n" +
                "Thank you for trusting us with your health!",
                subFont));

            doc.close();
            showStatus("✅ Receipt saved to Desktop!", false);

        } catch (Exception e) {
            showStatus("❌ PDF Error: " + e.getMessage(), true);
        }
    }

    private void populateForm(String[] row) {
        editingId = Integer.parseInt(row[0]);
        formTitle.setText("Edit Bill #" + editingId);
        patientCombo.setValue(row[1]);
        descField.setText("—".equals(row[2]) ? "" : row[2]);
        amountField.setText(row[3]);
        discountField.setText(row[4]);

        boolean hasPhil = "true".equals(row[5]);
        philhealthCheck.setSelected(hasPhil);
        philhealthBox.setVisible(hasPhil);
        philhealthBox.setManaged(hasPhil);
        philhealthAmountField.setText(row[6]);

        statusCombo.setValue(row[8]);
        methodCombo.setValue(
            "—".equals(row[10]) ? null : row[10]);
        saveButton.setText("💾  Update Bill");
        updateTotal();
    }

    @FXML
    public void clearForm() {
        editingId = -1;
        formTitle.setText("New Bill");
        patientCombo.setValue(null);
        descField.clear();
        amountField.clear();
        discountField.setText("0");
        philhealthCheck.setSelected(false);
        philhealthBox.setVisible(false);
        philhealthBox.setManaged(false);
        philhealthAmountField.clear();
        statusCombo.setValue("Unpaid");
        methodCombo.setValue(null);
        billingTable.getSelectionModel().clearSelection();
        saveButton.setText("💾  Save Bill");
        statusLabel.setText("");
        totalLabel.setText("TOTAL DUE: \u20B1 0.00");
    }

    // Safe number parser — returns 0 if empty or invalid
    private double toDouble(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
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