package nl.punishments.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import nl.punishments.velocity.commands.*;
import nl.punishments.velocity.config.ConfigManager;
import nl.punishments.velocity.discord.DiscordWebhook;
import nl.punishments.velocity.listeners.LoginListener;
import nl.punishments.velocity.managers.DatabaseManager;
import nl.punishments.velocity.managers.PunishmentManager;
import nl.punishments.velocity.messaging.PluginMessenger;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(
    id = "punishments-velocity",
    name = "PunishmentsVelocity",
    version = "1.0.0",
    description = "Volledig punishment systeem voor Velocity proxy",
    authors = {"JouwNaam"}
)
public class PunishmentsVelocity {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager config;
    private DatabaseManager db;
    private PunishmentManager pm;
    private DiscordWebhook discord;
    private PluginMessenger messenger;

    @Inject
    public PunishmentsVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        logger.info("╔═══════════════════════════════════╗");
        logger.info("║   PunishmentsVelocity v1.0.0      ║");
        logger.info("╚═══════════════════════════════════╝");

        // Config laden
        config = new ConfigManager(dataDirectory, logger);
        try {
            config.load();
        } catch (Exception e) {
            logger.severe("[Punishments] Configuratie laden mislukt: " + e.getMessage());
            return;
        }

        // Database
        db = new DatabaseManager(logger);
        try {
            db.connect(
                config.get("db.host"),
                config.getInt("db.port", 3306),
                config.get("db.name"),
                config.get("db.user"),
                config.get("db.password")
            );
        } catch (Exception e) {
            logger.severe("[Punishments] Database verbinding mislukt: " + e.getMessage());
            return;
        }

        // Discord
        discord = new DiscordWebhook(logger);
        discord.setUrl(config.get("discord.webhook"));

        // Messaging kanaal registreren
        MinecraftChannelIdentifier channel = PluginMessenger.CHANNEL;
        proxy.getChannelRegistrar().register(channel);
        messenger = new PluginMessenger(proxy);

        // PunishmentManager
        pm = new PunishmentManager(this, db, proxy, discord, messenger);

        // Commands registreren
        BanCommands banCmds = new BanCommands(proxy, pm, db);
        MuteCommands muteCmds = new MuteCommands(proxy, pm, db);
        WarnKickCommands warnKickCmds = new WarnKickCommands(proxy, pm, db);
        HistoryCommand historyCmds = new HistoryCommand(proxy, pm, db);

        var cm = proxy.getCommandManager();
        cm.register(cm.metaBuilder("ban").plugin(this).build(), banCmds.ban());
        cm.register(cm.metaBuilder("tempban").plugin(this).build(), banCmds.tempban());
        cm.register(cm.metaBuilder("unban").plugin(this).build(), banCmds.unban());
        cm.register(cm.metaBuilder("mute").plugin(this).build(), muteCmds.mute());
        cm.register(cm.metaBuilder("tempmute").plugin(this).build(), muteCmds.tempmute());
        cm.register(cm.metaBuilder("unmute").plugin(this).build(), muteCmds.unmute());
        cm.register(cm.metaBuilder("warn").plugin(this).build(), warnKickCmds.warn());
        cm.register(cm.metaBuilder("kick").plugin(this).build(), warnKickCmds.kick());
        cm.register(cm.metaBuilder("history").plugin(this).build(), historyCmds.history());

        // Listeners
        proxy.getEventManager().register(this, new LoginListener(pm));

        logger.info("[Punishments] Plugin succesvol geladen! Alle commands zijn actief.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (db != null) db.close();
        logger.info("[Punishments] Plugin gestopt.");
    }

    public Logger getLogger() { return logger; }
    public ProxyServer getProxy() { return proxy; }
}
