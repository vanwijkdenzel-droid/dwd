package dev.cpvp.util;

import net.minecraft.block.BlockWithEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class PlaceUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private PlaceUtil() {}

    /** Places a full-cube block from the hotbar at pos (swap via packet, swap back). */
    public static boolean place(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;
        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > 4.5 * 4.5) return false;
        if (!mc.world.getOtherEntities(null, new Box(pos), e -> !e.isSpectator()).isEmpty()) return false;
        int slot = SlotUtil.find(s -> s.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BlockWithEntity)
                && bi.getBlock().getDefaultState().isFullCube(mc.world, BlockPos.ORIGIN));
        if (slot < 0) return false;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.offset(d);
            var st = mc.world.getBlockState(n);
            if (st.isReplaceable() || !st.isSolidBlock(mc.world, n)) continue;
            Direction face = d.getOpposite();
            Vec3d hitPos = Vec3d.ofCenter(n).add(Vec3d.of(face.getVector()).multiply(0.5));
            int prev = SlotUtil.selected();
            SlotUtil.swap(slot);
            boolean ok = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, n, false)).isAccepted();
            if (ok) mc.player.swingHand(Hand.MAIN_HAND);
            SlotUtil.swap(prev);
            return ok;
        }
        return false;
    }
}
