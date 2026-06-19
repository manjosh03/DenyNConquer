package com.example.denyconquer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage launchSet) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(WaitingStageController.class.getResource("ConnectionScreen.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            launchSet.setScene(scene);
            launchSet.show();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}