package nl.punishments.paper.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import nl.punishments.paper.models.Punishment;

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
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=UTF-8");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.addDataSourceProperty("cachePrepStmts", "true");

        dataSource = new HikariDataSource(config);
        logger.info("[Punishments] Database verbinding succesvol!");
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

    public void deactivatePunishment(UUID playerUuid, Punishment.Type type) throws SQLException {
        String sql = "UPDATE punishments SET active = FALSE WHERE player_uuid = ? AND type = ? AND active = TRUE";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        }
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
