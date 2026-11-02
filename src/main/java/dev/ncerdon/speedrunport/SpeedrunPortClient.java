package dev.ncerdon.speedrunport;

import dev.ncerdon.speedrunport.borderless.BorderlessToggle;
import dev.ncerdon.speedrunport.f3.ClassicDebugKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedrunPortClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("speedrunport");

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(ClassicDebugKeys.CYCLE_RENDER_DISTANCE);
        KeyMappingHelper.registerKeyMapping(ClassicDebugKeys.HELP);
        KeyMappingHelper.registerKeyMapping(BorderlessToggle.KEY);
        LOGGER.info("SpeedrunPort initialized.");
    }
}
