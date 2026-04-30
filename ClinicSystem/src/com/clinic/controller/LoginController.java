package com.clinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

public class LoginController { 
 
    @FXML
    private TextField usernameField;

    @FXML
    private void handleLogin() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/com/clinic/fxml/Dashboard.fxml")
            );

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}