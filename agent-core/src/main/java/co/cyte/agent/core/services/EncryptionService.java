package co.cyte.agent.core.services;

import co.cyte.agent.core.crypto.AESCipher;
import co.cyte.agent.core.crypto.EncryptionAlgorithm;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Servicio encargado de realizar operaciones de cifrado y descifrado.
 *
 * Utiliza internamente una implementación de EncryptionAlgorithm (en este caso, AESCipher)
 * para delegar la lógica de transformación.
 */
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    private final EncryptionAlgorithm encryptionAlgorithm;

    /**
     * Constructor por defecto que utiliza AESCipher como algoritmo de cifrado.
     */
    public EncryptionService() {
        this.encryptionAlgorithm = new AESCipher();
    }

    /**
     * Cifra los datos del InputStream y escribe el resultado en el OutputStream.
     *
     * @param in    El InputStream con los datos originales.
     * @param alias Alias que identifica el algoritmo (por ejemplo, "AES").
     * @param out   El OutputStream donde se escribirán los datos "cifrados".
     * @throws Exception Si ocurre algún error durante la operación.
     */
    public void encrypt(InputStream in, String alias, OutputStream out) throws Exception {
        encryptionAlgorithm.encrypt(in, alias, out);
    }

    /**
     * Descifra los datos del InputStream y escribe el resultado en el OutputStream.
     *
     * @param in    El InputStream con los datos "cifrados".
     * @param alias Alias que identifica el algoritmo.
     * @param out   El OutputStream donde se escribirán los datos "descifrados".
     * @throws Exception Si ocurre algún error durante la operación.
     */
    public void decrypt(InputStream in, String alias, OutputStream out) throws Exception {
        encryptionAlgorithm.decrypt(in, alias, out);
    }
}
