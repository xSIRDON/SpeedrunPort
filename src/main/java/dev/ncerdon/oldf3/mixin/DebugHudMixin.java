package dev.ncerdon.oldf3.mixin;

import dev.ncerdon.oldf3.render.LeftColumn;
import dev.ncerdon.oldf3.render.RightColumn;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * Replaces the modern per-entry debug text with the classic 1.16.1-style
 * columns. {@code DebugHud.render} builds the left/right line lists and draws
 * each via {@code drawText(context, lines, left)}; we swap the {@code lines}
 * argument at HEAD based on the {@code left} flag. The pie chart, charts, and
 * chunk-border rendering happen elsewhere in {@code render} and are untouched.
 */
@Mixin(DebugHud.class)
public abstract class DebugHudMixin {

    @ModifyVariable(
            method = "drawText(Lnet/minecraft/client/gui/DrawContext;Ljava/util/List;Z)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2)
    private List<String> oldf3$replaceColumn(List<String> original, DrawContext context, List<String> lines, boolean left) {
        MinecraftClient client = MinecraftClient.getInstance();
        return left ? LeftColumn.build(client) : RightColumn.build(client);
    }
}
