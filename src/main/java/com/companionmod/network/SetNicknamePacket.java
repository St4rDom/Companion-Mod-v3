package com.companionmod.network;

import com.companionmod.capability.FriendshipCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SetNicknamePacket {
    private final int entityId;
    private final String nickname;

    public SetNicknamePacket(int entityId, String nickname) {
        this.entityId = entityId; this.nickname = nickname;
    }

    public static void encode(SetNicknamePacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.entityId); buf.writeUtf(p.nickname, 64);
    }
    public static SetNicknamePacket decode(FriendlyByteBuf buf) {
        return new SetNicknamePacket(buf.readInt(), buf.readUtf(64));
    }

    public static void handle(SetNicknamePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var entity = player.level().getEntity(pkt.entityId);
            if (!(entity instanceof LivingEntity living)) return;
            living.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
                if (!data.isFriendOf(player.getUUID())) return;
                data.setNickname(pkt.nickname);
                PacketHandler.sendToPlayer(SyncFriendshipPacket.forEntity(living, player.getUUID()), player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
