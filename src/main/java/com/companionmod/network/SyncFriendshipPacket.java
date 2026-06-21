package com.companionmod.network;

import com.companionmod.BehaviorMode;
import com.companionmod.capability.FriendshipCapability;
import com.companionmod.client.ClientFriendshipStore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncFriendshipPacket {

    private final UUID mobUUID, playerUUID;
    private final boolean isFriend;
    private final BehaviorMode mode;
    private final String nickname;

    public SyncFriendshipPacket(UUID mobUUID, UUID playerUUID, boolean isFriend, BehaviorMode mode, String nickname) {
        this.mobUUID = mobUUID; this.playerUUID = playerUUID;
        this.isFriend = isFriend; this.mode = mode; this.nickname = nickname;
    }

    public static SyncFriendshipPacket forEntity(LivingEntity e, UUID playerUUID) {
        boolean[] f = {false}; BehaviorMode[] m = {BehaviorMode.FOLLOW}; String[] n = {""};
        e.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
            f[0] = data.isFriendOf(playerUUID);
            m[0] = data.getBehaviorMode(playerUUID);
            n[0] = data.getNickname();
        });
        return new SyncFriendshipPacket(e.getUUID(), playerUUID, f[0], m[0], n[0]);
    }

    public static void encode(SyncFriendshipPacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.mobUUID); buf.writeUUID(p.playerUUID);
        buf.writeBoolean(p.isFriend); buf.writeEnum(p.mode);
        buf.writeUtf(p.nickname, 64);
    }

    public static SyncFriendshipPacket decode(FriendlyByteBuf buf) {
        return new SyncFriendshipPacket(buf.readUUID(), buf.readUUID(),
                buf.readBoolean(), buf.readEnum(BehaviorMode.class), buf.readUtf(64));
    }

    public static void handle(SyncFriendshipPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !mc.player.getUUID().equals(pkt.playerUUID)) return;
            ClientFriendshipStore.setFriend(pkt.mobUUID, pkt.isFriend);
            if (pkt.isFriend) {
                ClientFriendshipStore.setBehavior(pkt.mobUUID, pkt.mode);
                ClientFriendshipStore.setNickname(pkt.mobUUID, pkt.nickname);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
