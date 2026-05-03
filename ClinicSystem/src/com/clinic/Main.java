package com.clinic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Show splash screen first
        Parent splash = FXMLLoader.load(
            getClass().getResource(
                "/com/clinic/fxml/SplashScreen.fxml"));
        stage.setTitle(
            "BHC Management System — Loading...");
        stage.setScene(new Scene(splash, 600, 400));
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}