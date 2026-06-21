package com.companionmod.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Equipment screen: Head / Chest / Legs / Feet / MainHand / OffHand + player inventory. */
public class CompanionArmorScreen extends AbstractContainerScreen<CompanionArmorMenu> {

    private static final String[] LABELS = {"H", "C", "L", "B", "M", "O"};

    public CompanionArmorScreen(CompanionArmorMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        imageWidth  = 176;
        imageHeight = 140;
        inventoryLabelY = imageHeight - 94;
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

        // Equipment slot backgrounds + tiny labels
        for (int i = 0; i < 6; i++) {
            int sx = x + 33 + i * 18, sy = y + 19;
            gfx.fill(sx, sy, sx + 18, sy + 18, 0xFF555577);
            gfx.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF252540);
            gfx.drawString(font, LABELS[i], sx + 5, sy + 5, 0x888888, false);
        }

        // Equipment legend
        gfx.drawString(font, "H=Head C=Chest L=Legs B=Boots M=Main O=Off", x + 4, y + 9, 0x666666, false);

        // Player inventory slots
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) {
                int sx = x + 7 + col * 18, sy = y + 57 + row * 18;
                gfx.fill(sx, sy, sx + 18, sy + 18, 0xFF555577);
                gfx.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF252540);
            }
        // Hotbar slots
        for (int col = 0; col < 9; col++) {
            int sx = x + 7 + col * 18, sy = y + 115;
            gfx.fill(sx, sy, sx + 18, sy + 18, 0xFF555577);
            gfx.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF252540);
        }
    }
}
