package com.companionmod.network;

import com.companionmod.CompanionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;

public class PacketHandler {
    private static final String PROTOCOL = "3";
    private static int id = 0;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CompanionMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(id++, SetFriendPacket.class,    SetFriendPacket::encode,    SetFriendPacket::decode,    SetFriendPacket::handle,    Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, SetBehaviorPacket.class,  SetBehaviorPacket::encode,  SetBehaviorPacket::decode,  SetBehaviorPacket::handle,  Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, OpenInventoryPacket.class,OpenInventoryPacket::encode,OpenInventoryPacket::decode,OpenInventoryPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, SetNicknamePacket.class,  SetNicknamePacket::encode,  SetNicknamePacket::decode,  SetNicknamePacket::handle,  Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, MobActionPacket.class,    MobActionPacket::encode,    MobActionPacket::decode,    MobActionPacket::handle,    Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, OpenArmorPacket.class,    OpenArmorPacket::encode,    OpenArmorPacket::decode,    OpenArmorPacket::handle,    Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, RiderInputPacket.class,   RiderInputPacket::encode,   RiderInputPacket::decode,   RiderInputPacket::handle,   Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, BringAllPacket.class,     BringAllPacket::encode,     BringAllPacket::decode,     BringAllPacket::handle,     Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, SyncFriendshipPacket.class, SyncFriendshipPacket::encode, SyncFriendshipPacket::decode, SyncFriendshipPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToServer(Object pkt) { CHANNEL.sendToServer(pkt); }
    public static void sendToPlayer(Object pkt, ServerPlayer p) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
    }
}
