package co.cyte.agent.backend.filesystem;

import co.cyte.agent.core.domain.VirtualDrive;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import dev.dokan.dokan_java.DokanFileSystemStub;
import dev.dokan.dokan_java.DokanOperations;
import dev.dokan.dokan_java.DokanUtils;
import dev.dokan.dokan_java.FileSystemInformation;
import dev.dokan.dokan_java.constants.microsoft.CreateDisposition;
import dev.dokan.dokan_java.constants.microsoft.NtStatuses;
import dev.dokan.dokan_java.masking.EnumInteger;
import dev.dokan.dokan_java.structure.ByHandleFileInformation;
import dev.dokan.dokan_java.structure.DokanFileInfo;
import dev.dokan.dokan_java.structure.DokanIOSecurityContext;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DokanFileSystem extends DokanFileSystemStub {

    private AtomicLong handleHandler;
    private FileStore fileStore;
    private Path root;
    // Se obtiene la referencia al Map compartido desde VirtualDrive (p.ej., Vault)
    private final Map<String, ByteArrayOutputStream> decryptedFiles;
    private final String mountDrive;
    // Asocia el nombre original con sus archivos temporales
    private final Map<String, Set<String>> tempFilesByOriginal = new ConcurrentHashMap<>();
    private static final ThreadLocal<DokanContext> contextHolder = ThreadLocal.withInitial(DokanContext::new);
    private final AtomicBoolean isMounted = new AtomicBoolean(false);

    /**
     * Constructor que recibe la instancia de VirtualDrive para obtener el mapa compartido
     * de archivos descifrados. DokanFileSystem actuará sobre este Map sin involucrarse en el cifrado.
     *
     * @param root Directorio raíz donde se realiza el montaje.
     * @param fileSystemInformation Información del sistema de archivos.
     * @param virtualDrive Instancia de VirtualDrive (por ejemplo, Vault) que expone el Map.
     * @param mountDrive La letra o identificador de la unidad virtual.
     */
    public DokanFileSystem(Path root, FileSystemInformation fileSystemInformation, VirtualDrive virtualDrive, String mountDrive) {
        super(fileSystemInformation);
        this.root = root;
        this.decryptedFiles = virtualDrive.getDecryptedFiles();
        this.handleHandler = new AtomicLong(0);
        FileStore tmp = null;
        try {
            tmp = Files.getFileStore(this.root);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileStore = tmp;
        this.mountDrive = mountDrive;
    }

    @Override
    public int zwCreateFile(WString rawPath, DokanIOSecurityContext securityContext, int rawDesiredAccess,
                            int rawFileAttributes, int rawShareAccess, int rawCreateDisposition, int rawCreateOptions,
                            DokanFileInfo dokanFileInfo) {

        if (dokanFileInfo == null) {
            return NtStatuses.STATUS_INVALID_PARAMETER;
        }

        // Verificar que la ruta solicitada pertenezca a la unidad virtual
        String requestedPath = rawPath.toString();
        try {
            Path p = Paths.get(requestedPath);
            if (p.isAbsolute()) {
                String drive = (p.getRoot() != null ? p.getRoot().toString() : "");
                if (!drive.equalsIgnoreCase(mountDrive)) {
                    return NtStatuses.STATUS_ACCESS_DENIED;
                }
            }
        } catch (InvalidPathException e) {
            System.err.println("Ruta inválida: " + requestedPath);
            return NtStatuses.STATUS_INVALID_PARAMETER;
        }

        // Obtener la ruta relativa y marcar la raíz como directorio
        String fileName = resolveRelativeFileName(rawPath, dokanFileInfo);

        // Si fileName es vacío, ya se ha marcado como directorio (raíz)
        if (fileName.isEmpty()) {
            return NtStatuses.STATUS_SUCCESS;
        }

        // Si no existe una entrada exacta, verificar si se trata de un directorio
        if (!decryptedFiles.containsKey(fileName)) {
            String dirPrefix = fileName + "\\"; // O usa File.separator según corresponda
            boolean foundDir = false;
            for (String key : decryptedFiles.keySet()) {
                if (key.startsWith(dirPrefix)) {
                    foundDir = true;
                    break;
                }
            }
            if (foundDir) {
                // Se reconoce la ruta como un directorio virtual.
                dokanFileInfo.IsDirectory = 1;
                dokanFileInfo.Context = 1;
                return NtStatuses.STATUS_SUCCESS;
            }
        }

        synchronized (decryptedFiles) {

            boolean fileExists = decryptedFiles.containsKey(fileName);
            CreateDisposition createDisposition = EnumInteger.enumFromInt(rawCreateDisposition,
                    CreateDisposition.values());

            switch (createDisposition) {
                case FILE_SUPERSEDE:
                case FILE_OVERWRITE:
                case FILE_OVERWRITE_IF:
                    // Si el archivo existe, limpiar su contenido sin borrar el objeto en memoria
                    if (fileExists) {
                        decryptedFiles.get(fileName).reset();
                    } else {
                        decryptedFiles.put(fileName, new ByteArrayOutputStream());
                    }
                    dokanFileInfo.Context = this.handleHandler.incrementAndGet();
                    return NtStatuses.STATUS_SUCCESS;

                case FILE_CREATE:
                    if (fileExists) {
                        return NtStatuses.STATUS_OBJECT_NAME_COLLISION;
                    }
                    decryptedFiles.put(fileName, new ByteArrayOutputStream());
                    break;

                case FILE_OPEN:
                case FILE_OPEN_IF:
                    if (!fileExists) {
                        if (createDisposition == CreateDisposition.FILE_OPEN_IF) {
                            decryptedFiles.put(fileName, new ByteArrayOutputStream());
                        } else {
                            return NtStatuses.STATUS_OBJECT_NAME_NOT_FOUND;
                        }
                    }
                    dokanFileInfo.Context = this.handleHandler.incrementAndGet();
                    return NtStatuses.STATUS_SUCCESS;

                default:
                    return NtStatuses.STATUS_INVALID_PARAMETER;
            }

            // Asociar el archivo con el PID del proceso que lo abrió/creó.
            dokanFileInfo.Context = this.handleHandler.incrementAndGet();
            return NtStatuses.STATUS_SUCCESS;
        }
    }

    @Override
    public void cleanup(WString rawPath, DokanFileInfo dokanFileInfo) {
        String fileName = resolveRelativeFileName(rawPath, dokanFileInfo);

        // Si se indica que el archivo se debe borrar al cerrar
        if (dokanFileInfo.deleteOnClose()) {
            // Eliminar el archivo original
            decryptedFiles.remove(fileName);
            // También eliminar sus temporales si existen
            Set<String> tempSet = tempFilesByOriginal.remove(fileName);
            if (tempSet != null) {
                for (String tempName : tempSet) {
                    decryptedFiles.remove(tempName);
                }
            }
        }
    }

    @Override
    public void closeFile(WString rawPath, DokanFileInfo dokanFileInfo) {
        // Obtener la ruta relativa del archivo (incluyendo subcarpetas)
        String fileName = resolveRelativeFileName(rawPath, dokanFileInfo);

        // Solo si el archivo cerrado es el original (es decir, no es un archivo temporal)
        if (!isTemporary(fileName)) {
            // Primero, eliminar los temporales que se registraron en tempFilesByOriginal
            Set<String> tempSet = tempFilesByOriginal.get(fileName);
            if (tempSet != null) {
                for (String tempName : tempSet) {
                    decryptedFiles.remove(tempName);
                }
                tempFilesByOriginal.remove(fileName);
            }

            // Luego, recorrer el mapa de decryptedFiles para buscar archivos temporales que empiecen con "~$"
            List<String> keysToRemove = new ArrayList<>();

            for (String key : decryptedFiles.keySet()) {
                Path keyPath = Paths.get(key);
                String keyBase = keyPath.getFileName().toString();
                if (keyBase.startsWith("~$")) {
                    keysToRemove.add(key);
                }
            }
            for (String key : keysToRemove) {
                decryptedFiles.remove(key);
            }
        }
    }

    @Override
    public int writeFile(WString rawPath, Pointer rawBuffer, int rawBufferLength, IntByReference rawWrittenLength,
                         long rawOffset, DokanFileInfo dokanFileInfo) {

        String fileName = resolveRelativeFileName(rawPath, dokanFileInfo);

        synchronized (decryptedFiles) {
            ByteArrayOutputStream memoryStream = decryptedFiles.get(fileName);

            // Si no existe, se crea nuevo stream automáticamente (para casos de creación)
            if (memoryStream == null) {
                memoryStream = new ByteArrayOutputStream();
                decryptedFiles.put(fileName, memoryStream);
            }
            // Obtener contenido actual del archivo
            byte[] data = memoryStream.toByteArray();

            // Ajustar tamaño si es necesario
            int endOffset = (int) rawOffset + rawBufferLength;
            if (endOffset > data.length) {
                data = Arrays.copyOf(data, endOffset);
            }

            // Leer datos desde Dokan
            byte[] buffer = new byte[rawBufferLength];
            rawBuffer.read(0, buffer, 0, rawBufferLength);

            // Copiar datos en la posición correcta
            System.arraycopy(buffer, 0, data, (int) rawOffset, rawBufferLength);

            // Permitir sobrescritura desde un offset sin resetear todo el contenido
            memoryStream.reset();
            memoryStream.write(data, 0, endOffset);

            rawWrittenLength.setValue(rawBufferLength);
            return NtStatuses.STATUS_SUCCESS;

        }
    }

    @Override
    public int readFile(WString rawPath, Pointer rawBuffer, int rawBufferLength, IntByReference rawReadLength,
                        long rawOffset, DokanFileInfo dokanFileInfo) {

        // Actualizar el contexto con el PID del proceso que invoca la operación
        getContext().pid.set(dokanFileInfo.ProcessId);

        // Detectar si se trata de un intento de copia (por ejemplo, extrayendo el archivo fuera de la unidad virtual)
        if (isCopyAttempt(rawPath.toString())) {
            return NtStatuses.STATUS_ACCESS_DENIED;
        }

        // Obtener el nombre del archivo a partir de rawPath
        String fileName = resolveRelativeFileName(rawPath, dokanFileInfo);

        // Obtener los datos en memoria para el archivo solicitado
        ByteArrayOutputStream baos = decryptedFiles.get(fileName);
        if (baos == null) {
            return NtStatuses.STATUS_OBJECT_NAME_NOT_FOUND;
        }
        byte[] data = baos.toByteArray();

        // Validar límites de lectura
        int offset = (int) rawOffset;
        if (offset >= data.length) {
            rawReadLength.setValue(0);
            return NtStatuses.STATUS_END_OF_FILE;
        }

        // Calcular la cantidad de bytes a leer y copiarlos al buffer de Dokan
        int bytesToRead = Math.min(rawBufferLength, data.length - offset);
        rawBuffer.write(0, data, offset, bytesToRead);
        rawReadLength.setValue(bytesToRead);

        return NtStatuses.STATUS_SUCCESS;
    }

    @Override
    public int getFileInformation(WString rawPath, ByHandleFileInformation fileInfo, DokanFileInfo dokanFileInfo) {
        String rawStr = rawPath.toString(); // Por ejemplo: "\" o "\sub" o "\sub\prueba-sub.docx"

        // Si es la raíz, se devuelve información de directorio.
        if (rawStr.equals("\\") || rawStr.isEmpty()) {
            fileInfo.dwFileAttributes = WinNT.FILE_ATTRIBUTE_DIRECTORY;
            fileInfo.ftCreationTime = getCurrentFileTime();
            fileInfo.ftLastAccessTime = getCurrentFileTime();
            fileInfo.ftLastWriteTime = getCurrentFileTime();
            return NtStatuses.STATUS_SUCCESS;
        }

        // Eliminamos la barra inicial para obtener la ruta relativa, p.ej. "sub" o "sub\prueba-sub.docx"
        String relativePath = rawStr.startsWith("\\") ? rawStr.substring(1) : rawStr;

        // Primero, buscamos una entrada exacta en decryptedFiles
        if (decryptedFiles.containsKey(relativePath)) {
            ByteArrayOutputStream fileData = decryptedFiles.get(relativePath);
            fileInfo.dwFileAttributes = WinNT.FILE_ATTRIBUTE_ARCHIVE | WinNT.FILE_ATTRIBUTE_NORMAL;
            fileInfo.nFileSizeHigh = 0;
            fileInfo.nFileSizeLow = fileData.size();
            fileInfo.ftCreationTime = getCurrentFileTime();
            fileInfo.ftLastAccessTime = getCurrentFileTime();
            fileInfo.ftLastWriteTime = getCurrentFileTime();
            fileInfo.nNumberOfLinks = 1;
            fileInfo.dwVolumeSerialNumber = 0x19831116;
            fileInfo.nFileIndexHigh = 0;
            fileInfo.nFileIndexLow = (int)(relativePath.hashCode() & 0x7FFFFFFF);
            return NtStatuses.STATUS_SUCCESS;
        }

        // Si no se encontró, se verifica si la ruta corresponde a un directorio.
        // Si existe al menos un archivo cuya clave inicie con "relativePath\" se asume que es un directorio.
        String prefix = relativePath + "\\";
        for (String key : decryptedFiles.keySet()) {
            if (key.startsWith(prefix)) {
                fileInfo.dwFileAttributes = WinNT.FILE_ATTRIBUTE_DIRECTORY;
                fileInfo.ftCreationTime = getCurrentFileTime();
                fileInfo.ftLastAccessTime = getCurrentFileTime();
                fileInfo.ftLastWriteTime = getCurrentFileTime();
                return NtStatuses.STATUS_SUCCESS;
            }
        }

        return NtStatuses.STATUS_OBJECT_NAME_NOT_FOUND;
    }

    @Override
    public int findFiles(WString rawPath, DokanOperations.FillWin32FindData fillFindData, DokanFileInfo dokanFileInfo) {
        String rawStr = rawPath.toString();
        // Si es raíz, la ruta relativa es vacía; de lo contrario, eliminamos la barra inicial.
        String currentDir = (rawStr.equals("\\") || rawStr.isEmpty()) ? "" : rawStr.substring(1);

        // Set para evitar listar directorios duplicados.
        Set<String> foldersListed = new HashSet<>();

        try {
            for (Map.Entry<String, ByteArrayOutputStream> entry : decryptedFiles.entrySet()) {
                String key = entry.getKey(); // Ej: "tabla.xlsx" o "sub\prueba-sub.docx"

                if (currentDir.isEmpty()) {
                    // En la raíz:
                    // Si la clave no contiene separadores, es un archivo en la raíz.
                    fillFileFindData(fillFindData, dokanFileInfo, foldersListed, entry, key);
                } else {
                    String prefix = currentDir + "\\";
                    if (key.startsWith(prefix)) {
                        // Extraer la parte restante.
                        String remaining = key.substring(prefix.length());
                        // Si 'remaining' no contiene separadores, es un archivo directamente en el directorio.
                        fillFileFindData(fillFindData, dokanFileInfo, foldersListed, entry, remaining);
                    }
                }
            }
            return NtStatuses.STATUS_SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return NtStatuses.STATUS_IO_DEVICE_ERROR;
        }
    }

    private void fillFileFindData(DokanOperations.FillWin32FindData fillFindData, DokanFileInfo dokanFileInfo, Set<String> foldersListed, Map.Entry<String, ByteArrayOutputStream> entry, String key) {
        if (!key.contains("\\") && !key.contains("/")) {
            WinBase.WIN32_FIND_DATA findData = new WinBase.WIN32_FIND_DATA();
            findData.dwFileAttributes = WinNT.FILE_ATTRIBUTE_ARCHIVE | WinNT.FILE_ATTRIBUTE_NORMAL;
            char[] fileChars = key.toCharArray();
            System.arraycopy(fileChars, 0, findData.cFileName, 0, Math.min(fileChars.length, findData.cFileName.length));
            ByteArrayOutputStream baos = entry.getValue();
            findData.nFileSizeHigh = 0;
            findData.nFileSizeLow = baos.size();
            findData.ftCreationTime = getCurrentFileTime();
            findData.ftLastAccessTime = getCurrentFileTime();
            findData.ftLastWriteTime = getCurrentFileTime();
            fillFindData.fillWin32FindData(findData, dokanFileInfo);
        } else {
            // Si la clave contiene separadores, el primer token es el directorio.
            String folder = key.split("[/\\\\]")[0];
            if (!foldersListed.contains(folder)) {
                foldersListed.add(folder);
                WinBase.WIN32_FIND_DATA findData = new WinBase.WIN32_FIND_DATA();
                findData.dwFileAttributes = WinNT.FILE_ATTRIBUTE_DIRECTORY;
                char[] folderChars = folder.toCharArray();
                System.arraycopy(folderChars, 0, findData.cFileName, 0, Math.min(folderChars.length, findData.cFileName.length));
                findData.ftCreationTime = getCurrentFileTime();
                findData.ftLastAccessTime = getCurrentFileTime();
                findData.ftLastWriteTime = getCurrentFileTime();
                fillFindData.fillWin32FindData(findData, dokanFileInfo);
            }
        }
    }


    @Override
    public int deleteFile(WString rawPath, DokanFileInfo dokanFileInfo) {
        // Extraer el nombre del archivo a partir de la ruta
        String rawStr = rawPath.toString();
        Path p = Paths.get(rawStr);
        String fileName = (p.getFileName() != null ? p.getFileName().toString() : "");
        System.out.println("Eliminando archivo " + fileName);
        // Si el archivo existe en el mapa, se elimina
        if (decryptedFiles.containsKey(fileName)) {
            decryptedFiles.remove(fileName);
            return NtStatuses.STATUS_SUCCESS;
        }
        return NtStatuses.STATUS_OBJECT_NAME_NOT_FOUND;
    }

    @Override
    public int moveFile(WString existingFileName, WString newFileName, boolean replaceIfExisting,
                        DokanFileInfo dokanFileInfo) {
        synchronized (decryptedFiles) {
            String oldName = resolveRelativeFileName(existingFileName, dokanFileInfo);
            String newName = resolveRelativeFileName(newFileName, dokanFileInfo);

            // Obtener la letra de la unidad desde la ruta de los archivos
            String oldDrive = Paths.get(existingFileName.toString()).getRoot().toString();
            String newDrive = Paths.get(newFileName.toString()).getRoot().toString();

            // Bloquear movimientos fuera de la unidad virtual
            if (!oldDrive.equalsIgnoreCase(newDrive)) {
                System.err.println("(moveFile) Intento de mover el archivo fuera de la unidad virtual bloqueado: "
                        + existingFileName + " → " + newFileName);
                return NtStatuses.STATUS_ACCESS_DENIED;
            }

            // Verificar que el archivo exista en la unidad virtual
            if (!decryptedFiles.containsKey(oldName)) {
                return NtStatuses.STATUS_OBJECT_NAME_NOT_FOUND;
            }

            // Si se permite reemplazar y el archivo ya existe en la nueva ubicación, eliminarlo
            if (replaceIfExisting && decryptedFiles.containsKey(newName)) {
                decryptedFiles.remove(newName);
            }

            // Mover (renombrar) dentro de la unidad virtual
            ByteArrayOutputStream fileData = decryptedFiles.remove(oldName);
            decryptedFiles.put(newName, fileData);
            // Si el archivo nuevo es temporal, lo asociamos al original
            if (isTemporary(newName)) {
                // Asumamos que el archivo original es el que se muestra sin prefijo o sin extensión .tmp
                // Por ejemplo, si el temporal es "~$prueba.docx" o "E3F38718.tmp", se asocia a "prueba.docx"
                String originalName = deduceOriginalName(oldName, newName);
                System.out.println("(moveFile) originalName: " + originalName + " - newName");
                tempFilesByOriginal.computeIfAbsent(originalName, k -> new HashSet<>()).add(newName);
            } else {
                // Si se renombra de temporal a original, eliminamos ese temporal de la asociación
                String originalName = newName;
                Set<String> tempSet = tempFilesByOriginal.get(originalName);
                if (tempSet != null) {
                    tempSet.remove(oldName);
                    if (tempSet.isEmpty()) {
                        tempFilesByOriginal.remove(originalName);
                    }
                }
            }
            System.out.println("(moveFile) Archivo renombrado dentro de la unidad virtual: "
                    + oldName + " → " + newName);
        }
        return NtStatuses.STATUS_SUCCESS;
    }

    @Override
    public int getDiskFreeSpace(LongByReference freeBytesAvailable, LongByReference totalNumberOfBytes,
                                LongByReference totalNumberOfFreeBytes, DokanFileInfo dokanFileInfo) {
        if (this.fileStore == null) {
            return NtStatuses.STATUS_UNSUCCESSFUL;
        } else {
            try {
                freeBytesAvailable.setValue(fileStore.getUsableSpace());
                totalNumberOfBytes.setValue(fileStore.getTotalSpace());
                totalNumberOfFreeBytes.setValue(fileStore.getUnallocatedSpace());
                return NtStatuses.STATUS_SUCCESS;
            } catch (IOException e) {
                return NtStatuses.STATUS_IO_DEVICE_ERROR;
            }
        }
    }

    @Override
    public int getVolumeInformation(Pointer rawVolumeNameBuffer, int rawVolumeNameSize,
                                    IntByReference rawVolumeSerialNumber, IntByReference rawMaximumComponentLength,
                                    IntByReference rawFileSystemFlags, Pointer rawFileSystemNameBuffer, int rawFileSystemNameSize,
                                    DokanFileInfo dokanFileInfo) {
        rawVolumeNameBuffer.setWideString(0L, DokanUtils.trimStrToSize(this.volumeName, rawVolumeNameSize));
        rawVolumeSerialNumber.setValue(this.volumeSerialnumber);
        rawMaximumComponentLength.setValue(this.fileSystemInformation.getMaxComponentLength());
        rawFileSystemFlags.setValue(this.fileSystemInformation.getFileSystemFeatures().intValue());
        rawFileSystemNameBuffer.setWideString(0L,
                DokanUtils.trimStrToSize(this.fileSystemInformation.getFileSystemName(), rawFileSystemNameSize));
        return NtStatuses.STATUS_SUCCESS;
    }

    // ----------------------------------------------------------------
    // AUXILIARES
    // ----------------------------------------------------------------

    private String deduceOriginalName(String oldName, String newName) {
        // Convertir las rutas en objetos Path para trabajar con las partes
        Path oldPath = Paths.get(oldName);
        String oldBase = oldPath.getFileName().toString(); // Ej: "prueba.docx"
        Path parent = oldPath.getParent(); // Puede ser "sub"

        // Si el nuevo nombre termina en ".tmp" o ".~tmp", se asume que es un temporal;
        // se devuelve el nombre original, preservando el directorio.
        if (newName.toLowerCase().endsWith(".tmp") || newName.toLowerCase().endsWith(".~tmp")) {
            if (parent != null) {
                return parent.resolve(oldBase).toString();
            } else {
                return oldBase;
            }
        }

        // Si el nombre del nuevo archivo empieza con "~$", quitar ese prefijo y conservar el directorio.
        Path newPath = Paths.get(newName);
        String newBase = newPath.getFileName().toString();
        if (newBase.startsWith("~$")) {
            String base = newBase.substring(2);
            if (parent != null) {
                return parent.resolve(base).toString();
            } else {
                return base;
            }
        }
        // En caso contrario, se retorna newName
        return newName;
    }

    private DokanContext getContext() {
        return contextHolder.get();
    }

    private WinBase.FILETIME getCurrentFileTime() {
        long currentTimeMillis = System.currentTimeMillis();
        long fileTime = (currentTimeMillis + 11644473600000L) * 10000; // Convertir a FILETIME (100ns ticks)

        WinBase.FILETIME ft = new WinBase.FILETIME();
        ft.dwLowDateTime = (int) fileTime;
        ft.dwHighDateTime = (int) (fileTime >> 32);
        return ft;
    }
    /**
     * Genera un nombre corto en formato 8.3 para un nombre que no lo cumple.
     * (Puedes ajustar la lógica según tus necesidades)
     */
    private String generateShortName(String longName) {
        // Separa la parte del nombre y la extensión (si existe)
        String namePart = longName;
        String extension = "";
        int dotIndex = longName.lastIndexOf('.');
        if (dotIndex != -1) {
            namePart = longName.substring(0, dotIndex);
            extension = longName.substring(dotIndex + 1);
        }

        // Eliminar caracteres especiales y convertir a mayúsculas
        namePart = namePart.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        extension = extension.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

        // Limitar la longitud: si es muy largo, se recorta y se añade '~1'
        if (namePart.length() > 6) {
            namePart = namePart.substring(0, 6);
        }

        // Formar el nombre corto en formato 8.3
        return extension.isEmpty() ? String.format("%s~1", namePart) : String.format("%s~1.%s", namePart, extension);
    }
    private boolean isCopyAttempt(String path) {
        try {
            int pid = (int) getContext().pid.get();
            ProcessBuilder pb = new ProcessBuilder("wmic", "process", "where", "ProcessId=" + pid, "get", "CommandLine", "/FORMAT:LIST");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder commandLine = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("CommandLine=")) {
                        commandLine.append(line.substring("CommandLine=".length()).trim());
                    }
                }
            }
            process.waitFor();
            String processCmd = commandLine.toString();

            // Detectar comandos comunes de copia en Windows
            return processCmd.contains("copy") ||
                    processCmd.contains("xcopy") ||
                    processCmd.contains("robocopy") ||
                    processCmd.contains("explorer");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTemporary(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".tmp") || lower.endsWith(".~tmp") || fileName.startsWith("~$");
    }

    /**
     * Verifica si el nombre cumple con el formato 8.3:
     * - La parte del nombre es de máximo 8 caracteres
     * - La extensión es de máximo 3 caracteres (si existe)
     * - Solo contiene caracteres alfanuméricos (u otros permitidos)
     */
    private boolean isValid8Dot3(String fileName) {
        // Separa la parte de nombre y extensión
        String namePart = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            namePart = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex + 1);
        }

        // Verifica longitudes
        if (namePart.length() > 8 || extension.length() > 3) {
            return false;
        }

        // Opcional: verificar que solo contenga caracteres válidos (letras, números, y algunos símbolos permitidos)
        // Aquí se asume que solo se permiten letras y dígitos
        if (!namePart.matches("[A-Za-z0-9]+") || (!extension.isEmpty() && !extension.matches("[A-Za-z0-9]+"))) {
            return false;
        }

        return true;
    }
    private String resolveRelativeFileName(WString rawPath, DokanFileInfo dokanFileInfo) {
        String rawStr = rawPath.toString();
        // Si se trata de la raíz, marcar como directorio.
        if (rawStr.equals("\\") || rawStr.isEmpty()) {
            dokanFileInfo.IsDirectory = 1;
            dokanFileInfo.Context = 1;
            return "";
        }
        // Si tiene una barra inicial, eliminarla para obtener la ruta relativa.
        return rawStr.startsWith("\\") ? rawStr.substring(1) : rawStr;
    }


    @Override
    public int mounted(DokanFileInfo dokanFileInfo) {
        // Se invoca cuando la unidad se ha montado exitosamente
        System.out.println("Callback mounted: Unidad virtual " + mountPoint + " montada.");
        // Actualiza el estado interno, si lo necesitas:
        isMounted.set(true);
        return 0;  // 0 indica éxito.
    }

    @Override
    public int unmounted(DokanFileInfo dokanFileInfo) {
        // Se invoca cuando la unidad se ha desmontado
        System.out.println("Callback unmounted: Unidad virtual " + mountPoint + " desmontada.");
        // Actualiza el estado interno
        isMounted.set(false);
        return 0;
    }

}
