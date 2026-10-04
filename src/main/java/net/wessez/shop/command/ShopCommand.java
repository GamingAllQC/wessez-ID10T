package net.wessez.shop.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.wessez.Wessez;
import net.wessez.shop.data.ShopData;
import net.wessez.shop.data.ShopListing;
import net.wessez.shop.gui.ShopScreenHandler;

import java.util.List;
import java.util.UUID;

public class ShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
                // /shop  — open the GUI
                .executes(ShopCommand::openShop)

                // /shop create <price> <qty> <name>  — admin creates listing
                .then(Commands.literal("create")
                        .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                .then(Commands.argument("qty", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> createAdmin(ctx,
                                                        DoubleArgumentType.getDouble(ctx, "price"),
                                                        IntegerArgumentType.getInteger(ctx, "qty"),
                                                        StringArgumentType.getString(ctx, "name")))))))

                // /shop sell <price> <qty>  — player sells held item
                .then(Commands.literal("sell")
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                .then(Commands.argument("qty", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> playerSell(ctx,
                                                DoubleArgumentType.getDouble(ctx, "price"),
                                                IntegerArgumentType.getInteger(ctx, "qty"))))))

                // /shop delete <id>  — remove a listing (admin or own listing)
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> deleteListing(ctx,
                                        StringArgumentType.getString(ctx, "id")))))

                // /shop list  — chat list of listings
                .then(Commands.literal("list")
                        .executes(ShopCommand::listShop))

                // /shop history  — show recent purchases (stub)
                .then(Commands.literal("history")
                        .executes(ShopCommand::history))
        );
    }

    // ── Open GUI ──────────────────────────────────────────────────────────────

    private static int openShop(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("§6§lShop");
                }

                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                    return new ShopScreenHandler(syncId, inv);
                }
            });
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can open the shop"));
            return 0;
        }
    }

    // ── Admin: create listing ─────────────────────────────────────────────────

    private static int createAdmin(CommandContext<CommandSourceStack> ctx,
                                   double price, int qty, String name) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cHold an item to create a shop listing."));
                return 0;
            }

            String nbt = ShopData.itemToNbt(held, ctx.getSource().getServer());
            ShopListing listing = new ShopListing(
                    UUID.randomUUID(), nbt, name, qty, price,
                    null, "Admin", -1 // infinite stock
            );
            ShopData.get().addListing(listing);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aAdmin shop listing created: §e" + name +
                    " §7(x" + qty + ") §afor §6$" + String.format("%.2f", price)), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cError creating listing: " + e.getMessage()));
            return 0;
        }
    }

    // ── Player: sell held item ─────────────────────────────────────────────────

    private static int playerSell(CommandContext<CommandSourceStack> ctx, double price, int qty) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ShopData shop = ShopData.get();

            if (shop.countPlayerListings(player.getUUID()) >= shop.getMaxPlayerListings()) {
                player.sendSystemMessage(Component.literal(
                        "§cYou've reached the max of §e" + shop.getMaxPlayerListings() +
                        " §clistings. Delete one first."));
                return 0;
            }

            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cHold an item to sell."));
                return 0;
            }
            if (held.getCount() < qty) {
                player.sendSystemMessage(Component.literal("§cYou need at least §e" + qty + " §cof that item."));
                return 0;
            }

            String nbt  = ShopData.itemToNbt(held, ctx.getSource().getServer());
            String name = held.getHoverName().getString();

            // Take items from player
            held.shrink(qty);
            player.getInventory().setChanged();

            ShopListing listing = new ShopListing(
                    UUID.randomUUID(), nbt, name, qty, price,
                    player.getUUID(), player.getName().getString(), qty
            );
            shop.addListing(listing);

            player.sendSystemMessage(Component.literal(
                    "§aListed §ex" + qty + " " + name +
                    " §afor §6$" + String.format("%.2f", price) +
                    " §7(Tax " + (int)(shop.getTaxRate()*100) + "% on sale)"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cError listing item: " + e.getMessage()));
            return 0;
        }
    }

    // ── Delete listing ────────────────────────────────────────────────────────

    private static int deleteListing(CommandContext<CommandSourceStack> ctx, String idStr) {
        try {
            UUID id = UUID.fromString(idStr);
            ShopData shop = ShopData.get();

            var listingOpt = shop.findById(id);
            if (listingOpt.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("§cListing not found: " + idStr));
                return 0;
            }
            ShopListing listing = listingOpt.get();

            // Only admin (OP) or the seller can delete
            boolean isOp = ctx.getSource().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
            boolean isSeller = false;
            try {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                isSeller = p.getUUID().toString().equals(listing.sellerUuid);
            } catch (Exception ignored) {}

            if (!isOp && !isSeller) {
                ctx.getSource().sendFailure(Component.literal("§cYou can only delete your own listings."));
                return 0;
            }

            shop.removeListing(id);
            ctx.getSource().sendSuccess(() -> Component.literal("§aListing §e" + listing.itemName + " §aremoved."), true);
            return 1;
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("§cInvalid listing ID."));
            return 0;
        }
    }

    // ── List (chat) ───────────────────────────────────────────────────────────

    private static int listShop(CommandContext<CommandSourceStack> ctx) {
        List<ShopListing> listings = ShopData.get().getListings();
        if (listings.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§eShop is empty."), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder("§6§lShop Listings§r\n");
        int shown = Math.min(listings.size(), 15);
        for (int i = 0; i < shown; i++) {
            ShopListing l = listings.get(i);
            sb.append(String.format("§7[%s] §e%s §8x%d §a$%.2f §8(%s)\n",
                    l.id.substring(0, 8), l.itemName, l.quantity, l.price,
                    l.isAdminShop() ? "Admin" : l.sellerName));
        }
        if (listings.size() > shown) sb.append("§7...and ").append(listings.size() - shown).append(" more. Use /shop to browse.");
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }

    // ── History ───────────────────────────────────────────────────────────────

    private static int history(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§eTransaction history coming soon! Use /shop to browse current listings."), false);
        return 1;
    }
}
