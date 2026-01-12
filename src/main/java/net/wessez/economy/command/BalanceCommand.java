package net.wessez.economy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.wessez.Wessez;

public class BalanceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")
                .executes(BalanceCommand::executeSelf)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(BalanceCommand::executeOther)));

        dispatcher.register(Commands.literal("bal")
                .executes(BalanceCommand::executeSelf)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(BalanceCommand::executeOther)));
    }

    private static int executeSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            double balance = Wessez.getEconomyData().getBalance(player.getUUID());

            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("§aYour balance: §e$%.2f", balance)),
                    false
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
    }

    private static int executeOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            double balance = Wessez.getEconomyData().getBalance(target.getUUID());

            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("§a%s's balance: §e$%.2f",
                            target.getName().getString(), balance)),
                    false
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Player not found"));
            return 0;
        }
    }
}
