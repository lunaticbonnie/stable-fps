package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import patrolin.stablefps.StableFPS;

@Mixin(Window.class)
public class WindowMixin {
	// inputThread
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
				StableFPS.window = GLFW.glfwCreateWindow(width, height, title, monitor, share);
				StableFPS.window_ready.countDown();
				while (!StableFPS.shouldClose) {
					// handle inputThread events
					StableFPS.InputThreadEvent event;
					while ((event = StableFPS.inputThread_events.poll()) != null) {
						switch (event) {
							case StableFPS.GrabMouseEvent e:
								GLFW.glfwSetCursorPos(e.window(), e.x(), e.y());
								GLFW.glfwSetInputMode(e.window(), 208897, e.input_mode());
								break;
						}
					}
					// handle window events
					GLFW.glfwPollEvents();
				}
			} catch (Exception err) {
        try {
          Thread.sleep(16);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
				if (!StableFPS.shouldClose) {
					StableFPS.LOGGER.error("", err);
					System.exit(1);
				}
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
		method="onFramebufferResize",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/WindowEventHandler;resizeDisplay()V")
	)
	private void onFramebufferResize(WindowEventHandler eventHandler) {
		System.exit(1);
		StableFPS.resizeDisplay(eventHandler);
	}
}