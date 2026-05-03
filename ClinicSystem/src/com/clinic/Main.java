package com.clinic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
            getClass().getResource("/com/clinic/fxml/Login.fxml")
        );
        stage.setTitle("BHC System — Barangay Health Center Management");
        stage.setScene(new Scene(root, 900, 580));
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}