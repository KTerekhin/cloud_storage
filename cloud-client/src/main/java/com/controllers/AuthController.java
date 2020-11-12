package com.controllers;

import com.client.Network;
import com.main.ClientApp;
import com.help.utils.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {
    private Network network = Network.getInstance();
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    Button loginButton;
    @FXML
    Button exitButton;
    FileService fileService = new FileService();

    public void btnLoginOnAction(ActionEvent event) {
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showDialog("Authorization error", "First you need to enter your username and password!", Alert.AlertType.ERROR);
        } else {
            String username = loginField.getText();
            String password = passwordField.getText();
            fileService.sendCommand(network.getCurChannel(), String.format("/authorization\n%s\n%s", username, password));
        }
    }

    public void btnExitOnAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void showDialog(String title, String msg, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Thread t = new Thread(() -> {
            network.getHandler().setCallback(serviceMsg -> {
                System.out.println(serviceMsg);
                if (serviceMsg.equals("OK")) {
                    Platform.runLater(this::toMain);
                }
            });
        });
        t.start();
    }

    private void toMain() {
        ClientApp.getInstance().gotoMainApp();
    }
}
