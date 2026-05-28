package nl.punishments.velocity.config;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigManager {

    private final Properties props = new Properties();
    private final Path dataDir;
    private final Logger logger;

    public ConfigManager(Path dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.logger = logger;
    }

    public void load() throws IOException {
        File dir = dataDir.toFile();
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "config.properties");
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
            defaults.setProperty("discord.webhook", "JOUW_WEBHOOK_URL_HIER");
            defaults.store(os, "Punishments Plugin Configuratie\n# Vul hier je MySQL en Discord webhook in.");
        }
        logger.info("[Punishments] Standaard configuratie aangemaakt in plugins/punishments-velocity/config.properties");
    }

    public String get(String key) {
        return props.getProperty(key, "");
    }

    public int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key)); } catch (Exception e) { return def; }
    }
}
