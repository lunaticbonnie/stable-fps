package patrolin.stablefps.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.LevelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import patrolin.stablefps.StableFPS;
import patrolin.stablefps.StableFPS.RenderThreadEvent;
import patrolin.stablefps.StableFPS.ResizeDisplayEvent;
import patrolin.stablefps.StableFPS.RunOnRenderThreadEvent;

@Mixin(Minecraft.class)
public class MinecraftMixin {
  // inputThread
  @WrapMethod(method="setScreen")
  private void setScreen(Screen screen, Operation<Void> original) {
    StableFPS.runOnRenderThread(() -> original.call(screen));
  }
  @WrapMethod(method="selectLevel")
  private void selectLevel(String string, String string2, LevelSettings levelSettings, Operation<Void> original) {
    StableFPS.runOnRenderThread(() -> original.call(string, string2, levelSettings));
  }
  @WrapMethod(method="clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V")
  private void clearLevel(Screen screen, Operation<Void> original) {
    StableFPS.runOnRenderThread(() -> original.call(screen));
  }
  // renderThread
  @Inject(method="runTick", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/GLX;shouldClose(Lcom/mojang/blaze3d/platform/Window;)Z"))
  private void runTick(CallbackInfo ci) {
    WindowEventHandler resize_eventHandler = null;
    RenderThreadEvent event;
    while ((event = StableFPS.renderThread_events.poll()) != null) {
      switch (event.type) {
        case RenderThreadEvent.RESIZE_DISPLAY_EVENT: {
          ResizeDisplayEvent e = (ResizeDisplayEvent) event;
          resize_eventHandler = e.eventHandler;
          break;
        }
        case RenderThreadEvent.RUN_ON_RENDER_THREAD_EVENT: {
          RunOnRenderThreadEvent e = (RunOnRenderThreadEvent) event;
          e.callback.run();
          //e.result.submit(null);
          break;
        }
      }
    }
    if (resize_eventHandler != null) {
      /* NOTE: `this` must refer to the `Minecraft` for this to work */
      resize_eventHandler.resizeDisplay();
    }
  }
  @Inject(method="close", at=@At("HEAD"))
  private static void onClose(CallbackInfo ci) {
    StableFPS.shouldClose();
  }
}
