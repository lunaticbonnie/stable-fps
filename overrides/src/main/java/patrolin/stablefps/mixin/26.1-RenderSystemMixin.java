package patrolin.stablefps.mixin;

import net.minecraft.client.FramerateLimiter;
import org.spongepowered.asm.mixin.*;

@Mixin(FramerateLimiter.class)
public abstract class RenderSystemMixin {
	@Shadow
	private static long lastFrameTime;

	/**
	 * @author Patrolin
	 * @reason The original function has too many bizarre decisions.
	 * From using `GLFW.glfwGetTime()` instead of `System.nanoTime()`
	 * to incorrectly trying to use out-of-date information.
	 * We would have to target and replace every single line anyway.
	 */
	@Overwrite
	public static void limitDisplayFPS(final int target_fps) {
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
		} catch (InterruptedException _) {}
		while (nextDrawTime - System.nanoTime() > 0) {
			Thread.onSpinWait();
		}
		lastFrameTime = nextDrawTime;
	}
}