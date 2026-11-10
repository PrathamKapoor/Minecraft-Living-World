# HANDOFF.md — Living World v0.2.7 Engineering Handoff Document

**Project:** Minecraft: Living World  
**Release:** Version 0.2.7 — The Siege & Settlement Reliability Update  
**Target Platform:** Minecraft Java Edition 26.3 / Fabric Loader 0.20.1 / Java 25  
**Instance Path:** `instances/LivingWorld/` (Dedicated, isolated instance)  
**Latest Verified Artifact:** `instances/LivingWorld/minecraft/mods/livingworld-0.2.7.jar` (380,764 bytes, 78 compiled classes)  
**Rollback Artifact:** `backup_v0.2.6/livingworld-0.2.6.jar` (357,654 bytes)  
**Git Repository:** `https://github.com/PrathamKapoor/Minecraft-Living-World` (Branch: `main`)  
**Push Hold Status:** **ACTIVE (STRICT)** — All commits created locally. Zero pushes to remote repository until user explicitly commands `"PUSH"`.

---

## 1. Executive Summary
Living World v0.2.7 resolves all 10 real-gameplay defects (`DEF-01` through `DEF-10`) identified during hands-on creator playtesting. The update makes previously promised gameplay tangible in-world:
1. Illagers now possess a strategic macro-level raid objective: **Attacking the Village Bell**.
2. A coordinated 7-state assault machine (`ASSEMBLE -> ADVANCE -> ENGAGE_DEFENDERS -> BREACH -> ATTACK_BELL -> SECURE_VILLAGE -> VICTORY`) directs pillagers and illagers along village roads.
3. Physical bell siege mechanic with visible attack animations, metallic hit sounds, debris/silencing, and community material-driven reconstruction.
4. Stuck raiders are automatically detected via a 160m navigation diagnostic engine with 3-tiered resolution (200t surface recovery, 600t glowing outline + horn warning, 1200t failsafe discard), completely eliminating hanging raid boss bars.
5. Every qualifying settlement automatically maintains **two lookout watchtowers** with solid ladder backing columns, lookout platforms at y=10, and stationed archer sentries.
6. Settlements naturally maintain **2–4 civilian dogs** and **1 guard companion war dog per 4 melee guards**.
7. Settlements naturally mobilize **mounted cavalry** (Swordsmen, Scouts, Captains) riding armored, saddled horses when military capacity >= 3.
8. Blueprint architectural integrity overhaul across all 24 blueprints, fixing missing roofs, open sky gaps, missing doors, and unattached ladders.
9. All 10 administrative diagnostic commands registered under `/livingworld debug ...`.
10. All 88 automated acceptance tests, all 8 gameplay playtest scenarios, and a 60-second in-world Survival simulation passed with zero errors.

---

## 2. Defect Resolution Matrix

