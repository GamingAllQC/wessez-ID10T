package net.wessez.economy.api;

import net.wessez.Wessez;

import java.util.Map;
import java.util.UUID;

public class EconomyAPI {
    /**
     * Gets the balance of a player
     */
    public static double getBalance(UUID playerUuid) {
        return Wessez.getEconomyData().getBalance(playerUuid);
    }

    /**
     * Adds money to a player's balance
     */
    public static void addBalance(UUID playerUuid, double amount) {
        Wessez.getEconomyData().addBalance(playerUuid, amount);
    }

    /**
     * Removes money from a player's balance
     */
    public static void removeBalance(UUID playerUuid, double amount) {
        Wessez.getEconomyData().subtractBalance(playerUuid, amount);
    }

    /**
     * Sets a player's balance to a specific amount
     */
    public static void setBalance(UUID playerUuid, double amount) {
        Wessez.getEconomyData().setBalance(playerUuid, amount);
    }

    /**
     * Checks if a player has at least the specified amount
     */
    public static boolean hasBalance(UUID playerUuid, double amount) {
        return Wessez.getEconomyData().hasBalance(playerUuid, amount);
    }

    /**
     * Gets all player balances
     */
    public static Map<UUID, Double> getAllBalances() {
        return Wessez.getEconomyData().getAllBalances();
    }

    /**
     * Gets a player's total online time in milliseconds
     */
    public static long getTotalOnlineTime(UUID playerUuid) {
        return Wessez.getEconomyData().getTotalOnlineTime(playerUuid);
    }

    /**
     * Gets a player's daily session time in milliseconds
     */
    public static long getDailySessionTime(UUID playerUuid) {
        return Wessez.getEconomyData().getDailySessionTime(playerUuid);
    }

    /**
     * Checks if a player can claim their daily reward
     */
    public static boolean canClaimDaily(UUID playerUuid) {
        return Wessez.getEconomyData().canClaimDaily(playerUuid);
    }

    /**
     * Sets the last daily claim time for a player
     */
    public static void setLastDailyClaim(UUID playerUuid, long time) {
        Wessez.getEconomyData().setLastDailyClaim(playerUuid, time);
    }

    /**
     * Gets the last daily claim time for a player
     */
    public static long getLastDailyClaim(UUID playerUuid) {
        return Wessez.getEconomyData().getLastDailyClaim(playerUuid);
    }
}
