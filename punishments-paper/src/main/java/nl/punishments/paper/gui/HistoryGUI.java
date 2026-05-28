package nl.punishments.paper.gui;

import nl.punishments.paper.models.Punishment;
import nl.punishments.paper.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class HistoryGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int PAGE_SIZE = 45;

    public static Inventory build(String targetName, UUID targetUuid, List<Punishment> punishments, int page) {
        int totalPages = Math.max(1, (int) Math.ceil(punishments.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54,
                MM.deserialize("<dark_gray>History: <aqua>" + targetName + " <dark_gray>(" + (page + 1) + "/" + totalPages + ")"));

        // Speler hoofd
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
        skullMeta.displayName(MM.deserialize("<aqua><b>" + targetName));
        List<Component> skullLore = new ArrayList<>();
        skullLore.add(MM.deserialize("<gray>Totaal punishments: <white>" + punishments.size()));
        long activeBans = punishments.stream().filter(p ->
                (p.getType() == Punishment.Type.BAN || p.getType() == Punishment.Type.TEMPBAN) && p.isActive() && !p.isExpired()).count();
        long activeMutes = punishments.stream().filter(p ->
                (p.getType() == Punishment.Type.MUTE || p.getType() == Punishment.Type.TEMPMUTE) && p.isActive() && !p.isExpired()).count();
        long warns = punishments.stream().filter(p -> p.getType() == Punishment.Type.WARN).count();
        skullLore.add(MM.deserialize("<gray>Actieve bans: <red>" + activeBans));
        skullLore.add(MM.deserialize("<gray>Actieve mutes: <light_purple>" + activeMutes));
        skullLore.add(MM.deserialize("<gray>Waarschuwingen: <yellow>" + warns));
        skullMeta.lore(skullLore);
        skull.setItemMeta(skullMeta);
        inv.setItem(4, skull);

        // Punishment items (slots 9-53)
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, punishments.size());
        int slot = 9;
        for (int i = start; i < end && slot < 54; i++) {
            inv.setItem(slot++, buildPunishmentItem(punishments.get(i)));
        }

        // Navigatie
        if (page > 0) {
            inv.setItem(0, buildNavItem(Material.ARROW, "<green>← Vorige pagina", "<gray>Pagina " + page));
        }
        if (page < totalPages - 1) {
            inv.setItem(8, buildNavItem(Material.ARROW, "<green>Volgende pagina →", "<gray>Pagina " + (page + 2)));
        }

        // Sluit knop
        inv.setItem(49, buildNavItem(Material.BARRIER, "<red>Sluiten", "<gray>Sluit dit menu"));

        // Decoratie
        fillGlass(inv);

        return inv;
    }

    private static ItemStack buildPunishmentItem(Punishment p) {
        Material mat = switch (p.getType()) {
            case BAN -> Material.BARRIER;
            case TEMPBAN -> Material.CLOCK;
            case MUTE -> Material.NOTE_BLOCK;
            case TEMPMUTE -> Material.REPEATER;
            case WARN -> Material.YELLOW_BANNER;
            case KICK -> Material.LEATHER_BOOTS;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String status = (p.isActive() && !p.isExpired()) ? "<green>✔ Actief" : "<gray>✘ Inactief";
        String typeLabel = switch (p.getType()) {
            case BAN -> "<red>Ban";
            case TEMPBAN -> "<gold>Tijdelijke Ban";
            case MUTE -> "<light_purple>Mute";
            case TEMPMUTE -> "<dark_purple>Tijdelijke Mute";
            case WARN -> "<yellow>Waarschuwing";
            case KICK -> "<blue>Kick";
        };

        meta.displayName(MM.deserialize(typeLabel + " <dark_gray>#" + p.getId() + " <dark_gray>| " + status));

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<dark_gray>───────────────────"));
        lore.add(MM.deserialize("<gray>Reden: <white>" + p.getReason()));
        lore.add(MM.deserialize("<gray>Staf: <aqua>" + p.getStaffName()));
        lore.add(MM.deserialize("<gray>Datum: <white>" + TimeUtil.formatDate(p.getIssuedAt())));
        if (!p.isPermanent() && (p.getType() == Punishment.Type.TEMPBAN || p.getType() == Punishment.Type.TEMPMUTE)) {
            lore.add(MM.deserialize("<gray>Verloopt: <yellow>" + TimeUtil.formatDate(p.getExpiresAt())));
            if (p.isActive() && !p.isExpired()) {
                lore.add(MM.deserialize("<gray>Resterende tijd: <gold>" + TimeUtil.formatRelative(p.getExpiresAt())));
            }
        } else if (p.getType() == Punishment.Type.BAN) {
            lore.add(MM.deserialize("<gray>Duur: <red>Permanent"));
        }
        if (p.getServer() != null) {
            lore.add(MM.deserialize("<gray>Server: <aqua>" + p.getServer()));
        }
        lore.add(MM.deserialize("<dark_gray>───────────────────"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildNavItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(name));
        meta.lore(List.of(MM.deserialize(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private static void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }
}
