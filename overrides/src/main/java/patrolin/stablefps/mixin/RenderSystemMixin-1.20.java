package patrolin.stablefps.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    /**
     * @author Patrolin
     * @reason don't call `GLFW.glfwPollEvents()` on the render thread
     */
    @Overwrite
    public static void pollEvents() {}
}
