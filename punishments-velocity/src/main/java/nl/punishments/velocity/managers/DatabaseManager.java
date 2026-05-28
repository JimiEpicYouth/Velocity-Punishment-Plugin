package nl.punishments.velocity.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import nl.punishments.velocity.models.Punishment;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class DatabaseManager {

    private HikariDataSource dataSource;
    private final Logger logger;

    public DatabaseManager(Logger logger) {
        this.logger = logger;
    }

    public void connect(String host, int port, String database, String username, String password) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=UTF-8");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");

        dataSource = new HikariDataSource(config);
        createTables();
        logger.info("[Punishments] Database verbinding succesvol!");
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS punishments (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    staff_uuid VARCHAR(36) NOT NULL,
                    staff_name VARCHAR(16) NOT NULL,
                    type ENUM('BAN','TEMPBAN','MUTE','TEMPMUTE','WARN','KICK') NOT NULL,
                    reason TEXT NOT NULL,
                    issued_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL DEFAULT -1,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    server VARCHAR(64) DEFAULT NULL,
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_type_active (type, active)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);
        }
    }

    public int addPunishment(Punishment.Type type, UUID playerUuid, String playerName,
                              UUID staffUuid, String staffName,
                              String reason, long expiresAt, String server) throws SQLException {
        String sql = "INSERT INTO punishments (player_uuid, player_name, staff_uuid, staff_name, type, reason, issued_at, expires_at, active, server) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, staffUuid.toString());
            ps.setString(4, staffName);
            ps.setString(5, type.name());
            ps.setString(6, reason);
            ps.setLong(7, System.currentTimeMillis());
            ps.setLong(8, expiresAt);
            ps.setBoolean(9, true);
            ps.setString(10, server);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void deactivatePunishment(UUID playerUuid, Punishment.Type type) throws SQLException {
        String sql = "UPDATE punishments SET active = FALSE WHERE player_uuid = ? AND type = ? AND active = TRUE";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        }
    }

    public Optional<Punishment> getActivePunishment(UUID playerUuid, Punishment.Type type) throws SQLException {
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? AND type = ? AND active = TRUE ORDER BY issued_at DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = fromResultSet(rs);
                    if (p.isExpired()) {
                        deactivatePunishment(playerUuid, type);
                        return Optional.empty();
                    }
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }

    public List<Punishment> getPunishmentHistory(UUID playerUuid) throws SQLException {
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? ORDER BY issued_at DESC";
        List<Punishment> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(fromResultSet(rs));
            }
        }
        return list;
    }

    public Optional<UUID> getUUIDByName(String name) throws SQLException {
        String sql = "SELECT player_uuid FROM punishments WHERE LOWER(player_name) = LOWER(?) LIMIT 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(UUID.fromString(rs.getString("player_uuid")));
            }
        }
        return Optional.empty();
    }

    public List<String> getActivePunishedPlayers(Punishment.Type... types) throws SQLException {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            placeholders.append(i == 0 ? "?" : ",?");
        }
        String sql = "SELECT DISTINCT player_name FROM punishments WHERE type IN (" + placeholders + ") AND active = TRUE AND (expires_at = -1 OR expires_at > ?)";
        List<String> names = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < types.length; i++) {
                ps.setString(i + 1, types[i].name());
            }
            ps.setLong(types.length + 1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString("player_name"));
            }
        }
        return names;
    }

    private Punishment fromResultSet(ResultSet rs) throws SQLException {
        return new Punishment(
                rs.getInt("id"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                UUID.fromString(rs.getString("staff_uuid")),
                rs.getString("staff_name"),
                Punishment.Type.valueOf(rs.getString("type")),
                rs.getString("reason"),
                rs.getLong("issued_at"),
                rs.getLong("expires_at"),
                rs.getBoolean("active"),
                rs.getString("server")
        );
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
