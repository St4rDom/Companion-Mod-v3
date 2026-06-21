package com.companionmod.gui;

import com.companionmod.BehaviorMode;
import com.companionmod.client.ClientFriendshipStore;
import com.companionmod.network.BringAllPacket;
import com.companionmod.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Companion Manager — open with the ; key.
 * Shows a scrollable list of all loaded companion mobs.
 * Click an entry to open its CompanionScreen.
 * "Bring All" teleports every loaded companion to the player.
 */
public class CompanionManagerScreen extends Screen {

    // ── Palette (matches CompanionScreen) ─────────────────────────────────────
    private static final int BG        = 0xF20A0E1A;
    private static final int HEADER    = 0xFF0D1430;
    private static final int BORDER_HI = 0xFF4A7BFF;
    private static final int BORDER_LO = 0xFF1E3A7F;
    private static final int ACCENT    = 0xFF7B5FFF;
    private static final int ENTRY_BG  = 0xBB121E38;
    private static final int ENTRY_HOV = 0xCC1A2A50;
    private static final int TEXT_H    = 0xFFE0E8FF;
    private static final int TEXT_S    = 0xFF8899CC;
    private static final int TEXT_D    = 0xFF445577;
    private static final int HP_HI     = 0xFF44DD44;
    private static final int HP_MID    = 0xFFFFAA00;
    private static final int HP_LO     = 0xFFFF4444;

    private static final int W = 310, H = 230;
    private static final int ENTRY_H = 26;
    private static final int LIST_CAP = 6; // visible rows

    private List<LivingEntity> companions = new ArrayList<>();
    private int scrollOffset = 0;

    public CompanionManagerScreen() {
        super(Component.literal("Companion Manager"));
    }

    @Override
    protected void init() {
        super.init();
        refreshList();
        int L = (width - W) / 2, T = (height - H) / 2;

        // "Bring All" button in header
        addRenderableWidget(Button.builder(
                Component.literal("Bring All"),
                btn -> PacketHandler.sendToServer(new BringAllPacket())
        ).bounds(L + W - 82, T + 5, 74, 16).build());
    }

    private void refreshList() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        companions = mc.level.getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(512.0))
                .stream()
                .filter(e -> !(e instanceof Player) && ClientFriendshipStore.isFriend(e.getUUID()))
                .sorted(Comparator.comparingDouble(e -> e.distanceTo(mc.player)))
                .collect(java.util.stream.Collectors.toList());
        int maxScroll = Math.max(0, companions.size() - LIST_CAP);
        scrollOffset  = Math.min(scrollOffset, maxScroll);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        refreshList();
        int L = (width - W) / 2, T = (height - H) / 2;

        // Panel
        g.fill(L, T, L + W, T + H, BG);
        g.fill(L,         T,         L + W, T + 1,     BORDER_HI);
        g.fill(L,         T + H - 1, L + W, T + H,     BORDER_HI);
        g.fill(L,         T,         L + 1, T + H,     BORDER_LO);
        g.fill(L + W - 1, T,         L + W, T + H,     BORDER_LO);
        g.fill(L + 1, T + 1, L + 4, T + H - 1, ACCENT);

        // Header bar
        g.fill(L + 4, T + 1, L + W - 1, T + 26, HEADER);
        g.drawString(font, "COMPANION MANAGER", L + 9, T + 8, TEXT_H, false);

        // Divider below header
        g.fill(L + 4, T + 26, L + W - 1, T + 27, BORDER_LO);

        int listL = L + 6;
        int listT = T + 30;
        int entryW = W - 12;

        if (companions.isEmpty()) {
            g.drawCenteredString(font, "No companions loaded nearby.", L + W / 2, listT + 60, TEXT_D);
        } else {
            int visible = Math.min(LIST_CAP, companions.size() - scrollOffset);
            for (int i = 0; i < visible; i++) {
                LivingEntity mob = companions.get(i + scrollOffset);
                int ey = listT + i * ENTRY_H;

                // Entry background (highlight if hovered)
                boolean hovered = mx >= listL && mx <= listL + entryW - 22
                        && my >= ey && my <= ey + ENTRY_H - 2;
                g.fill(listL, ey, listL + entryW - 22, ey + ENTRY_H - 2, hovered ? ENTRY_HOV : ENTRY_BG);

                // Mode icon + name
                BehaviorMode mode = ClientFriendshipStore.getBehavior(mob.getUUID());
                String icon = switch (mode) {
                    case ATTACK  -> "[!]";
                    case PROTECT -> "[+]";
                    case STAY    -> "[..]";
                    case FOLLOW  -> "[>>]";
                };
                String nick = ClientFriendshipStore.getNickname(mob.getUUID());
                String name = nick.isEmpty() ? mob.getType().getDescription().getString() : nick;
                g.drawString(font, icon + " " + name, listL + 4, ey + 4, hovered ? TEXT_H : TEXT_S, false);

                // Distance
                Minecraft mc = Minecraft.getInstance();
                int dist = (int) mob.distanceTo(mc.player);
                g.drawString(font, dist + "m", listL + 4, ey + 14, TEXT_D, false);

                // Mini HP bar
                float ratio = mob.getHealth() / mob.getMaxHealth();
                int barL = listL + entryW - 80 - 24, barT = ey + 6, barW2 = 66, barH2 = 7;
                g.fill(barL, barT, barL + barW2, barT + barH2, 0xFF111828);
                int fill = (int)(barW2 * ratio);
                int hpCol = ratio > 0.5f ? HP_HI : ratio > 0.25f ? HP_MID : HP_LO;
                if (fill > 0) g.fill(barL, barT, barL + fill, barT + barH2, hpCol);

                // Arrow button area
                g.fill(listL + entryW - 20, ey, listL + entryW - 2, ey + ENTRY_H - 2, ENTRY_BG);
                g.drawCenteredString(font, ">", listL + entryW - 11, ey + 8, BORDER_HI);
            }
        }

        // Scroll indicators
        if (scrollOffset > 0)
            g.drawCenteredString(font, "^", L + W / 2, listT - 10, TEXT_D);
        if (scrollOffset + LIST_CAP < companions.size())
            g.drawCenteredString(font, "v", L + W / 2, listT + LIST_CAP * ENTRY_H + 2, TEXT_D);

        // Footer
        g.fill(L + 4, T + H - 18, L + W - 1, T + H - 17, BORDER_LO);
        String countTxt = companions.isEmpty()
                ? "No companions in range"
                : companions.size() + " companion" + (companions.size() == 1 ? "" : "s") + " loaded";
        g.drawCenteredString(font, countTxt, L + W / 2, T + H - 12, TEXT_D);

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int L = (width - W) / 2, T = (height - H) / 2;
        int listL = L + 6, listT = T + 30, entryW = W - 12;

        for (int i = 0; i < Math.min(LIST_CAP, companions.size() - scrollOffset); i++) {
            int ey = listT + i * ENTRY_H;
            if (mx >= listL && mx <= listL + entryW - 2 && my >= ey && my <= ey + ENTRY_H - 2) {
                LivingEntity mob = companions.get(i + scrollOffset);
                onClose();
                Minecraft.getInstance().setScreen(new CompanionScreen(mob,
                        ClientFriendshipStore.isFriend(mob.getUUID()),
                        ClientFriendshipStore.getBehavior(mob.getUUID()),
                        ClientFriendshipStore.getNickname(mob.getUUID())));
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        int max = Math.max(0, companions.size() - LIST_CAP);
        scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - amount));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
