package co.cyte.agent.backend.controllers;

import co.cyte.agent.backend.filesystem.DokanFileSystem;
import co.cyte.agent.core.domain.Vault;
import co.cyte.agent.core.services.EncryptionService;
import dev.dokan.dokan_java.FileSystemInformation;
import dev.dokan.dokan_java.constants.dokany.MountOption;
import dev.dokan.dokan_java.constants.microsoft.FileSystemFlag;
import dev.dokan.dokan_java.masking.MaskValueSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controlador para gestionar las Vaults del usuario.
 * Permite crear, desbloquear (unlock) y bloquear (lock) Vaults.
 * Además, se encarga de orquestar la comunicación entre Vault y DokanFileSystem.
 */
@RestController
@RequestMapping("/api/vaults")
public class VaultController {

    // Mapa para almacenar las Vaults del usuario (podría provenir de una base de datos en un caso real)
    private final Map<String, Vault> userVaults = new ConcurrentHashMap<>();
    // Mapa para almacenar las instancias de DokanFileSystem montadas por cada Vault
    private final Map<String, DokanFileSystem> mountedFileSystems = new ConcurrentHashMap<>();
    // Map para almacenar los hilos de montaje por vaultId
    private final Map<String, Thread> mountThreads = new ConcurrentHashMap<>();
    private final EncryptionService encryptionService;
    // Información del sistema de archivos necesaria para instanciar DokanFileSystem
    private final FileSystemInformation fileSystemInformation;

    private final AtomicBoolean isMounted = new AtomicBoolean(false);

    @Autowired
    public VaultController(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        MaskValueSet<FileSystemFlag> fsFeatures = MaskValueSet.of(
                FileSystemFlag.CASE_PRESERVED_NAMES
        );
        this.fileSystemInformation = new FileSystemInformation(fsFeatures);
    }

    private Path getNextAvailableDriveLetter() {
        Set<String> usedDrives = new HashSet<>();
        File[] roots = File.listRoots();
        for (File root : roots) {
            usedDrives.add(root.getAbsolutePath().toUpperCase()); // Ej: "C:\", "D:\"
        }

        // Buscar desde Z: hasta D: (evita C: y A:/B:)
        for (char drive = 'Z'; drive >= 'D'; drive--) {
            String drivePath = drive + ":\\";
            if (!usedDrives.contains(drivePath)) {
                return Paths.get(drivePath);
            }
        }

        throw new RuntimeException("No hay letras de unidad disponibles para montar la bóveda.");
    }

    /**
     * Crea una nueva Vault para el usuario, dado un identificador y la ruta en disco.
     *
     * @param vaultId   Identificador único de la Vault.
     * @param vaultPath Ruta en disco donde se almacenan los archivos cifrados.
     * @return Respuesta indicando que la Vault fue creada.
     */
    @PostMapping("/create")
    public ResponseEntity<String> createVault(@RequestParam String vaultId,
                                              @RequestParam String vaultPath) {
        Path path = Paths.get(vaultPath);
        Vault vault = new Vault(path, encryptionService);
        userVaults.put(vaultId, vault);
        return ResponseEntity.ok("Vault creada con id: " + vaultId);
    }

    /**
     * Desbloquea (unlock) la Vault identificada, leyendo los archivos cifrados y
     * cargándolos en memoria. A continuación, instancia DokanFileSystem y se monta
     * la unidad virtual usando las opciones especificadas.
     *
     * @param vaultId Identificador de la Vault a desbloquear.
     * @return Respuesta indicando el resultado de la operación.
     */
    @PostMapping("/{vaultId}/unlock")
    public ResponseEntity<String> unlockVault(@PathVariable String vaultId) {
        Vault vault = userVaults.get(vaultId);
        if (vault == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            vault.unlock();

            // Obtener una letra de unidad disponible
            Path dynamicMountDrive = getNextAvailableDriveLetter();

            DokanFileSystem fs = new DokanFileSystem(
                    vault.getVaultPath(),
                    fileSystemInformation,
                    vault,
                    dynamicMountDrive.toString()
            );

            MaskValueSet<MountOption> mountOptions = MaskValueSet.of(
                    MountOption.ALT_STREAM,
                    MountOption.NETWORK_DRIVE,
                    MountOption.DEBUG_MODE
            );

            Thread mountThread = new Thread(() -> {
                try {
                    fs.mount(dynamicMountDrive, mountOptions);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            mountThread.setDaemon(false);
            mountThread.start();

            mountedFileSystems.put(vaultId, fs);
            mountThreads.put(vaultId, mountThread);
            return ResponseEntity.ok("Vault desbloqueada y montada en " + dynamicMountDrive);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error desbloqueando vault: " + e.getMessage());
        }
    }
    /**
     * Bloquea (lock) la Vault identificada, cifrando el contenido actual en memoria y
     * persistiendo los archivos cifrados en disco. También desmonta la unidad virtual.
     *
     * @param vaultId Identificador de la Vault a bloquear.
     * @return Respuesta indicando el resultado de la operación.
     */
    @PostMapping("/{vaultId}/lock")
    public ResponseEntity<String> lockVault(@PathVariable String vaultId) {
        Vault vault = userVaults.get(vaultId);
        if (vault == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            // Recuperar el sistema de archivos y el hilo de montaje asociados
            DokanFileSystem fs = mountedFileSystems.remove(vaultId);
            Thread mountThread = mountThreads.remove(vaultId);
            if (fs != null) {
                System.out.println("Solicitando desmontaje de la unidad virtual.");
                // Llamar a unmount() para pedir que se termine la sesión de Dokan
                fs.unmount();
                // Si el hilo sigue bloqueado, forzamos la interrupción
                if (mountThread != null) {
                    // Se envía la interrupción para que, si fs.mount() no responde, se salga del bloqueo
                    mountThread.interrupt();
                    mountThread.join(5000); // Esperar hasta 5 segundos a que finalice
                }
                fs.close();
                fs = null;
                System.gc();
                System.out.println("Unidad virtual desmontada y cerrada.");
            }
            // Bloquear la Vault: cifra y persiste los archivos en disco
            vault.lock();
            return ResponseEntity.ok("Vault bloqueada y cambios guardados.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error bloqueando vault: " + e.getMessage());
        }
    }

    /**
     * Lista los identificadores de Vaults disponibles para el usuario.
     *
     * @return Lista de Vaults.
     */
    @GetMapping
    public ResponseEntity<Map<String, Vault>> listVaults() {
        return ResponseEntity.ok(userVaults);
    }

    @GetMapping("/{vaultId}/status")
    public ResponseEntity<String> getVaultStatus(@PathVariable String vaultId) {
        if (!userVaults.containsKey(vaultId)) {
            return ResponseEntity.notFound().build();
        }
        boolean isMounted = mountedFileSystems.containsKey(vaultId);
        return ResponseEntity.ok(isMounted ? "mounted" : "locked");
    }
}
