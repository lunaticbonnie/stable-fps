package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import patrolin.stablefps.StableFPS;

import java.util.concurrent.CountDownLatch;

@Mixin(Window.class)
public class WindowMixin {
	// input thread
	@Unique
	private static final CountDownLatch ready = new CountDownLatch(1);
	@Unique
	private static volatile long window;

	@Redirect(
		method = "createGlfwWindow",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J")
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
				window = GLFW.glfwCreateWindow(width, height, title, monitor, share);
				ready.countDown();
				while (!GLFW.glfwWindowShouldClose(window)) {
					StableFPS.InputThreadEvent event;
					while ((event = StableFPS.inputThread_events.poll()) != null) {
						switch (event) {
							case StableFPS.GrabMouseEvent e:
								GLFW.glfwSetCursorPos(e.window(), e.x(), e.y());
								GLFW.glfwSetInputMode(e.window(), 208897, e.input_mode());
								break;
						}
					}
					// poll events
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
			ready.await();
		} catch (InterruptedException err) {
			throw new RuntimeException(err);
		}
		return window;
	}

	@Redirect(
		method = "onFramebufferResize",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/WindowEventHandler;resizeGui()V")
	)
	private void onFramebufferResize(WindowEventHandler eventHandler) {
		StableFPS.resizeDisplay(eventHandler);
	}

	// render thread
	@Inject(method = "shouldClose", at = @At("HEAD"))
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
			WindowAccessorMixin window = (WindowAccessorMixin) this;
			window.stableFPS_setIsResized(true);
			/* NOTE: `this` must refer the `Window` for this to work */
			resize_eventHandler.resizeGui();
		}
	}
}