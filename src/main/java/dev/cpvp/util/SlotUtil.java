package dev.cpvp.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import java.util.function.Predicate;

public final class SlotUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private SlotUtil() {}

    public static int selected() { return mc.player.getInventory().selectedSlot; }

    public static int find(Predicate<ItemStack> filter) {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) if (filter.test(inv.getStack(i))) return i;
        return -1;
    }

    /**
     * Packet-level hotbar swap (ServerboundSetCarriedItemPacket == UpdateSelectedSlotC2SPacket).
     * The client slot is mirrored locally so interactionManager uses the right stack.
     */
    public static void swap(int slot) {
        if (slot < 0 || slot > 8) return;
        PlayerInventory inv = mc.player.getInventory();
        if (inv.selectedSlot == slot) return;
        inv.selectedSlot = slot;
        PacketUtil.send(new UpdateSelectedSlotC2SPacket(slot));
    }

    /** Hotbar slot with the best mining speed against the block, or the current slot if nothing beats it. */
    public static int bestTool(BlockState state) {
        PlayerInventory inv = mc.player.getInventory();
        int best = inv.selectedSlot;
        float bestSpeed = inv.getStack(best).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            float s = inv.getStack(i).getMiningSpeedMultiplier(state);
            if (s > bestSpeed) { bestSpeed = s; best = i; }
        }
        return best;
    }
}
