package com.companionmod.network;

import com.companionmod.capability.FriendshipCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Teleports ALL loaded companion mobs to the player's position. */
public class BringAllPacket {
    public static void encode(BringAllPacket p, FriendlyByteBuf buf) {}
    public static BringAllPacket decode(FriendlyByteBuf buf) { return new BringAllPacket(); }

    public static void handle(BringAllPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.level() instanceof ServerLevel sl)) return;
            sl.getAllEntities().forEach(entity -> {
                if (!(entity instanceof LivingEntity living) || living instanceof Player) return;
                living.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
                    if (data.isFriendOf(player.getUUID()))
                        living.teleportTo(player.getX(), player.getY(), player.getZ());
                });
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
