package net.wessez.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.wessez.essentials.config.EssentialsConfig;
import net.wessez.essentials.data.EssentialsData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UtilCommands {

    /** Last PM partners: sender UUID -> recipient UUID */
    private static final Map<UUID, UUID> lastMsgPartner = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerFly(dispatcher);
        registerHeal(dispatcher);
        registerFeed(dispatcher);
        registerGod(dispatcher);
        registerNick(dispatcher);
        registerMsg(dispatcher);
    }

    // ── /fly ─────────────────────────────────────────────────────────────────

    private static void registerFly(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fly")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER) || EssentialsConfig.isFlyEnabled())
                .executes(ctx -> toggleFly(ctx, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .executes(ctx -> toggleFly(ctx, EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int toggleFly(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        try {
            ServerPlayer player = target != null ? target : ctx.getSource().getPlayerOrException();
            boolean flying = EssentialsData.get().toggleFly(player.getUUID());
            player.getAbilities().mayfly = flying;
            if (!flying) player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.sendSystemMessage(Component.literal(flying
                    ? "§aFlight §2enabled§a."
                    : "§cFlight §4disabled§c."));
            if (target != null) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "§aToggled fly for §e" + player.getName().getString()), true);
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /fly"));
            return 0;
        }
    }

    // ── /heal ─────────────────────────────────────────────────────────────────

    private static void registerHeal(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("heal")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .executes(ctx -> heal(ctx, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> heal(ctx, EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int heal(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        try {
            ServerPlayer player = target != null ? target : ctx.getSource().getPlayerOrException();
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20f);
            // Remove negative effects
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WITHER);
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.SLOWNESS);
            player.removeEffect(MobEffects.HUNGER);
            player.sendSystemMessage(Component.literal("§aYou have been healed!"));
            if (target != null) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "§aHealed §e" + player.getName().getString()), true);
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /heal"));
            return 0;
        }
    }

    // ── /feed ─────────────────────────────────────────────────────────────────

    private static void registerFeed(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("feed")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .executes(ctx -> feed(ctx, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> feed(ctx, EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int feed(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        try {
            ServerPlayer player = target != null ? target : ctx.getSource().getPlayerOrException();
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20f);
            player.sendSystemMessage(Component.literal("§aYou have been fed!"));
            if (target != null) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "§aFed §e" + player.getName().getString()), true);
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /feed"));
            return 0;
        }
    }

    // ── /god ─────────────────────────────────────────────────────────────────

    private static void registerGod(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("god")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .executes(ctx -> toggleGod(ctx, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> toggleGod(ctx, EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int toggleGod(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        try {
            ServerPlayer player = target != null ? target : ctx.getSource().getPlayerOrException();
            boolean god = EssentialsData.get().toggleGod(player.getUUID());
            // God mode via invulnerable tag isn't persistent cleanly, use ability setInvulnerable
            player.setInvulnerable(god);
            player.sendSystemMessage(Component.literal(god
                    ? "§aGod mode §2enabled§a."
                    : "§cGod mode §4disabled§c."));
            if (target != null) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "§aToggled god for §e" + player.getName().getString()), true);
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /god"));
            return 0;
        }
    }

    // ── /nick ─────────────────────────────────────────────────────────────────

    private static void registerNick(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nick")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> setNick(ctx, StringArgumentType.getString(ctx, "name")))));
    }

    private static int setNick(CommandContext<CommandSourceStack> ctx, String nick) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            if (nick.equalsIgnoreCase("clear") || nick.equalsIgnoreCase("reset")) {
                player.setCustomName(null);
                player.sendSystemMessage(Component.literal("§aNickname cleared."));
            } else {
                if (nick.length() > 32) {
                    player.sendSystemMessage(Component.literal("§cNickname too long (max 32 chars)."));
                    return 0;
                }
                // Allow color codes with & prefix
                String formatted = nick.replace("&", "§");
                player.setCustomName(Component.literal(formatted));
                player.sendSystemMessage(Component.literal("§aNickname set to: " + formatted));
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /nick"));
            return 0;
        }
    }

    // ── /msg & /reply ─────────────────────────────────────────────────────────

    private static void registerMsg(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /msg <player> <message>
        dispatcher.register(Commands.literal("msg")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> sendMsg(ctx,
                                        EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "message"))))));

        dispatcher.register(Commands.literal("tell")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> sendMsg(ctx,
                                        EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "message"))))));

        dispatcher.register(Commands.literal("w")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> sendMsg(ctx,
                                        EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "message"))))));

        // /reply <message>
        dispatcher.register(Commands.literal("reply")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> reply(ctx, StringArgumentType.getString(ctx, "message")))));

        dispatcher.register(Commands.literal("r")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> reply(ctx, StringArgumentType.getString(ctx, "message")))));
    }

    private static int sendMsg(CommandContext<CommandSourceStack> ctx, ServerPlayer recipient, String message) {
        try {
            ServerPlayer sender = ctx.getSource().getPlayerOrException();
            if (sender.getUUID().equals(recipient.getUUID())) {
                sender.sendSystemMessage(Component.literal("§cYou can't message yourself!"));
                return 0;
            }
            lastMsgPartner.put(sender.getUUID(), recipient.getUUID());
            lastMsgPartner.put(recipient.getUUID(), sender.getUUID());

            String senderName = sender.getName().getString();
            String recipientName = recipient.getName().getString();

            sender.sendSystemMessage(Component.literal("§7[Me → " + recipientName + "] §f" + message));
            recipient.sendSystemMessage(Component.literal("§7[" + senderName + " → Me] §f" + message));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /msg"));
            return 0;
        }
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String message) {
        try {
            ServerPlayer sender = ctx.getSource().getPlayerOrException();
            UUID partnerId = lastMsgPartner.get(sender.getUUID());
            if (partnerId == null) {
                sender.sendSystemMessage(Component.literal("§cNo one to reply to!"));
                return 0;
            }
            ServerPlayer partner = ctx.getSource().getServer().getPlayerList().getPlayer(partnerId);
            if (partner == null) {
                sender.sendSystemMessage(Component.literal("§cThat player is no longer online."));
                return 0;
            }
            return sendMsg(ctx, partner, message);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use /reply"));
            return 0;
        }
    }
}
