package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Window.class)
public class RenderSystemMixin {
    /**
     * @author Patrolin
     * @reason don't call `GLFW.glfwPollEvents()` on the render thread
     */
    @Overwrite
    public static void pollEventQueue() {}
}
