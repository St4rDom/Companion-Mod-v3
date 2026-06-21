package com.companionmod.client;

import com.companionmod.CompanionMod;
import com.companionmod.gui.CompanionArmorScreen;
import com.companionmod.gui.CompanionInventoryScreen;
import com.companionmod.gui.MenuRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CompanionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MenuRegistry.COMPANION_MENU.get(),       CompanionInventoryScreen::new);
            MenuScreens.register(MenuRegistry.COMPANION_ARMOR_MENU.get(), CompanionArmorScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CompanionKeys.OPEN_MANAGER);
    }
}
