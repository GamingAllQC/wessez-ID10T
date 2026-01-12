package net.wessez.economy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.wessez.economy.gui.BaltopScreenHandler;

public class BaltopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register main command: /balancetop
        dispatcher.register(Commands.literal("balancetop")
                .executes(BaltopCommand::execute));

        // Register alias: /baltop
        dispatcher.register(Commands.literal("baltop")
                .executes(BaltopCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            BaltopScreenHandler.openScreen(player);
        }
        return 1;
    }
}
