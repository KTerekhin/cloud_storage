package com.controllers;

import com.client.Network;
import com.database.service.ClientService;
import com.help.utils.FileInfo;
import com.help.utils.FileService;
import com.help.utils.FileType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private Network network = Network.getInstance();

    @FXML
    TextField clientPathField;
    @FXML
    TableView<FileInfo> clientFilesTable;
    @FXML
    TextField serverPathField;
    @FXML
    TableView<FileInfo> serverFilesTable;
    @FXML
    ComboBox<String> diskBox;

    private List<FileInfo> serverFileList;
    FileService fileService = new FileService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Tables.prepareTable(clientFilesTable);
        Tables.prepareComboBox(diskBox);
        Tables.prepareTable(serverFilesTable);

        prepareTableEvents(clientFilesTable);
        prepareTableEvents(serverFilesTable);

        Thread t = new Thread(() -> {
            network.getHandler().setCallback(serviceMsg -> {
                String[] command = serviceMsg.split("\n");
                if (command[0].equals("/FileList")) {
                    if (serviceMsg.split("\n").length != 1) {
                        serverFileList = fileService.createFileList(serviceMsg.split("\n", 2)[1]);
                        Platform.runLater(() -> {
                            serverFilesTable.getItems().clear();
                            serverFileList.forEach(o -> serverFilesTable.getItems().add(o));
                            serverFilesTable.sort();
                        });
                    } else {
                        Platform.runLater(() -> {
                            serverFilesTable.getItems().clear();
                        });
                    }
                } else if (command[0].equals("/updateClientFileList")) {
                    updateList(Paths.get(getCurrentPath()));
                } else if (command[0].equals("/Error")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, command[2], ButtonType.OK);
                    alert.setTitle(command[1]);
                    alert.showAndWait();
                }
            });
        });
        t.start();
        updateList(Paths.get("testClient"));
        updateServerList(ClientService.getUserPath());
    }

    private void prepareTableEvents(TableView<FileInfo> tableView) {
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (tableView.getSelectionModel().getSelectedItem().getType() == FileType.DIRECTORY) {
                    enterToDirectory();
                }
            }
        });
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                enterToDirectory();
            } else if (event.getCode() == KeyCode.DELETE) {
                delete();
            }
        });
    }

    public void enterToDirectory() {
        if (serverFilesTable.isFocused()) {
            fileService.sendCommand(network.getCurChannel(), "/enterToDirectory\n" + getSelectedFileName());
        } else {
            Path path = Paths.get(clientPathField.getText())
                    .resolve(clientFilesTable
                            .getSelectionModel()
                            .getSelectedItem()
                            .getFileName());
            if (Files.isDirectory(path)) {
                updateList(path);
                network.getHandler().setCurrentPath(path);
            }
        }
    }

    public void btnDownload(ActionEvent actionEvent) {
        if (serverFilesTable.isFocused()) {
            network.getHandler().setCurrentPath(Paths.get(getCurrentPath()));
            fileService.sendCommand(network.getCurChannel(), "/download\n" + getSelectedFileName());
        } else {
            try {
                fileService.uploadFile(network.getCurChannel(), getSelectedFile(), null);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
                alert.setTitle(e.getClass().getSimpleName());
                alert.showAndWait();
            } finally {
                updateList(Paths.get(getCurrentPath()));
            }
        }
    }

    public void btnNewFolder(ActionEvent actionEvent) {
        if (clientFilesTable.isFocused()) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Create new folder");
            dialog.setHeaderText("Enter a name for the new folder");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    fileService.createDirectory(Paths.get(getCurrentPath()), result.get());
                } catch (Exception e) {
                    Alert alertError = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
                    alertError.setTitle(e.getClass().getSimpleName());
                    alertError.showAndWait();
                } finally {
                    updateList(Paths.get(getCurrentPath()));
                }
            }
        } else if (serverFilesTable.isFocused()) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Create new folder");
            dialog.setHeaderText("Enter a name for the new folder");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(s -> fileService.sendCommand(network.getCurChannel(), "/mkdir\n" + s));
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select any item on the server-side or client-side", ButtonType.OK);
        }
    }

    public String getCurrentPath() {
        return clientPathField.getText();
    }

    public String getCurrentServerPath() {
        return serverPathField.getText();
    }

    public void updateList(Path path) {
        try {
            clientPathField.setText(path.normalize().toAbsolutePath().toString());
            clientFilesTable.getItems().clear();
            clientFilesTable.getItems()
                    .addAll(Files.list(path)
                            .map(FileInfo::new)
                            .collect(Collectors.toList()));
            clientFilesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Something went wrong", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public String getSelectedFileName() {
        if (clientFilesTable.isFocused()) {
            return clientFilesTable.getSelectionModel().getSelectedItem().getFileName();
        } else if (serverFilesTable.isFocused()) {
            return serverFilesTable.getSelectionModel().getSelectedItem().getFileName();
        } else {
            Alert alertError = new Alert(Alert.AlertType.ERROR, "No one file selected", ButtonType.OK);
            alertError.showAndWait();
            return null;
        }
    }

    private void delete() {
        if (serverFilesTable.isFocused()) {
            String fileName = getSelectedFileName();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete");
            alert.setContentText("Are you sure you want to delete the selected item?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                fileService.sendCommand(network.getCurChannel(), "/delete\n" + fileName);
            }
        } else if (clientFilesTable.isFocused()) {
            Path path = getSelectedFile();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete");
            alert.setContentText("Are you sure you want to delete the selected item?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    fileService.delete(path);
                } catch (IOException e) {
                    Alert alertError = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
                    alertError.setTitle(e.getClass().getSimpleName());
                    alertError.showAndWait();
                }
                updateList(Paths.get(getCurrentPath()));
            }
        } else {
            Alert alertError = new Alert(Alert.AlertType.ERROR, "No one item selected", ButtonType.OK);
            alertError.showAndWait();
        }
    }

    public Path getSelectedFile() {
        return Paths.get(getCurrentPath(), getSelectedFileName());
    }

    public void updateServerList(Path path) {
        fileService.sendCommand(network.getCurChannel(), "/updateFileList\n");
    }

    public void btnDelete(ActionEvent actionEvent) {
        delete();
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        upClientPathDirectory();
    }

    public void upClientPathDirectory() {
        Path upPath = Paths.get(clientPathField.getText()).getParent();
        if (upPath != null) {
            network.getHandler().setCurrentPath(upPath);
            updateList(upPath);
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void btnExitAction(ActionEvent actionEvent) {
        network.close();
        Platform.exit();
    }

    public void btnServerPathUpAction(ActionEvent actionEvent) {
        upServerPathDirectory();
    }

    public void upServerPathDirectory() {
        fileService.sendCommand(network.getCurChannel(), "/upDirectory");
    }

    public void btnRefreshServerFileList(ActionEvent actionEvent) {
        fileService.sendCommand(network.getCurChannel(), "/updateFileList\n");
    }

    public void btnRefreshClientFileList(ActionEvent actionEvent) {
        updateList(Paths.get(getCurrentPath()));
    }
}
