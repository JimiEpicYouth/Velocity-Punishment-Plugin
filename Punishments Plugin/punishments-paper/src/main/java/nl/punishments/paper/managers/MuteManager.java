package nl.punishments.paper.managers;

import nl.punishments.paper.PunishmentsPaper;
import nl.punishments.paper.models.Punishment;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MuteManager {

    private final Map<UUID, MuteEntry> mutedPlayers = new ConcurrentHashMap<>();
    private final DatabaseManager db;
    private final PunishmentsPaper plugin;

    public MuteManager(DatabaseManager db, PunishmentsPaper plugin) {
        this.db = db;
        this.plugin = plugin;
    }

    public void setMuted(UUID uuid, boolean muted, String reason, long expiresAt) {
        if (muted) {
            mutedPlayers.put(uuid, new MuteEntry(reason, expiresAt));
        } else {
            mutedPlayers.remove(uuid);
        }
    }

    public boolean isMuted(UUID uuid) {
        MuteEntry entry = mutedPlayers.get(uuid);
        if (entry == null) return false;
        if (entry.expiresAt != -1L && System.currentTimeMillis() > entry.expiresAt) {
            mutedPlayers.remove(uuid);
            return false;
        }
        return true;
    }

    public MuteEntry getMuteEntry(UUID uuid) {
        return mutedPlayers.get(uuid);
    }

    public void loadFromDatabase(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<Punishment> mute = db.getActivePunishment(uuid, Punishment.Type.MUTE);
                if (mute.isEmpty()) {
                    mute = db.getActivePunishment(uuid, Punishment.Type.TEMPMUTE);
                }
                mute.ifPresent(p -> setMuted(uuid, true, p.getReason(), p.getExpiresAt()));
            } catch (SQLException e) {
                plugin.getLogger().severe("[Punishments] Mute laden fout: " + e.getMessage());
            }
        });
    }

    public record MuteEntry(String reason, long expiresAt) {
        public boolean isPermanent() { return expiresAt == -1L; }
    }
}
