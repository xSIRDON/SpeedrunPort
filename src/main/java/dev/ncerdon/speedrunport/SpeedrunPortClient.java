package dev.ncerdon.speedrunport;

import dev.ncerdon.speedrunport.borderless.BorderlessToggle;
import dev.ncerdon.speedrunport.f3.ClassicDebugKeys;
import dev.ncerdon.speedrunport.standardsettings.StandardSettings;
import dev.ncerdon.speedrunport.standardsettings.StandardSettingsCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedrunPortClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("speedrunport");

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(ClassicDebugKeys.CYCLE_RENDER_DISTANCE);
        KeyMappingHelper.registerKeyMapping(ClassicDebugKeys.HELP);
        KeyMappingHelper.registerKeyMapping(BorderlessToggle.KEY);

        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> StandardSettings.onJoin(client));
        ClientTickEvents.END_CLIENT_TICK.register(StandardSettings::onTick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> StandardSettingsCommand.register(dispatcher));

        LOGGER.info("SpeedrunPort initialized.");
    }
}
