package patrolin.stablefps.mixin;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import patrolin.stablefps.StableFPS;

@Mixin(LoadingOverlay.class)
public class ForgeWindowMixin {
  /*@Inject(method="render", at=@At(value = "INVOKE", target="Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
  private void onLoad(CallbackInfo ci) {
    StableFPS.shouldCloseOldWindow();
  }*/
}
