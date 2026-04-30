package com.clinic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

    	FXMLLoader loader = new FXMLLoader(
    		    getClass().getResource("/com/clinic/fxml/Login.fxml")
    		);
    	System.out.println(getClass().getResource("/com/clinic/fxml/Login.fxml"));
    		Parent root = loader.load();

        stage.setTitle("Clinic Scheduler");
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}