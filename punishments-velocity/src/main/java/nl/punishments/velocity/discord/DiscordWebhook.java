package nl.punishments.velocity.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nl.punishments.velocity.models.Punishment;
import nl.punishments.velocity.utils.TimeUtil;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DiscordWebhook {

    private String webhookUrl;
    private final Logger logger;

    public DiscordWebhook(Logger logger) {
        this.logger = logger;
    }

    public void setUrl(String url) {
        this.webhookUrl = url;
    }

    public void sendPunishment(Punishment punishment) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("JOUW_WEBHOOK_URL_HIER")) return;
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", getEmoji(punishment.getType()) + " " + getTypeNaam(punishment.getType()));
                embed.addProperty("color", getColor(punishment.getType()));
                embed.addProperty("timestamp", java.time.Instant.now().toString());

                JsonArray fields = new JsonArray();
                fields.add(buildField("🎮 Speler", punishment.getPlayerName(), true));
                fields.add(buildField("🛡️ Staf", punishment.getStaffName(), true));
                fields.add(buildField("📋 Reden", punishment.getReason(), false));

                if (punishment.getType() == Punishment.Type.TEMPBAN || punishment.getType() == Punishment.Type.TEMPMUTE) {
                    fields.add(buildField("⏳ Verloopt op", TimeUtil.formatDate(punishment.getExpiresAt()), true));
                    fields.add(buildField("🕐 Duur", TimeUtil.formatDuration(punishment.getExpiresAt() - punishment.getIssuedAt()), true));
                } else if (punishment.getType() == Punishment.Type.BAN) {
                    fields.add(buildField("⏳ Duur", "Permanent", true));
                }

                if (punishment.getServer() != null) {
                    fields.add(buildField("🖥️ Server", punishment.getServer(), true));
                }

                embed.add("fields", fields);

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "Punishments Systeem • ID #" + punishment.getId());
                embed.add("footer", footer);

                JsonArray embeds = new JsonArray();
                embeds.add(embed);

                JsonObject payload = new JsonObject();
                payload.addProperty("username", "Punishments Bot");
                payload.addProperty("avatar_url", "https://i.imgur.com/oBB9lfk.png");
                payload.add("embeds", embeds);

                sendRequest(payload.toString());
            } catch (Exception e) {
                logger.warning("[Punishments] Discord webhook fout: " + e.getMessage());
            }
        });
    }

    public void sendUnpunishment(String playerName, String staffName, Punishment.Type type) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("JOUW_WEBHOOK_URL_HIER")) return;
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", "✅ " + getUndoTypeNaam(type));
                embed.addProperty("color", 0x2ecc71);
                embed.addProperty("timestamp", java.time.Instant.now().toString());

                JsonArray fields = new JsonArray();
                fields.add(buildField("🎮 Speler", playerName, true));
                fields.add(buildField("🛡️ Staff", staffName, true));
                embed.add("fields", fields);

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "Punishments Systeem");
                embed.add("footer", footer);

                JsonArray embeds = new JsonArray();
                embeds.add(embed);

                JsonObject payload = new JsonObject();
                payload.addProperty("username", "Punishments Bot");
                payload.add("embeds", embeds);

                sendRequest(payload.toString());
            } catch (Exception e) {
                logger.warning("[Punishments] Discord webhook fout: " + e.getMessage());
            }
        });
    }

    private void sendRequest(String json) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        conn.getResponseCode();
        conn.disconnect();
    }

    private JsonObject buildField(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value != null ? value : "Onbekend");
        field.addProperty("inline", inline);
        return field;
    }

    private String getEmoji(Punishment.Type type) {
        return switch (type) {
            case BAN -> "🔨";
            case TEMPBAN -> "⏱️";
            case MUTE -> "🔇";
            case TEMPMUTE -> "⏰";
            case WARN -> "⚠️";
            case KICK -> "👢";
        };
    }

    private String getTypeNaam(Punishment.Type type) {
        return switch (type) {
            case BAN -> "Speler Gebanned";
            case TEMPBAN -> "Speler Tijdelijk Gebanned";
            case MUTE -> "Speler Gemute";
            case TEMPMUTE -> "Speler Tijdelijk Gemute";
            case WARN -> "Speler warning";
            case KICK -> "Speler Gekickt";
        };
    }

    private String getUndoTypeNaam(Punishment.Type type) {
        return switch (type) {
            case BAN, TEMPBAN -> "Speler Geunbanned";
            case MUTE, TEMPMUTE -> "Speler Geunmute";
            default -> "Punishment Verwijderd";
        };
    }

    private int getColor(Punishment.Type type) {
        return switch (type) {
            case BAN -> 0xe74c3c;
            case TEMPBAN -> 0xe67e22;
            case MUTE -> 0x9b59b6;
            case TEMPMUTE -> 0x8e44ad;
            case WARN -> 0xf1c40f;
            case KICK -> 0x3498db;
        };
    }
}
