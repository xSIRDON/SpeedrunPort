# Porting notes (26.3)

Notes to self for the next version bump.

26.x ships unobfuscated, so there are no Yarn mappings anymore, everything is Mojang names.
`./gradlew genSources` and then read the sources jar in
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/<version>/`.

## How 26.3 draws F3

`DebugScreenOverlay.extractRenderState`:

- gets the enabled entries from `minecraft.debugEntries` (in-overlay vs always-on is set in F3 + F6)
- runs every `DebugScreenEntry.display(...)` into left/right/group lists
- draws each column with the private `extractLines(graphics, lines, alignLeft, screenWidth)`
- then the charts: `FpsDebugChart`, `TpsDebugChart` (F3 + 2), bandwidth/ping (F3 + 3),
  lightmap (F3 + 4) and `ProfilerPieChart` (F3 + 1)

Debug keys are `KeyMapping`s now (`options.keyDebugReloadChunk` etc), handled in
`KeyboardHandler.handleDebugKeys`. Key codes are SDL scancodes: A = 4, F = 9, Q = 20, F3 = 60,
F12 = 69. 26.3 removed F3 + F and F3 + Q.

## Mixins

- `DebugScreenOverlayMixin`
  - `Collection.isEmpty` (ordinal 0) in `extractRenderState`: keep drawing while F3 is open even
    if every entry is turned off
  - wraps `DebugScreenEntry.display`: skip vanilla entries while F3 is open, other mods' entries
    go into a `LineCollector` (bottom of the right column)
  - wraps `extractLines`: swap in `LeftColumn` / `RightColumn`
  - wraps `FpsDebugChart` / `TpsDebugChart.extractRenderState`: draw `ClassicGraphs` instead
- `ProfilerPieChartMixin`: cancel at HEAD and draw `ClassicPieChart`. Profiling and 0-9
  navigation are still vanilla.
- `KeyboardHandlerMixin`: F3 + F / F3 + Q at the TAIL of `handleDebugKeys`, and Shift/Alt
  handling right after `toggleDebugOverlay()` in `keyPress`
- `WindowMixin` + `MinecraftMixin`: borderless window, see below

With F3 closed nothing is touched, so "always on" entries still work. Block/fluid tags only show
when they're turned on in F3 + F6 (26.3 blocks have dozens of tags).

Sodium 0.9 also hooks the overlay. It inserts FPS percentiles right before the left
`extractLines` (lost, since the left column gets replaced) and adds its own entry, which ends up
at the bottom of the right column.

## Data sources

- tick graph: `tickTimeLogger` dimension `TpsDebugDimensions.TICK_SERVER_METHOD`. The full sample
  is the whole gap between ticks (idle included), so it's always ~50ms.
- frame graph: `frameTimeLogger`, one sample per frame
- particle count: `countParticles()` is a per-type breakdown now, the total is the last number
- Shift/Alt: check `minecraft.hasShiftDown()` too, not just the event's modifier flags

## Drawing

The GUI only takes quads. `ColoredMesh` sends triangles as quads with the last corner repeated,
same winding as vanilla's filled rects, via `graphics.guiRenderState.addGuiElement(...)` with
`RenderPipelines.GUI`. Text always draws over shapes in the same stratum; `nextStratum()` starts a
new layer on top.

The pie is drawn in window pixels (pose reset to identity, scaled by 1 / gui scale) like 1.16.1.

## Borderless window

26.3's own borderless (F11 with exclusive fullscreen off) is a window 1px wider than the monitor
(`BORDERLESS_FULLSCREEN_PADDING`) and the game treats it as fullscreen. The 1px exists because a
window covering the monitor exactly gets taken over by the NVIDIA OpenGL driver, and then
always-on-top windows (Ninjabrain Bot) don't show over it. But vanilla always pads to the right,
so with a second monitor there you get a 1px strip on it.

Ours stays a normal window: no border, monitor bounds plus 1px past an edge with no monitor next
to it (`BorderlessBounds.padded`), old bounds + maximized state restored on exit, saved in
`config/speedrunport.properties`, and re-applied after leaving F11 since vanilla puts the border
back.

## Building / testing

Needs JDK 25 (Gradle 9.8.2, Loom 1.19.3).

- `./gradlew build`: jar + unit tests
- `./gradlew runClientGameTest`: opens the game, makes a world, goes through the F3 combos and the
  borderless key, saves screenshots to `build/run/clientGameTest/screenshots`
- add `"-PextraMods=path/to/sodium.jar;path/to/lithium.jar"` to load other mods alongside (not
  Fabric API, that's already on the dev classpath)
