package co.cyte.agent.core.crypto;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interfaz que define el contrato para los algoritmos de cifrado/descifrado.
 */
public interface EncryptionAlgorithm {
    /**
     * TODO: Simula el cifrado copiando el contenido del stream de entrada al de salida.
     *
     * @param in El InputStream con los datos originales.
     * @param alias Alias que identifica el algoritmo (por ejemplo, "AES").
     * @param out El OutputStream donde se escribirán los datos "cifrados".
     * @throws Exception Si ocurre algún error durante la operación.
     */
    void encrypt(InputStream in, String alias, OutputStream out) throws Exception;

    /**
     * TODO: Simula el descifrado copiando el contenido del stream de entrada al de salida.
     *
     * @param in El InputStream con los datos "cifrados".
     * @param alias Alias que identifica el algoritmo.
     * @param out El OutputStream donde se escribirán los datos "descifrados".
     * @throws Exception Si ocurre algún error durante la operación.
     */
    void decrypt(InputStream in, String alias, OutputStream out) throws Exception;
}
