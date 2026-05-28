package patrolin.stablefps.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import patrolin.stablefps.StableFPS;

@Mixin(Minecraft.class)
public class RenderThreadMixin {
  // render thread
  @WrapOperation(method="runTick", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/GLX;shouldClose(Lcom/mojang/blaze3d/platform/Window;)Z"))
  private boolean runTick(Window window, Operation<Boolean> original) {
    WindowEventHandler resize_eventHandler = null;
    StableFPS.RenderThreadEvent event;
    while ((event = StableFPS.renderThread_events.poll()) != null) {
      switch (event) {
        case StableFPS.ResizeDisplayEvent e:
          resize_eventHandler = e.eventHandler();
          break;
      }
    }
    if (resize_eventHandler != null) {
      /* NOTE: `this` must refer the `Window` for this to work */
      resize_eventHandler.resizeDisplay();
    }
    boolean shouldClose = original.call(window);
    if (shouldClose) StableFPS.shouldClose.set(true);
    return shouldClose;
  }
}
