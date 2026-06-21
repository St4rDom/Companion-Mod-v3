package com.companionmod.network;

import com.companionmod.event.EntityEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Sent every client tick while the player is riding a companion. Provides WASD input. */
public class RiderInputPacket {
    public final int   vehicleId;
    public final float forward, strafe, yaw, pitch;

    public RiderInputPacket(int vehicleId, float forward, float strafe, float yaw, float pitch) {
        this.vehicleId = vehicleId;
        this.forward = forward; this.strafe = strafe;
        this.yaw = yaw; this.pitch = pitch;
    }

    public static void encode(RiderInputPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.vehicleId);
        buf.writeFloat(p.forward); buf.writeFloat(p.strafe);
        buf.writeFloat(p.yaw);    buf.writeFloat(p.pitch);
    }
    public static RiderInputPacket decode(FriendlyByteBuf buf) {
        return new RiderInputPacket(buf.readInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
    public static void handle(RiderInputPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
                EntityEventHandler.setRiderInput(player.getUUID(),
                        new float[]{pkt.forward, pkt.strafe, pkt.yaw, pkt.pitch});
        });
        ctx.get().setPacketHandled(true);
    }
}
