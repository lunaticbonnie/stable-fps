Minecraft mod to fix long-standing FPS stability issues.
1) Implement proper frame pacing when FPS target is limited (due to user setting or being inside a menu):
    - Minecraft 1.21.11 and below: Allow FPS to actually reach the target instead of randomly being 5-20 FPS lower.
    - Minecraft 26.1 and above: Fix dropping by 1 FPS due to using out-of-date information.
2) Get inputs on a separate thread to fix FPS dropping by 2-3x when moving the mouse (especially on high polling rate mice on Windows) due to `glfwPollInputs()`.

NOTE: You may experience graphical glitches when starting the game or resizing the window - this is intentional, as it would require a ton of work to fix and I want to keep the changes to a minimum to be compatible with other mods.

Compatible with Sodium.

NOTE: `fabric-api` is broken for versions `1.16`, `1.16.1`, `1.17`, so we can't support those...
NOTE: The forge-based version works by opening a second window, and closing the original. This has the side effect of the window not getting focus when starting the game. (In theory you should just be able to piggyback off of the existing window, but in practice Java doesn't actually allow you to schedule work to run on the same thread as the current thread (which is where Windows requires you to get inputs)...)

Available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/stable-fps).

## dev
Download https://github.com/Patrolin/justice
Download Python 3
Download mod templates from https://fabricmc.net/develop/template/ into `templates/*` for all desired Minecraft versions
    Mod Name="ExampleMod"
    Package Name="com.examplemod"
    Minecraft Version=...
    Split client and common sources=false
`ice change-version <version>`
`ice run-client` or Open `./current` in IntelliJ IDEA

### Run with tracy profiler
Download some verson of tracy
Add `--tracy` to application args
Run tracy
- Check if protocol version matches, else download different version of tracy...
- Connect

### Java versions
Java 25 for Minecraft 26.1 and later
Java 21 for Minecraft 1.20.5 to 1.21.11
Java 17 for Minecraft 1.17 to 1.20.4
Java 8 for Minecraft 1.16 and earlier

### Minecraft versions
Minecraft 1.21.9 and above has F3 menu on the main menu

### Todo list
- neoforge versions?
- more forge versions when fg7 (forge-gradle) is updated to support mixins in every minecraft version