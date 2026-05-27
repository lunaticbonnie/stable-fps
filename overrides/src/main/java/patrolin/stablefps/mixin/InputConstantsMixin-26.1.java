package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import patrolin.stablefps.StableFPS;

@Mixin(InputConstants.class)
public class InputConstantsMixin {
	/**
	 * @author Patrolin
	 * @reason Must run on our inputThread
	 */
	@Overwrite
	public static void grabOrReleaseMouse(final Window window, final int cursorMode, final double x, final double y) {
		StableFPS.grabOrReleaseMouse(window.handle(), cursorMode, x, y);
	}
}