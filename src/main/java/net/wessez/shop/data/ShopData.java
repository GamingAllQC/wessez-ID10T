package net.wessez.shop.data;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.wessez.Wessez;
import net.wessez.economy.data.EconomyData;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShopData {

    private static ShopData instance;

    private final List<ShopListing> listings = new CopyOnWriteArrayList<>();

    private static final int    MAX_PLAYER_LISTINGS = 10;
    private static final double TAX_RATE            = 0.05;

    public static ShopData get() {
        if (instance == null) instance = new ShopData();
        return instance;
    }

    // ── Listings ──────────────────────────────────────────────────────────────

    public List<ShopListing> getListings()                  { return Collections.unmodifiableList(listings); }
    public void addListing(ShopListing l)                   { listings.add(l); }
    public boolean removeListing(UUID id)                   { return listings.removeIf(l -> l.getId().equals(id)); }
    public Optional<ShopListing> findById(UUID id)          { return listings.stream().filter(l -> l.getId().equals(id)).findFirst(); }

    public int countPlayerListings(UUID uuid) {
        return (int) listings.stream()
                .filter(l -> !l.isAdminShop() && uuid.toString().equals(l.sellerUuid))
                .count();
    }

    // ── Purchase ──────────────────────────────────────────────────────────────

    /** Returns null on success, or an error message string. */
    public String tryPurchase(ServerPlayer buyer, ShopListing listing) {
        EconomyData eco = Wessez.getEconomyData();
        if (eco == null) return "§cEconomy system not ready.";

        if (!eco.hasBalance(buyer.getUUID(), listing.price)) {
            double bal = eco.getBalance(buyer.getUUID());
            return String.format("§cInsufficient funds! You have §6$%.2f§c but need §6$%.2f", bal, listing.price);
        }

        if (buyer.getInventory().getFreeSlot() == -1) return "§cYour inventory is full!";

        if (!listing.isAdminShop() && listing.stock <= 0) {
            listings.remove(listing);
            return "§cThat item is out of stock!";
        }

        ItemStack item = parseItem(listing.itemNbt, listing.quantity);
        if (item.isEmpty()) return "§cCouldn't load item data. Contact an admin.";

        eco.subtractBalance(buyer.getUUID(), listing.price);

        if (!listing.isAdminShop() && listing.sellerUuid != null) {
            UUID sellerUuid = listing.getSellerUuid();
            double payout   = listing.price * (1.0 - TAX_RATE);
            eco.addBalance(sellerUuid, payout);

            var seller = Wessez.getServer().getPlayerList().getPlayer(sellerUuid);
            if (seller != null) {
                seller.sendSystemMessage(Component.literal(
                        String.format("§a%s §abought your §e%s §afor §6$%.2f §7(tax: $%.2f)",
                                buyer.getName().getString(), listing.itemName,
                                listing.price, listing.price * TAX_RATE)));
            }
        }

        buyer.getInventory().add(item);

        if (!listing.isAdminShop()) {
            listing.stock--;
            if (listing.stock <= 0) listings.remove(listing);
        }

        return null; // success
    }

    // ── Item serialization ────────────────────────────────────────────────────
    // Store items as "namespace:name" (the registry key string).
    // We avoid ResourceLocation imports entirely by using getKey().toString().

    public static String itemToNbt(ItemStack stack, MinecraftServer server) {
        // getKey() returns ResourceLocation; toString() gives "minecraft:diamond"
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static ItemStack parseItem(String itemId, int count) {
        // Iterate registry — O(n) but fine for ~1500 items
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).toString().equals(itemId)) {
                return new ItemStack(item, count);
            }
        }
        return ItemStack.EMPTY;
    }

    // ── Save / Load ───────────────────────────────────────────────────────────

    public void save(MinecraftServer server) {
        Path dir  = server.getServerDirectory().resolve("data");
        Path path = dir.resolve("wessez-shop.json");
        try {
            Files.createDirectories(dir);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter w = new FileWriter(path.toFile())) {
                gson.toJson(listings, w);
            }
        } catch (Exception e) {
            Wessez.LOGGER.error("[Shop] Failed to save data", e);
        }
    }

    public static ShopData load(MinecraftServer server) {
        ShopData data = new ShopData();
        instance = data;
        Path path = server.getServerDirectory().resolve("data").resolve("wessez-shop.json");
        if (!Files.exists(path)) return data;
        try (FileReader r = new FileReader(path.toFile())) {
            ShopListing[] arr = new Gson().fromJson(r, ShopListing[].class);
            if (arr != null) data.listings.addAll(Arrays.asList(arr));
            Wessez.LOGGER.info("[Shop] Loaded {} listings", data.listings.size());
        } catch (Exception e) {
            Wessez.LOGGER.error("[Shop] Failed to load data", e);
        }
        return data;
    }

    public int    getMaxPlayerListings() { return MAX_PLAYER_LISTINGS; }
    public double getTaxRate()           { return TAX_RATE; }
}
