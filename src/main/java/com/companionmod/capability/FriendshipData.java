package com.companionmod.capability;

import com.companionmod.BehaviorMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public class FriendshipData implements IFriendshipData {

    private final Set<UUID>            friends    = new HashSet<>();
    private final Map<UUID,BehaviorMode> behaviors = new HashMap<>();
    private String                     nickname   = "";
    // 45 slots max (5 rows); CompanionMenu shows only as many rows as the mob's health warrants
    private final SimpleContainer      inventory  = new SimpleContainer(45);

    @Override public boolean isFriendOf(UUID id)  { return friends.contains(id); }
    @Override public boolean hasAnyFriend()        { return !friends.isEmpty(); }
    @Override public String  getNickname()         { return nickname; }
    @Override public void    setNickname(String n) { this.nickname = n == null ? "" : n; }
    @Override public SimpleContainer getMobInventory() { return inventory; }

    @Override
    public void addFriend(UUID id) {
        friends.add(id);
        behaviors.putIfAbsent(id, BehaviorMode.FOLLOW);
    }

    @Override
    public void removeFriend(UUID id) {
        friends.remove(id);
        behaviors.remove(id);
    }

    @Override
    public BehaviorMode getBehaviorMode(UUID id) {
        return behaviors.getOrDefault(id, BehaviorMode.FOLLOW);
    }

    @Override
    public void setBehaviorMode(UUID id, BehaviorMode mode) {
        if (friends.contains(id)) behaviors.put(id, mode);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("nickname", nickname);

        ListTag fl = new ListTag();
        for (UUID uuid : friends) {
            CompoundTag e = new CompoundTag();
            e.putUUID("uuid", uuid);
            e.putString("behavior", behaviors.getOrDefault(uuid, BehaviorMode.FOLLOW).name());
            fl.add(e);
        }
        tag.put("friends", fl);

        ListTag il = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag it = new CompoundTag();
                it.putByte("Slot", (byte) i);
                s.save(it);
                il.add(it);
            }
        }
        tag.put("inv", il);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        nickname = nbt.getString("nickname");
        friends.clear(); behaviors.clear();
        if (nbt.contains("friends", Tag.TAG_LIST)) {
            ListTag fl = nbt.getList("friends", Tag.TAG_COMPOUND);
            for (int i = 0; i < fl.size(); i++) {
                CompoundTag e = fl.getCompound(i);
                UUID uuid = e.getUUID("uuid");
                BehaviorMode m;
                try { m = BehaviorMode.valueOf(e.getString("behavior")); }
                catch (Exception ex) { m = BehaviorMode.FOLLOW; }
                friends.add(uuid); behaviors.put(uuid, m);
            }
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) inventory.setItem(i, ItemStack.EMPTY);
        if (nbt.contains("inv", Tag.TAG_LIST)) {
            ListTag il = nbt.getList("inv", Tag.TAG_COMPOUND);
            for (int i = 0; i < il.size(); i++) {
                CompoundTag it = il.getCompound(i);
                int slot = it.getByte("Slot") & 0xFF;
                if (slot < inventory.getContainerSize())
                    inventory.setItem(slot, ItemStack.of(it));
            }
        }
    }
}
