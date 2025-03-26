package co.cyte.agent.frontend.ui.controllers;

import co.cyte.agent.config.ConfigManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ConfigController {

    @FXML private TextField txtServerIP;
    @FXML private TextField txtRootPath;

    private ConfigManager configManager;

    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @FXML
    public void handleSaveConfig(ActionEvent event) {
        try {
            String serverIP = txtServerIP.getText().trim();
            String rootPath = txtRootPath.getText().trim();

            if (serverIP.isEmpty() || rootPath.isEmpty()) {
                // Muestra un mensaje de error, p.ej., usando un Alert
                return;
            }
            configManager.saveConfig(serverIP, rootPath);

            // Cerrar la ventana de configuración y lanzar el login
            Stage stage = (Stage) txtServerIP.getScene().getWindow();
            stage.close();

            // Aquí podrías lanzar la vista de Login
            // Por ejemplo, llamar a un método en MainApp para continuar el flujo
        } catch (Exception e) {
            e.printStackTrace();
            // Mostrar alerta de error
        }
    }
}
