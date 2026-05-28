package nl.punishments.velocity.messaging;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import nl.punishments.velocity.models.Punishment;

import java.util.UUID;

public class PluginMessenger {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from("punishments:main");

    private final ProxyServer proxy;

    public PluginMessenger(ProxyServer proxy) {
        this.proxy = proxy;
    }

    /**
     * Stuur mute-status naar alle Paper servers.
     */
    public void broadcastMute(UUID playerUuid, boolean muted, String reason, long expiresAt) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("MUTE");
        out.writeUTF(playerUuid.toString());
        out.writeBoolean(muted);
        out.writeUTF(reason != null ? reason : "");
        out.writeLong(expiresAt);
        broadcast(out.toByteArray());
    }

    /**
     * Stuur kick opdracht naar specifieke server.
     */
    public void sendKickToServer(String serverName, UUID playerUuid, String reason) {
        proxy.getServer(serverName).ifPresent(server -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("KICK");
            out.writeUTF(playerUuid.toString());
            out.writeUTF(reason);
            server.sendPluginMessage(CHANNEL, out.toByteArray());
        });
    }

    /**
     * Stuur warn melding naar speler op alle servers.
     */
    public void broadcastWarn(UUID playerUuid, String reason, String staffName, int totalWarnings) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("WARN");
        out.writeUTF(playerUuid.toString());
        out.writeUTF(reason);
        out.writeUTF(staffName);
        out.writeInt(totalWarnings);
        broadcast(out.toByteArray());
    }

    private void broadcast(byte[] data) {
        proxy.getAllServers().forEach(server -> {
            if (!server.getPlayersConnected().isEmpty()) {
                server.sendPluginMessage(CHANNEL, data);
            }
        });
    }
}
