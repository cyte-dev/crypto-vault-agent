package co.cyte.agent.frontend.ui.persistence;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class VaultPersistence {
    private static final String FILE_NAME = "connectedVaults.json";
    private Gson gson = new Gson();
    private Map<String, String> vaults = new HashMap<>();

    public void save() throws IOException {
        try (Writer writer = new FileWriter(FILE_NAME)) {
            gson.toJson(vaults, writer);
        }
    }

    public void load() throws IOException {
        Path filePath = Paths.get(FILE_NAME);
        if (Files.exists(filePath)) {
            try (Reader reader = new FileReader(FILE_NAME)) {
                Type type = new TypeToken<HashMap<String, String>>(){}.getType();
                vaults = gson.fromJson(reader, type);
            }
        }
    }

    public void addVault(String vaultId, String vaultPath) {
        vaults.put(vaultId, vaultPath);
    }

    public void removeVault(String vaultId) throws IOException {
        vaults.remove(vaultId);
        save();
    }

    public Map<String, String> getVaults() {
        return vaults;
    }
}
