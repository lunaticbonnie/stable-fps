package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import patrolin.stablefps.StableFPS;

import java.util.concurrent.CountDownLatch;

@Mixin(Window.class)
public class AsyncInputThreadMixin {
	@Unique
	private static final CountDownLatch ready = new CountDownLatch(1);
	@Unique
	private static volatile long window;
	/**
	 * @author Patrolin
	 * @reason Minecraft does not poll quickly enough for high poll-rate mice,
	 * so we have to run it on a new async thread instead.
	 */
	@Redirect(
		method="<init>",
		at=@At(value="INVOKE", target="Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J")
	)
	private long glfwCreateWindow(
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
		method="onFramebufferResize",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/WindowEventHandler;resizeDisplay()V")
	)
	private void onFramebufferResize(WindowEventHandler eventHandler) {
		StableFPS.resizeDisplay(eventHandler);
	}
}