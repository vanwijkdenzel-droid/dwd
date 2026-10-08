package dev.cpvp.module.impl;

import dev.cpvp.gui.ClickGuiScreen;
import dev.cpvp.gui.RenderUtil;
import dev.cpvp.mixin.HandledScreenAccessor;
import dev.cpvp.mixin.MouseAccessor;
import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.module.ModuleManager;
import dev.cpvp.setting.BooleanSetting;
import dev.cpvp.setting.NumberSetting;
import dev.cpvp.util.CameraControl;
import dev.cpvp.util.PacketUtil;
import dev.cpvp.util.PlaceUtil;
import dev.cpvp.util.PotionUtil;
import dev.cpvp.util.Project;
import dev.cpvp.util.Render3D;
import dev.cpvp.util.SlotUtil;
import dev.cpvp.util.TargetUtil;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.block.BlockWithEntity;

/** Every module lives here as a nested class (keeps the repo under GitHub's 100-files-per-upload limit). */
public final class Modules {
    private Modules() {}

public static final class AimAssistModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 2, 8, 0.5, 5));
    private final NumberSetting fov   = add(new NumberSetting("FOV", 10, 180, 5, 90));
    private final NumberSetting speed = add(new NumberSetting("Smoothing Speed", 1, 100, 1, 12));
    private final BooleanSetting head = add(new BooleanSetting("Aim At Head", false));
    private final BooleanSetting click = add(new BooleanSetting("Only While Attacking", true));

    private long last = System.nanoTime();

    public AimAssistModule() { super("AimAssist", "Smoothly pulls your aim toward nearby players", Category.COMBAT); }

    @Override public void onFrame() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        if (mc.currentScreen != null || (click.get() && !mc.options.attackKey.isPressed())) return;

        PlayerEntity best = null; double bestAng = fov.get() / 2.0;
        Vec3d eye = mc.player.getEyePos();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator() || mc.player.distanceTo(p) > range.get()) continue;
            float[] r = rot(eye, aimPoint(p));
            double ang = Math.abs(MathHelper.wrapDegrees(r[0] - mc.player.getYaw()));
            if (ang < bestAng) { bestAng = ang; best = p; }
        }
        if (best == null) return;

        float[] r = rot(eye, aimPoint(best));
        float k = 1f - (float) Math.pow(1.0 - speed.get() / 100.0, dt * 60.0); // frame-rate independent
        float dy = MathHelper.wrapDegrees(r[0] - mc.player.getYaw());
        float dp = r[1] - mc.player.getPitch();
        mc.player.setYaw(mc.player.getYaw() + dy * k);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + dp * k, -90f, 90f));
    }

    private Vec3d aimPoint(PlayerEntity p) {
        return head.get() ? p.getEyePos() : p.getPos().add(0, p.getHeight() * 0.6, 0);
    }

    private static float[] rot(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new float[] { yaw, pitch };
    }
}

public static final class AntiAfkModule extends Module {
    private final NumberSetting interval = add(new NumberSetting("Interval (s)", 5, 120, 5, 30));
    private final BooleanSetting rotate  = add(new BooleanSetting("Rotate", true));
    private long last = System.currentTimeMillis();

    public AntiAfkModule() { super("Anti-AFK", "Small actions so you don't get kicked", Category.WORLD); }

    @Override public void onTick() {
        long now = System.currentTimeMillis();
        if (mc.currentScreen != null || now - last < interval.getInt() * 1000L) return;
        last = now;
        if (mc.player.isOnGround()) mc.player.jump();
        mc.player.swingHand(Hand.MAIN_HAND);
        if (rotate.get()) mc.player.setYaw(mc.player.getYaw() + ThreadLocalRandom.current().nextInt(-12, 13));
    }
}

/** Strips blindness / nausea / darkness visuals client-side. */
public static final class AntiDebuffModule extends Module {
    public AntiDebuffModule() { super("AntiDebuff", "Removes screen-obscuring effects", Category.WORLD); }

    @Override public void onTick() {
        mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
        mc.player.removeStatusEffect(StatusEffects.NAUSEA);
        mc.player.removeStatusEffect(StatusEffects.DARKNESS);
    }
}

public static final class AntiFireballModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 2, 5, 0.1, 4.0));

    public AntiFireballModule() { super("AntiFireball", "Punches incoming fireballs", Category.COMBAT); }

    @Override public void onTick() {
        double r = range.get();
        for (FireballEntity f : mc.world.getEntitiesByClass(FireballEntity.class, mc.player.getBoundingBox().expand(r + 1), e -> e.isAlive())) {
            if (mc.player.distanceTo(f) > r) continue;
            PacketUtil.attack(f);
            PacketUtil.swing();
            return;
        }
    }
}

public static final class ArmorHudModule extends HudElement {
    public ArmorHudModule() { super("ArmorHUD", "Shows your armor and durability", 6, 60); }
    @Override public int width() { return 20; }
    @Override public int height() { return 72; }

    @Override public void draw(DrawContext c, int x, int y) {
        if (mc.player == null) return;
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getInventory().armor.get(3 - i);
            if (s.isEmpty()) continue;
            c.drawItem(s, x, y + i * 18);
            c.drawItemInSlot(mc.textRenderer, s, x, y + i * 18);
        }
    }
}

/** Arrows around your crosshair pointing at players who are outside your view. */
public static final class ArrowsModule extends Module {
    private final NumberSetting radius = add(new NumberSetting("Radius", 30, 140, 5, 70));
    private final NumberSetting range  = add(new NumberSetting("Range", 16, 200, 8, 80));

    public ArrowsModule() { super("Arrows", "Off-screen player arrows", Category.RENDER); }

    @Override public void onHudRender(DrawContext c) {
        if (mc.world == null || mc.player == null || mc.currentScreen != null || mc.options.hudHidden) return;
        int cx = c.getScaledWindowWidth() / 2, cy = c.getScaledWindowHeight() / 2;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || mc.player.distanceTo(p) > range.get()) continue;
            double dx = p.getX() - mc.player.getX(), dz = p.getZ() - mc.player.getZ();
            float rel = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0) - mc.player.getYaw());
            if (Math.abs(rel) < 35) continue;               // already roughly in view
            var m = c.getMatrices();
            m.push();
            m.translate(cx, cy, 0);
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rel));
            m.translate(0, -radius.get(), 0);
            for (int r = 0; r < 7; r++) { int h = r / 2; c.fill(-h, -6 + r, h + 1, -5 + r, RenderUtil.ACCENT); }
            m.pop();
        }
    }
}

/**
 * Place -> charge -> explode. With "Double Anchor" it places and charges TWO anchors,
 * then detonates them back to back (anchors survive explosions, so both go off).
 *
 * Needs: another player within Target Range, anchor + glowstone in the HOTBAR, overworld/end.
 * The action bar tells you why it isn't firing.
 */
public static final class AutoAnchorModule extends Module {
    private final NumberSetting range     = add(new NumberSetting("Target Range", 2, 10, 0.5, 6));
    private final NumberSetting stepDelay = add(new NumberSetting("Step Delay (ticks)", 0, 5, 1, 0));
    private final NumberSetting cooldown  = add(new NumberSetting("Cooldown (ticks)", 0, 20, 1, 4));
    private final NumberSetting minHealth = add(new NumberSetting("Min Health", 1, 20, 1, 8));
    private final BooleanSetting doubleAnchor = add(new BooleanSetting("Double Anchor", false));
    private final BooleanSetting swapBack = add(new BooleanSetting("Swap Back", true));
    private final BooleanSetting auto     = add(new BooleanSetting("Auto Trigger", true));

    private enum Kind { PLACE, CHARGE, EXPLODE }
    private record Step(Kind kind, int idx) {}
    private record Placement(BlockPos pos, BlockHitResult hit) {}

    private final List<Placement> spots = new ArrayList<>();
    private final ArrayDeque<Step> steps = new ArrayDeque<>();
    private boolean active;
    private final List<BlockPos> pendingList = new ArrayList<>();
    private int pendingTicks;
    private int timer, cooldownLeft, prevSlot = -1;

    public AutoAnchorModule() {
        super("AutoAnchor", "Place, charge and explode anchors on a target", Category.COMBAT);
    }

    @Override public void onDisable() { if (active) finish(); }

    /** Used by Shieldbreaker's anchor follow-up. */
    public void queue(PlayerEntity target) {
        if (!active && cooldownLeft == 0 && canRun()) begin(target);
    }

    @Override public boolean ticksWhenDisabled() { return true; }

    /** Keybind: run place/charge/explode on the nearest target right now. */
    @Override public void onKeyPress() {
        if (active || mc.player == null || mc.world == null) return;
        PlayerEntity t = TargetUtil.nearest(range.get());
        if (t == null) { status("No target in range"); return; }
        cooldownLeft = 0;
        if (canRun()) begin(t);
    }

    @Override public void onTick() {
        if (!isEnabled() && !active) return;   // disabled: only finish a sequence the key started
        if (!canRun()) { if (active) finish(); return; }
        if (cooldownLeft > 0) cooldownLeft--;

        // Manual assist (works with Auto Trigger OFF): you place the anchor, we charge + explode it.
        if (!pendingList.isEmpty()) {
            for (BlockPos pp : pendingList) {
                if (mc.world.getBlockState(pp).isOf(Blocks.RESPAWN_ANCHOR)) {
                    if (!active) beginManual(pp);
                    pendingList.clear();
                    break;
                }
            }
            if (!pendingList.isEmpty() && --pendingTicks <= 0) pendingList.clear();
        }

        if (!active && auto.get() && cooldownLeft == 0) {
            PlayerEntity t = TargetUtil.nearest(range.get());
            if (t != null) begin(t);
        }
        if (active) run();
    }

    private boolean canRun() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return false;
        if (mc.world.getDimension().respawnAnchorWorks()) { status("Anchors don't explode in this dimension"); return false; }
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < minHealth.get()) { status("Health below Min Health"); return false; }
        return true;
    }

    private int count(Item item) {
        int n = 0;
        for (int i = 0; i < 9; i++) {
            var st = mc.player.getInventory().getStack(i);
            if (st.isOf(item)) n += st.getCount();
        }
        return n;
    }

    @Override public void onRightClick() {
        if (mc.player == null || !mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;
        if (mc.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            pendingList.clear();
            pendingList.add(b.getBlockPos().offset(b.getSide()));   // placed against the clicked face
            pendingList.add(b.getBlockPos());                        // or replaced the clicked (replaceable) block
            pendingTicks = 10;                                       // watch for ~half a second
        }
    }

    /** Uses the anchor like a player would, even while sneaking (sneaking would otherwise place a block instead). */
    private boolean useAnchor(BlockHitResult hit) {
        boolean sneaking = mc.player.isSneaking();
        if (sneaking) {
            PacketUtil.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            mc.player.setSneaking(false);
        }
        boolean ok = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit).isAccepted();
        if (sneaking) {
            PacketUtil.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            mc.player.setSneaking(true);
        }
        return ok;
    }

    private void beginManual(BlockPos pos) {
        spots.clear();
        spots.add(new Placement(pos, null));
        steps.clear();
        steps.add(new Step(Kind.CHARGE, 0));
        steps.add(new Step(Kind.EXPLODE, 0));
        prevSlot = SlotUtil.selected();
        timer = 0;
        active = true;
    }

    private void begin(PlayerEntity target) {
        int n = doubleAnchor.get() ? 2 : 1;
        n = Math.min(n, Math.min(count(Items.RESPAWN_ANCHOR), count(Items.GLOWSTONE)));
        if (n <= 0) { status("Put a Respawn Anchor AND Glowstone in your hotbar"); return; }

        spots.clear();
        spots.addAll(findPlacements(target, n));
        if (spots.isEmpty()) { status("No valid anchor spot next to the target (out of reach?)"); return; }

        steps.clear();
        for (int i = 0; i < spots.size(); i++) { steps.add(new Step(Kind.PLACE, i)); steps.add(new Step(Kind.CHARGE, i)); }
        for (int i = 0; i < spots.size(); i++) steps.add(new Step(Kind.EXPLODE, i));
        prevSlot = SlotUtil.selected();
        timer = 0;
        active = true;
    }

    private void run() {
        for (int guard = 0; guard < 12 && !steps.isEmpty(); guard++) {
            if (timer > 0) { timer--; return; }
            if (!exec(steps.peek())) { finish(); return; }
            steps.poll();
            timer = stepDelay.getInt();
            if (timer > 0) return;
        }
        if (steps.isEmpty()) finish();
    }

    private boolean exec(Step s) {
        Placement p = spots.get(s.idx());
        switch (s.kind()) {
            case PLACE -> {
                int slot = SlotUtil.find(st -> st.isOf(Items.RESPAWN_ANCHOR));
                if (slot < 0) return false;
                SlotUtil.swap(slot);
                ActionResult r = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, p.hit());
                if (r.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
                return r.isAccepted();
            }
            case CHARGE -> {
                int slot = SlotUtil.find(st -> st.isOf(Items.GLOWSTONE));
                if (slot < 0) return false;
                SlotUtil.swap(slot);
                return useAnchor(anchorHit(p));
            }
            default -> {
                int slot = safeSlot();
                if (slot < 0) return false;
                SlotUtil.swap(slot); // non-glowstone item in hand -> charged anchor explodes
                useAnchor(anchorHit(p));
                return true;
            }
        }
    }

    private BlockHitResult anchorHit(Placement p) {
        return new BlockHitResult(Vec3d.ofCenter(p.pos()), Direction.UP, p.pos(), false);
    }

    private int safeSlot() {
        for (int i = 0; i < 9; i++) {
            var st = mc.player.getInventory().getStack(i);
            if (!st.isOf(Items.GLOWSTONE) && !st.isOf(Items.RESPAWN_ANCHOR)) return i;
        }
        if (prevSlot >= 0 && !isGlow(prevSlot)) return prevSlot;
        for (int i = 0; i < 9; i++) if (!isGlow(i)) return i;
        return -1;
    }

    private boolean isGlow(int slot) { return mc.player.getInventory().getStack(slot).isOf(Items.GLOWSTONE); }

    private void finish() {
        if (swapBack.get() && prevSlot >= 0 && mc.player != null) SlotUtil.swap(prevSlot);
        prevSlot = -1; active = false; steps.clear(); spots.clear();
        cooldownLeft = cooldown.getInt();
    }

    /** Free blocks around the target that we can click against. Works in open ground (uses the floor). */
    private List<Placement> findPlacements(PlayerEntity t, int n) {
        BlockPos feet = t.getBlockPos();
        List<BlockPos> cand = new ArrayList<>();
        for (Direction d : Direction.Type.HORIZONTAL) cand.add(feet.offset(d));           // beside feet (floor support)
        cand.add(feet.up(2));                                                             // above head
        for (Direction d : Direction.Type.HORIZONTAL) cand.add(feet.up(1).offset(d));     // beside head

        Vec3d eye = mc.player.getEyePos();
        List<Placement> out = new ArrayList<>();
        for (BlockPos c : cand) {
            if (out.size() >= n) break;
            if (!mc.world.getBlockState(c).isReplaceable()) continue;
            if (eye.squaredDistanceTo(Vec3d.ofCenter(c)) > 4.5 * 4.5) continue;
            if (!mc.world.getOtherEntities(null, new Box(c), e -> !e.isSpectator()).isEmpty()) continue;
            BlockHitResult hit = support(c);
            if (hit != null) out.add(new Placement(c, hit));
        }
        return out;
    }

    private BlockHitResult support(BlockPos c) {
        for (Direction d : Direction.values()) {
            BlockPos n = c.offset(d);
            var st = mc.world.getBlockState(n);
            if (st.isReplaceable() || !st.isSolidBlock(mc.world, n)) continue;
            Direction face = d.getOpposite();
            Vec3d hitPos = Vec3d.ofCenter(n).add(Vec3d.of(face.getVector()).multiply(0.5));
            return new BlockHitResult(hitPos, face, n, false);
        }
        return null;
    }
}

