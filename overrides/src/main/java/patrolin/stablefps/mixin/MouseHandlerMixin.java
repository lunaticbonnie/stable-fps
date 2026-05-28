package patrolin.stablefps.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import patrolin.stablefps.StableFPS;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
  @WrapMethod(method="onPress")
  private void onPress(long l, int i, int j, int k, Operation<Void> original) {
    StableFPS.runOnRenderThread(() -> original.call(l, i, j, k));
  }
}
