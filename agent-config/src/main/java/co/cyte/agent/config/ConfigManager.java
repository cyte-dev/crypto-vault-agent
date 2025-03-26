package co.cyte.agent.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "../config.cfg";
    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
    }

    public boolean configExists() {
        return Files.exists(Paths.get(CONFIG_FILE));
    }

    public void loadConfig() throws IOException {
        try (InputStream is = new FileInputStream(CONFIG_FILE)) {
            properties.load(is);
        }
    }

    public void saveConfig(String serverIP, String rootPath) throws IOException {
        properties.setProperty("serverIP", serverIP);
        properties.setProperty("rootPath", rootPath);
        try (OutputStream os = new FileOutputStream(CONFIG_FILE)) {
            properties.store(os, "Configuración de la aplicación");
        }
    }

    public String getServerIP() {
        return properties.getProperty("serverIP");
    }

    public String getRootPath() {
        return properties.getProperty("rootPath");
    }
}
