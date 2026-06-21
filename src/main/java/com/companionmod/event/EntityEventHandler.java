package com.companionmod.event;

import com.companionmod.BehaviorMode;
import com.companionmod.CompanionMod;
import com.companionmod.capability.FriendshipCapability;
import com.companionmod.capability.IFriendshipData;
import com.companionmod.capability.FriendshipProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = CompanionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityEventHandler {

    // ── Rider input storage (filled by RiderInputPacket) ─────────────────────
    private static final Map<UUID, float[]> riderInputs  = new ConcurrentHashMap<>();
    // ── Auto-teleport: track last teleport time per mob ──────────────────────
    private static final Map<UUID, Long>    lastTeleport = new ConcurrentHashMap<>();

    public static void setRiderInput(UUID playerUUID, float[] input) {
        riderInputs.put(playerUUID, input);
    }

    // ── Capability attachment ─────────────────────────────────────────────────
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof LivingEntity)) return;
        if (event.getObject() instanceof Player) return;
        FriendshipProvider provider = new FriendshipProvider();
        event.addCapability(new ResourceLocation(CompanionMod.MOD_ID, "friendship"), provider);
        event.addListener(provider::invalidate);
    }

    // ── Never target a friend ─────────────────────────────────────────────────
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof Player player)) return;
        event.getEntity().getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
            if (data.isFriendOf(player.getUUID())) event.setCanceled(true);
        });
    }

    // ── Block damage / trigger protect / redirect attack companions ───────────
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity src = event.getSource().getEntity();
        if (event.getEntity() instanceof Player player
                && src instanceof LivingEntity atk && !(src instanceof Player)) {
            atk.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
                if (data.isFriendOf(player.getUUID())) event.setCanceled(true);
            });
            if (!event.isCanceled() && !player.level().isClientSide())
                triggerProtect(player, (LivingEntity) src);
        }
        // When player hits a mob, all ATTACK-mode companions also target it
        if (src instanceof Player attacker && !(event.getEntity() instanceof Player)
                && !attacker.level().isClientSide()) {
            redirectAttackCompanions(attacker, event.getEntity());
        }
    }

    private static void triggerProtect(Player player, LivingEntity attacker) {
        player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24.0))
            .forEach(mob -> { if (!(mob instanceof Mob m)) return;
                mob.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
                    if (data.isFriendOf(player.getUUID())
                            && data.getBehaviorMode(player.getUUID()) == BehaviorMode.PROTECT)
                        m.setTarget(attacker);
                });
            });
    }

    private static void redirectAttackCompanions(Player player, LivingEntity target) {
        player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32.0))
            .forEach(mob -> { if (!(mob instanceof Mob m) || mob == target) return;
                mob.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
                    if (data.isFriendOf(player.getUUID())
                            && data.getBehaviorMode(player.getUUID()) == BehaviorMode.ATTACK)
                        m.setTarget(target);
                });
            });
    }

    // ── Main behaviour tick (every 5 game ticks = 4x/sec) ────────────────────
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob mob)) return;
        if (entity.tickCount % 5 != 0) return;

        mob.getCapability(FriendshipCapability.INSTANCE).ifPresent(data -> {
            if (!data.hasAnyFriend()) return;

            // Sun-burn protection for undead friends
            if (mob.getMobType() == MobType.UNDEAD && mob.isOnFire()
                    && mob.level().dimension().equals(Level.OVERWORLD)
                    && mob.level().isDay()) {
                mob.clearFire();
            }

            // If player is riding this mob, delegate to rider control
            if (mob.getFirstPassenger() instanceof Player rider
                    && data.isFriendOf(rider.getUUID())) {
                applyRiderControl(mob, rider);
                return;
            }

            Player friend = findClosestFriend(mob, data, 32.0);
            if (friend == null) return;
            BehaviorMode mode = data.getBehaviorMode(friend.getUUID());

            // Speed = 75% of player's current movement speed
            double playerSpd = friend.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double mobBase   = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double speedMult = mobBase > 0 ? Math.min((playerSpd * 0.75) / mobBase, 3.0) : 1.0;

            switch (mode) {
                case FOLLOW  -> doFollow(mob, friend, speedMult);
                case STAY    -> { mob.getNavigation().stop(); mob.setTarget(null); }
                case ATTACK  -> {
                    doFollow(mob, friend, speedMult);
                    if (mob.getTarget() != null && !mob.getTarget().isAlive()) mob.setTarget(null);
                }
                case PROTECT -> {
                    doFollow(mob, friend, speedMult);
                    if (mob.getTarget() != null && !mob.getTarget().isAlive()) mob.setTarget(null);
                }
            }
        });
    }

    /**
     * Follow the player, auto-teleporting if the mob is stuck more than 10 blocks away.
     * Teleport is throttled to once per second to avoid constant flickering.
     */
    private static void doFollow(Mob mob, Player friend, double speedMult) {
        double dist = mob.distanceTo(friend);
        if (dist > 10.0) {
            long now      = mob.level().getGameTime();
            long lastTp   = lastTeleport.getOrDefault(mob.getUUID(), 0L);
            if (now - lastTp > 20) { // max 1 teleport per second
                mob.teleportTo(friend.getX(), friend.getY(), friend.getZ());
                lastTeleport.put(mob.getUUID(), now);
                mob.getNavigation().stop();
            } else {
                mob.getNavigation().moveTo(friend, speedMult);
            }
        } else if (dist > 3.5) {
            mob.getNavigation().moveTo(friend, speedMult);
        } else {
            mob.getNavigation().stop();
        }
    }

    /** Apply rider's WASD input to the mob, overriding its AI movement. */
    private static void applyRiderControl(Mob mob, Player rider) {
        float[] input = riderInputs.get(rider.getUUID());
        mob.getNavigation().stop();
        mob.setTarget(null);

        if (input == null) return;
        float forward = input[0], strafe = input[1], yaw = input[2];

        // Orient mob to match player look direction
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
        mob.yRotO    = yaw;

        if (Math.abs(forward) > 0.01f || Math.abs(strafe) > 0.01f) {
            double spd = mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.8;
            double rad = Math.toRadians(yaw);
            double dx  = (-Math.sin(rad) * forward - Math.cos(rad) * strafe) * spd;
            double dz  = ( Math.cos(rad) * forward - Math.sin(rad) * strafe) * spd;
            mob.setDeltaMovement(dx, mob.getDeltaMovement().y, dz);
            mob.hasImpulse = true;
        } else {
            mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
        }
    }

    private static Player findClosestFriend(Mob mob, IFriendshipData data, double range) {
        Player closest = null; double min = Double.MAX_VALUE;
        for (Player p : mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(range))) {
            if (!data.isFriendOf(p.getUUID())) continue;
            double d = mob.distanceToSqr(p);
            if (d < min) { min = d; closest = p; }
        }
        return closest;
    }
}
