package co.cyte.agent.core.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Representa un archivo en su forma cifrada, tal como se almacena en la bóveda.
 *
 * Contiene la información esencial:
 * - originalName: el nombre original del archivo (sin la extensión de cifrado, por ejemplo, sin ".cv").
 * - filePath: la ubicación del archivo cifrado en el sistema de archivos.
 * - fileSize: el tamaño del archivo cifrado, en bytes.
 *
 * Esta clase sirve como modelo de dominio y se puede complementar con métodos de
 * validación o de acceso a metadatos en futuras versiones.
 */
public class EncryptedFile {

    private final String originalName;
    private final Path filePath;
    private final long fileSize;

    /**
     * Crea una instancia de EncryptedFile.
     *
     * @param originalName El nombre original del archivo (sin extensión de cifrado).
     * @param filePath La ruta completa del archivo cifrado en disco.
     * @param fileSize El tamaño del archivo cifrado, en bytes.
     */
    public EncryptedFile(String originalName, Path filePath, long fileSize) {
        this.originalName = originalName;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    public String toString() {
        return "EncryptedFile{" +
                "originalName='" + originalName + '\'' +
                ", filePath=" + filePath +
                ", fileSize=" + fileSize +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncryptedFile)) return false;
        EncryptedFile that = (EncryptedFile) o;
        return fileSize == that.fileSize &&
                Objects.equals(originalName, that.originalName) &&
                Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalName, filePath, fileSize);
    }
}
