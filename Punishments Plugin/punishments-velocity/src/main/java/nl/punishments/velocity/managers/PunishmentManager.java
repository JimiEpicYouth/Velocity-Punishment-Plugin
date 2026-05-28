package nl.punishments.velocity.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.punishments.velocity.PunishmentsVelocity;
import nl.punishments.velocity.discord.DiscordWebhook;
import nl.punishments.velocity.messaging.PluginMessenger;
import nl.punishments.velocity.models.Punishment;
import nl.punishments.velocity.utils.TimeUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PunishmentManager {

    private final PunishmentsVelocity plugin;
    private final DatabaseManager db;
    private final ProxyServer proxy;
    private final DiscordWebhook discord;
    private final PluginMessenger messenger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PunishmentManager(PunishmentsVelocity plugin, DatabaseManager db,
                              ProxyServer proxy, DiscordWebhook discord, PluginMessenger messenger) {
        this.plugin = plugin;
        this.db = db;
        this.proxy = proxy;
        this.discord = discord;
        this.messenger = messenger;
    }

    // ============== BAN ==============

    public boolean ban(UUID staffUuid, String staffName, UUID targetUuid, String targetName, String reason) {
        try {
            deactivateIfExists(targetUuid, Punishment.Type.BAN);
            deactivateIfExists(targetUuid, Punishment.Type.TEMPBAN);
            int id = db.addPunishment(Punishment.Type.BAN, targetUuid, targetName, staffUuid, staffName, reason, -1L, null);
            Punishment p = new Punishment(id, targetUuid, targetName, staffUuid, staffName,
                    Punishment.Type.BAN, reason, System.currentTimeMillis(), -1L, true, null);
            discord.sendPunishment(p);

            Optional<Player> onlinePlayer = proxy.getPlayer(targetUuid);
            onlinePlayer.ifPresent(player -> player.disconnect(buildBanScreen(reason, "Permanent", staffName)));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Ban fout: " + e.getMessage());
            return false;
        }
    }

    public boolean tempban(UUID staffUuid, String staffName, UUID targetUuid, String targetName, String reason, long durationMs) {
        try {
            deactivateIfExists(targetUuid, Punishment.Type.BAN);
            deactivateIfExists(targetUuid, Punishment.Type.TEMPBAN);
            long expiresAt = System.currentTimeMillis() + durationMs;
            int id = db.addPunishment(Punishment.Type.TEMPBAN, targetUuid, targetName, staffUuid, staffName, reason, expiresAt, null);
            Punishment p = new Punishment(id, targetUuid, targetName, staffUuid, staffName,
                    Punishment.Type.TEMPBAN, reason, System.currentTimeMillis(), expiresAt, true, null);
            discord.sendPunishment(p);

            Optional<Player> onlinePlayer = proxy.getPlayer(targetUuid);
            onlinePlayer.ifPresent(player -> player.disconnect(
                    buildBanScreen(reason, TimeUtil.formatDate(expiresAt), staffName)));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Tempban fout: " + e.getMessage());
            return false;
        }
    }

    public boolean unban(UUID staffUuid, String staffName, UUID targetUuid, String targetName) {
        try {
            deactivateIfExists(targetUuid, Punishment.Type.BAN);
            deactivateIfExists(targetUuid, Punishment.Type.TEMPBAN);
            discord.sendUnpunishment(targetName, staffName, Punishment.Type.BAN);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Unban fout: " + e.getMessage());
            return false;
        }
    }

    public Optional<Punishment> getActiveBan(UUID uuid) {
        try {
            Optional<Punishment> ban = db.getActivePunishment(uuid, Punishment.Type.BAN);
            if (ban.isPresent()) return ban;
            return db.getActivePunishment(uuid, Punishment.Type.TEMPBAN);
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Ban check fout: " + e.getMessage());
            return Optional.empty();
        }
    }

    // ============== MUTE ==============

    public boolean mute(UUID staffUuid, String staffName, UUID targetUuid, String targetName, String reason) {
        try {
            deactivateIfExists(targetUuid, Punishment.Type.MUTE);
            deactivateIfExists(targetUuid, Punishment.Type.TEMPMUTE);
            int id = db.addPunishment(Punishment.Type.MUTE, targetUuid, targetName, staffUuid, staffName, reason, -1L, null);
            Punishment p = new Punishment(id, targetUuid, targetName, staffUuid, staffName,
                    Punishment.Type.MUTE, reason, System.currentTimeMillis(), -1L, true, null);
            discord.sendPunishment(p);
            messenger.broadcastMute(targetUuid, true, reason, -1L);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Mute fout: " + e.getMessage());
            return false;
        }
    }

    public boolean tempmute(UUID staffUuid, String staffName, UUID targetUuid, String targetName, String reason, long durationMs) {
        try {
            deactivateIfExists(targetUuid, Punishment.Type.MUTE);
            deactivateIfExists(targetUuid, Punishment.Type.TEMPMUTE);
            long expiresAt = System.currentTimeMillis() + durationMs;
            int id = db.addPunishment(Punishment.Type.TEMPMUTE, targetUuid, targetName, staffUuid, staffName, reason, expiresAt, null);
            Punishment p = new Punishment(id, targetUuid, targetName, staffUuid, staffName,
                    Punishment.Type.TEMPMUTE, reason, System.currentTimeMillis(), expiresAt, true, null);
            discord.sendPunishment(p);
            messenger.broadcastMute(targetUuid, true, reason, expiresAt);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Tempmute fout: " + e.getMessage());
            return false;
        }
    }

    public boolean unmute(UUID staffUuid, String staffName, UUID targetUuid, String targetName) {
        try {
            deactivateIfExists(targetUuid, Punishment.Type.MUTE);
            deactivateIfExists(targetUuid, Punishment.Type.TEMPMUTE);
            discord.sendUnpunishment(targetName, staffName, Punishment.Type.MUTE);
            messenger.broadcastMute(targetUuid, false, null, 0L);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Unmute fout: " + e.getMessage());
            return false;
        }
    }

    // ============== WARN ==============

    public boolean warn(UUID staffUuid, String staffName, UUID targetUuid, String targetName, String reason) {
        try {
            int id = db.addPunishment(Punishment.Type.WARN, targetUuid, targetName, staffUuid, staffName, reason, -1L, null);
            Punishment p = new Punishment(id, targetUuid, targetName, staffUuid, staffName,
                    Punishment.Type.WARN, reason, System.currentTimeMillis(), -1L, true, null);
            discord.sendPunishment(p);
            List<Punishment> warns = db.getPunishmentHistory(targetUuid).stream()
                    .filter(pw -> pw.getType() == Punishment.Type.WARN).toList();
            messenger.broadcastWarn(targetUuid, reason, staffName, warns.size());
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Warn fout: " + e.getMessage());
            return false;
        }
    }

    // ============== KICK ==============

    public boolean kick(UUID staffUuid, String staffName, UUID targetUuid, String targetName, String reason, String serverName) {
        try {
            db.addPunishment(Punishment.Type.KICK, targetUuid, targetName, staffUuid, staffName, reason, -1L, serverName);
            Punishment p = new Punishment(-1, targetUuid, targetName, staffUuid, staffName,
                    Punishment.Type.KICK, reason, System.currentTimeMillis(), -1L, true, serverName);
            discord.sendPunishment(p);
            messenger.sendKickToServer(serverName, targetUuid, reason);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] Kick fout: " + e.getMessage());
            return false;
        }
    }

    // ============== HISTORY ==============

    public List<Punishment> getHistory(UUID uuid) {
        try {
            return db.getPunishmentHistory(uuid);
        } catch (SQLException e) {
            plugin.getLogger().severe("[Punishments] History fout: " + e.getMessage());
            return List.of();
        }
    }

    // ============== HELPERS ==============

    private void deactivateIfExists(UUID uuid, Punishment.Type type) throws SQLException {
        db.deactivatePunishment(uuid, type);
    }

    private Component buildBanScreen(String reason, String until, String admin) {
        return mm.deserialize(
                "<red><bold>Je bent gebanned van deze server!</bold></dark_red>\n\n" +
                "<gray>Reden: </gray><white>" + reason + "</white>\n" +
                "<gray>Gebanned door: </gray><red>" + admin + "</red>\n" +
                "<gray>Geldig tot: </gray><yellow>" + until + "</yellow>\n\n" +
                "<dark_gray>Wil je meer informatie over je punishment of bezwaar indienen? Maak een ticket aan in de discord!."
        );
    }
}
