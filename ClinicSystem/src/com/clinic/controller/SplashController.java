package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashController {

    @FXML private ProgressBar progressBar;
    @FXML private Label       loadingLabel;
    @FXML private Label       titleLabel;
    @FXML private Circle      logoCircle;

    @FXML
    public void initialize() {
        animateLogo();
        startLoading();
    }

    private void animateLogo() {
        // Pulse animation on the logo circle
        ScaleTransition pulse = new ScaleTransition(
            Duration.millis(800), logoCircle);
        pulse.setFromX(0.8);
        pulse.setFromY(0.8);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        // Fade in title
        FadeTransition fade = new FadeTransition(
            Duration.millis(1000), titleLabel);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void startLoading() {
        String[] steps = {
            "Connecting to database...",
            "Loading patient records...",
            "Initializing modules...",
            "Preparing dashboard...",
            "Almost ready..."
        };

        Timeline timeline = new Timeline();

        for (int i = 0; i < steps.length; i++) {
            final int idx    = i;
            final double progress = (double)(i + 1) / steps.length;

            KeyFrame frame = new KeyFrame(
                Duration.millis(600 * (i + 1)),
                e -> {
                    progressBar.setProgress(progress);
                    loadingLabel.setText(steps[idx]);
                }
            );
            timeline.getKeyFrames().add(frame);
        }

        // After loading finishes — open Login
        timeline.setOnFinished(e -> {
            // Test actual DB connection
            new Thread(() -> {
                try {
                    DatabaseManager.getConnection();
                    Platform.runLater(this::openLogin);
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        loadingLabel.setText(
                            "⚠️ DB Error: " + ex.getMessage());
                        loadingLabel.setStyle(
                            "-fx-text-fill: #EF4444;");
                        // Still open login after 2 seconds
                        new Timeline(new KeyFrame(
                            Duration.seconds(2),
                            ev -> openLogin()
                        )).play();
                    });
                }
            }).start();
        });

        timeline.play();
    }

    private void openLogin() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/Login.fxml"));
            Stage stage = (Stage) progressBar
                .getScene().getWindow();

            // Fade transition to login
            FadeTransition fade = new FadeTransition(
                Duration.millis(500),
                progressBar.getScene().getRoot());
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> {
                stage.setScene(new Scene(root, 900, 580));
                stage.setTitle(
                    "BHC System — Login");
                FadeTransition fadeIn =
                    new FadeTransition(
                        Duration.millis(500), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fade.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}