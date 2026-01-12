package net.wessez.economy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.wessez.Wessez;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static String currencySymbol = "$";
    private static double startingBalance = 100.0;
    private static double dailyAmount = 50.0;
    private static long dailyCooldown = 86400000; // 24 hours in milliseconds
    private static int autoSaveInterval = 6000; // 6000 ticks = 5 minutes

    public static void load(MinecraftServer server) {
        Path configDir = server.getServerDirectory().resolve("config");
        Path configPath = configDir.resolve("wessez-economy.json");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (Files.exists(configPath)) {
                Gson gson = new Gson();
                try (FileReader reader = new FileReader(configPath.toFile())) {
                    Map<String, Object> config = gson.fromJson(reader, Map.class);

                    if (config.containsKey("currencySymbol")) {
                        currencySymbol = (String) config.get("currencySymbol");
                    }
                    if (config.containsKey("startingBalance")) {
                        startingBalance = ((Number) config.get("startingBalance")).doubleValue();
                    }
                    if (config.containsKey("dailyAmount")) {
                        dailyAmount = ((Number) config.get("dailyAmount")).doubleValue();
                    }
                    if (config.containsKey("dailyCooldown")) {
                        dailyCooldown = ((Number) config.get("dailyCooldown")).longValue();
                    }
                    if (config.containsKey("autoSaveInterval")) {
                        autoSaveInterval = ((Number) config.get("autoSaveInterval")).intValue();
                    }
                }
                Wessez.LOGGER.info("Config loaded successfully");
            } else {
                save(server);
            }
        } catch (Exception e) {
            Wessez.LOGGER.error("Failed to load config", e);
        }
    }

    public static void save(MinecraftServer server) {
        Path configDir = server.getServerDirectory().resolve("config");
        Path configPath = configDir.resolve("wessez-economy.json");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Map<String, Object> config = new HashMap<>();
            config.put("currencySymbol", currencySymbol);
            config.put("startingBalance", startingBalance);
            config.put("dailyAmount", dailyAmount);
            config.put("dailyCooldown", dailyCooldown);
            config.put("autoSaveInterval", autoSaveInterval);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                gson.toJson(config, writer);
            }

            Wessez.LOGGER.info("Config saved successfully");
        } catch (Exception e) {
            Wessez.LOGGER.error("Failed to save config", e);
        }
    }

    // Getters
    public static String getCurrencySymbol() {
        return currencySymbol;
    }

    public static double getStartingBalance() {
        return startingBalance;
    }

    public static double getDailyAmount() {
        return dailyAmount;
    }

    public static long getDailyCooldown() {
        return dailyCooldown;
    }

    public static int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    // Setters
    public static void setCurrencySymbol(String symbol) {
        currencySymbol = symbol;
    }

    public static void setStartingBalance(double balance) {
        startingBalance = balance;
    }

    public static void setDailyAmount(double amount) {
        dailyAmount = amount;
    }

    public static void setDailyCooldown(long cooldown) {
        dailyCooldown = cooldown;
    }

    public static void setAutoSaveInterval(int interval) {
        autoSaveInterval = interval;
    }
}
