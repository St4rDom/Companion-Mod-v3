package com.companionmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class CompanionKeys {
    public static final KeyMapping OPEN_MANAGER = new KeyMapping(
            "key.companionmod.manager",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SEMICOLON,
            "key.categories.companionmod"
    );
}
