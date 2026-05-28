package nl.punishments.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.punishments.velocity.managers.DatabaseManager;
import nl.punishments.velocity.managers.PunishmentManager;
import nl.punishments.velocity.models.Punishment;
import nl.punishments.velocity.utils.TimeUtil;

import java.util.*;

public class BanCommands {

    private final ProxyServer proxy;
    private final PunishmentManager pm;
    private final DatabaseManager db;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public BanCommands(ProxyServer proxy, PunishmentManager pm, DatabaseManager db) {
        this.proxy = proxy;
        this.pm = pm;
        this.db = db;
    }

    public SimpleCommand ban() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                if (args.length < 2) {
                    sendMsg(inv, "<red>Gebruik: /ban <speler> <reden>");
                    return;
                }
                String targetName = args[0];
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                UUID staffUuid = getStaffUuid(inv);
                String staffName = getStaffName(inv);

                resolvePlayer(targetName, inv, (uuid, name) -> {
                    if (pm.ban(staffUuid, staffName, uuid, name, reason)) {
                        sendMsg(inv, "<green>✔ <white>" + name + " <green>is gebanned. Reden: <yellow>" + reason);
                        broadcastStaff("<red>🔨 <white>" + name + " <gray>is gebanned door <red>" + staffName + " <gray>| Reden: <white>" + reason);
                    } else {
                        sendMsg(inv, "<red>❌ Er is een fout opgetreden bij de ban.");
                    }
                });
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                return inv.source().hasPermission("punishments.ban");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1) return getPlayerNames();
                return List.of();
            }
        };
    }

    public SimpleCommand tempban() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                if (args.length < 3) {
                    sendMsg(inv, "<red>Gebruik: /tempban <speler> <tijd> <reden> (bijv. 1d2h30m)");
                    return;
                }
                String targetName = args[0];
                String timeStr = args[1];
                if (!TimeUtil.isValidDuration(timeStr)) {
                    sendMsg(inv, "<red>❌ Ongeldige tijdsduur. Gebruik bijv: 30s, 30m, 2h, 1d, 1w");
                    return;
                }
                long duration = TimeUtil.parseDuration(timeStr);
                String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                UUID staffUuid = getStaffUuid(inv);
                String staffName = getStaffName(inv);

                resolvePlayer(targetName, inv, (uuid, name) -> {
                    if (pm.tempban(staffUuid, staffName, uuid, name, reason, duration)) {
                        sendMsg(inv, "<green>✔ <white>" + name + " <green>is tijdelijk gebanned voor <yellow>" + TimeUtil.formatDuration(duration) + "<green>. Reden: <yellow>" + reason);
                        broadcastStaff("<gold>⏱️ <white>" + name + " <gray>is tempbanned door <gold>" + staffName + " <gray>voor <yellow>" + TimeUtil.formatDuration(duration) + " <gray>| Reden: <white>" + reason);
                    } else {
                        sendMsg(inv, "<red>❌ Er is een fout opgetreden bij de tempban.");
                    }
                });
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                return inv.source().hasPermission("punishments.tempban");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1) return getPlayerNames();
                if (inv.arguments().length == 2) return List.of("30m", "1h", "6h", "1d", "3d", "7d", "14d", "30d");
                return List.of();
            }
        };
    }

    public SimpleCommand unban() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                if (args.length < 1) {
                    sendMsg(inv, "<red>Gebruik: /unban <speler>");
                    return;
                }
                UUID staffUuid = getStaffUuid(inv);
                String staffName = getStaffName(inv);

                resolvePlayer(args[0], inv, (uuid, name) -> {
                    if (pm.unban(staffUuid, staffName, uuid, name)) {
                        sendMsg(inv, "<green>✔ <white>" + name + " <green>is geunbanned.");
                        broadcastStaff("<green>✅ <white>" + name + " <gray>is geunbanned door <green>" + staffName);
                    } else {
                        sendMsg(inv, "<red>❌ Er is een fout opgetreden bij de unban.");
                    }
                });
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                return inv.source().hasPermission("punishments.unban");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1) {
                    try { return db.getActivePunishedPlayers(Punishment.Type.BAN, Punishment.Type.TEMPBAN); }
                    catch (Exception e) { return getPlayerNames(); }
                }
                return List.of();
            }
        };
    }

    // ============ Helpers ============

    void resolvePlayer(String name, SimpleCommand.Invocation inv, PlayerCallback callback) {
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
                sendMsg(inv, "<red>❌ Database fout bij opzoeken van speler.");
            }
        }).schedule();
    }

    void sendMsg(SimpleCommand.Invocation inv, String msg) {
        inv.source().sendMessage(mm.deserialize("<dark_gray>[<aqua>Punishments</aqua>] </dark_gray>" + msg));
    }

    void broadcastStaff(String msg) {
        proxy.getAllPlayers().stream()
                .filter(p -> p.hasPermission("punishments.staff"))
                .forEach(p -> p.sendMessage(mm.deserialize("<dark_gray>[<aqua>Staff</aqua>] </dark_gray>" + msg)));
    }

    UUID getStaffUuid(SimpleCommand.Invocation inv) {
        return inv.source() instanceof Player p ? p.getUniqueId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    String getStaffName(SimpleCommand.Invocation inv) {
        return inv.source() instanceof Player p ? p.getUsername() : "Console";
    }

    List<String> getPlayerNames() {
        return proxy.getAllPlayers().stream().map(Player::getUsername).toList();
    }

    @FunctionalInterface
    interface PlayerCallback {
        void accept(UUID uuid, String name);
    }
}
