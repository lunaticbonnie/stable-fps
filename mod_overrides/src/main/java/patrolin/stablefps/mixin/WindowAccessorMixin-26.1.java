package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Window.class)
public interface WindowAccessorMixin {
    @Accessor("isResized")
    void stableFPS_setIsResized(boolean newValue);
}
