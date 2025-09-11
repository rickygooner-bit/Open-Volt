package com.volt.module.modules.combat;

import com.volt.event.impl.render.EventRender3D;

import com.volt.module.Category;
import com.volt.module.Module;
import com.volt.module.modules.misc.Teams;
import com.volt.module.setting.BooleanSetting;
import com.volt.module.setting.ModeSetting;
import com.volt.module.setting.NumberSetting;
import com.volt.utils.friend.FriendManager;


import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;


public class AimAssist extends Module {
    private static final ModeSetting mode = new ModeSetting("Aim Mode", "Distance","Distance", "Health");
    private final NumberSetting speed = new NumberSetting("Speed", 1.0, 10.0, 5.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 10.0, 180.0, 90.0, 1.0);
    private final NumberSetting range = new NumberSetting("Range", 1.0, 10.0, 4.5, 0.1);
    private final NumberSetting pitchSpeed = new NumberSetting("Pitch Speed", 0.1, 5.0, 2.0, 0.1);
    private final NumberSetting yawSpeed = new NumberSetting("Yaw Speed", 0.1, 5.0, 2.0, 0.1);
    private final NumberSetting smoothing = new NumberSetting("Smoothing", 1.0, 20.0, 10.0, 0.5);
    private final NumberSetting steepNess = new NumberSetting("Steepness", 1.0, 10.0, 5.0, 0.5);
    private final NumberSetting noiseSpeed = new NumberSetting("Noise Speed", 0.5, 5.0, 1.5, 0.5);
    private final BooleanSetting targetPlayers = new BooleanSetting("Target Players", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Target Mobs", false);
    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private final BooleanSetting disableOnTarget  = new BooleanSetting("Disable on target", false);
    private final BooleanSetting workOnlyWhileRMBPressed = new BooleanSetting("Work only when RMB is pressed", false);

    private Entity currentTarget = null;
    private long lastUpdateTime = 0;
    float noiseTime = 0f;

    public AimAssist() {
        super("Aim Assist", "Helps you with aiming", Category.COMBAT);
        addSettings(mode, speed, fov, range, pitchSpeed, yawSpeed, smoothing,steepNess,noiseSpeed, targetPlayers, targetMobs, weaponsOnly, throughWalls, disableOnTarget, workOnlyWhileRMBPressed);
    }
    
    @EventHandler
    private void onRender3D(EventRender3D event) {
        if (isNull()) return;
        
        if (weaponsOnly.getValue() && !isHoldingWeapon()) return;
        if (mc.currentScreen != null) return;
        HitResult hit = mc.crosshairTarget;

        if (disableOnTarget.getValue() && hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (isValidTarget(entity) && entity != currentTarget) {
                return;
            }
        }
        if (workOnlyWhileRMBPressed.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),  GLFW.GLFW_MOUSE_BUTTON_2) != GLFW.GLFW_PRESS) return;

        currentTarget = findBestTarget();
        
        if (currentTarget != null) {
            if (!throughWalls.getValue() && !mc.player.canSee(currentTarget)) return;
            Vec3d chestPos = getChestPosition(currentTarget);
            float[] rotation = calculateRotation(chestPos);
            applySmoothAiming(rotation[0], rotation[1]);
        }
    }
    private Entity findBestTarget() {
        if (isNull()) return null;
        
            for (Entity entity : mc.world.getEntities()) {
                if (!isValidTarget(entity)) continue;
                    double distance = mc.player.distanceTo(entity);
                    if (distance <= range.getValue()) {
                        Vec3d chestPos = getChestPosition(entity);
                        float[] rotation = calculateRotation(chestPos);
                        double fovDistance = getFOVDistance(rotation[0], rotation[1]);
                        if (fovDistance <= fov.getValue() / 2.0) {
                            return entity;
                        }
                    
                    break;
                
            }
        }
        
    
        Entity bestTarget = null;
        double bestScore = Double.MAX_VALUE;
        
        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity)) continue;
            
            double distance = mc.player.distanceTo(entity);
            if (distance > range.getValue()) continue;
            
            Vec3d chestPos = getChestPosition(entity);
            float[] rotation = calculateRotation(chestPos);
            double fovDistance = getFOVDistance(rotation[0], rotation[1]);
            
            if (fovDistance <= fov.getValue() / 2.0) {
                double score;
                if (mode.getMode().equals("Health") && entity instanceof LivingEntity livingEntity) {
                    float health = livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
                    score = health + (fovDistance * 2.0);
                } else {
                    score = distance + (fovDistance * 2.0);
                }
                if (score < bestScore) {
                    bestScore = score;
                    bestTarget = entity;
                }
            }
        }
        
        return bestTarget;
    }
    
    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !(entity instanceof LivingEntity livingEntity)) return false;
        if (!livingEntity.isAlive() || livingEntity.isDead()) return false;
        if (entity instanceof PlayerEntity player && FriendManager.isFriend(player.getUuid())) return false;
        if (Teams.isTeammate(entity)) return false;
        
        return entity instanceof PlayerEntity ? targetPlayers.getValue() : targetMobs.getValue();
    }

    // i didnt know sigmoid existed lel - graph
    private float sigmoid(float x) {
        float steep = steepNess.getValueFloat();
        return 1.0f / (1.0f + ((float)Math.exp(-steep * ((x - 0.5f)))));
    }
    public float noise(float x) {
        return (float)(Math.sin(x) + Math.sin(x * 0.5)) * 0.5f;
    }

    private Vec3d getChestPosition(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.6, entity.getZ());
    }
    
    private float[] calculateRotation(Vec3d target) {
        Vec3d diff = target.subtract(mc.player.getEyePos());
        double distance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distance));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -89.0f, 89.0f)};
    }
    
    private double getFOVDistance(float targetYaw, float targetPitch) {
        float yawDiff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float pitchDiff = targetPitch - mc.player.getPitch();
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private void applySmoothAiming(float targetYaw, float targetPitch) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000.0f, 0.1f);
        lastUpdateTime = currentTime;

        float yawDiff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float pitchDiff = targetPitch - mc.player.getPitch();
        float angularDistance = ((float)Math.hypot(yawDiff, pitchDiff));

        float normalizedDist = MathHelper.clamp(angularDistance / 90f, 0.0f, 1.0f);

        float easingFactor = sigmoid(normalizedDist);
        float stepScale = easingFactor * (float) (speed.getValue() * deltaTime / smoothing.getValue());
        
        float yawStep = MathHelper.clamp(yawDiff * stepScale * (float) yawSpeed.getValue(), -10.0f, 10.0f);
        float pitchStep = MathHelper.clamp(pitchDiff * stepScale * (float) pitchSpeed.getValue(), -10.0f, 10.0f);

        
        noiseTime += deltaTime * noiseSpeed.getValueFloat();

        float noiseYaw = noise(noiseTime) * 0.05f;
        float noisePitch = noise(noiseTime + 100f) * 0.05f;

        yawStep += noiseYaw;
        pitchStep += noisePitch;

        
        if (Math.abs(yawDiff) > 0.1f || Math.abs(pitchDiff) > 0.1f) {
            mc.player.setYaw(mc.player.getYaw() + yawStep);
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + pitchStep, -89.0f, 89.0f));
        }
    }
private boolean isHoldingWeapon() {
    if (mc.player == null) return false;
    if (mc.player.getMainHandStack().isEmpty()) return false;
    Item heldItem = mc.player.getMainHandStack().getItem();
    return heldItem instanceof SwordItem || heldItem instanceof AxeItem;
}
    @Override
    public void onEnable() {
        super.onEnable();
        lastUpdateTime = System.currentTimeMillis();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }
}