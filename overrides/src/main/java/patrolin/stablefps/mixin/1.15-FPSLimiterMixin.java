package patrolin.stablefps.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;

@Mixin(RenderSystem.class)
public class FPSLimiterMixin {
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
		double dt = (1.0 / target_fps) + 1e-9;
		double nextDrawTime = lastDrawTime + dt;
		double now_s = System.nanoTime() / 1e9;
		double remaining_timeout = nextDrawTime - now_s;
		if (remaining_timeout < -dt) {
			/* NOTE: we fell too far behind, restart */
			nextDrawTime = now_s;
		}
		// wait until the correct time
		do {
			// NOTE: wait for OS input events with a positive timeout, or zero timeout if we're behind
			GLFW.glfwWaitEventsTimeout(Math.max(remaining_timeout, 0));
			remaining_timeout = nextDrawTime - (System.nanoTime() / 1e9);
		} while (remaining_timeout > 0);
		lastDrawTime = nextDrawTime;
	}
}