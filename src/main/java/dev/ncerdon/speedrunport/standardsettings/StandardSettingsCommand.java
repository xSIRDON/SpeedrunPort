package dev.ncerdon.speedrunport.standardsettings;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.ncerdon.speedrunport.SpeedrunPortClient;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * {@code /standardsettings}: status, {@code save} (current options become the standard),
 * {@code apply}, {@code on} and {@code off}.
 */
public final class StandardSettingsCommand {
    private StandardSettingsCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("standardsettings")
                .executes(StandardSettingsCommand::status)
                .then(literal("save").executes(StandardSettingsCommand::save))
                .then(literal("apply").executes(StandardSettingsCommand::apply))
                .then(literal("on").executes(context -> setEnabled(context, true)))
                .then(literal("off").executes(context -> setEnabled(context, false))));
    }

    private static int status(CommandContext<FabricClientCommandSource> context) {
        Map<String, String> standard = StandardSettings.read();
        if (standard == null) {
            context.getSource().sendFeedback(Component.translatable("speedrunport.standardsettings.missing"));
        } else {
            context.getSource().sendFeedback(Component.translatable(
                    StandardOptionsFile.isEnabled(standard) ? "speedrunport.standardsettings.on" : "speedrunport.standardsettings.off",
                    StandardOptionsFile.settingsOnly(standard).size()));
        }
        return 1;
    }

    private static int save(CommandContext<FabricClientCommandSource> context) {
        try {
            Map<String, String> standard = StandardSettings.saveCurrent(context.getSource().getClient());
            context.getSource().sendFeedback(Component.translatable("speedrunport.standardsettings.saved",
                    StandardOptionsFile.settingsOnly(standard).size(), StandardSettings.file().toString()));
            return 1;
        } catch (IOException e) {
            return fail(context, e);
        }
    }

    private static int apply(CommandContext<FabricClientCommandSource> context) {
        try {
            if (!StandardSettings.applyNow(context.getSource().getClient())) {
                context.getSource().sendError(Component.translatable("speedrunport.standardsettings.missing"));
                return 0;
            }
            context.getSource().sendFeedback(Component.translatable("speedrunport.standardsettings.applied"));
            return 1;
        } catch (IOException e) {
            return fail(context, e);
        }
    }

    private static int setEnabled(CommandContext<FabricClientCommandSource> context, boolean enabled) {
        if (!Files.exists(StandardSettings.file())) {
            context.getSource().sendError(Component.translatable("speedrunport.standardsettings.missing"));
            return 0;
        }
        try {
            StandardSettings.setEnabled(enabled);
            return status(context);
        } catch (IOException e) {
            return fail(context, e);
        }
    }

    private static int fail(CommandContext<FabricClientCommandSource> context, IOException e) {
        SpeedrunPortClient.LOGGER.warn("StandardSettings command failed", e);
        context.getSource().sendError(Component.translatable("speedrunport.standardsettings.error", e.getMessage()));
        return 0;
    }
}
