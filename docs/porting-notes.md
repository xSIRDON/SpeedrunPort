# porting notes

stuff to remember for the next version bump

26.x is unobfuscated so it's all mojang names. `./gradlew genSources -Pminecraft_version=... -Pfabric_version=...`
then the sources jar is in `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/<version>/`

## F3

`DebugScreenOverlay.extractRenderState` runs every enabled `DebugScreenEntry` into left/right lists
and draws them with `extractLines`, then the charts and the pie.

- DebugScreenOverlayMixin wraps `extractLines` and swaps in LeftColumn/RightColumn, wraps the
  fps/tps chart calls to draw ClassicGraphs, and skips vanilla entries while F3 is open (other mods'
  entries still run and go at the bottom of the right column)
- `Collection.isEmpty` ordinal 0 = the "no entries enabled" check, forced false while F3 is open
- ProfilerPieChartMixin cancels at HEAD and draws ClassicPieChart
- KeyboardHandlerMixin: F3+F / F3+Q at TAIL of `handleDebugKeys`, shift/alt right after
  `toggleDebugOverlay()`, F12 at HEAD of `keyPress`
- with F3 closed nothing is touched so "always on" entries still work

sodium 0.9 also hooks extractLines (fps percentiles, lost since we replace the left column) and adds
its own entry (ends up in the right column)

- tick graph uses `TpsDebugDimensions.TICK_SERVER_METHOD`, the full sample includes idle so it's always ~50ms
- `countParticles()` is a breakdown string, total is the last number
- the GUI only does quads, ColoredMesh sends triangles as quads with the last corner repeated
- text always draws over shapes in the same stratum, `nextStratum()` for a new layer
- pie is in window pixels like 1.16 (pose identity, scale 1/guiScale)

## borderless

vanilla 26.3 borderless (F11 + exclusive fullscreen off) is 1px wider than the monitor because an
exact fit gets taken over by the nvidia gl driver and always-on-top windows (ninjabrain bot) vanish.
but it always goes right, so it shows on a second monitor. ours picks an edge with nothing next to
it (BorderlessBounds). has to be re-applied after leaving F11 since vanilla puts the border back.

## standardsettings

hooked at `Minecraft.doWorldLoad` HEAD when newWorld is true, before generation. applies by
save -> overlay the lines onto options.txt -> load -> save, so vanilla parses everything. on-join
values from `ClientPlayConnectionEvents.JOIN`, only if the joined world is the one just created.

`waitForChunksRender()` in game tests never finishes after the render distance changes mid-join,
wait for one chunk past the old distance instead

## versions

`versions/26.1` (26.1 - 26.1.2), `versions/26.2`, `versions/26.3`, each just gradle.properties +
Compat (+ TestCompat). window code in `platform/glfw` (26.1, 26.2) and `platform/sdl` (26.3+)

what changed between them:

- window: glfw until 26.3, sdl3 in 26.3. key codes too (F = 70 / Q = 81 / F12 = 301 vs SDL 9 / 20 / 69)
- 26.1: `mc.screen`, `gui.getChat()`, `options.hideGui`, `levelRenderer.getSectionStatistics()`,
  `GpuDevice.getVendor()`, no `handleGlobalKeyPress` (F11 is inline in keyPress)
- before 26.3: `currentPostEffect()` instead of `getAppliedPostEffects()`, `RenderPipeline` in
  `com.mojang.blaze3d.pipeline`, `extractLines` / charts / pie don't take the screen size
  (handled with one handler per signature, require = 0), no debug gui scale
- 26.1's fabric api has the older client gametest api (`getClientLevel()`, no `lookAt`)

new version: genSources, compile against it, copy the closest `versions/` folder and fix what breaks
