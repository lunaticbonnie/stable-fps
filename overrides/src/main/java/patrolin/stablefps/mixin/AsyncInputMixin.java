package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import patrolin.stablefps.StableFPS;

import java.util.concurrent.CountDownLatch;

@Mixin(Window.class)
public class AsyncInputMixin {
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
	private long replaceWindowCreation(
		int width,
		int height,
		CharSequence title,
		long monitor,
		long share
	) {
		new Thread(() -> {
			window = GLFW.glfwCreateWindow(width, height, title, monitor, share);
			ready.countDown();
			while (!GLFW.glfwWindowShouldClose(window)) {
				long mouse_window = StableFPS.mouse_window;
				int mouse_input_mode = StableFPS.mouse_input_mode;
				double mouse_x = StableFPS.mouse_x;
				double mouse_y = StableFPS.mouse_y;
				if (StableFPS.do_grabOrReleaseMouse.getAndSet(false)) {
					GLFW.glfwSetCursorPos(mouse_window, mouse_x, mouse_y);
					GLFW.glfwSetInputMode(mouse_window, 208897, mouse_input_mode);
				}
				GLFW.glfwPollEvents();
			}
		}, "Async input thread").start();

        try {
            ready.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return window;
	}
}