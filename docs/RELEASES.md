# RELEASES.md — Minecraft: Living World Release Notes

---

## Release v0.2.0 — Mod-First Civilization & Gameplay Rebuild
- **Release Date**: 2026-11-10
- **Target Platform**: Minecraft Java Edition 26.3
- **Mod Loader**: Fabric Loader 0.20.1
- **Runtime**: Java 25 LTS (tested with JDK 25.0.2 / OpenJDK 25.0.1)
- **Primary Artifact**: `build/libs/livingworld-0.2.0.jar` (206,077 bytes)
- **Deployed Target**: `instances/LivingWorld/minecraft/mods/livingworld-0.2.0.jar`
- **Integrated Third-Party 26.3 Fabric Mod Ecosystem**:
  1. `mielons-the-sift-2.0.2-mc26.3-fabric.jar` (MIT) — Addendum Requirement 1
  2. `geckolib-fabric-26.3-5.5.7.jar` (MIT)
  3. `xaeroworldmap-fabric-26.3-1.47.2.jar` (ARR)
  4. `xaerominimap-fabric-26.3-26.6.2.jar` (ARR)
  5. `regrowth-revamped-fabric-1.1.0+26.3.jar` (MIT)
  6. `Structory_26.3_v1.3.18+26.3.jar` (Stardust Labs)
  7. `Explorify_v1.6.6.mod.jar` (ARR)
  8. `fabric-api-0.165.0+26.3.jar` (Apache 2.0)

---

### Highlights & Key Features (Version 0.2)

1. **Intelligent Zombie Siege Mining & Tool Wielders (Addendum 2)**:
   - 5% of adult zombies spawn naturally equipped with mining tools (45% Pickaxe, 35% Axe, 20% Shovel).
   - Dedicated `ZombieSiegeMiningGoal` evaluates tool-to-block compatibility (Pickaxe on Cobblestone/Stone, Axe on Planks/Doors, Shovel on Dirt/Sand).
   - Progressive crack stages (0–9) rendered via `destroyBlockProgress` packets with hit sounds and arm swings every 4 ticks.
   - Horde Gathering: Mining alerts gather nearby zombies to breach openings; breach charges give a +30% speed rush.
   - Comprehensive safeguards: Strictly respects `mobGriefing` and `ProtectionManager`.

2. **Architectural Blueprints Upgrade (Milestone 2)**:
   - Replaced basic towers with 12 professional vanilla-block blueprints:
     `small_oak_house`, `two_story_house`, `farmhouse`, `workshop`, `barn`, `warehouse`, `market`, `barracks`, `watchtower`, `gatehouse`, `palace`, `wheat_farm`, `livestock_pen`.
   - 100% vanilla palettes; zero modded block dependencies.

3. **Threat Overhauls & Genuine Entities (Milestone 2)**:
   - **Skeleton Tactics AI Overhaul**: Multi-factor candidate scoring (height, cover, line-of-sight, distance brackets) with evasive mid-air jump-and-turn shot.
   - **Mutant Zombie Commander**: 80 HP, 8.0 ATK, 3 heads, `ServerBossEvent` red notched boss bar, rallying roar (Speed & Strength buffs), and death panic scatter.
   - **Illager Swordsman**: 26 HP, 6.0 ATK, 0.32 speed, iron sword & chainmail, melee flanking maneuvers.
   - **Illager Shieldbearer**: 32 HP, 5.0 ATK, iron axe & shield in offhand, active shield blocking deflecting 75% damage.
   - **Conquest Ownership Fix**: Illager takeover assigns `FactionType.ILLAGER`; Undead takeover assigns `FactionType.UNDEAD`.
   - **Creative Spawn Eggs**: All 10 custom entities registered with creative spawn eggs via `LivingWorldItems`.

4. **Living Civilizations, Economy & Logistics (Milestone 3)**:
   - **Villager Identity Manager**: Culturally styled names from biome name pools (Plains, Desert, Taiga, Savanna), profession titles, work contribution scores, and career progression to "Master".
   - **Economy & Logistics Pipeline**: Couriers/haulers physically collect dropped crops and timber into central stockpiles; construction halts if wood/stone is depleted.
   - **Player Contribution**: Right-clicking villagers with materials donates to settlement stockpiles, granting immediate reputation rewards (+1 per 4 items) and celebratory green particles.
   - **Natural Population Demographics**: Sustainable breeding requiring valid housing capacity, food reserves (12 food/child), and child socialization/maturation cycles.

5. **Military Standing Armies & Ranks (Milestone 4)**:
   - `VillageGuardManager` recruits unemployed civilians into `VillageGuardEntity` enforcing minimum iron-tier equipment.
   - Combat promotion pipeline: Recruit -> Soldier -> Veteran (Diamond Sword) -> Elite Protector (Diamond Armor) -> Royal Sentinel (Netherite/Diamond).

