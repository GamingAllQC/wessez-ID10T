package net.wessez.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.wessez.essentials.config.EssentialsConfig;
import net.wessez.essentials.data.EssentialsData;
import net.wessez.essentials.data.EssentialsData.SavedLocation;

public class SpawnCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn")
                .executes(SpawnCommand::goToSpawn));

        dispatcher.register(Commands.literal("setspawn")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .executes(SpawnCommand::setSpawn));
    }

    private static int goToSpawn(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            EssentialsData data = EssentialsData.get();

            if (data.isOnCooldown(player.getUUID(), "spawn")) {
                long rem = data.getRemainingCooldownSeconds(player.getUUID(), "spawn");
                player.sendSystemMessage(Component.literal("§cSpawn on cooldown! §e" + rem + "s remaining."));
                return 0;
            }

            SavedLocation spawn = data.getSpawnLocation(ctx.getSource().getServer());
            if (spawn == null) {
                player.sendSystemMessage(Component.literal("§cNo spawn point set."));
                return 0;
            }

            data.setCooldown(player.getUUID(), "spawn");
            EssentialsData.scheduleTeleport(player, spawn);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /spawn"));
            return 0;
        }
    }

    private static int setSpawn(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Vec3 pos = player.position();
            SavedLocation loc = new SavedLocation(
                    net.wessez.essentials.data.EssentialsData.getLevelId(player.level()),
                    pos.x, pos.y, pos.z,
                    player.getYRot(), player.getXRot()
            );
            EssentialsData.get().setSpawnLocation(loc);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aSpawn set to §e" + String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can set spawn"));
            return 0;
        }
    }
}
