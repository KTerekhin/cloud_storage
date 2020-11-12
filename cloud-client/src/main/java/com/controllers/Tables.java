package com.controllers;

import com.help.utils.FileInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class Tables {
    public static void prepareTable(TableView<FileInfo> tableView) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>("Type");
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Name");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        filenameColumn.setPrefWidth(180);

        TableColumn<FileInfo, Long> filesizeColumn = new TableColumn<>("Size");
        filesizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        filesizeColumn.setPrefWidth(100);
        filesizeColumn.setCellFactory(column -> new TableCell<FileInfo, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    String text = String.format("%d bytes", item);
                    if (item == -1) {
                        text = "[DIR]";
                    }
                    setText(text);
                }
            }
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Last Update");
        fileDateColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(200);

        tableView.getColumns().addAll(fileTypeColumn, filenameColumn, filesizeColumn, fileDateColumn);
        tableView.getSortOrder().add(fileTypeColumn);

//        tableView.getItems().clear();
//        tableView.getSelectionModel().select(0);
    }

    public static void prepareComboBox(ComboBox<String> comboBox) {
        for (Path path : FileSystems.getDefault().getRootDirectories()) {
            comboBox.getItems().add(path.toString());
        }
        comboBox.getSelectionModel().select(0);
    }
}
