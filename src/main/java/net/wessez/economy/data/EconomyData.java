package net.wessez.economy.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EconomyData {
    private static final Logger LOGGER = LoggerFactory.getLogger("WessezEconomy");

    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> totalOnlineTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dailySessionTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDailyReset = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDailyClaim = new ConcurrentHashMap<>();
    public final Map<UUID, Long> sessionStartTime = new ConcurrentHashMap<>();

    public void ensurePlayerExists(UUID uuid) {
        balances.putIfAbsent(uuid, 0.0);
        totalOnlineTime.putIfAbsent(uuid, 0L);
        dailySessionTime.putIfAbsent(uuid, 0L);
        lastDailyReset.putIfAbsent(uuid, System.currentTimeMillis());
        lastDailyClaim.putIfAbsent(uuid, 0L);
    }

    public void startSession(UUID uuid) {
        long currentTime = System.currentTimeMillis();
        sessionStartTime.put(uuid, currentTime);
        checkDailyReset(uuid);
        LOGGER.info("Session started for player {}", uuid);
    }

    public void endSession(UUID uuid) {
        Long startTime = sessionStartTime.remove(uuid);
        if (startTime != null) {
            long sessionDuration = System.currentTimeMillis() - startTime;
            totalOnlineTime.merge(uuid, sessionDuration, Long::sum);
            dailySessionTime.merge(uuid, sessionDuration, Long::sum);
            LOGGER.info("Session ended for {}. Duration: {}ms, Total: {}ms", uuid, sessionDuration, totalOnlineTime.get(uuid));
        }
    }

    private void checkDailyReset(UUID uuid) {
        long lastReset = lastDailyReset.getOrDefault(uuid, 0L);
        long currentTime = System.currentTimeMillis();

        // Get midnight of current day
        LocalDate today = LocalDate.now();
        ZonedDateTime midnight = today.atStartOfDay(ZoneId.systemDefault());
        long midnightMillis = midnight.toInstant().toEpochMilli();

        // If last reset was before today's midnight, reset the daily timer
        if (lastReset < midnightMillis) {
            dailySessionTime.put(uuid, 0L);
            lastDailyReset.put(uuid, currentTime);
            sessionStartTime.remove(uuid); // Clear session start
            LOGGER.info("Daily reset triggered for player {} at midnight", uuid);
        }
    }

    public double getBalance(UUID uuid) {
        ensurePlayerExists(uuid);
        return balances.get(uuid);
    }

    public void setBalance(UUID uuid, double amount) {
        ensurePlayerExists(uuid);
        balances.put(uuid, Math.max(0, amount));
    }

    public void addBalance(UUID uuid, double amount) {
        ensurePlayerExists(uuid);
        balances.merge(uuid, amount, Double::sum);
    }

    public void subtractBalance(UUID uuid, double amount) {
        ensurePlayerExists(uuid);
        double currentBalance = balances.get(uuid);
        balances.put(uuid, Math.max(0, currentBalance - amount));
    }

    public boolean hasBalance(UUID uuid, double amount) {
        ensurePlayerExists(uuid);
        return balances.get(uuid) >= amount;
    }

    public Map<UUID, Double> getAllBalances() {
        return new HashMap<>(balances);
    }

    public boolean canClaimDaily(UUID uuid) {
        ensurePlayerExists(uuid);
        checkDailyReset(uuid); // Check for midnight reset first

        long lastClaim = lastDailyClaim.get(uuid);

        // Get today's midnight
        LocalDate today = LocalDate.now();
        ZonedDateTime midnight = today.atStartOfDay(ZoneId.systemDefault());
        long midnightMillis = midnight.toInstant().toEpochMilli();

        // Check if last claim was before today's midnight (meaning they can claim again today)
        if (lastClaim >= midnightMillis) {
            return false; // Already claimed today
        }

        // Check if player has played at least 1 hour today
        long dailyTime = getDailySessionTime(uuid);
        return dailyTime >= 3600000; // 1 hour in milliseconds
    }


    public long getTotalOnlineTime(UUID uuid) {
        ensurePlayerExists(uuid);
        long storedTime = totalOnlineTime.getOrDefault(uuid, 0L);

        // Add current session time if player is online
        Long sessionStart = sessionStartTime.get(uuid);
        if (sessionStart != null) {
            long currentSessionTime = System.currentTimeMillis() - sessionStart;
            return storedTime + currentSessionTime;
        }

        return storedTime;
    }

    public long getDailySessionTime(UUID uuid) {
        ensurePlayerExists(uuid);
        checkDailyReset(uuid); // Always check for midnight reset
        long storedTime = dailySessionTime.getOrDefault(uuid, 0L);

        // Add current session time if player is online
        Long sessionStart = sessionStartTime.get(uuid);
        if (sessionStart != null) {
            long currentSessionTime = System.currentTimeMillis() - sessionStart;
            return storedTime + currentSessionTime;
        }

        return storedTime;
    }

    public void setTotalOnlineTime(UUID uuid, long time) {
        ensurePlayerExists(uuid);
        totalOnlineTime.put(uuid, Math.max(0, time));
    }

    public void setDailySessionTime(UUID uuid, long time) {
        ensurePlayerExists(uuid);
        dailySessionTime.put(uuid, Math.max(0, time));
    }

    public long getLastDailyClaim(UUID uuid) {
        ensurePlayerExists(uuid);
        return lastDailyClaim.get(uuid);
    }

    public void setLastDailyClaim(UUID uuid, long timestamp) {
        ensurePlayerExists(uuid);
        lastDailyClaim.put(uuid, timestamp);
    }

    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        return balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void save(MinecraftServer server) {
        try {
            Path dataDir = server.getServerDirectory().resolve("data");
            Files.createDirectories(dataDir);
            Path dataPath = dataDir.resolve("wessez-economy.json");

            Map<String, Object> dataToSave = new HashMap<>();

            Map<String, Double> balancesStr = new HashMap<>();
            balances.forEach((k, v) -> balancesStr.put(k.toString(), v));
            dataToSave.put("balances", balancesStr);

            Map<String, Long> totalTimeStr = new HashMap<>();
            totalOnlineTime.forEach((k, v) -> totalTimeStr.put(k.toString(), v));
            dataToSave.put("totalOnlineTime", totalTimeStr);

            Map<String, Long> dailyTimeStr = new HashMap<>();
            dailySessionTime.forEach((k, v) -> dailyTimeStr.put(k.toString(), v));
            dataToSave.put("dailySessionTime", dailyTimeStr);

            Map<String, Long> resetTimeStr = new HashMap<>();
            lastDailyReset.forEach((k, v) -> resetTimeStr.put(k.toString(), v));
            dataToSave.put("lastDailyReset", resetTimeStr);

            Map<String, Long> claimTimeStr = new HashMap<>();
            lastDailyClaim.forEach((k, v) -> claimTimeStr.put(k.toString(), v));
            dataToSave.put("lastDailyClaim", claimTimeStr);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                gson.toJson(dataToSave, writer);
            }

            LOGGER.info("Economy data saved successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to save economy data", e);
        }
    }

    public static EconomyData load(MinecraftServer server) {
        EconomyData data = new EconomyData();
        Path dataDir = server.getServerDirectory().resolve("data");
        Path dataPath = dataDir.resolve("wessez-economy.json");

        try {
            if (Files.exists(dataPath)) {
                Gson gson = new Gson();
                try (FileReader reader = new FileReader(dataPath.toFile())) {
                    Map<String, Object> loadedData = gson.fromJson(reader, Map.class);

                    if (loadedData.containsKey("balances")) {
                        Map<String, Double> loadedBalances = (Map<String, Double>) loadedData.get("balances");
                        loadedBalances.forEach((key, value) -> data.balances.put(UUID.fromString(key), value));
                    }

                    if (loadedData.containsKey("totalOnlineTime")) {
                        Map<String, Double> loadedTime = (Map<String, Double>) loadedData.get("totalOnlineTime");
                        loadedTime.forEach((key, value) -> data.totalOnlineTime.put(UUID.fromString(key), value.longValue()));
                    }

                    if (loadedData.containsKey("dailySessionTime")) {
                        Map<String, Double> loadedDaily = (Map<String, Double>) loadedData.get("dailySessionTime");
                        loadedDaily.forEach((key, value) -> data.dailySessionTime.put(UUID.fromString(key), value.longValue()));
                    }

                    if (loadedData.containsKey("lastDailyReset")) {
                        Map<String, Double> loadedReset = (Map<String, Double>) loadedData.get("lastDailyReset");
                        loadedReset.forEach((key, value) -> data.lastDailyReset.put(UUID.fromString(key), value.longValue()));
                    }

                    if (loadedData.containsKey("lastDailyClaim")) {
                        Map<String, Double> loadedClaims = (Map<String, Double>) loadedData.get("lastDailyClaim");
                        loadedClaims.forEach((key, value) -> data.lastDailyClaim.put(UUID.fromString(key), value.longValue()));
                    }
                }

                LOGGER.info("Economy data loaded successfully");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load economy data", e);
        }

        return data;
    }
}
