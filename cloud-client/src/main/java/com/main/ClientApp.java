package com.main;

import com.client.Network;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ClientApp extends Application {
    private Stage stage;
    private static ClientApp instance;
    private static Network network;

    public ClientApp() {
        instance = this;
    }

    public static ClientApp getInstance() {
        return instance;
    }

    public void gotoMainApp() {
        try {
            replaceSceneContent("/fxml/MainAppController.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void gotoLogin() {
        try {
            replaceSceneContent("/fxml/Auth.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void gotoSignIn() {
        try {
            replaceSceneContent("/fxml/RegWindow.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void replaceSceneContent(String fxml) throws Exception {
        Parent page = FXMLLoader.load(ClientApp.class.getResource(fxml), null, new JavaFXBuilderFactory());
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(page);
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(page);
        }
        stage.sizeToScene();
    }

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new StackPane());
        stage = primaryStage;
        primaryStage.setOnCloseRequest(event -> {
            network.close();
            Platform.exit();
        });
        gotoLogin();
        primaryStage.show();
    }

    public static void main(String[] args) {
        network = new Network("localhost", 8189);
        launch();
    }
}