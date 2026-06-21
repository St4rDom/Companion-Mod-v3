package com.companionmod.event;

import com.companionmod.BehaviorMode;
import com.companionmod.CompanionMod;
import com.companionmod.client.ClientFriendshipStore;
import com.companionmod.client.CompanionKeys;
import com.companionmod.gui.CompanionManagerScreen;
import com.companionmod.gui.CompanionScreen;
import com.companionmod.network.PacketHandler;
import com.companionmod.network.RiderInputPacket;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import java.util.List;

@Mod.EventBusSubscriber(modid = CompanionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    private static final float RANGE = 6.0f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Open companion manager on ; key
        if (mc.screen == null && CompanionKeys.OPEN_MANAGER.consumeClick()) {
            mc.setScreen(new CompanionManagerScreen());
        }

        // Send rider input while mounted on a companion
        if (mc.player.isPassenger() && mc.player.getVehicle() instanceof LivingEntity vehicle) {
            if (ClientFriendshipStore.isFriend(vehicle.getUUID())) {
                long win = mc.getWindow().getWindow();
                float fwd = 0, str = 0;
                if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_W)) fwd += 1.0f;
                if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_S)) fwd -= 1.0f;
                if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_A)) str -= 1.0f;
                if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_D)) str += 1.0f;
                PacketHandler.sendToServer(new RiderInputPacket(
                        vehicle.getId(), fwd, str,
                        mc.player.getYRot(), mc.player.getXRot()));
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity living)) return;
        if (living instanceof Player) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.distanceTo(living) > RANGE) return;

        mc.setScreen(new CompanionScreen(living,
                ClientFriendshipStore.isFriend(living.getUUID()),
                ClientFriendshipStore.getBehavior(living.getUUID()),
                ClientFriendshipStore.getNickname(living.getUUID())));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack ps = event.getPoseStack();
        List<LivingEntity> entities = mc.level.getEntitiesOfClass(
                LivingEntity.class, mc.player.getBoundingBox().inflate(RANGE));
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        for (LivingEntity entity : entities) {
            if (entity instanceof Player) continue;
            if (entity.distanceTo(mc.player) > RANGE) continue;

            boolean isFriend  = ClientFriendshipStore.isFriend(entity.getUUID());
            String  nickname  = ClientFriendshipStore.getNickname(entity.getUUID());
            BehaviorMode mode = ClientFriendshipStore.getBehavior(entity.getUUID());

            String label; int textColor, bgColor;
            if (isFriend) {
                String icon = switch (mode) {
                    case ATTACK  -> "[!]";
                    case PROTECT -> "[+]";
                    case STAY    -> "[..]";
                    case FOLLOW  -> "[>>]";
                };
                label     = icon + " " + (nickname.isEmpty() ? modeName(mode) : nickname);
                textColor = 0xFF66FF99;
                bgColor   = 0xA000882A;
            } else {
                label     = "[  ...  ]";
                textColor = 0xFFCCDDFF;
                bgColor   = 0x88050510;
            }

            double ex = entity.getX() - camPos.x;
            double ey = entity.getY() - camPos.y + entity.getBbHeight() + 0.55;
            double ez = entity.getZ() - camPos.z;

            ps.pushPose();
            ps.translate(ex, ey, ez);
            ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            ps.scale(-0.025f, -0.025f, 0.025f);
            float tw = font.width(label);
            Matrix4f matrix = ps.last().pose();
            font.drawInBatch(label, -tw / 2f, 0f, textColor, false,
                    matrix, buf, Font.DisplayMode.NORMAL, bgColor, LightTexture.FULL_BRIGHT);
            ps.popPose();
        }
        buf.endBatch();
    }

    private static String modeName(BehaviorMode mode) {
        return switch (mode) {
            case ATTACK  -> "Attack";
            case PROTECT -> "Protect";
            case STAY    -> "Stay";
            case FOLLOW  -> "Follow";
        };
    }
}
