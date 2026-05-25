package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;

@Mixin(Window.class)
public abstract class RenderSystemMixin {
	@Shadow
	private double lastDrawTime;
	@Shadow
	public abstract int getFramerateLimit();

	/**
	 * @author Patrolin
	 * @reason The original function has too many bizarre decisions.
	 * From using `GLFW.glfwGetTime()` instead of `Util.getNanos()`
	 * to incorrectly waiting 1/60th of a second every frame regardless of how much time already passed,
	 * We would have to target and replace every single line anyway.
	 */
	@Overwrite
	public void limitDisplayFPS() {
		int target_fps = this.getFramerateLimit();
		// NOTE: pretend that we are always running at a stable framerate by default
		double dt = 1.0 / target_fps;
		double nextDrawTime = lastDrawTime + dt;
		double now_s = Util.getNanos() / 1e9;
		nextDrawTime = 0.999*nextDrawTime + 0.001*now_s; // NOTE: bias towards current time
		double remaining_timeout = nextDrawTime - now_s;
		if (remaining_timeout < -1*dt) {
			// NOTE: we fell too far behind, restart
			nextDrawTime = now_s;
		}
		do {
			// wait for OS input events with a positive timeout, or zero timeout if we're behind
			GLFW.glfwWaitEventsTimeout(Math.max(remaining_timeout, 0));
			remaining_timeout = nextDrawTime - (Util.getNanos() / 1e9);
		} while (remaining_timeout > 0);
		lastDrawTime = nextDrawTime;
	}
}