public static final class AutoArmorModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 0, 10, 1, 2));
    private int timer;

    public AutoArmorModule() { super("AutoArmor", "Equips the best armor", Category.INVENTORY); }

    private static int armorSlot(EquipmentSlot s) {
        return switch (s) { case HEAD -> 5; case CHEST -> 6; case LEGS -> 7; case FEET -> 8; default -> -1; };
    }

    private static int prot(ItemStack s) {
        return s.getItem() instanceof ArmorItem a ? a.getProtection() : -1;
    }

    @Override public void onTick() {
        if (timer-- > 0 || mc.currentScreen != null) return;
        var slots = mc.player.playerScreenHandler.slots;
        for (int id = 9; id <= 44; id++) {
            ItemStack cand = slots.get(id).getStack();
            if (!(cand.getItem() instanceof ArmorItem a)) continue;
            int target = armorSlot(a.getType().getEquipmentSlot());
            if (target < 0) continue;
            ItemStack worn = slots.get(target).getStack();
            if (prot(cand) <= prot(worn)) continue;
            int sync = mc.player.playerScreenHandler.syncId;
            if (worn.isEmpty()) {
                mc.interactionManager.clickSlot(sync, id, 0, SlotActionType.QUICK_MOVE, mc.player);
            } else {
                mc.interactionManager.clickSlot(sync, id, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(sync, target, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(sync, id, 0, SlotActionType.PICKUP, mc.player);
            }
            timer = delay.getInt();
            return;
        }
    }
}

public static final class AutoClickerModule extends Module {
    private final NumberSetting cps = add(new NumberSetting("CPS", 1, 20, 1, 10));
    private long last;

    public AutoClickerModule() { super("AutoClicker", "Attacks for you while LMB is held", Category.COMBAT); }

    @Override public void onFrame() {
        if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult h)) return;
        long now = System.currentTimeMillis();
        if (now - last < 1000L / cps.getInt()) return;
        last = now;
        mc.interactionManager.attackEntity(mc.player, h.getEntity());
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}

/** Reels in when the bobber dips, then recasts. Bite detection is a velocity heuristic - tune if needed. */
public static final class AutoFishModule extends Module {
    private final NumberSetting recast = add(new NumberSetting("Recast Delay (ticks)", 5, 40, 1, 15));
    private int wait;

    public AutoFishModule() { super("AutoFish", "Automatic fishing", Category.UTILITY); }

    @Override public void onTick() {
        if (!mc.player.getMainHandStack().isOf(Items.FISHING_ROD)) return;
        if (wait > 0) { if (--wait == 0) cast(); return; }
        FishingBobberEntity hook = mc.player.fishHook;
        if (hook != null && hook.isTouchingWater() && hook.age > 40 && hook.getVelocity().y < -0.08) {
            cast();               // reel in
            wait = recast.getInt();
        }
    }

    private void cast() {
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}

public static final class AutoToolModule extends Module {
    private int prev = -1;
    public AutoToolModule() { super("AutoTool", "Best tool when mining", Category.UTILITY); }

    @Override public void onTick() {
        if (mc.currentScreen == null && mc.options.attackKey.isPressed()
                && mc.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            if (prev < 0) prev = SlotUtil.selected();
            SlotUtil.swap(SlotUtil.bestTool(mc.world.getBlockState(b.getBlockPos())));
        } else if (prev >= 0) { SlotUtil.swap(prev); prev = -1; }
    }

    @Override public void onDisable() { if (prev >= 0 && mc.player != null) SlotUtil.swap(prev); prev = -1; }
}

/** Keeps a totem in your offhand. "Delay" is how many ms after the offhand empties before it re-equips. */
public static final class AutoTotemModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ms)", 0, 1000, 10, 120));
    private long missingSince;

    public AutoTotemModule() { super("AutoTotem", "Re-equips a totem after a set delay", Category.INVENTORY); }

    @Override public void onFrame() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) { missingSince = 0; return; }
        long now = System.currentTimeMillis();
        if (missingSince == 0) { missingSince = now; return; }
        if (now - missingSince < delay.getInt()) return;

        var slots = mc.player.playerScreenHandler.slots;
        for (int id = 9; id <= 44; id++) {
            if (!slots.get(id).getStack().isOf(Items.TOTEM_OF_UNDYING)) continue;
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, id, 40, SlotActionType.SWAP, mc.player);
            missingSince = 0;
            return;
        }
    }
}

/** Holds ALL movement packets until you turn it off. */
public static final class BlinkModule extends Module {
    public BlinkModule() { super("Blink", "Freeze your position for the server", Category.NETWORK); }
    @Override public void onEnable()  { FakeLagModule.setBlink(true); }
    @Override public void onDisable() { FakeLagModule.setBlink(false); }
}

/** Surrounds you with blocks (feet, head, and above). */
public static final class BlockInModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 0, 5, 1, 1));
    private int timer;

    public BlockInModule() { super("BlockIn", "Cover yourself in blocks", Category.WORLD); }

    @Override public void onTick() {
        if (timer-- > 0 || mc.currentScreen != null) return;
        BlockPos f = mc.player.getBlockPos();
        for (Direction d : Direction.Type.HORIZONTAL) {
            if (PlaceUtil.place(f.offset(d)) || PlaceUtil.place(f.up().offset(d))) { timer = delay.getInt(); return; }
        }
        if (PlaceUtil.place(f.up(2))) timer = delay.getInt();
    }
}

/**
 * Attribute swap: on your click, the server receives  [swap to Breach mace] -> [attack] -> [swap back to sword]
 * inside one tick. The vanilla attack that would follow is cancelled (MinecraftClientMixin), so you land
 * exactly one mace hit and keep the sword in hand.
 */
public static final class BreachSwapModule extends Module {
    private static boolean cancelNext;
    private final BooleanSetting onlySword     = add(new BooleanSetting("Only When Holding Sword", true));
    private final BooleanSetting requireBreach = add(new BooleanSetting("Require Breach Enchant", true));

    public BreachSwapModule() { super("BreachSwap", "Hit with Breach mace, keep the sword in hand", Category.COMBAT); }

    public static boolean consumeCancel() { boolean r = cancelNext; cancelNext = false; return r; }

    @Override public void onTick() { cancelNext = false; }   // safety: never carry the flag over a tick
    @Override public void onDisable() { cancelNext = false; }

    @Override public void onLeftClick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) return;
        if (onlySword.get() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) return;

        int mace = SlotUtil.find(s -> s.isOf(Items.MACE) && (!requireBreach.get() || hasBreach(s)));
        if (mace < 0) { status(requireBreach.get() ? "No Breach mace in hotbar" : "No mace in hotbar"); return; }
        if (mace == SlotUtil.selected()) return;

        int prev = SlotUtil.selected();
        SlotUtil.swap(mace);
        PacketUtil.attack(target);
        PacketUtil.swing();
        SlotUtil.swap(prev);
        mc.player.resetLastAttackedTicks();
        cancelNext = true;
    }

    private static boolean hasBreach(ItemStack stack) {
        ItemEnchantmentsComponent e = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> en : e.getEnchantments()) if (en.matchesKey(Enchantments.BREACH)) return true;
        return false;
    }
}

/** Glow outline on other players (client-side glowing flag). */
public static final class ChamsModule extends Module {
    public ChamsModule() { super("Chams", "Glow outline on players", Category.RENDER); }

    @Override public void onTick() {
        for (PlayerEntity p : mc.world.getPlayers()) if (p != mc.player) p.setGlowing(true);
    }

    @Override public void onDisable() {
        if (mc.world != null) for (PlayerEntity p : mc.world.getPlayers()) p.setGlowing(false);
    }
}

public static final class ChestStealModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 0, 10, 1, 1));
    private int timer;

    public ChestStealModule() { super("ChestSteal", "Takes everything from an open chest", Category.INVENTORY); }

    @Override public void onTick() {
        if (!(mc.currentScreen instanceof GenericContainerScreen gs)) return;
        if (timer-- > 0) return;
        GenericContainerScreenHandler h = gs.getScreenHandler();
        for (int i = 0; i < h.getRows() * 9; i++) {
            if (!h.getSlot(i).hasStack()) continue;
            mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            timer = delay.getInt();
            return;
        }
    }
}

/** Saves you from a fall: water bucket MLG right before landing (then picks it back up), or a block under you. */
public static final class ClutchModule extends Module {
        private final NumberSetting minFall = add(new NumberSetting("Min Fall", 2, 20, 1, 4));
        private final BooleanSetting water  = add(new BooleanSetting("Water Bucket", true));
        private final BooleanSetting webs   = add(new BooleanSetting("Cobweb / Slime / Hay", true));
        private final BooleanSetting pickup = add(new BooleanSetting("Pick Water Back Up", true));
        private boolean placedWater;
        private int cooldown, pickupWait, timeout;

        public ClutchModule() { super("Clutch", "Water bucket / cobweb clutch when falling", Category.UTILITY); }

        @Override public void onDisable() { placedWater = false; }

        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
            if (cooldown > 0) cooldown--;
            if (placedWater) { handlePickup(); return; }

            if (p.isOnGround() || p.isFallFlying() || p.isTouchingWater() || p.isClimbing()
                    || (p.getAbilities().allowFlying && p.getAbilities().flying)) return;
            if (p.fallDistance < minFall.get() || p.getVelocity().y > -0.1 || cooldown > 0) return;

            // Exactly what the server will check: a ray straight down from the eyes, 4.3 blocks (reach is 4.5).
            Vec3d eye = p.getEyePos();
            BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, eye.add(0, -4.3, 0),
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
            if (hit.getType() != HitResult.Type.BLOCK) return;       // ground not in reach yet - try again next tick

            int bucket = SlotUtil.find(s -> s.isOf(Items.WATER_BUCKET));
            if (water.get() && bucket >= 0) {
                useDown(bucket);
                placedWater = true; pickupWait = 0; timeout = 60; cooldown = 4;
                return;
            }
            if (!webs.get()) return;
            int slot = SlotUtil.find(s -> s.isOf(Items.COBWEB));
            if (slot < 0) slot = SlotUtil.find(s -> s.isOf(Items.SLIME_BLOCK));
            if (slot < 0) slot = SlotUtil.find(s -> s.isOf(Items.HAY_BLOCK));
            if (slot < 0) { status("Clutch: no water bucket / cobweb / slime / hay in hotbar"); return; }
            int prev = SlotUtil.selected();
            SlotUtil.swap(slot);
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND,
                    new BlockHitResult(hit.getPos(), Direction.UP, hit.getBlockPos(), false));
            p.swingHand(Hand.MAIN_HAND);
            SlotUtil.swap(prev);
            cooldown = 4;
        }

        /** After landing in the water, scoop it back up with the (now empty) bucket. */
        private void handlePickup() {
            var p = mc.player;
            if (--timeout <= 0 || !pickup.get()) { placedWater = false; return; }
            if (!p.isOnGround() && !p.isTouchingWater()) return;     // still falling
            if (++pickupWait < 2) return;                            // let the server settle the water
            int b = SlotUtil.find(s -> s.isOf(Items.BUCKET));
            if (b >= 0) useDown(b);
            placedWater = false;
        }

        private void useDown(int slot) {
            var p = mc.player;
            int prev = SlotUtil.selected();
            float op = p.getPitch();
            SlotUtil.swap(slot);
            p.setPitch(90f);                                        // the use packet carries this pitch
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            p.setPitch(op);
            p.swingHand(Hand.MAIN_HAND);
            SlotUtil.swap(prev);
        }
    }

