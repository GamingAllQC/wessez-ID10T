package net.wessez;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.wessez.economy.gui.BaltopScreen;
import net.wessez.shop.gui.ShopScreen;

public class WessezClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Wessez.BALTOP_SCREEN_HANDLER, BaltopScreen::new);
        MenuScreens.register(Wessez.SHOP_SCREEN_HANDLER, ShopScreen::new);
    }
}
