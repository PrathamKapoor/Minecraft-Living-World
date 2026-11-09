# MOD_RESEARCH.md — Minecraft: Living World v0.2 Mod Candidate Research

**Target Environment:** Minecraft Java Edition 26.3 | Fabric Loader 0.20.1 | Java 25 LTS  
**Instance:** `instances/LivingWorld/`  
**Date:** 2026-11-10  

---

## 1. Executive Summary & Research Methodology

In accordance with Section 2 of the *Living World v0.2 Lead Developer Master Directive*, all candidate systems must be evaluated across four options:
- **Option A — Direct integration:** Compatible, legally distributable mod installed as an independent dependency in `instances/LivingWorld/minecraft/mods/`.
- **Option B — Licensed source adaptation:** Porting, forking, or adapting code from a permitted license (MIT, LGPL, AGPL) with strict notice/attribution preservation.
- **Option C — Datapack and structure integration:** Data-driven jigsaw pools, world generation structures, or structure packs.
- **Option D — Original implementation:** Custom Living World implementation using established libraries and patterns where dependencies are fragile, unmaintained, or conflicting.

---

## 2. Comprehensive Candidate Evaluations

### Candidate 01: Mielon's The Sift (ADDENDUM REQUIREMENT 1)
- **Author:** Mielon
- **Official Project URL:** https://www.curseforge.com/minecraft/mc-mods/mielons-the-sift / https://modrinth.com/mod/mielons-the-sift
- **Source / Release Tag:** 2.0.2 (`mielon's-the-sift-2.0.2-mc26.3-fabric.jar`)
- **Minecraft Version:** 26.3
- **Loader:** Fabric
- **Required Dependencies:** Fabric API (`P7dR8mSH`), GeckoLib (`8BmcQJ2H`)
- **Code License:** MIT
- **Asset License:** Custom / MIT (Permitted for inclusion in modpacks and client instances)
- **Compatibility Confidence:** HIGH (Official 26.3 Fabric build verified on Modrinth CDN)
- **Relevant Gameplay Feature:** Dimensional expansion, Ancient City portal awakening, Singer entity, Echo Golems, Siftite gear, Charoite enchanting
- **Proposed Integration Method:** **Option A (Direct Integration)** into `instances/LivingWorld/minecraft/mods/`
- **Conflicting Functionality:** None. Overworld entity hooks do not conflict with Living World AI goals.
- **Performance Implications:** Dimension loaded only on portal entry; minimal baseline Overworld footprint.
- **Final Decision:** **INTEGRATE**

---

### Candidate 02: GeckoLib (Section 3.H)
- **Author:** Gecko, Eliot, AzureDoom
- **Official Project URL:** https://modrinth.com/mod/geckolib
- **Source / Release Tag:** 5.5.7 (`geckolib-fabric-26.3-5.5.7.jar`)
- **Minecraft Version:** 26.3
- **Loader:** Fabric
- **Required Dependencies:** Fabric API
- **Code License:** MIT
- **Asset License:** MIT
- **Compatibility Confidence:** HIGH (Standard animation library for 26.3 Fabric)
- **Relevant Gameplay Feature:** Animated mob models, bone animation, multi-headed zombie commander, expressive NPC movement
- **Proposed Integration Method:** **Option A (Direct Integration)** as required runtime dependency
- **Conflicting Functionality:** None; independent animation pipeline.
- **Performance Implications:** Negligible overhead when distant rendering LODs are respected.
- **Final Decision:** **INTEGRATE**

---

### Candidate 03: Xaero's World Map & Xaero's Minimap (Section 3.J, F18)
- **Author:** xaero96
- **Official Project URL:** https://modrinth.com/mod/xaeros-world-map / https://modrinth.com/mod/xaeros-minimap
- **Source / Release Tag:** `xaeroworldmap-fabric-26.3-1.47.2.jar` & `xaerominimap-fabric-26.3-26.6.2.jar`
- **Minecraft Version:** 26.3
- **Loader:** Fabric
- **Required Dependencies:** Fabric API
- **Code License:** All Rights Reserved (ARR)
- **Asset License:** All Rights Reserved (ARR)
- **Compatibility Confidence:** HIGH (Official 26.3 Fabric releases tested)
- **Relevant Gameplay Feature:** F18 Global graphical map, fog of war, waypoints, minimap radar
- **Proposed Integration Method:** **Option A (Direct Integration)** as unmodified binary dependency in the dedicated instance; integration with Living World waypoint API.
- **Conflicting Functionality:** Replaces previously needed ASCII book radar with high-definition graphical UI.
- **Performance Implications:** Extremely efficient C/OpenGL multithreaded chunk mapper.
- **Final Decision:** **INTEGRATE**

---

### Candidate 04: Regrowth: Revamped (Section 3.B, F06)
- **Author:** mqlEubgZ
- **Official Project URL:** https://modrinth.com/mod/regrowth-revamped
- **Source / Release Tag:** 1.1.0+26.3 (`regrowth-revamped-fabric-1.1.0+26.3.jar`)
- **Minecraft Version:** 26.3
- **Loader:** Fabric
- **Required Dependencies:** Fabric API
- **Code License:** MIT
- **Asset License:** MIT
- **Compatibility Confidence:** HIGH
- **Relevant Gameplay Feature:** F06 Village perimeter wall building, road maintenance, villager repair routines
- **Proposed Integration Method:** **Option A (Direct Integration)** with Living World coordinating settlement growth and scheduling.
- **Conflicting Functionality:** Wall placement must coordinate with Living World's `ConstructionManager` to prevent blueprint collisions.
- **Performance Implications:** Low; path-based block repair on idle ticks.
- **Final Decision:** **INTEGRATE**

