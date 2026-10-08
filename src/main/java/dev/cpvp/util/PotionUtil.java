package dev.cpvp.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class PotionUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private PotionUtil() {}

    @SafeVarargs
    public static boolean is(ItemStack s, RegistryEntry<Potion>... potions) {
        if (!s.isOf(Items.SPLASH_POTION)) return false;
        PotionContentsComponent pc = s.get(DataComponentTypes.POTION_CONTENTS);
        if (pc == null || pc.potion().isEmpty()) return false;
        for (RegistryEntry<Potion> k : potions) if (pc.potion().get().equals(k)) return true;
        return false;
    }

    /** Swap to the slot, throw it aimed at a world position, swap back. */
    public static void throwAt(int slot, Vec3d aim) {
        Vec3d to = aim.subtract(mc.player.getEyePos());
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        int prev = SlotUtil.selected();
        float oy = mc.player.getYaw(), op = mc.player.getPitch();
        SlotUtil.swap(slot);
        mc.player.setYaw(yaw); mc.player.setPitch(pitch);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.setYaw(oy); mc.player.setPitch(op);
        mc.player.swingHand(Hand.MAIN_HAND);
        SlotUtil.swap(prev);
    }
}
