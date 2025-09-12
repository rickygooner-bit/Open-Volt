package com.volt.module.modules.combat;


import org.lwjgl.glfw.GLFW;

import com.volt.event.impl.player.TickEvent;
import com.volt.mixin.MinecraftClientAccessor;
import com.volt.module.Category;
import com.volt.module.Module;
import com.volt.module.setting.NumberSetting;
import com.volt.utils.math.MathUtils;
import com.volt.utils.math.TimerUtil;
import com.volt.utils.mc.InventoryUtil;
import com.volt.utils.mc.MouseSimulation;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.util.hit.EntityHitResult;

public final class ShieldBreaker extends Module {
    public static final NumberSetting maxMs = new NumberSetting("Reaction Max MS", 0, 1200, 555, 0.5);
    public static final NumberSetting minMS = new NumberSetting("Reaction Min MS", 0, 1200, 545, 0.5);

    public static final NumberSetting switchDelayMax = new NumberSetting("Switch Max MS", 0, 1200, 555, 0.5);
    public static final NumberSetting switchDelayMin = new NumberSetting("Switch Min MS", 0, 1200, 545, 0.5);

    public static final NumberSetting attackDelayMax = new NumberSetting("Attack Max MS", 0, 1200, 555, 0.5);
    public static final NumberSetting attackDelayMin = new NumberSetting("Attack Min MS", 0, 1200, 545, 0.5);

    public static final NumberSetting switchBackMax = new NumberSetting("Switch Back Max MS", 0, 1200, 555, 0.5);
    public static final NumberSetting switchBackMin = new NumberSetting("Switch Back Min MS", 0, 1200, 545, 0.5);
    private final TimerUtil switchDelayTimer = new TimerUtil();
    private final TimerUtil attackDelayTimer = new TimerUtil();
    private final TimerUtil switchBackTimer = new TimerUtil();
    private int previousSlot = -1;

    public ShieldBreaker() {
        super("Shield Breaker", "Uses your axe to break their shield", -1, Category.COMBAT);
        this.addSettings(maxMs, minMS, switchDelayMax, switchDelayMin, attackDelayMax, attackDelayMin, switchBackMax, switchBackMin);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;

        // Sanity checks to keep min < max
        if (minMS.getValueInt() >= maxMs.getValueInt()) maxMs.setValue(minMS.getValueInt() + 1);
        if (switchDelayMin.getValueInt() >= switchDelayMax.getValueInt())
            switchDelayMax.setValue(switchDelayMin.getValueInt() + 1);
        if (attackDelayMin.getValueInt() >= attackDelayMax.getValueInt())
            attackDelayMax.setValue(attackDelayMin.getValueInt() + 1);
        if (switchBackMin.getValueInt() >= switchBackMax.getValueInt())
            switchBackMax.setValue(switchBackMin.getValueInt() + 1);

        if (!(mc.crosshairTarget instanceof EntityHitResult hitResult)) return;

        Entity target = hitResult.getEntity();

        if (!(target instanceof PlayerEntity playerTarget)) return;

        if (playerTarget.isBlocking() && !mc.player.isBlocking()) {
            previousSlot = mc.player.getInventory().selectedSlot;

            if (switchDelayTimer.hasElapsedTime(getRandomDelay(switchDelayMin.getValueInt(), switchDelayMax.getValueInt()), true)) {
                InventoryUtil.swapToWeapon(AxeItem.class);
            }

            if (attackDelayTimer.hasElapsedTime(getRandomDelay(attackDelayMin.getValueInt(), attackDelayMax.getValueInt()), true)) {
                ((MinecraftClientAccessor) mc).invokeDoAttack();
                MouseSimulation.mousePress(GLFW.GLFW_MOUSE_BUTTON_LEFT);
                MouseSimulation.mouseRelease(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            }

            if (switchBackTimer.hasElapsedTime(getRandomDelay(switchBackMin.getValueInt(), switchBackMax.getValueInt()), true)) {
                if (previousSlot != -1) {
                    mc.player.getInventory().selectedSlot = previousSlot;
                    previousSlot = -1; // reset
                }
            }
        }
    }

    ;

    private long getRandomDelay(int min, int max) {
        return (long) MathUtils.randomDoubleBetween(min, max);
    }

    @Override
    public void onEnable() {
        switchBackTimer.reset();
        switchDelayTimer.reset();
        attackDelayTimer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        switchBackTimer.reset();
        switchDelayTimer.reset();
        attackDelayTimer.reset();
        super.onDisable();
    }
}
