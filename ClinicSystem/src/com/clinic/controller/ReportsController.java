package com.clinic.controller;

import com.clinic.db.DatabaseManager;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportsController {

    @FXML private HBox    summaryCards;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label   statusLabel;
    @FXML private TableView<String[]> previewTable;

    @FXML
    public void initialize() {
        fromDate.setValue(LocalDate.now().withDayOfMonth(1));
        toDate.setValue(LocalDate.now());
        loadSummaryCards();
    }

    private void loadSummaryCards() {
        summaryCards.getChildren().clear();
        summaryCards.getChildren().addAll(
            card("👥 Total Patients",
                getCount("SELECT COUNT(*) FROM patients"), "#3B82F6"),
            card("📅 This Month",
                getCount("SELECT COUNT(*) FROM appointments " +
                    "WHERE appointment_date >= date_trunc('month', CURRENT_DATE)"),
                "#10B981"),
            card("✅ Completed",
                getCount("SELECT COUNT(*) FROM appointments " +
                    "WHERE status='Completed'"), "#8B5CF6"),
            card("💊 Low Stock",
                getCount("SELECT COUNT(*) FROM inventory " +
                    "WHERE quantity <= reorder_level"), "#EF4444")
        );
    }

    private VBox card(String title, String value, String color) {
        VBox c = new VBox(6);
        c.setStyle(
            "-fx-background-color:white;-fx-padding:16;" +
            "-fx-background-radius:10;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2);" +
            "-fx-border-color:" + color + ";" +
            "-fx-border-width:0 0 0 4;" +
            "-fx-border-radius:0 10 10 0;"
        );
        HBox.setHgrow(c, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle("-fx-font-size:28px;-fx-font-weight:bold;" +
            "-fx-text-fill:" + color + ";");
        Label t = new Label(title);
        t.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
        c.getChildren().addAll(v, t);
        return c;
    }

    @FXML
    private void generatePatientReport() {
        List<String[]> data = new ArrayList<>();
        String sql = "SELECT id::text, full_name, gender, " +
                     "COALESCE(contact_number,'—'), " +
                     "COALESCE(email,'—'), " +
                     "COALESCE(created_at::text,'—') " +
                     "FROM patients ORDER BY full_name";
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            while (rs.next()) {
                String createdAt = rs.getString(6);
                data.add(new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    createdAt != null && createdAt.length() >= 10
                        ? createdAt.substring(0, 10) : "—"
                });
            }
        } catch (SQLException e) {
            showStatus("Error: " + e.getMessage(), true);
            return;
        }

        showPreview(
            new String[]{"ID","Name","Gender","Contact","Email","Registered"},
            data
        );

        String path = System.getProperty("user.home") +
            "/Desktop/PatientReport_" + LocalDate.now() + ".pdf";
        try {
            generatePDF(
                "PATIENT REPORT",
                "Barangay Health Center — " +
                    LocalDate.now().format(
                        DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                new String[]{"ID","Name","Gender","Contact","Email","Registered"},
                data, path
            );
            showStatus("✅ Patient Report saved to Desktop!", false);
        } catch (Exception e) {
            showStatus("❌ PDF Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void generateAppointmentReport() {
        LocalDate from = fromDate.getValue() != null
            ? fromDate.getValue()
            : LocalDate.now().withDayOfMonth(1);
        LocalDate to = toDate.getValue() != null
            ? toDate.getValue()
            : LocalDate.now();

        List<String[]> data = new ArrayList<>();
        String sql =
            "SELECT a.id::text, p.full_name, " +
            "a.appointment_date::text, " +
            "a.appointment_time::text, " +
            "COALESCE(a.reason,'—'), a.status " +
            "FROM appointments a " +
            "JOIN patients p ON a.patient_id = p.id " +
            "WHERE a.appointment_date BETWEEN ? AND ? " +
            "ORDER BY a.appointment_date, a.appointment_time";

        try {
            PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement(sql);

            // Use java.sql.Date explicitly to avoid ambiguity
            s.setDate(1, java.sql.Date.valueOf(from));
            s.setDate(2, java.sql.Date.valueOf(to));

            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                String dateStr = rs.getString(3);
                String timeStr = rs.getString(4);
                data.add(new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    dateStr != null && dateStr.length() >= 10
                        ? dateStr.substring(0, 10) : "—",
                    timeStr != null && timeStr.length() >= 5
                        ? timeStr.substring(0, 5) : "—",
                    rs.getString(5),
                    rs.getString(6)
                });
            }
        } catch (SQLException e) {
            showStatus("Error: " + e.getMessage(), true);
            return;
        }

        showPreview(
            new String[]{"ID","Patient","Date","Time","Reason","Status"},
            data
        );

        String path = System.getProperty("user.home") +
            "/Desktop/AppointmentReport_" + from + "_to_" + to + ".pdf";
        try {
            generatePDF(
                "APPOINTMENT REPORT",
                "Period: " +
                    from.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) +
                    " to " +
                    to.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                new String[]{"ID","Patient","Date","Time","Reason","Status"},
                data, path
            );
            showStatus("✅ Appointment Report saved to Desktop!", false);
        } catch (Exception e) {
            showStatus("❌ PDF Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void generateInventoryReport() {
        List<String[]> data = new ArrayList<>();
        String sql =
            "SELECT id::text, item_name, " +
            "COALESCE(category,'—'), " +
            "quantity::text, " +
            "COALESCE(unit,'—'), " +
            "reorder_level::text, " +
            "COALESCE(expiry_date::text,'No Expiry') " +
            "FROM inventory ORDER BY item_name";

        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            while (rs.next()) {
                int qty     = Integer.parseInt(rs.getString(4));
                int reorder = Integer.parseInt(rs.getString(6));
                String status = qty <= reorder ? "LOW STOCK" : "OK";
                String expiry = rs.getString(7);
                data.add(new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    expiry != null && expiry.length() >= 10
                        ? expiry.substring(0, 10) : "No Expiry",
                    status
                });
            }
        } catch (SQLException e) {
            showStatus("Error: " + e.getMessage(), true);
            return;
        }

        showPreview(
            new String[]{"ID","Item","Category","Qty",
                "Unit","Reorder","Expiry","Status"},
            data
        );

        String path = System.getProperty("user.home") +
            "/Desktop/InventoryReport_" + LocalDate.now() + ".pdf";
        try {
            generatePDF(
                "INVENTORY REPORT",
                "Barangay Health Center — " +
                    LocalDate.now().format(
                        DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                new String[]{"ID","Item","Category","Qty",
                    "Unit","Reorder","Expiry","Status"},
                data, path
            );
            showStatus("✅ Inventory Report saved to Desktop!", false);
        } catch (Exception e) {
            showStatus("❌ PDF Error: " + e.getMessage(), true);
        }
    }

    private void showPreview(String[] headers, List<String[]> rows) {
        previewTable.getColumns().clear();
        previewTable.getItems().clear();

        for (int i = 0; i < headers.length; i++) {
            final int idx = i;

            // Explicit full generic type — fixes the TableColumn<> error
            TableColumn<String[], String> col =
                new TableColumn<String[], String>(headers[i]);

            col.setCellValueFactory(cellData -> {
                String[] rowData = cellData.getValue();
                String val = (idx < rowData.length && rowData[idx] != null)
                    ? rowData[idx] : "";
                return new SimpleStringProperty(val);
            });

            col.setPrefWidth(120);
            previewTable.getColumns().add(col);
        }

        ObservableList<String[]> items =
            FXCollections.observableArrayList(rows);
        previewTable.setItems(items);
    }

    private void generatePDF(String title, String subtitle,
                              String[] headers, List<String[]> rows,
                              String outputPath) throws Exception {
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, new FileOutputStream(outputPath));
        doc.open();

        Font titleFont = new Font(
            Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Font subtitleFont = new Font(
            Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY);
        Font headerFont = new Font(
            Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        Font cellFont = new Font(
            Font.FontFamily.HELVETICA, 9);

        // Title block
        Paragraph titlePara = new Paragraph(
            "BARANGAY HEALTH CENTER\n" + title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        doc.add(titlePara);

        Paragraph subPara = new Paragraph(
            subtitle + "\nGenerated: " + LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            subtitleFont);
        subPara.setAlignment(Element.ALIGN_CENTER);
        doc.add(subPara);
        doc.add(Chunk.NEWLINE);

        // Table
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);

        // Header row
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new BaseColor(29, 78, 216));
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Data rows with alternating color
        boolean alt = false;
        for (String[] row : rows) {
            for (String val : row) {
                PdfPCell cell = new PdfPCell(
                    new Phrase(val != null ? val : "—", cellFont));
                cell.setPadding(6);
                if (alt) {
                    cell.setBackgroundColor(
                        new BaseColor(241, 245, 249));
                }
                table.addCell(cell);
            }
            alt = !alt;
        }

        doc.add(table);
        doc.add(Chunk.NEWLINE);

        // Footer
        Paragraph footer = new Paragraph(
            "Total Records: " + rows.size() + "\n" +
            "Generated by BHC Management System v2.0",
            subtitleFont
        );
        footer.setAlignment(Element.ALIGN_RIGHT);
        doc.add(footer);

        doc.close();
    }

    private String getCount(String sql) {
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("Count error: " + e.getMessage());
        }
        return "0";
    }

    private void showStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill:" + (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size:13px;-fx-padding:8 12;" +
            "-fx-background-color:" + (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius:6;"
        );
    }
}