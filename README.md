# SpeedrunPort

Fabric mod for Minecraft 26.x (26.1 to 26.3) that brings back the old 1.16.1 F3 screen, adds a borderless
window toggle that plays nice with overlays like Ninjabrain Bot, and has StandardSettings built in.

## Features

**1.16.1 debug screen**

- F3 shows the old two-column layout with the same lines and wording as 1.16.1 (XYZ, Block,
  Chunk, Facing, light, heightmaps, biome, local difficulty, mob caps, sounds, targeted
  block/fluid/entity...), filled in with real data from the game you are running
- Shift + F3 opens it with the profiler pie chart, Alt + F3 with the FPS / TPS graphs, drawn the
  way 1.16.1 drew them. Number keys still navigate the pie.
- F3 + F cycles render distance again (Shift + F3 + F to go down)
- F3 + Q lists all the debug keys in chat
- F3 + F and F3 + Q can be rebound in Controls > Debug like the vanilla debug keys
- F3 and the graphs follow your GUI scale like they did in 1.16.1 (26.3's separate "Debug GUI Scale"
  setting is ignored). The pie chart keeps its 1.16.1 size
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

**StandardSettings**

Resets your options every time you make a new world, so every run starts the same. Uses the
`standardoptions.txt` format from the StandardSettings mod before 2.0 (2.0 moved to a json file and
an in-game screen).

- set your options how you want them, then run `/standardsettings save` in any world. That writes
  `config/standardoptions.txt` from your current settings (it does nothing until that file exists)
- the file is just options.txt lines, so you can also edit it by hand. Delete the lines you don't
  want forced. If the file's only line is a path to another file, that file is used instead, so
  several instances can share one
- every new world gets a copy in its folder (`saves/<world>/standardoptions.txt`) for verification
- reopening the world you were in before the reset brings back the settings you had there
- `/standardsettings` shows the status, `/standardsettings off` / `on` turns it off and on,
  `/standardsettings apply` applies it right now

Extra lines it understands:

| line | does |
|---|---|
| `chunkborders:true` | chunk borders on (always, not just with F3 open) |
| `hitboxes:true` | hitboxes on |
| `perspective:0` | camera: 0 first person, 1 back, 2 front |
| `f1:false` | hide the HUD or not |
| `sneaking:false` | sneak state when toggle sneak is on |
| `sprinting:false` | sprint state when toggle sprint is on |
| `pieDirectory:root.render.level` | where the F3 pie chart starts. `/standardsettings save` stores wherever you navigated the pie to |
| `fovOnWorldJoin:90` | FOV once you're in the world and the game window is focused. 30 to 110, or -1.0 to 1.0 like options.txt |
| `renderDistanceOnWorldJoin:12` | same for render distance (keep the normal one low for a fast world load and background instances) |
| `simulationDistanceOnWorldJoin:8` | same for simulation distance |
| `entityDistanceScalingOnWorldJoin:1.0` | same for entity distance |
| `guiScaleOnWorldJoin:3` | same for GUI scale |
| `changeOnResize:true` | resizing the window also applies the OnWorldJoin values, for macros |
| `autoF3Esc:true` | pause without the menu when a world finishes loading in an unfocused window |
| `firstAutoF3EscDelay:22` | ticks to wait before that pause, only on the first world after launch |
| `toggleStandardSettings:false` | turn it all off |

## Install

Grab the jar for your Minecraft version and drop it in your `mods` folder:

| Minecraft | jar |
|---|---|
| 26.1, 26.1.1, 26.1.2 | `speedrunport-<version>+26.1.jar` |
| 26.2 | `speedrunport-<version>+26.2.jar` |
| 26.3 | `speedrunport-<version>+26.3.jar` |

Needs Fabric Loader 0.19.3+ and Fabric API. Works alongside Sodium, Lithium and FerriteCore.

## Building

JDK 25.

```
./gradlew build collectJars
```

That builds every version and copies the jars to `build/libs`. The shared code is in `src/`, and
each `versions/<mc>` folder only has what's different for that version (its Minecraft / Fabric API
versions and a small `Compat` class). `platform/glfw` and `platform/sdl` hold the borderless window
code, since 26.3 moved the game window from GLFW to SDL.

`./gradlew :26.3:runClientGameTest` (or `:26.1:` / `:26.2:`) runs the in-game tests for one version
(it opens the game for about a minute). See `docs/porting-notes.md` for how it hooks into the game.

## License

MIT
