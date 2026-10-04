package net.wessez.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.wessez.essentials.config.EssentialsConfig;
import net.wessez.essentials.data.EssentialsData;
import net.wessez.essentials.data.EssentialsData.SavedLocation;
import net.wessez.essentials.data.EssentialsData.TpaRequest;

public class TpaCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tpa <player>  — requester teleports TO target
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> sendRequest(ctx, false))));

        // /tpahere <player>  — target teleports TO requester
        dispatcher.register(Commands.literal("tpahere")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> sendRequest(ctx, true))));

        // /tpaccept  /tpyes
        dispatcher.register(Commands.literal("tpaccept").executes(TpaCommand::accept));
        dispatcher.register(Commands.literal("tpyes").executes(TpaCommand::accept));

        // /tpdeny  /tpno
        dispatcher.register(Commands.literal("tpdeny").executes(TpaCommand::deny));
        dispatcher.register(Commands.literal("tpno").executes(TpaCommand::deny));
    }

    /** isHere=false → requester goes to target. isHere=true → target goes to requester. */
    private static int sendRequest(CommandContext<CommandSourceStack> ctx, boolean isHere) {
        try {
            ServerPlayer requester = ctx.getSource().getPlayerOrException();
            ServerPlayer target    = EntityArgument.getPlayer(ctx, "player");

            if (requester.getUUID().equals(target.getUUID())) {
                requester.sendSystemMessage(Component.literal("§cYou can't TPA to yourself!"));
                return 0;
            }

            long expiresAt = System.currentTimeMillis() + EssentialsConfig.getTpaTimeoutSeconds() * 1000L;
            TpaRequest req = new TpaRequest(requester.getUUID(), requester.getName().getString(), isHere, expiresAt);
            EssentialsData.get().addTpaRequest(target.getUUID(), req);

            String verb = isHere ? "teleport to you" : "teleport to them";
            requester.sendSystemMessage(Component.literal(
                    "§aTPA request sent to §e" + target.getName().getString() +
                    "§a. Expires in §e" + EssentialsConfig.getTpaTimeoutSeconds() + "s."));
            target.sendSystemMessage(Component.literal(
                    "§e" + requester.getName().getString() +
                    " §awants to §e" + verb + "§a.\n" +
                    "§a  /tpaccept §7to accept  §c/tpdeny §7to deny"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cPlayer not found."));
            return 0;
        }
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = ctx.getSource().getPlayerOrException();

            return EssentialsData.get().getIncomingTpa(target.getUUID()).map(req -> {
                EssentialsData.get().removeTpaRequest(target.getUUID());

                ServerPlayer requester = ctx.getSource().getServer()
                        .getPlayerList().getPlayer(req.requesterUuid());
                if (requester == null) {
                    target.sendSystemMessage(Component.literal("§cRequester is no longer online."));
                    return 0;
                }

                if (req.isHere()) {
                    // Target teleports to requester
                    Vec3 rPos = requester.position();
                    SavedLocation dest = new SavedLocation(
                            net.wessez.essentials.data.EssentialsData.getLevelId(requester.level()),
                            rPos.x, rPos.y, rPos.z,
                            requester.getYRot(), requester.getXRot()
                    );
                    EssentialsData.scheduleTeleport(target, dest);
                    requester.sendSystemMessage(Component.literal("§a" + target.getName().getString() + " accepted your TPA request!"));
                    target.sendSystemMessage(Component.literal("§aTeleporting to §e" + requester.getName().getString() + "§a!"));
                } else {
                    // Requester teleports to target
                    Vec3 tPos = target.position();
                    SavedLocation dest = new SavedLocation(
                            net.wessez.essentials.data.EssentialsData.getLevelId(target.level()),
                            tPos.x, tPos.y, tPos.z,
                            target.getYRot(), target.getXRot()
                    );
                    EssentialsData.scheduleTeleport(requester, dest);
                    requester.sendSystemMessage(Component.literal("§a" + target.getName().getString() + " accepted your TPA request! Teleporting..."));
                    target.sendSystemMessage(Component.literal("§aTPA accepted. §e" + requester.getName().getString() + " §ais teleporting to you."));
                }
                return 1;
            }).orElseGet(() -> {
                target.sendSystemMessage(Component.literal("§cNo pending TPA request (or it expired)."));
                return 0;
            });
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /tpaccept"));
            return 0;
        }
    }

    private static int deny(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = ctx.getSource().getPlayerOrException();

            return EssentialsData.get().getIncomingTpa(target.getUUID()).map(req -> {
                EssentialsData.get().removeTpaRequest(target.getUUID());

                ServerPlayer requester = ctx.getSource().getServer()
                        .getPlayerList().getPlayer(req.requesterUuid());
                if (requester != null) {
                    requester.sendSystemMessage(Component.literal(
                            "§c" + target.getName().getString() + " denied your TPA request."));
                }
                target.sendSystemMessage(Component.literal("§aTPA request denied."));
                return 1;
            }).orElseGet(() -> {
                target.sendSystemMessage(Component.literal("§cNo pending TPA request."));
                return 0;
            });
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /tpdeny"));
            return 0;
        }
    }
}
