package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import patrolin.stablefps.ForgeWindowAccessor;
import patrolin.stablefps.StableFPS;
import patrolin.stablefps.StableFPS.InputThreadEvent;
import patrolin.stablefps.StableFPS.GrabMouseEvent;
import patrolin.stablefps.StableFPS.ShouldCloseEvent;
import patrolin.stablefps.StableFPS.RenderThreadEvent;
import patrolin.stablefps.StableFPS.ResizeDisplayEvent;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Mixin(Window.class)
public class WindowMixin {
	// inputThread
	@Redirect(
		method="<init>",
		at=@At(value="INVOKE", target="Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(IILjava/lang/String;JLjava/util/function/Supplier;)J", remap=false)
	)
	private long setupMinecraftWindow(int width, int height, String title, long monitor, Supplier<Object> backend) {
		StableFPS.inputThread = new Thread(() -> {
			try {
				// open the window
				StableFPS.window = ForgeWindowAccessor.recreateForgeWindow(width, height, title, monitor, 0L);
				GLFW.glfwPollEvents(); /* NOTE: prevent race condition with initializing the window size */
				StableFPS.window_ready.countDown();
				while (!StableFPS.forge_is_setup.await(0, TimeUnit.MILLISECONDS)) {
					GLFW.glfwPollEvents(); /* NOTE: prevent race condition with initializing OpenGL */
				}
				GLFW.glfwShowWindow(StableFPS.window);
				while (true) {
					// handle inputThread events
					InputThreadEvent event;
					while ((event = StableFPS.inputThread_events.poll()) != null) {
						switch (event.type) {
							case InputThreadEvent.GRAB_MOUSE_EVENT: {
								GrabMouseEvent e = (GrabMouseEvent)event;
								GLFW.glfwSetCursorPos(e.window, e.x, e.y);
								GLFW.glfwSetInputMode(e.window, 208897, e.input_mode);
								break;
							}
							case InputThreadEvent.SHOULD_CLOSE_EVENT: {
								ShouldCloseEvent e = (ShouldCloseEvent) event;
								e.result.submit(null);
								return;
							}
						}
					}
					// handle window events
					GLFW.glfwPollEvents();
				}
			} catch (Exception err) {
				StableFPS.LOGGER.error("", err);
				System.exit(1);
			}
		}, "Async input thread");
		StableFPS.inputThread.start();
		// wait for the window to be opened
		try {
			StableFPS.window_ready.await();
			//long forgeEarlyWindow = net.minecraftforge.fml.loading.ImmediateWindowHandler.setupMinecraftWindow(width, height, title, monitor, backend);
			ForgeWindowAccessor.closeForgeEarlyWindow();
			ForgeWindowAccessor.recreateForgeFramebuffer(StableFPS.window);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return StableFPS.window;
	}
	@Inject(method="<init>", at=@At("RETURN"))
	private void afterInit(CallbackInfo ci) {
		StableFPS.forge_is_setup.countDown();
	}
	@Redirect(
		method = "onFramebufferResize",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/WindowEventHandler;resizeGui()V")
	)
	private void onFramebufferResize(WindowEventHandler eventHandler) {
		StableFPS.resizeDisplay(eventHandler);
	}

	// renderThread
	@Inject(method="shouldClose", at=@At("HEAD"))
	private void onRunTick(CallbackInfoReturnable<Boolean> cir) {
		WindowEventHandler resize_eventHandler = null;
		RenderThreadEvent event;
		while ((event = StableFPS.renderThread_events.poll()) != null) {
			switch (event.type) {
				case RenderThreadEvent.RESIZE_DISPLAY_EVENT: {
					ResizeDisplayEvent e = (ResizeDisplayEvent) event;
					resize_eventHandler = e.eventHandler;
					break;
				}
			}
		}
		if (resize_eventHandler != null) {
			WindowAccessorMixin window = (WindowAccessorMixin) this;
			window.stableFPS_setIsResized(true);
			/* NOTE: `this` must refer the `Window` for this to work */
			resize_eventHandler.resizeGui();
		}
	}
}