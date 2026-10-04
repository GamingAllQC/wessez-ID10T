package net.wessez.essentials.config;

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

public class EssentialsConfig {

    // Teleport
    private static int spawnCooldownSeconds   = 30;
    private static int homeCooldownSeconds    = 10;
    private static int warpCooldownSeconds    = 10;
    private static int rtpCooldownSeconds     = 300;
    private static int teleportWarmupSeconds  = 3;
    private static int tpaTimeoutSeconds      = 60;
    private static int backCooldownSeconds    = 10;

    // Homes
    private static int homeMaxDefault  = 3;
    private static int homeMinY        = -64;

    // RTP
    private static int rtpMinDistance  = 1000;
    private static int rtpMaxDistance  = 10000;

    // Toggles
    private static boolean backEnabled  = true;
    private static boolean warpEnabled  = true;
    private static boolean rtpEnabled   = true;
    private static boolean flyEnabled   = true;

    // ── Load / Save ──────────────────────────────────────────────────────────

    public static void load(MinecraftServer server) {
        Path dir  = server.getServerDirectory().resolve("config");
        Path path = dir.resolve("wessez-essentials.json");
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            if (Files.exists(path)) {
                Gson gson = new Gson();
                try (FileReader r = new FileReader(path.toFile())) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cfg = gson.fromJson(r, Map.class);
                    spawnCooldownSeconds  = getInt(cfg, "spawn_cooldown_seconds",  spawnCooldownSeconds);
                    homeCooldownSeconds   = getInt(cfg, "home_cooldown_seconds",   homeCooldownSeconds);
                    warpCooldownSeconds   = getInt(cfg, "warp_cooldown_seconds",   warpCooldownSeconds);
                    rtpCooldownSeconds    = getInt(cfg, "rtp_cooldown_seconds",    rtpCooldownSeconds);
                    teleportWarmupSeconds = getInt(cfg, "teleport_warmup_seconds", teleportWarmupSeconds);
                    tpaTimeoutSeconds     = getInt(cfg, "tpa_timeout_seconds",     tpaTimeoutSeconds);
                    backCooldownSeconds   = getInt(cfg, "back_cooldown_seconds",   backCooldownSeconds);
                    homeMaxDefault        = getInt(cfg, "home_max_default",        homeMaxDefault);
                    homeMinY              = getInt(cfg, "home_min_y",              homeMinY);
                    rtpMinDistance        = getInt(cfg, "rtp_min_distance",        rtpMinDistance);
                    rtpMaxDistance        = getInt(cfg, "rtp_max_distance",        rtpMaxDistance);
                    backEnabled  = getBool(cfg, "back_enabled",  backEnabled);
                    warpEnabled  = getBool(cfg, "warp_enabled",  warpEnabled);
                    rtpEnabled   = getBool(cfg, "rtp_enabled",   rtpEnabled);
                    flyEnabled   = getBool(cfg, "fly_enabled",   flyEnabled);
                }
                Wessez.LOGGER.info("[Essentials] Config loaded");
            } else {
                save(server);
            }
        } catch (Exception e) {
            Wessez.LOGGER.error("[Essentials] Failed to load config", e);
        }
    }

    public static void save(MinecraftServer server) {
        Path dir  = server.getServerDirectory().resolve("config");
        Path path = dir.resolve("wessez-essentials.json");
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Map<String, Object> cfg = new HashMap<>();
            cfg.put("spawn_cooldown_seconds",  spawnCooldownSeconds);
            cfg.put("home_cooldown_seconds",   homeCooldownSeconds);
            cfg.put("warp_cooldown_seconds",   warpCooldownSeconds);
            cfg.put("rtp_cooldown_seconds",    rtpCooldownSeconds);
            cfg.put("teleport_warmup_seconds", teleportWarmupSeconds);
            cfg.put("tpa_timeout_seconds",     tpaTimeoutSeconds);
            cfg.put("back_cooldown_seconds",   backCooldownSeconds);
            cfg.put("home_max_default",        homeMaxDefault);
            cfg.put("home_min_y",              homeMinY);
            cfg.put("rtp_min_distance",        rtpMinDistance);
            cfg.put("rtp_max_distance",        rtpMaxDistance);
            cfg.put("back_enabled",  backEnabled);
            cfg.put("warp_enabled",  warpEnabled);
            cfg.put("rtp_enabled",   rtpEnabled);
            cfg.put("fly_enabled",   flyEnabled);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter w = new FileWriter(path.toFile())) {
                gson.toJson(cfg, w);
            }
        } catch (Exception e) {
            Wessez.LOGGER.error("[Essentials] Failed to save config", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int getInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    private static boolean getBool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : def;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static int  getSpawnCooldownSeconds()   { return spawnCooldownSeconds; }
    public static int  getHomeCooldownSeconds()    { return homeCooldownSeconds; }
    public static int  getWarpCooldownSeconds()    { return warpCooldownSeconds; }
    public static int  getRtpCooldownSeconds()     { return rtpCooldownSeconds; }
    public static int  getTeleportWarmupSeconds()  { return teleportWarmupSeconds; }
    public static int  getTpaTimeoutSeconds()      { return tpaTimeoutSeconds; }
    public static int  getBackCooldownSeconds()    { return backCooldownSeconds; }
    public static int  getHomeMaxDefault()         { return homeMaxDefault; }
    public static int  getHomeMinY()               { return homeMinY; }
    public static int  getRtpMinDistance()         { return rtpMinDistance; }
    public static int  getRtpMaxDistance()         { return rtpMaxDistance; }
    public static boolean isBackEnabled()          { return backEnabled; }
    public static boolean isWarpEnabled()          { return warpEnabled; }
    public static boolean isRtpEnabled()           { return rtpEnabled; }
    public static boolean isFlyEnabled()           { return flyEnabled; }
}
