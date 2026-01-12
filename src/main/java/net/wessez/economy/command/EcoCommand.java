package net.wessez.economy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.wessez.Wessez;
import net.wessez.economy.data.EconomyData;
import net.minecraft.server.permissions.Permissions;

public class EcoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eco")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(EcoCommand::setBalance))))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(EcoCommand::addBalance))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(EcoCommand::takeBalance))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(EcoCommand::resetPlayer)))
                .then(Commands.literal("resetdaily")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(EcoCommand::resetDailyPlaytime)))
                .then(Commands.literal("resetclaim")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(EcoCommand::resetDailyClaim)))
                .then(Commands.literal("dailyset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("hours", IntegerArgumentType.integer(0))
                                        .executes(EcoCommand::setDailyTime)))));
    }

    private static int setBalance(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            EconomyData data = Wessez.getEconomyData();

            data.setBalance(target.getUUID(), amount);

            context.getSource().sendSuccess(() ->
                    Component.literal("§aSet " + target.getName().getString() + "'s balance to $" +
                            String.format("%.2f", amount)), true);

            target.sendSystemMessage(Component.literal("§aYour balance has been set to $" +
                    String.format("%.2f", amount)));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found or invalid amount"));
            return 0;
        }
    }

    private static int addBalance(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            EconomyData data = Wessez.getEconomyData();

            double currentBalance = data.getBalance(target.getUUID());
            double newBalance = currentBalance + amount;
            data.setBalance(target.getUUID(), newBalance);

            context.getSource().sendSuccess(() ->
                    Component.literal("§aAdded $" + String.format("%.2f", amount) + " to " +
                            target.getName().getString() + "'s balance (New: $" +
                            String.format("%.2f", newBalance) + ")"), true);

            target.sendSystemMessage(Component.literal("§a$" + String.format("%.2f", amount) +
                    " has been added to your balance"));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found or invalid amount"));
            return 0;
        }
    }

    private static int takeBalance(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            EconomyData data = Wessez.getEconomyData();

            double currentBalance = data.getBalance(target.getUUID());
            double newBalance = Math.max(0, currentBalance - amount);
            data.setBalance(target.getUUID(), newBalance);

            context.getSource().sendSuccess(() ->
                    Component.literal("§aTook $" + String.format("%.2f", amount) + " from " +
                            target.getName().getString() + "'s balance (New: $" +
                            String.format("%.2f", newBalance) + ")"), true);

            target.sendSystemMessage(Component.literal("§c$" + String.format("%.2f", amount) +
                    " has been taken from your balance"));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found or invalid amount"));
            return 0;
        }
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            EconomyData data = Wessez.getEconomyData();

            // Reset EVERYTHING (balance, total playtime, daily claim)
            data.setBalance(target.getUUID(), 0);
            data.setTotalOnlineTime(target.getUUID(), 0);
            data.setDailySessionTime(target.getUUID(), 0);
            data.setLastDailyClaim(target.getUUID(), 0);

            context.getSource().sendSuccess(() ->
                    Component.literal("§aReset all economy data for " + target.getName().getString()), true);

            target.sendSystemMessage(Component.literal("§cYour economy data has been completely reset"));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found"));
            return 0;
        }
    }

    private static int resetDailyPlaytime(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            EconomyData data = Wessez.getEconomyData();

            // Reset ONLY daily session time (not total, not claim)
            data.setDailySessionTime(target.getUUID(), 0);

            context.getSource().sendSuccess(() ->
                    Component.literal("§aReset daily playtime for " + target.getName().getString()), true);

            target.sendSystemMessage(Component.literal("§aYour daily playtime has been reset"));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found"));
            return 0;
        }
    }

    private static int resetDailyClaim(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            EconomyData data = Wessez.getEconomyData();

            // Get current time and calculate last midnight
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.ZonedDateTime midnight = today.atStartOfDay(java.time.ZoneId.systemDefault());
            long midnightMillis = midnight.toInstant().toEpochMilli();

            // Set last claim to yesterday (before midnight) so they can claim again
            data.setLastDailyClaim(target.getUUID(), midnightMillis - (25 * 60 * 60 * 1000L)); // 25 hours ago

            context.getSource().sendSuccess(() ->
                    Component.literal("§aReset daily claim cooldown for " + target.getName().getString()), true);

            target.sendSystemMessage(Component.literal("§aYour daily claim is now available"));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found"));
            return 0;
        }
    }

    private static int setDailyTime(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            int hours = IntegerArgumentType.getInteger(context, "hours");
            EconomyData data = Wessez.getEconomyData();

            // Set daily session time (not total playtime)
            long milliseconds = hours * 60L * 60L * 1000L;
            data.setDailySessionTime(target.getUUID(), milliseconds);

            context.getSource().sendSuccess(() ->
                    Component.literal("§aSet " + target.getName().getString() + "'s daily playtime to " +
                            hours + " hours"), true);

            target.sendSystemMessage(Component.literal("§aYour daily playtime has been set to " +
                    hours + " hours"));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found or invalid hours"));
            return 0;
        }
    }
}
