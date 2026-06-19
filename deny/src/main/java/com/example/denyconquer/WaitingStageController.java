package com.example.denyconquer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import networking.client.ClientNetwork;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicReference;


public class WaitingStageController {

    @FXML
    private TextField hostName;
    @FXML
    private TextField portText;
    @FXML
    private Text connectMessage;
    @FXML
    private Button connectButton;
    @FXML
    private ClientNetwork networkClient;


    @FXML
    private void connectButtonClicked(MouseEvent mouseEvent) {
        AtomicReference<Boolean> isSuccess = new AtomicReference<>(false);

        // The default host and port are the prompt text
        String host = hostName.getText().isBlank() ? hostName.getPromptText() : hostName.getText();
        String port = portText.getText().isBlank() ? portText.getPromptText() : portText.getText();

        // Attempt to connect in another thread to prevent UI from freezing during the processes
        Thread connectThread = new Thread(() -> {
            try {
                connectMessage.setFill(Color.BLACK);
                connectMessage.setText("Connecting...");
                connectMessage.setVisible(true);

                networkClient = new ClientNetwork(host, port);

                // Only reaches here if no exceptions
                connectMessage.setText("Connection Successful");
                connectButton.setDisable(true); // Don't allow connecting multiple times
                //registerColorButton.setDisable(false); // Now allow registering colours
                isSuccess.set(true);
            }
            catch(ConnectException e) {
                e.printStackTrace();
                connectMessage.setFill(Color.RED);
                connectMessage.setText("Unable to connect");
            }
            catch(IllegalArgumentException e) {
                e.printStackTrace();
                connectMessage.setFill(Color.RED);
                connectMessage.setText("Invalid hostname or port");
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        });

        connectThread.start();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ChooseColor.fxml"));
                Parent root = loader.load();
                ((Stage) ((Node) mouseEvent.getSource()).getScene().getWindow()).close(); // Get the stage and close it

                Scene scene = new Scene(root);
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.setTitle("ChooseColor");
                stage.show();

                ChooseColorController chooseColorControllerObj = loader.getController();
                chooseColorControllerObj.setNetworkClient(networkClient);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
        }
    }

    @FXML
    private void unFocus(MouseEvent e) {
        ((AnchorPane)e.getSource()).requestFocus();
    }

}


