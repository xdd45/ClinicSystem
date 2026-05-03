package com.clinic.controller;

import com.clinic.db.AuditLogger;
import com.clinic.db.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;

public class InventoryController {

    @FXML private TableView<String[]>           inventoryTable;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colName;
    @FXML private TableColumn<String[], String> colCategory;
    @FXML private TableColumn<String[], String> colQty;
    @FXML private TableColumn<String[], String> colUnit;
    @FXML private TableColumn<String[], String> colReorder;
    @FXML private TableColumn<String[], String> colExpiry;
    @FXML private TableColumn<String[], String> colStatus;

    @FXML private TextField        nameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField        qtyField;
    @FXML private TextField        unitField;
    @FXML private TextField        reorderField;
    @FXML private DatePicker       expiryPicker;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterCategory;
    @FXML private Label            statusLabel;
    @FXML private Label            lowStockLabel;
    @FXML private Label            formTitle;
    @FXML private Button           saveButton;

    private int editingId = -1;
    private ObservableList<String[]> masterList   = FXCollections.observableArrayList();
    private ObservableList<String[]> filteredList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        setupDropdowns();
        searchField.textProperty().addListener((o, old, n) -> applyFilter());
        filterCategory.valueProperty().addListener((o, old, n) -> applyFilter());
        loadInventory();
    }

    @SuppressWarnings("unchecked")
    private void setupColumns() {
        colId.setCellValueFactory(d       -> new SimpleStringProperty(d.getValue()[0]));
        colName.setCellValueFactory(d     -> new SimpleStringProperty(d.getValue()[1]));
        colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colQty.setCellValueFactory(d      -> new SimpleStringProperty(d.getValue()[3]));
        colUnit.setCellValueFactory(d     -> new SimpleStringProperty(d.getValue()[4]));
        colReorder.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue()[5]));
        colExpiry.setCellValueFactory(d   -> new SimpleStringProperty(d.getValue()[6]));
        colStatus.setCellValueFactory(d   -> new SimpleStringProperty(d.getValue()[7]));

        // Color code by status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                switch (s) {
                    case "OK"       -> setStyle("-fx-text-fill:#059669;-fx-font-weight:bold;");
                    case "Low Stock"-> setStyle("-fx-text-fill:#D97706;-fx-font-weight:bold;");
                    case "Expired"  -> setStyle("-fx-text-fill:#DC2626;-fx-font-weight:bold;");
                    default         -> setStyle("");
                }
            }
        });

        // Color quantity red if low
        colQty.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) { setText(null); return; }
                setText(qty);
                try {
                    String[] row = getTableRow().getItem();
                    if (row != null) {
                        int q = Integer.parseInt(row[3]);
                        int r = Integer.parseInt(row[5]);
                        setStyle(q <= r
                            ? "-fx-text-fill:#DC2626;-fx-font-weight:bold;"
                            : "-fx-text-fill:#059669;");
                    }
                } catch (Exception ignored) {}
            }
        });

        inventoryTable.setItems(filteredList);
        inventoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, o, n) -> { if (n != null) populateForm(n); }
        );
    }

    private void setupDropdowns() {
        ObservableList<String> cats = FXCollections.observableArrayList(
            "Medicine", "Vaccine", "Supplies", "Equipment", "Other"
        );
        categoryCombo.setItems(cats);
        filterCategory.setItems(
            FXCollections.observableArrayList("All Categories",
                "Medicine","Vaccine","Supplies","Equipment","Other")
        );
        filterCategory.setValue("All Categories");
    }

    private void loadInventory() {
        masterList.clear();
        int lowCount = 0;
        String sql = "SELECT id, item_name, category, quantity, unit, " +
                     "reorder_level, expiry_date::text FROM inventory ORDER BY item_name";
        try {
            ResultSet rs = DatabaseManager.getConnection()
                .prepareStatement(sql).executeQuery();
            while (rs.next()) {
                int qty     = rs.getInt("quantity");
                int reorder = rs.getInt("reorder_level");
                String expiry = rs.getString("expiry_date");
                String expiryDisplay = expiry != null ? expiry.substring(0,10) : "No Expiry";

                // Determine status
                String status = "OK";
                if (expiry != null && LocalDate.parse(expiry.substring(0,10))
                        .isBefore(LocalDate.now())) {
                    status = "Expired";
                } else if (qty <= reorder) {
                    status = "Low Stock";
                    lowCount++;
                }

                masterList.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("item_name"),
                    rs.getString("category") != null ? rs.getString("category") : "—",
                    String.valueOf(qty),
                    rs.getString("unit") != null ? rs.getString("unit") : "—",
                    String.valueOf(reorder),
                    expiryDisplay,
                    status
                });
            }
            applyFilter();
            lowStockLabel.setText(lowCount > 0
                ? "⚠️ " + lowCount + " items low on stock!" : "");
            lowStockLabel.setVisible(lowCount > 0);
        } catch (SQLException e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    private void applyFilter() {
        String kw  = searchField.getText().trim().toLowerCase();
        String cat = filterCategory.getValue();
        filteredList.clear();
        masterList.stream()
            .filter(r ->
                (kw.isEmpty() || r[1].toLowerCase().contains(kw)) &&
                ("All Categories".equals(cat) || cat == null || cat.equals(r[2]))
            )
            .forEach(filteredList::add);
    }

    @FXML private void resetFilter() {
        searchField.clear();
        filterCategory.setValue("All Categories");
    }

    @FXML private void showLowStock() {
        filteredList.clear();
        masterList.stream()
            .filter(r -> "Low Stock".equals(r[7]) || "Expired".equals(r[7]))
            .forEach(filteredList::add);
    }

    @FXML
    private void handleSave() {
        if (nameField.getText().trim().isEmpty()) {
            showStatus("Item name is required.", true); return;
        }
        try {
            Integer.parseInt(qtyField.getText().trim());
        } catch (NumberFormatException e) {
            showStatus("Quantity must be a number.", true); return;
        }

        String sql = editingId == -1
            ? "INSERT INTO inventory(item_name,category,quantity,unit,reorder_level,expiry_date) VALUES(?,?,?,?,?,?)"
            : "UPDATE inventory SET item_name=?,category=?,quantity=?,unit=?,reorder_level=?,expiry_date=? WHERE id=?";

        try {
            PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql);
            s.setString(1, nameField.getText().trim());
            s.setString(2, categoryCombo.getValue());
            s.setInt(3, Integer.parseInt(qtyField.getText().trim()));
            s.setString(4, unitField.getText().trim());
            s.setInt(5, reorderField.getText().trim().isEmpty()
                ? 10 : Integer.parseInt(reorderField.getText().trim()));
            if (expiryPicker.getValue() != null)
                s.setDate(6, Date.valueOf(expiryPicker.getValue()));
            else s.setNull(6, Types.DATE);
            if (editingId != -1) s.setInt(7, editingId);

            s.executeUpdate();
            AuditLogger.log(
                editingId == -1 ? "ADD INVENTORY" : "UPDATE INVENTORY",
                "inventory", editingId, "Item: " + nameField.getText()
            );
            showStatus("✅ Item saved!", false);
            clearForm();
            loadInventory();
        } catch (SQLException e) {
            showStatus("❌ Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        String[] row = inventoryTable.getSelectionModel().getSelectedItem();
        if (row == null) { showStatus("Select an item first.", true); return; }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete " + row[1] + "?", ButtonType.OK, ButtonType.CANCEL);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    PreparedStatement s = DatabaseManager.getConnection()
                        .prepareStatement("DELETE FROM inventory WHERE id=?");
                    s.setInt(1, Integer.parseInt(row[0]));
                    s.executeUpdate();
                    showStatus("Item deleted.", false);
                    clearForm();
                    loadInventory();
                } catch (SQLException e) {
                    showStatus("❌ Error: " + e.getMessage(), true);
                }
            }
        });
    }

    private void populateForm(String[] row) {
        editingId = Integer.parseInt(row[0]);
        formTitle.setText("Edit Item #" + editingId);
        nameField.setText(row[1]);
        categoryCombo.setValue(row[2].equals("—") ? null : row[2]);
        qtyField.setText(row[3]);
        unitField.setText(row[4].equals("—") ? "" : row[4]);
        reorderField.setText(row[5]);
        if (!row[6].equals("No Expiry")) {
            try { expiryPicker.setValue(LocalDate.parse(row[6])); }
            catch (Exception ignored) {}
        }
        saveButton.setText("💾  Update Item");
    }

    @FXML
    public void clearForm() {
        editingId = -1;
        formTitle.setText("Add Item");
        nameField.clear(); categoryCombo.setValue(null);
        qtyField.clear(); unitField.clear();
        reorderField.setText("10"); expiryPicker.setValue(null);
        inventoryTable.getSelectionModel().clearSelection();
        saveButton.setText("💾  Save Item");
        statusLabel.setText("");
    }

    private void showStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
            "-fx-text-fill:" + (err ? "#DC2626" : "#059669") + ";" +
            "-fx-font-size:12px;-fx-padding:6 10;" +
            "-fx-background-color:" + (err ? "#FEE2E2" : "#D1FAE5") + ";" +
            "-fx-background-radius:6;"
        );
    }
}