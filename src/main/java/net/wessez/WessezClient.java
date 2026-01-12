package net.wessez;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.wessez.economy.gui.BaltopScreen;

public class WessezClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Use Wessez.BALTOP_SCREEN_HANDLER instead of BaltopScreenHandler.TYPE
        MenuScreens.register(Wessez.BALTOP_SCREEN_HANDLER, BaltopScreen::new);
    }
}
