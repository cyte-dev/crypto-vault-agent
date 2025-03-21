package co.cyte.agent.core.domain;

public interface VirtualDrive {
    /**
     * Monta la unidad virtual en la ruta especificada.
     *
     * @param mountPoint la letra o ruta de montaje
     * @return 0 si la operación fue exitosa; otro valor indica error.
     */
    int mount(String mountPoint);

    /**
     * Desmonta la unidad virtual.
     *
     * @param mountPoint la letra o ruta de montaje que se debe desmontar.
     * @return 0 si la operación fue exitosa; otro valor indica error.
     */
    int unmount(String mountPoint);
}
