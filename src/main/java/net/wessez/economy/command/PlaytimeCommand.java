package net.wessez.economy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.wessez.Wessez;
import net.wessez.economy.data.EconomyData;

public class PlaytimeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playtime")
                .executes(PlaytimeCommand::showPlaytime));
    }

    private static int showPlaytime(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can use this command"));
            return 0;
        }

        EconomyData data = Wessez.getEconomyData();
        long totalTime = data.getTotalOnlineTime(player.getUUID());
        long dailyTime = data.getDailySessionTime(player.getUUID());

        String totalFormatted = formatTime(totalTime);
        String dailyFormatted = formatTime(dailyTime);

        player.sendSystemMessage(Component.literal(
                "§6§l━━━━━━ §e⏰ Playtime Stats §6§l━━━━━━\n" +
                        "§eTotal Playtime: §f" + totalFormatted + "\n" +
                        "§eCurrent Daily Playtime: §f" + dailyFormatted + "\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━"
        ));

        return 1;
    }

    private static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
