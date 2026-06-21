package com.companionmod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 6-slot equipment container (Head, Chest, Legs, Feet, MainHand, OffHand) + player inventory.
 * A SimpleContainer listener keeps the mob's actual equipment in sync every time a slot changes.
 */
public class CompanionArmorMenu extends AbstractContainerMenu {

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
        EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private final LivingEntity    mob;
    private final SimpleContainer armorContainer;

    // Server-side
    public CompanionArmorMenu(int id, Inventory playerInv, LivingEntity mob) {
        super(MenuRegistry.COMPANION_ARMOR_MENU.get(), id);
        this.mob = mob;
        armorContainer = new SimpleContainer(6);
        if (mob != null) {
            for (int i = 0; i < SLOTS.length; i++)
                armorContainer.setItem(i, mob.getItemBySlot(SLOTS[i]).copy());
            // Sync changes back to the mob
            armorContainer.addListener(c -> {
                for (int i = 0; i < SLOTS.length; i++)
                    mob.setItemSlot(SLOTS[i], c.getItem(i).copy());
            });
        }
        addSlots(playerInv);
    }

    // Client-side
    public CompanionArmorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(MenuRegistry.COMPANION_ARMOR_MENU.get(), id);
        int entityId = buf.readInt();
        LivingEntity found = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) { var e = mc.level.getEntity(entityId); if (e instanceof LivingEntity le) found = le; }
        this.mob = found;
        armorContainer = new SimpleContainer(6); // synced via slot packets
        addSlots(playerInv);
    }

    private void addSlots(Inventory playerInv) {
        // Equipment slots — centered in 176px panel
        // 6 slots x 18px = 108; offset = (176-108)/2 = 34
        for (int i = 0; i < 6; i++)
            addSlot(new Slot(armorContainer, i, 34 + i * 18, 20));

        // Player inventory
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 58 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 116));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < 6) {
            if (!moveItemStackTo(stack, 6, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 6, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public boolean stillValid(Player player) {
        if (mob == null) return true;
        return mob.isAlive() && player.distanceTo(mob) < 10.0;
    }
}