| Defect ID | Title | Root Cause & Resolution | Verification |
|---|---|---|---|
| **DEF-01** | Illagers fail to reach village / wander in forest | Vanilla nearest-mob targeting distracted raiders into forests/caves. Replaced with macro-level `RaidObjective` bell routing along surface terrain. | `Test 79`, `Scenario 8` |
| **DEF-02** | Raids remain active with trapped/lost raiders | Mobs trapped in subterranean caverns could not exit. Implemented 160m `RaiderDiagnostic` with 200t surface recovery, 600t glowing outline, and 1200t failsafe resolution. | `Test 81`, `Scenario 8` |
| **DEF-03** | Pillagers lack meaningful tactical objectives | Illagers acted as disconnected brawlers. Implemented 7-state tactical assault machine and physical bell attack strikes (100t threshold). | `Test 79`, `Test 80` |
| **DEF-04** | Village watchtowers do not reliably generate | Watchtowers were only attempted once during initial village discovery when perimeter chunks were unloaded. Added periodic pass in `SettlementManager.tick()` (every 200t) ensuring 2 watchtowers. | `Test 82`, `Scenario 8` |
| **DEF-05** | Village cavalry does not naturally appear | Missing automated mobilization trigger in recruitment loop. Added cavalry mobilization in `VillageGuardManager` when military capacity >= 3. | `Test 84`, `Scenario 8` |
| **DEF-06** | Village dogs and military companions do not appear | Entities registered but not ticked by demographic engine. Implemented `tickSettlementDogs()` maintaining 2-4 civilian dogs and 1 guard companion per 4 guards. | `Test 83`, `Scenario 8` |
| **DEF-07** | Several houses incomplete, visually malformed, or open | Blueprint wall/roof loops omitted doors, ladders, and solid ceilings in `barracks`, `farmhouse`, `two_story_house`, `inn`, `clinic`. Added complete roofs and valid doors. | `Test 85`, `Scenario 8` |
| **DEF-08** | Villager and illager AI remains unreliable | Competing AI goals constantly cleared navigation targets. Centralized objective movement in `RaidTactics.tickRaidTactics()` with role-specific speeds and vindicator door breaches. | `Test 79`, `Test 81` |
| **DEF-09** | Guards and defenders fail to coordinate | Threat scoring treated all monsters equally. Implemented dynamic scoring in `VillageDefenseController` prioritizing boss mobs (15x), miners (6x), and civilian threats (+8x). | `Test 86`, `Scenario 8` |
| **DEF-10** | Iron golem water-navigation defects | Unchecked block loading during water searches caused potential chunk stalls. Added `level.isLoaded(p)` guards and 0.20 vertical buoyancy impulse. | `Test 74`, `Test 88` |

---

## 3. Core Architectural Implementations

### A. Village Bell Raid Objective & 7-State Assault Machine (`RaidTactics.java`)
- `findVillageBell(level, center, radius)`: Scans village volume to locate physical `Blocks.BELL`.
- `RaidObjective`: Registers target bell coordinate, physical presence, assault state, and active raiders.
- 7 Tactical States:
  1. `ASSEMBLE`: Gathering invasion forces at raid spawn perimeter.
  2. `ADVANCE`: Moving along village roads toward target bell (pillagers: 1.15 speed, shieldbearers: 1.10 speed, vindicators: 1.25 speed).
  3. `ENGAGE_DEFENDERS`: Clashing with village guards, archers, and golems.
  4. `BREACH`: Vindicators breaking wooden doors with axe strikes and sounds.
  5. `ATTACK_BELL`: Melee units within 3.5m assaulting the village bell.
  6. `SECURE_VILLAGE`: Bell neutralized; mopping up surviving civilian defenders.
  7. `VICTORY`: Raid victory declared.
- Physical Bell Strike Mechanic (`performBellAssault`):
  - Visible main-hand swing animation.
  - Metallic clang audio (`SoundEvents.BELL_BLOCK`) and critical hit particles.
  - At 100 ticks (5s sustained assault):
    - `mobGriefing=true`: Block destroyed with `SoundEvents.ANVIL_DESTROY` and explosion particles.
    - `mobGriefing=false`: Bell silenced with `SoundEvents.BELL_RESONATE`.
- Community Reconstruction: Surviving villagers consume 5 stone or 1 iron from stockpiles to reconstruct the bell at its original position.

### B. 160m Stuck Raider Navigation Diagnostics (`RaidTactics.java`)
- `RaiderDiagnostic`: Tracks UUID, entity type, position, distance to bell, stationary ticks, underground status, and trapped flag.
- Three-tiered recovery escalation:
  - **T1 (> 200 ticks)**: If underground (`surfaceY > currentY + 4`), teleports to surface heightmap and resumes pathfinding to bell.
  - **T2 (> 600 ticks)**: Applies `MobEffects.GLOWING` for 10s and sounds `RAID_HORN` audio at raider position.
  - **T3 (> 1200 ticks)**: If <= 3 raiders remain alive, discards lingering entity to cleanly dismiss the raid boss bar.

### C. Guaranteed Dual Lookout Watchtowers (`SettlementManager.java`)
- `ensureWatchtowers()`: Automatically verified every 200 ticks in `SettlementManager.tick()`.
- Guarantees 2 watchtowers placed at 45° and 225° perimeter angles.
- Foundation columns filled down to ground using `LivingWorldCommand.fillFoundationColumns()`.
- Solid cobblestone ladder backing column with climbing ladder firmly attached.
- Sentry lookout platform at y=10 with parapet fences and solid roof at y=12.
- Stations elevated `VillageArcherEntity` with `isWatchtowerSentry=true` scanning 48–64m perimeters.