---

### Candidate 05: Villager Guards / Guard Villagers (Section 3.C, 3.D, F05)
- **Author:** Talrey / modded datapack
- **Official Project URL:** https://modrinth.com/datapack/villager-guards
- **Source / Release Tag:** 1.2.0+mod (`villager-guards-1.2.0.jar`)
- **Minecraft Version:** 26.3
- **Loader:** Fabric
- **Required Dependencies:** Fabric API
- **Code License:** AGPL-3.0-or-later
- **Asset License:** AGPL-3.0-or-later
- **Compatibility Confidence:** MEDIUM-HIGH
- **Relevant Gameplay Feature:** F05 Villager military recruitment, equipment distribution, combat patrols
- **Proposed Integration Method:** **Option A / Option D Hybrid:** Use `villager-guards` as external base, orchestrated by Living World `MilitaryManager` to enforce minimum iron protection and promotion tiers.
- **Conflicting Functionality:** Guard recruitment must preserve existing profession NBT if retired.
- **Performance Implications:** Bounded AI target goals during peacetime; active during raids.
- **Final Decision:** **INTEGRATE & COORDINATE**

---

### Candidate 06: MineColonies (Section 3.A)
- **Author:** MineColonies Team
- **Official Project URL:** https://www.curseforge.com/minecraft/mc-mods/minecolonies
- **Minecraft Version:** 1.20.x / 1.21.x NeoForge (No native Fabric 26.3 build)
- **Code License:** GPLv3
- **Asset License:** All Rights Reserved
- **Compatibility Confidence:** INCOMPATIBLE AS BINARY JAR
- **Relevant Gameplay Feature:** Construction logistics, courier item transport, warehouse accounting
- **Proposed Integration Method:** **Option D (Original Implementation informed by MineColonies patterns):** Replicate the multi-step construction logistics (Need -> Plot -> Materials -> Courier Transport -> Builder Placement) natively in Living World's `ConstructionManager` and `EconomyManager`.
- **Final Decision:** **REFERENCE ONLY (Architecture & Logistics Patterns)**

---

### Candidate 07: More Villagers (Section 3.E, F07)
- **Author:** samedifferent
- **Official Project URL:** https://github.com/samedifferent/morevillagers
- **Minecraft Version:** 1.20.x Fabric (No 26.3 release on Modrinth)
- **Code License:** MIT
- **Asset License:** All Rights Reserved
- **Compatibility Confidence:** INCOMPATIBLE AS BINARY JAR
- **Relevant Gameplay Feature:** Extended professions (Rancher, Carpenter, Miner, Courier)
- **Proposed Integration Method:** **Option D (Original Implementation):** Implement custom `VillagerProfession` registrations and point-of-interest (POI) types natively in Living World, tying them to real economic output rather than simple trading tables.
- **Final Decision:** **ADAPT PATTERNS / NATIVE IMPLEMENTATION**

---

### Candidate 08: MCA Reborn (Section 3.F, Section 6)
- **Author:** Luke100000, Wildfire, et al.
- **Official Project URL:** https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn
- **Minecraft Version:** 1.20.x (Heavy overhaul)
- **Code License:** LGPL-3.0
- **Asset License:** Custom / ARR
- **Compatibility Confidence:** INCOMPATIBLE AS BINARY JAR
- **Relevant Gameplay Feature:** Villager names, persistent identities, dialogue
- **Proposed Integration Method:** **Option D (Native Implementation):** Maintain persistent name generation, identity UUIDs, and civic titles via Living World's `VillagerIdentityManager`.
- **Final Decision:** **REFERENCE ONLY**

---

### Candidate 09: Illager Invasion (Section 3.G, F04)
- **Author:** Fuzs
- **Official Project URL:** https://www.curseforge.com/minecraft/mc-mods/illager-invasion
- **Code License:** MIT
- **Asset License:** ARR / Custom
- **Compatibility Confidence:** INCOMPATIBLE AS BINARY JAR FOR 26.3
- **Relevant Gameplay Feature:** Swordsman illagers, shieldbearers, tactical raiders
- **Proposed Integration Method:** **Option D (Native Living World Entities):** Implement genuine custom illager classes (`IllagerSwordsmanEntity`, `IllagerShieldbearerEntity`, `TacticalRaiderEntity`) using vanilla-style models and MIT tactical AI patterns.
- **Final Decision:** **NATIVE IMPLEMENTATION**

---

### Candidate 10: Structure Libraries — Explorify, Structory, Towns and Towers (Section 3.I)
- **Explorify (v1.6.6):** Modrinth ID `HSfsxuTo`, ARR. Validated 26.3 Fabric. Jigsaw pool additions for dungeons, camps, and ruins.
- **Structory (1.3.18+26.3):** Modrinth ID `aKCwCJlY`, Stardust Labs License. Validated 26.3 Fabric. Adds atmospheric vanilla-block waystations, watchtowers, and cottages.
- **Towns and Towers (1.13.13):** Modrinth ID `CC-BY-NC-SA-4.0`. Validated 26.3 Fabric. Architectural redesign of vanilla villages and pillager outposts.
- **Proposed Integration Method:** **Option A (Direct Integration)** for `Structory` and `Explorify` into `instances/LivingWorld/minecraft/mods/` as compatible worldgen foundations.
- **Final Decision:** **INTEGRATE**
