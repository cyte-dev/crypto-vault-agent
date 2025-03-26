import co.cyte.agent.core.domain.Vault;
import co.cyte.agent.core.services.EncryptionService;
import org.junit.jupiter.api.*;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VaultTest {

    private Path tempDir;
    private EncryptionService encryptionService;
    private Vault vault;

    @BeforeAll
    public void setupAll() throws Exception {
        // Crear un directorio temporal para simular la bóveda
        tempDir = Files.createTempDirectory("vaultTest");
        encryptionService = new EncryptionService(); // Utiliza la implementación simulada (AESCipher)
    }

    @BeforeEach
    public void setup() {
        // Instanciar Vault para cada prueba
        vault = new Vault(tempDir, encryptionService);
    }

    @AfterEach
    public void cleanVaultMemory() {
        // Limpiar el mapa de archivos en memoria
        vault.getDecryptedFiles().clear();
    }

    @AfterAll
    public void cleanupAll() throws Exception {
        // Eliminar el directorio temporal y su contenido
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> p.toFile().delete());
    }

    @Test
    public void testUnlockLoadsFiles() throws Exception {
        // Crear un archivo "testfile.cv" en el directorio temporal con contenido de ejemplo
        String fileContent = "Hello World";
        String baseFileName = "testfile";
        Path encryptedFile = tempDir.resolve(baseFileName + ".cv");
        Files.write(encryptedFile, fileContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Invocar unlock() para que Vault lea y "descifre" el archivo
        vault.unlock();

        // Verificar que el Map contenga la entrada sin la extensión
        Map<String, ByteArrayOutputStream> files = vault.getDecryptedFiles();
        assertTrue(files.containsKey(baseFileName), "El archivo 'testfile' debería estar cargado en memoria");

        // Verificar que el contenido leído sea igual al contenido original
        String decryptedContent = files.get(baseFileName).toString();
        assertEquals(fileContent, decryptedContent, "El contenido descifrado debe coincidir con el contenido original");
    }

    @Test
    public void testLockWritesFiles() throws Exception {
        // Simular que Vault tiene un archivo descifrado en memoria
        String fileContent = "Datos a cifrar";
        String baseFileName = "myfile";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fileContent.getBytes());
        vault.getDecryptedFiles().put(baseFileName, baos);

        // Invocar lock() para cifrar y persistir los archivos
        vault.lock();

        // Verificar que el mapa de archivos en memoria esté vacío
        assertTrue(vault.getDecryptedFiles().isEmpty(), "El mapa de archivos debe estar vacío luego de bloquear la vault");

        // Verificar que el archivo cifrado exista en disco con la extensión ".cv"
        Path outputFile = tempDir.resolve(baseFileName + ".cv");
        assertTrue(Files.exists(outputFile), "El archivo cifrado debería existir en disco");

        // Leer el contenido del archivo y verificar que coincida (ya que la 'encriptación' es simulada)
        String encryptedContent = new String(Files.readAllBytes(outputFile));
        assertEquals(fileContent, encryptedContent, "El contenido cifrado debe ser igual al original (simulación)");
    }
}
