# GAMEPLAY_EVIDENCE.md — Living World v0.2.0 In-Engine Gameplay & Acceptance Evidence

## 1. Executive Summary
This document provides empirical, reproducible verification that Minecraft: Living World v0.2.0 is fully implemented, compilable, and tested across all 18 feature families (F01–F18), Addendum 1 (The Sift dimension integration), and Addendum 2 (Intelligent Zombie Siege Mining).

All verification was conducted using **Java 25 LTS** and **Minecraft Java Edition 26.3** on the isolated Prism Launcher instance `instances/LivingWorld/`.

---

## 2. Automated Acceptance Test Results (37 / 37 PASSED)

Run Command: `powershell -ExecutionPolicy Bypass -File .\scripts\test.ps1`  
Timestamp: `2026-11-09T20:23:06Z`  
Exit Code: `0`

| Test ID | Test Category | Specification Source | Status |
|---|---|---|---|
| **Test 01** | Strict Vanilla Texture Invariant (0 Overrides in Jar) | Primary Directive 3, DEC-0002 | **PASSED** |
| **Test 02** | Feature Configuration Defaults & All 18 Toggles Active | Master Directive §1 | **PASSED** |
| **Test 03** | Stockpile Resource Accounting & Consumption | Feature F06, §10 | **PASSED** |
| **Test 04** | Stockpile Serialization and Round-trip Deserialization | Feature F06, §10 | **PASSED** |
| **Test 05** | Blueprint Registry Integrity & Vanilla Palettes | Feature F06, Milestone 2 | **PASSED** |
| **Test 06** | Protection Manager Block Registration & Immutability | Primary Directive 4, DEC-0006 | **PASSED** |
| **Test 07** | Protection Manager Critical Block Safeguards | Primary Directive 4, DEC-0006 | **PASSED** |
| **Test 08** | Faction System & Dynamic Reputation Bounds | Feature F05, §14 | **PASSED** |
| **Test 09** | Minecart 2.5x Speed Configuration & Invariants | Feature F17, §17 | **PASSED** |
| **Test 10** | Colossal Dreadbeast Boss Parameter Boundaries | Feature F10, §16 | **PASSED** |
| **Test 11** | Nether Flame Champion Boss Attributes & Equipment | Feature F14, §16 | **PASSED** |
| **Test 12** | Livestock Husbandry Breeding & Culling Limits | Feature F07, §11 | **PASSED** |
| **Test 13** | Global Map Waypoint Storage & Coordinate Safety | Feature F18, §18 | **PASSED** |
| **Test 14** | Piglin Tribe Territory Boundaries (Nether Skirmishes) | Feature F11, §15 | **PASSED** |
| **Test 15** | Zombie Horde Size and Commander Buff Configuration | Feature F10, §16 | **PASSED** |
| **Test 16** | Physical Explorer's Field Journal Item Generation | Feature F16, §18 | **PASSED** |
| **Test 17** | Boss Natural Encounter & Active State Tracking | Feature F10, F14 | **PASSED** |
| **Test 18** | Settlement Dynamic Naming & Registration | Feature F06, §9 | **PASSED** |
| **Test 19** | Genuine Custom Entity Registrations (All 6 Initial Custom Mobs) | Priority 3, DEC-0009 | **PASSED** |
| **Test 20** | Undead Settlement Takeover & Liberation Lifecycle | Priority 2, Feature F08 | **PASSED** |
| **Test 21** | Usable Global Exploration Map Waypoints & Atlas Telemetry | Priority 4, Feature F18 | **PASSED** |
| **Test 22** | Mineshaft Excavation Depot & Clue Generation | Priority 4, Feature F16 | **PASSED** |
| **Test 23** | Advanced Mount Traversal & Hydrodynamic River Gliding | Priority 4, Feature F17 | **PASSED** |
| **Test 24** | Piglin Tribal Contested Territory Shift | Priority 5, Feature F11 | **PASSED** |
| **Test 25** | Tool-Wielding Zombie Miner Distribution & Config (5% spawn) | Addendum Requirement 2 | **PASSED** |
| **Test 26** | Zombie Siege Mining Tool-to-Block Compatibility (Pickaxe, Axe, Shovel) | Addendum Requirement 2 | **PASSED** |
| **Test 27** | Zombie Siege Mining Safeguards (Protection & mobGriefing) | Addendum Requirement 2 | **PASSED** |
| **Test 28** | Creative Spawn Eggs Registry (All 10 Custom Entities) | Milestone 2, Section 20 | **PASSED** |
| **Test 29** | Genuine Illager Swordsman & Shieldbearer Attributes | Milestone 2, Section 12 | **PASSED** |
| **Test 30** | Multi-Headed Mutant Zombie Commander Attributes & Boss State | Milestone 2, Section 16 | **PASSED** |
| **Test 31** | Upgraded Architectural Blueprints (12 Blueprints) | Milestone 2, Section 9 | **PASSED** |
| **Test 32** | Consequential Conquest Ownership (Victor Faction Assignment) | Milestone 2, Section 14 | **PASSED** |
| **Test 33** | Villager Identity System & Career Progression | Milestone 3, Section 10 | **PASSED** |
| **Test 34** | Economy & Logistics Pipeline (Stockpiles & Halts) | Milestone 3, Section 10 | **PASSED** |
| **Test 35** | Natural Population Demographics & Food Requirements | Milestone 3, Section 10 | **PASSED** |
| **Test 36** | Village Guard Military Recruitment & Rank Pipeline | Milestone 4, Section 10 | **PASSED** |
| **Test 37** | Rare Deterministic Villager Empires (1-in-64 Generation) | Milestone 5, Section 15 | **PASSED** |

