package com.companionmod.gui;

import com.companionmod.BehaviorMode;
import com.companionmod.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * v3 Revamped companion GUI.
 * Dark-navy panel with accent borders, sectioned layout, segmented health bar.
 */
public class CompanionScreen extends Screen {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final int BG        = 0xF20A0E1A;
    private static final int SURFACE   = 0xCC10182C;
    private static final int HEADER    = 0xFF0D1430;
    private static final int BORDER_HI = 0xFF4A7BFF;
    private static final int BORDER_LO = 0xFF1E3A7F;
    private static final int ACCENT    = 0xFF7B5FFF;
    private static final int TEXT_H    = 0xFFE0E8FF;
    private static final int TEXT_S    = 0xFF8899CC;
    private static final int TEXT_D    = 0xFF445577;
    private static final int HP_HI     = 0xFF44DD44;
    private static final int HP_MID    = 0xFFFFAA00;
    private static final int HP_LO     = 0xFFFF4444;
    private static final int BTN_ACT   = 0xFF1C3060;  // active-mode button bg tint

    private static final int W = 252, H = 284;

    private final LivingEntity mob;
    private final int          entityId;
    private       boolean      isFriend;
    private       BehaviorMode currentMode;
    private       String       nicknameText;
    private       EditBox      nicknameField;

    public CompanionScreen(LivingEntity mob, boolean isFriend, BehaviorMode mode, String nickname) {
        super(Component.literal("Companion"));
        this.mob          = mob;
        this.entityId     = mob.getId();
        this.isFriend     = isFriend;
        this.currentMode  = mode;
        this.nicknameText = nickname == null ? "" : nickname;
    }

    @Override protected void init() { super.init(); buildButtons(); }

    private int L() { return (width  - W) / 2; }
    private int T() { return (height - H) / 2; }