public static final class CoordsModule extends HudElement {
    public CoordsModule() { super("Coordinates", "XYZ position", 6, 164); }
    @Override public int width() { return 128; }
    @Override public int height() { return 18; }

    @Override public void draw(DrawContext c, int x, int y) {
        if (mc.player == null) return;
        c.fill(x, y, x + width(), y + height(), 0xA0000000);
        RenderUtil.outline(c, x, y, x + width(), y + height(), 0xFF000000);
        RenderUtil.text(c, "XYZ  " + (int) Math.floor(mc.player.getX()) + "  " + (int) Math.floor(mc.player.getY())
                + "  " + (int) Math.floor(mc.player.getZ()), x + 6, y + 5, RenderUtil.TEXT, 0.85f);
    }
}

public static final class CpsModule extends HudElement {
    private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
    public CpsModule() { super("CPS", "Clicks per second", 6, 140); }
    @Override public int width() { return 74; }
    @Override public int height() { return 18; }

    private static int count(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    @Override public void onLeftClick()  { left.addLast(System.currentTimeMillis()); }
    @Override public void onRightClick() { right.addLast(System.currentTimeMillis()); }

    @Override public void draw(DrawContext c, int x, int y) {
        c.fill(x, y, x + width(), y + height(), 0xA0000000);
        RenderUtil.outline(c, x, y, x + width(), y + height(), 0xFF000000);
        RenderUtil.text(c, "CPS  " + count(left) + " | " + count(right), x + 6, y + 5, RenderUtil.TEXT, 0.85f);
    }
}

/**
 * Places crystals on obsidian/bedrock near the target and detonates them.
 * Damage is estimated (armor aware, no line-of-sight) - tune Min Damage / Max Self Damage.
 * Action bar tells you why nothing is happening (no obsidian, no crystals, too dangerous...).
 */
public static final class CrystalAuraModule extends Module {
    private final NumberSetting targetRange = add(new NumberSetting("Target Range", 3, 12, 0.5, 9));
    private final NumberSetting placeRange  = add(new NumberSetting("Place Range", 1, 6, 0.1, 4.5));
    private final NumberSetting breakRange  = add(new NumberSetting("Break Range", 1, 6, 0.1, 4.5));
    private final NumberSetting wallRange   = add(new NumberSetting("Walls Range", 1, 6, 0.1, 3.0));
    private final NumberSetting placeDelay  = add(new NumberSetting("Place Delay (ticks)", 0, 10, 1, 0));
    private final NumberSetting breakDelay  = add(new NumberSetting("Break Delay (ticks)", 0, 10, 1, 0));
    private final NumberSetting minDamage   = add(new NumberSetting("Min Damage", 1, 36, 1, 4));
    private final NumberSetting maxSelf     = add(new NumberSetting("Max Self Damage", 1, 36, 1, 10));
    private final BooleanSetting place      = add(new BooleanSetting("Place", true));
    private final BooleanSetting breakIt    = add(new BooleanSetting("Break", true));
    private final BooleanSetting mobs       = add(new BooleanSetting("Target Mobs", true));
    private final BooleanSetting autoSwap   = add(new BooleanSetting("Auto Swap", true));
    private final BooleanSetting antiSuicide = add(new BooleanSetting("Anti Suicide", true));

    private int placeTimer, breakTimer;

    public CrystalAuraModule() { super("CrystalAura", "Auto place and break crystals", Category.COMBAT); }

    @Override public void onTick() {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        LivingEntity t = TargetUtil.nearestLiving(targetRange.get(), mobs.get());
        if (t == null) return;
        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        if (breakIt.get() && breakTimer == 0) doBreak(t);
        if (place.get() && placeTimer == 0) doPlace(t);
    }

    private double hp() { return mc.player.getHealth() + mc.player.getAbsorptionAmount(); }

    /** Fraction of 9 sample points (corners + centre) of the entity the blast can reach. */
    private static double exposure(Vec3d crystal, LivingEntity e) {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        Box b = e.getBoundingBox();
        double[] xs = { b.minX, b.maxX }, ys = { b.minY, b.maxY }, zs = { b.minZ, b.maxZ };
        int seen = 0;
        for (double x : xs) for (double y : ys) for (double z : zs)
            if (mc.world.raycast(new RaycastContext(crystal, new Vec3d(x, y, z), RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, e)).getType() == HitResult.Type.MISS) seen++;
        if (mc.world.raycast(new RaycastContext(crystal, b.getCenter(), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, e)).getType() == HitResult.Type.MISS) seen++;
        return seen / 9.0;
    }

    private static double damage(Vec3d crystal, LivingEntity e) {
        double w = Math.sqrt(e.getBoundingBox().getCenter().squaredDistanceTo(crystal)) / 12.0;
        if (w > 1) return 0;
        double a = (1.0 - w) * exposure(crystal, e);
        double raw = (int) ((a * a + a) / 2.0 * 7.0 * 12.0 + 1.0);
        double armor = e.getArmor(), tough = e.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        double f = MathHelper.clamp(armor - raw / (2.0 + tough / 4.0), armor * 0.2, 20.0);
        return raw * (1.0 - f / 25.0);
    }

    private boolean safe(double selfDamage) {
        return selfDamage <= maxSelf.get() && (!antiSuicide.get() || selfDamage < hp() - 1);
    }

    private boolean canSee(Vec3d to) {
        return mc.world.raycast(new RaycastContext(mc.player.getEyePos(), to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }

    private void doBreak(LivingEntity t) {
        EndCrystalEntity best = null; double bestScore = -1e9;
        double r = breakRange.get();
        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                mc.player.getBoundingBox().expand(r + 1), e -> e.isAlive())) {
            double d = mc.player.distanceTo(c);
            if (d > r) continue;
            if (d > wallRange.get() && !canSee(c.getPos().add(0, 1, 0))) continue;   // far crystals need line of sight
            Vec3d pos = c.getPos();
            double td = damage(pos, t);
            if (td < minDamage.get()) continue;
            double sd = damage(pos, mc.player);
            if (!safe(sd)) continue;
            if (td - sd > bestScore) { bestScore = td - sd; best = c; }
        }
        if (best == null) return;
        PacketUtil.attack(best);
        PacketUtil.swing();
        breakTimer = breakDelay.getInt();
    }

    private void doPlace(LivingEntity t) {
        boolean offhand = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
        int slot = SlotUtil.find(s -> s.isOf(Items.END_CRYSTAL));
        if (!offhand && slot < 0) { status("No end crystals in hotbar/offhand"); return; }

        BlockPos tp = t.getBlockPos(), bestBase = null;
        double bestScore = -1e9;
        int bases = 0;
        Vec3d eye = mc.player.getEyePos();
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) for (int dy = -3; dy <= 2; dy++) {
            BlockPos base = tp.add(dx, dy, dz);
            var st = mc.world.getBlockState(base);
            if (!st.isOf(Blocks.OBSIDIAN) && !st.isOf(Blocks.BEDROCK)) continue;
            bases++;
            Vec3d crystal = new Vec3d(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
            if (eye.distanceTo(crystal) > placeRange.get()) continue;
            if (!mc.world.getBlockState(base.up()).isAir() || !mc.world.getBlockState(base.up(2)).isAir()) continue;
            Box box = new Box(base.getX(), base.getY() + 1, base.getZ(), base.getX() + 1, base.getY() + 3, base.getZ() + 1);
            if (!mc.world.getOtherEntities(null, box).isEmpty()) continue;
            double td = damage(crystal, t);
            if (td < minDamage.get()) continue;
            double sd = damage(crystal, mc.player);
            if (!safe(sd)) continue;
            if (td - sd > bestScore) { bestScore = td - sd; bestBase = base; }
        }
        if (bestBase == null) { if (bases == 0) status("No obsidian/bedrock near the target"); return; }

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        int prev = SlotUtil.selected();
        if (!offhand) {
            if (!autoSwap.get() && prev != slot) return;
            SlotUtil.swap(slot);
        }
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(bestBase).add(0, 0.5, 0), Direction.UP, bestBase, false);
        if (mc.interactionManager.interactBlock(mc.player, hand, hit).isAccepted()) mc.player.swingHand(hand);
        if (!offhand) SlotUtil.swap(prev);
        placeTimer = placeDelay.getInt();
    }
}

/** Through-wall boxes for players, dropped items and storage blocks. */
public static final class EspModule extends Module {
    private final BooleanSetting players = add(new BooleanSetting("Players", true));
    private final BooleanSetting items   = add(new BooleanSetting("Items", true));
    private final BooleanSetting chests  = add(new BooleanSetting("Chests", true));
    private final BooleanSetting tracers = add(new BooleanSetting("Tracers", false));
    private final NumberSetting range    = add(new NumberSetting("Range", 16, 128, 8, 64));

    private record Target(Box box, int color) {}
    private final List<Target> storage = new ArrayList<>();
    private int scanTimer;

    public EspModule(String name, boolean p, boolean i, boolean c) {
        super(name, "Through-wall boxes", Category.RENDER);
        players.set(p); items.set(i); chests.set(c); tracers.set(c && !p);
    }

    @Override public void onTick() {
        if (!chests.get()) { storage.clear(); return; }
        if (scanTimer-- > 0) return;
        scanTimer = 20;
        storage.clear();
        ChunkPos cp = mc.player.getChunkPos();
        int r = Math.min(8, mc.options.getViewDistance().getValue());
        double rsq = range.get() * range.get();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            WorldChunk ch = mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
            if (ch == null) continue;
            for (BlockEntity be : ch.getBlockEntities().values()) {
                int color;
                if (be instanceof ChestBlockEntity) color = 0xFFFFA500;
                else if (be instanceof EnderChestBlockEntity) color = 0xFFB400FF;
                else if (be instanceof BarrelBlockEntity) color = 0xFFC8A064;
                else if (be instanceof ShulkerBoxBlockEntity) color = 0xFFFF55FF;
                else continue;
                if (mc.player.squaredDistanceTo(Vec3d.ofCenter(be.getPos())) > rsq) continue;
                storage.add(new Target(new Box(be.getPos()).contract(0.06), color));
            }
        }
    }

    /** Green when healthy, red when low. */
    private static int healthColor(PlayerEntity p) {
        float f = Math.max(0f, Math.min(1f, (p.getHealth() + p.getAbsorptionAmount()) / p.getMaxHealth()));
        return 0xFF000000 | ((int) (255 * (1 - f)) << 16) | ((int) (255 * f) << 8) | 0x40;
    }

    @Override public void onWorldRender(WorldRenderContext ctx) {
        float td = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        double rsq = range.get() * range.get();
        Render3D.begin();
        for (Entity e : mc.world.getEntities()) {
            boolean isPlayer = e instanceof PlayerEntity && e.isAlive()
                    && (e != mc.player || mc.getCameraEntity() != mc.player);
            boolean isItem = e instanceof ItemEntity;
            if (!((isPlayer && players.get()) || (isItem && items.get()))) continue;
            if (e.squaredDistanceTo(mc.player) > rsq) continue;
            Box b = e.getBoundingBox().offset(e.getLerpedPos(td).subtract(e.getPos()));
            int col = isPlayer ? healthColor((PlayerEntity) e) : 0xFF3BE8FF;
            Render3D.box(ctx.matrixStack(), cam, b, col);
            Render3D.corners(ctx.matrixStack(), cam, b, 0xFFFFFFFF);
        }
        Vec3d tracerStart = cam.add(Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(0.5));
        for (Target t : storage) {
            Render3D.box(ctx.matrixStack(), cam, t.box(), t.color());
            if (tracers.get()) Render3D.line(ctx.matrixStack(), cam, tracerStart, t.box().getCenter(), t.color());
        }
        Render3D.end();
    }
}

/** Holds your movement packets for N ticks, then releases them all at once (ClientPlayNetworkHandlerMixin). */
public static final class FakeLagModule extends Module {
    private static boolean active, flushing, blink;
    private static final List<Packet<?>> QUEUE = new ArrayList<>();

    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 20, 1, 4));
    private int ticks;

    public FakeLagModule() { super("FakeLag", "Delays your movement packets in bursts", Category.NETWORK); }

    /** Returns true if the packet was captured (and must not be sent now). */
    public static boolean intercept(Packet<?> p) {
        if ((!active && !blink) || flushing || !(p instanceof PlayerMoveC2SPacket)) return false;
        QUEUE.add(p);
        if (QUEUE.size() > 200) flush();   // never hold more than ~10s of movement
        return true;
    }

    public static void setBlink(boolean b) { blink = b; if (!b) flush(); }

    private static void flush() {
        var h = net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler();
        flushing = true;
        try { if (h != null) for (Packet<?> p : QUEUE) h.sendPacket(p); }
        finally { flushing = false; QUEUE.clear(); }
    }

    @Override public void onEnable()  { active = true; ticks = 0; }
    @Override public void onDisable() { active = false; flush(); }

    @Override public void onTick() {
        if (++ticks >= delay.getInt()) { flush(); ticks = 0; }
    }
}

