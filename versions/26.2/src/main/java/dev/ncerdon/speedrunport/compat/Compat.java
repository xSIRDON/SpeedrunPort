package dev.ncerdon.speedrunport.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

// Everything that differs between 26.x versions goes through here. This is the 26.2 one.
public final class Compat {
    // GLFW key codes (the window is GLFW before 26.3)
    public static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYSYM;
    public static final int KEY_F = InputConstants.KEY_F;
    public static final int KEY_Q = InputConstants.KEY_Q;
    public static final int KEY_F12 = InputConstants.KEY_F12;

    private Compat() {}

    public static @Nullable Screen screen(Minecraft mc) {
        return mc.gui.screen();
    }

    public static ChatComponent chat(Minecraft mc) {
        return mc.gui.hud.getChat();
    }

    public static boolean isHudHidden(Minecraft mc) {
        return mc.gui.hud.isHidden();
    }

    public static void setHudHidden(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) {
            mc.gui.hud.toggle();
        }
    }

    public static @Nullable String sectionStatistics(Minecraft mc) {
        return mc.levelExtractor.sectionStatistics();
    }

    public static @Nullable String entityStatistics(Minecraft mc) {
        return mc.levelExtractor.entityStatistics();
    }

    public static String particleCount(Minecraft mc) {
        return mc.particleEngine.countParticles();
    }

    // only one post effect at a time before 26.3
    public static List<String> postEffects(Minecraft mc) {
        Identifier effect = mc.gameRenderer.currentPostEffect();
        return effect == null ? List.of() : List.of(effect.toString());
    }

    public static String mobCategoryAbbreviation(MobCategory category) {
        return category.getDebugAbbreviation();
    }

    public static String cpuInfo() {
        return GLX._getCpuInfo();
    }

    public static List<String> gpuLines(Window window) {
        var device = RenderSystem.getDevice().getDeviceInfo();
        return List.of(
                String.format(Locale.ROOT, "Display: %dx%d (%s)", window.getWidth(), window.getHeight(), device.vendorName()),
                device.name(),
                device.backendName() + " " + device.driverInfo().lines().findFirst().orElse(""));
    }

    public static int @Nullable [] intRange(OptionInstance<Integer> option) {
        return option.values() instanceof OptionInstance.IntRangeBase range
                ? new int[] {range.minInclusive(), range.maxInclusive()}
                : null;
    }

    // no improved transparency key before 26.3
    public static @Nullable KeyMapping translucencyKey(Options options) {
        return null;
    }

    public static long windowHandle(Window window) {
        return window.handle();
    }

    public static GuiElementRenderState coloredQuads(Matrix3x2fc pose, float[] positions, int[] colors,
                                                     @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) {
        return new ColoredQuads(pose, positions, colors, scissorArea, bounds);
    }

    private record ColoredQuads(Matrix3x2fc pose, float[] positions, int[] colors,
                                @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (int i = 0; i < colors.length; i++) {
                consumer.addVertexWith2DPose(pose, positions[i * 2], positions[i * 2 + 1]).setColor(colors[i]);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }

    // no "Debug GUI Scale" before 26.3
    public static void setDebugGuiScaleAuto(Minecraft mc) {}

    public static void clearToasts(Minecraft mc) {
        mc.gui.toastManager().clear();
    }
}
