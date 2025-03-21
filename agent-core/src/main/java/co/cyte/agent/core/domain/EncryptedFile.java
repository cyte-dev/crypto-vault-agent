package co.cyte.agent.core.domain;

import co.cyte.agent.core.crypto.EncryptionAlgorithm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class EncryptedFile {
    private final Path filePath;
    private final EncryptionAlgorithm encryptionAlgorithm;

    /**
     * Constructor que recibe la ruta del archivo y la instancia de algoritmo de cifrado.
     *
     * @param filePath la ruta del archivo
     * @param encryptionAlgorithm la implementación a usar para cifrar/descifrar
     */
    public EncryptedFile(Path filePath, EncryptionAlgorithm encryptionAlgorithm) {
        this.filePath = filePath;
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public Path getFilePath() {
        return filePath;
    }

    /**
     * Verifica si el archivo está cifrado.
     * Se considera cifrado si la primera línea es "ARCHIVO CIFRADO".
     *
     * @return true si el archivo está cifrado, false de lo contrario.
     * @throws IOException si ocurre un error al leer el archivo.
     */
    public boolean isEncrypted() throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        return !lines.isEmpty() && lines.get(0).equals("ARCHIVO CIFRADO");
    }

    /**
     * Cifra el archivo utilizando el algoritmo configurado.
     *
     * @throws IOException si ocurre un error al leer o escribir el archivo.
     */
    public void encrypt() throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }
        String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        if (content.startsWith("ARCHIVO CIFRADO")) {
            System.out.println("File already encrypted: " + filePath);
            return;
        }
        String encryptedContent = encryptionAlgorithm.encrypt(content);
        Files.write(filePath, encryptedContent.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("File encrypted: " + filePath);
    }

    /**
     * Descifra el archivo utilizando el algoritmo configurado.
     *
     * @throws IOException si ocurre un error al leer o escribir el archivo.
     */
    public void decrypt() throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }
        String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        if (!content.startsWith("ARCHIVO CIFRADO")) {
            System.out.println("File is not encrypted: " + filePath);
            return;
        }
        String decryptedContent = encryptionAlgorithm.decrypt(content);
        Files.write(filePath, decryptedContent.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("File decrypted: " + filePath);
    }
}
