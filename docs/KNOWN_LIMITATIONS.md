# KNOWN_LIMITATIONS.md — Living World v0.2.0 Boundary Conditions & Known Limitations

## 1. Aquatic Predator Navigation in Shallow Canals
- **Behavior**: Great White Sharks (`SharkEntity`) require at least 3 vertical blocks of water depth to navigate at full swimming speed.
- **Limitation**: In man-made 1-block shallow canals or shallow swamp fringes, sharks may temporarily beach themselves or lose target pathing until floating into deeper open ocean or river channels.
- **Mitigation**: Shark spawning is restricted strictly to `OCEAN`, `DEEP_OCEAN`, and `WARM_OCEAN` biomes at Y < 55.

---

## 2. Third-Party Custom Mod Blocks in Siege Mining
- **Behavior**: `ZombieSiegeMiningGoal` evaluates tool-to-block compatibility using Minecraft's official vanilla block tags (`BlockTags.MINEABLE_WITH_PICKAXE`, `BlockTags.MINEABLE_WITH_AXE`, `BlockTags.MINEABLE_WITH_SHOVEL`).
- **Limitation**: If an external mod adds custom blocks without registering them to Mojang's official data tags, zombie miners will treat them as non-mineable obstacles rather than risk breaking modded tile entities improperly.

---

## 3. Physical Construction in Unloaded Chunks
- **Behavior**: By architectural design (DEC-0003), physical block placement is restricted strictly to loaded chunks within active player view distance.
- **Limitation**: If a player queues a 200-block construction project and immediately teleports 5,000 blocks away, the settlement will accumulate pending materials and population capacity mathematically, but the actual block placement will pause until a player revisits the area.
- **Rationale**: Prevents chunk generation cascades, memory leaks, and server TPS degradation.

---

## 4. Headless Prism Launcher CLI Automation
- **Behavior**: When running Prism Launcher in non-interactive background terminals or remote environments, `prismlauncher.exe` terminates after translation loading because Qt requires an interactive display server.
- **Mitigation**: Living World includes dedicated PowerShell automation runners (`scripts/test_quickplay_world.ps1` and `scripts/test_gameplay.ps1`) that boot the Fabric `KnotClient` directly using Prism Launcher's exact local natives and JVM classpath.