public static final class FastBowModule extends Module {
    private final NumberSetting charge = add(new NumberSetting("Charge Ticks", 3, 20, 1, 3));

    public FastBowModule() { super("FastBow", "Release arrows early and re-draw", Category.COMBAT); }

    @Override public void onTick() {
        if (!mc.player.getMainHandStack().isOf(Items.BOW) || !mc.options.useKey.isPressed()) return;
        if (mc.player.isUsingItem() && mc.player.getItemUseTime() >= charge.getInt()) {
            PacketUtil.send(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            mc.player.stopUsingItem();
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
    }
}

/** Removes the 4-tick right-click delay (MinecraftClientMixin#doItemUse). */
public static final class FastPlaceModule extends Module {
    public static boolean ACTIVE;
    public FastPlaceModule() { super("FastPlace", "No delay between placements", Category.WORLD); }
    @Override public void onEnable()  { ACTIVE = true; }
    @Override public void onDisable() { ACTIVE = false; }
}

/** Hold RMB with XP bottles in hand: throws several per tick instead of one every 4 ticks. */
public static final class FastXpModule extends Module {
    private final NumberSetting throwsPerTick = add(new NumberSetting("Throws / Tick", 1, 8, 1, 3));

    public FastXpModule() { super("FastXP", "Throw XP bottles much faster while holding RMB", Category.UTILITY); }

    @Override public void onTick() {
        if (mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
        if (!mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) return;
        for (int i = 0; i < throwsPerTick.getInt(); i++) {
            if (!mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) break;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}

public static final class FovModule extends Module {
    private final NumberSetting fov = add(new NumberSetting("FOV", 30, 110, 1, 100));
    private int original = 70;

    public FovModule() { super("FOV", "Change your field of view", Category.RENDER); }

    @Override public void onEnable()  { original = mc.options.getFov().getValue(); }
    @Override public void onDisable() { if (mc.options != null) mc.options.getFov().setValue(original); }
    @Override public void onTick()    { mc.options.getFov().setValue(fov.getInt()); }
}

/**
 * Spectator-style free camera: you fly through blocks with smooth acceleration while your real
 * player stands still (input blanked, rotation pinned). Camera is overridden in CameraMixin so the
 * HUD, hotbar and hands stay yours.
 */
public static final class FreecamModule extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 0.5, 10, 0.5, 3.0));

    private Input savedInput;
    private float savedYaw, savedPitch;
    private Vec3d vel = Vec3d.ZERO;
    private long last;

    public FreecamModule() { super("Freecam", "Spectator-style camera", Category.RENDER); }

    @Override public void onEnable() {
        if (mc.player == null) { setEnabled(false); return; }
        savedYaw = CameraControl.yaw = mc.player.getYaw();
        savedPitch = CameraControl.pitch = mc.player.getPitch();
        Vec3d eye = mc.player.getEyePos();
        CameraControl.x = eye.x; CameraControl.y = eye.y; CameraControl.z = eye.z;
        CameraControl.active = true;
        savedInput = mc.player.input;
        mc.player.input = new Input();
        vel = Vec3d.ZERO;
        last = System.nanoTime();
    }

    @Override public void onDisable() {
        CameraControl.active = false;
        if (mc.player == null) return;
        if (savedInput != null) mc.player.input = savedInput;
        mc.player.setYaw(savedYaw);
        mc.player.setPitch(savedPitch);
    }

    @Override public void onFrame() {
        if (!CameraControl.active || mc.player == null) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;

        // Mouse turned the real player: move that rotation onto the camera, pin the player.
        CameraControl.yaw += mc.player.getYaw() - savedYaw;
        CameraControl.pitch = MathHelper.clamp(CameraControl.pitch + mc.player.getPitch() - savedPitch, -90f, 90f);
        mc.player.setYaw(savedYaw); mc.player.setPitch(savedPitch);
        mc.player.prevYaw = savedYaw; mc.player.prevPitch = savedPitch;

        double y = Math.toRadians(CameraControl.yaw), p = Math.toRadians(CameraControl.pitch);
        Vec3d fwd = new Vec3d(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
        Vec3d left = new Vec3d(Math.cos(y), 0, Math.sin(y));
        Vec3d wish = Vec3d.ZERO;
        if (mc.currentScreen == null) {
            if (mc.options.forwardKey.isPressed()) wish = wish.add(fwd);
            if (mc.options.backKey.isPressed())    wish = wish.subtract(fwd);
            if (mc.options.leftKey.isPressed())    wish = wish.add(left);
            if (mc.options.rightKey.isPressed())   wish = wish.subtract(left);
            if (mc.options.jumpKey.isPressed())    wish = wish.add(0, 1, 0);
            if (mc.options.sneakKey.isPressed())   wish = wish.add(0, -1, 0);
        }
        Vec3d target = wish.lengthSquared() > 0 ? wish.normalize().multiply(speed.get() * 6.0) : Vec3d.ZERO;
        vel = vel.lerp(target, 1.0 - Math.exp(-dt * 9.0));
        CameraControl.x += vel.x * dt; CameraControl.y += vel.y * dt; CameraControl.z += vel.z * dt;
    }
}

/** Orbit camera around you while your real aim stays put. */
public static final class FreelookModule extends Module {
    private final NumberSetting distance = add(new NumberSetting("Distance", 1, 8, 0.5, 3.5));
    private float savedYaw, savedPitch;

    public FreelookModule() { super("Freelook", "Look around without turning", Category.RENDER); }

    @Override public void onEnable() {
        if (mc.player == null) { setEnabled(false); return; }
        savedYaw = CameraControl.yaw = mc.player.getYaw();
        savedPitch = CameraControl.pitch = mc.player.getPitch();
        CameraControl.active = true;
    }

    @Override public void onDisable() {
        CameraControl.active = false;
        if (mc.player != null) { mc.player.setYaw(savedYaw); mc.player.setPitch(savedPitch); }
    }

    @Override public void onFrame() {
        if (!CameraControl.active || mc.player == null) return;
        CameraControl.yaw += mc.player.getYaw() - savedYaw;
        CameraControl.pitch = MathHelper.clamp(CameraControl.pitch + mc.player.getPitch() - savedPitch, -90f, 90f);
        mc.player.setYaw(savedYaw); mc.player.setPitch(savedPitch);
        mc.player.prevYaw = savedYaw; mc.player.prevPitch = savedPitch;

        float td = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d eye = mc.player.getLerpedPos(td).add(0, mc.player.getStandingEyeHeight(), 0);
        double y = Math.toRadians(CameraControl.yaw), p = Math.toRadians(CameraControl.pitch);
        Vec3d fwd = new Vec3d(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
        Vec3d pos = eye.subtract(fwd.multiply(distance.get()));
        CameraControl.x = pos.x; CameraControl.y = pos.y; CameraControl.z = pos.z;
    }
}

public static final class FullbrightModule extends Module {
    private boolean added;
    public FullbrightModule() { super("Fullbright", "See in the dark", Category.RENDER); }

    @Override public void onTick() {
        if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) return;
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 100000, 0, false, false));
        added = true;
    }

    @Override public void onDisable() {
        if (added && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        added = false;
    }
}

/** Target HUD: name, health bar and distance of whoever you aim at (or the nearest player). */
public static final class HealthModule extends Module {
    public HealthModule() { super("Health", "Target health display", Category.RENDER); }

    @Override public void onHudRender(DrawContext c) {
        if (mc.world == null || mc.player == null || mc.currentScreen != null || mc.options.hudHidden) return;
        LivingEntity t = (mc.crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof LivingEntity le) ? le
                : TargetUtil.nearest(8);
        if (t == null || !t.isAlive()) return;
        int w = 120, x = c.getScaledWindowWidth() / 2 + 30, y = c.getScaledWindowHeight() / 2 + 24;
        c.fill(x, y, x + w, y + 34, 0xC0000000);
        RenderUtil.outline(c, x, y, x + w, y + 34, RenderUtil.BORDER);
        RenderUtil.text(c, t.getName().getString(), x + 6, y + 5, RenderUtil.TEXT, 0.9f);
        String d = Math.round(mc.player.distanceTo(t)) + "m";
        RenderUtil.text(c, d, x + w - 6 - RenderUtil.width(d, 0.8f), y + 6, RenderUtil.DIM, 0.8f);
        float f = MathHelper.clamp(t.getHealth() / t.getMaxHealth(), 0f, 1f);
        c.fill(x + 6, y + 21, x + w - 6, y + 27, RenderUtil.TRACK);
        c.fill(x + 6, y + 21, x + 6 + Math.round((w - 12) * f), y + 27, f > 0.5f ? RenderUtil.ACCENT : 0xFFFF5555);
        RenderUtil.text(c, Math.round(t.getHealth()) + " hp", x + 6, y + 12, RenderUtil.DIM, 0.7f);
    }
}

/** Cancels your swings at players until the attack cooldown is high enough (MinecraftClientMixin#doAttack). */
public static final class HitSelectModule extends Module {
    public static boolean ACTIVE;
    private static double min = 0.9;
    private final NumberSetting minCooldown = add(new NumberSetting("Min Cooldown", 0.1, 1.0, 0.05, 0.9));

    public HitSelectModule() { super("HitSelect", "Only hit when your cooldown is ready", Category.COMBAT); }

    public static boolean shouldCancel() {
        MinecraftClient c = MinecraftClient.getInstance();
        return ACTIVE && c.player != null && c.crosshairTarget instanceof EntityHitResult
                && c.player.getAttackCooldownProgress(0f) < min;
    }

    @Override public void onEnable()  { ACTIVE = true; }
    @Override public void onDisable() { ACTIVE = false; }
    @Override public void onTick()    { min = minCooldown.get(); }
}

/** Expands other players' targeting hitbox on the client (EntityMixin#getTargetingMargin). */
public static final class HitboxesModule extends Module {
    public static boolean ACTIVE;
    private static double expand;
    private final NumberSetting size = add(new NumberSetting("Expand", 0.0, 1.0, 0.05, 0.3));

    public HitboxesModule() { super("Hitboxes", "Bigger player hitboxes for aiming", Category.COMBAT); }

    public static double expand() { return expand; }

    @Override public void onEnable()  { ACTIVE = true; expand = size.get(); }
    @Override public void onDisable() { ACTIVE = false; }
    @Override public void onTick()    { expand = size.get(); }
}

/**
 * Manual hold-to-explode. Never places anything: it only detonates an End Crystal that is
 * already under the crosshair while RMB is clicked / held.
 *  - click path: MouseMixin -> onRightClick() (fires on the raw GLFW press)
 *  - hold path : onFrame() every rendered frame while useKey is down
 */
public static final class HoldExplodeModule extends Module {
    private final NumberSetting range   = add(new NumberSetting("Explode Range", 1.0, 6.0, 0.1, 4.5));
    private final NumberSetting delay   = add(new NumberSetting("Attack Delay (ticks)", 0, 10, 1, 0));
    private final NumberSetting packets = add(new NumberSetting("Packets / Burst", 1, 5, 1, 2));
    private final BooleanSetting swing  = add(new BooleanSetting("Swing Hand", true));
    private final BooleanSetting walls  = add(new BooleanSetting("Wall Check", true));

    private long lastAttackMs;

    public HoldExplodeModule() {
        super("HoldExplode", "Detonates the crystal you look at while RMB is held", Category.COMBAT);
    }

    @Override public void onRightClick() { fire(true); }

    @Override public void onFrame() {
        if (mc.options.useKey.isPressed()) fire(false);
    }

    private void fire(boolean fromClick) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.getNetworkHandler() == null) return;

        long now = System.currentTimeMillis();
        // delay 0 => every frame; otherwise N ticks (50ms each). A fresh click always bypasses the gate.
        if (!fromClick && now - lastAttackMs < delay.getInt() * 50L) return;

        EndCrystalEntity crystal = findCrystal(range.get());
        if (crystal == null) return;

        lastAttackMs = now;
        for (int i = 0; i < packets.getInt(); i++) PacketUtil.attack(crystal);
        if (swing.get()) PacketUtil.swing();
    }

