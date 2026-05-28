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

public class MuteCommands {

    private final ProxyServer proxy;
    private final PunishmentManager pm;
    private final DatabaseManager db;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MuteCommands(ProxyServer proxy, PunishmentManager pm, DatabaseManager db) {
        this.proxy = proxy;
        this.pm = pm;
        this.db = db;
    }

    public SimpleCommand mute() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                if (args.length < 2) {
                    sendMsg(inv, "<red>Gebruik: /mute <speler> <reden>");
                    return;
                }
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                UUID staffUuid = getStaffUuid(inv);
                String staffName = getStaffName(inv);

                resolvePlayer(args[0], inv, (uuid, name) -> {
                    if (pm.mute(staffUuid, staffName, uuid, name, reason)) {
                        sendMsg(inv, "<green>✔ <white>" + name + " <green>is gemute. Reden: <yellow>" + reason);
                        broadcastStaff("<light_purple>🔇 <white>" + name + " <gray>is gemute door <light_purple>" + staffName + " <gray>| Reden: <white>" + reason);
                    } else {
                        sendMsg(inv, "<red>❌ Er is een fout opgetreden bij de mute.");
                    }
                });
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                return inv.source().hasPermission("punishments.mute");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1) return getPlayerNames();
                return List.of();
            }
        };
    }

    public SimpleCommand tempmute() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                if (args.length < 3) {
                    sendMsg(inv, "<red>Gebruik: /tempmute <speler> <tijd> <reden>");
                    return;
                }
                if (!TimeUtil.isValidDuration(args[1])) {
                    sendMsg(inv, "<red>❌ Ongeldige tijdsduur. Gebruik bijv: 1h, 30m, 1d");
                    return;
                }
                long duration = TimeUtil.parseDuration(args[1]);
                String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                UUID staffUuid = getStaffUuid(inv);
                String staffName = getStaffName(inv);

                resolvePlayer(args[0], inv, (uuid, name) -> {
                    if (pm.tempmute(staffUuid, staffName, uuid, name, reason, duration)) {
                        sendMsg(inv, "<green>✔ <white>" + name + " <green>is tijdelijk gemute voor <yellow>" + TimeUtil.formatDuration(duration) + "<green>. Reden: <yellow>" + reason);
                        broadcastStaff("<light_purple>⏰ <white>" + name + " <gray>is tempmuted door <light_purple>" + staffName + " <gray>voor <yellow>" + TimeUtil.formatDuration(duration));
                    } else {
                        sendMsg(inv, "<red>❌ Er is een fout opgetreden bij de tempmute.");
                    }
                });
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                return inv.source().hasPermission("punishments.tempmute");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1) return getPlayerNames();
                if (inv.arguments().length == 2) return List.of("15m", "30m", "1h", "6h", "12h", "1d", "7d");
                return List.of();
            }
        };
    }

    public SimpleCommand unmute() {
        return new SimpleCommand() {
            @Override
            public void execute(Invocation inv) {
                String[] args = inv.arguments();
                if (args.length < 1) {
                    sendMsg(inv, "<red>Gebruik: /unmute <speler>");
                    return;
                }
                UUID staffUuid = getStaffUuid(inv);
                String staffName = getStaffName(inv);

                resolvePlayer(args[0], inv, (uuid, name) -> {
                    if (pm.unmute(staffUuid, staffName, uuid, name)) {
                        sendMsg(inv, "<green>✔ <white>" + name + " <green>is geunmute.");
                        broadcastStaff("<green>🔊 <white>" + name + " <gray>is geunmute door <green>" + staffName);
                    } else {
                        sendMsg(inv, "<red>❌ Er is een fout opgetreden bij de unmute.");
                    }
                });
            }

            @Override
            public boolean hasPermission(Invocation inv) {
                return inv.source().hasPermission("punishments.unmute");
            }

            @Override
            public List<String> suggest(Invocation inv) {
                if (inv.arguments().length <= 1) {
                    try { return db.getActivePunishedPlayers(Punishment.Type.MUTE, Punishment.Type.TEMPMUTE); }
                    catch (Exception e) { return getPlayerNames(); }
                }
                return List.of();
            }
        };
    }

    // ============ Helpers ============

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
                sendMsg(inv, "<red>❌ Database fout bij opzoeken van speler.");
            }
        }).schedule();
    }

    private void sendMsg(SimpleCommand.Invocation inv, String msg) {
        inv.source().sendMessage(mm.deserialize("<dark_gray>[<aqua>Punishments</aqua>] </dark_gray>" + msg));
    }

    private void broadcastStaff(String msg) {
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
