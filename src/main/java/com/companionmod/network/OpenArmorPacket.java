package com.companionmod.network;

import com.companionmod.capability.FriendshipCapability;
import com.companionmod.gui.CompanionArmorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import java.util.function.Supplier;

public class OpenArmorPacket {
    private final int entityId;
    public OpenArmorPacket(int entityId) { this.entityId = entityId; }

    public static void encode(OpenArmorPacket p, FriendlyByteBuf buf) { buf.writeInt(p.entityId); }
    public static OpenArmorPacket decode(FriendlyByteBuf buf) { return new OpenArmorPacket(buf.readInt()); }

    public static void handle(OpenArmorPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var entity = player.level().getEntity(pkt.entityId);
            if (!(entity instanceof LivingEntity living)) return;
            boolean[] ok = {false};
            living.getCapability(FriendshipCapability.INSTANCE)
                  .ifPresent(d -> ok[0] = d.isFriendOf(player.getUUID()));
            if (!ok[0]) return;
            int eid = pkt.entityId;
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override public Component getDisplayName() {
                    String nick = "";
                    var cap = living.getCapability(FriendshipCapability.INSTANCE);
                    if (cap.isPresent()) nick = cap.resolve().get().getNickname();
                    String name = nick.isEmpty() ? living.getType().getDescription().getString() : nick;
                    return Component.literal(name + "'s Equipment");
                }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new CompanionArmorMenu(id, inv, living);
                }
            }, buf -> buf.writeInt(eid));
        });
        ctx.get().setPacketHandled(true);
    }
}
