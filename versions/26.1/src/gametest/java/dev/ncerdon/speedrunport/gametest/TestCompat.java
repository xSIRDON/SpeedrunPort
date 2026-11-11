package dev.ncerdon.speedrunport.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

final class TestCompat {
    private TestCompat() {}

    static void waitForChunksRender(TestSingleplayerContext singleplayer) {
        singleplayer.getClientLevel().waitForChunksRender();
    }

    static void lookAt(ClientGameTestContext context, float yaw, float pitch) {
        context.runOnClient(mc -> {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        });
    }

    static void setDebugGuiScaleAuto(Minecraft mc) {}

    static void clearToasts(Minecraft mc) {
        mc.getToastManager().clear();
    }
}
