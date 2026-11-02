package dev.ncerdon.speedrunport.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ncerdon.speedrunport.borderless.BorderlessToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The borderless key is a global key like F11: it works in menus too, but not while F3 is held. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final public Options options;

    @Inject(method = "handleGlobalKeyPress", at = @At("HEAD"), cancellable = true)
    private void speedrunport$borderlessKey(InputConstants.Key key, boolean controlDown, CallbackInfoReturnable<Boolean> cir) {
        if (!options.keyDebugModifier.isDown() && BorderlessToggle.KEY.matches(key)) {
            BorderlessToggle.toggle((Minecraft) (Object) this);
            cir.setReturnValue(true);
        }
    }
}
