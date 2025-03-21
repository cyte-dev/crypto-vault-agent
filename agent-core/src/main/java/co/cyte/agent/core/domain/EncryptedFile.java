package co.cyte.agent.core.domain;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * Representa un archivo cifrado de la bóveda.
 * Esta implementación simula el cifrado anteponiendo la línea "ARCHIVO CIFRADO"
 * y el descifrado reemplazándola por "ARCHIVO DESCIFRADO".
 */
public class EncryptedFile {
    private final Path filePath;

    /**
     * Constructor que recibe la ruta del archivo.
     *
     * @param filePath ruta del archivo
     */
    public EncryptedFile(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Retorna la ruta del archivo.
     *
     * @return la ruta del archivo
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Verifica si el archivo está cifrado.
     * Se considera cifrado si la primera línea es "ARCHIVO CIFRADO".
     *
     * @return true si el archivo está cifrado, false en caso contrario.
     * @throws IOException si ocurre un error al leer el archivo.
     */
    public boolean isEncrypted() throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }
        List<String> lines = Files.readAllLines(filePath);
        return !lines.isEmpty() && "ARCHIVO CIFRADO".equals(lines.get(0));
    }

    /**
     * Simula el cifrado del archivo anteponiendo la línea "ARCHIVO CIFRADO".
     *
     * @throws IOException si ocurre un error al leer o escribir el archivo.
     */
    public void encrypt() throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }
        List<String> lines = Files.readAllLines(filePath);
        // Si ya está cifrado, no se hace nada.
        if (!lines.isEmpty() && "ARCHIVO CIFRADO".equals(lines.get(0))) {
            return;
        }
        // Anteponer la línea de cifrado.
        lines.add(0, "ARCHIVO CIFRADO");
        Files.write(filePath, lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("File encrypted: " + filePath);
    }

    /**
     * Simula el descifrado del archivo reemplazando "ARCHIVO CIFRADO" por "ARCHIVO DESCIFRADO".
     *
     * @throws IOException si ocurre un error al leer o escribir el archivo.
     */
    public void decrypt() throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }
        List<String> lines = Files.readAllLines(filePath);
        if (!lines.isEmpty() && "ARCHIVO CIFRADO".equals(lines.get(0))) {
            lines.set(0, "ARCHIVO DESCIFRADO");
            Files.write(filePath, lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("File decrypted: " + filePath);
        } else {
            System.out.println("File is not encrypted: " + filePath);
        }
    }
}
