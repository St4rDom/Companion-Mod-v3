package com.companionmod;

import com.companionmod.capability.FriendshipCapability;
import com.companionmod.gui.MenuRegistry;
import com.companionmod.network.PacketHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CompanionMod.MOD_ID)
public class CompanionMod {

    public static final String MOD_ID = "companionmod";

    public CompanionMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(FriendshipCapability::register);
        MenuRegistry.MENUS.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }
}
