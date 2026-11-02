# SpeedrunPort

Fabric mod for Minecraft 26.3 that brings back the old 1.16.1 F3 screen and adds a borderless
window toggle that plays nice with overlays like Ninjabrain Bot.

## Features

**1.16.1 debug screen**

- F3 shows the old two-column layout with the same lines and wording as 1.16.1 (XYZ, Block,
  Chunk, Facing, light, heightmaps, biome, local difficulty, mob caps, sounds, targeted
  block/fluid/entity...), filled in with real 26.3 data
- Shift + F3 opens it with the profiler pie chart, Alt + F3 with the FPS / TPS graphs, drawn the
  way 1.16.1 drew them. Number keys still navigate the pie.
- F3 + F cycles render distance again (Shift + F3 + F to go down)
- F3 + Q lists all the debug keys in chat
- F3 + F and F3 + Q can be rebound in Controls > Debug like the vanilla debug keys
- lines added by other mods (e.g. Sodium) show up at the bottom of the right column
- block tags are hidden unless you turn them on in F3 + F6

**Borderless window**

- F12 toggles it (Controls > Miscellaneous > Toggle Borderless Window)
- it's a normal window without a border that covers the monitor it's on, so alt-tab doesn't
  minimize it and resize macros still work
- the window sticks out 1px past an edge with no other monitor, so the driver doesn't treat it as
  real fullscreen and always-on-top windows stay on top. Nothing spills onto a second monitor.
- turning it off puts the window back where it was, and it's remembered between launches
- F11 is still vanilla fullscreen

## Install

Needs Fabric Loader 0.19.3+ and Fabric API for 26.3. Drop the jar in your `mods` folder.

Works alongside Sodium, Lithium and FerriteCore.

## Building

JDK 25.

```
./gradlew build
```

The jar ends up in `build/libs`. `./gradlew runClientGameTest` runs the in-game tests (it opens
the game for about a minute). See `docs/porting-notes.md` for how it hooks into 26.3.

## License

MIT
