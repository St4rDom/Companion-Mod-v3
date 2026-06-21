package com.companionmod.network;

import com.companionmod.capability.FriendshipCapability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SetFriendPacket {
    private final int entityId;
    private final boolean befriend;

    public SetFriendPacket(int entityId, boolean befriend) {
        this.entityId = entityId; this.befriend = befriend;
    }
    public static void encode(SetFriendPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.entityId); buf.writeBoolean(p.befriend);
    }
    public static SetFriendPacket decode(FriendlyByteBuf buf) {
        return new SetFriendPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(SetFriendPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var entity = player.level().getEntity(pkt.entityId);
            if (!(entity instanceof LivingEntity living)) return;
            if (living instanceof Player) return;

            living.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
                if (pkt.befriend) {
                    data.addFriend(player.getUUID());
                    if (living instanceof Mob mob) {
                        if (mob.getTarget() instanceof Player p && p.getUUID().equals(player.getUUID()))
                            mob.setTarget(null);
                        mob.setPersistenceRequired();
                    }
                    // Heart particles to celebrate the new friendship
                    if (living.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART,
                                living.getX(), living.getY() + living.getBbHeight() + 0.3,
                                living.getZ(), 7, 0.4, 0.3, 0.4, 0.05);
                    }
                } else {
                    data.removeFriend(player.getUUID());
                }
                PacketHandler.sendToPlayer(SyncFriendshipPacket.forEntity(living, player.getUUID()), player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
