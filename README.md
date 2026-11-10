# Minecraft: Living World

[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg)](https://www.minecraft.net/)
[![Fabric Loader](https://img.shields.io/badge/Fabric-0.20.1-blue.svg)](https://fabricmc.net/)
[![Java 25 LTS](https://img.shields.io/badge/Java-25%20LTS-orange.svg)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tests: 55/55 Passing](https://img.shields.io/badge/Tests-55%2F55%20Passing-success.svg)](docs/FEATURE_MATRIX.md)

**Minecraft: Living World** is a comprehensive, civilization-driven Survival overhaul for **Minecraft Java Edition 26.3**, powered by **Fabric Loader 0.20.1** and **Java 25 LTS**.

The mod transforms the Minecraft sandbox into a living, responsive world where villages autonomously expand and defend themselves, hostiles execute coordinated siege tactics, undead hordes conquer and fortify settlements, predator ecologies thrive in oceans and skies, and player creations remain strictly protected.

All features are designed to feel native to Minecraft, preserving 100% of vanilla textures and original gameplay freedom.

---

## Key Feature Families

### 1. Autonomous Civilizations & Architecture
- **Physical Block-by-Block Construction**: Villagers physically walk to surveyed construction plots, swing tools, and place blocks one-by-one with authentic placement sounds and particle effects.
- **Ground Surveying & Foundations**: Structures survey complete footprints ($W \times D$), evaluate terrain variance, and build solid downward foundation columns (Stone Bricks and Cobblestone) down to natural ground level, eliminating floating buildings.
- **Architectural Blueprints**: 13 hand-crafted vanilla-block blueprints including the Imperial Royal Palace (35×14×25 with vaulted throne room and twin gilded thrones), Timber Cottage (9×8×8 with porch and stone chimney), Fortified Watchtower (9×16×9 with machicolated battlements), Barracks, Gatehouses, and Markets.
- **Domestic Livestock Husbandry**: Villagers breed cattle, pigs, sheep, and chickens using food stockpiles, sustainably culling surplus adults above breeding baselines and depositing meat, leather, and wool into settlement stockpiles.

### 2. Settlement Defense & Military Standing Armies
- **Village Guards & Archers**: Unemployed villagers are recruited into standing town defense forces. Melee guards field iron swords, shields, and full iron armor; archers field recurve bows, arrows, and armor.
- **3D Armor & Weapon Rendering**: Features dedicated bipedal models (`VillageGuardModel`) with authentic 3D villager noses and humanoid limbs. Equipped armor (iron, diamond, netherite) renders in 3D across all four slots (`HEAD`, `CHEST`, `LEGS`, `FEET`) via Minecraft's native `HumanoidArmorLayer`.
- **Dynamic Combat Mechanics**:
  - **Shield Defense**: Melee guards raise shields across their chest (`ArmPose.BLOCK`) in combat, deflecting 100% of projectile and frontal melee damage before executing counter-strikes.
  - **Archery Stance**: Archers aim with `ArmPose.BOW_AND_ARROW`, draw bowstrings back with realistic tension curves (`bow_pulling_0..2`), strafe for line-of-sight, and discharge arrows along calculated elevation arcs.
- **Military Rank Progression**: Guards earn promotions through combat (Recruit -> Soldier -> Veteran -> Elite -> Royal Guard), visibly upgrading equipment from iron to diamond and netherite.

### 3. Intelligent Hostile Mobs & Undead Sieges
- **Tactical Breaching**: Zombies breach wooden doors and trapdoors on all difficulties; tool-wielding siege miners equipped with pickaxes, axes, or shovels break through matching barrier blocks with progressive crack stages (0–9) and coordinate breach charges.
- **Tactical Archery**: Skeletons maintain firing standoffs, kiting players when rushed and seeking elevated vantage points.
- **Cover Breaching**: Creepers evaluate obstacle thickness and detonate tactically to flush players from cover, while strictly respecting player-built protected claims.
- **Flanking Raids**: Illager squads field swift melee shocktroopers sprint-charging frontline defenders while crossbowmen fire from flanking angles.

### 4. Undead Occupations & Zombified Livestock
- **Settlement Conquest & Fortification**: If zombies overwhelm a village, the settlement falls into Undead Occupation. Undead garrison commanders, guards, builders, and herders spawn, fortifying the town with cobblestone barricades and iron bars.
- **Daytime Shelter**: Undead occupiers seek indoor roofed shelter during daytime to prevent sunlight burn.
- **Livestock Infection**: Non-player cattle, pigs, and sheep attacked by zombies convert into original registered `livingworld:zombie_cow`, `livingworld:zombie_pig`, and `livingworld:zombie_sheep` entities.
- **Heroic Liberation**: Defeating the undead garrison liberates the settlement, restoring civilian control and awarding substantial reputation.

### 5. Marine Ecology & Predators
- **Great White Shark (`livingworld:shark`)**: Apex marine predator navigating deep oceans with a custom 3D model (pointed snout, articulated biting jaws, triangular dorsal fin, vertical heterocercal caudal fin). Hunts fish, tracks bleeding players (< 50% HP) via blood scent, and diverts to raw meat/fish bait.
- **Gargoyle Swoopers (`livingworld:gargoyle_swooper`)**: High-altitude aerial threats in mountains and badlands executing steep dive attacks. Blocking with a shield stuns the swooper.

### 6. Exploration, Traversal & Quality of Life
- **Cartography Atlas Book**: Right-clicking with a Map or Compass opens an interactive in-game ledger with live coordinates, fog of war, ASCII chunk radar, settlement directory, and saved waypoints.
- **Mineshaft Excavation Depots**: Abandoned mineshafts feature reinforced supply depots with miner chests guarded by forgotten miner skeletons.
- **Swift Mounts & Water Gliding**: Horses gain smooth single-block step-up assist; boats in river biomes gain hydrodynamic current acceleration (+35% speed); powered rails accelerate minecarts by 2.5×.

### 7. Core Non-Negotiable Invariants
- **Vanilla Invariant**: Zero overrides, recoloring, or replacements of any vanilla texture or model. Existing textures are strictly referenced natively.
- **Player & World Protection**: Faction griefing, creeper breaches, and zombie mining strictly respect player claims, chests, portals, and tamed/named pets.
- **Two-Scale Simulation**: Low-frequency strategic updates for unloaded settlements (population, trade, diplomacy); physical block placement and tactical AI restricted strictly to loaded chunks near players.

---

## Technical Specifications

| Parameter | Specification |
| :--- | :--- |
| **Minecraft Version** | 26.3 |
| **Loader** | Fabric Loader 0.20.1+ |
| **Java Environment** | Java 25 LTS (OpenJDK / Oracle JDK) |
| **Mappings** | Official Mojang Mappings |
| **Fabric API** | 0.165.0+26.3 |
| **Architecture** | Component-Driven & RenderState Two-Phase Pipeline |

---

## Ecosystem Mod Compatibility

Living World coexists seamlessly with established Fabric 26.3 ecosystem mods:
- **Mutant Monsters** (Fuzss): Native coexistence with isolated AI goals and village defense priority.
- **ChoiceTheorem's Overhauled Village (CTOV)**: Overhauled villages integrate automatically as base settlements.
- **Mielon's The Sift**: Dimension portal and ancient structures fully integrated.
- **Lithostitched**: World generation stitching and data pack optimization.
- **Better Vanilla Dragon Fight**: Multi-phase boss mechanics preserving vanilla ambient audio.
- **Moog's End & Nether Structures**: Expanded structures and bastions.
- **Xaero's World Map & Minimap**: Integrated waypoints and mapping support.

---

## Building from Source

### Prerequisites
- JDK 25 installed with `JAVA_HOME` configured.
- Git.

### Build Instructions
```powershell
# Compile Java sources, generate spawn egg & guard textures, and package mod JAR:
.\scripts\build.ps1
```
The compiled JAR is produced at `build/libs/livingworld-0.2.4.jar`.

### Running Tests
```powershell
# Run the 55-test automated acceptance suite:
.\scripts\test.ps1

# Run the 7-scenario live gameplay playtest suite:
.\scripts\test_gameplay.ps1

# Run the 60-second in-world Survival simulation:
.\scripts\test_quickplay_world.ps1
```

---

## In-Game Commands

```mcfunction
# Status & Configuration
/livingworld status
/livingworld config
/livingworld villages

# Instant Structure Placement & Building
/livingworld blueprints
/livingworld place <name>
/livingworld build <name>

# Guard Spawning & Showcase
/livingworld debug guard spawn melee
/livingworld debug guard spawn archer

# Integrations & Diagnostics
/livingworld integrations list
/livingworld integrations mutant_monsters status
/livingworld debug mutants showcase
/livingworld debug horde inspect
/livingworld debug visual catalog
```

---

## Documentation

Comprehensive architecture, reverse engineering, and design records are available in the [`docs/`](docs/) directory:
- [`docs/FEATURE_MATRIX.md`](docs/FEATURE_MATRIX.md) — Complete 18-family feature status and verification record.
- [`docs/REVERSE_ENGINEERING.md`](docs/REVERSE_ENGINEERING.md) — Bytecode analysis of Minecraft 26.3 rendering pipeline.
- [`docs/GUARD_RENDERING_ROOT_CAUSE.md`](docs/GUARD_RENDERING_ROOT_CAUSE.md) — Root cause analysis of guard armor rendering.
- [`docs/ARMOR_AND_WEAPON_TESTS.md`](docs/ARMOR_AND_WEAPON_TESTS.md) — Equipment verification and animation matrices.
- [`docs/MOD_SOURCE_MATRIX.md`](docs/MOD_SOURCE_MATRIX.md) — Mod reuse matrix, licensing, and attribution.
- [`decisions.md`](decisions.md) — Architecture Decision Records (DEC-0001 through DEC-0016).
- [`flow.md`](flow.md) — Chronological development work journal.

---

## License

This project is licensed under the [MIT License](LICENSE).
Copyright (c) 2026 Pratham Kapoor.
