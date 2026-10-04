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

import java.util.Map;

public class HomeCommand {

    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS = (ctx, builder) -> {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            return SharedSuggestionProvider.suggest(
                    EssentialsData.get().getHomes(player.getUUID()).keySet(), builder);
        } catch (Exception e) { return builder.buildFuture(); }
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /home [name]
        dispatcher.register(Commands.literal("home")
                .executes(ctx -> goHome(ctx, "home"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(ctx -> goHome(ctx, StringArgumentType.getString(ctx, "name")))));

        // /sethome [name]
        dispatcher.register(Commands.literal("sethome")
                .executes(ctx -> setHome(ctx, "home"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> setHome(ctx, StringArgumentType.getString(ctx, "name")))));

        // /delhome <name>
        dispatcher.register(Commands.literal("delhome")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(ctx -> delHome(ctx, StringArgumentType.getString(ctx, "name")))));

        // /homes
        dispatcher.register(Commands.literal("homes")
                .executes(HomeCommand::listHomes));
    }

    private static int goHome(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            EssentialsData data = EssentialsData.get();

            if (data.isOnCooldown(player.getUUID(), "home")) {
                long rem = data.getRemainingCooldownSeconds(player.getUUID(), "home");
                player.sendSystemMessage(Component.literal("§cHome on cooldown! §e" + rem + "s remaining."));
                return 0;
            }

            return data.getHome(player.getUUID(), name).map(loc -> {
                data.setCooldown(player.getUUID(), "home");
                EssentialsData.scheduleTeleport(player, loc);
                return 1;
            }).orElseGet(() -> {
                player.sendSystemMessage(Component.literal("§cHome §e'" + name + "' §cnot found. Use /sethome " + name));
                return 0;
            });
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /home"));
            return 0;
        }
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Vec3 pos = player.position();

            if (pos.y < EssentialsConfig.getHomeMinY()) {
                player.sendSystemMessage(Component.literal(
                        "§cCannot set home below Y=" + EssentialsConfig.getHomeMinY()));
                return 0;
            }

            SavedLocation loc = new SavedLocation(
                    net.wessez.essentials.data.EssentialsData.getLevelId(player.level()),
                    pos.x, pos.y, pos.z,
                    player.getYRot(), player.getXRot()
            );

            boolean set = EssentialsData.get().setHome(player.getUUID(), name, loc);
            if (set) {
                player.sendSystemMessage(Component.literal(
                        "§aHome §e'" + name + "' §aset at §e" +
                        String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)));
            } else {
                int max = EssentialsConfig.getHomeMaxDefault();
                player.sendSystemMessage(Component.literal(
                        "§cYou've reached the max of §e" + max + " §chomes. Delete one with /delhome."));
            }
            return set ? 1 : 0;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /sethome"));
            return 0;
        }
    }

    private static int delHome(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            boolean removed = EssentialsData.get().deleteHome(player.getUUID(), name);
            if (removed) {
                player.sendSystemMessage(Component.literal("§aHome §e'" + name + "' §adeleted."));
            } else {
                player.sendSystemMessage(Component.literal("§cNo home named §e'" + name + "'."));
            }
            return removed ? 1 : 0;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /delhome"));
            return 0;
        }
    }

    private static int listHomes(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Map<String, SavedLocation> homes = EssentialsData.get().getHomes(player.getUUID());
            if (homes.isEmpty()) {
                player.sendSystemMessage(Component.literal("§eNo homes set. Use /sethome <name>"));
                return 0;
            }
            StringBuilder sb = new StringBuilder("§6§lYour Homes §7(" + homes.size() + "/" +
                    EssentialsConfig.getHomeMaxDefault() + ")§r\n");
            homes.forEach((name, loc) -> sb.append(String.format("§e• %s §8[%.0f, %.0f, %.0f]\n",
                    name, loc.x(), loc.y(), loc.z())));
            player.sendSystemMessage(Component.literal(sb.toString().trim()));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /homes"));
            return 0;
        }
    }
}
