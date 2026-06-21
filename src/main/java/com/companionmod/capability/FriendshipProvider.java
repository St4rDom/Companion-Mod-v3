package com.companionmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FriendshipProvider implements ICapabilitySerializable<CompoundTag> {

    private final FriendshipData data = new FriendshipData();
    private final LazyOptional<IFriendshipData> lazy = LazyOptional.of(() -> data);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == FriendshipCapability.INSTANCE ? lazy.cast() : LazyOptional.empty();
    }

    @Override public CompoundTag serializeNBT()            { return data.serializeNBT(); }
    @Override public void deserializeNBT(CompoundTag nbt)  { data.deserializeNBT(nbt); }
    public void invalidate()                               { lazy.invalidate(); }
}
