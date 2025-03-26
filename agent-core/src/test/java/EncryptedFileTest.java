import co.cyte.agent.core.domain.EncryptedFile;
import org.junit.jupiter.api.Test;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

public class EncryptedFileTest {

    @Test
    public void testEqualsAndHashCode() {
        EncryptedFile file1 = new EncryptedFile("documento", Paths.get("/vaultTest/documento.cv"), 1024);
        EncryptedFile file2 = new EncryptedFile("documento", Paths.get("/vaultTest/documento.cv"), 1024);
        EncryptedFile file3 = new EncryptedFile("otro", Paths.get("/vaultTest/otro.cv"), 2048);

        assertEquals(file1, file2, "Los archivos con misma metadata deben ser iguales");
        assertEquals(file1.hashCode(), file2.hashCode(), "Los hashCode deben ser iguales para archivos iguales");
        assertNotEquals(file1, file3, "Archivos diferentes no deben ser iguales");
    }

    @Test
    public void testToString() {
        EncryptedFile file = new EncryptedFile("doc", Paths.get("/vaultTest/doc.cv"), 512);
        String str = file.toString();
        assertTrue(str.contains("doc"), "El toString debe contener el nombre del archivo");
        assertTrue(str.contains("512"), "El toString debe contener el tamaño del archivo");
    }
}
