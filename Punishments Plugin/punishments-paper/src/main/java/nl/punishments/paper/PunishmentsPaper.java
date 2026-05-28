package nl.punishments.paper;

import nl.punishments.paper.config.ConfigManager;
import nl.punishments.paper.listeners.ChatListener;
import nl.punishments.paper.listeners.PluginMessageReceiver;
import nl.punishments.paper.managers.DatabaseManager;
import nl.punishments.paper.managers.GUIManager;
import nl.punishments.paper.managers.MuteManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PunishmentsPaper extends JavaPlugin {

    private ConfigManager config;
    private DatabaseManager db;
    private MuteManager muteManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        getLogger().info("╔═══════════════════════════════════╗");
        getLogger().info("║   PunishmentsPaper v1.0.0         ║");
        getLogger().info("╚═══════════════════════════════════╝");

        // Config
        config = new ConfigManager(getDataFolder(), getLogger());
        try {
            config.load();
        } catch (Exception e) {
            getLogger().severe("[Punishments] Configuratie laden mislukt: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Database
        db = new DatabaseManager(getLogger());
        try {
            db.connect(
                config.get("db.host"),
                config.getInt("db.port", 3306),
                config.get("db.name"),
                config.get("db.user"),
                config.get("db.password")
            );
        } catch (Exception e) {
            getLogger().severe("[Punishments] Database verbinding mislukt: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Managers
        muteManager = new MuteManager(db, this);
        guiManager = new GUIManager();

        // Plugin messaging kanalen registreren
        getServer().getMessenger().registerIncomingPluginChannel(this, "punishments:main",
                new PluginMessageReceiver(this, muteManager, db));
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Listeners registreren
        getServer().getPluginManager().registerEvents(new ChatListener(muteManager), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        getLogger().info("[Punishments] Paper plugin succesvol geladen!");
    }

    @Override
    public void onDisable() {
        if (db != null) db.close();
        getLogger().info("[Punishments] Paper plugin gestopt.");
    }

    public GUIManager getGuiManager() { return guiManager; }
    public DatabaseManager getDatabase() { return db; }
    public MuteManager getMuteManager() { return muteManager; }
}
