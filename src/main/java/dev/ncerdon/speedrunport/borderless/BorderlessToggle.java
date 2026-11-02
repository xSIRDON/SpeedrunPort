package dev.ncerdon.speedrunport.borderless;

import dev.ncerdon.speedrunport.SpeedrunPortConfig;
import dev.ncerdon.speedrunport.compat.Compat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The "Toggle Borderless Window" key (Controls > Miscellaneous, F12 by default). */
public final class BorderlessToggle {
    public static final KeyMapping KEY = new KeyMapping(
            "key.speedrunport.borderlessWindow", Compat.KEYBOARD, Compat.KEY_F12, KeyMapping.Category.MISC);

    private BorderlessToggle() {}

    public static void toggle(Minecraft mc) {
        BorderlessWindow window = (BorderlessWindow) (Object) mc.getWindow();
        boolean enable = !window.speedrunport$isBorderless();
        if (enable && mc.options.fullscreen().get()) {
            // Asking for borderless means leaving F11 fullscreen; the window goes borderless on the way out.
            mc.options.fullscreen().set(false);
            mc.options.save();
        }
        window.speedrunport$setBorderless(enable);
        SpeedrunPortConfig.setBorderlessWindow(enable);
    }
}
