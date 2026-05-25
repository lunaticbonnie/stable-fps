
Minecraft mod to fix long-standing FPS stability issues.
1) Implement proper frame pacing when FPS target is limited (due to user config or being inside a menu):
    - Minecraft 1.21.9 and below: Allow FPS to actually reach the target instead of randomly being ~10 FPS lower.
    - Minecraft 26.1 and above: Fix dropping 1 FPS due to using out-of-date information.
2) TODO: fix massive lag spikes when moving the mouse