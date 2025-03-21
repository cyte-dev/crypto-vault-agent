package co.cyte.agent.core.domain;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Vault {
    private final Path vaultPath;
    private final List<EncryptedFile> encryptedFiles;
    private boolean unlocked;

    // Referencia al servicio de unidad virtual (inyectado o configurado externamente)
    private VirtualDrive virtualDrive;
    private final String mountPoint = "X:\\"; // Ejemplo: letra de unidad virtual

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
        this.encryptedFiles = new ArrayList<>();
        loadEncryptedFiles();
        this.virtualDrive = virtualDrive;
        this.unlocked = false;
    }

    private void loadEncryptedFiles() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(vaultPath)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    encryptedFiles.add(new EncryptedFile(file));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading encrypted files: " + e.getMessage());
        }
    }

    /**
     * Desbloquea la bóveda: descifra todos los archivos y monta la unidad virtual.
     */
    public void unlockVault() {
        if (unlocked) {
            System.out.println("Vault is already unlocked.");
            return;
        }
        // Descifrar archivos (simulación)
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

    /**
     * Bloquea la bóveda: cifra todos los archivos y desmonta la unidad virtual.
     */
    public void lockVault() {
        if (!unlocked) {
            System.out.println("Vault is already locked.");
            return;
        }
        // Desmontar la unidad virtual
        int result = virtualDrive.unmount(mountPoint);
        if (result == 0) {
            // Opcional: volver a cifrar archivos si fuera necesario.
            for (EncryptedFile ef : encryptedFiles) {
                try {
                    ef.encrypt();
                } catch (IOException e) {
                    System.err.println("Error encrypting file " + ef.getFilePath() + ": " + e.getMessage());
                }
            }
            unlocked = false;
            System.out.println("Vault locked and virtual drive unmounted.");
        } else {
            System.err.println("Failed to unmount virtual drive. Error code: " + result);
        }
    }

    public List<EncryptedFile> getEncryptedFiles() {
        return encryptedFiles;
    }
}
