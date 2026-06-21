package com.companionmod.network;

import com.companionmod.capability.FriendshipCapability;
import com.companionmod.gui.CompanionMenu;
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

public class OpenInventoryPacket {
    private final int entityId;
    public OpenInventoryPacket(int entityId) { this.entityId = entityId; }
    public static void encode(OpenInventoryPacket p, FriendlyByteBuf buf) { buf.writeInt(p.entityId); }
    public static OpenInventoryPacket decode(FriendlyByteBuf buf) { return new OpenInventoryPacket(buf.readInt()); }

    public static void handle(OpenInventoryPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var entity = player.level().getEntity(pkt.entityId);
            if (!(entity instanceof LivingEntity living)) return;
            boolean[] ok = {false};
            living.getCapability(FriendshipCapability.INSTANCE).ifPresent(d -> ok[0] = d.isFriendOf(player.getUUID()));
            if (!ok[0]) return;
            int eid = pkt.entityId;
            // Calculate rows server-side so client menu matches
            int rows = Math.min(Math.max((int)(living.getMaxHealth() / 10) + 1, 1), 5);
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override public Component getDisplayName() {
                    return Component.translatable("container.companionmod.companion_bag",
                            living.getType().getDescription().getString());
                }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new CompanionMenu(id, inv, living);
                }
            }, buf -> { buf.writeInt(eid); buf.writeInt(rows); });
        });
        ctx.get().setPacketHandled(true);
    }
}