### D. Natural Companion Dogs & Cavalry Mobilization (`VillageGuardManager.java`)
- `tickSettlementDogs()`:
  - Maintains 2–4 civilian pet dogs wandering village streets.
  - Maintains 1 guard companion dog per 4 melee guards.
  - Companions link via UUID (`pairWithGuard`), follow handler within 6m, and attack enemies targeting the handler or civilians.
- `forceRecruitCavalry()`:
  - Mobilizes when military capacity >= 3 (1 cavalry) and >= 6 (2 cavalry).
  - Tames, saddles, and equips horse with iron or diamond horse armor.
  - Full mount/dismount combat AI: remounts horse if separated, fights on foot if mount is destroyed.

### E. Blueprint Structural Integrity Overhaul (`BlueprintRegistry.java`, `Blueprint.java`)
- Added `addDoor(x, y, z, facing, doorBlock)` ensuring correct double-block half states.
- Added `addLadder(x, y, z, facing)` attaching ladders to solid backing blocks.
- Overhauled 24 blueprints:
  - `barracks`: Added solid stone brick roof (y=5..6), oak entrance door, internal lantern lighting.
  - `small_oak_house`: Added front entrance door and solid pitched roof.
  - `two_story_house`: Added entrance door, internal staircase with stairwell opening, and pitched roof with zero sky gaps.
  - `farmhouse`, `workshop`, `warehouse`, `barn`, `inn_guesthouse`, `cleric_clinic`: Added solid roofs, doors, and authentic 2-block beds.

---

## 4. Diagnostic Debug Commands Suite

| Command | Permission | Purpose |
|---|---|---|
| `/livingworld debug raid objective` | All | Displays raid phase, target bell coordinates, physical presence, distance, and siege progress. |
| `/livingworld debug raid raiders` | All | 160m diagnostic scan of all active raiders, distance to bell, stuck status, and underground status. |
| `/livingworld debug raid stuck` | All | Detects stationary/trapped raiders within 160m and triggers surface road navigation recovery. |
| `/livingworld debug raid bell` | All | Inspects targeted village bell block status, siege strike ticks, and emits visual particles. |
| `/livingworld debug village towers` | All | Inspects watchtower count, coordinates, platform sentry archers, and generates missing towers. |
| `/livingworld debug village buildings` | All | Lists all registered settlement blueprints, coordinates, completion status, and bed counts. |
| `/livingworld debug village construction` | All | Displays progress of current active block-by-block construction project and queued projects. |
| `/livingworld debug village dogs` | All | Lists all village dogs, roles (`Civilian Pet` / `Guard Companion`), and handler UUID links. |
| `/livingworld debug cavalry status` | All | Displays deployed village cavalry units within 128m, role, horse health, and armor status. |
| `/livingworld debug military patrols` | All | Comprehensive military readiness diagnostic: threat posture, active rally point, and forces deployed. |

---

## 5. Verification & Testing

### Acceptance Test Suite (`scripts/test.ps1`)
- **Total Tests:** 88
- **Passed:** 88
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
- **Scenario 8:** The Siege & Settlement Reliability (Bell Objective, Stuck Resolution, Towers, Dogs, Cavalry) — **PASS**

### In-World Survival Simulation (`scripts/test_quickplay_world.ps1`)
- Launched official Minecraft 26.3 client into dedicated instance `instances/LivingWorld/` on NVIDIA GPU.
- Ran for 60 seconds of full in-world Survival gameplay in `PlaytestSurvival`.
- Monitored working set (738 MB), heap, chunk loading, tool-bearing zombie spawn, and simulation tick.
- Exited cleanly with 0 crashes, 0 errors, and clean world save.

---

## 6. Git Status & Remote Restrictions
- **Push Hold Status:** **ACTIVE (STRICT)** — Zero pushes to remote repository.
- **Configured Git Author:** `PrathamKapoor <prathamkapoor027@gmail.com>`
- **Local Commits:** Prepared for clean local commit.
