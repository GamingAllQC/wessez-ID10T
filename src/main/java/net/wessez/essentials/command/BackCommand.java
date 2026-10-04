package net.wessez.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.wessez.essentials.config.EssentialsConfig;
import net.wessez.essentials.data.EssentialsData;

public class BackCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back")
                .executes(BackCommand::goBack));
    }

    private static int goBack(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();

            if (!EssentialsConfig.isBackEnabled()) {
                player.sendSystemMessage(Component.literal("§c/back is disabled on this server."));
                return 0;
            }

            EssentialsData data = EssentialsData.get();

            if (data.isOnCooldown(player.getUUID(), "back")) {
                long rem = data.getRemainingCooldownSeconds(player.getUUID(), "back");
                player.sendSystemMessage(Component.literal("§c/back on cooldown! §e" + rem + "s remaining."));
                return 0;
            }

            return data.getLastPosition(player.getUUID()).map(loc -> {
                data.setCooldown(player.getUUID(), "back");
                EssentialsData.scheduleTeleport(player, loc);
                player.sendSystemMessage(Component.literal("§aTeleporting to your last location..."));
                return 1;
            }).orElseGet(() -> {
                player.sendSystemMessage(Component.literal("§cNo previous location saved. Teleport somewhere first!"));
                return 0;
            });
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /back"));
            return 0;
        }
    }
}
