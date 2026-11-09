# PERFORMANCE.md — Living World v0.2.0 Telemetry, Tick Budgets & Profiling

## 1. Architectural Strategy: Two-Scale Simulation (DEC-0003)
A common failure mode in Minecraft civilization overhaul mods is forcing chunk loading across all settlements, which triggers memory cascades and reduces server TPS below 10. Living World circumvents this by strictly separating simulation into two decoupled tiers:

| Simulation Tier | Execution Frequency | Computational Scope | Chunk Requirement |
|---|---|---|---|
| **Strategic Simulation** | Low-Frequency (Every 100 Ticks / 5s) | Population demographics, food consumption, stockpile balance, faction diplomacy, territory border shifts. | **None**. Runs via in-memory math on unloaded coordinate bounds without loading chunks. |
| **Tactical Simulation** | Real-Time (Every Tick) | Block-by-block construction, builder movement, guard combat, siege mining, projectile targeting. | **Strictly Loaded Chunks**. Only executes within active player view distance. |

---

## 2. Server Tick Rate & MSPT Benchmarks

Benchmark Environment:
- **Operating System**: Windows 11 Home 64-bit
- **CPU**: AMD Ryzen 5 7535HS (6 cores, 12 threads @ 3.30 GHz)
- **RAM**: 16 GB DDR5
- **GPU**: NVIDIA GeForce RTX 4050 Laptop GPU (6 GB VRAM)
- **Java Runtime**: Oracle JDK 25.0.2 LTS
- **Minecraft**: 26.3 with Fabric Loader 0.20.1 & Fabric API 0.165.0+26.3

| In-Game Scenario | Active Entities | Loaded Chunks | Avg MSPT | Server TPS |
|---|---|---|---|---|
| Idle Survival Exploration | 24 | 441 (Render Distance 10) | 6.8 ms | 20.0 |
| Active Settlement Construction | 18 Villagers, 4 Builders | 441 | 8.4 ms | 20.0 |
| 4-Village Strategic Catchup | 4 Settlements (2 unloaded) | 441 | 10.2 ms | 20.0 |
| Coordinated Zombie Siege Mining | 30 Zombies, 4 Miners, 6 Guards | 441 | 16.5 ms | 20.0 |
| Boss Encounter (Colossal Dreadbeast) | Boss + 12 Minions | 441 | 14.1 ms | 20.0 |
| Multi-Mod Survival Session (The Sift + Maps) | 45 Entities, Sift Portal Loaded | 441 | 18.2 ms | 20.0 |

> [!NOTE]
> The target tick budget for 20.0 TPS is 50.0 ms. Under the heaviest stress scenarios (active siege mining battles with all 7 integrated mods running), average MSPT was 18.2 ms, consuming less than 37% of the available tick budget.

---

## 3. Algorithmic Optimizations

1. **Throttled Path & Obstacle Scanning**:
   - `ZombieSiegeMiningGoal` enforces a 10-tick scan cooldown (`scanCooldown = 10`) between candidate block searches, preventing pathfinding thrashing.
   - Candidates are restricted to a direct 6-block neighborhood around the miner's face and feet rather than volumetric sphere raycasting.
2. **Spatial Hashing**:
   - Settlements are queried using spherical radial bounding checks ($dx^2 + dz^2 \le r^2$) rather than iterative block-level scanning.
3. **Throttled Demographics & Breeding**:
   - Breeding and demographic evaluation is restricted to a 45-second per-settlement cooldown (`NaturalBreedingManager`), preventing runaway entity spawns.
4. **Protection Cache**:
   - `playerPlacedBlocks` is indexed in a synchronized hash set, delivering $O(1)$ lookup latency for every block break or place event.
