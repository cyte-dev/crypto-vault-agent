package co.cyte.agent.core.domain;

import co.cyte.agent.core.services.EncryptionService;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La clase Vault representa la bóveda cifrada en disco.
 *
 * Responsabilidades:
 * - unlock(): Lee los archivos cifrados (con la extensión ".cv") de la ruta indicada,
 *   los descifra utilizando EncryptionService y los carga en un Map en memoria.
 * - lock(): Toma el estado actual de la bóveda (el Map compartido), cifra cada archivo y
 *   persiste el resultado en disco (agregando la extensión ".cv"), limpiando la memoria.
 *
 * Además, al implementar VirtualDrive, expone el mapa de archivos descifrados para que
 * componentes externos (por ejemplo, DokanFileSystem) puedan operar sobre él.
 */
public class Vault implements VirtualDrive {

    private static final String ENCRYPTED_EXTENSION = ".cv";

    private final Path vaultPath;
    private final EncryptionService encryptionService;
    // Mapa que almacena en memoria los archivos descifrados.
    private final Map<String, ByteArrayOutputStream> decryptedFiles;
    private final String alias; // Alias del algoritmo de cifrado (por ejemplo, "AES")

    /**
     * Constructor de Vault.
     *
     * @param vaultPath Ruta de la bóveda en disco.
     * @param encryptionService Servicio para realizar operaciones de cifrado y descifrado.
     */
    public Vault(Path vaultPath, EncryptionService encryptionService) {
        this.vaultPath = vaultPath;
        this.encryptionService = encryptionService;
        this.decryptedFiles = new ConcurrentHashMap<>();
        this.alias = "AES";

        // TODO: Si hay archivos sin cifrar, se cifran al inicializar la Vault
        try {
            if (Files.exists(vaultPath) && Files.isDirectory(vaultPath)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(vaultPath)) {
                    for (Path file : stream) {
                        String fileName = file.getFileName().toString();
                        if (!fileName.endsWith(ENCRYPTED_EXTENSION) && Files.isRegularFile(file)) {
                            // Cifrar el archivo
                            Path encryptedFile = vaultPath.resolve(fileName + ENCRYPTED_EXTENSION);
                            try (InputStream in = Files.newInputStream(file);
                                 OutputStream out = Files.newOutputStream(encryptedFile)) {
                                encryptionService.encrypt(in, alias, out);
                            }

                            // Eliminar el archivo original
                            Files.delete(file);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar los archivos no cifrados presentes en la bóveda.", e);
        }
    }


    /**
     * Retorna la ruta asociada a la bóveda.
     *
     * @return Ruta de la bóveda en disco.
     */
    public Path getVaultPath(){
        return vaultPath;
    }

    /**
     * Retorna el mapa de archivos descifrados en memoria.
     * Esta implementación forma parte del contrato de VirtualDrive.
     *
     * @return Map que asocia el nombre del archivo con su contenido en memoria.
     */
    @Override
    public Map<String, ByteArrayOutputStream> getDecryptedFiles() {
        return decryptedFiles;
    }

    /**
     * Desbloquea la bóveda.
     *
     * Lee los archivos cifrados (se asume que tienen la extensión ".cv", de forma insensible a mayúsculas)
     * de la ruta indicada, los descifra utilizando EncryptionService y los carga en el mapa de archivos descifrados.
     *
     * @throws Exception Si ocurre algún error durante la lectura o el descifrado.
     */
    public void unlock() throws Exception {
        // Validar que la ruta exista y sea un directorio.
        if (!Files.exists(vaultPath) || !Files.isDirectory(vaultPath)) {
            throw new IllegalArgumentException("La ruta de la bóveda no es válida: " + vaultPath);
        }

        // Utilizamos un DirectoryStream con un filtro para archivos que terminen en ".cv" (ignora mayúsculas/minúsculas)
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(vaultPath, entry -> {
            String name = entry.getFileName().toString().toLowerCase();
            return name.endsWith(ENCRYPTED_EXTENSION);
        })) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                // Eliminar la extensión de forma segura:
                fileName = fileName.substring(0, fileName.length() - ENCRYPTED_EXTENSION.length());

                try (InputStream fis = Files.newInputStream(file);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    encryptionService.decrypt(fis, alias, baos);
                    decryptedFiles.put(fileName, baos);
                    System.out.println("Archivo descifrado: " + fileName);
                } catch (Exception e) {
                    System.out.println( "Error al descifrar el archivo: " + file.getFileName());
                }
            }
        }
    }

    /**
     * Bloquea la bóveda.
     *
     * Cifra cada archivo presente en el mapa de archivos descifrados utilizando EncryptionService,
     * persiste el resultado en disco (agregando la extensión ".cv") y limpia el mapa en memoria.
     *
     * @throws Exception Si ocurre algún error durante la encriptación o escritura en disco.
     */
    public void lock() throws Exception {
        // Asegurar que la ruta de la bóveda exista.
        if (!Files.exists(vaultPath)) {
            Files.createDirectories(vaultPath);
        }

        // Lista para acumular errores durante el procesamiento de archivos
        List<String> errores = new ArrayList<>();

        // Iterar sobre cada archivo descifrado en memoria y cifrarlo, sobreescribiendo el archivo original
        for (Map.Entry<String, ByteArrayOutputStream> entry : decryptedFiles.entrySet()) {
            String fileName = entry.getKey();
            ByteArrayOutputStream baos = entry.getValue();
            byte[] decryptedData = baos.toByteArray();

            // Construir la ruta de salida para el archivo cifrado, agregando la extensión ".cv"
            Path outputFile = vaultPath.resolve(fileName + ENCRYPTED_EXTENSION);

            // Asegurarse de que el directorio destino existe
            Path parentDir = outputFile.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Intentar cifrar y escribir el archivo
            try (OutputStream fos = Files.newOutputStream(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                 ByteArrayInputStream bais = new ByteArrayInputStream(decryptedData)) {
                encryptionService.encrypt(bais, alias, fos);
                System.out.println("Archivo cifrado y guardado: " + outputFile);
            } catch (Exception e) {
                String errorMsg = "Error cifrando el archivo " + fileName + ": " + e.getMessage();
                System.out.println(errorMsg);
                errores.add(errorMsg);
            }
        }

        // Si se registraron errores, se lanza una excepción con todos ellos
        if (!errores.isEmpty()) {
            throw new Exception("Se produjeron errores durante el bloqueo de la Vault: " + String.join("; ", errores));
        }

        // Eliminar los archivos cifrados en disco que no están presentes en decryptedFiles.
        // Esto asegura que solo queden en disco los archivos que se cargaron en memoria.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(vaultPath, "*" + ENCRYPTED_EXTENSION)) {
            for (Path file : stream) {
                String encryptedFileName = file.getFileName().toString();
                // Extraer el nombre base quitando la extensión
                if (encryptedFileName.endsWith(ENCRYPTED_EXTENSION)) {
                    String baseName = encryptedFileName.substring(0, encryptedFileName.length() - ENCRYPTED_EXTENSION.length());
                    if (!decryptedFiles.containsKey(baseName)) {
                        Files.delete(file);
                        System.out.println("Archivo eliminado (no presente en memoria): " + file);
                    }
                }
            }
        } catch (IOException ioex) {
            System.out.println("Error eliminando archivos sobrantes: " + ioex.getMessage());
        }

        // Limpiar la memoria una vez que se han procesado correctamente los archivos.
        decryptedFiles.clear();
    }

}
