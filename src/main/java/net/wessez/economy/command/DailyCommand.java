package net.wessez.economy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.wessez.Wessez;
import net.wessez.economy.config.Config;
import net.wessez.economy.data.EconomyData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;

public class DailyCommand {
    private static final long ONE_HOUR_MS = 60 * 60 * 1000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("daily")
                .executes(DailyCommand::claimDaily));
    }

    private static int claimDaily(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can use this command"));
            return 0;
        }

        EconomyData data = Wessez.getEconomyData();
        long dailySessionTime = data.getDailySessionTime(player.getUUID());
        long lastClaim = data.getLastDailyClaim(player.getUUID());
        long currentTime = System.currentTimeMillis();

        // Check if player has played at least 1 hour today
        if (dailySessionTime < ONE_HOUR_MS) {
            long remainingMs = ONE_HOUR_MS - dailySessionTime;
            long remainingMinutes = remainingMs / (60 * 1000);

            player.sendSystemMessage(Component.literal(
                    "§cYou need to play for at least 1 hour today to claim your daily reward!\n" +
                            "§eTime remaining: §6" + remainingMinutes + " minutes"
            ));
            return 0;
        }

        // Get today's midnight
        LocalDate today = LocalDate.now();
        ZonedDateTime midnight = today.atStartOfDay(ZoneId.systemDefault());
        long midnightMillis = midnight.toInstant().toEpochMilli();

        // Check if already claimed today (after midnight)
        if (lastClaim >= midnightMillis) {
            // Get next midnight
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            ZonedDateTime nextMidnight = tomorrow.atStartOfDay(ZoneId.systemDefault());
            long nextMidnightMillis = nextMidnight.toInstant().toEpochMilli();

            long timeUntilMidnight = nextMidnightMillis - currentTime;
            long hoursUntil = timeUntilMidnight / (60 * 60 * 1000);
            long minutesUntil = (timeUntilMidnight % (60 * 60 * 1000)) / (60 * 1000);

            player.sendSystemMessage(Component.literal(
                    "§cYou've already claimed your daily reward!\n" +
                            "§eNext claim available at: §600:00 (Midnight)\n" +
                            "§eTime remaining: §6" + hoursUntil + "h " + minutesUntil + "m"
            ));
            return 0;
        }

        // Give daily reward
        double dailyAmount = Config.getDailyAmount();
        double currentBalance = data.getBalance(player.getUUID());
        data.setBalance(player.getUUID(), currentBalance + dailyAmount);
        data.setLastDailyClaim(player.getUUID(), currentTime);

        player.sendSystemMessage(Component.literal(
                "§a✦ Daily Reward Claimed! §a✦\n" +
                        "§e+$" + String.format("%.2f", dailyAmount) + "\n" +
                        "§7New Balance: §f$" + String.format("%.2f", currentBalance + dailyAmount)
        ));

        return 1;
    }
}
