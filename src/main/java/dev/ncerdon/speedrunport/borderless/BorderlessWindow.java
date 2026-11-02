package dev.ncerdon.speedrunport.borderless;

/** Added to 26.3's {@code Window} by {@code WindowMixin}. */
public interface BorderlessWindow {
    boolean speedrunport$isBorderless();

    /**
     * Turns the borderless window on or off. While the game is in F11 fullscreen this only records
     * the choice; it takes effect when the window comes back out of fullscreen.
     */
    void speedrunport$setBorderless(boolean borderless);
}
