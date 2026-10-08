package dev.cpvp.setting;

import net.minecraft.util.math.MathHelper;

public final class NumberSetting extends Setting {
    private final double min, max, step;
    private double value;

    public NumberSetting(String name, double min, double max, double step, double def) {
        super(name);
        this.min = min; this.max = max; this.step = step;
        set(def);
    }

    public double get() { return value; }
    public int getInt() { return (int) Math.round(value); }
    public double min() { return min; }
    public double max() { return max; }

    public void set(double v) {
        double snapped = Math.round(v / step) * step;
        value = MathHelper.clamp(Math.round(snapped * 1000.0) / 1000.0, min, max);
    }

    public String display() {
        return step >= 1.0 ? String.valueOf(getInt()) : String.format("%.1f", value);
    }
}
