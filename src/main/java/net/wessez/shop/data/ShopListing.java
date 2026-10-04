package net.wessez.shop.data;

import java.util.UUID;

public class ShopListing {
    public String id;           // UUID string
    public String itemNbt;      // SNBT of the ItemStack
    public String itemName;     // Display name (for chat messages)
    public int    quantity;     // Items per purchase
    public double price;        // Cost in currency
    public String sellerUuid;   // null = admin shop (infinite stock)
    public String sellerName;
    public int    stock;        // -1 = infinite (admin shop)
    public long   createdAt;

    public ShopListing() {}

    public ShopListing(UUID id, String itemNbt, String itemName, int quantity,
                       double price, UUID sellerUuid, String sellerName, int stock) {
        this.id         = id.toString();
        this.itemNbt    = itemNbt;
        this.itemName   = itemName;
        this.quantity   = quantity;
        this.price      = price;
        this.sellerUuid = sellerUuid == null ? null : sellerUuid.toString();
        this.sellerName = sellerName;
        this.stock      = stock;
        this.createdAt  = System.currentTimeMillis();
    }

    public boolean isAdminShop() {
        return sellerUuid == null || stock < 0;
    }

    public UUID getId()        { return UUID.fromString(id); }
    public UUID getSellerUuid(){ return sellerUuid == null ? null : UUID.fromString(sellerUuid); }
}
