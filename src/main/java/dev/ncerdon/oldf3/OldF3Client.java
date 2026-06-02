package dev.ncerdon.oldf3;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OldF3Client implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("old-f3");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Old F3 initialized — F3 will show the classic-style debug screen.");
    }
}
