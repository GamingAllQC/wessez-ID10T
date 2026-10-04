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
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

public class BaltopScreenHandler extends AbstractContainerMenu {

    private final List<Map.Entry<String, Double>> topBalances;
    private static Map<UUID, String> nameCache = new HashMap<>();

    /** Server-side constructor — receives data from the factory. */
    public BaltopScreenHandler(int syncId, Inventory playerInventory, BaltopScreenData data) {
        super(Wessez.BALTOP_SCREEN_HANDLER, syncId);
        this.topBalances = new ArrayList<>(data.balances().entrySet());
    }

    /** Client-side constructor — called by the registered factory with an empty data shell.
     *  Real data arrives via the ExtendedScreenHandlerType codec. */
    public BaltopScreenHandler(int syncId, Inventory playerInventory) {
        super(Wessez.BALTOP_SCREEN_HANDLER, syncId);
        this.topBalances = new ArrayList<>();
    }

    // ── Name resolution helpers ──────────────────────────────────────────────

    private static String getPlayerName(UUID uuid) {
        if (Wessez.getServer() != null) {
            ServerPlayer online = Wessez.getServer().getPlayerList().getPlayer(uuid);
            if (online != null) return online.getName().getString();
        }
        if (nameCache.containsKey(uuid)) return nameCache.get(uuid);
        return "§7" + uuid.toString().substring(0, 8);
    }

    private static void loadNameCache() {
        try {
            File usercacheFile = new File("usercache.json");
            if (!usercacheFile.exists()) {
                Wessez.LOGGER.warn("usercache.json not found");
                return;
            }
            try (FileReader reader = new FileReader(usercacheFile)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (jsonElement.isJsonArray()) {
                    for (JsonElement element : jsonElement.getAsJsonArray()) {
                        if (element.isJsonObject()) {
                            JsonObject profile = element.getAsJsonObject();
                            if (profile.has("uuid") && profile.has("name")) {
                                try {
                                    UUID uuid = UUID.fromString(profile.get("uuid").getAsString());
                                    nameCache.put(uuid, profile.get("name").getAsString());
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    }
                }
            }
            Wessez.LOGGER.info("Loaded {} player names from usercache.json", nameCache.size());
        } catch (Exception e) {
            Wessez.LOGGER.error("Failed to load usercache.json", e);
        }
    }

    // ── Screen opening ───────────────────────────────────────────────────────

    public static void openScreen(ServerPlayer player) {
        if (nameCache.isEmpty()) loadNameCache();

        // Build data map (top 50, name-keyed)
        Map<String, Double> balances = new LinkedHashMap<>();
        Wessez.getEconomyData().getTopBalances(50).forEach(entry ->
                balances.put(getPlayerName(entry.getKey()), entry.getValue()));

        BaltopScreenData screenData = new BaltopScreenData(balances);

        player.openMenu(new ExtendedScreenHandlerFactory<BaltopScreenData>() {
            @Override
            public BaltopScreenData getScreenOpeningData(ServerPlayer p) {
                return screenData;
            }

            @Override
            public Component getDisplayName() {
                return Component.literal("§6§lBalance Top");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new BaltopScreenHandler(syncId, inv, screenData);
            }
        });
    }

    // ── Container contract ───────────────────────────────────────────────────

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
}
