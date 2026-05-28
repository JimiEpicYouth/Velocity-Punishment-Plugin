package nl.punishments.paper.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.punishments.paper.PunishmentsPaper;
import nl.punishments.paper.gui.HistoryGUI;
import nl.punishments.paper.managers.DatabaseManager;
import nl.punishments.paper.managers.MuteManager;
import nl.punishments.paper.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;
import java.util.UUID;

public class PluginMessageReceiver implements PluginMessageListener {

    private final PunishmentsPaper plugin;
    private final MuteManager muteManager;
    private final DatabaseManager db;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PluginMessageReceiver(PunishmentsPaper plugin, MuteManager muteManager, DatabaseManager db) {
        this.plugin = plugin;
        this.muteManager = muteManager;
        this.db = db;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player receivingPlayer, byte[] message) {
        if (!channel.equals("punishments:main")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String action = in.readUTF();

        switch (action) {
            case "MUTE" -> handleMute(in);
            case "KICK" -> handleKick(in);
            case "WARN" -> handleWarn(in);
            case "OPEN_HISTORY" -> handleOpenHistory(in);
        }
    }

    private void handleMute(ByteArrayDataInput in) {
        UUID uuid = UUID.fromString(in.readUTF());
        boolean muted = in.readBoolean();
        String reason = in.readUTF();
        long expiresAt = in.readLong();

        Bukkit.getScheduler().runTask(plugin, () -> {
            muteManager.setMuted(uuid, muted, reason, expiresAt);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (muted) {
                    String durationStr = (expiresAt == -1L) ? "Permanent" :
                            nl.punishments.paper.utils.TimeUtil.formatDate(expiresAt);
                    player.sendMessage(mm.deserialize(
                            "\n<dark_purple><bold>Je bent gemute!</bold></dark_purple>\n" +
                            "<gray>Reden: <white>" + reason + "\n" +
                            "<gray>Geldig tot: <light_purple>" + durationStr + "\n"
                    ));
                } else {
                    player.sendMessage(mm.deserialize(
                            "<green>✔ Je mute is opgeheven. Je kunt weer chatten!"
                    ));
                }
            }
        });
    }

    private void handleKick(ByteArrayDataInput in) {
        UUID uuid = UUID.fromString(in.readUTF());
        String reason = in.readUTF();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Stuur speler naar lobby via BungeeCord/Velocity
                // Geef visuele melding voor kick
                player.sendMessage(mm.deserialize(
                        "\n<red><bold>Je bent gekickt van deze server!</bold></red>\n" +
                        "<gray>Reden: <white>" + reason + "\n" +
                        "<gray>Je wordt teruggestuurd naar de lobby...\n"
                ));
                // Stuur speler naar lobby na korte delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    sendToLobby(player);
                }, 40L);
            }
        });
    }

    private void handleWarn(ByteArrayDataInput in) {
        UUID uuid = UUID.fromString(in.readUTF());
        String reason = in.readUTF();
        String staffName = in.readUTF();
        int totalWarnings = in.readInt();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(mm.deserialize(
                        "\n<yellow><bold>⚠ Je hebt een waarschuwing ontvangen! ⚠</bold></yellow>\n" +
                        "<gray>Reden: <white>" + reason + "\n" +
                        "<gray>Gegeven door: <yellow>" + staffName + "\n" +
                        "<gray>Totaal waarschuwingen: <red>" + totalWarnings + "\n"
                ));
                // Titel tonen
                player.showTitle(net.kyori.adventure.title.Title.title(
                        mm.deserialize("<yellow><bold>⚠ WAARSCHUWING ⚠</bold>"),
                        mm.deserialize("<gray>" + reason)
                ));
            }
        });
    }

    private void handleOpenHistory(ByteArrayDataInput in) {
        UUID callerUuid = UUID.fromString(in.readUTF());
        UUID targetUuid = UUID.fromString(in.readUTF());
        String targetName = in.readUTF();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<Punishment> history = db.getPunishmentHistory(targetUuid);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player caller = Bukkit.getPlayer(callerUuid);
                    if (caller != null) {
                        caller.openInventory(HistoryGUI.build(targetName, targetUuid, history, 0));
                        // Sla pagina + data op voor navigatie
                        plugin.getGuiManager().setHistorySession(callerUuid, targetUuid, targetName, history);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("[Punishments] History GUI fout: " + e.getMessage());
            }
        });
    }

    private void sendToLobby(Player player) {
        // Stuur speler naar de lobby server via plugin messaging
        com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF("lobby");
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}
