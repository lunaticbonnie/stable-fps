Minecraft mod to fix long-standing FPS stability issues.
1) Implement proper frame pacing when FPS target is limited (due to user config or being inside a menu):
    - Minecraft 1.21.9 and below: Allow FPS to actually reach the target instead of randomly being 5-20 FPS lower.
    - Minecraft 26.1 and above: Fix dropping by 1 FPS due to using out-of-date information.
2) Get inputs on a separate thread to fix FPS dropping by 2-3x when moving the mouse (especially on high polling rate mice) due to `glfwPollInputs()`.

## dev
`ice change-version <version>`
`ice run-client` or Open `./current` in IntelliJ IDEA
Add `--tracy` to application args

TODO: give the window focus
TODO: replace GLFW.glfwWindowShouldClose() with thread-safe boolean