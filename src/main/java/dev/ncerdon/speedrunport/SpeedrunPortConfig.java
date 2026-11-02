package dev.ncerdon.speedrunport;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** {@code config/speedrunport.properties}: settings the mod remembers between launches. */
public final class SpeedrunPortConfig {
    private static final String BORDERLESS_WINDOW = "borderlessWindow";

    private static Properties properties;

    private SpeedrunPortConfig() {}

    public static synchronized boolean borderlessWindow() {
        return Boolean.parseBoolean(properties().getProperty(BORDERLESS_WINDOW, "false"));
    }

    public static synchronized void setBorderlessWindow(boolean enabled) {
        properties().setProperty(BORDERLESS_WINDOW, Boolean.toString(enabled));
        save();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("speedrunport.properties");
    }

    // Loaded on first use: the window is created before mod initializers are guaranteed to have run.
    private static Properties properties() {
        if (properties == null) {
            properties = new Properties();
            Path file = file();
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    properties.load(reader);
                } catch (IOException e) {
                    SpeedrunPortClient.LOGGER.warn("Could not read {}, using defaults", file, e);
                }
            }
        }
        return properties;
    }

    private static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "SpeedrunPort");
            }
        } catch (IOException e) {
            SpeedrunPortClient.LOGGER.warn("Could not save {}", file, e);
        }
    }
}
