package dev.cpvp.setting;

public final class BooleanSetting extends Setting {
    private boolean value;
    public BooleanSetting(String name, boolean def) { super(name); this.value = def; }
    public boolean get() { return value; }
    public void set(boolean v) { value = v; }
    public void toggle() { value = !value; }
}
