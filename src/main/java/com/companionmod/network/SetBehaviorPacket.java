package com.companionmod.network;

import com.companionmod.BehaviorMode;
import com.companionmod.capability.FriendshipCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SetBehaviorPacket {

    private final int entityId;
    private final BehaviorMode mode;

    public SetBehaviorPacket(int entityId, BehaviorMode mode) {
        this.entityId = entityId;
        this.mode = mode;
    }

    public static void encode(SetBehaviorPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.entityId);
        buf.writeEnum(p.mode);
    }

    public static SetBehaviorPacket decode(FriendlyByteBuf buf) {
        return new SetBehaviorPacket(buf.readInt(), buf.readEnum(BehaviorMode.class));
    }

    public static void handle(SetBehaviorPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            var entity = player.level().getEntity(pkt.entityId);
            if (!(entity instanceof LivingEntity living)) return;

            living.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
                if (!data.isFriendOf(player.getUUID())) return;
                data.setBehaviorMode(player.getUUID(), pkt.mode);

                // Immediate effect for STAY: halt movement
                if (pkt.mode == BehaviorMode.STAY && living instanceof Mob mob) {
                    mob.getNavigation().stop();
                    mob.setTarget(null);
                }

                PacketHandler.sendToPlayer(SyncFriendshipPacket.forEntity(living, player.getUUID()), player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
