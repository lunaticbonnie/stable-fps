package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import patrolin.stablefps.StableFPS;

@Mixin(Window.class)
public class AsyncRenderThreadMixin {
	@Inject(method="shouldClose", at=@At("HEAD"))
	private void onRunTick(CallbackInfoReturnable<Boolean> cir) {
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