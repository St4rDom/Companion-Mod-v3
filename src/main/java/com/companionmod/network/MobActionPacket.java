package com.companionmod.network;

import com.companionmod.capability.FriendshipCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Single packet: Teleport mob here / Go to mob / Mount / Dismount. */
public class MobActionPacket {

    public enum Action { TELEPORT_MOB_HERE, GO_TO_MOB, MOUNT, DISMOUNT }

    private final int    entityId;
    private final Action action;

    public MobActionPacket(int entityId, Action action) {
        this.entityId = entityId; this.action = action;
    }

    public static void encode(MobActionPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.entityId); buf.writeEnum(p.action);
    }
    public static MobActionPacket decode(FriendlyByteBuf buf) {
        return new MobActionPacket(buf.readInt(), buf.readEnum(Action.class));
    }

    public static void handle(MobActionPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Entity entity = player.level().getEntity(pkt.entityId);
            if (!(entity instanceof LivingEntity living)) return;

            boolean[] ok = {false};
            living.getCapability(FriendshipCapability.INSTANCE)
                  .ifPresent(d -> ok[0] = d.isFriendOf(player.getUUID()));
            if (!ok[0]) return;

            switch (pkt.action) {
                case TELEPORT_MOB_HERE ->
                    // Teleport mob to player (same dimension only — mobs can't cross dimensions)
                    living.teleportTo(player.getX(), player.getY(), player.getZ());

                case GO_TO_MOB -> {
                    // Player teleports to mob; handles cross-dimension
                    if (living.level() instanceof ServerLevel targetLevel) {
                        if (player.level() == targetLevel) {
                            player.teleportTo(living.getX(), living.getY(), living.getZ());
                        } else {
                            // Cross-dimension: use ServerPlayer#teleportTo overload
                            player.teleportTo(targetLevel,
                                    living.getX(), living.getY(), living.getZ(),
                                    player.getYRot(), player.getXRot());
                        }
                    }
                }

                case MOUNT -> {
                    if (player.isPassenger()) player.stopRiding();
                    player.startRiding(living, true);
                }

                case DISMOUNT -> player.stopRiding();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
