Minecraft mod to fix long-standing FPS stability issues.
1) Implement proper frame pacing when FPS target is limited (due to user setting or being inside a menu):
    - Minecraft 1.21.9 and below: Allow FPS to actually reach the target instead of randomly being 5-20 FPS lower.
    - Minecraft 26.1 and above: Fix dropping by 1 FPS due to using out-of-date information.
2) Get inputs on a separate thread to fix FPS dropping by 2-3x when moving the mouse (especially on high polling rate mice on Windows) due to `glfwPollInputs()`.

NOTE: You may experience graphical glitches when starting the game or resizing the window - this is intentional, as it would require a ton of work to fix and I want to keep the changes to a minimum to be compatible with other mods.

Compatible with Sodium.

## dev
Download https://github.com/Patrolin/justice
Download Python 3
`ice change-version <version>`
`ice run-client` or Open `./current` in IntelliJ IDEA

### Run with tracy profiler
Download some verson of tracy
Add `--tracy` to application args
Run tracy
- Check if protocol version matches, else download different version of tracy...
- Connect

### TODO
- Rewrite to Java 8, so that CurseForge launcher, etc. can launch it with a lower java version than 21