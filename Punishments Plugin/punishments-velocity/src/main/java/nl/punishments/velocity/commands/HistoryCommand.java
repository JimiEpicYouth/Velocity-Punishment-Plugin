package nl.punishments.velocity.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.punishments.velocity.managers.DatabaseManager;
import nl.punishments.velocity.managers.PunishmentManager;
import nl.punishments.velocity.messaging.PluginMessenger;
import nl.punishments.velocity.models.Punishment;
import nl.punishments.velocity.utils.TimeUtil;

import java.util.*;

public class HistoryCommand {

    private final ProxyServer proxy;
    private final PunishmentManager pm;
    private final DatabaseManager db;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public HistoryCommand(ProxyServer proxy, PunishmentManager pm, DatabaseManager db) {
        this.proxy = proxy;
        this.pm = pm;
        this.db = db;
    }

    public SimpleCommand history() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                boolean isAdmin = inv.source().hasPermission("punishments.history.others");
                boolean isSelf = args.length == 0 || (inv.source() instanceof Player p && p.getUsername().equalsIgnoreCase(args[0]));

                // Eigen history via GUI op Paper server
                if (inv.source() instanceof Player caller) {
                    String targetName = (args.length == 0) ? caller.getUsername() : args[0];
                    if (!isSelf && !isAdmin) {
                        sendMsg(inv, "<red>❌ Je heb niet de juiste perms voor dit command!.");
                        return;
                    }
                    // Stuur opdracht naar Paper server om GUI te openen
                    caller.getCurrentServer().ifPresent(serverConn -> {
                        resolvePlayer(targetName, inv, (uuid, name) -> {
                            ByteArrayDataOutput out = ByteStreams.newDataOutput();
                            out.writeUTF("OPEN_HISTORY");
                            out.writeUTF(caller.getUniqueId().toString());
                            out.writeUTF(uuid.toString());
                            out.writeUTF(name);
                            serverConn.getServer().sendPluginMessage(PluginMessenger.CHANNEL, out.toByteArray());
                        });
                    });
                } else {
                    // Console: toon in tekst
                    if (args.length == 0) {
                        sendMsg(inv, "<red>Gebruik: /history <speler>");
                        return;
                    }
                    resolvePlayer(args[0], inv, (uuid, name) -> {
                        List<Punishment> history = pm.getHistory(uuid);
                        if (history.isEmpty()) {
                            sendMsg(inv, "<yellow>⚠ Geen punishments gevonden voor <white>" + name);
                            return;
                        }
                        sendMsg(inv, "<aqua>═══ History van " + name + " (" + history.size() + ") ═══");
                        for (Punishment p : history) {
                            String status = p.isActive() && !p.isExpired() ? "<green>[Actief]" : "<gray>[Inactief]";
                            String expires = p.isPermanent() ? "Permanent" : TimeUtil.formatDate(p.getExpiresAt());
                            sendMsg(inv, status + " <white>" + p.getType().name() + " <gray>- <white>" + p.getReason()
                                    + " <gray>| Door: <white>" + p.getStaffName()
                                    + " <gray>| Datum: <white>" + TimeUtil.formatDate(p.getIssuedAt())
                                    + (p.getType() == Punishment.Type.TEMPBAN || p.getType() == Punishment.Type.TEMPMUTE
                                        ? " <gray>| Tot: <white>" + expires : ""));
                        }
                    });
                }
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                if (!(inv.source() instanceof Player)) return true;
                return inv.source().hasPermission("punishments.history");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1 && inv.source().hasPermission("punishments.history.others"))
                    return proxy.getAllPlayers().stream().map(Player::getUsername).toList();
                return List.of();
            }
        };
    }

    private void resolvePlayer(String name, SimpleCommand.Invocation inv, BanCommands.PlayerCallback callback) {
        Optional<Player> online = proxy.getPlayer(name);
        if (online.isPresent()) {
            callback.accept(online.get().getUniqueId(), online.get().getUsername());
            return;
        }
        proxy.getScheduler().buildTask(proxy.getPluginManager().getPlugin("punishments-velocity").get().getInstance().get(), () -> {
            try {
                Optional<UUID> uuid = db.getUUIDByName(name);
                if (uuid.isPresent()) {
                    callback.accept(uuid.get(), name);
                } else {
                    sendMsg(inv, "<red>❌ Speler '<white>" + name + "<red>' is niet gevonden.");
                }
            } catch (Exception e) {
                sendMsg(inv, "<red>❌ Database fout.");
            }
        }).schedule();
    }

    private void sendMsg(SimpleCommand.Invocation inv, String msg) {
        inv.source().sendMessage(mm.deserialize("<dark_gray>[<aqua>Punishments</aqua>] </dark_gray>" + msg));
    }
}
