package com.clinic.controller;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class RoleSelectionController {

    @FXML private VBox adminCard;
    @FXML private VBox doctorCard;
    @FXML private VBox nurseCard;

    @FXML
    public void initialize() {
        addHoverEffect(adminCard);
        addHoverEffect(doctorCard);
        addHoverEffect(nurseCard);
    }

    private void addHoverEffect(VBox card) {
        card.setOnMouseEntered(e -> {
            ScaleTransition st =
                new ScaleTransition(
                    Duration.millis(150), card);
            st.setToX(1.08);
            st.setToY(1.08);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st =
                new ScaleTransition(
                    Duration.millis(150), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    @FXML private void selectAdmin() {
        openLogin("Admin", "#1D4ED8");
    }

    @FXML private void selectDoctor() {
        openLogin("Doctor", "#059669");
    }

    @FXML private void selectNurse() {
        openLogin("Nurse", "#F59E0B");
    }

    private void openLogin(String role, String color) {
        try {
            LoginController.setSelectedRole(role);
            LoginController.setRoleColor(color);
            Parent root = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/Login.fxml"));
            Stage stage = (Stage) adminCard
                .getScene().getWindow();
            stage.setScene(
                new Scene(root, 1000, 620));
            stage.setTitle(
                "BHC System — " + role + " Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}