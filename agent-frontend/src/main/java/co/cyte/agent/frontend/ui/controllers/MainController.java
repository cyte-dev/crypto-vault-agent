package co.cyte.agent.frontend.ui.controllers;

import co.cyte.agent.frontend.ui.persistence.VaultPersistence;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * MainController gestiona la interfaz principal del frontend.
 *
 * Implementa los métodos para conectar, desconectar, bloquear y desbloquear vaults,
 * invocando los endpoints REST expuestos por el VaultController del backend.
 */
public class MainController {

    @FXML
    private ListView<String> vaultListView; // ListView para mostrar los vaultId conectados

    // HttpClient para enviar solicitudes HTTP al backend
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private VaultPersistence vaultPersistence = new VaultPersistence();

    @FXML private Button btnUnlockVault;
    @FXML private Button btnLockVault;


    public void initialize() {
        // Al inicio, desactivar botones
        btnUnlockVault.setDisable(true);
        btnLockVault.setDisable(true);
        try {
            vaultPersistence.load();
            Map<String, String> storedVaults = vaultPersistence.getVaults();

            for (Map.Entry<String, String> entry : storedVaults.entrySet()) {
                String vaultId = entry.getKey();
                String vaultPath = entry.getValue();

                // Crear vault también en el backend
                String url = "http://localhost:8080/api/vaults/create?vaultId="
                        + URLEncoder.encode(vaultId, StandardCharsets.UTF_8)
                        + "&vaultPath=" + URLEncoder.encode(vaultPath, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    vaultListView.getItems().add(vaultId);
                } else {
                    System.err.println("No se pudo registrar la bóveda en el backend: " + vaultId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al cargar los vaults persistidos: " + e.getMessage());
        }

        // Listener para la selección
        vaultListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateVaultButtonStates(newVal);
            } else {
                btnUnlockVault.setDisable(true);
                btnLockVault.setDisable(true);
            }
        });
    }


    /**
     * Permite seleccionar una carpeta y conectar una Vault.
     * Se abre un DirectoryChooser, se genera un vaultId, y se envía una solicitud POST a
     * /api/vaults/create con los parámetros vaultId y vaultPath.
     *
     * @param event Evento generado al presionar "Connect Vault".
     */
    @FXML
    public void handleConnectVault(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar carpeta de la bóveda");

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            String vaultPath = selectedDirectory.getAbsolutePath();
            String vaultId = selectedDirectory.getName() + "_" + System.currentTimeMillis();
            try {
                String url = "http://localhost:8080/api/vaults/create?vaultId="
                        + URLEncoder.encode(vaultId, StandardCharsets.UTF_8)
                        + "&vaultPath=" + URLEncoder.encode(vaultPath, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    vaultListView.getItems().add(vaultId);
                    // Agregar a la persistencia
                    vaultPersistence.addVault(vaultId, vaultPath);
                    vaultPersistence.save();
                    showAlert("Conexión", "Bóveda conectada exitosamente.\nVault ID: "
                            + vaultId + "\nPath: " + vaultPath);
                    updateVaultButtonStates(vaultId);
                } else {
                    showAlert("Error de conexión", "Error conectando la bóveda: " + response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error de conexión", "Excepción: " + e.getMessage());
            }
        }
    }

    /**
     * Permite desconectar una Vault seleccionada.
     * Envía una solicitud DELETE al endpoint /api/vaults/{vaultId} y, si es exitosa,
     * remueve el vaultId del ListView.
     *
     * @param event Evento generado al presionar "Disconnect Vault".
     */
    public void handleDisconnectVault(ActionEvent event) {
        String vaultId = vaultListView.getSelectionModel().getSelectedItem();
        if (vaultId == null) {
            showAlert("Bóveda desconectada", "Selecciona una bóveda para desconectar.");
            return;
        }

        try {
            String url = "http://localhost:8080/api/vaults/"
                    + URLEncoder.encode(vaultId, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 404) {
                // Eliminar de la lista visual
                vaultListView.getItems().remove(vaultId);

                // Eliminar del JSON de persistencia
                vaultPersistence.removeVault(vaultId);
                showAlert("Desconexión", "Bóveda " + vaultId + " desconectada exitosamente.");
                updateVaultButtonStates(vaultId);
            } else {
                showAlert("Error en desconexión", "Error desconectando la bóveda: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Excepción en desconexión", "Excepción: " + e.getMessage());
        }
    }

    /**
     * Envía una solicitud POST al endpoint /api/vaults/{vaultId}/lock para bloquear la vault.
     *
     * @param event Evento generado al presionar "Lock".
     */
    @FXML
    public void handleLockVault(ActionEvent event) {
        String vaultId = vaultListView.getSelectionModel().getSelectedItem();
        if (vaultId == null) {
            showAlert("Bloqueo de bóveda", "Por favor, seleccione una bóveda para bloquear.");
            return;
        }
        try {
            String url = "http://localhost:8080/api/vaults/"
                    + URLEncoder.encode(vaultId, StandardCharsets.UTF_8)
                    + "/lock";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Bóveda bloqueada", response.body());
                updateVaultButtonStates(vaultId);
            } else {
                showAlert("Error", "Error bloqueando la bóveda: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Excepción", "Excepción bloqueando la bóveda: " + e.getMessage());
        }
    }

    /**
     * Envía una solicitud POST al endpoint /api/vaults/{vaultId}/unlock para desbloquear la vault.
     *
     * @param event Evento generado al presionar "Unlock".
     */
    @FXML
    public void handleUnlockVault(ActionEvent event) {
        String vaultId = vaultListView.getSelectionModel().getSelectedItem();
        if (vaultId == null) {
            showAlert("Desbloqueo de bóveda", "Por favor, seleccione una bóveda para desbloquear.");
            return;
        }
        try {
            String url = "http://localhost:8080/api/vaults/"
                    + URLEncoder.encode(vaultId, StandardCharsets.UTF_8)
                    + "/unlock";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Bóveda desbloqueada", response.body());
                updateVaultButtonStates(vaultId);
            } else {
                showAlert("Error", "Error desbloqueando la bóveda: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Excepción", "Excepción desbloqueando la bóveda: " + e.getMessage());
        }
    }

    private void updateVaultButtonStates(String vaultId) {
        try {
            String url = "http://localhost:8080/api/vaults/"
                    + URLEncoder.encode(vaultId, StandardCharsets.UTF_8) + "/status";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String status = response.body();
                if (status.equalsIgnoreCase("mounted")) {
                    // Si el vault está montado, habilitar "Lock" y deshabilitar "Unlock"
                    btnUnlockVault.setDisable(true);
                    btnLockVault.setDisable(false);
                } else if (status.equalsIgnoreCase("locked")) {
                    // Si el vault no está montado, habilitar "Unlock" y deshabilitar "Lock"
                    btnUnlockVault.setDisable(false);
                    btnLockVault.setDisable(true);
                } else {
                    // En caso de respuesta desconocida, deshabilitar ambos
                    btnUnlockVault.setDisable(true);
                    btnLockVault.setDisable(true);
                }
            } else if (response.statusCode() == 404) {
                    // Si el vault no está registrado en el backend, se asume que está "locked"
                    btnUnlockVault.setDisable(false);
                    btnLockVault.setDisable(true);
            } else {
                btnUnlockVault.setDisable(true);
                btnLockVault.setDisable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnUnlockVault.setDisable(true);
            btnLockVault.setDisable(true);
        }
    }

    /**
     * Método auxiliar para mostrar alertas informativas.
     *
     * @param title   Título de la alerta.
     * @param message Mensaje a mostrar.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}

