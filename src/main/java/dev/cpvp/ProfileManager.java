package dev.cpvp;

import com.google.gson.*;
import dev.cpvp.gui.MenuWindow;
import dev.cpvp.module.Module;
import dev.cpvp.module.ModuleManager;
import dev.cpvp.setting.BooleanSetting;
import dev.cpvp.setting.NumberSetting;
import dev.cpvp.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * JSON configs in .minecraft/config/apex/. The active profile is loaded when the game starts
 * (settings + keybinds immediately, module on/off once you are in a world) and saved automatically.
 */
public final class ProfileManager {
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("fogv1");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<String> cache = new ArrayList<>(List.of("Default"));
    private static final Map<Module, Boolean> pending = new HashMap<>();
    private static boolean dirty;
    private static long lastSave;
    public static String current = "Default";

    private ProfileManager() {}

    public static void init() {
        try { Files.createDirectories(DIR); } catch (IOException ignored) {}
        if (!Files.exists(file("Default"))) save("Default");
        preset("Base Finding", Set.of("Xray", "StorageESP", "SpawnerFinder", "ItemESP", "Logo", "FPS", "ModuleList", "Interface"));
        preset("Visuals", Set.of("ESP", "NameTags", "Arrows", "Health", "Tracers", "Logo", "FPS", "ModuleList", "Interface"));
        try {
            String c = Files.readString(DIR.resolve("current.txt")).trim();
            if (Files.exists(file(c))) current = c;
        } catch (IOException ignored) {}
        refresh();
        apply(current, false);
    }

    public static List<String> names() { return cache; }
    public static void markDirty() { dirty = true; }

    private static Path file(String n) { return DIR.resolve(n + ".json"); }

    private static void refresh() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DIR)) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5)).sorted().forEach(out::add);
        } catch (IOException ignored) {}
        if (!out.contains("Default")) out.add(0, "Default");
        cache = out;
    }

    /** Called every tick: applies the startup on/off states once in a world, autosaves when something changed. */
    public static void tick(boolean inWorld) {
        if (inWorld && !pending.isEmpty()) {
            for (Map.Entry<Module, Boolean> e : pending.entrySet()) {
                try { e.getKey().setEnabled(e.getValue()); } catch (Exception ignored) {}
            }
            pending.clear();
        }
        long now = System.currentTimeMillis();
        if (dirty && pending.isEmpty() && now - lastSave > 3000) saveCurrent();
    }

    /** Writes a ready-made profile once: only the listed modules on, everything else off. */
    private static void preset(String name, Set<String> on) {
        if (Files.exists(file(name))) return;
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.INSTANCE.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", on.contains(m.getName()));
            o.addProperty("key", m.getKey());
            JsonObject st = new JsonObject();
            for (Setting s : m.getSettings()) {
                if (s instanceof NumberSetting n) st.addProperty(s.getName(), n.get());
                else if (s instanceof BooleanSetting b) st.addProperty(s.getName(), b.get());
            }
            o.add("settings", st);
            root.add(m.getName(), o);
        }
        try { Files.writeString(file(name), GSON.toJson(root)); } catch (IOException ignored) {}
    }

    public static void saveCurrent() { save(current); }

    public static void save(String name) {
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.INSTANCE.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            JsonObject st = new JsonObject();
            for (Setting s : m.getSettings()) {
                if (s instanceof NumberSetting n) st.addProperty(s.getName(), n.get());
                else if (s instanceof BooleanSetting b) st.addProperty(s.getName(), b.get());
            }
            o.add("settings", st);
            root.add(m.getName(), o);
        }
        JsonObject ws = new JsonObject();
        for (MenuWindow w : MenuWindow.ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("x", w.x); o.addProperty("y", w.y); o.addProperty("open", w.open);
            ws.add(w.key, o);
        }
        root.add("_windows", ws);
        try {
            Files.writeString(file(name), GSON.toJson(root));
            Files.writeString(DIR.resolve("current.txt"), current);
        } catch (IOException ignored) {}
        dirty = false;
        lastSave = System.currentTimeMillis();
    }

    public static void load(String name) { apply(name, true); }

    private static void apply(String name, boolean enableNow) {
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file(name))).getAsJsonObject();
            for (Module m : ModuleManager.INSTANCE.all()) {
                if (!root.has(m.getName())) continue;
                JsonObject o = root.getAsJsonObject(m.getName());
                if (o.has("key")) m.setKey(o.get("key").getAsInt());
                JsonObject st = o.has("settings") ? o.getAsJsonObject("settings") : new JsonObject();
                for (Setting s : m.getSettings()) {
                    if (!st.has(s.getName())) continue;
                    if (s instanceof NumberSetting n) n.set(st.get(s.getName()).getAsDouble());
                    else if (s instanceof BooleanSetting b) b.set(st.get(s.getName()).getAsBoolean());
                }
                if (o.has("enabled")) {
                    boolean en = o.get("enabled").getAsBoolean();
                    if (enableNow) { try { m.setEnabled(en); } catch (Exception ignored) {} }
                    else if (en != m.isEnabled()) pending.put(m, en);
                }
            }
            if (root.has("_windows")) {
                JsonObject ws = root.getAsJsonObject("_windows");
                for (MenuWindow w : MenuWindow.ALL) {
                    if (!ws.has(w.key)) continue;
                    JsonObject o = ws.getAsJsonObject(w.key);
                    if (o.has("x")) w.x = o.get("x").getAsInt();
                    if (o.has("y")) w.y = o.get("y").getAsInt();
                    if (o.has("open")) w.open = o.get("open").getAsBoolean();
                }
            }
            current = name;
        } catch (Exception ignored) {}
    }

    public static void create() {
        int i = 1;
        while (Files.exists(file("Profile " + i))) i++;
        current = "Profile " + i;
        save(current);
        refresh();
    }

    public static void delete(String name) {
        if (name.equals("Default")) return;
        try { Files.deleteIfExists(file(name)); } catch (IOException ignored) {}
        if (current.equals(name)) current = "Default";
        refresh();
    }
}
