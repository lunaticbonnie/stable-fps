package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import patrolin.stablefps.StableFPS;

@Mixin(Minecraft.class)
public class RenderThreadMixin {
  // render thread
  @Inject(method="runTick", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/GLX;shouldClose(Lcom/mojang/blaze3d/platform/Window;)Z"))
  private void runTick(CallbackInfo ci) {
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
  }
}
