package dev.cpvp.module;

public enum Category {
    COMBAT("Combat"), RENDER("Render"), UTILITY("Utility"), WORLD("World"),
    INVENTORY("Inventory"), NETWORK("Network"),
    SETTINGS("Settings"),   // floating window
    MODS("Mods");           // floating window: HUD mods (armor, CPS, ...)
    public final String label;
    Category(String label) { this.label = label; }
}
