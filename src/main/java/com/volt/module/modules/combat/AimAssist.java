package com.volt.module.modules.combat;

import com.volt.event.impl.render.EventRender3D;
import com.volt.module.Category;
import com.volt.module.Module;
import com.volt.module.modules.misc.Teams;
import com.volt.module.setting.BooleanSetting;
import com.volt.module.setting.ModeSetting;
import com.volt.module.setting.NumberSetting;
import com.volt.utils.math.TimerUtil;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProfessionalAimAssist extends Module {
    
    private final ModeSetting targetMode = new ModeSetting("Target Mode", "Combined","Distance", "Health", "Angle", "Combined", "Threat");
    private final ModeSetting aimingMode = new ModeSetting("Aiming Mode", "Adaptive", "Linear", "Smooth", "Exponential", "Cubic", "Adaptive");
    private final ModeSetting targetPart = new ModeSetting("Target Part", "Smart", "Head", "Neck", "Chest", "Waist", "Feet", "Center", "Smart", "Dynamic");
    private final ModeSetting activationMode = new ModeSetting("Activation", "Mouse Movement", "Always", "On Attack", "On RMB", "On LMB", "Mouse Movement", "Combat Mode");
    
    private final NumberSetting baseSpeed = new NumberSetting("Base Speed", 0.5, 15.0, 3.2, 0.1);
    private final NumberSetting yawSpeed = new NumberSetting("Yaw Speed", 0.1, 10.0, 2.8, 0.1);
    private final NumberSetting pitchSpeed = new NumberSetting("Pitch Speed", 0.1, 10.0, 2.2, 0.1);
    private final NumberSetting smoothing = new NumberSetting("Smoothing", 1.0, 50.0, 14.5, 0.5);
    private final NumberSetting acceleration = new NumberSetting("Acceleration", 0.1, 5.0, 1.8, 0.1);
    private final NumberSetting deceleration = new NumberSetting("Deceleration", 0.1, 5.0, 2.4, 0.1);
    private final NumberSetting maxAimSpeed = new NumberSetting("Max Aim Speed", 1.0, 20.0, 8.5, 0.5);
    
    private final NumberSetting maxRange = new NumberSetting("Max Range", 1.0, 15.0, 4.8, 0.1);
    private final NumberSetting minRange = new NumberSetting("Min Range", 0.0, 3.0, 0.5, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 5.0, 180.0, 65.0, 1.0);
    private final NumberSetting verticalFov = new NumberSetting("Vertical FOV", 5.0, 180.0, 55.0, 1.0);
    private final NumberSetting dynamicFov = new NumberSetting("Dynamic FOV", 0.0, 2.0, 1.3, 0.1);
    
    private final NumberSetting aimPrediction = new NumberSetting("Aim Prediction", 0.0, 5.0, 1.6, 0.1);
    private final NumberSetting reactionDelay = new NumberSetting("Reaction Time", 0, 300, 85, 5);
    private final NumberSetting maxAimAngle = new NumberSetting("Max Aim Angle", 1.0, 90.0, 35.0, 1.0);
    private final NumberSetting aimDuration = new NumberSetting("Aim Duration", 100, 8000, 2500, 100);
    private final NumberSetting aimPersistence = new NumberSetting("Aim Persistence", 0.1, 3.0, 1.4, 0.1);
    private final NumberSetting targetSwitchDelay = new NumberSetting("Target Switch Delay", 50, 1000, 200, 25);
    
    private final NumberSetting humanization = new NumberSetting("Humanization", 0.0, 10.0, 6.2, 0.1);
    private final NumberSetting noiseStrength = new NumberSetting("Noise Strength", 0.0, 2.0, 0.45, 0.05);
    private final NumberSetting noiseFrequency = new NumberSetting("Noise Frequency", 0.1, 10.0, 3.2, 0.1);
    private final NumberSetting jitterChance = new NumberSetting("Jitter Chance", 0.0, 100.0, 22.0, 1.0);
    private final NumberSetting overshootChance = new NumberSetting("Overshoot Chance", 0.0, 100.0, 15.0, 1.0);
    private final NumberSetting undershootChance = new NumberSetting("Undershoot Chance", 0.0, 100.0, 12.0, 1.0);
    private final NumberSetting aimWobble = new NumberSetting("Aim Wobble", 0.0, 2.0, 0.38, 0.05);
    private final NumberSetting fatigue = new NumberSetting("Fatigue Factor", 0.0, 3.0, 1.1, 0.1);
    
    private final BooleanSetting targetPlayers = new BooleanSetting("Target Players", true);
    private final BooleanSetting targetHostileMobs = new BooleanSetting("Target Hostile Mobs", false);
    private final BooleanSetting targetPassiveMobs = new BooleanSetting("Target Passive Mobs", false);
    private final BooleanSetting targetInvisible = new BooleanSetting("Target Invisible", true);
    private final BooleanSetting ignoreTeammates = new BooleanSetting("Ignore Teammates", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final BooleanSetting prioritizeLowHealth = new BooleanSetting("Prioritize Low Health", true);
    private final BooleanSetting prioritizeCloseTargets = new BooleanSetting("Prioritize Close Targets", true);
    private final BooleanSetting ignoreArmoredTargets = new BooleanSetting("Ignore Armored Targets", false);
    
    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", false);
    private final BooleanSetting meleeOnly = new BooleanSetting("Melee Only", false);
    private final BooleanSetting rangedOnly = new BooleanSetting("Ranged Only", false);
    private final BooleanSetting toolsAsWeapons = new BooleanSetting("Tools as Weapons", true);
    private final BooleanSetting requireSword = new BooleanSetting("Sword Only", false);
    private final BooleanSetting adaptToWeapon = new BooleanSetting("Adapt to Weapon", true);
    
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private final BooleanSetting onlyWhenAttacking = new BooleanSetting("Only When Attacking", false);
    private final BooleanSetting stopOnTarget = new BooleanSetting("Stop on Target", true);
    private final BooleanSetting pauseOnGUI = new BooleanSetting("Pause on GUI", true);
    private final BooleanSetting pauseOnChat = new BooleanSetting("Pause on Chat", true);
    private final BooleanSetting adaptiveSpeed = new BooleanSetting("Adaptive Speed", true);
    private final BooleanSetting combatMode = new BooleanSetting("Combat Mode", true);
    private final BooleanSetting sneakDisable = new BooleanSetting("Disable When Sneaking", false);
    
    private final BooleanSetting multiTarget = new BooleanSetting("Multi Target", true);
    private final NumberSetting maxTargets = new NumberSetting("Max Targets", 1, 8, 3, 1);
    private final BooleanSetting targetQueue = new BooleanSetting("Target Queue", true);
    private final BooleanSetting smartSwitching = new BooleanSetting("Smart Switching", true);
    private final NumberSetting switchThreshold = new NumberSetting("Switch Threshold", 0.5, 5.0, 2.2, 0.1);
    
    private final NumberSetting updateRate = new NumberSetting("Update Rate", 10, 120, 45, 5);
    private final BooleanSetting asyncTargeting = new BooleanSetting("Async Targeting", true);
    private final BooleanSetting smoothFrameRate = new BooleanSetting("Smooth Frame Rate", true);
    private final BooleanSetting antiCheatBypass = new BooleanSetting("Anti-Cheat Bypass", true);
    private final NumberSetting randomization = new NumberSetting("Randomization", 0.0, 5.0, 2.8, 0.1);
    private final BooleanSetting mouseBind = new BooleanSetting("Bind to Mouse Movement", true);
    
    private final BooleanSetting aimLock = new BooleanSetting("Aim Lock", false);
    private final NumberSetting lockStrength = new NumberSetting("Lock Strength", 0.1, 3.0, 1.2, 0.1);
    private final BooleanSetting wallCheck = new BooleanSetting("Wall Check", true);
    private final BooleanSetting entityPrediction = new BooleanSetting("Entity Prediction", true);
    private final NumberSetting predictionAccuracy = new NumberSetting("Prediction Accuracy", 0.1, 2.0, 1.45, 0.05);
    private final BooleanSetting autoBlock = new BooleanSetting("Auto Block Integration", false);
    private final BooleanSetting criticalHits = new BooleanSetting("Critical Hit Timing", true);
    
    private Entity primaryTarget = null;
    private final List<Entity> targetList = new ArrayList<>();
    private final Map<Entity, TargetInfo> targetCache = new ConcurrentHashMap<>();
    private long lastUpdateTime = 0;
    private long lastTargetSwitch = 0;
    private long aimStartTime = 0;
    private long combatStartTime = 0;
    private float currentNoiseTime = 0f;
    private Vec3d lastAimDirection = Vec3d.ZERO;
    private boolean isAiming = false;
    private boolean inCombat = false;
    private final Random humanRandom = new Random();
    private TimerUtil updateTimer = new TimerUtil();
    private TimerUtil reactionTimer = new TimerUtil();
    private TimerUtil fatigueTimer = new TimerUtil();
    
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private long lastMouseMovement = 0;
    private double mouseSensitivity = 1.0;
    private Vec3d aimingVelocity = Vec3d.ZERO;
    private double currentFatigue = 0;
    
    private static class TargetInfo {
        Vec3d lastPosition;
        Vec3d predictedPosition;
        Vec3d velocity;
        long lastUpdate;
        long firstSeen;
        float health;
        float maxHealth;
        double distance;
        double fovDistance;
        int hitCount;
        boolean isMoving;
        double threat;
        
        TargetInfo(Vec3d pos, float hp, float maxHp, double dist, double fov) {
            this.lastPosition = pos;
            this.predictedPosition = pos;
            this.velocity = Vec3d.ZERO;
            this.lastUpdate = System.currentTimeMillis();
            this.firstSeen = this.lastUpdate;
            this.health = hp;
            this.maxHealth = maxHp;
            this.distance = dist;
            this.fovDistance = fov;
            this.hitCount = 0;
            this.isMoving = false;
            this.threat = calculateThreat();
        }
        
        private double calculateThreat() {
            double healthPercent = maxHealth > 0 ? health / maxHealth : 1.0;
            double distanceThreat = Math.max(0, (10.0 - distance) / 10.0);
            double angleThreat = Math.max(0, (45.0 - fovDistance) / 45.0);
            return (1.0 - healthPercent) * 0.4 + distanceThreat * 0.35 + angleThreat * 0.25;
        }
        
        void updateThreat() {
            this.threat = calculateThreat();
        }
    }

    public ProfessionalAimAssist() {
        super("Professional Aim Assist", "Advanced aim assistance with professional configuration", Category.COMBAT);
        
        addSettings(
            targetMode, aimingMode, targetPart, activationMode,
            baseSpeed, yawSpeed, pitchSpeed, smoothing, acceleration, deceleration, maxAimSpeed,
            maxRange, minRange, fov, verticalFov, dynamicFov,
            aimPrediction, reactionDelay, maxAimAngle, aimDuration, aimPersistence, targetSwitchDelay,
            humanization, noiseStrength, noiseFrequency, jitterChance, overshootChance, 
            undershootChance, aimWobble, fatigue,
            targetPlayers, targetHostileMobs, targetPassiveMobs, targetInvisible, 
            ignoreTeammates, ignoreFriends, prioritizeLowHealth, prioritizeCloseTargets, ignoreArmoredTargets,
            weaponsOnly, meleeOnly, rangedOnly, toolsAsWeapons, requireSword, adaptToWeapon,
            throughWalls, onlyWhenAttacking, stopOnTarget, pauseOnGUI, pauseOnChat, 
            adaptiveSpeed, combatMode, sneakDisable,
            multiTarget, maxTargets, targetQueue, smartSwitching, switchThreshold,
            updateRate, asyncTargeting, smoothFrameRate, antiCheatBypass, randomization, mouseBind,
            aimLock, lockStrength, wallCheck, entityPrediction, predictionAccuracy, autoBlock, criticalHits
        );
    }
    
    @EventHandler
    private void onRender3D(EventRender3D event) {
        if (shouldPause()) return;
        
        updateFatigueSystem();
        
        if (!shouldUpdate()) return;
        
        trackMouseMovement();
        
        if (!isActivationValid()) return;
        
        if (!isWeaponValid()) return;
        
        if (!hasReactionTimeElapsed()) return;
        
        updateCombatState();
        
        updateAdvancedTargeting();
        
        if (primaryTarget != null && shouldPerformAiming()) {
            performAdvancedAiming(event.getTickDelta());
        } else {
            performAimingDecay();
        }
        
        performMaintenanceTasks();
    }
    
    private boolean shouldPause() {
        if (isNull()) return true;
        if (pauseOnGUI.getValue() && mc.currentScreen != null) return true;
        if (pauseOnChat.getValue() && mc.currentScreen != null) return true;
        if (sneakDisable.getValue() && mc.player.isSneaking()) return true;
        
        if (stopOnTarget.getValue() && isPerfectlyAligned()) return true;
        
        return false;
    }
    
    private boolean isPerfectlyAligned() {
        if (primaryTarget == null) return false;
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            return entityHit.getEntity() == primaryTarget;
        }
        return false;
    }
    
    private void updateFatigueSystem() {
        if (fatigue.getValue() <= 0) {
            currentFatigue = 0;
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (isAiming && primaryTarget != null) {
            double fatigueRate = fatigue.getValue() * 0.001;
            currentFatigue = Math.min(1.0, currentFatigue + fatigueRate);
        } else if (currentTime - lastUpdateTime > 2000) {
            currentFatigue = Math.max(0, currentFatigue - 0.002);
        }
    }
    
    private boolean shouldUpdate() {
        long currentTime = System.currentTimeMillis();
        double baseInterval = 1000.0 / updateRate.getValue();
        
        double randomFactor = 1.0;
        if (randomization.getValue() > 0) {
            randomFactor = 0.7 + (humanRandom.nextDouble() * 0.6);
            randomFactor *= (1.0 + humanRandom.nextGaussian() * randomization.getValue() * 0.1);
        }
        
        double actualInterval = baseInterval * randomFactor;
        
        if (currentTime - lastUpdateTime < actualInterval) return false;
        
        lastUpdateTime = currentTime;
        updateTimer.reset();
        return true;
    }
    
    private void trackMouseMovement() {
        if (mc.player == null) return;
        
        double currentYaw = mc.player.getYaw();
        double currentPitch = mc.player.getPitch();
        
        double mouseDeltaX = Math.abs(currentYaw - lastMouseX);
        double mouseDeltaY = Math.abs(currentPitch - lastMouseY);
        
        if (mouseDeltaX > 0.1 || mouseDeltaY > 0.1) {
            lastMouseMovement = System.currentTimeMillis();
            
            mouseSensitivity = Math.max(0.1, Math.min(3.0, (mouseDeltaX + mouseDeltaY) / 2.0));
        }
        
        lastMouseX = currentYaw;
        lastMouseY = currentPitch;
    }
    
    private boolean isActivationValid() {
        String mode = activationMode.getMode();
        long timeSinceMouseMove = System.currentTimeMillis() - lastMouseMovement;
        
        switch (mode) {
            case "Always": return true;
            case "On Attack": return mc.options.attackKey.isPressed();
            case "On RMB": return isMousePressed(GLFW.GLFW_MOUSE_BUTTON_2);
            case "On LMB": return isMousePressed(GLFW.GLFW_MOUSE_BUTTON_1);
            case "Mouse Movement": return timeSinceMouseMove < 500;
            case "Combat Mode": return inCombat || timeSinceMouseMove < 200;
            default: return true;
        }
    }
    
    private boolean isMousePressed(int button) {
        try {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), button) == GLFW.GLFW_PRESS;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isWeaponValid() {
        if (mc.player == null) return false;
        
        if (!weaponsOnly.getValue() && !meleeOnly.getValue() && !rangedOnly.getValue() && !requireSword.getValue()) {
            return true;
        }
        
        if (mc.player.getMainHandStack().isEmpty()) return !weaponsOnly.getValue();
        
        Item item = mc.player.getMainHandStack().getItem();
        
        if (requireSword.getValue() && !(item instanceof SwordItem)) return false;
        if (meleeOnly.getValue() && !isMeleeWeapon(item)) return false;
        if (rangedOnly.getValue() && !isRangedWeapon(item)) return false;
        
        if (weaponsOnly.getValue()) {
            return isMeleeWeapon(item) || isRangedWeapon(item) || 
                   (toolsAsWeapons.getValue() && isToolWeapon(item));
        }
        
        return true;
    }
    
    private boolean isMeleeWeapon(Item item) {
        return item instanceof SwordItem || item instanceof AxeItem;
    }
    
    private boolean isRangedWeapon(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem;
    }
    
    private boolean isToolWeapon(Item item) {
        return item instanceof PickaxeItem || item instanceof ShovelItem || item instanceof HoeItem;
    }
    
    private boolean hasReactionTimeElapsed() {
        if (reactionDelay.getValue() <= 0) return true;
        
        double variation = humanization.getValue() * 0.1;
        double actualDelay = reactionDelay.getValue() * (0.8 + humanRandom.nextDouble() * 0.4);
        actualDelay *= (1.0 + humanRandom.nextGaussian() * variation);
        
        return reactionTimer.hasTimeElapsed((long)actualDelay);
    }
    
    private void updateCombatState() {
        boolean wasInCombat = inCombat;
        
        inCombat = mc.options.attackKey.isPressed() || 
                  isMousePressed(GLFW.GLFW_MOUSE_BUTTON_1) ||
                  (System.currentTimeMillis() - lastMouseMovement < 1000 && primaryTarget != null);
        
        if (inCombat && !wasInCombat) {
            combatStartTime = System.currentTimeMillis();
        }
    }
    
    private void updateAdvancedTargeting() {
        targetList.clear();
        List<Entity> potentialTargets = new ArrayList<>();
        
        if (mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
                if (isValidTarget(entity)) {
                    double distance = mc.player.distanceTo(entity);
                    if (distance >= minRange.getValue() && distance <= getEffectiveRange()) {
                        potentialTargets.add(entity);
                        updateOrCreateTargetInfo(entity);
                    }
                }
            }
        }
        
        potentialTargets.sort(this::compareTargetsAdvanced);
        
        int maxTargetCount = multiTarget.getValue() ? Math.min((int)maxTargets.getValue(), potentialTargets.size()) : 1;
        
        for (int i = 0; i < maxTargetCount; i++) {
            Entity target = potentialTargets.get(i);
            if (isInEffectiveFOV(target)) {
                targetList.add(target);
            }
        }
        
        updatePrimaryTarget();
    }
    
    private double getEffectiveRange() {
        double baseRange = maxRange.getValue();
        
        if (adaptToWeapon.getValue() && mc.player != null && !mc.player.getMainHandStack().isEmpty()) {
            Item item = mc.player.getMainHandStack().getItem();
            if (isRangedWeapon(item)) {
                baseRange *= 1.5;
            }
        }
        
        return baseRange;
    }
    
    private void updateOrCreateTargetInfo(Entity entity) {
        Vec3d currentPos = getOptimalTargetPosition(entity);
        float health = entity instanceof LivingEntity living ? living.getHealth() : 100f;
        float maxHealth = entity instanceof LivingEntity living ? living.getMaxHealth() : 100f;
        double distance = mc.player.distanceTo(entity);
        
        float[] rotation = calculateRotationToTarget(currentPos);
        double fovDistance = getFOVDistance(rotation[0], rotation[1]);
        
        TargetInfo info = targetCache.get(entity);
        if (info != null) {
            long timeDelta = System.currentTimeMillis() - info.lastUpdate;
            if (timeDelta > 0) {
                Vec3d velocity = currentPos.subtract(info.lastPosition).multiply(1000.0 / timeDelta);
                info.velocity = velocity.multiply(0.7).add(info.velocity.multiply(0.3));
                info.isMoving = velocity.length() > 0.1;
                
                if (entityPrediction.getValue() && info.isMoving) {
                    double predictionTime = aimPrediction.getValue() * (distance / 20.0);
                    info.predictedPosition = currentPos.add(velocity.multiply(predictionTime * predictionAccuracy.getValue()));
                }
            }
            
            info.lastPosition = currentPos;
            info.lastUpdate = System.currentTimeMillis();
            info.health = health;
            info.maxHealth = maxHealth;
            info.distance = distance;
            info.fovDistance = fovDistance;
            info.updateThreat();
        } else {
            targetCache.put(entity, new TargetInfo(currentPos, health, maxHealth, distance, fovDistance));
        }
    }
    
    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player) return false;
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive() || living.isDead()) return false;
        
        if (!targetInvisible.getValue() && living.isInvisible()) return false;
        
        if (ignoreTeammates.getValue() && Teams.isTeammate(entity)) return false;
        
        if (ignoreArmoredTargets.getValue() && living.getArmor() > 0) return false;
        
        if (entity instanceof PlayerEntity) {
            return targetPlayers.getValue();
        } else if (entity instanceof HostileEntity) {
            return targetHostileMobs.getValue();
        } else if (entity instanceof PassiveEntity) {
            return targetPassiveMobs.getValue();
        }
        
        return false;
    }
    
    private boolean isInEffectiveFOV(Entity entity) {
        Vec3d targetPos = getOptimalTargetPosition(entity);
        float[] rotation = calculateRotationToTarget(targetPos);
        double fovDistance = getFOVDistance(rotation[0], rotation[1]);
        
        double effectiveFOV = fov.getValue();
        if (dynamicFov.getValue() > 0) {
            double distance = mc.player.distanceTo(entity);
            effectiveFOV += Math.min(30, distance * dynamicFov.getValue());
        }
        
        double effectiveVerticalFOV = verticalFov.getValue();
        
        float yawDiff = Math.abs(MathHelper.wrapDegrees(rotation[0] - mc.player.getYaw()));
        float pitchDiff = Math.abs(rotation[1] - mc.player.getPitch());
        
        return yawDiff <= effectiveFOV / 2.0 && pitchDiff <= effectiveVerticalFOV / 2.0;
    }
    
    private int compareTargetsAdvanced(Entity a, Entity b) {
        TargetInfo infoA = targetCache.get(a);
        TargetInfo infoB = targetCache.get(b);
        
        if (infoA == null || infoB == null) {
            return infoA == null ? 1 : -1;
        }
        
        String mode = targetMode.getMode();
        switch (mode) {
            case "Distance":
                return Double.compare(infoA.distance, infoB.distance);
            case "Health":
                if (prioritizeLowHealth.getValue()) {
                    return Float.compare(infoA.health, infoB.health);
                } else {
                    return Float.compare(infoB.health, infoA.health);
                }
            case "Angle":
                return Double.compare(infoA.fovDistance, infoB.fovDistance);
            case "Threat":
                return Double.compare(infoB.threat, infoA.threat);
            case "Combined":
            default:
                double scoreA = calculateTargetScore(infoA);
                double scoreB = calculateTargetScore(infoB);
                return Double.compare(scoreA, scoreB);
        }
    }
    
    private double calculateTargetScore(TargetInfo info) {
        double distanceScore = (maxRange.getValue() - info.distance) / maxRange.getValue();
        double angleScore = (90.0 - info.fovDistance) / 90.0;
        double healthScore = prioritizeLowHealth.getValue() ? 
            (info.maxHealth - info.health) / info.maxHealth :
            info.health / info.maxHealth;
        double threatScore = info.threat;
        
        return distanceScore * 0.3 + angleScore * 0.25 + healthScore * 0.2 + threatScore * 0.25;
    }
    
    private void updatePrimaryTarget() {
        if (targetList.isEmpty()) {
            if (primaryTarget != null) {
                primaryTarget = null;
                reactionTimer.reset();
            }
            return;
        }
        
        Entity newPrimaryTarget = targetList.get(0);
        
        if (primaryTarget != newPrimaryTarget) {
            boolean shouldSwitch = false;
            
            if (primaryTarget == null) {
                shouldSwitch = true;
            } else if (System.currentTimeMillis() - lastTargetSwitch >= targetSwitchDelay.getValue()) {
                TargetInfo currentInfo = targetCache.get(primaryTarget);
                TargetInfo newInfo = targetCache.get(newPrimaryTarget);
                
                if (currentInfo == null || newInfo == null) {
                    shouldSwitch = true;
                } else if (smartSwitching.getValue()) {
                    double currentScore = calculateTargetScore(currentInfo);
                    double newScore = calculateTargetScore(newInfo);
                    shouldSwitch = newScore > currentScore + switchThreshold.getValue();
                } else {
                    shouldSwitch = true;
                }
            }
            
            if (shouldSwitch) {
                primaryTarget = newPrimaryTarget;
                lastTargetSwitch = System.currentTimeMillis();
                aimStartTime = System.currentTimeMillis();
                reactionTimer.reset();
            }
        }
    }
    
    private Vec3d getOptimalTargetPosition(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        
        Vec3d basePos = entity.getPos();
        double height = entity.getHeight();
        
        String part = targetPart.getMode();
        switch (part) {
            case "Head":
                return new Vec3d(basePos.x, basePos.y + height * 0.95, basePos.z);
            case "Neck":
                return new Vec3d(basePos.x, basePos.y + height * 0.85, basePos.z);
            case "Chest":
                return new Vec3d(basePos.x, basePos.y + height * 0.65, basePos.z);
            case "Waist":
                return new Vec3d(basePos.x, basePos.y + height * 0.45, basePos.z);
            case "Feet":
                return new Vec3d(basePos.x, basePos.y + height * 0.1, basePos.z);
            case "Center":
                return new Vec3d(basePos.x, basePos.y + height * 0.5, basePos.z);
            case "Dynamic":
                double distance = mc.player.distanceTo(entity);
                double multiplier = distance > 4 ? 0.75 : 0.65;
                return new Vec3d(basePos.x, basePos.y + height * multiplier, basePos.z);
            case "Smart":
            default:
                return calculateSmartTargetPosition(entity, basePos, height);
        }
    }
    
    private Vec3d calculateSmartTargetPosition(Entity entity, Vec3d basePos, double height) {
        double distance = mc.player.distanceTo(entity);
        TargetInfo info = targetCache.get(entity);
        
        double baseMultiplier = 0.65;
        
        if (distance > 4) baseMultiplier += 0.1;
        if (distance > 6) baseMultiplier += 0.05;
        
        if (info != null && info.isMoving) {
            baseMultiplier += 0.05;
        }
        
        if (inCombat && criticalHits.getValue()) {
            baseMultiplier += 0.05;
        }
        
        baseMultiplier += (humanRandom.nextGaussian() * 0.02);
        
        return new Vec3d(basePos.x, basePos.y + height * MathHelper.clamp(baseMultiplier, 0.1, 0.95), basePos.z);
    }
    
    private boolean shouldPerformAiming() {
        if (primaryTarget == null) return false;
        
        if (!throughWalls.getValue() && wallCheck.getValue() && !hasLineOfSight(primaryTarget)) return false;
        
        if (onlyWhenAttacking.getValue() && !isAttacking()) return false;
        
        long aimTime = System.currentTimeMillis() - aimStartTime;
        if (aimTime > aimDuration.getValue()) return false;
        
        if (!isValidTarget(primaryTarget)) return false;
        
        return true;
    }
    
    private boolean hasLineOfSight(Entity target) {
        if (!wallCheck.getValue()) return true;
        
        Vec3d start = mc.player.getEyePos();
        Vec3d end = getOptimalTargetPosition(target);
        
        RaycastContext context = new RaycastContext(
            start, end,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        );
        
        return mc.world.raycast(context).getType() == HitResult.Type.MISS;
    }
    
    private boolean isAttacking() {
        return mc.options.attackKey.isPressed() || isMousePressed(GLFW.GLFW_MOUSE_BUTTON_1);
    }
    
    private void performAdvancedAiming(float tickDelta) {
        if (primaryTarget == null || mc.player == null) return;
        
        Vec3d targetPos = getOptimalTargetPosition(primaryTarget);
        TargetInfo info = targetCache.get(primaryTarget);
        
        if (aimPrediction.getValue() > 0 && info != null && info.isMoving) {
            targetPos = info.predictedPosition;
        }
        
        float[] targetRotation = calculateRotationToTarget(targetPos);
        float targetYaw = targetRotation[0];
        float targetPitch = targetRotation[1];
        
        float yawDiff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float pitchDiff = targetPitch - mc.player.getPitch();
        
        double totalAngle = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        if (totalAngle > maxAimAngle.getValue()) return;
        
        applyProfessionalAiming(yawDiff, pitchDiff, totalAngle, tickDelta);
        isAiming = true;
    }
    
    private void applyProfessionalAiming(float yawDiff, float pitchDiff, double totalAngle, float tickDelta) {
        double distanceToTarget = mc.player.distanceTo(primaryTarget);
        double speedMultiplier = calculateSpeedMultiplier(totalAngle, distanceToTarget);
        
        float[] aimingFactors = calculateAimingFactors(totalAngle, speedMultiplier);
        
        float baseYawStep = yawDiff * aimingFactors[0] * (float)yawSpeed.getValue();
        float basePitchStep = pitchDiff * aimingFactors[1] * (float)pitchSpeed.getValue();
        
        if (adaptToWeapon.getValue()) {
            float[] weaponMods = getWeaponModifications();
            baseYawStep *= weaponMods[0];
            basePitchStep *= weaponMods[1];
        }
        
        float[] humanizedSteps = applyHumanization(baseYawStep, basePitchStep, tickDelta);
        
        if (currentFatigue > 0) {
            double fatigueMultiplier = 1.0 - (currentFatigue * 0.3);
            humanizedSteps[0] *= fatigueMultiplier;
            humanizedSteps[1] *= fatigueMultiplier;
        }
        
        humanizedSteps[0] = MathHelper.clamp(humanizedSteps[0], -(float)maxAimSpeed.getValue(), (float)maxAimSpeed.getValue());
        humanizedSteps[1] = MathHelper.clamp(humanizedSteps[1], -(float)maxAimSpeed.getValue() * 0.7f, (float)maxAimSpeed.getValue() * 0.7f);
        
        if (aimLock.getValue() && Math.abs(yawDiff) < 2.0 && Math.abs(pitchDiff) < 1.5) {
            humanizedSteps[0] *= lockStrength.getValue();
            humanizedSteps[1] *= lockStrength.getValue();
        }
        
        if (Math.abs(yawDiff) > 0.02f || Math.abs(pitchDiff) > 0.02f) {
            mc.player.setYaw(mc.player.getYaw() + humanizedSteps[0]);
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + humanizedSteps[1], -89.0f, 89.0f));
            
            aimingVelocity = new Vec3d(humanizedSteps[0], humanizedSteps[1], 0);
        }
    }
    
    private double calculateSpeedMultiplier(double totalAngle, double distance) {
        double baseMultiplier = 1.0;
        
        if (adaptiveSpeed.getValue()) {
            baseMultiplier = 0.3 + (totalAngle / 45.0) * 0.7;
            baseMultiplier = MathHelper.clamp(baseMultiplier, 0.3, 1.5);
        }
        
        double distanceMultiplier = 1.0;
        if (distance > 3) {
            distanceMultiplier = Math.min(1.3, 1.0 + (distance - 3) * 0.1);
        }
        
        double mouseSensMultiplier = 1.0;
        if (mouseBind.getValue()) {
            mouseSensMultiplier = Math.max(0.5, Math.min(2.0, mouseSensitivity));
        }
        
        return baseMultiplier * distanceMultiplier * mouseSensMultiplier;
    }
    
    private float[] calculateAimingFactors(double totalAngle, double speedMultiplier) {
        String mode = aimingMode.getMode();
        float easingFactor;
        
        switch (mode) {
            case "Linear":
                easingFactor = 1.0f;
                break;
            case "Exponential":
                easingFactor = (float)Math.pow(totalAngle / 45.0, 1.5);
                break;
            case "Cubic":
                double t = MathHelper.clamp(totalAngle / 45.0, 0.0, 1.0);
                easingFactor = (float)(3 * t * t - 2 * t * t * t);
                break;
            case "Smooth":
                easingFactor = sigmoid((float)(totalAngle / 45.0));
                break;
            case "Adaptive":
            default:
                easingFactor = calculateAdaptiveEasing(totalAngle);
                break;
        }
        
        float finalSpeedFactor = (float)(baseSpeed.getValue() * speedMultiplier * easingFactor / smoothing.getValue());
        
        return new float[]{finalSpeedFactor, finalSpeedFactor * 0.85f};
    }
    
    private float calculateAdaptiveEasing(double angle) {
        if (angle < 5.0) return 0.4f;
        if (angle < 15.0) return sigmoid((float)(angle / 20.0));
        return Math.min(1.5f, (float)(0.8 + angle / 30.0));
    }
    
    private float sigmoid(float x) {
        return 1.0f / (1.0f + (float)Math.exp(-6.0f * (x - 0.5f)));
    }
    
    private float[] getWeaponModifications() {
        if (mc.player == null || mc.player.getMainHandStack().isEmpty()) {
            return new float[]{1.0f, 1.0f};
        }
        
        Item item = mc.player.getMainHandStack().getItem();
        
        if (item instanceof SwordItem) {
            return new float[]{1.1f, 1.0f};
        } else if (item instanceof AxeItem) {
            return new float[]{0.95f, 0.9f};
        } else if (isRangedWeapon(item)) {
            return new float[]{1.2f, 1.3f};
        } else if (isToolWeapon(item)) {
            return new float[]{0.9f, 0.85f};
        }
        
        return new float[]{1.0f, 1.0f};
    }
    
    private float[] applyHumanization(float yawStep, float pitchStep, float tickDelta) {
        float humanFactor = (float)humanization.getValue() / 10.0f;
        
        if (noiseStrength.getValue() > 0) {
            currentNoiseTime += tickDelta * (float)noiseFrequency.getValue();
            
            float noiseX = (float)(Math.sin(currentNoiseTime) + Math.sin(currentNoiseTime * 0.7)) * 
                          (float)noiseStrength.getValue() * 0.1f;
            float noiseY = (float)(Math.cos(currentNoiseTime + 100) + Math.cos(currentNoiseTime * 0.5 + 100)) * 
                          (float)noiseStrength.getValue() * 0.05f;
            
            yawStep += noiseX;
            pitchStep += noiseY;
        }
        
        if (aimWobble.getValue() > 0) {
            float wobbleX = (float)(humanRandom.nextGaussian() * aimWobble.getValue() * 0.1);
            float wobbleY = (float)(humanRandom.nextGaussian() * aimWobble.getValue() * 0.05);
            
            yawStep += wobbleX;
            pitchStep += wobbleY;
        }
        
        if (jitterChance.getValue() > 0 && humanRandom.nextFloat() * 100 < jitterChance.getValue()) {
            yawStep += (humanRandom.nextFloat() - 0.5f) * 0.8f;
            pitchStep += (humanRandom.nextFloat() - 0.5f) * 0.4f;
        }
        
        if (overshootChance.getValue() > 0 && humanRandom.nextFloat() * 100 < overshootChance.getValue()) {
            float overshootMultiplier = 1.1f + humanRandom.nextFloat() * 0.3f;
            yawStep *= overshootMultiplier;
            pitchStep *= overshootMultiplier;
        } else if (undershootChance.getValue() > 0 && humanRandom.nextFloat() * 100 < undershootChance.getValue()) {
            float undershootMultiplier = 0.7f + humanRandom.nextFloat() * 0.2f;
            yawStep *= undershootMultiplier;
            pitchStep *= undershootMultiplier;
        }
        
        yawStep *= (1.0f + humanRandom.nextGaussian() * humanFactor * 0.1f);
        pitchStep *= (1.0f + humanRandom.nextGaussian() * humanFactor * 0.08f);
        
        return new float[]{yawStep, pitchStep};
    }
    
    private float[] calculateRotationToTarget(Vec3d target) {
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
    
    private void performAimingDecay() {
        if (isAiming) {
            aimingVelocity = aimingVelocity.multiply(0.85);
            
            if (aimingVelocity.length() < 0.01) {
                isAiming = false;
                currentNoiseTime = 0f;
            }
        }
    }
    
    private void performMaintenanceTasks() {
        long currentTime = System.currentTimeMillis();
        
        targetCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().lastUpdate > 3000 ||
            !entry.getKey().isAlive() ||
            mc.player.distanceTo(entry.getKey()) > maxRange.getValue() * 2
        );
        
        if (antiCheatBypass.getValue()) {
            performAntiCheatMeasures();
        }
    }
    
    private void performAntiCheatMeasures() {
        if (humanRandom.nextInt(1000) < 2) {
            try {
                Thread.sleep(humanRandom.nextInt(3) + 1);
            } catch (InterruptedException ignored) {}
        }
        
        if (humanRandom.nextInt(500) < 1) {
            lastUpdateTime += 50;
        }
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        lastUpdateTime = System.currentTimeMillis();
        updateTimer.reset();
        reactionTimer.reset();
        fatigueTimer.reset();
        targetCache.clear();
        targetList.clear();
        
        if (mc.player != null) {
            lastMouseX = mc.player.getYaw();
            lastMouseY = mc.player.getPitch();
        }
        
        currentFatigue = 0;
        isAiming = false;
        inCombat = false;
        aimingVelocity = Vec3d.ZERO;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        primaryTarget = null;
        targetList.clear();
        targetCache.clear();
        isAiming = false;
        inCombat = false;
        aimingVelocity = Vec3d.ZERO;
        currentNoiseTime = 0f;
        currentFatigue = 0;
    }
}
