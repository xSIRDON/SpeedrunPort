package dev.ncerdon.speedrunport.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

final class TestCompat {
    private TestCompat() {}

    static void waitForChunksRender(TestSingleplayerContext singleplayer) {
        singleplayer.getConnection().waitForChunksRender();
    }

    static void lookAt(ClientGameTestContext context, float yaw, float pitch) {
        context.getInput().lookAt(yaw, pitch);
    }

    static void setDebugGuiScaleAuto(Minecraft mc) {}

    static void clearToasts(Minecraft mc) {
        mc.gui.toastManager().clear();
    }
}
