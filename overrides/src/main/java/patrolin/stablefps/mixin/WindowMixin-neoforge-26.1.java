package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.neoforged.fml.loading.EarlyLoadingScreenController;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import patrolin.stablefps.NeoforgeWindowAccessor;
import patrolin.stablefps.StableFPS;
import patrolin.stablefps.StableFPS.InputThreadEvent;
import patrolin.stablefps.StableFPS.GrabMouseEvent;
import patrolin.stablefps.StableFPS.ShouldCloseEvent;
import patrolin.stablefps.StableFPS.RenderThreadEvent;
import patrolin.stablefps.StableFPS.ResizeDisplayEvent;

import java.util.concurrent.TimeUnit;

@Mixin(Window.class)
public class WindowMixin {
	// inputThread
	@Redirect(
		method="createGlfwWindow",
		at=@At(value="INVOKE", target="Lnet/neoforged/fml/loading/EarlyLoadingScreenController;takeOverGlfwWindow()J", remap=false)
	)
	private static long windowFromEarlyForgeWindow(EarlyLoadingScreenController instance) {
		StableFPS.inputThread = new Thread(() -> {
			try {
				// open the window
				StableFPS.window = NeoforgeWindowAccessor.recreateForgeWindow("...", 0L);
				GLFW.glfwPollEvents(); /* NOTE: prevent race condition with initializing the window size */
				StableFPS.window_ready.countDown();
				while (!StableFPS.forge_is_setup.await(0, TimeUnit.MILLISECONDS)) {
					GLFW.glfwPollEvents(); /* NOTE: prevent race condition with initializing OpenGL */
				}
				GLFW.glfwSetWindowPos(StableFPS.window, 100, 100); /* NOTE: prevent race condition in neoforge */
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
			//long forgeEarlyWindow = net.minecraftforge.fml.loading.ImmediateWindowHandler.setupMinecraftWindow(width, height, title, monitor);
			NeoforgeWindowAccessor.closeForgeEarlyWindow();
			NeoforgeWindowAccessor.recreateForgeFramebuffer(StableFPS.window);
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
		method="createGlfwWindow",
		at=@At(value="INVOKE", target="Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J")
	)
	private static long glfwCreateWindow(
		int width,
		int height,
		CharSequence title,
		long monitor,
		long share
  ) {
		StableFPS.inputThread = new Thread(() -> {
			try {
				// open the window
				StableFPS.window = GLFW.glfwCreateWindow(width, height, title, monitor, share);
				GLFW.glfwPollEvents(); /* NOTE: prevent race condition with initializing the window size */
				StableFPS.window_ready.countDown();
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
		} catch (InterruptedException err) {
			throw new RuntimeException(err);
		}
		return StableFPS.window;
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