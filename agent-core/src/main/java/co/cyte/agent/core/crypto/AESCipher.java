package co.cyte.agent.core.crypto;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO: Implementación simulada de EncryptionAlgorithm utilizando "AES".
 */
public class AESCipher implements EncryptionAlgorithm {

    @Override
    public void encrypt(InputStream in, String alias, OutputStream out) throws Exception {
        // Simulación: se copia el contenido sin transformar.
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    }

    @Override
    public void decrypt(InputStream in, String alias, OutputStream out) throws Exception {
        // Simulación: se copia el contenido sin transformar.
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    }
}
