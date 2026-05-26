package patrolin.stablefps.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import patrolin.stablefps.StableFPS;

@Mixin(InputConstants.class)
public class AsyncMouseInputMixin {
	/**
	 * @author Patrolin
	 * @reason Must run our async thread
	 */
	@Overwrite
	public static void grabOrReleaseMouse(long l, int i, double d, double e) {
		StableFPS.grabOrReleaseMouse(l, i, d, e);
	}
}