    private EndCrystalEntity findCrystal(double reach) {
        Vec3d eye  = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end  = eye.add(look.multiply(reach));
        Box box    = mc.player.getBoundingBox().stretch(look.multiply(reach)).expand(1.0);

        EntityHitResult hit = ProjectileUtil.raycast(
                mc.player, eye, end, box,
                e -> e instanceof EndCrystalEntity && e.isAlive(),
                reach * reach); // squared distance
        if (hit == null) return null;

        if (walls.get()) {
            BlockHitResult block = mc.world.raycast(new RaycastContext(
                    eye, hit.getPos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (block.getType() != HitResult.Type.MISS) return null;
        }
        return (EndCrystalEntity) hit.getEntity();
    }
}

/** Base for draggable HUD mods. Position is stored in two settings and edited in the Layout editor. */
public abstract static class HudElement extends Module {
    protected final NumberSetting posX, posY;

    protected HudElement(String name, String desc, int defX, int defY) {
        super(name, desc, Category.MODS);
        posX = add(new NumberSetting("X", 0, 4000, 1, defX));
        posY = add(new NumberSetting("Y", 0, 4000, 1, defY));
    }

    public abstract int width();
    public abstract int height();
    public abstract void draw(DrawContext c, int x, int y);

    public int x() { return Math.min(posX.getInt(), Math.max(0, mc.getWindow().getScaledWidth() - width())); }
    public int y() { return Math.min(posY.getInt(), Math.max(0, mc.getWindow().getScaledHeight() - height())); }
    public void setPos(int x, int y) { posX.set(x); posY.set(y); }

    @Override public void onHudRender(DrawContext c) {
        if (mc.options.hudHidden || mc.currentScreen instanceof dev.cpvp.gui.ClickGuiScreen
                || mc.currentScreen instanceof dev.cpvp.gui.HudEditorScreen) return;
        draw(c, x(), y());
    }
}

/** ClickGUI options: theme colour (hue / saturation), scrolling and animation speed. */
public static final class InterfaceModule extends Module {
    public final NumberSetting hue         = add(new NumberSetting("Theme Hue", 0, 360, 1, 140));
    public final NumberSetting saturation  = add(new NumberSetting("Theme Saturation", 20, 100, 1, 80));
    public final NumberSetting brightness  = add(new NumberSetting("Theme Brightness", 40, 100, 1, 95));
    public final NumberSetting guiScale    = add(new NumberSetting("UI Scale", 0.5, 1.2, 0.05, 0.85));
    public final NumberSetting scrollSpeed = add(new NumberSetting("Scroll Speed", 10, 60, 2, 28));
    public final NumberSetting animSpeed   = add(new NumberSetting("Animation Speed", 6, 30, 1, 18));
    public final BooleanSetting blur       = add(new BooleanSetting("Background Blur", true));

    public InterfaceModule() {
        super("Interface", "Theme colour and GUI behaviour", Category.SETTINGS);
        setEnabled(true);
    }

    public int accent() {
        return 0xFF000000 | Color.HSBtoRGB((float) (hue.get() / 360.0), (float) (saturation.get() / 100.0), (float) (brightness.get() / 100.0));
    }
}

/** Drops obvious junk only (rotten flesh, spider eyes, poisonous potatoes...). Never touches tools, armor or blocks. */
public static final class InvCleanerModule extends Module {
    private static final Set<Item> JUNK = Set.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO,
            Items.PUFFERFISH, Items.BONE, Items.INK_SAC, Items.GLOW_INK_SAC, Items.TROPICAL_FISH);
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 20, 1, 4));
    private int timer;

    public InvCleanerModule() { super("InvCleaner", "Drops junk items", Category.INVENTORY); }

    @Override public void onTick() {
        if (timer-- > 0 || mc.currentScreen != null) return;
        var slots = mc.player.playerScreenHandler.slots;
        for (int id = 9; id <= 44; id++) {
            if (!JUNK.contains(slots.get(id).getStack().getItem())) continue;
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, id, 1, SlotActionType.THROW, mc.player);
            timer = delay.getInt();
            return;
        }
    }
}

/** Keep moving while an inventory / container / the ClickGUI is open. */
public static final class InvMoveModule extends Module {
    public InvMoveModule() { super("InvMove", "Move with menus open", Category.INVENTORY); }

    @Override public void onTick() {
        if (!(mc.currentScreen instanceof HandledScreen) && !(mc.currentScreen instanceof ClickGuiScreen)) return;
        long win = mc.getWindow().getHandle();
        for (KeyBinding kb : new KeyBinding[] { mc.options.forwardKey, mc.options.backKey, mc.options.leftKey,
                mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey }) {
            InputUtil.Key k = InputUtil.fromTranslationKey(kb.getBoundKeyTranslationKey());
            kb.setPressed(k.getCategory() == InputUtil.Type.KEYSYM && InputUtil.isKeyPressed(win, k.getCode()));
        }
    }
}

/** When you open your inventory, the mouse cursor jumps onto a totem of undying. */
public static final class InvTotemModule extends Module {
    private Screen last;
    private int wait = -1;

    public InvTotemModule() { super("InvTotem", "Cursor jumps to a totem when inventory opens", Category.INVENTORY); }

    @Override public void onTick() {
        Screen s = mc.currentScreen;
        if (s != last) { last = s; wait = (s instanceof InventoryScreen) ? 1 : -1; }
        if (wait > 0) { wait--; if (wait == 0 && s instanceof InventoryScreen inv) moveToTotem(inv); }
    }

    private void moveToTotem(InventoryScreen screen) {
        HandledScreenAccessor acc = (HandledScreenAccessor) (Object) screen;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.id < 9 || slot.id > 44 || !slot.getStack().isOf(Items.TOTEM_OF_UNDYING)) continue;
            double scale = mc.getWindow().getScaleFactor();
            double px = (acc.cpvp$getX() + slot.x + 8) * scale;
            double py = (acc.cpvp$getY() + slot.y + 8) * scale;
            GLFW.glfwSetCursorPos(mc.getWindow().getHandle(), px, py);
            ((MouseAccessor) (Object) mc.mouse).cpvp$setX(px);
            ((MouseAccessor) (Object) mc.mouse).cpvp$setY(py);
            return;
        }
    }
}

/** Jumps the tick you get hit so the knockback is cut short. */
public static final class JumpResetModule extends Module {
    public JumpResetModule() { super("JumpReset", "Jump when hit to reduce knockback", Category.COMBAT); }

    @Override public void onTick() {
        if (mc.currentScreen == null && mc.player.hurtTime >= 8 && mc.player.isOnGround()) mc.player.jump();
    }
}

public static final class KeystrokesModule extends HudElement {
    public KeystrokesModule() { super("Keystrokes", "WASD and mouse buttons", 6, 212); }
    @Override public int width() { return 66; }
    @Override public int height() { return 66; }

    private void key(DrawContext c, int x, int y, int w, int h, String label, KeyBinding kb) {
        boolean on = kb.isPressed();
        c.fill(x, y, x + w, y + h, on ? RenderUtil.alpha(RenderUtil.ACCENT, 0.55f) : 0xA0000000);
        RenderUtil.outline(c, x, y, x + w, y + h, 0xFF000000);
        RenderUtil.centerText(c, label, x + w / 2, y + (h - 9) / 2, on ? 0xFFFFFFFF : RenderUtil.ASH, 0.85f);
    }

    @Override public void draw(DrawContext c, int x, int y) {
        key(c, x + 22, y, 20, 20, "W", mc.options.forwardKey);
        key(c, x, y + 22, 20, 20, "A", mc.options.leftKey);
        key(c, x + 22, y + 22, 20, 20, "S", mc.options.backKey);
        key(c, x + 44, y + 22, 20, 20, "D", mc.options.rightKey);
        key(c, x, y + 44, 31, 20, "LMB", mc.options.attackKey);
        key(c, x + 33, y + 44, 31, 20, "RMB", mc.options.useKey);
    }
}

public static final class KillAuraModule extends Module {
    private final NumberSetting aps    = add(new NumberSetting("APS", 1, 20, 1, 12));
    private final NumberSetting range  = add(new NumberSetting("Range", 2, 6, 0.1, 3.8));
    private final BooleanSetting team  = add(new BooleanSetting("Target Team", false));
    private final BooleanSetting rotate = add(new BooleanSetting("Silent Rotate", true));
    private long last;

    public KillAuraModule() { super("KillAura", "Attacks the nearest player in range", Category.COMBAT); }

    @Override public void onTick() {
        if (mc.currentScreen != null) return;
        long now = System.currentTimeMillis();
        if (now - last < 1000L / aps.getInt()) return;

        PlayerEntity best = null; double bd = range.get() * range.get();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            if (!team.get() && p.isTeammate(mc.player)) continue;
            double d = mc.player.squaredDistanceTo(p);
            if (d < bd) { bd = d; best = p; }
        }
        if (best == null) return;
        last = now;

        if (rotate.get()) {
            Vec3d to = best.getEyePos().subtract(mc.player.getEyePos());
            float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
            PacketUtil.send(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
        }
        PacketUtil.attack(best);
        PacketUtil.swing();
    }
}

/** Name + health over every player, visible through walls. */
public static final class NameTagsModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 16, 200, 8, 96));
    private final NumberSetting scale = add(new NumberSetting("Scale", 0.6, 1.6, 0.1, 1.0));
    private final BooleanSetting health = add(new BooleanSetting("Show Health", true));

    public NameTagsModule() { super("NameTags", "Readable name + health tags", Category.RENDER); }

    @Override public void onHudRender(DrawContext c) {
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        float td = mc.getRenderTickCounter().getTickDelta(false);
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (!p.isAlive() || (p == mc.player && mc.getCameraEntity() == mc.player && !dev.cpvp.util.CameraControl.active)) continue;
            double d = mc.player.distanceTo(p);
            if (d > range.get()) continue;
            float[] sp = Project.toScreen(p.getLerpedPos(td).add(0, p.getHeight() + 0.35, 0));
            if (sp == null) continue;
            float hp = p.getHealth() + p.getAbsorptionAmount();
            String s = p.getName().getString() + (health.get() ? "  " + Math.round(hp) : "");
            float sc = (float) (scale.get() * MathHelper.clamp(1.2 - d / 90.0, 0.6, 1.1));
            int w = RenderUtil.width(s, sc);
            int x = Math.round(sp[0]) - w / 2, y = Math.round(sp[1]) - 10;
            c.fill(x - 3, y - 2, x + w + 3, y + Math.round(10 * sc) + 2, 0xB0000000);
            int col = hp > 14 ? 0xFFFFFFFF : hp > 7 ? 0xFFFFD24A : 0xFFFF5555;
            RenderUtil.text(c, s, x, y, col, sc);
        }
    }
}

public static final class NoFallModule extends Module {
    public NoFallModule() { super("NoFall", "Cancel fall damage", Category.UTILITY); }

    @Override public void onTick() {
        if (mc.player.fallDistance > 2.5f && !mc.player.isOnGround())
            PacketUtil.send(new PlayerMoveC2SPacket.OnGroundOnly(true));
    }
}

/** Turns every module off. Bind a key to it (or click it). */
public static final class PanicModule extends Module {
    public PanicModule() { super("Panic", "Disable everything at once", Category.UTILITY); }

    private void panic() {
        for (Module m : ModuleManager.INSTANCE.all()) if (m != this && m.isEnabled() && !(m instanceof InterfaceModule)) m.setEnabled(false);
    }

    @Override public void onKeyPress() { panic(); }
    @Override public void onEnable() { panic(); setEnabled(false); }
}

/** Jumps for you at the edge of a block. */
public static final class ParkourModule extends Module {
    public ParkourModule() { super("Parkour", "Auto jump at ledges", Category.WORLD); }

    @Override public void onTick() {
        if (mc.currentScreen != null || !mc.player.isOnGround() || mc.player.isSneaking()) return;
        Vec3d v = mc.player.getVelocity();
        if (v.horizontalLengthSquared() < 0.0004) return;
        Vec3d dir = new Vec3d(v.x, 0, v.z).normalize().multiply(0.55);
        BlockPos ahead = BlockPos.ofFloored(mc.player.getX() + dir.x, mc.player.getY() - 0.6, mc.player.getZ() + dir.z);
        if (mc.world.getBlockState(ahead).isAir() && mc.world.getBlockState(ahead.down()).isAir()) mc.player.jump();
    }
}

/**
 * Throw a pearl as normal. A few ticks later this finds YOUR pearl in flight and throws a wind charge
 * straight at it, so the pearl is boosted and you teleport higher in the air.
 */
public static final class PearlCatchModule extends Module {
    private final NumberSetting delay   = add(new NumberSetting("Delay (ticks)", 1, 15, 1, 2));
    private final NumberSetting lead    = add(new NumberSetting("Aim Lead (ticks)", 0, 6, 0.5, 1.5));
    private final BooleanSetting swapBack = add(new BooleanSetting("Swap Back", true));

    private int countdown = -1;

    public PearlCatchModule() { super("Pearlcatch", "Wind charge hits your pearl mid-air", Category.COMBAT); }

    @Override public boolean ticksWhenDisabled() { return true; }

    /** Keybind: swap to a pearl, throw it, swap back, then the wind charge follows automatically. */
    @Override public void onKeyPress() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        int pearl = SlotUtil.find(s -> s.isOf(Items.ENDER_PEARL));
        if (pearl < 0) { status("No ender pearl in hotbar"); return; }
        if (SlotUtil.find(s -> s.isOf(Items.WIND_CHARGE)) < 0) { status("No wind charge in hotbar"); return; }
        if (mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL)) { status("Pearl on cooldown"); return; }
        int prev = SlotUtil.selected();
        SlotUtil.swap(pearl);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        SlotUtil.swap(prev);
        countdown = delay.getInt();
    }

    @Override public void onRightClick() {
        if (mc.player == null || !mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL)) return;
        countdown = delay.getInt();
    }

    @Override public void onTick() {
        if (countdown < 0) return;
        if (countdown > 0) { countdown--; return; }
        countdown = -1;
        fire();
    }

    private void fire() {
        EnderPearlEntity pearl = null;
        for (EnderPearlEntity p : mc.world.getEntitiesByClass(EnderPearlEntity.class,
                mc.player.getBoundingBox().expand(32), e -> e.getOwner() == mc.player)) {
            if (pearl == null || p.age < pearl.age) pearl = p;   // newest pearl
        }
        if (pearl == null) { status("Pearl not found"); return; }
        int slot = SlotUtil.find(s -> s.isOf(Items.WIND_CHARGE));
        if (slot < 0) { status("No wind charge in hotbar"); return; }

        Vec3d aim = pearl.getPos().add(pearl.getVelocity().multiply(lead.get())).add(0, pearl.getHeight() / 2, 0);
        Vec3d to = aim.subtract(mc.player.getEyePos());
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));

        int prev = SlotUtil.selected();
        float oy = mc.player.getYaw(), op = mc.player.getPitch();
        SlotUtil.swap(slot);
        mc.player.setYaw(yaw); mc.player.setPitch(pitch);          // server receives this aim inside the use packet
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.setYaw(oy); mc.player.setPitch(op);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (swapBack.get()) SlotUtil.swap(prev);
    }
}

