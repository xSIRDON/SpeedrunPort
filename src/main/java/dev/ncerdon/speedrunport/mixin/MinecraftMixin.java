package dev.ncerdon.speedrunport.mixin;

import dev.ncerdon.speedrunport.standardsettings.StandardSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "doWorldLoad", at = @At("HEAD"))
    private void speedrunport$standardSettings(LevelStorageSource.LevelStorageAccess levelAccess, PackRepository packs,
                                               WorldStem worldStem, Optional<GameRules> gameRules, boolean newWorld,
                                               CallbackInfo ci) {
        StandardSettings.onWorldLoad((Minecraft) (Object) this, levelAccess.getLevelPath(LevelResource.ROOT), newWorld);
    }

    @Inject(method = "resizeGui", at = @At("TAIL"))
    private void speedrunport$changeOnResize(CallbackInfo ci) {
        StandardSettings.onResize((Minecraft) (Object) this);
    }
}
