package net.wessez.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.wessez.essentials.config.EssentialsConfig;
import net.wessez.essentials.data.EssentialsData;
import net.wessez.essentials.data.EssentialsData.SavedLocation;

public class WarpCommand {

    private static final SuggestionProvider<CommandSourceStack> WARP_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(EssentialsData.get().getWarps().keySet(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /warp <name>
        dispatcher.register(Commands.literal("warp")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(WARP_SUGGESTIONS)
                        .executes(ctx -> goWarp(ctx, StringArgumentType.getString(ctx, "name")))));

        // /warps
        dispatcher.register(Commands.literal("warps")
                .executes(WarpCommand::listWarps));

        // /setwarp <name>  (OP only)
        dispatcher.register(Commands.literal("setwarp")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> setWarp(ctx, StringArgumentType.getString(ctx, "name")))));

        // /delwarp <name>  (OP only)
        dispatcher.register(Commands.literal("delwarp")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(WARP_SUGGESTIONS)
                        .executes(ctx -> delWarp(ctx, StringArgumentType.getString(ctx, "name")))));
    }

    private static int goWarp(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();

            if (!EssentialsConfig.isWarpEnabled()) {
                player.sendSystemMessage(Component.literal("§cWarps are disabled on this server."));
                return 0;
            }

            EssentialsData data = EssentialsData.get();

            if (data.isOnCooldown(player.getUUID(), "warp")) {
                long rem = data.getRemainingCooldownSeconds(player.getUUID(), "warp");
                player.sendSystemMessage(Component.literal("§cWarp on cooldown! §e" + rem + "s remaining."));
                return 0;
            }

            return data.getWarp(name).map(loc -> {
                data.setCooldown(player.getUUID(), "warp");
                EssentialsData.scheduleTeleport(player, loc);
                return 1;
            }).orElseGet(() -> {
                player.sendSystemMessage(Component.literal("§cWarp §e'" + name + "' §cnot found. Use /warps for a list."));
                return 0;
            });
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /warp"));
            return 0;
        }
    }

    private static int listWarps(CommandContext<CommandSourceStack> ctx) {
        var warps = EssentialsData.get().getWarps();
        if (warps.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§eNo warps set."), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder("§6§lWarps §7(" + warps.size() + ")§r\n");
        warps.forEach((name, loc) -> sb.append(String.format("§e• %s §8[%.0f, %.0f, %.0f]\n",
                name, loc.x(), loc.y(), loc.z())));
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }

    private static int setWarp(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Vec3 pos = player.position();
            SavedLocation loc = new SavedLocation(
                    net.wessez.essentials.data.EssentialsData.getLevelId(player.level()),
                    pos.x, pos.y, pos.z,
                    player.getYRot(), player.getXRot()
            );
            EssentialsData.get().setWarp(name, loc);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aWarp §e'" + name + "' §aset at §e" +
                    String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can set warps"));
            return 0;
        }
    }

    private static int delWarp(CommandContext<CommandSourceStack> ctx, String name) {
        boolean removed = EssentialsData.get().deleteWarp(name);
        if (removed) {
            ctx.getSource().sendSuccess(() -> Component.literal("§aWarp §e'" + name + "' §adeleted."), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("§cWarp §e'" + name + "' §cnot found."));
        }
        return removed ? 1 : 0;
    }
}
