# HANDOFF.md — Living World v0.2.6 Engineering Handoff Document

**Project:** Minecraft: Living World  
**Release:** Version 0.2.6 — Civilization Intelligence, Dimensional Scars & Release Engineering  
**Target Platform:** Minecraft Java Edition 26.3 / Fabric Loader 0.20.1 / Java 25  
**Instance Path:** `instances/LivingWorld/` (Dedicated, isolated instance)  
**Latest Verified Artifact:** `instances/LivingWorld/minecraft/mods/livingworld-0.2.6.jar` (355,243 bytes, 78 compiled classes)  
**Rollback Artifact:** `backup_v0.2.5/livingworld-0.2.5.jar` (309,832 bytes)  
**Git Repository:** `https://github.com/PrathamKapoor/Minecraft-Living-World` (Branch: `main`)  
**Push Hold Status:** **ACTIVE (STRICT)** — All commits created locally. Zero pushes to remote repository until user explicitly commands `"PUSH"`.

---

## 1. Executive Summary
Living World v0.2.6 resolves all 16 defects (`BUG-01` through `BUG-16`) identified during creator playtests of v0.2.5, introduces bidirectional Nether–Overworld biome incursions (Feature F19 Dimensional Scars), and overhauls all custom spawn egg textures to authentic vanilla 12x14 egg silhouette proportions. All 78 automated acceptance tests and all 7 gameplay playtest scenarios have been executed and passed in the official Minecraft 26.3 / Java 25 runtime environment.

---

## 2. Defect Resolution Matrix

| Defect ID | Summary | Root Cause & Resolution | Verification |
|---|---|---|---|
| **BUG-01** | Sentry Horn Alarm Audio Localization | Sentries previously played global horn sound. Now localized at sentry coordinates; sentry equips goat horn in main hand for 30 ticks (1.5s) with `startUsingItem`, then restores bow. | `Test 64` (Passed) |
| **BUG-02** | Sentry False-Alarm Suppression | Replaced unbounded AABB search with line-of-sight and elevation checks. Mobs > 6 blocks below tower floor or underground without direct path are ignored. | `Test 65` (Passed) |
| **BUG-03** | Raider Building Assault & Door Breaches | Raiders previously congregated at village center. Now assign squads across village sectors; Vindicators breach wooden doors along assault corridors. | `Test 66` (Passed) |
| **BUG-04** | Raider Stuck Recovery | Raiders motionless > 200 ticks are detected by `RaidTactics.recoverStuckRaiders()` and repositioned toward village roads. | `Test 67` (Passed) |
| **BUG-05** | Guard Patrol Leash & State Machine | 6-state machine (`PATROL -> INVESTIGATE -> ENGAGE -> PURSUE -> DISENGAGE -> RETURN_TO_POST`). Max tether 32m; disengages at 48m to prevent abandonment. | `Test 68` (Passed) |
| **BUG-06** | Guard & Archer Persistence across Reloads | Constructor now calls `setPersistenceRequired()`. Full NBT serialization in `addAdditionalSaveData`/`readAdditionalSaveData` saves `HomeSettlementId`, `PostX/Y/Z`, `GuardRank`, and `PatrolState`. | `Test 69` (Passed) |
| **BUG-07** | Threat Prioritization vs Boss Mobs | Threat scoring prioritizes Mutant Commanders (15x), mutant monsters (12x), siege miners (6x), and civilian threats (+8x) over ordinary zombies. | `Test 70` (Passed) |
| **BUG-08** | Squad Tactics & Chunk Unload Safety | Guards coordinate into `COMMANDER_RESPONSE`, `FRONTLINE`, `RANGED`, and `RESERVE` squads. Chunk unloads maintain roster in allegiance registry; unregister only on death. | `Test 70`, `Test 71` (Passed) |
| **BUG-09** | 26-Worker Demographic Workforce Scaling | `VillageWorkforceManager` manages 13 vanilla professions with targets of 2 active adults each (26 total), driving expansion demand. | `Test 75` (Passed) |
| **BUG-10** | Watchtower Proportions & Varied Architecture | Overhauled blueprints to authentic fortified watchtowers (12-14m height max) and added diverse trade workshops with authentic two-block beds. | `Test 61` (Passed) |
| **BUG-11** | Iron Golem Water Buoyancy Recovery | `VillageGolemManager.handleGolemWaterRecovery()` applies upward buoyant impulse (0.20) and 16-block shoreline navigation search when submerged. | `Test 74` (Passed) |
| **BUG-12** | Village Dogs & Village Cavalry | Created `VillageDogEntity` (civilian pets and guard companions) and `VillageCavalryEntity` (mounted swordsmen, scouts, captains on persistent armored horses). | `Test 72`, `Test 73` (Passed) |
| **BUG-13** | Authentic 12x14 Spawn Egg Silhouettes | Overhauled `generate_spawn_egg_textures.ps1` to match vanilla 12x14 egg silhouette proportions across all 13 custom spawn eggs. | `Test 78` (Passed) |
| **BUG-14** | Admin Raider Grouping Tag & Commands | Created `data/livingworld/tags/entity_type/all_raiders.json` and added `/livingworld debug raiders count\|list\|teleport` and `/livingworld debug raid status\|stuck\|resolve`. | `Test 67` (Passed) |
| **BUG-15** | Mutant Commander Boss Bar & Zone Density | Enforced max 1 commander per 96m zone; `bossEvent.removeAllPlayers()` invoked on death and removal to eliminate lingering boss bars. | `Test 76` (Passed) |
| **BUG-16** | Feature F19 Dimensional Scars & Spacing | Created `DimensionalScarGenerator` and `DimensionalScarManager` supporting 7 scar types (5 Nether in Overworld, 2 Overworld in Nether), noise boundaries, and 380m spacing. | `Test 77` (Passed) |

