package net.wessez.shop.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.wessez.Wessez;
import net.wessez.shop.data.ShopData;
import net.wessez.shop.data.ShopListing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopScreenHandler extends AbstractContainerMenu {

    private static final int ROWS      = 5;  // rows of shop items
    private static final int COLS      = 9;
    private static final int ITEM_SLOTS = ROWS * COLS; // 45
    private static final int TOTAL_SLOTS = 54;         // 6 rows = 54

    private final SimpleContainer shopContainer = new SimpleContainer(TOTAL_SLOTS);
    private final Player player;

    private List<ShopListing> allListings;
    private int currentPage;
    private int totalPages;

    // Track which slot index maps to which listing (for click handling)
    private final ShopListing[] slotListings = new ShopListing[ITEM_SLOTS];

    public ShopScreenHandler(int syncId, Inventory playerInventory) {
        super(Wessez.SHOP_SCREEN_HANDLER, syncId);
        this.player = playerInventory.player;
        this.allListings = new ArrayList<>(ShopData.get().getListings());
        this.totalPages  = Math.max(1, (int) Math.ceil((double) allListings.size() / ITEM_SLOTS));
        this.currentPage = 0;

        // Add shop container slots (rows 0-5)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < COLS; col++) {
                int slotIndex = row * COLS + col;
                addSlot(new ShopSlot(shopContainer, slotIndex, 8 + col * 18, 18 + row * 18, this, slotIndex));
            }
        }

        populatePage(0);
    }

    // ── Page population ───────────────────────────────────────────────────────

    void populatePage(int page) {
        this.currentPage = page;
        this.totalPages  = Math.max(1, (int) Math.ceil((double) allListings.size() / ITEM_SLOTS));

        // Clear all shop slots and mapping
        for (int i = 0; i < TOTAL_SLOTS; i++) shopContainer.setItem(i, ItemStack.EMPTY);
        for (int i = 0; i < ITEM_SLOTS; i++) slotListings[i] = null;

        // Fill item rows (0-44)
        int start = page * ITEM_SLOTS;
        for (int i = 0; i < ITEM_SLOTS && (start + i) < allListings.size(); i++) {
            ShopListing listing = allListings.get(start + i);
            slotListings[i] = listing;
            shopContainer.setItem(i, buildDisplayItem(listing));
        }

        // Navigation row (slots 45-53)
        // Prev page button (slot 45)
        if (page > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    Component.literal("§ePrevious Page"));
            shopContainer.setItem(45, prev);
        } else {
            shopContainer.setItem(45, filler());
        }

        // Page info (slot 49)
        ItemStack info = new ItemStack(Items.PAPER);
        info.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("§fPage §e" + (page + 1) + "§f/§e" + totalPages));
        shopContainer.setItem(49, info);

        // Next page button (slot 53)
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    Component.literal("§eNext Page"));
            shopContainer.setItem(53, next);
        } else {
            shopContainer.setItem(53, filler());
        }

        // Filler for remaining nav slots
        for (int i : new int[]{46, 47, 48, 50, 51, 52}) {
            shopContainer.setItem(i, filler());
        }

        broadcastChanges();
    }

    private ItemStack buildDisplayItem(ShopListing listing) {
        ItemStack display = ShopData.parseItem(listing.itemNbt, listing.quantity).copy();
        if (display.isEmpty()) display = new ItemStack(Items.BARRIER);

        display.setCount(listing.quantity);

        // Build lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§ePrice: §6$" + String.format("%.2f", listing.price)));
        lore.add(Component.literal("§7Qty: §f" + listing.quantity));
        if (listing.isAdminShop()) {
            lore.add(Component.literal("§7Stock: §aInfinite"));
            lore.add(Component.literal("§7Seller: §bAdmin Shop"));
        } else {
            lore.add(Component.literal("§7Stock: §f" + listing.stock));
            lore.add(Component.literal("§7Seller: §f" + listing.sellerName));
        }
        lore.add(Component.literal("§7Click to §apurchase"));

        display.set(net.minecraft.core.component.DataComponents.LORE,
                new net.minecraft.world.item.component.ItemLore(lore));
        return display;
    }

    private static ItemStack filler() {
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal(" "));
        return glass;
    }

    // ── Slot click handling ───────────────────────────────────────────────────

    /** Called by ShopSlot when clicked. */
    void handleSlotClick(int slotIndex, Player clicker) {
        if (!(clicker instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;

        if (slotIndex == 45 && currentPage > 0) {
            populatePage(currentPage - 1);
            return;
        }
        if (slotIndex == 53 && currentPage < totalPages - 1) {
            populatePage(currentPage + 1);
            return;
        }

        ShopListing listing = slotIndex < ITEM_SLOTS ? slotListings[slotIndex] : null;
        if (listing == null) return;

        String error = ShopData.get().tryPurchase(serverPlayer, listing);
        if (error != null) {
            serverPlayer.sendSystemMessage(Component.literal(error));
        } else {
            serverPlayer.sendSystemMessage(Component.literal(
                    String.format("§aPurchased §e%s §afor §6$%.2f§a!", listing.itemName, listing.price)));
            // Refresh page (stock may have changed)
            allListings = new ArrayList<>(ShopData.get().getListings());
            populatePage(currentPage);
        }
    }

    // ── Contract ──────────────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // ── Inner: custom slot ────────────────────────────────────────────────────

    static class ShopSlot extends Slot {
        private final ShopScreenHandler handler;
        private final int slotIndex;

        ShopSlot(Container container, int index, int x, int y,
                 ShopScreenHandler handler, int slotIndex) {
            super(container, index, x, y);
            this.handler   = handler;
            this.slotIndex = slotIndex;
        }

        @Override
        public boolean mayPickup(Player player) {
            handler.handleSlotClick(slotIndex, player);
            return false; // Never let the item be picked up
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
