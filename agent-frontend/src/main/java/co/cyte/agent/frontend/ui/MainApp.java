package co.cyte.agent.frontend.ui;

import co.cyte.agent.config.ConfigManager;
import co.cyte.agent.frontend.ui.controllers.ConfigController;
import co.cyte.agent.frontend.ui.controllers.LoginController;
import co.cyte.agent.frontend.ui.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        ConfigManager configManager = new ConfigManager();
        if (!configManager.configExists()) {
            // Si no existe la configuración, muestra la vista de configuración
            showConfigView(configManager);
        } else {
            // Si existe, cárgala y continúa con el login
            configManager.loadConfig();
            showLoginView(configManager);
        }
    }

    public void showConfigView(ConfigManager configManager) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/cyte/agent/frontend/ui/views/ConfigView.fxml"));
            Parent root = loader.load();
            // El controlador de configuración recibe el ConfigManager
            ConfigController controller = loader.getController();
            controller.setConfigManager(configManager);
            primaryStage.setTitle("Configuración Inicial");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showLoginView(ConfigManager configManager) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/cyte/agent/frontend/ui/views/LoginView.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setMainApp(this);
            primaryStage.setTitle("Login");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/cyte/agent/frontend/ui/views/MainView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Main Application");
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
