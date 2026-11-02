package dev.ncerdon.speedrunport.borderless;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ncerdon.speedrunport.SpeedrunPortConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The "Toggle Borderless Window" key (Controls > Miscellaneous, F12 by default). */
public final class BorderlessToggle {
    /** SDL scancode for F12; 26.3 key mappings store SDL scancodes. */
    private static final int KEY_F12 = 69;

    public static final KeyMapping KEY = new KeyMapping(
            "key.speedrunport.borderlessWindow", InputConstants.Type.KEYBOARD, KEY_F12, KeyMapping.Category.MISC);

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
