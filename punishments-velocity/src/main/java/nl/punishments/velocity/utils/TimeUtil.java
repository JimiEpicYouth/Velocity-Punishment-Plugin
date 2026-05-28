package nl.punishments.velocity.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    /**
     * Parse tijdstring zoals "1d2h30m" naar milliseconden.
     * Ondersteunde eenheden: s (seconden), m (minuten), h (uren), d (dagen), w (weken)
     */
    public static long parseDuration(String input) {
        long total = 0;
        Pattern pattern = Pattern.compile("(\\d+)([smhdw])");
        Matcher matcher = pattern.matcher(input.toLowerCase());
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            total += switch (matcher.group(2)) {
                case "s" -> value * 1000L;
                case "m" -> value * 60_000L;
                case "h" -> value * 3_600_000L;
                case "d" -> value * 86_400_000L;
                case "w" -> value * 604_800_000L;
                default -> 0L;
            };
        }
        return total;
    }

    public static boolean isValidDuration(String input) {
        return parseDuration(input) > 0;
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) return "Permanent";
        long seconds = millis / 1000;
        long weeks = seconds / 604800; seconds %= 604800;
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append("w ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("u ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static String formatRelative(long timestamp) {
        long diff = timestamp - System.currentTimeMillis();
        if (diff <= 0) return "Verlopen";
        return formatDuration(diff);
    }
}