---

## 3. Core Architectural Implementations

### A. Guard AI & State Machine (`VillageGuardEntity.java`)
- Constructor enforces `this.setPersistenceRequired()`.
- Implements 6-state machine: `PATROL`, `INVESTIGATE`, `ENGAGE`, `PURSUE`, `DISENGAGE`, `RETURN_TO_POST`.
- Disengages if distance from assigned post exceeds 48 blocks or target is unreachable.
- Custom NBT tags: `HomeSettlementId`, `PostX`, `PostY`, `PostZ`, `GuardRank`, `PatrolState`, `CompanionDogUuid`.
- Overrode `remove(RemovalReason)`: `UNLOADED_TO_CHUNK` retains allegiance registry profile; `KILLED` and `DISCARDED` perform clean unregistration.

### B. Sentry Threat Filtering & Horn Alarm (`VillageArcherEntity.java`)
- Constructor enforces `this.setPersistenceRequired()`.
- `SentryLookoutGoal.isValidThreat()`: filters out subterranean mobs (> 6 blocks below sentry position) and mobs lacking clear line-of-sight.
- `performHornAlarm()`: equips `Items.GOAT_HORN`, calls `startUsingItem`, plays localized audio at sentry coordinates, triggers alert, restores bow after 30 ticks.

### C. Village Dogs & Guard Companions (`VillageDogEntity.java`)
- Subclasses `Wolf` with 24 HP, 5.0 attack damage, 0.32 movement speed.
- Roles: `DogRole.CIVILIAN_PET` (neighborhood roaming) and `DogRole.GUARD_COMPANION` (guard escort).
- `pairWithGuard(VillageGuardEntity)` links companion to guard handler via UUID, defends handler in combat, and registers under `VILLAGE_ALLIANCE` with `MilitaryRole.MELEE_GUARD`.

### D. Village Cavalry (`VillageCavalryEntity.java`)
- Subclasses `VillageGuardEntity` with 36 HP, 7.0 attack damage, 0.30 movement speed.
- Roles: `MOUNTED_SWORDSMAN`, `MOUNTED_SCOUT`, `MOUNTED_CAPTAIN`.
- Automatically spawns and mounts persistent horse equipped with saddle and iron/diamond horse armor.
- Re-mounts assigned horse when separated within 16 blocks; fights on foot if mount is destroyed.

### E. Feature F19 Dimensional Scars (`DimensionalScarGenerator.java`, `DimensionalScarManager.java`)
- Bidirectional Nether–Overworld incursions spanning 80–180 block diameters (default: 120 blocks).
- 5 Nether Incursions in Overworld: Warped Forest, Crimson Forest, Soul Sand Valley, Basalt Deltas, Nether Wastes.
- 2 Overworld Enclaves in Nether: Lost Village Enclave, Woodland Fragment.
- Multi-harmonic noise boundary blending for natural irregular perimeter borders.
- Ruined Nether Gateway and abandoned Overworld cottage structures.
- Spacing constraint: minimum 380 blocks between scars; respects player claims and settlements.
- Chunk load hook registered in `LivingWorldMod.java` via `ServerChunkEvents.CHUNK_LOAD`.

---

## 4. Verification & Testing

### Acceptance Test Suite (`scripts/test.ps1`)
- **Total Tests:** 78
- **Passed:** 78
- **Failed:** 0
- **Vanilla Invariant:** 0 files in `assets/minecraft/*`. Zero vanilla texture overrides.

### Gameplay Playtest Suite (`scripts/test_gameplay.ps1`)
- **Scenario 1:** Autonomous Civilization (Expansion, Husbandry, Stockpiles, Save/Reload) — **PASS**
- **Scenario 2:** Undead Takeover (Horde Assault, Garrison Roles, Day Shelter, Liberation) — **PASS**
- **Scenario 3:** Original Mobs (Shark, Swooper, Zombie Livestock Models & Textures) — **PASS**
- **Scenario 4:** Faction Warfare (Nether Piglin Border Shift & Territory Control) — **PASS**
- **Scenario 5:** Exploration & Traversal (Atlas Book GUI, Mineshaft Depot, 2.5x Rails, Mount Step-Up, Boat Glide) — **PASS**
- **Scenario 6:** Boss Encounters (Colossal Dreadbeast, Nether Champion, Multi-Phase Dragon) — **PASS**
- **Scenario 7:** Performance & Stability (MSPT, Heap Allocations, Two-Scale Simulation) — **PASS**

---

## 5. Deployment & Git Status
- **Compiled JAR:** `build/libs/livingworld-0.2.6.jar` (355,243 bytes)
- **Active Mod Instance:** `instances/LivingWorld/minecraft/mods/livingworld-0.2.6.jar`
- **Git Author:** `PrathamKapoor <prathamkapoor027@gmail.com>`
- **Identity Confidentiality:** Strict zero AI disclosure across all commits, files, and git history.
- **Push Hold Status:** **HELD**. DO NOT PUSH until user issues `"PUSH"`.
