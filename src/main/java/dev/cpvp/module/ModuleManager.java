package dev.cpvp.module;

import dev.cpvp.module.impl.Modules.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public final class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {}

    public void init() {
        for (Module m : new Module[] {
                // Combat
                new AimAssistModule(), new AutoClickerModule(), new VelocityModule(), new KillAuraModule(),
                new SilentAuraModule(), new RightClickerModule(), new HitSelectModule(), new WTapModule(),
                new TriggerBotModule(), new ReachModule(), new CrystalAuraModule(), new AutoAnchorModule(),
                new SwapStunModule(), new BreachSwapModule(), new HoldExplodeModule(), new PearlCatchModule(),
                new HitboxesModule(), new AntiFireballModule(), new FastBowModule(), new JumpResetModule(), new SprintModule(),
                // Render
                new EspModule("ESP", true, true, false), new ChamsModule(), new TracersModule(),
                new EspModule("StorageESP", false, false, true), new EspModule("ItemESP", false, true, false),
                new NameTagsModule(), new HealthModule(), new ArrowsModule(), new SpawnerFinderModule(), new XRayModule(), new FullbrightModule(),
                new SearchModule(), new ProjectilesModule(), new IndicatorsModule(), new FreecamModule(), new FreelookModule(), new FovModule(), new LogoModule(), new FpsModule(), new ModuleListModule(),
                // Utility
                new AutoToolModule(), new AutoFishModule(), new FastXpModule(), new NoFallModule(), new ClutchModule(), new TargetFilterModule(), new PanicModule(),
                // World
                new ScaffoldModule(), new SafeWalkModule(), new BlockInModule(), new FastPlaceModule(), new AntiDebuffModule(), new ParkourModule(), new AntiAfkModule(),
                // Inventory
                new AutoTotemModule(), new AutoArmorModule(), new RefillModule(), new ChestStealModule(),
                new InvManagerModule(), new InvMoveModule(), new InvTotemModule(), new ThrowpotModule(), new ThrowDebuffModule(), new InvCleanerModule(),
                // Network
                new FakeLagModule(), new BlinkModule(),
                // HUD mods
                new ArmorHudModule(), new CpsModule(), new CoordsModule(), new PingModule(), new KeystrokesModule(), new PotionHudModule(),
                new ClockModule(), new BpsModule(), new DirectionModule(), new ServerInfoModule(), new ItemCounterModule(),
                // Settings window
                new InterfaceModule() }) modules.add(m);
    }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c) out.add(m);
        out.sort((x, y) -> x.getName().compareToIgnoreCase(y.getName()));
        return out;
    }

    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    private static boolean inWorld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.world != null;
    }

    public void onTick()  { if (inWorld()) for (Module m : modules) if (m.isEnabled() || m.ticksWhenDisabled()) m.onTick(); }
    private final Map<Module, Boolean> keyDown = new HashMap<>();

    /** Edge-detected keybind polling, once per rendered frame. Keys are ignored while any screen is open. */
    private void pollKeys() {
        MinecraftClient mc = MinecraftClient.getInstance();
        long win = mc.getWindow().getHandle();
        boolean free = mc.currentScreen == null;
        for (Module m : modules) {
            int k = m.getKey();
            boolean pressed = free && k >= 0 && InputUtil.isKeyPressed(win, k);
            boolean was = keyDown.getOrDefault(m, false);
            if (pressed && !was) m.onKeyPress();
            keyDown.put(m, pressed);
        }
    }

    public void onFrame() {
        if (!inWorld()) return;
        pollKeys();
        for (Module m : modules) if (m.isEnabled()) m.onFrame();
    }

    public void onHud(DrawContext ctx) {
        for (Module m : modules) if (m.isEnabled()) m.onHudRender(ctx);
    }

    public void onWorldRender(WorldRenderContext ctx) {
        if (inWorld()) for (Module m : modules) if (m.isEnabled()) m.onWorldRender(ctx);
    }

    public void onMouseClick(int button) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!inWorld() || mc.currentScreen != null) return;
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) m.onRightClick();
            else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) m.onLeftClick();
        }
    }
}
