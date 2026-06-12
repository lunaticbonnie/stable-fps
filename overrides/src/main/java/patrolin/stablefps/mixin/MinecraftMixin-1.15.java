package patrolin.stablefps.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import patrolin.stablefps.StableFPS;

@Mixin(Minecraft.class)
public class MinecraftMixin {
  /* TODO: in what mc version does this become public? */
  @Inject(method="close", at=@At("HEAD"))
  private void onClose(CallbackInfo ci) {
    StableFPS.shouldClose();
  }
}
