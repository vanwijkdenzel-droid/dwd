package dev.cpvp.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mouse.class)
public interface MouseAccessor {
    @Accessor("x") void cpvp$setX(double x);
    @Accessor("y") void cpvp$setY(double y);
}
