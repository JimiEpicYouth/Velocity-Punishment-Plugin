package nl.punishments.paper.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.punishments.paper.managers.MuteManager;
import nl.punishments.paper.utils.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ChatListener implements Listener {

    private final MuteManager muteManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ChatListener(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        if (!muteManager.isMuted(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
        MuteManager.MuteEntry entry = muteManager.getMuteEntry(event.getPlayer().getUniqueId());
        if (entry == null) return;

        String durationStr = entry.isPermanent() ? "Permanent" : TimeUtil.formatDate(entry.expiresAt());
        event.getPlayer().sendMessage(mm.deserialize(
                "<dark_red>❌ <red>Je bent gemute en kunt niet chatten!\n" +
                "<gray>Reden: <white>" + entry.reason() + "\n" +
                "<gray>Geldig tot: <light_purple>" + durationStr
        ));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        muteManager.loadFromDatabase(event.getPlayer().getUniqueId());
    }
}
