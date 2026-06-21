package com.companionmod.gui;

import com.companionmod.capability.FriendshipCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Mob inventory container.
 * Rows shown = clamp(floor(maxHealth / 10) + 1, 1, 5).
 * The underlying SimpleContainer always has 45 slots so no items are lost
 * if health changes; extra slots are just not displayed.
 */
public class CompanionMenu extends AbstractContainerMenu {

    private final SimpleContainer mobInventory;
    private final LivingEntity    mob;
    private final int             mobRows;

    // Server-side
    public CompanionMenu(int id, Inventory playerInv, LivingEntity mob) {
        super(MenuRegistry.COMPANION_MENU.get(), id);
        this.mob     = mob;
        this.mobRows = calcRows(mob);
        SimpleContainer[] c = {new SimpleContainer(45)};
        if (mob != null)
            mob.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> c[0] = data.getMobInventory());
        this.mobInventory = c[0];
        addSlots(playerInv);
    }

    // Client-side
    public CompanionMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(MenuRegistry.COMPANION_MENU.get(), id);
        int entityId  = buf.readInt();
        this.mobRows  = buf.readInt();
        LivingEntity found = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) { var e = mc.level.getEntity(entityId); if (e instanceof LivingEntity le) found = le; }
        this.mob          = found;
        this.mobInventory = new SimpleContainer(45);
        addSlots(playerInv);
    }

    private static int calcRows(LivingEntity mob) {
        if (mob == null) return 1;
        return Math.min(Math.max((int)(mob.getMaxHealth() / 10) + 1, 1), 5);
    }

    public int getMobRows() { return mobRows; }

    private void addSlots(Inventory playerInv) {
        // Mob inventory (up to 5 rows)
        for (int row = 0; row < mobRows; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(mobInventory, col + row * 9, 8 + col * 18, 18 + row * 18));

        int playerY = 18 + mobRows * 18 + 14;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, playerY + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, playerY + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int mobSlots = mobRows * 9;
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < mobSlots) {
            if (!moveItemStackTo(stack, mobSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, mobSlots, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public boolean stillValid(Player player) {
        if (mob == null) return true;
        return mob.isAlive() && player.distanceTo(mob) < 10.0;
    }
}
