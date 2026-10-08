package dev.cpvp.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;

public final class TargetUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private TargetUtil() {}

    public static PlayerEntity nearest(double range) {
        PlayerEntity best = null;
        double bestSq = range * range;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator() || !dev.cpvp.module.impl.Modules.TargetFilterModule.allows(p)) continue;
            double d = mc.player.squaredDistanceTo(p);
            if (d < bestSq) { bestSq = d; best = p; }
        }
        return best;
    }

    /** Nearest player; if none and mobs=true, the nearest hostile mob. */
    public static LivingEntity nearestLiving(double range, boolean mobs) {
        PlayerEntity p = nearest(range);
        if (p != null || !mobs) return p;
        LivingEntity best = null;
        double bestSq = range * range;
        for (var e : mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(range), x -> x instanceof Monster && x.isAlive())) {
            double d = mc.player.squaredDistanceTo(e);
            if (d < bestSq) { bestSq = d; best = e; }
        }
        return best;
    }
}