public static final class PingModule extends HudElement {
    public PingModule() { super("Ping", "Latency to the server", 6, 188); }
    @Override public int width() { return 74; }
    @Override public int height() { return 18; }

    @Override public void draw(DrawContext c, int x, int y) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        String s = e == null ? "Ping  --" : "Ping  " + e.getLatency() + "ms";
        c.fill(x, y, x + width(), y + height(), 0xA0000000);
        RenderUtil.outline(c, x, y, x + width(), y + height(), 0xFF000000);
        RenderUtil.text(c, s, x + 6, y + 5, RenderUtil.TEXT, 0.85f);
    }
}

public static final class PotionHudModule extends HudElement {
    public PotionHudModule() { super("Effects", "Active potion effects", 6, 290); }
    @Override public int width() { return 120; }
    @Override public int height() { return mc.player == null ? 18 : Math.max(18, mc.player.getStatusEffects().size() * 14 + 4); }

    @Override public void draw(DrawContext c, int x, int y) {
        if (mc.player == null || mc.player.getStatusEffects().isEmpty()) return;
        c.fill(x, y, x + width(), y + height(), 0xA0000000);
        RenderUtil.outline(c, x, y, x + width(), y + height(), 0xFF000000);
        int i = 0;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            int s = e.getDuration() / 20;
            RenderUtil.text(c, e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1)
                    + "  " + s / 60 + ":" + String.format("%02d", s % 60), x + 6, y + 5 + i * 14, RenderUtil.TEXT, 0.8f);
            i++;
        }
    }
}

/** Extends your entity interaction range on the client (PlayerEntityMixin). The server still enforces its own limit. */
public static final class ReachModule extends Module {
    public static boolean ACTIVE;
    private static double range = 3.0;
    private final NumberSetting dist = add(new NumberSetting("Range", 3.0, 6.0, 0.1, 3.5));

    public ReachModule() { super("Reach", "Longer attack reach", Category.COMBAT); }

    public static double range() { return range; }

    @Override public void onEnable()  { ACTIVE = true; range = dist.get(); }
    @Override public void onDisable() { ACTIVE = false; }
    @Override public void onTick()    { range = dist.get(); }
}

/** Tops up low hotbar stacks from your inventory. */
public static final class RefillModule extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Refill At", 1, 32, 1, 8));
    private final NumberSetting delay     = add(new NumberSetting("Delay (ticks)", 1, 20, 1, 4));
    private int timer;

    public RefillModule() { super("Refill", "Refill hotbar stacks", Category.INVENTORY); }

    @Override public void onTick() {
        if (timer-- > 0 || mc.currentScreen != null) return;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || !s.isStackable() || s.getCount() > threshold.getInt() || s.getCount() >= s.getMaxCount()) continue;
            for (int j = 9; j < 36; j++) {
                if (!ItemStack.areItemsAndComponentsEqual(s, inv.getStack(j))) continue;
                mc.interactionManager.clickSlot(0, j, i, SlotActionType.SWAP, mc.player);
                timer = delay.getInt();
                return;
            }
        }
    }
}

public static final class RightClickerModule extends Module {
    private final NumberSetting cps = add(new NumberSetting("CPS", 1, 20, 1, 12));
    private long last;

    public RightClickerModule() { super("RightClicker", "Uses / places for you while RMB is held", Category.COMBAT); }

    @Override public void onFrame() {
        if (mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
        long now = System.currentTimeMillis();
        if (now - last < 1000L / cps.getInt()) return;
        last = now;
        if (mc.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK)
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, b);
        else mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}

/** Never walk off ledges (PlayerEntityMixin#clipAtLedge). */
public static final class SafeWalkModule extends Module {
    public static boolean ACTIVE;
    public SafeWalkModule() { super("SafeWalk", "Don't fall off edges", Category.WORLD); }
    @Override public void onEnable()  { ACTIVE = true; }
    @Override public void onDisable() { ACTIVE = false; }
}

public static final class ScaffoldModule extends Module {
    public ScaffoldModule() { super("Scaffold", "Places blocks under your feet", Category.WORLD); }

    @Override public void onTick() {
        if (mc.currentScreen != null) return;
        PlaceUtil.place(mc.player.getBlockPos().down());
    }
}

/**
 * Cooldown-aware aura with SILENT rotation: the server gets a look packet at the target, your
 * screen never turns. Uses the normal attack path so cooldown/animation stay in sync.
 */
public static final class SilentAuraModule extends Module {
    private final NumberSetting range    = add(new NumberSetting("Range", 2, 6, 0.1, 3.0));
    private final NumberSetting cooldown = add(new NumberSetting("Min Cooldown", 0.5, 1.0, 0.05, 0.95));
    private final BooleanSetting mobs    = add(new BooleanSetting("Target Mobs", true));
    private final BooleanSetting rotate  = add(new BooleanSetting("Silent Rotate", true));

    public SilentAuraModule() { super("SilentAura", "Cooldown-timed aura with silent rotations", Category.COMBAT); }

    @Override public void onTick() {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        if (mc.player.getAttackCooldownProgress(0f) < cooldown.get()) return;
        LivingEntity t = TargetUtil.nearestLiving(range.get(), mobs.get());
        if (t == null) return;

        if (rotate.get()) {
            Vec3d to = t.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
            float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
            PacketUtil.send(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
        }
        mc.interactionManager.attackEntity(mc.player, t);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}

public static final class SpawnerFinderModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 16, 128, 8, 96));
    private final List<Box> boxes = new ArrayList<>();
    private int timer;

    public SpawnerFinderModule() { super("SpawnerFinder", "Highlights mob spawners", Category.RENDER); }

    @Override public void onTick() {
        if (timer-- > 0) return;
        timer = 20;
        boxes.clear();
        ChunkPos cp = mc.player.getChunkPos();
        int r = Math.min(8, mc.options.getViewDistance().getValue());
        double rsq = range.get() * range.get();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            WorldChunk ch = mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
            if (ch == null) continue;
            for (BlockEntity be : ch.getBlockEntities().values())
                if (be instanceof MobSpawnerBlockEntity && mc.player.squaredDistanceTo(Vec3d.ofCenter(be.getPos())) <= rsq)
                    boxes.add(new Box(be.getPos()));
        }
    }

    @Override public void onWorldRender(WorldRenderContext ctx) {
        Vec3d cam = ctx.camera().getPos();
        Render3D.begin();
        for (Box b : boxes) Render3D.box(ctx.matrixStack(), cam, b, 0xFFFF3B3B);
        Render3D.end();
    }
}

public static final class SprintModule extends Module {
    public SprintModule() { super("Sprint", "Always sprint while moving forward", Category.COMBAT); }

    @Override public void onTick() {
        if (mc.currentScreen != null || mc.player.isSneaking() || mc.player.isUsingItem()) return;
        if (mc.player.input.movementForward > 0 && !mc.player.horizontalCollision
                && mc.player.getHungerManager().getFoodLevel() > 6) mc.player.setSprinting(true);
    }
}

/**
 * Watches the nearest target and fires a burst the moment it is vulnerable:
 *  - Shield Stun : target is blocking -> swap to axe, hit, swap back
 *  - Mace Slam   : you are falling onto the target -> swap to mace, hit, swap back
 *  - Anchor Slam : target is launched / knocked back -> hand over to Auto Anchor
 *
 * NOTE: servers reset your attack-strength when the held item changes, so "Swap Delay" lets the
 * cooldown recover a little before the hit if you want more than the minimum damage.
 */
public static final class SwapStunModule extends Module {
    private final NumberSetting reach      = add(new NumberSetting("Attack Reach", 2, 6, 0.1, 3.0));
    private final NumberSetting vision    = add(new NumberSetting("Vision FOV", 10, 180, 5, 60));
    private final NumberSetting swapDelay  = add(new NumberSetting("Swap Delay (ticks)", 0, 12, 1, 1));
    private final NumberSetting cooldown   = add(new NumberSetting("Cooldown (ticks)", 0, 40, 1, 10));
    private final BooleanSetting shieldStun = add(new BooleanSetting("Shield Stun", true));
    private final BooleanSetting maceSlam   = add(new BooleanSetting("Mace Slam", true));
    private final NumberSetting minFall     = add(new NumberSetting("Min Fall (blocks)", 1.5, 10, 0.5, 2.5));
    private final BooleanSetting anchorSlam = add(new BooleanSetting("Anchor Follow-up", true));
    private final NumberSetting displace    = add(new NumberSetting("Displace Threshold", 0.2, 1.5, 0.05, 0.5));
    private final BooleanSetting swapBack   = add(new BooleanSetting("Swap Back", true));

    private final Map<Integer, Vec3d> lastPos = new HashMap<>();
    private PlayerEntity victim;
    private int prevSlot = -1, timer, cooldownLeft;

    public SwapStunModule() {
        super("Shieldbreaker", "Axes a shield in your vision, then swaps back", Category.COMBAT);
    }

    @Override public void onDisable() { finish(false); lastPos.clear(); }

    @Override public void onTick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) { finish(false); return; }

        // Movement tracking (client-side entity velocity is unreliable for other players; use position deltas).
        PlayerEntity target = TargetUtil.nearest(Math.max(reach.get() + 3.0, 6.0));
        Vec3d motion = Vec3d.ZERO;
        if (target != null) {
            Vec3d now = target.getPos();
            motion = now.subtract(lastPos.getOrDefault(target.getId(), now));
            lastPos.put(target.getId(), now);
        }

        // Executing a queued swap-hit.
        if (victim != null) {
            if (timer-- > 0) return;
            if (victim.isAlive() && mc.player.distanceTo(victim) <= reach.get()) {
                PacketUtil.attack(victim);
                PacketUtil.swing();
            }
            finish(true);
            return;
        }

        if (cooldownLeft > 0) { cooldownLeft--; return; }
        if (target == null || mc.player.distanceTo(target) > reach.get()) {
            // Anchor follow-up is allowed at longer range since Auto Anchor has its own range.
            if (target != null) tryAnchor(target, motion);
            return;
        }

        if (shieldStun.get() && target.isBlocking() && inVision(target)) {
            int axe = SlotUtil.find(s -> s.getItem() instanceof AxeItem);
            if (axe >= 0) { begin(target, axe); return; }
        }
        if (maceSlam.get() && !mc.player.isOnGround() && mc.player.fallDistance >= minFall.get()) {
            int mace = SlotUtil.find(s -> s.isOf(Items.MACE));
            if (mace >= 0) { begin(target, mace); return; }
        }
        tryAnchor(target, motion);
    }

    private boolean inVision(PlayerEntity t) {
        Vec3d look = mc.player.getRotationVec(1f);
        Vec3d to = t.getEyePos().subtract(mc.player.getEyePos()).normalize();
        double ang = Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(to), -1, 1)));
        return ang <= vision.get() / 2.0;
    }

    private void tryAnchor(PlayerEntity target, Vec3d motion) {
        if (!anchorSlam.get()) return;
        boolean launched = motion.y > 0.3 || motion.horizontalLength() > displace.get();
        if (!launched) return;
        AutoAnchorModule anchor = ModuleManager.INSTANCE.get(AutoAnchorModule.class);
        if (anchor != null) {
            anchor.queue(target);
            cooldownLeft = cooldown.getInt();
        }
    }

    private void begin(PlayerEntity target, int weaponSlot) {
        victim = target;
        prevSlot = SlotUtil.selected();
        SlotUtil.swap(weaponSlot);
        timer = swapDelay.getInt();
    }

    private void finish(boolean applyCooldown) {
        if (swapBack.get() && prevSlot >= 0 && mc.player != null) SlotUtil.swap(prevSlot);
        victim = null; prevSlot = -1;
        if (applyCooldown) cooldownLeft = cooldown.getInt();
    }
}

/** Global target rules used by every aura / aim / crystal module. */
public static final class TargetFilterModule extends Module {
    private static TargetFilterModule instance;
    private final BooleanSetting team      = add(new BooleanSetting("Ignore Teammates", true));
    private final BooleanSetting invisible = add(new BooleanSetting("Ignore Invisible", false));
    private final BooleanSetting naked     = add(new BooleanSetting("Ignore Unarmored", false));

    public TargetFilterModule() { super("TargetFilter", "Who the combat modules may target", Category.UTILITY); instance = this; }

