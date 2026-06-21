package com.companionmod.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dynamically sized inventory screen. Height scales with mob's health (1-5 rows). */
public class CompanionInventoryScreen extends AbstractContainerScreen<CompanionMenu> {

    private final int mobRows;

    public CompanionInventoryScreen(CompanionMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.mobRows        = menu.getMobRows();
        this.imageWidth     = 176;
        this.imageHeight    = 18 + mobRows * 18 + 14 + 3 * 18 + 4 + 18 + 6; // dynamic
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float pt) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, pt);
        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        gfx.fill(x, y, x + w, y + h, 0xDD1A1A2E);
        gfx.fill(x, y, x + w, y + 1, 0xFF4A90D9);
        gfx.fill(x, y + h - 1, x + w, y + h, 0xFF4A90D9);
        gfx.fill(x, y, x + 1, y + h, 0xFF4A90D9);
        gfx.fill(x + w - 1, y, x + w, y + h, 0xFF4A90D9);

        // Mob inventory slots
        for (int row = 0; row < mobRows; row++)
            drawSlotRow(gfx, x + 7, y + 17 + row * 18, 9);

        // Player inv + hotbar
        int playerY = y + 17 + mobRows * 18 + 14;
        for (int row = 0; row < 3; row++) drawSlotRow(gfx, x + 7, playerY + row * 18, 9);
        drawSlotRow(gfx, x + 7, playerY + 58, 9);
    }

    private static void drawSlotRow(GuiGraphics gfx, int sx, int sy, int count) {
        for (int i = 0; i < count; i++) {
            gfx.fill(sx + i * 18, sy, sx + i * 18 + 18, sy + 18, 0xFF555577);
            gfx.fill(sx + i * 18 + 1, sy + 1, sx + i * 18 + 17, sy + 17, 0xFF252540);
        }
    }
}
