package nl.punishments.paper.managers;

import nl.punishments.paper.gui.HistoryGUI;
import nl.punishments.paper.models.Punishment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager implements Listener {

    private final Map<UUID, HistorySession> sessions = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public void setHistorySession(UUID playerUuid, UUID targetUuid, String targetName, List<Punishment> history) {
        sessions.put(playerUuid, new HistorySession(targetUuid, targetName, history, 0));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!sessions.containsKey(uuid)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        HistorySession session = sessions.get(uuid);
        int slot = event.getRawSlot();

        // Sluit knop (slot 49)
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        // Vorige pagina (slot 0)
        if (slot == 0 && session.page > 0) {
            session.page--;
            player.openInventory(HistoryGUI.build(session.targetName, session.targetUuid, session.history, session.page));
            return;
        }

        // Volgende pagina (slot 8)
        int totalPages = (int) Math.ceil(session.history.size() / 45.0);
        if (slot == 8 && session.page < totalPages - 1) {
            session.page++;
            player.openInventory(HistoryGUI.build(session.targetName, session.targetUuid, session.history, session.page));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            sessions.remove(player.getUniqueId());
        }
    }

    private static class HistorySession {
        UUID targetUuid;
        String targetName;
        List<Punishment> history;
        int page;

        HistorySession(UUID targetUuid, String targetName, List<Punishment> history, int page) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.history = history;
            this.page = page;
        }
    }
}
