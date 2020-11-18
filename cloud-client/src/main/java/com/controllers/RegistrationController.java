package com.controllers;

import com.client.Network;
import com.help.utils.FileService;
import com.main.ClientApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class RegistrationController implements Initializable {
    private Network network = Network.getInstance();
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    Button RegButton;
    @FXML
    Button BackButton;
    FileService fileService = new FileService();

    public void btnRegOnAction(ActionEvent event) {
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showDialog("Registration error", "First you need to enter your login, password!", Alert.AlertType.ERROR);
        } else {
            String login = loginField.getText();
            String password = passwordField.getText();
            fileService.sendCommand(network.getCurChannel(), String.format("/registration\n%s\n%s", login, password));
        }
    }

    public void btnBackOnAction(ActionEvent actionEvent) {
        Platform.runLater(this::toAuth);
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
                    Platform.runLater(this::toAuth);
                }
            });
        });
        t.start();
    }

    private void toAuth() {
        ClientApp.getInstance().gotoLogin();
    }
}
