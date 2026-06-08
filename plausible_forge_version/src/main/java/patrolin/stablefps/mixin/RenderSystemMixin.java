package patrolin.stablefps.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import patrolin.stablefps.StableFPS;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(method="initRenderer", at=@At(value = "HEAD"))
    private static void onInit(CallbackInfo ci) {
        StableFPS.LOGGER.info("ayaya.setup_listenersReady");
        StableFPS.setup_windowListenersReady.countDown();
    }
    /**
     * @author Patrolin
     * @reason don't call `GLFW.glfwPollEvents()` on the render thread
     */
    @Overwrite
    public static void pollEvents() {}
}
