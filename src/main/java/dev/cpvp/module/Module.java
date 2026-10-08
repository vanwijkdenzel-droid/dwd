package dev.cpvp.module;

import dev.cpvp.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    private static long lastStatus;

    private final String name, description;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;
    private int key = -1;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected <T extends Setting> T add(T setting) { settings.add(setting); return setting; }

    /** Throttled action-bar message so you can see WHY a module isn't doing anything. */
    protected static void status(String msg) {
        long n = System.currentTimeMillis();
        if (mc.player == null || n - lastStatus < 800) return;
        lastStatus = n;
        mc.player.sendMessage(Text.literal("\u00a7a[CPVP] \u00a7f" + msg), true);
    }

    public void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean v) {
        if (v == enabled) return;
        enabled = v;
        dev.cpvp.ProfileManager.markDirty();
        if (v) onEnable(); else onDisable();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public List<Setting> getSettings() { return Collections.unmodifiableList(settings); }

    /** Called when this module's keybind is pressed. Default: toggle. Action modules override it. */
    public void onKeyPress() { toggle(); }
    /** Modules whose key triggers an action that must keep ticking even while "disabled". */
    public boolean ticksWhenDisabled() { return false; }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onFrame() {}
    public void onRightClick() {}
    public void onLeftClick() {}
    public void onHudRender(DrawContext ctx) {}
    public void onWorldRender(WorldRenderContext ctx) {}
}
