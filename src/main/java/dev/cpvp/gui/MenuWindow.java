package dev.cpvp.gui;

import dev.cpvp.module.Category;

import java.util.ArrayList;
import java.util.List;

/** A movable menu window. Category panels and popups all use this so the layout editor can move them too. */
public final class MenuWindow {
    /** Draw order - last entry is on top. */
    public static final List<MenuWindow> ALL = new ArrayList<>();

    public final String key, title;
    public final Category cat;          // null for popups (Profiles / Mods / Settings)
    public int x, y, w, h = 180;
    public boolean open;
    public final float[] sc = new float[2];   // scroll {target, current}

    private MenuWindow(String key, String title, Category cat, int x, int y, int w, boolean open) {
        this.key = key; this.title = title; this.cat = cat; this.x = x; this.y = y; this.w = w; this.open = open;
        ALL.add(this);
    }

    public static MenuWindow NAV, PROFILES, MODS, SETTINGS;
    private static boolean ready;

    public static void init() {
        if (ready) return;
        ready = true;
        NAV = new MenuWindow("NAV", "King Client", null, 8, 8, 150, true);
        Category[] cats = { Category.COMBAT, Category.RENDER, Category.UTILITY, Category.WORLD, Category.INVENTORY, Category.NETWORK };
        for (int i = 0; i < cats.length; i++)
            new MenuWindow(cats[i].name(), cats[i].label, cats[i], 166 + i * 184, 8, 176, i == 0);
        PROFILES = new MenuWindow("PROFILES", "Profiles", null, 60, 90, 180, false);
        MODS     = new MenuWindow("MODS", "Mods", null, 100, 100, 232, false);
        SETTINGS = new MenuWindow("SETTINGS", "Settings", null, 140, 110, 252, false);
    }

    public static MenuWindow of(Category c) {
        for (MenuWindow w : ALL) if (w.cat == c) return w;
        return null;
    }

    public void front() { ALL.remove(this); ALL.add(this); }
}