    // ─────────────────────────────────────────────────────────────────────────
    private void buildButtons() {
        if (nicknameField != null) nicknameText = nicknameField.getValue();
        clearWidgets();
        int L = L(), T = T();

        if (!isFriend) {
            // Pre-friend — single big button
            addRenderableWidget(Button.builder(
                    Component.literal("Let's be Friends!"),
                    btn -> {
                        PacketHandler.sendToServer(new SetFriendPacket(entityId, true));
                        isFriend = true; currentMode = BehaviorMode.FOLLOW; buildButtons();
                    }
            ).bounds(L + 36, T + 98, 180, 24).build());
            return;
        }

        // ── Nickname row ──────────────────────────────────────────────────────
        nicknameField = new EditBox(font, L + 8, T + 44, 192, 16, Component.literal("Nickname"));
        nicknameField.setMaxLength(24);
        nicknameField.setValue(nicknameText);
        nicknameField.setHint(Component.literal("Enter nickname..."));
        addRenderableWidget(nicknameField);
        addRenderableWidget(Button.builder(Component.literal("Set"),
                btn -> {
                    nicknameText = nicknameField.getValue().trim();
                    PacketHandler.sendToServer(new SetNicknamePacket(entityId, nicknameText));
                }
        ).bounds(L + 204, T + 44, 40, 16).build());

        // ── Behaviour row (4 buttons, equal width) ────────────────────────────
        int bw = 57, gap = 4, bx = L + 8;
        BehaviorMode[] modes = BehaviorMode.values();
        for (int i = 0; i < modes.length; i++) {
            BehaviorMode bm = modes[i];
            boolean active = currentMode == bm;
            String lbl = (active ? "[" : "") + capFirst(bm.name()) + (active ? "]" : "");
            int fi = i;
            addRenderableWidget(Button.builder(Component.literal(lbl),
                    btn -> {
                        PacketHandler.sendToServer(new SetBehaviorPacket(entityId, BehaviorMode.values()[fi]));
                        currentMode = BehaviorMode.values()[fi]; buildButtons();
                    }
            ).bounds(bx + i * (bw + gap), T + 100, bw, 18).build());
        }

        // ── Transport row ─────────────────────────────────────────────────────
        addRenderableWidget(Button.builder(Component.literal("Mob → Me"),
                btn -> PacketHandler.sendToServer(new MobActionPacket(entityId, MobActionPacket.Action.TELEPORT_MOB_HERE))
        ).bounds(L + 8, T + 146, 114, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Me → Mob"),
                btn -> PacketHandler.sendToServer(new MobActionPacket(entityId, MobActionPacket.Action.GO_TO_MOB))
        ).bounds(L + 130, T + 146, 114, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Mount"),
                btn -> PacketHandler.sendToServer(new MobActionPacket(entityId, MobActionPacket.Action.MOUNT))
        ).bounds(L + 8, T + 168, 114, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Dismount"),
                btn -> PacketHandler.sendToServer(new MobActionPacket(entityId, MobActionPacket.Action.DISMOUNT))
        ).bounds(L + 130, T + 168, 114, 18).build());

        // ── Storage row ───────────────────────────────────────────────────────
        addRenderableWidget(Button.builder(Component.literal("Inventory"),
                btn -> { PacketHandler.sendToServer(new OpenInventoryPacket(entityId)); onClose(); }
        ).bounds(L + 8, T + 212, 114, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Armory"),
                btn -> { PacketHandler.sendToServer(new OpenArmorPacket(entityId)); onClose(); }
        ).bounds(L + 130, T + 212, 114, 18).build());

        // ── Unfriend ─────────────────────────────────────────────────────────
        addRenderableWidget(Button.builder(Component.literal("Unfriend"),
                btn -> {
                    PacketHandler.sendToServer(new SetFriendPacket(entityId, false));
                    isFriend = false; buildButtons();
                }
        ).bounds(L + 86, T + 248, 80, 16).build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int L = L(), T = T();

        // ── Outer panel ──────────────────────────────────────────────────────
        g.fill(L, T, L + W, T + H, BG);
        // Border — bright top/bottom, darker sides
        g.fill(L,         T,         L + W,     T + 1,     BORDER_HI);
        g.fill(L,         T + H - 1, L + W,     T + H,     BORDER_HI);
        g.fill(L,         T,         L + 1,     T + H,     BORDER_LO);
        g.fill(L + W - 1, T,         L + W,     T + H,     BORDER_LO);
        // Left accent stripe
        g.fill(L + 1, T + 1, L + 4, T + H - 1, ACCENT);

        // ── Header bar ───────────────────────────────────────────────────────
        g.fill(L + 4, T + 1, L + W - 1, T + 24, HEADER);
        String typeName = mob.getType().getDescription().getString();
        g.drawCenteredString(font, typeName, L + W / 2 + 1, T + 8, TEXT_H);

        if (!isFriend) {
            // Pre-friend copy
            g.fill(L + 4, T + 28, L + W - 1, T + 29, BORDER_LO);
            g.drawCenteredString(font, "This " + typeName + " is watching you.", L + W / 2 + 1, T + 48, TEXT_S);
            g.drawCenteredString(font, "Would you like to be friends?",           L + W / 2 + 1, T + 62, TEXT_D);
        } else {
            // ── Nickname section label ────────────────────────────────────────
            g.fill(L + 4, T + 28, L + W - 1, T + 29, BORDER_LO);
            g.drawString(font, "NICKNAME", L + 9, T + 32, TEXT_D, false);

            // ── Health bar (y = 66..76) ───────────────────────────────────────
            float hp    = mob.getHealth();
            float maxHp = mob.getMaxHealth();
            float ratio = maxHp > 0 ? hp / maxHp : 1f;
            int bLeft = L + 8, bTop = T + 66, bW = W - 16, bH = 10;
            // Segmented background
            g.fill(bLeft, bTop, bLeft + bW, bTop + bH, 0xFF111828);
            // Filled portion
            int fill  = (int)(bW * ratio);
            int hpCol = ratio > 0.5f ? HP_HI : ratio > 0.25f ? HP_MID : HP_LO;
            if (fill > 0) g.fill(bLeft, bTop, bLeft + fill, bTop + bH, hpCol);
            // Segment tick marks every 10% 
            for (int seg = 1; seg < 10; seg++) {
                int tx = bLeft + bW * seg / 10;
                g.fill(tx, bTop, tx + 1, bTop + bH, 0x55000000);
            }
            // HP text centred on bar
            String hpTxt = (int) hp + " / " + (int) maxHp + " HP";
            g.drawCenteredString(font, hpTxt, bLeft + bW / 2, bTop + 1, 0xFFFFFFFF);

            // ── Behaviour section ─────────────────────────────────────────────
            section(g, L, T + 82, W, "BEHAVIOR");

            // ── Transport section ─────────────────────────────────────────────
            section(g, L, T + 130, W, "TRANSPORT");

            // ── Storage section ───────────────────────────────────────────────
            section(g, L, T + 196, W, "STORAGE");

            // ── Danger section ────────────────────────────────────────────────
            section(g, L, T + 236, W, "");
        }

        g.drawCenteredString(font, "ESC to close", L + W / 2 + 1, T + H - 10, TEXT_D);
        super.render(g, mx, my, pt);
    }

    private void section(GuiGraphics g, int L, int y, int w, String label) {
        g.fill(L + 4, y, L + w - 1, y + 1, BORDER_LO);
        if (!label.isEmpty())
            g.drawString(font, label, L + 9, y + 3, TEXT_D, false);
    }

    private static String capFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    @Override public boolean isPauseScreen() { return false; }
}
