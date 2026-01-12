package net.wessez.economy.util;

public class TimeUtil {
    private static final long HOUR_IN_MILLIS = 60 * 60 * 1000;
    private static final long DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;

    public static boolean hasBeenOnlineForHour(long totalOnlineTime) {
        return totalOnlineTime >= HOUR_IN_MILLIS;
    }

    public static boolean canClaimDaily(long lastClaimTime) {
        if (lastClaimTime == 0) return true;
        return System.currentTimeMillis() - lastClaimTime >= DAY_IN_MILLIS;
    }

    public static String getTimeUntilNextDaily(long lastClaimTime) {
        long nextClaimTime = lastClaimTime + DAY_IN_MILLIS;
        long timeRemaining = nextClaimTime - System.currentTimeMillis();
        return formatTime(timeRemaining);
    }

    public static String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
