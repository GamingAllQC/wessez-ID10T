package net.wessez.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.wessez.essentials.config.EssentialsConfig;
import net.wessez.essentials.data.EssentialsData;
import net.wessez.essentials.data.EssentialsData.SavedLocation;

import java.util.Random;

public class RtpCommand {

    private static final Random RANDOM = new Random();
    private static final int MAX_TRIES = 20;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rtp").executes(RtpCommand::rtp));
        dispatcher.register(Commands.literal("wild").executes(RtpCommand::rtp));
    }

    private static int rtp(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();

            if (!EssentialsConfig.isRtpEnabled()) {
                player.sendSystemMessage(Component.literal("§c/rtp is disabled on this server."));
                return 0;
            }

            EssentialsData data = EssentialsData.get();

            if (data.isOnCooldown(player.getUUID(), "rtp")) {
                long rem = data.getRemainingCooldownSeconds(player.getUUID(), "rtp");
                player.sendSystemMessage(Component.literal("§c/rtp on cooldown! §e" + rem + "s remaining."));
                return 0;
            }

            ServerLevel level = ctx.getSource().getServer().getLevel(Level.OVERWORLD);
            if (level == null) {
                player.sendSystemMessage(Component.literal("§cOverworld not available."));
                return 0;
            }

            player.sendSystemMessage(Component.literal("§eSearching for a safe location..."));

            SavedLocation dest = findSafeLocation(level);
            if (dest == null) {
                player.sendSystemMessage(Component.literal("§cCouldn't find a safe location. Try again!"));
                return 0;
            }

            data.setCooldown(player.getUUID(), "rtp");
            EssentialsData.scheduleTeleport(player, dest);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /rtp"));
            return 0;
        }
    }

    private static SavedLocation findSafeLocation(ServerLevel level) {
        int minDist = EssentialsConfig.getRtpMinDistance();
        int maxDist = EssentialsConfig.getRtpMaxDistance();

        for (int attempt = 0; attempt < MAX_TRIES; attempt++) {
            // Random angle + distance
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            int dist = minDist + RANDOM.nextInt(maxDist - minDist);
            int x = (int) (Math.cos(angle) * dist);
            int z = (int) (Math.sin(angle) * dist);

            // Find surface Y
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, surfaceY - 1, z);

            // Safety checks
            if (level.getBlockState(pos).isAir()) continue;
            if (isUnsafe(level, pos)) continue;

            return new SavedLocation("minecraft:overworld", x + 0.5, surfaceY, z + 0.5, 0, 0);
        }
        return null;
    }

    private static boolean isUnsafe(ServerLevel level, BlockPos pos) {
        var block = level.getBlockState(pos).getBlock();
        return block == Blocks.LAVA
                || block == Blocks.WATER
                || block == Blocks.FIRE
                || block == Blocks.MAGMA_BLOCK
                || block == Blocks.CACTUS;
    }
}
