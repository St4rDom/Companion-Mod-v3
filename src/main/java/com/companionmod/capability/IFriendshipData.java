package com.companionmod.capability;

import com.companionmod.BehaviorMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import java.util.UUID;

public interface IFriendshipData {
    boolean isFriendOf(UUID playerUUID);
    boolean hasAnyFriend();
    void addFriend(UUID playerUUID);
    void removeFriend(UUID playerUUID);
    BehaviorMode getBehaviorMode(UUID playerUUID);
    void setBehaviorMode(UUID playerUUID, BehaviorMode mode);
    String getNickname();
    void setNickname(String nickname);
    SimpleContainer getMobInventory();
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}
