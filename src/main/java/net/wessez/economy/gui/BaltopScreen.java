package net.wessez.economy.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Map;

public class BaltopScreen extends AbstractContainerScreen<BaltopScreenHandler> {

    private int scrollOffset = 0;

    public BaltopScreen(BaltopScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.scrollOffset = 0;
        this.imageWidth = 280;
        this.imageHeight = 240;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // FULL BACKGROUND
        graphics.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xFF404040, 0xFF202020);
        graphics.fillGradient(x + 12, y + 50, x + imageWidth - 12, y + imageHeight - 20, 0xFF303030, 0xFF101010);

        // TITLE BAR
        graphics.drawString(font, "§l§6🏆 Top Player’s Balance", x + 20, y + 16, 0xFFFFAA00, false);
        graphics.drawString(font, "§7All Players", x + 20, y + 32, 0xFFDDDDDD, false);
        graphics.fill(x + 16, y + 44, x + imageWidth - 16, y + 46, 0xFF777777);

        renderBaltopList(graphics, x, y);
    }

    private void renderBaltopList(GuiGraphics graphics, int x, int y) {
        List<Map.Entry<String, Double>> balances = menu.getTopBalances();
        if (balances.isEmpty()) {
            graphics.drawString(font, "§7No data loaded...", x + 25, y + 70, 0xFFAAAAAA, false);
            return;
        }

        // Already sorted from the handler
        // SHOW 14 LINES MAX
        for (int i = scrollOffset; i < Math.min(scrollOffset + 14, balances.size()); i++) {
            var entry = balances.get(i);
            String rank = switch (i) {
                case 0 -> "§6🥇";
                case 1 -> "§7🥈";
                case 2 -> "§c🥉";
                default -> "§f#" + (i + 1);
            };

            String line = String.format("%s §r%s §8» §a$%.1f", rank, entry.getKey(), entry.getValue());
            int lineIndex = i - scrollOffset;
            graphics.drawString(font, line, x + 25, y + 55 + (lineIndex * 12), 0xFFFFFFFF, false);
        }

        // PAGE INFO
        String footer = String.format("§7Page %d/%d (%d players total)",
                scrollOffset / 14 + 1, (balances.size() + 13) / 14, balances.size());
        graphics.drawString(font, footer, x + 20, y + imageHeight - 15, 0xFFAAAAAA, false);
    }

    // Removed @Override - method signature might differ in 1.21.1
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<Map.Entry<String, Double>> balances = menu.getTopBalances();

        int maxScroll = Math.max(0, balances.size() - 14);
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 2);
        } else {
            scrollOffset = Math.min(maxScroll, scrollOffset + 2);
        }
        return true;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // EMPTY - NO INVENTORY LABELS
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
