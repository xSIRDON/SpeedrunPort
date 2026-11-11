package dev.ncerdon.speedrunport.borderless;

// implemented on Window by WindowMixin
public interface BorderlessWindow {
    boolean speedrunport$isBorderless();

    // if the game is in F11 fullscreen this just remembers it, and applies when fullscreen ends
    void speedrunport$setBorderless(boolean borderless);
}