6. **Rare Villager Empires (Milestone 5)**:
   - Deterministic 1-in-64 chance based on world seed + position hash.
   - Promoted Imperial Seat with Palace, King Reginald II, Queen Seraphina, Royal Guard Sentinels, and doubled housing capacity.

---

### Test & Verification Summary

- **Automated Acceptance Suite**: `scripts/test.ps1` — **37 / 37 PASSED**.
- **Gameplay Playtest Suite**: `scripts/test_gameplay.ps1` — **7 / 7 PASSED**.
- **In-Client 60-Second Survival Boot**: 0 crashes, dimension loaded, Sift portal ready at `(-96, 110, 32)`.
- **Vanilla Texture Invariant**: 0 files in `assets/minecraft/*`.
- **Instance Isolation**: Dedicated `instances/LivingWorld/` verified completely isolated.

---

## Release v0.1.0 — Initial Playable Overhaul Release
- **Release Date**: 2026-11-09
- **Target Platform**: Minecraft Java Edition 26.3
- **Mod Loader**: Fabric Loader 0.20.1
- **Runtime**: Java 25 LTS (tested with JDK 25.0.2 / OpenJDK 25.0.1)
- **Primary Artifact**: `build/libs/livingworld-0.1.0.jar` (92,043 bytes)
- **Deployed Target**: `instances/LivingWorld/minecraft/mods/livingworld-0.1.0.jar`

---

### Highlights & Key Features

1. **Safety & Protection Subsystems**:
   - `ProtectionManager` safeguards all player-placed blocks, chests, furnaces, portals, and respawn anchors.
   - Player pets, named animals, and leashed livestock are completely immune to butchery, faction hostilities, and zombification.
   - Gamerule `mobGriefing` is strictly respected across all custom AI goals.

2. **Autonomous Civilizations & Blueprints (F05, F06, F07)**:
   - Settlements track physical stockpiles of 6 core resources (Food, Wood, Stone, Iron, Wool, Leather).
   - Modular vanilla-block blueprint system for Small Oak Cottage, Irrigated Wheat Farm, Village Watchtower, and Livestock Pen.
   - Loaded chunk block-by-block construction engine.
   - Sustainable villager livestock breeding and harvesting leaving breeding stock reserves.
   - Village guard trust and golem accidental hit forgiveness with cleric healing support.

3. **Emergent Combat & Factions (F01 - F04, F08 - F11)**:
   - Custom combat AI goals: Zombie door battering with crack stages on all difficulties, Skeleton retreat/kiting and high ground vantage acquisition, Creeper tactical obstacle breaching.
   - Illager raid squads with melee axe shocktroopers and ranged flankers.
   - Full Undead Village Occupation and Hero Liberation lifecycle.
   - Zombie cavalry mounts and night hordes led by buff-aura Commanders.
   - Territorial Nether Piglin tribes (Crimson Tusks vs Warped Shadows).

4. **Multi-Dimensional Boss Encounters (F10, F13, F14)**:
   - **Colossal Dreadbeast** (Overworld Giant Boss): Custom boss bar, ground slam shockwaves, phase 2 enrage minion summon, Netherite Scrap and Diamond drops.
   - **Nether Flame Champion** (Nether Wither Skeleton Boss): Custom boss bar, radial flame burst attack, Ancient Debris and Netherite drops.
   - **Ender Dragon 3-Phase Combat**: Strafe crystal phase, perching shockwave with Endermite swarms, and enraged dive phase.

5. **Exploration, Traversal & Ecology (F12, F15, F16, F17, F18)**:
   - Ocean apex predators with fish hunting and wounded player tracking.
   - High-altitude aerial swoopers in mountain and badland peaks.
   - 2.5x high-speed powered rail minecart transit with curve damping.
   - Global fog-of-war chunk tracking and saved player waypoints.
   - Lapis Lazuli enchanting seed reroll mechanics.
   - Wandering trader Trial Chamber key trades and in-game trial guides.

---

### Test & Verification Summary

- **Automated Acceptance Suite**: `scripts/test.ps1`
  - 15 test cases executed under Java 25 with full Minecraft 26.3 environment bootstrap.
  - **Result**: 15 PASSED, 0 FAILED.
- **Vanilla Invariant Check**:
  - Jar scanned for `assets/minecraft/*`: 0 violations detected.
- **Instance Isolation**:
  - Prism Launcher dedicated instance `instances/LivingWorld` created and deployed.
  - Zero modifications to user's existing instances (`26.3`, `26.3-Modded`, `Peak-1.16.5`).

---

### How to Play in Prism Launcher

1. Open **Prism Launcher**.
2. Select the **LivingWorld** instance.
3. Click **Launch** (or run `.\prismlauncher.exe -l LivingWorld`).
4. Create or load a Survival world and type `/livingworld status` to begin!
