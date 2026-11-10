# MOD_SOURCE_MATRIX.md — Living World Ecosystem Mod Reuse & Integration Matrix

## 1. Directive & Methodology

Under the **Source-First, Research-First, Integration-First** directive for Version 0.2.4:
- Living World inspects mature, high-quality mods within the Minecraft Fabric ecosystem.
- We integrate verified, licensed dependencies where appropriate, rather than reinventing complex external engines.
- We adapt proven architectural patterns and techniques (e.g. humanoid entity rendering, terrain survey foundations, tactical combat AI).
- We maintain strict license compliance, source attribution, and ensure zero violation of the Vanilla Invariant.

---

## 2. Comprehensive Mod Integration & Reference Matrix

| Mod Name | Creator / Maintainer | Version in Instance | License | Source / Repository | Role in Living World Ecosystem | Integration Type |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Mutant Monsters** | Fuzss | `v26.3.1-mc26.3.x+fabric` | AGPL-3.0 | [GitHub](https://github.com/Fuzss/mutant-monsters) | Classic mutant creatures (Mutant Zombie, Mutant Skeleton, Mutant Creeper, Hulk Hammer). Coexists with Living World's 3-headed Horde Commander. | Ingested Mod Jar + AI Isolation + Guard Defense Priority |
| **Puzzles Lib** | Fuzss | `v26.3.17-mc26.3.x+fabric` | MPL-2.0 | [GitHub](https://github.com/Fuzss/puzzles-lib) | Framework dependency providing config architecture and rendering hooks for Mutant Monsters. | Ingested Mod Jar |
| **Forge Config API Port**| Fuzss | `v26.3.1-mc26.3.x-Fabric` | MPL-2.0 | [GitHub](https://github.com/Fuzss/forge-config-api-port) | Multi-loader configuration bridge enabling client/server config sync without crashes. | Ingested Mod Jar |
| **ChoiceTheorem's Overhauled Village (CTOV)** | ChoiceTheorem | `26.3-4.2.2a` | CC-BY-NC-ND-4.0 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/choicetheorems-overhauled-village) | Completely overhauls vanilla villages with authentic cultural architecture across biomes. | Ingested Mod Jar + Auto-Discovery Integration |
| **Cloth Config** | shedaniel | `26.3.159` | LGPL-3.0 | [GitHub](https://github.com/shedaniel/cloth-config) | Client configuration library required by CTOV and modern Fabric UI menus. | Ingested Mod Jar |
| **Mielon's The Sift** | Mielon | `2.0.2-mc26.3-fabric` | MIT | [Modrinth](https://modrinth.com/mod/the-sift) | Atmospheric dimension portal, ancient structures, and traversal mechanics. | Ingested Mod Jar + Teleportation Integration |
| **GeckoLib** | Gecko | `5.5.7` | MIT | [GitHub](https://github.com/bernie-g/geckolib) | Keyframe skeletal animation library for complex creatures. | Ingested Mod Jar |
| **Lithostitched** | Apollo | `2.0.6-fabric-26.3` | LGPL-3.0 | [Modrinth](https://modrinth.com/mod/lithostitched) | Modern world generation optimization and data pack stitching. | Ingested Mod Jar |
| **Better Vanilla Dragon Fight** | AzureDoom | `v1.0.1+26.3.0` | MIT | [GitHub](https://github.com/AzureDoom/Better-Vanilla-Dragon-Fight) | Enhanced multi-phase Ender Dragon combat with shockwaves and strafing attacks. | Ingested Mod Jar |
| **Moog's End Structures**| finallion_builder | `1.21-2.1.1` | LGPL-3.0 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/moogs-end-structures) | Adds temples, airships, and towers across outer End islands. | Ingested Mod Jar |
| **Moog's Nether Structures**| finallion_builder | `1.21-3.1.1` | LGPL-3.0 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/moogs-nether-structures) | Adds Piglin settlements, volcanic altars, and fortified bastions to Nether biomes. | Ingested Mod Jar |
| **Moog's Structure Lib** | finallion_builder | `26.3-3.4.2` | LGPL-3.0 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/moogs-structure-lib) | Common structure placement and bounding box validation library. | Ingested Mod Jar |
| **Xaero's World Map** | Xaero96 | `1.47.2` | ARR | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map) | Full-screen interactive world map with fog of war and waypoints. | Ingested Mod Jar |
| **Xaero's Minimap** | Xaero96 | `26.6.2` | ARR | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap) | Radar minimap with entity tracking, cardinal directions, and waypoints. | Ingested Mod Jar |
| **Regrowth Revamped** | MacTso | `1.1.0+26.3` | MIT | [GitHub](https://github.com/MacTso/Regrowth) | Villagers actively repair and improve settlements; animals re-seed grass. | Ingested Mod Jar |
| **Structory** | Botmon (Stardust Labs)| `1.3.18+26.3` | Stardust Labs | [Modrinth](https://modrinth.com/mod/structory) | Worldgen structures including outposts, ruins, and pilgrim camps. | Ingested Mod Jar |
| **Explorify** | Team Bloom | `v1.6.6` | ARR | [Modrinth](https://modrinth.com/mod/explorify) | Dungeons, campsites, and ambient structures across overworld. | Ingested Mod Jar |
| **Fabric Language Kotlin**| FabricMC | `1.14.2+kotlin.2.4.21`| Apache 2.0 | [GitHub](https://github.com/FabricMC/fabric-language-kotlin) | Kotlin language adapter for modern Fabric mods. | Ingested Mod Jar |
| **Fabric API** | FabricMC | `0.165.0+26.3` | Apache 2.0 | [GitHub](https://github.com/FabricMC/fabric) | Essential Fabric hooks, event bus, and registry lifecycle. | Ingested Mod Jar |
| **Guard Villagers** (Pattern) | TallestEgg | Pattern Reference | MIT | [GitHub](https://github.com/TallestEgg/Guard-Villagers) | Reference architecture for villager defense, bipedal guard models with 3D noses, and equipment layers. | Independent Source-First Adaptation |

---

## 3. Coexistence & Architecture Rules

1. **Namespace Separation Invariant**:
   - Living World entities and items reside strictly under `livingworld:*`.
   - Mutant Monsters entities and items reside strictly under `mutantmonsters:*`.
   - External dependencies are never re-namespaced or bundled into `livingworld.jar`.
2. **Vanilla Asset Preservation Invariant**:
   - Zero overrides in `assets/minecraft/*`. All vanilla textures and models are referenced natively.
3. **Tactical AI Safeguard**:
   - Living World's entity load listener (`ServerEntityEvents.ENTITY_LOAD`) verifies the entity namespace before attaching tactical breach or mining AI. Modded entities (such as Mutant Zombies) run 100% native specialized AI without conflict.
4. **Defense Priority Pipeline**:
   - Living World's `VillageGuardEntity` and `VillageArcherEntity` prioritize defending settlements against elite threats (target selector Priority 2 targets `mutantmonsters:*` and `livingworld:mutant_commander`).