    public static boolean allows(PlayerEntity p) {
        TargetFilterModule f = instance;
        if (f == null || !f.isEnabled()) return true;
        if (f.team.get() && p.isTeammate(MinecraftClient.getInstance().player)) return false;
        if (f.invisible.get() && p.isInvisible()) return false;
        return !(f.naked.get() && p.getArmor() == 0);
    }
}

/** Throws harming / poison / slowness / weakness splash potions at the nearest player. */
public static final class ThrowDebuffModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 3, 12, 0.5, 8));
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 5, 60, 1, 20));
    private int timer;

    public ThrowDebuffModule() { super("ThrowDebuff", "Auto throw harmful potions", Category.INVENTORY); }

    @Override public void onTick() {
        if (timer > 0) { timer--; return; }
        if (mc.currentScreen != null) return;
        PlayerEntity t = TargetUtil.nearest(range.get());
        if (t == null || mc.player.distanceTo(t) < 2.5) return;
        int slot = SlotUtil.find(s -> PotionUtil.is(s, Potions.HARMING, Potions.STRONG_HARMING, Potions.POISON,
                Potions.STRONG_POISON, Potions.SLOWNESS, Potions.STRONG_SLOWNESS, Potions.WEAKNESS));
        if (slot < 0) return;
        PotionUtil.throwAt(slot, t.getPos().add(t.getVelocity().multiply(3)));
        timer = delay.getInt();
    }
}

/** Throws a healing splash potion at your feet when your health drops below the threshold. */
public static final class ThrowpotModule extends Module {
    private final NumberSetting health = add(new NumberSetting("Health Below", 2, 19, 1, 10));
    private final NumberSetting delay  = add(new NumberSetting("Delay (ticks)", 1, 40, 1, 12));
    private int timer;

    public ThrowpotModule() { super("Throwpot", "Auto throw healing potions", Category.INVENTORY); }

    @Override public void onTick() {
        if (timer > 0) { timer--; return; }
        if (mc.currentScreen != null || mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.get()) return;
        int slot = SlotUtil.find(s -> PotionUtil.is(s, Potions.HEALING, Potions.STRONG_HEALING));
        if (slot < 0) return;
        PotionUtil.throwAt(slot, mc.player.getPos().add(0, -1, 0));
        timer = delay.getInt();
    }
}

public static final class TracersModule extends Module {
    public TracersModule() { super("Tracers", "Lines to every player", Category.RENDER); }

    @Override public void onWorldRender(WorldRenderContext ctx) {
        float td = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        Vec3d start = cam.add(Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(0.5));
        Render3D.begin();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            Vec3d end = p.getLerpedPos(td).add(0, p.getHeight() / 2, 0);
            Render3D.line(ctx.matrixStack(), cam, start, end, 0xFF00D2FF);
        }
        Render3D.end();
    }
}

public static final class TriggerBotModule extends Module {
    private final NumberSetting cooldown = add(new NumberSetting("Min Cooldown", 0.1, 1.0, 0.05, 0.95));
    private final BooleanSetting crits   = add(new BooleanSetting("Crits Only", false));

    public TriggerBotModule() { super("TriggerBot", "Attacks when your crosshair is on a player", Category.COMBAT); }

    @Override public void onFrame() {
        if (mc.currentScreen != null || mc.player.isUsingItem() || mc.interactionManager == null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof PlayerEntity p)) return;
        if (p == mc.player || !p.isAlive() || p.isSpectator()) return;
        if (mc.player.getAttackCooldownProgress(0f) < cooldown.get()) return;
        if (crits.get() && (mc.player.isOnGround() || mc.player.fallDistance <= 0f)) return;
        mc.interactionManager.attackEntity(mc.player, p);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}

/** Scales the knockback you take (hooked from ClientPlayNetworkHandlerMixin). */
public static final class VelocityModule extends Module {
    private static VelocityModule instance;
    private final NumberSetting horizontal = add(new NumberSetting("Horizontal %", 0, 100, 1, 80));
    private final NumberSetting vertical   = add(new NumberSetting("Vertical %", 0, 100, 1, 100));

    public VelocityModule() { super("Velocity", "Reduces knockback", Category.COMBAT); instance = this; }

    public static void apply(EntityVelocityUpdateS2CPacket p) {
        if (instance == null || !instance.isEnabled() || mc.player == null || p.getEntityId() != mc.player.getId()) return;
        Vec3d v = mc.player.getVelocity();
        double h = instance.horizontal.get() / 100.0, y = instance.vertical.get() / 100.0;
        mc.player.setVelocity(v.x * h, v.y * y, v.z * h);
    }
}

/** Resets your sprint on each hit so every hit deals full knockback. */
public static final class WTapModule extends Module {
    public WTapModule() { super("WTap", "Sprint reset on hit", Category.COMBAT); }

    @Override public void onLeftClick() {
        if (mc.player == null || !mc.player.isSprinting() || !(mc.crosshairTarget instanceof EntityHitResult)) return;
        PacketUtil.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        PacketUtil.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
    }
}

/**
 * X-Ray = a resource pack (built into the mod: "Fog X-Ray"). Enabling the module turns the pack on and
 * reloads resources; stone, deepslate, dirt, grass, glass... stop rendering and ores are drawn through the rock.
 * Works with Sodium because it only changes models. Extra ore outlines are optional.
 */
public static final class XRayModule extends Module {
    private static final Map<Block, Boolean> ORE = new ConcurrentHashMap<>();
    private final BooleanSetting fullbright = add(new BooleanSetting("Fullbright", true));
    private final BooleanSetting outlines   = add(new BooleanSetting("Ore Outlines", true));
    private final NumberSetting range       = add(new NumberSetting("Outline Range", 16, 64, 8, 32));
    private final List<Box> ores = new ArrayList<>();
    private boolean addedNv;
    private int scanTimer;

    public XRayModule() { super("Xray", "Highlights ores through walls", Category.RENDER); }

    public static boolean isOre(BlockState s) {
        return ORE.computeIfAbsent(s.getBlock(), b -> {
            String p = Registries.BLOCK.getId(b).getPath();
            return p.endsWith("_ore") || p.equals("ancient_debris");
        });
    }

