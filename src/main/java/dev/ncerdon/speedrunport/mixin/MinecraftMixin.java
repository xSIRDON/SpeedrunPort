package dev.ncerdon.speedrunport.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ncerdon.speedrunport.borderless.BorderlessToggle;
import dev.ncerdon.speedrunport.standardsettings.StandardSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * The borderless key is a global key like F11: it works in menus too, but not while F3 is held.
 * Also where StandardSettings hooks new worlds.
 */
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

    /** Every way of making a new singleplayer world ends up here with {@code newWorld} set. */
    @Inject(method = "doWorldLoad", at = @At("HEAD"))
    private void speedrunport$standardSettings(LevelStorageSource.LevelStorageAccess levelAccess, PackRepository packs,
                                               WorldStem worldStem, Optional<GameRules> gameRules, boolean newWorld,
                                               CallbackInfo ci) {
        if (newWorld) {
            StandardSettings.onNewWorld((Minecraft) (Object) this, levelAccess.getLevelPath(LevelResource.ROOT));
        }
    }
}
