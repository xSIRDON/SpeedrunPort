package dev.ncerdon.speedrunport.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Test harness calls that changed between Fabric API versions; 26.1's harness is the older one. */
final class TestCompat {
    private TestCompat() {}

    static void waitForChunksRender(TestSingleplayerContext singleplayer) {
        singleplayer.getClientLevel().waitForChunksRender();
    }

    /** No TestInput.lookAt yet, so turn the player directly. */
    static void lookAt(ClientGameTestContext context, float yaw, float pitch) {
        context.runOnClient(mc -> {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        });
    }
}
