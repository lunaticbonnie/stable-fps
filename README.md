Minecraft mod to fix long-standing FPS stability issues.
1) Implement proper frame pacing when FPS target is limited (due to user setting or being inside a menu):
    - Minecraft 1.21.11 and below: Allow FPS to actually reach the target instead of randomly being 5-20 FPS lower.
    - Minecraft 26.1 and above: Fix dropping by 1 FPS due to using out-of-date information.
2) Get inputs on a separate thread to fix FPS dropping by 2-3x when moving the mouse (especially on high polling rate mice on Windows) due to `glfwPollInputs()`.

NOTE: You may experience graphical glitches when starting the game or resizing the window - this is intentional, as it would require a ton of work to fix and I want to keep the changes to a minimum to be compatible with other mods.

Compatible with Sodium.

Recommended settings:
- `Fullscreen: ON` (due to Minecraft using `Hardware: Legacy Flip` on Windows)
- `VSync: ON` (to not waste time rendering frames that won't be shown)
- In Vanilla: `Max Framerate: Unlimited` (due to Vanilla fps limiter being broken)
- On Fabric: `Max Framerate: any`
- On Forge/NeoForge: `Max Framerate: lower?` (due to Forge renderer being slow)

Available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/stable-fps) and [Modrinth](https://modrinth.com/mod/stablefps-xyz).

NOTE: `fabric-api` is broken for versions `1.16`, `1.16.1` and `1.17`, so we can't support those... \
NOTE: `neoforged` is broken for version `1.20.5`, so we can't support that...

## Todo list
- more forge versions when fg7 (forge-gradle) is updated to support mixins in every minecraft version

## dev
```
Download https://github.com/Patrolin/justice
Download Python 3
Download mod templates for all desired Minecraft versions into `templates/*` from:
  a) https://fabricmc.net/develop/template/
    Mod Name="ExampleMod"
    Package Name="com.examplemod"
    Minecraft Version=...
    Split client and common sources=false
  b) https://files.minecraftforge.net/net/minecraftforge/forge/
  c) https://neoforged.net/mod-generator/
    Mod Name="ExampleMod"
    Package Name="com.examplemod"
    Minecraft Version=...
    Mod Authors="Me!"
    Mod Description="Description"
    Advanced Options.Add mixin configuration=true
```
`ice list` to list versions \
`ice <fabric|forge|neoforge> <mc_version>` to change to the selected version \
`ice run` or Open `./current` in IntelliJ IDEA and run `runClient` gradle task \
`ice build-version <version>` to run and build the selected version

### Run with tracy profiler
```
Download some version of tracy profiler
Add `--tracy` to application args
Run tracy
  Check if protocol version matches, else download different version of tracy...
  Connect
```

### Java versions
```
Java 25 for Minecraft 26.1 and later
Java 21 for Minecraft 1.20.5 to 1.21.11
Java 17 for Minecraft 1.17 to 1.20.4
Java 8 for Minecraft 1.16 and earlier
```

### Minecraft versions
```
Minecraft 1.21.9 and above has F3 menu on the main menu
```