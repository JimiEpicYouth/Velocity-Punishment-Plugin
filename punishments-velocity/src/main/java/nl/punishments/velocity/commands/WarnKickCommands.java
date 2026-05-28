package nl.punishments.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.punishments.velocity.managers.DatabaseManager;
import nl.punishments.velocity.managers.PunishmentManager;

import java.util.*;

public class WarnKickCommands {

    private final ProxyServer proxy;
    private final PunishmentManager pm;
    private final DatabaseManager db;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WarnKickCommands(ProxyServer proxy, PunishmentManager pm, DatabaseManager db) {
        this.proxy = proxy;
        this.pm = pm;
        this.db = db;
    }

    public SimpleCommand warn() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                if (args.length < 2) {
                    sendMsg(inv, "<red>Gebruik: /warn <speler> <reden>");
                    return;
                }
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                UUID staffUuid = getStaffUuid(inv);
                String staffName = getStaffName(inv);

                resolvePlayer(args[0], inv, (uuid, name) -> {
                    if (pm.warn(staffUuid, staffName, uuid, name, reason)) {
                        sendMsg(inv, "<green>✔ <white>" + name + " <green>heeft een waarschuwing ontvangen. Reden: <yellow>" + reason);
                        broadcastStaff(inv, "<yellow>⚠️ <white>" + name + " <gray>is gewaarschuwd door <yellow>" + staffName + " <gray>| Reden: <white>" + reason);
                    } else {
                        sendMsg(inv, "<red>❌ Er is een fout opgetreden bij het geven van de warn, probeer het later opnieuw.");
                    }
                });
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                return inv.source().hasPermission("punishments.warn");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1) return getPlayerNames();
                return List.of();
            }
        };
    }

    public SimpleCommand kick() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                if (args.length < 3) {
                    sendMsg(inv, "<red>Gebruik: /kick <speler> <server> <reden>");
                    return;
                }
                String targetName = args[0];
                String serverName = args[1];
                String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                UUID staffUuid = getStaffUuid(inv);
                String staffName = getStaffName(inv);

                if (proxy.getServer(serverName).isEmpty()) {
                    sendMsg(inv, "<red>❌ Server '<white>" + serverName + "<red>' bestaat niet.");
                    return;
                }

                resolveOnlinePlayer(targetName, inv, (uuid, name) -> {
                    if (pm.kick(staffUuid, staffName, uuid, name, reason, serverName)) {
                        sendMsg(inv, "<green>✔ <white>" + name + " <green>is gekickt van <yellow>" + serverName + "<green>. Reden: <yellow>" + reason);
                        broadcastStaff(inv, "<blue>👢 <white>" + name + " <gray>is gekickt van <aqua>" + serverName + " <gray>door <blue>" + staffName + " <gray>| Reden: <white>" + reason);
                    } else {
                        sendMsg(inv, "<red>❌ Er is een fout opgetreden bij het uitvoeren van de kick.");
                    }
                });
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                return inv.source().hasPermission("punishments.kick");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1) return getPlayerNames();
                if (inv.arguments().length == 2) return proxy.getAllServers().stream()
                        .map(s -> s.getServerInfo().getName()).toList();
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

    private void resolveOnlinePlayer(String name, SimpleCommand.Invocation inv, BanCommands.PlayerCallback callback) {
        Optional<Player> online = proxy.getPlayer(name);
        if (online.isPresent()) {
            callback.accept(online.get().getUniqueId(), online.get().getUsername());
        } else {
            sendMsg(inv, "<red>❌ Speler '<white>" + name + "<red>' is niet online.");
        }
    }

    private void sendMsg(SimpleCommand.Invocation inv, String msg) {
        inv.source().sendMessage(mm.deserialize("<dark_gray>[<aqua>Punishments</aqua>] </dark_gray>" + msg));
    }

    private void broadcastStaff(SimpleCommand.Invocation inv, String msg) {
        proxy.getAllPlayers().stream()
                .filter(p -> p.hasPermission("punishments.staff"))
                .forEach(p -> p.sendMessage(mm.deserialize("<dark_gray>[<aqua>Staff</aqua>] </dark_gray>" + msg)));
    }

    private UUID getStaffUuid(SimpleCommand.Invocation inv) {
        return inv.source() instanceof Player p ? p.getUniqueId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private String getStaffName(SimpleCommand.Invocation inv) {
        return inv.source() instanceof Player p ? p.getUsername() : "Console";
    }

    private List<String> getPlayerNames() {
        return proxy.getAllPlayers().stream().map(Player::getUsername).toList();
    }
}
