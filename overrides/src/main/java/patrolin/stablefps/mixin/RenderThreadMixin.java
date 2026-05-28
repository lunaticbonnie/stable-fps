package patrolin.stablefps.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.LevelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import patrolin.stablefps.StableFPS;

@Mixin(Minecraft.class)
public class RenderThreadMixin {
  // inputThread
  @WrapMethod(method="setScreen")
  private void setScreen(Screen screen, Operation<Void> original) {
    StableFPS.LOGGER.info("setScreen()");
    StableFPS.runOnRenderThread(() -> original.call(screen));
  }
  @WrapMethod(method="selectLevel")
  private void selectLevel(String string, String string2, LevelSettings levelSettings, Operation<Void> original) {
    StableFPS.LOGGER.info("selectLevel()");
    StableFPS.runOnRenderThread(() -> original.call(string, string2, levelSettings));
  }
  @WrapMethod(method="clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V")
  private void clearLevel(Screen screen, Operation<Void> original) {
    StableFPS.LOGGER.info("clearLevel()");
    StableFPS.runOnRenderThread(() -> original.call(screen));
  }
  // renderThread
  @WrapOperation(method="runTick", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/GLX;shouldClose(Lcom/mojang/blaze3d/platform/Window;)Z"))
  private boolean runTick(Window window, Operation<Boolean> original) {
    WindowEventHandler resize_eventHandler = null;
    StableFPS.RenderThreadEvent event;
    while ((event = StableFPS.renderThread_events.poll()) != null) {
      switch (event) {
        case StableFPS.ResizeDisplayEvent e:
          resize_eventHandler = e.eventHandler();
          break;
        case StableFPS.RunOnRenderThreadEvent e:
          e.callback().run();
          //e.result.submit(null);
          break;
      }
    }
    if (resize_eventHandler != null) {
      /* NOTE: `this` must refer to the `Minecraft` for this to work */
      resize_eventHandler.resizeDisplay();
    }
    boolean shouldClose = original.call(window);
    if (shouldClose) StableFPS.shouldClose = true;
    return shouldClose;
  }
}
