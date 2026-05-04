package com.clinic.controller;

import com.clinic.db.DatabaseManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
            "WAIT HUHU",
            "WALA NA NASIRA NA BINUKSAN KASI",
            "PASIKAT KASE SI BALID",
            "BAHALA KAU DYAN.."
        };

        Timeline timeline = new Timeline();

        for (int i = 0; i < steps.length; i++) {
            final int idx = i;
            final double progress =
                (double)(i + 1) / steps.length;

            KeyFrame frame = new KeyFrame(
                Duration.millis(2000 * (i + 1)),
                e -> {
                    progressBar.setProgress(progress);
                    loadingLabel.setText(steps[idx]);
                }
            );
            timeline.getKeyFrames().add(frame);
        }

        // After loading finishes — open Role Selection
        timeline.setOnFinished(e -> {
            new Thread(() -> {
                try {
                    DatabaseManager.getConnection();
                    Platform.runLater(
                        this::openRoleSelection);
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        loadingLabel.setText(
                            "⚠️ DB Error: " +
                            ex.getMessage());
                        loadingLabel.setStyle(
                            "-fx-text-fill: #EF4444;");
                        // Still open after 2 seconds
                        new Timeline(
                            new KeyFrame(
                                Duration.seconds(2),
                                ev -> openRoleSelection()
                            )
                        ).play();
                    });
                }
            }).start();
        });

        timeline.play();
    }

    private void openRoleSelection() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(
                    "/com/clinic/fxml/" +
                    "RoleSelection.fxml"));

            Stage stage = (Stage) progressBar
                .getScene().getWindow();

            // Fade out splash
            FadeTransition fadeOut =
                new FadeTransition(
                    Duration.millis(500),
                    progressBar.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.setScene(
                    new Scene(root, 1000, 620));
                stage.setTitle(
                    "BHC System — Select Your Role");

                // Fade in role selection
                FadeTransition fadeIn =
                    new FadeTransition(
                        Duration.millis(500), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}