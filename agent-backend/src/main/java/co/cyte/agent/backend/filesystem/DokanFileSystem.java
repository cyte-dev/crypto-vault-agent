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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import java.nio.charset.StandardCharsets;

public class DokanFileSystem extends DokanFileSystemStub implements VirtualDrive {

    private AtomicLong handleHandler;
    private FileStore fileStore;
    private Path root;
    private Map<String, ByteArrayOutputStream> decryptedFiles;
    private final String mountDrive;

    // Mapa global para almacenar el PID autorizado para cada archivo.
    private final Map<String, Integer> fileProcessMap = new ConcurrentHashMap<>();

    // Asocia el nombre original con un conjunto de nombres temporales
    private final Map<String, Set<String>> tempFilesByOriginal = new ConcurrentHashMap<>();

    public DokanFileSystem(Path root, FileSystemInformation fileSystemInformation, String mountDrive) {
        super(fileSystemInformation);
        this.root = root;
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

    public DokanFileSystem(Path root, FileSystemInformation fileSystemInformation,
                                Map<String, ByteArrayOutputStream> decryptedFiles, String mountDrive) {
        super(fileSystemInformation);
        this.root = root;
        this.decryptedFiles = decryptedFiles;
        this.handleHandler = new AtomicLong(0);
        this.mountDrive = mountDrive;
        System.out.println(decryptedFiles.keySet());
    }

    @Override
    public int mount(String mountPoint) {
        System.out.println("Mounting virtual drive at: " + mountPoint);
        return 0;
    }

    @Override
    public int unmount(String mountPoint) {
        System.out.println("Unmounting virtual drive at: " + mountPoint);
        return 0;
    }

}
