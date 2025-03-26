import static org.junit.jupiter.api.Assertions.*;

import co.cyte.agent.backend.filesystem.DokanFileSystem;
import co.cyte.agent.core.domain.Vault;
import co.cyte.agent.core.services.EncryptionService;
import dev.dokan.dokan_java.FileSystemInformation;
import dev.dokan.dokan_java.constants.microsoft.CreateDisposition;
import dev.dokan.dokan_java.constants.microsoft.FileSystemFlag;
import dev.dokan.dokan_java.masking.MaskValueSet;
import dev.dokan.dokan_java.structure.DokanFileInfo;
import com.sun.jna.Memory;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DokanFileSystemTest {

    private DokanFileSystem dokanFS;
    private Vault vault;
    private FileSystemInformation fsInfo;
    private Path root;
    // Simulamos la unidad virtual, por ejemplo "C:\"
    private final String mountDrive = "C:\\";

    private EncryptionService encryptionService;

    @BeforeAll
    public void setupAll() throws Exception {
        // Crear un directorio temporal para simular la raíz del sistema de archivos
        root = Files.createTempDirectory("dokanTestRoot");
        encryptionService = new EncryptionService();
    }

    @BeforeEach
    public void setup() throws Exception {
        // Inicializar la información del sistema de archivos
        MaskValueSet<FileSystemFlag> fsFeatures = MaskValueSet.of(
                FileSystemFlag.CASE_PRESERVED_NAMES
        );
        fsInfo = new FileSystemInformation(fsFeatures);
        // Instanciar Vault (que implementa VirtualDrive) con la ruta raíz y el servicio de cifrado
        vault = new Vault(root, encryptionService);
        // Instanciar DokanFileSystem pasando la instancia de Vault para obtener el Map compartido
        dokanFS = new DokanFileSystem(root, fsInfo, vault, mountDrive);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Limpiar el Map de archivos en memoria de la Vault
        vault.getDecryptedFiles().clear();
    }

    @AfterAll
    public void cleanupAll() throws Exception {
        // Eliminar el directorio temporal y su contenido
        Files.walk(root)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> p.toFile().delete());
    }

    @Test
    public void testZwCreateFile_NewFile() {
        // Simular la creación de un archivo "file1.txt" en la raíz de la unidad virtual
        WString filePath = new WString("\\file1.txt");
        DokanFileInfo fileInfo = new DokanFileInfo();
        int status = dokanFS.zwCreateFile(filePath, null, 0, 0, 0,
                CreateDisposition.FILE_CREATE.intValue(), 0, fileInfo);
        assertEquals(0, status, "zwCreateFile debe retornar éxito (STATUS_SUCCESS)");
        // Verificar que el Map (obtenido desde Vault) contenga la entrada sin la barra inicial
        assertTrue(vault.getDecryptedFiles().containsKey("file1.txt"), "El Map debe contener la clave 'file1.txt'");
    }

    @Test
    public void testWriteFileAndReadFile() {
        // Crear el archivo "file2.txt" usando FILE_CREATE
        WString filePath = new WString("\\file2.txt");
        DokanFileInfo fileInfo = new DokanFileInfo();
        int status = dokanFS.zwCreateFile(filePath, null, 0, 0, 0,
                CreateDisposition.FILE_CREATE.intValue(), 0, fileInfo);
        assertEquals(0, status, "La creación debe ser exitosa");

        // Escribir datos en el archivo
        String content = "Contenido de prueba para file2";
        byte[] contentBytes = content.getBytes();
        Memory writeBuffer = new Memory(contentBytes.length);
        writeBuffer.write(0, contentBytes, 0, contentBytes.length);
        IntByReference writtenLength = new IntByReference();

        status = dokanFS.writeFile(filePath, writeBuffer, contentBytes.length, writtenLength, 0, fileInfo);
        assertEquals(0, status, "writeFile debe retornar éxito");
        assertEquals(contentBytes.length, writtenLength.getValue(), "La cantidad de bytes escritos debe coincidir");

        // Leer el contenido escrito
        Memory readBuffer = new Memory(contentBytes.length);
        IntByReference readLength = new IntByReference();
        status = dokanFS.readFile(filePath, readBuffer, contentBytes.length, readLength, 0, fileInfo);
        assertEquals(0, status, "readFile debe retornar éxito");
        assertEquals(contentBytes.length, readLength.getValue(), "La cantidad de bytes leídos debe coincidir");

        // Alternativamente, leer directamente del ByteArrayOutputStream almacenado en Vault
        ByteArrayOutputStream baos = vault.getDecryptedFiles().get("file2.txt");
        assertNotNull(baos, "El archivo 'file2.txt' debe existir en el Map");
        String storedContent = baos.toString();
        assertEquals(content, storedContent, "El contenido almacenado debe coincidir con lo escrito");
    }

    @Test
    public void testDeleteFile() {
        // Simular que existe el archivo "file3.txt" en el Map a través de Vault
        String fileName = "file3.txt";
        vault.getDecryptedFiles().put(fileName, new ByteArrayOutputStream());
        DokanFileInfo fileInfo = new DokanFileInfo();
        WString filePath = new WString("\\" + fileName);

        int status = dokanFS.deleteFile(filePath, fileInfo);
        assertEquals(0, status, "deleteFile debe retornar éxito");
        assertFalse(vault.getDecryptedFiles().containsKey(fileName), "El archivo debe eliminarse del Map");
    }
}
