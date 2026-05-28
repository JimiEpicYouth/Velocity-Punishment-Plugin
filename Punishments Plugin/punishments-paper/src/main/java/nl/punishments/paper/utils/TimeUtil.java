package nl.punishments.paper.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
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

    public static String formatRelative(long timestamp) {
        long diff = timestamp - System.currentTimeMillis();
        if (diff <= 0) return "Verlopen";
        return formatDuration(diff);
    }
}
