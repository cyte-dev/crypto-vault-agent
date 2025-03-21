package co.cyte.agent.core.domain;

import co.cyte.agent.core.crypto.AESCipher;
import co.cyte.agent.core.crypto.EncryptionAlgorithm;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Vault {
    private final Path vaultPath;
    private final List<EncryptedFile> encryptedFiles;
    private boolean unlocked;

    // Servicio para montar la unidad virtual (definido en otro módulo)
    private final VirtualDrive virtualDrive;
    private final String mountPoint = "F:\\";

    // Instancia del algoritmo de cifrado/descifrado (simulado)
    private final EncryptionAlgorithm encryptionAlgorithm;

    /**
     * Crea una bóveda en el directorio especificado y recibe la implementación de VirtualDrive.
     * Se instancia el algoritmo de cifrado (AESCipherSimulator) que se usará para cada archivo.
     *
     * @param vaultDirectory ruta del directorio que contendrá la bóveda
     * @param virtualDrive   servicio para montar/desmontar la unidad virtual
     */
    public Vault(String vaultDirectory, VirtualDrive virtualDrive) {
        this.vaultPath = Paths.get(vaultDirectory);
        if (!Files.exists(vaultPath)) {
            try {
                Files.createDirectories(vaultPath);
                System.out.println("Vault created at: " + vaultPath.toAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Error creating vault directory", e);
            }
        }
        this.virtualDrive = virtualDrive;
        // Instanciar la implementación simulada del algoritmo criptográfico
        this.encryptionAlgorithm = new AESCipher();
        this.encryptedFiles = new ArrayList<>();
        loadEncryptedFiles();
        this.unlocked = false;
    }

    /**
     * Carga todos los archivos en el directorio de la bóveda y crea para cada uno una instancia de EncryptedFile,
     * pasando la instancia de EncryptionAlgorithm.
     */
    private void loadEncryptedFiles() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(vaultPath)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    // Se pasa encryptionAlgorithm para que EncryptedFile use AESCipherSimulator
                    encryptedFiles.add(new EncryptedFile(file, encryptionAlgorithm));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading encrypted files: " + e.getMessage());
        }
    }

    /**
     * Desbloquea la bóveda: para cada archivo, llama a decrypt() y, luego, monta la unidad virtual.
     */
    public void unlockVault() {
        if (unlocked) {
            System.out.println("Vault is already unlocked.");
            return;
        }
        // Descifrar todos los archivos (simulación)
        for (EncryptedFile ef : encryptedFiles) {
            try {
                ef.decrypt();
            } catch (IOException e) {
                System.err.println("Error decrypting file " + ef.getFilePath() + ": " + e.getMessage());
            }
        }
        // Montar la unidad virtual
        int result = virtualDrive.mount(mountPoint);
        if (result == 0) {
            unlocked = true;
            System.out.println("Vault unlocked and virtual drive mounted at " + mountPoint);
        } else {
            System.err.println("Failed to mount virtual drive. Error code: " + result);
        }
    }
}
