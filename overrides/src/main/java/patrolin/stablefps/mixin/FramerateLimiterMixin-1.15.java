package patrolin.stablefps.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;

@Mixin(RenderSystem.class)
public class FramerateLimiterMixin {
	@Unique
	private static long lastFrameTime;
	@Shadow
	private static double lastDrawTime;

	/**
	 * @author Patrolin
	 * @reason The original function has too many bizarre decisions.
	 * From using `GLFW.glfwGetTime()` instead of `System.nanoTime()`
	 * to incorrectly waiting 1/60th of a second every frame regardless of how much time already passed.
	 * We would have to target and replace every single line anyway.
	 */
	@Overwrite
	public static void limitDisplayFPS(int target_fps) {
		// NOTE: pretend that we are always running at a stable framerate by default
		long dt = (1_000_000_000L / target_fps) + 1;
		long nextDrawTime = lastFrameTime + dt;
		long now_ns = System.nanoTime();
		long remaining_ns = nextDrawTime - now_ns;
		if (remaining_ns < -dt) {
			/* NOTE: we fell too far behind, restart */
			nextDrawTime = now_ns;
		}
		// wait until the correct time
		long sleep_ms = remaining_ns / 1_000_000L;
		try {
			if (sleep_ms > 0) Thread.sleep(sleep_ms);
		} catch (InterruptedException ignored) {}
		while (nextDrawTime - System.nanoTime() > 0) {
			Thread.onSpinWait();
		}
		lastFrameTime = nextDrawTime;
		lastDrawTime = nextDrawTime*1e-9;
	}
}