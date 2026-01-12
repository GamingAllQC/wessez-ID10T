package net.wessez.economy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.wessez.Wessez;

public class PayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .executes(PayCommand::executePay))));
    }

    private static int executePay(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer sender = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            if (sender.getUUID().equals(target.getUUID())) {
                context.getSource().sendFailure(Component.literal("§cYou cannot pay yourself!"));
                return 0;
            }

            double senderBalance = Wessez.getEconomyData().getBalance(sender.getUUID());

            if (!Wessez.getEconomyData().hasBalance(sender.getUUID(), amount)) {
                context.getSource().sendFailure(Component.literal(String.format("§cInsufficient funds! You have §6$%.2f§c but need §6$%.2f", senderBalance, amount)));
                return 0;
            }

            Wessez.getEconomyData().subtractBalance(sender.getUUID(), amount);
            Wessez.getEconomyData().addBalance(target.getUUID(), amount);

            context.getSource().sendSuccess(() -> Component.literal(String.format("§aYou sent §6$%.2f §ato %s", amount, target.getName().getString())), false);
            target.sendSystemMessage(Component.literal(String.format("§aYou received §6$%.2f §afrom %s", amount, sender.getName().getString())));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError processing payment: " + e.getMessage()));
            return 0;
        }
    }
}
