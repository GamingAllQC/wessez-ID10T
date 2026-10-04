package net.wessez.essentials.data;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.wessez.Wessez;
import net.wessez.essentials.config.EssentialsConfig;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EssentialsData {

    // ── Inner types ───────────────────────────────────────────────────────────

    public record SavedLocation(String world, double x, double y, double z, float yaw, float pitch) {}

    public record TpaRequest(UUID requesterUuid, String requesterName, boolean isHere, long expiresAt) {}

    public record PendingTeleport(
            SavedLocation destination,
            long executeAtMs,
            double originX, double originY, double originZ
    ) {}

    // ── State ─────────────────────────────────────────────────────────────────

    private final Map<UUID, Map<String, SavedLocation>> homes = new ConcurrentHashMap<>();
    private final Map<String, SavedLocation> warps                = new ConcurrentHashMap<>();
    private final Map<UUID, SavedLocation> lastPositions          = new ConcurrentHashMap<>();
    private SavedLocation spawnLocation = null;
    private final Map<UUID, Map<String, Long>> cooldowns          = new ConcurrentHashMap<>();
    private final Map<UUID, TpaRequest> incomingTpa               = new ConcurrentHashMap<>();
    public  static final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();
    private final Set<UUID> flyPlayers  = ConcurrentHashMap.newKeySet();
    private final Set<UUID> godPlayers  = ConcurrentHashMap.newKeySet();

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static EssentialsData instance;

    public static EssentialsData get() {
        if (instance == null) instance = new EssentialsData();
        return instance;
    }

    // ── Dimension helpers ─────────────────────────────────────────────────────

    /** Returns a stable, save-friendly string for a Level (no ResourceLocation import needed). */
    public static String getLevelId(Level level) {
        if (level.dimension().equals(Level.OVERWORLD)) return "minecraft:overworld";
        if (level.dimension().equals(Level.NETHER))    return "minecraft:the_nether";
        if (level.dimension().equals(Level.END))       return "minecraft:the_end";
        // Custom dimension fallback — toString() gives a unique key
        return level.dimension().toString();
    }

    /** Finds the ServerLevel matching a saved ID string. */
    private static ServerLevel getLevel(MinecraftServer server, String worldId) {
        return switch (worldId) {
            case "minecraft:the_nether" -> server.getLevel(Level.NETHER);
            case "minecraft:the_end"    -> server.getLevel(Level.END);
            // custom: iterate all levels and match by toString
            default -> {
                if (worldId.equals("minecraft:overworld")) yield server.getLevel(Level.OVERWORLD);
                for (ServerLevel lvl : server.getAllLevels()) {
                    if (getLevelId(lvl).equals(worldId)) yield lvl;
                }
                yield server.getLevel(Level.OVERWORLD); // safe fallback
            }
        };
    }

    // ── Teleport scheduling ───────────────────────────────────────────────────

    public static void scheduleTeleport(ServerPlayer player, SavedLocation dest) {
        int warmup = EssentialsConfig.getTeleportWarmupSeconds();
        if (warmup <= 0) {
            executeTeleport(player, dest, Wessez.getServer());
            return;
        }
        Vec3 pos = player.position();
        pendingTeleports.put(player.getUUID(), new PendingTeleport(
                dest,
                System.currentTimeMillis() + warmup * 1000L,
                pos.x, pos.y, pos.z
        ));
        player.sendSystemMessage(Component.literal(
                "§eTeleporting in §6" + warmup + "§e seconds... don't move!"));
    }

    public static void processPendingTeleports(MinecraftServer server) {
        long now = System.currentTimeMillis();
        pendingTeleports.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) return true;

            PendingTeleport pending = entry.getValue();
            Vec3 cur = player.position();
            double moved = Math.abs(cur.x - pending.originX())
                    + Math.abs(cur.y - pending.originY())
                    + Math.abs(cur.z - pending.originZ());

            if (moved > 0.5) {
                player.sendSystemMessage(Component.literal("§cTeleport cancelled — you moved!"));
                return true;
            }
            if (now >= pending.executeAtMs()) {
                executeTeleport(player, pending.destination(), server);
                return true;
            }
            return false;
        });
    }

    public static void executeTeleport(ServerPlayer player, SavedLocation dest, MinecraftServer server) {
        // Save last position for /back
        get().lastPositions.put(player.getUUID(), new SavedLocation(
                getLevelId(player.level()),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        ));

        ServerLevel level = getLevel(server, dest.world());
        if (level == null) {
            player.sendSystemMessage(Component.literal("§cWorld not found: " + dest.world()));
            return;
        }
        // Signature: teleportTo(ServerLevel, double, double, double, Set<Relative>, float, float, boolean)
        player.teleportTo(level, dest.x(), dest.y(), dest.z(),
                java.util.Set.of(), dest.yaw(), dest.pitch(), false);
    }

    // ── Cooldowns ─────────────────────────────────────────────────────────────

    public boolean isOnCooldown(UUID uuid, String command) {
        long cd = getCooldownMs(command);
        if (cd <= 0) return false;
        long last = cooldowns.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(command, 0L);
        return System.currentTimeMillis() - last < cd;
    }

    public long getRemainingCooldownSeconds(UUID uuid, String command) {
        long cd = getCooldownMs(command);
        long last = cooldowns.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(command, 0L);
        return Math.max(0, (cd - (System.currentTimeMillis() - last)) / 1000);
    }

    public void setCooldown(UUID uuid, String command) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(command, System.currentTimeMillis());
    }

    private long getCooldownMs(String command) {
        return switch (command) {
            case "spawn" -> EssentialsConfig.getSpawnCooldownSeconds() * 1000L;
            case "home"  -> EssentialsConfig.getHomeCooldownSeconds()  * 1000L;
            case "warp"  -> EssentialsConfig.getWarpCooldownSeconds()  * 1000L;
            case "rtp"   -> EssentialsConfig.getRtpCooldownSeconds()   * 1000L;
            case "back"  -> EssentialsConfig.getBackCooldownSeconds()  * 1000L;
            default      -> 0L;
        };
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    public SavedLocation getSpawnLocation(MinecraftServer server) {
        if (spawnLocation != null) return spawnLocation;
        // spawnLocation is null — admin should use /setspawn to configure the spawn point.
        // We fall back to world coordinates 0, 64, 0 until /setspawn is used.
        return new SavedLocation("minecraft:overworld", 0, 64, 0, 0, 0);
    }

    public void setSpawnLocation(SavedLocation loc) { this.spawnLocation = loc; }

    // ── Homes ─────────────────────────────────────────────────────────────────

    public Map<String, SavedLocation> getHomes(UUID uuid) {
        return Collections.unmodifiableMap(homes.getOrDefault(uuid, Collections.emptyMap()));
    }

    public Optional<SavedLocation> getHome(UUID uuid, String name) {
        return Optional.ofNullable(homes.getOrDefault(uuid, Collections.emptyMap()).get(name.toLowerCase()));
    }

    public boolean setHome(UUID uuid, String name, SavedLocation loc) {
        Map<String, SavedLocation> playerHomes = homes.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        if (!playerHomes.containsKey(name.toLowerCase()) && playerHomes.size() >= EssentialsConfig.getHomeMaxDefault()) {
            return false;
        }
        playerHomes.put(name.toLowerCase(), loc);
        return true;
    }

    public boolean deleteHome(UUID uuid, String name) {
        Map<String, SavedLocation> playerHomes = homes.get(uuid);
        if (playerHomes == null) return false;
        return playerHomes.remove(name.toLowerCase()) != null;
    }

    // ── Warps ─────────────────────────────────────────────────────────────────

    public Map<String, SavedLocation> getWarps() { return Collections.unmodifiableMap(warps); }
    public Optional<SavedLocation> getWarp(String name) { return Optional.ofNullable(warps.get(name.toLowerCase())); }
    public void setWarp(String name, SavedLocation loc)  { warps.put(name.toLowerCase(), loc); }
    public boolean deleteWarp(String name)               { return warps.remove(name.toLowerCase()) != null; }

    // ── Back ─────────────────────────────────────────────────────────────────

    public Optional<SavedLocation> getLastPosition(UUID uuid) { return Optional.ofNullable(lastPositions.get(uuid)); }
    public void setLastPosition(UUID uuid, SavedLocation loc) { lastPositions.put(uuid, loc); }

    // ── Fly / God ─────────────────────────────────────────────────────────────

    public boolean toggleFly(UUID uuid)  { if (flyPlayers.remove(uuid)) return false; flyPlayers.add(uuid); return true; }
    public boolean hasFly(UUID uuid)     { return flyPlayers.contains(uuid); }
    public boolean toggleGod(UUID uuid)  { if (godPlayers.remove(uuid)) return false; godPlayers.add(uuid); return true; }
    public boolean hasGod(UUID uuid)     { return godPlayers.contains(uuid); }

    // ── TPA ───────────────────────────────────────────────────────────────────

    public void addTpaRequest(UUID targetUuid, TpaRequest request) { incomingTpa.put(targetUuid, request); }

    public Optional<TpaRequest> getIncomingTpa(UUID targetUuid) {
        TpaRequest req = incomingTpa.get(targetUuid);
        if (req == null) return Optional.empty();
        if (System.currentTimeMillis() > req.expiresAt()) { incomingTpa.remove(targetUuid); return Optional.empty(); }
        return Optional.of(req);
    }

    public void removeTpaRequest(UUID targetUuid) { incomingTpa.remove(targetUuid); }

    // ── Save / Load ───────────────────────────────────────────────────────────

    public void save(MinecraftServer server) {
        Path dir  = server.getServerDirectory().resolve("data");
        Path path = dir.resolve("wessez-essentials.json");
        try {
            Files.createDirectories(dir);
            JsonObject root = new JsonObject();

            JsonObject homesJson = new JsonObject();
            homes.forEach((uuid, hMap) -> {
                JsonObject ph = new JsonObject();
                hMap.forEach((name, loc) -> ph.add(name, locToJson(loc)));
                homesJson.add(uuid.toString(), ph);
            });
            root.add("homes", homesJson);

            JsonObject warpsJson = new JsonObject();
            warps.forEach((name, loc) -> warpsJson.add(name, locToJson(loc)));
            root.add("warps", warpsJson);

            JsonObject lastPosJson = new JsonObject();
            lastPositions.forEach((uuid, loc) -> lastPosJson.add(uuid.toString(), locToJson(loc)));
            root.add("lastPositions", lastPosJson);

            if (spawnLocation != null) root.add("spawn", locToJson(spawnLocation));

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter w = new FileWriter(path.toFile())) { gson.toJson(root, w); }
        } catch (Exception e) {
            Wessez.LOGGER.error("[Essentials] Failed to save data", e);
        }
    }

    public static EssentialsData load(MinecraftServer server) {
        EssentialsData data = new EssentialsData();
        instance = data;
        Path path = server.getServerDirectory().resolve("data").resolve("wessez-essentials.json");
        if (!Files.exists(path)) return data;
        try (FileReader r = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            if (root.has("homes")) {
                root.getAsJsonObject("homes").entrySet().forEach(e -> {
                    UUID uuid = UUID.fromString(e.getKey());
                    Map<String, SavedLocation> ph = new ConcurrentHashMap<>();
                    e.getValue().getAsJsonObject().entrySet().forEach(h ->
                            ph.put(h.getKey(), jsonToLoc(h.getValue().getAsJsonObject())));
                    data.homes.put(uuid, ph);
                });
            }
            if (root.has("warps")) {
                root.getAsJsonObject("warps").entrySet().forEach(e ->
                        data.warps.put(e.getKey(), jsonToLoc(e.getValue().getAsJsonObject())));
            }
            if (root.has("lastPositions")) {
                root.getAsJsonObject("lastPositions").entrySet().forEach(e ->
                        data.lastPositions.put(UUID.fromString(e.getKey()),
                                jsonToLoc(e.getValue().getAsJsonObject())));
            }
            if (root.has("spawn")) data.spawnLocation = jsonToLoc(root.getAsJsonObject("spawn"));
            Wessez.LOGGER.info("[Essentials] Data loaded");
        } catch (Exception e) {
            Wessez.LOGGER.error("[Essentials] Failed to load data", e);
        }
        return data;
    }

    private static JsonObject locToJson(SavedLocation loc) {
        JsonObject o = new JsonObject();
        o.addProperty("world", loc.world());
        o.addProperty("x",     loc.x());
        o.addProperty("y",     loc.y());
        o.addProperty("z",     loc.z());
        o.addProperty("yaw",   loc.yaw());
        o.addProperty("pitch", loc.pitch());
        return o;
    }

    private static SavedLocation jsonToLoc(JsonObject o) {
        return new SavedLocation(
                o.get("world").getAsString(),
                o.get("x").getAsDouble(),
                o.get("y").getAsDouble(),
                o.get("z").getAsDouble(),
                o.get("yaw").getAsFloat(),
                o.get("pitch").getAsFloat()
        );
    }
}
