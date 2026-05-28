package nl.punishments.paper.config;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigManager {

    private final Properties props = new Properties();
    private final File dataDir;
    private final Logger logger;

    public ConfigManager(File dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.logger = logger;
    }

    public void load() throws IOException {
        if (!dataDir.exists()) dataDir.mkdirs();
        File file = new File(dataDir, "config.properties");
        if (!file.exists()) {
            createDefault(file);
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }
        logger.info("[Punishments] Configuratie geladen.");
    }

    private void createDefault(File file) throws IOException {
        try (OutputStream os = new FileOutputStream(file)) {
            Properties defaults = new Properties();
            defaults.setProperty("db.host", "localhost");
            defaults.setProperty("db.port", "3306");
            defaults.setProperty("db.name", "punishments");
            defaults.setProperty("db.user", "root");
            defaults.setProperty("db.password", "wachtwoord");
            defaults.setProperty("lobby.server", "lobby");
            defaults.store(os, "Punishments Paper Plugin Configuratie");
        }
        logger.info("[Punishments] Standaard configuratie aangemaakt. Pas deze aan in plugins/PunishmentsPaper/config.properties");
    }

    public String get(String key) { return props.getProperty(key, ""); }
    public int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key)); } catch (Exception e) { return def; }
    }
}
