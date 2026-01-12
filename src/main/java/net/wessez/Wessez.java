package net.wessez;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.wessez.economy.command.*;
import net.wessez.economy.data.EconomyData;
import net.wessez.economy.gui.BaltopScreenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wessez implements ModInitializer {
	public static final String MOD_ID = "wessez";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static EconomyData economyData;
	private static MinecraftServer server;

	// Declare the menu type without registering it yet
	public static MenuType<BaltopScreenHandler> BALTOP_SCREEN_HANDLER;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Wessez Economy Mod");

		// Register the menu type here using a string identifier
		BALTOP_SCREEN_HANDLER = Registry.register(
				BuiltInRegistries.MENU,
				MOD_ID + ":baltop",
				new MenuType<>(BaltopScreenHandler::new, FeatureFlags.VANILLA_SET)
		);

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BalanceCommand.register(dispatcher);
			PayCommand.register(dispatcher);
			EcoCommand.register(dispatcher);
			DailyCommand.register(dispatcher);
			BaltopCommand.register(dispatcher);
			PlaytimeCommand.register(dispatcher);
		});

		// Server lifecycle events
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

		// Player connection events
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			LOGGER.info("Player {} joined - starting session tracking", handler.getPlayer().getName().getString());
			economyData.ensurePlayerExists(handler.getPlayer().getUUID());
			economyData.startSession(handler.getPlayer().getUUID());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			LOGGER.info("Player {} disconnected - ending session tracking", handler.getPlayer().getName().getString());
			economyData.endSession(handler.getPlayer().getUUID());
			economyData.save(server);
		});

		LOGGER.info("Wessez Economy Mod initialized successfully");
	}

	private void onServerStarting(MinecraftServer minecraftServer) {
		server = minecraftServer;
		economyData = EconomyData.load(minecraftServer);
		LOGGER.info("Economy system initialized on server start");
	}

	private void onServerStopping(MinecraftServer minecraftServer) {
		if (economyData != null) {
			// End all active sessions
			economyData.sessionStartTime.keySet().forEach(uuid -> {
				LOGGER.info("Ending session for UUID: {}", uuid);
				economyData.endSession(uuid);
			});
			economyData.save(minecraftServer);
			LOGGER.info("Economy data saved on server stop");
		}
	}

	public static EconomyData getEconomyData() {
		return economyData;
	}

	public static MinecraftServer getServer() {
		return server;
	}
}
