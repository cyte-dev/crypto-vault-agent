package co.cyte.agent.frontend.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.io.File;

public class MainController {

    @FXML
    private ListView<String> folderListView;

    @FXML
    private Button lockButton;

    @FXML
    private Button unlockButton;

    @FXML
    public void initialize() {
        // Cargar las carpetas desde el directorio del usuario
        File[] directories = new File(System.getProperty("user.home") + "\\Documents").listFiles(File::isDirectory);

        if (directories != null) {
            for (File dir : directories) {
                folderListView.getItems().add(dir.getName());
            }
        }
    }

    @FXML
    public void onFolderSelected(MouseEvent event) {
        String selectedFolder = folderListView.getSelectionModel().getSelectedItem();
        if (selectedFolder != null) {
            lockButton.setDisable(false);
            unlockButton.setDisable(false);
        }
    }

    @FXML
    public void onLockClicked() {
        String selectedFolder = folderListView.getSelectionModel().getSelectedItem();
        if (selectedFolder != null) {
            System.out.println("Locking folder: " + selectedFolder);
        }
    }

    @FXML
    public void onUnlockClicked() {
        String selectedFolder = folderListView.getSelectionModel().getSelectedItem();
        if (selectedFolder != null) {
            System.out.println("Unlocking folder: " + selectedFolder);
        }
    }
}
