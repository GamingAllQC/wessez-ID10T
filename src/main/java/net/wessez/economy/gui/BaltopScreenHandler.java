package net.wessez.economy.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.wessez.Wessez;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

public class BaltopScreenHandler extends AbstractContainerMenu {

    private final List<Map.Entry<String, Double>> topBalances;
    private static Map<UUID, String> nameCache = new HashMap<>();

    public BaltopScreenHandler(int syncId, Inventory playerInventory) {
        super(Wessez.BALTOP_SCREEN_HANDLER, syncId);

        // Load name cache from usercache.json if not loaded
        if (nameCache.isEmpty()) {
            loadNameCache();
        }

        // Get the top balances and convert UUID keys to player names
        this.topBalances = Wessez.getEconomyData().getTopBalances(10).stream()
                .map(entry -> {
                    String playerName = getPlayerName(entry.getKey());
                    return new AbstractMap.SimpleEntry<>(playerName, entry.getValue());
                })
                .collect(Collectors.toList());
    }

    private static String getPlayerName(UUID uuid) {
        // Try online players first
        if (Wessez.getServer() != null) {
            ServerPlayer onlinePlayer = Wessez.getServer().getPlayerList().getPlayer(uuid);
            if (onlinePlayer != null) {
                return onlinePlayer.getName().getString();
            }
        }

        // Try cache
        if (nameCache.containsKey(uuid)) {
            return nameCache.get(uuid);
        }

        // Fallback to shortened UUID
        return "§7" + uuid.toString().substring(0, 8);
    }

    private static void loadNameCache() {
        try {
            File usercacheFile = new File("usercache.json");
            if (!usercacheFile.exists()) {
                Wessez.LOGGER.warn("usercache.json not found");
                return;
            }

            FileReader reader = new FileReader(usercacheFile);
            JsonElement jsonElement = JsonParser.parseReader(reader);

            if (jsonElement.isJsonArray()) {
                for (JsonElement element : jsonElement.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject profile = element.getAsJsonObject();

                        if (profile.has("uuid") && profile.has("name")) {
                            String uuidStr = profile.get("uuid").getAsString();
                            String name = profile.get("name").getAsString();

                            try {
                                UUID uuid = UUID.fromString(uuidStr);
                                nameCache.put(uuid, name);
                            } catch (IllegalArgumentException e) {
                                // Invalid UUID format, skip
                            }
                        }
                    }
                }
                Wessez.LOGGER.info("Loaded {} player names from usercache.json", nameCache.size());
            }

            reader.close();
        } catch (Exception e) {
            Wessez.LOGGER.error("Failed to load usercache.json", e);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public List<Map.Entry<String, Double>> getTopBalances() {
        return topBalances;
    }

    public static void openScreen(ServerPlayer player) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§6§lBalance Top");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new BaltopScreenHandler(syncId, inv);
            }
        });
    }
}
