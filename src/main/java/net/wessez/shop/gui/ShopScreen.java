package net.wessez.shop.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ShopScreen extends AbstractContainerScreen<ShopScreenHandler> {

    public ShopScreen(ShopScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 222; // 6 rows * 18 + header + padding
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        // Background
        graphics.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xFF2B2B2B, 0xFF1A1A1A);

        // Title bar
        graphics.fill(x, y, x + imageWidth, y + 18, 0xFF111111);
        graphics.drawString(font, "§6§lShop", x + 6, y + 5, 0xFFFFAA00, false);

        // Item area border
        graphics.fill(x + 7, y + 17, x + imageWidth - 7, y + 19, 0xFF555555);

        // Navigation row background
        graphics.fill(x, y + imageHeight - 22, x + imageWidth, y + imageHeight - 1, 0xFF111111);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Suppress player inventory label
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