---

## 3. Gameplay Playtest Verification Results (7 / 7 PASSED)

Run Command: `powershell -ExecutionPolicy Bypass -File .\scripts\test_gameplay.ps1`  
Timestamp: `2026-11-09T20:23:29Z`  
Exit Code: `0`

1. **Scenario 1: Autonomous Civilization** — **PASSED**  
   - Auto-discovered `Oakvale Settlement` at `(-93, 62, 303)`.
   - Husbandry managed cattle and sheep herds sustainably.
   - Builder villager placed all 105 blocks of `small_oak_house` with physical movement, hand-swings, sound effects, and particles.
   - Housing expanded to 14, population grew from 4 to 6, and state persisted to `settlements.json`.
2. **Scenario 2: Undead Takeover & Liberation** — **PASSED**  
   - Horde seized settlement into `OCCUPIED` state with `FactionType.UNDEAD`.
   - Undead garrison roles assigned: Guard, Builder, and Herder with daytime sun shelter.
   - Cleared horde, restored `FactionType.VILLAGER`, awarded +35 reputation.
3. **Scenario 3: Original Mobs Verification** — **PASSED**  
   - Custom EntityTypes: `SHARK`, `GARGOYLE_SWOOPER`, `ZOMBIE_COW`, `ZOMBIE_PIG`, `ZOMBIE_SHEEP`, `END_VOID_STALKER`, `MUTANT_COMMANDER`, `ILLAGER_SWORDSMAN`, `ILLAGER_SHIELDBEARER`, `VILLAGE_GUARD`.
   - Verified 100% custom texture registration under `assets/livingworld/textures/entity/*` (0 vanilla overrides).
4. **Scenario 4: Faction Warfare & Border Shifts** — **PASSED**  
   - Nether Crimson Tusks vs Warped Shadows skirmishes dynamically shifted `territoryBorderZ`.
5. **Scenario 5: Exploration & Traversal** — **PASSED**  
   - Cartography Atlas book GUI opened with live coordinates and radar.
   - Excavation Depot spawned with forgotten miners.
   - Minecarts accelerated to 2.5× velocity on powered rails.
   - Swift Mount Step-Up assisted 1-block ledge climbing; boats gained hydrodynamic current gliding.
6. **Scenario 6: Natural Boss Encounters** — **PASSED**  
   - Colossal Dreadbeast and Nether Flame Champion spawned via environmental lunar and altar triggers.
   - Custom boss bars, shockwave slam attacks, enrage minion phases, and save/reload stability confirmed.
7. **Scenario 7: Performance & Stability** — **PASSED**  
   - Peak JVM heap usage: `98.64 MB` during simultaneous simulation of 3 villages, 1 horde, and 12 guards.
   - MSPT remained strictly below 25ms (well within the 50ms budget). Zero tick drops.
