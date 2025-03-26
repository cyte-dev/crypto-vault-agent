package co.cyte.agent.core.domain;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Interfaz que define el contrato de una unidad virtual.
 * Permite obtener el mapa de archivos descifrados en memoria,
 * de modo que otros componentes puedan operar sobre el contenido.
 */
public interface VirtualDrive {
    /**
     * Retorna el mapa que asocia el nombre de archivo con su contenido descifrado.
     * @return Map en el que la clave es el nombre del archivo y el valor es su contenido en memoria.
     */
    Map<String, ByteArrayOutputStream> getDecryptedFiles();
}
