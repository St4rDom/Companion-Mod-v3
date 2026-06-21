package com.companionmod.gui;

import com.companionmod.CompanionMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuRegistry {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CompanionMod.MOD_ID);

    public static final RegistryObject<MenuType<CompanionMenu>> COMPANION_MENU =
            MENUS.register("companion_menu",
                    () -> IForgeMenuType.create(CompanionMenu::new));

    public static final RegistryObject<MenuType<CompanionArmorMenu>> COMPANION_ARMOR_MENU =
            MENUS.register("companion_armor_menu",
                    () -> IForgeMenuType.create(CompanionArmorMenu::new));
}
