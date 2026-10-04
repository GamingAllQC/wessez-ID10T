package net.wessez;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.wessez.economy.command.*;
import net.wessez.economy.config.Config;
import net.wessez.economy.data.EconomyData;
import net.wessez.economy.gui.BaltopScreenData;
import net.wessez.economy.gui.BaltopScreenHandler;
import net.wessez.essentials.command.*;
import net.wessez.essentials.config.EssentialsConfig;
import net.wessez.essentials.data.EssentialsData;
import net.wessez.shop.command.ShopCommand;
import net.wessez.shop.data.ShopData;
import net.wessez.shop.gui.ShopScreenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wessez implements ModInitializer {
	public static final String MOD_ID = "wessez";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static EconomyData economyData;
	private static MinecraftServer server;
	private static int autoSaveTick = 0;

	// Menu types
	public static ExtendedScreenHandlerType<BaltopScreenHandler, BaltopScreenData> BALTOP_SCREEN_HANDLER;
	public static MenuType<ShopScreenHandler> SHOP_SCREEN_HANDLER;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Wessez Mod");

		// ── Register menu types ──────────────────────────────────────────────
		BALTOP_SCREEN_HANDLER = Registry.register(
				BuiltInRegistries.MENU,
				MOD_ID + ":baltop",
				new ExtendedScreenHandlerType<>(BaltopScreenHandler::new, BaltopScreenData.PACKET_CODEC)
		);

		SHOP_SCREEN_HANDLER = Registry.register(
				BuiltInRegistries.MENU,
				MOD_ID + ":shop",
				new MenuType<>(ShopScreenHandler::new, FeatureFlags.VANILLA_SET)
		);

		// ── Register commands ────────────────────────────────────────────────
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			// Economy
			BalanceCommand.register(dispatcher);
			PayCommand.register(dispatcher);
			EcoCommand.register(dispatcher);
			DailyCommand.register(dispatcher);
			BaltopCommand.register(dispatcher);
			PlaytimeCommand.register(dispatcher);

			// Essentials
			SpawnCommand.register(dispatcher);
			HomeCommand.register(dispatcher);
			TpaCommand.register(dispatcher);
			BackCommand.register(dispatcher);
			WarpCommand.register(dispatcher);
			RtpCommand.register(dispatcher);
			UtilCommands.register(dispatcher);

			// Shop
			ShopCommand.register(dispatcher);
		});

		// ── Server lifecycle ─────────────────────────────────────────────────
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

		// ── Auto-save tick + essentials warmup processing ────────────────────
		ServerTickEvents.END_SERVER_TICK.register(s -> {
			autoSaveTick++;
			if (economyData != null && autoSaveTick % Config.getAutoSaveInterval() == 0) {
				economyData.save(s);
			}
			// Process warmup teleports every tick
			EssentialsData.processPendingTeleports(s);
		});

		// ── Player join / leave ───────────────────────────────────────────────
		ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> {
			LOGGER.info("Player {} joined", handler.getPlayer().getName().getString());
			economyData.ensurePlayerExists(handler.getPlayer().getUUID());
			economyData.startSession(handler.getPlayer().getUUID());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
			LOGGER.info("Player {} disconnected", handler.getPlayer().getName().getString());
			economyData.endSession(handler.getPlayer().getUUID());
			economyData.save(s);
		});

		LOGGER.info("Wessez Mod initialized — Economy | Essentials | Shop");
	}

	private void onServerStarting(MinecraftServer minecraftServer) {
		server = minecraftServer;
		// Load configs first
		Config.load(minecraftServer);
		EssentialsConfig.load(minecraftServer);
		// Then load data
		economyData = EconomyData.load(minecraftServer);
		EssentialsData.load(minecraftServer);
		ShopData.load(minecraftServer);
		LOGGER.info("All systems initialized on server start");
	}

	private void onServerStopping(MinecraftServer minecraftServer) {
		if (economyData != null) {
			new java.util.HashSet<>(economyData.getActiveSessions()).forEach(uuid -> {
				LOGGER.info("Ending session for UUID: {}", uuid);
				economyData.endSession(uuid);
			});
			economyData.save(minecraftServer);
		}
		EssentialsData.get().save(minecraftServer);
		ShopData.get().save(minecraftServer);
		LOGGER.info("All data saved on server stop");
	}

	public static EconomyData getEconomyData() { return economyData; }
	public static MinecraftServer getServer()  { return server; }
}
