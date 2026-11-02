package dev.ncerdon.oldf3;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OldF3Client implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("old-f3");

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OldF3Keys.CYCLE_RENDER_DISTANCE);
        KeyMappingHelper.registerKeyMapping(OldF3Keys.HELP);
        LOGGER.info("Old F3 initialized: F3 shows the classic 1.16.1 debug screen.");
    }
}
