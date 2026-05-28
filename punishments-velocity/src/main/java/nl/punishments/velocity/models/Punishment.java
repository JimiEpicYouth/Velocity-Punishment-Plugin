package nl.punishments.velocity.models;

import java.util.UUID;

public class Punishment {

    public enum Type {
        BAN, TEMPBAN, MUTE, TEMPMUTE, WARN, KICK
    }

    private final int id;
    private final UUID playerUuid;
    private final String playerName;
    private final UUID staffUuid;
    private final String staffName;
    private final Type type;
    private final String reason;
    private final long issuedAt;
    private final long expiresAt; // -1 = permanent
    private boolean active;
    private final String server; // null = all servers

    public Punishment(int id, UUID playerUuid, String playerName,
                      UUID staffUuid, String staffName,
                      Type type, String reason,
                      long issuedAt, long expiresAt,
                      boolean active, String server) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.type = type;
        this.reason = reason;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.server = server;
    }

    public boolean isPermanent() {
        return expiresAt == -1L;
    }

    public boolean isExpired() {
        if (isPermanent()) return false;
        return System.currentTimeMillis() > expiresAt;
    }

    public int getId() { return id; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public UUID getStaffUuid() { return staffUuid; }
    public String getStaffName() { return staffName; }
    public Type getType() { return type; }
    public String getReason() { return reason; }
    public long getIssuedAt() { return issuedAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getServer() { return server; }
}
