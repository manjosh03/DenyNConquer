package com.example.denyconquer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import networking.client.ClientNetwork;

import java.io.IOException;


public class ChooseColorController {


    @FXML
    private GridPane colorGrid;
    @FXML
    private Text colorMessage;
    @FXML
    private Canvas selectedColorCanvas;

    @FXML
    private TextField redTextField;
    @FXML
    private TextField greenTextField;
    @FXML
    private TextField blueTextField;
    @FXML
    private Button registerColorButton;
    @FXML
    private Button connectButton;
    @FXML
    private Button startGameButton;


    private ClientNetwork networkClient;

    private final Color colours[] = {
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.CHOCOLATE,
            Color.ORANGE,
            Color.BLACK,
    };

    @FXML
    public void initialize() {
        for(int x = 0; x < 3; x++) {
            for(int y = 0; y < 2; y++) {

                Canvas canvas = new Canvas(30, 20);
                colorCanvas(canvas, 2*x + y);
                StackPane pane = new StackPane(canvas);

                canvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        canvasClicked(canvas);
                    }
                });

                pane.setMaxSize(canvas.getWidth() + 3, canvas.getHeight() + 3);
                pane.setStyle("-fx-background-color: black");
                colorGrid.add(pane, x, y);
            }

        }
    }

    public void setNetworkClient(ClientNetwork client) {
        networkClient = client;
    }


    private void canvasClicked(Canvas canvas) {
        colorMessage.setVisible(false); // Hide message once canvas clicked
        GraphicsContext gc = selectedColorCanvas.getGraphicsContext2D();
        Paint canvasPaint = canvas.getGraphicsContext2D().getFill();
        gc.setFill(canvasPaint);
        gc.fillRect(0, 0, selectedColorCanvas.getWidth(), selectedColorCanvas.getHeight());
//        redTextField.setText(Double.toString(((Color)canvasPaint).getRed()));
//        blueTextField.setText(Double.toString(((Color)canvasPaint).getBlue()));
//        greenTextField.setText(Double.toString(((Color)canvasPaint).getGreen()));
        registerColorButton.setDisable(false);
    }
    private void colorCanvas(Canvas canvas, int colorIndex) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(colours[colorIndex]);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }


    @FXML
    private void registerColorButtonClicked(MouseEvent e) {
        boolean success = networkClient.registerColor((Color)selectedColorCanvas.getGraphicsContext2D().getFill());
        if(success) {
            colorMessage.setFill(Color.BLACK);
            colorMessage.setText("Colour selected successfully"); // Extra spaces prevent UI from moving when text changes
            startGameButton.setDisable(false);
        }
        else {
            colorMessage.setFill(Color.RED);
            colorMessage.setText("Already taken");
        }

        colorMessage.setVisible(true);
    }

    @FXML
    private void startGameButtonClicked(MouseEvent e) {
        networkClient.startClient();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Lobby.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Waiting Area");

            LobbyController lobbyController = loader.getController();
            lobbyController.setNetworkClient(networkClient);

            stage.show();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        ((Stage)((Node)e.getSource()).getScene().getWindow()).close(); // Get the stage and close it
    }

    private void displayCustomColor() {
        try {
            double red = Double.parseDouble(redTextField.getText());
            double green = Double.parseDouble(greenTextField.getText());
            double blue = Double.parseDouble(blueTextField.getText());

            // Only get here if no exception

            if(red <= 1 && green <= 1 && blue <= 1) {
                Color color = new Color(red, green, blue, 1);
                GraphicsContext gc = selectedColorCanvas.getGraphicsContext2D();
                gc.setFill(color);
                gc.fillRect(0, 0, selectedColorCanvas.getWidth(), selectedColorCanvas.getHeight());
                registerColorButton.setDisable(false);
            }
        }
        catch(NumberFormatException ignored) {
            // Ignore error
        }


    }


    private class NumericInput implements ChangeListener<String> {

        private final TextField textField;

        NumericInput(TextField textField) {
            this.textField = textField;
        }

        @Override
        public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
            if(!newValue.matches("\\d*")) {
                textField.setText(oldValue);
            }
            else {
                displayCustomColor();
            }
        }
    }

    private class DecimalInput implements ChangeListener<String> {

        private final TextField textField;

        DecimalInput(TextField textField) {
            this.textField = textField;
        }

        @Override
        public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
            if(!newValue.matches("\\d*(\\.\\d*)?")) {
                textField.setText(oldValue);
            }
            else {
                displayCustomColor();
            }
        }
    }

}


