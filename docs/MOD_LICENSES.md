# MOD_LICENSES.md — Software & Asset Licensing Registry

**Project:** Minecraft: Living World v0.2.0  
**Compliance Standard:** Section 2.1 & 2.2 of Living World Directives  

---

## 1. Mod Licensing Registry

| Artifact / Mod Name | Project URL | Code License | Asset / Resource License | Usage Rights & Redistribution Terms | Living World Strategy |
|---|---|---|---|---|---|
| **Living World v0.2** | Local Project | MIT | MIT / Vanilla Reference | Proprietary mod project; zero vanilla texture overrides. | Primary authoring |
| **Mutant Monsters** | https://github.com/Fuzss/mutant-monsters | AGPL-3.0 | AGPL-3.0 | Permitted as unmodified external dependency; source link provided. | Separate unmodified dependency in `instances/LivingWorld/minecraft/mods/` |
| **Puzzles Lib** | https://github.com/Fuzss/puzzles-lib | MPL-2.0 | MPL-2.0 | Permitted as unmodified library dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Forge Config API Port** | https://github.com/Fuzss/forge-config-api-port | MPL-2.0 | MPL-2.0 | Permitted as unmodified library dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Cloth Config** | https://github.com/shedaniel/cloth-config | LGPL-3.0 | LGPL-3.0 | Permitted as unmodified library dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **ChoiceTheorem's Overhauled Village** | https://modrinth.com/mod/ctov | CC-BY-NC-ND-4.0 | CC-BY-NC-ND-4.0 | Permitted in modpacks with attribution. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Lithostitched** | https://modrinth.com/mod/lithostitched | LGPL-3.0 | LGPL-3.0 | Permitted as unmodified library dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Better Vanilla Dragon Fight** | https://modrinth.com/mod/better-vanilla-dragon-fight | MIT | MIT | Permitted as unmodified dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Moog's End Structures** | https://modrinth.com/mod/moogs-end-structures | LGPL-3.0 | LGPL-3.0 | Permitted as unmodified dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Moog's Nether Structures** | https://modrinth.com/mod/moogs-nether-structures | LGPL-3.0 | LGPL-3.0 | Permitted as unmodified dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Moog's Structure Lib** | https://modrinth.com/mod/moogs-structure-lib | LGPL-3.0 | LGPL-3.0 | Permitted as unmodified dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Fabric Language Kotlin** | https://github.com/FabricMC/fabric-language-kotlin | Apache-2.0 | Apache-2.0 | Permitted as unmodified runtime dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Fabric API** | https://github.com/FabricMC/fabric | Apache-2.0 | Apache-2.0 | Permitted as unmodified framework dependency. | Separate dependency in `instances/LivingWorld/minecraft/mods/` |
| **Mielon's The Sift** | https://modrinth.com/mod/mielons-the-sift | MIT | MIT / Custom | Unmodified binary distribution permitted in instances/modpacks. | Direct dependency in `instances/LivingWorld/minecraft/mods/` |
| **GeckoLib** | https://modrinth.com/mod/geckolib | MIT | MIT | Free for integration and runtime distribution. | Direct dependency |
| **Xaero's World Map** | https://modrinth.com/mod/xaeros-world-map | All Rights Reserved (ARR) | All Rights Reserved (ARR) | Used as unmodified binary dependency only; no redistribution of decompiled source or altered assets. | Unmodified instance dependency |
| **Xaero's Minimap** | https://modrinth.com/mod/xaeros-minimap | All Rights Reserved (ARR) | All Rights Reserved (ARR) | Used as unmodified binary dependency only. | Unmodified instance dependency |
| **Regrowth: Revamped** | https://modrinth.com/mod/regrowth-revamped | MIT | MIT | Permitted for binary inclusion and API integration. | Direct dependency |
| **Structory** | https://modrinth.com/mod/structory | Stardust Labs Custom | Stardust Labs Custom | Permitted in modpacks with attribution; no standalone asset re-hosting. | Unmodified instance dependency |
| **Explorify** | https://modrinth.com/mod/explorify | All Rights Reserved (ARR) | All Rights Reserved (ARR) | Permitted in modpacks on CurseForge/Modrinth. | Unmodified instance dependency |
| **MineColonies** | https://curseforge.com/minecraft/mc-mods/minecolonies | GPLv3 | All Rights Reserved (ARR) | Code GPLv3; assets ARR. Incompatible with Fabric 26.3. | Architectural reference only; NO CODE OR ASSET COPYING. |
| **More Villagers** | https://github.com/samedifferent/morevillagers | MIT | All Rights Reserved (ARR) | Code MIT; textures ARR. Incompatible with 26.3. | Architectural reference only; native Living World implementation. |
| **Illager Invasion** | https://github.com/Fuzss/illagerinvasion | MIT | All Rights Reserved (ARR) | Code MIT; models/textures ARR. | Architectural reference only; native vanilla-style entities. |

---

## 2. Non-Negotiable Licensing Invariants

1. **Zero Asset Copying from ARR Projects:**
   - Under no circumstances shall models, textures, or sounds from *MineColonies*, *More Villagers*, *Illager Invasion*, or *Xaero's Map* be decompiled, extracted, or bundled into Living World JARs.
2. **Vanilla Texture Integrity:**
   - Minecraft base textures (`minecraft:textures/*`) are sourced strictly by reference.
3. **Attribution & Provenance:**
   - All third-party binaries installed into the instance directory maintain their original filenames, manifests, and author metadata.