    @Override public void onEnable()  { scanTimer = 0; }
    @Override public void onDisable() {
        ores.clear();
        if (addedNv && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        addedNv = false;
    }

    @Override public void onTick() {
        if (fullbright.get() && !mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 100000, 0, false, false));
            addedNv = true;
        }
        if (!outlines.get()) { ores.clear(); return; }
        if (scanTimer-- > 0) return;
        scanTimer = 30;
        scan();
    }

    private void scan() {
        ores.clear();
        ChunkPos cp = mc.player.getChunkPos();
        int r = Math.max(1, (int) Math.ceil(range.get() / 16.0));
        double rsq = range.get() * range.get();
        Vec3d pp = mc.player.getPos();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            WorldChunk ch = mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
            if (ch == null) continue;
            ChunkSection[] secs = ch.getSectionArray();
            for (int i = 0; i < secs.length; i++) {
                ChunkSection s = secs[i];
                if (s == null || s.isEmpty() || !s.hasAny(XRayModule::isOre)) continue;
                int by = ch.getBottomY() + i * 16;
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    if (!isOre(s.getBlockState(x, y, z))) continue;
                    BlockPos bp = new BlockPos(ch.getPos().getStartX() + x, by + y, ch.getPos().getStartZ() + z);
                    if (pp.squaredDistanceTo(Vec3d.ofCenter(bp)) > rsq) continue;
                    ores.add(new Box(bp));
                    if (ores.size() >= 800) return;
                }
            }
        }
    }

    @Override public void onWorldRender(WorldRenderContext ctx) {
        if (!outlines.get() || ores.isEmpty()) return;
        Render3D.begin();
        Render3D.outlines(ctx.matrixStack(), ctx.camera().getPos(), ores, 0xFF30F271);
        Render3D.end();
    }
}

    // ---------------------------------------------------------------- movable HUD panels
    public static final class LogoModule extends HudElement {
        public LogoModule() { super("Logo", "Yellow crown (no text)", 8, 6); setEnabled(true); }
        @Override public int width()  { return 28; }
        @Override public int height() { return 26; }
        @Override public void draw(DrawContext c, int x, int y) { RenderUtil.crown(c, x + 2, y + 2, 22, RenderUtil.CROWN); }
    }

    public static final class FpsModule extends HudElement {
        public FpsModule() { super("FPS", "King | FPS counter", 760, 4); setEnabled(true); }

        private String tail() { return " | FPS: " + mc.getCurrentFps(); }

        @Override public int width()  { return RenderUtil.width("King", 0.9f) + RenderUtil.width(tail(), 0.9f) + 6; }
        @Override public int height() { return 14; }

        @Override public void draw(DrawContext c, int x, int y) {
            int ax = x + 2;
            RenderUtil.text(c, "King", ax + 1, y + 2, 0xFF000000, 0.9f);
            RenderUtil.text(c, "King", ax, y + 1, RenderUtil.ACCENT, 0.9f);
            int bx = ax + RenderUtil.width("King", 0.9f);
            RenderUtil.text(c, tail(), bx + 1, y + 2, 0xFF000000, 0.9f);
            RenderUtil.text(c, tail(), bx, y + 1, 0xFFFFFFFF, 0.9f);
        }
    }

    public static final class ModuleListModule extends HudElement {
        public ModuleListModule() { super("ModuleList", "List of enabled modules", 760, 22); setEnabled(true); }

        private List<Module> enabled() {
            return ModuleManager.INSTANCE.all().stream()
                    .filter(m -> m.isEnabled() && m != this && m.getCategory() != Category.SETTINGS && m.getCategory() != Category.MODS)
                    .sorted(Comparator.comparingInt((Module m) -> RenderUtil.width(m.getName(), 1f)).reversed())
                    .toList();
        }

        @Override public int width() {
            int w = 60;
            for (Module m : enabled()) w = Math.max(w, RenderUtil.width(m.getName(), 1f) + 10);
            return w;
        }
        @Override public int height() { return Math.max(12, enabled().size() * 11 + 2); }

        @Override public void draw(DrawContext c, int x, int y) {
            int right = x + width() - 2, yy = y + 1;
            for (Module m : enabled()) {
                int w = RenderUtil.width(m.getName(), 1f);
                c.fill(right - w - 6, yy - 1, right + 2, yy + 10, 0xA0000000);
                c.fill(right + 1, yy - 1, right + 2, yy + 10, RenderUtil.ACCENT);
                RenderUtil.text(c, m.getName(), right - w - 2, yy, RenderUtil.ACCENT, 1f);
                yy += 11;
            }
        }
    }

    // ================================================================ Search
    public static final class SearchModule extends Module {
        private static final String[] NAMES = { "Diamond", "Emerald", "Gold", "Iron", "Coal", "Redstone", "Lapis", "Ancient Debris", "Spawners", "Chests" };
        private static final int[] COLORS = { 0xFF55FFFF, 0xFF55FF55, 0xFFFFD24A, 0xFFD8AF93, 0xFF777777, 0xFFFF3030, 0xFF3050FF, 0xFFAA6030, 0xFFFF3B3B, 0xFFFFA500 };
        private static final Map<Block, Integer> KIND = new ConcurrentHashMap<>();
        private final BooleanSetting tracers = add(new BooleanSetting("Tracers", false));
        private final NumberSetting range = add(new NumberSetting("Range", 16, 96, 8, 48));
        private final BooleanSetting[] on = new BooleanSetting[NAMES.length];
        private final List<Box> boxes = new ArrayList<>();
        private final List<Integer> kinds = new ArrayList<>();
        private int timer;

        public SearchModule() {
            super("Search", "Highlights the blocks you pick", Category.RENDER);
            for (int i = 0; i < NAMES.length; i++) on[i] = add(new BooleanSetting(NAMES[i], i == 0 || i == 7 || i == 8));
        }

        private static int kind(Block b) {
            return KIND.computeIfAbsent(b, k -> {
                String p = Registries.BLOCK.getId(k).getPath();
                if (p.contains("diamond_ore")) return 0;
                if (p.contains("emerald_ore")) return 1;
                if (p.contains("gold_ore")) return 2;
                if (p.contains("iron_ore")) return 3;
                if (p.contains("coal_ore")) return 4;
                if (p.contains("redstone_ore")) return 5;
                if (p.contains("lapis_ore")) return 6;
                if (p.equals("ancient_debris")) return 7;
                if (p.endsWith("spawner")) return 8;
                if (p.endsWith("chest")) return 9;
                return -1;
            });
        }

        private boolean wanted(BlockState s) { int k = kind(s.getBlock()); return k >= 0 && on[k].get(); }

        @Override public void onDisable() { boxes.clear(); kinds.clear(); }

        @Override public void onTick() {
            if (timer-- > 0) return;
            timer = 30;
            boxes.clear(); kinds.clear();
            ChunkPos cp = mc.player.getChunkPos();
            int r = Math.max(1, (int) Math.ceil(range.get() / 16.0));
            double rsq = range.get() * range.get();
            Vec3d pp = mc.player.getPos();
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                WorldChunk ch = mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
                if (ch == null) continue;
                ChunkSection[] secs = ch.getSectionArray();
                for (int i = 0; i < secs.length; i++) {
                    ChunkSection sec = secs[i];
                    if (sec == null || sec.isEmpty() || !sec.hasAny(this::wanted)) continue;
                    int by = ch.getBottomY() + i * 16;
                    for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                        BlockState st = sec.getBlockState(x, y, z);
                        if (!wanted(st)) continue;
                        BlockPos bp = new BlockPos(ch.getPos().getStartX() + x, by + y, ch.getPos().getStartZ() + z);
                        if (pp.squaredDistanceTo(Vec3d.ofCenter(bp)) > rsq) continue;
                        boxes.add(new Box(bp)); kinds.add(kind(st.getBlock()));
                        if (boxes.size() >= 600) return;
                    }
                }
            }
        }

        @Override public void onWorldRender(WorldRenderContext ctx) {
            if (boxes.isEmpty()) return;
            Vec3d cam = ctx.camera().getPos();
            Render3D.begin();
            for (int k = 0; k < NAMES.length; k++) {
                List<Box> sub = new ArrayList<>();
                for (int i = 0; i < boxes.size(); i++) if (kinds.get(i) == k) sub.add(boxes.get(i));
                Render3D.outlines(ctx.matrixStack(), cam, sub, COLORS[k]);
            }
            if (tracers.get()) {
                Vec3d start = cam.add(Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(0.5));
                for (int i = 0; i < boxes.size(); i++) Render3D.line(ctx.matrixStack(), cam, start, boxes.get(i).getCenter(), COLORS[kinds.get(i)]);
            }
            Render3D.end();
        }
    }

    // ================================================================ Projectiles
    public static final class ProjectilesModule extends Module {
        public ProjectilesModule() { super("Projectiles", "Shows where your throwable / arrow lands", Category.RENDER); }

        @Override public void onWorldRender(WorldRenderContext ctx) {
            if (mc.player == null) return;
            ItemStack st = mc.player.getMainHandStack();
            double speed, grav; double roll = 0;
            if (st.isOf(Items.BOW)) {
                if (!mc.player.isUsingItem()) return;
                float f = mc.player.getItemUseTime() / 20f;
                f = (f * f + f * 2f) / 3f;
                if (f < 0.1f) return;
                speed = 3.0 * Math.min(1f, f); grav = 0.05;
            } else if (st.isOf(Items.ENDER_PEARL) || st.isOf(Items.SNOWBALL) || st.isOf(Items.EGG)) { speed = 1.5; grav = 0.03; }
            else if (st.isOf(Items.SPLASH_POTION) || st.isOf(Items.LINGERING_POTION)) { speed = 0.5; grav = 0.05; roll = -20; }
            else if (st.isOf(Items.EXPERIENCE_BOTTLE)) { speed = 0.7; grav = 0.07; roll = -20; }
            else return;

            float td = mc.getRenderTickCounter().getTickDelta(false);
            Vec3d pos = mc.player.getLerpedPos(td).add(0, mc.player.getStandingEyeHeight() - 0.1, 0);
            double yaw = Math.toRadians(mc.player.getYaw()), pitch = Math.toRadians(mc.player.getPitch());
            Vec3d vel = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch + Math.toRadians(roll)), Math.cos(yaw) * Math.cos(pitch)).multiply(speed);

            List<Vec3d> pts = new ArrayList<>();
            pts.add(pos);
            boolean landed = false;
            for (int i = 0; i < 160; i++) {
                Vec3d next = pos.add(vel);
                var hit = mc.world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                if (hit.getType() != HitResult.Type.MISS) { pts.add(hit.getPos()); pos = hit.getPos(); landed = true; break; }
                pts.add(next); pos = next;
                vel = vel.multiply(0.99).subtract(0, grav, 0);
            }
            Vec3d cam = ctx.camera().getPos();
            Render3D.begin();
            for (int i = 0; i + 1 < pts.size(); i++) Render3D.line(ctx.matrixStack(), cam, pts.get(i), pts.get(i + 1), landed ? 0xFF30F271 : 0xFFFFD24A);
            if (landed) Render3D.box(ctx.matrixStack(), cam, new Box(pos.x - 0.25, pos.y - 0.05, pos.z - 0.25, pos.x + 0.25, pos.y + 0.45, pos.z + 0.25), 0xFF30F271);
            Render3D.end();
        }
    }

    // ================================================================ Indicators
    public static final class IndicatorsModule extends Module {
        public IndicatorsModule() { super("Indicators", "Warns about incoming projectiles", Category.RENDER); }

        @Override public void onHudRender(DrawContext c) {
            if (mc.world == null || mc.player == null || mc.currentScreen != null || mc.options.hudHidden) return;
            int cx = c.getScaledWindowWidth() / 2, cy = c.getScaledWindowHeight() / 2;
            Vec3d me = mc.player.getEyePos();
            for (PersistentProjectileEntity e : mc.world.getEntitiesByClass(PersistentProjectileEntity.class,
                    mc.player.getBoundingBox().expand(45), x -> x.getOwner() != mc.player)) {
                Vec3d v = e.getVelocity();
                if (v.lengthSquared() < 0.1) continue;
                Vec3d to = me.subtract(e.getPos());
                if (to.length() > 42 || v.normalize().dotProduct(to.normalize()) < 0.9) continue;   // not heading at you
                float rel = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(to.z * -1, to.x * -1)) - 90.0) - mc.player.getYaw());
                var m = c.getMatrices();
                m.push();
                m.translate(cx, cy, 0);
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rel));
                m.translate(0, -52, 0);
                for (int r = 0; r < 9; r++) { int h = r / 2; c.fill(-h, -8 + r, h + 1, -7 + r, 0xFFFF4040); }
                m.pop();
            }
        }
    }

    // ================================================================ InvManager
    public static final class InvManagerModule extends Module {
        private static final java.util.Set<Item> JUNK = java.util.Set.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO,
                Items.PUFFERFISH, Items.BONE, Items.INK_SAC, Items.GLOW_INK_SAC, Items.TROPICAL_FISH);
        private final NumberSetting delay   = add(new NumberSetting("Delay (ticks)", 1, 20, 1, 3));
        private final NumberSetting sword   = add(new NumberSetting("Sword Slot", 1, 9, 1, 1));
        private final NumberSetting pick    = add(new NumberSetting("Pickaxe Slot", 1, 9, 1, 2));
        private final NumberSetting axe     = add(new NumberSetting("Axe Slot", 1, 9, 1, 3));
        private final NumberSetting blocks  = add(new NumberSetting("Blocks Slot", 1, 9, 1, 4));
        private final NumberSetting gapple  = add(new NumberSetting("Gapple Slot", 1, 9, 1, 5));
        private final NumberSetting pearl   = add(new NumberSetting("Pearl Slot", 1, 9, 1, 6));
        private final BooleanSetting clean  = add(new BooleanSetting("Drop Junk", true));
        private final BooleanSetting onlyOpen = add(new BooleanSetting("Only With Inventory Open", false));
        private int timer;

        public InvManagerModule() { super("InvManager", "Sorts tools/blocks into your hotbar, drops junk", Category.INVENTORY); }

        private static int tier(Item i) {
            String p = Registries.ITEM.getId(i).getPath();
            return p.startsWith("netherite") ? 5 : p.startsWith("diamond") ? 4 : p.startsWith("iron") ? 3
                    : p.startsWith("stone") ? 2 : p.startsWith("golden") ? 1 : 0;
        }

        private static boolean isBlock(ItemStack s) {
            return s.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BlockWithEntity)
                    && bi.getBlock().getDefaultState().isFullCube(net.minecraft.client.MinecraftClient.getInstance().world, BlockPos.ORIGIN);
        }

        private int rank(int rule, ItemStack s) {
            if (s.isEmpty()) return -1;
            return switch (rule) {
                case 0 -> s.getItem() instanceof SwordItem ? tier(s.getItem()) : -1;
                case 1 -> s.getItem() instanceof PickaxeItem ? tier(s.getItem()) : -1;
                case 2 -> s.getItem() instanceof AxeItem ? tier(s.getItem()) : -1;
                case 3 -> isBlock(s) ? s.getCount() : -1;
                case 4 -> s.isOf(Items.ENCHANTED_GOLDEN_APPLE) ? 1000 + s.getCount() : s.isOf(Items.GOLDEN_APPLE) ? s.getCount() : -1;
                default -> s.isOf(Items.ENDER_PEARL) ? s.getCount() : -1;
            };
        }

        @Override public void onTick() {
            if (timer-- > 0) return;
            boolean invOpen = mc.currentScreen instanceof InventoryScreen;
            if (mc.currentScreen != null && !invOpen) return;
            if (onlyOpen.get() && !invOpen) return;
            var slots = mc.player.playerScreenHandler.slots;
            int sync = mc.player.playerScreenHandler.syncId;
            NumberSetting[] target = { sword, pick, axe, blocks, gapple, pearl };
            for (int rule = 0; rule < target.length; rule++) {
                int hot = target[rule].getInt() - 1, hotId = 36 + hot;
                int cur = rank(rule, slots.get(hotId).getStack()), best = cur, bestId = -1;
                for (int id = 9; id <= 44; id++) {
                    if (id == hotId) continue;
                    int r = rank(rule, slots.get(id).getStack());
                    if (r > best) { best = r; bestId = id; }
                }
                if (bestId >= 0) {
                    mc.interactionManager.clickSlot(sync, bestId, hot, SlotActionType.SWAP, mc.player);
                    timer = delay.getInt();
                    return;
                }
            }
            if (!clean.get()) return;
            for (int id = 9; id <= 44; id++) {
                if (!JUNK.contains(slots.get(id).getStack().getItem())) continue;
                mc.interactionManager.clickSlot(sync, id, 1, SlotActionType.THROW, mc.player);
                timer = delay.getInt();
                return;
            }
        }
    }

    // ================================================================ more HUD mods
    public static final class ClockModule extends HudElement {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
        public ClockModule() { super("Clock", "Real-time clock", 6, 262); }
        @Override public int width() { return 64; }
        @Override public int height() { return 18; }
        @Override public void draw(DrawContext c, int x, int y) {
            c.fill(x, y, x + width(), y + height(), 0xA0000000);
            RenderUtil.outline(c, x, y, x + width(), y + height(), 0xFF000000);
            RenderUtil.text(c, LocalTime.now().format(FMT), x + 8, y + 5, RenderUtil.TEXT, 0.85f);
        }
    }

    public static final class BpsModule extends HudElement {
        private double bps; private Vec3d last;
        public BpsModule() { super("BPS", "Blocks per second", 80, 262); }
        @Override public int width() { return 74; }
        @Override public int height() { return 18; }
        @Override public void onTick() {
            Vec3d p = mc.player.getPos();
            if (last != null) bps = Math.sqrt((p.x - last.x) * (p.x - last.x) + (p.z - last.z) * (p.z - last.z)) * 20;
            last = p;
        }
        @Override public void draw(DrawContext c, int x, int y) {
            c.fill(x, y, x + width(), y + height(), 0xA0000000);
            RenderUtil.outline(c, x, y, x + width(), y + height(), 0xFF000000);
            RenderUtil.text(c, String.format("BPS  %.1f", bps), x + 6, y + 5, RenderUtil.TEXT, 0.85f);
        }
    }

    public static final class DirectionModule extends HudElement {
        public DirectionModule() { super("Direction", "Which way you are facing", 6, 286); }
        @Override public int width() { return 96; }
        @Override public int height() { return 18; }
        @Override public void draw(DrawContext c, int x, int y) {
            if (mc.player == null) return;
            String f = switch (mc.player.getHorizontalFacing()) {
                case NORTH -> "North  -Z"; case SOUTH -> "South  +Z"; case EAST -> "East  +X"; default -> "West  -X"; };
            c.fill(x, y, x + width(), y + height(), 0xA0000000);
            RenderUtil.outline(c, x, y, x + width(), y + height(), 0xFF000000);
            RenderUtil.text(c, f, x + 6, y + 5, RenderUtil.TEXT, 0.85f);
        }
    }

    public static final class ServerInfoModule extends HudElement {
        public ServerInfoModule() { super("ServerInfo", "Server address", 6, 310); }
        private String text() {
            var e = mc.getCurrentServerEntry();
            return e == null ? "Singleplayer" : e.address;
        }
        @Override public int width() { return RenderUtil.width(text(), 0.85f) + 14; }
        @Override public int height() { return 18; }
        @Override public void draw(DrawContext c, int x, int y) {
            c.fill(x, y, x + width(), y + height(), 0xA0000000);
            RenderUtil.outline(c, x, y, x + width(), y + height(), 0xFF000000);
            RenderUtil.text(c, text(), x + 7, y + 5, RenderUtil.ASH, 0.85f);
        }
    }

    public static final class ItemCounterModule extends HudElement {
        private static final Item[] ITEMS = { Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.GOLDEN_APPLE, Items.ENDER_PEARL,
                Items.RESPAWN_ANCHOR, Items.GLOWSTONE, Items.WIND_CHARGE };
        public ItemCounterModule() { super("ItemCounter", "Counts your PvP items", 6, 334); }
        @Override public int width() { return 52; }
        @Override public int height() { return ITEMS.length * 18; }
        @Override public void draw(DrawContext c, int x, int y) {
            if (mc.player == null) return;
            for (int i = 0; i < ITEMS.length; i++) {
                int n = mc.player.getInventory().count(ITEMS[i]);
                c.drawItem(new ItemStack(ITEMS[i]), x, y + i * 18);
                RenderUtil.text(c, "x" + n, x + 20, y + i * 18 + 5, n > 0 ? RenderUtil.TEXT : RenderUtil.DIM, 0.85f);
            }
        }
    }
}
