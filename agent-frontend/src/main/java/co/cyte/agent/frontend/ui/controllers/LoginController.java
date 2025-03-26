package co.cyte.agent.frontend.ui.controllers;

import co.cyte.agent.core.domain.UserSession;
import co.cyte.agent.core.services.AuthenticationService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * LoginController gestiona la lógica de autenticación del usuario.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    // Referencia a MainApp para cambiar la escena
    private co.cyte.agent.frontend.ui.MainApp mainApp;

    // Servicio para manejar la autenticación
    private AuthenticationService authenticationService;

    /**
     * Inicializar el controlador.
     */
    @FXML
    public void initialize() {
        authenticationService = new AuthenticationService();
    }

    /**
     * Inyectar la instancia de MainApp.
     *
     * @param mainApp La instancia principal de la aplicación.
     */
    public void setMainApp(co.cyte.agent.frontend.ui.MainApp mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Maneja el evento de login. Si la autenticación es exitosa, cambia a la vista principal.
     */
    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        UserSession session = authenticationService.authenticate(username, password);
        if (session.isAuthenticated()) {
            mainApp.showMainView();
        } else {
            showError("Invalid credentials. Please try again.");
        }
    }

    /**
     * Muestra una alerta de error con el mensaje indicado.
     *
     * @param message Mensaje de error.
     */
    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Login Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
