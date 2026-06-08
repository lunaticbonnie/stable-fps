package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import patrolin.stablefps.ForgeWindowAccessor;
import patrolin.stablefps.StableFPS;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import patrolin.stablefps.StableFPS.InputThreadEvent;
import patrolin.stablefps.StableFPS.GrabMouseEvent;
import patrolin.stablefps.StableFPS.ShouldCloseEvent;
import patrolin.stablefps.StableFPS.RenderThreadEvent;
import patrolin.stablefps.StableFPS.ResizeDisplayEvent;

@Mixin(Window.class)
public class WindowMixin {
	// inputThread
	@Redirect(
			method="<init>",
			at=@At(value="INVOKE", target="Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J")
	)
	private long setupMinecraftWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
		StableFPS.inputThread = new Thread(() -> {
			try {
				StableFPS.window = ForgeWindowAccessor.recreateWindow();
				GLFW.glfwPollEvents(); /* NOTE: prevent race condition with initializing GLFW */
				StableFPS.LOGGER.info("ayaya.window_ready");
				StableFPS.window_ready.countDown();
				while (StableFPS.setup_renderThreadReady.getCount() > 0) {
					GLFW.glfwPollEvents();
				}
				StableFPS.LOGGER.info("ayaya.setup_goRecreateFb");
				//StableFPS.setup_windowListenersReady.await();
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
		// wait for the window to be opened
		try {
			ForgeWindowAccessor.closeOldWindow();
			StableFPS.inputThread.start();
			StableFPS.window_ready.await();
			StableFPS.LOGGER.info("ayaya.setupMinecraftWindow()");
			net.minecraftforge.fml.loading.ImmediateWindowHandler.setupMinecraftWindow(width, height, title, monitor);
			StableFPS.LOGGER.info("- ayaya.setup_renderThreadReady");
			StableFPS.setup_renderThreadReady.countDown();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return StableFPS.window;
	}
	@Redirect(
			method="onFramebufferResize",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/WindowEventHandler;resizeDisplay()V")
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
			/* NOTE: `this` must refer the `Window` for this to work */
			resize_eventHandler.resizeDisplay();
		}
	}
}