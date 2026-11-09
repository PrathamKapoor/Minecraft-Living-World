# SPAWN_EGG_AUDIT.md — Custom Spawn Egg Item Models & Textures Audit

## Overview
This audit details the complete resolution of missing/checkerboard/placeholder textures across all Living World custom entity spawn eggs.
All 11 spawn eggs now possess authoritative Modern 26.3 Item Model definitions, Item Model JSONs, and bespoke 16×16 pixel-art textures.

---

## 1. Complete Spawn Egg Catalog

| # | Item ID | Associated Entity | Base Color | Spot Color | Item Model JSON | Texture PNG (16×16) | Status |
|---|---|---|---|---|---|---|---|
| 1 | `livingworld:shark_spawn_egg` | `SharkEntity` | Slate Blue (`#2C3E50`) | Oceanic Teal (`#1ABC9C`) | `items/shark_spawn_egg.json` | `textures/item/shark_spawn_egg.png` | **VERIFIED** |
| 2 | `livingworld:gargoyle_swooper_spawn_egg` | `GargoyleSwooperEntity` | Basalt Gray (`#4A4E53`) | Magma Orange (`#D35400`) | `items/gargoyle_swooper_spawn_egg.json` | `textures/item/gargoyle_swooper_spawn_egg.png` | **VERIFIED** |
| 3 | `livingworld:zombie_cow_spawn_egg` | `ZombieCowEntity` | Putrid Green (`#3A532B`) | Rotten Brown (`#5D4037`) | `items/zombie_cow_spawn_egg.json` | `textures/item/zombie_cow_spawn_egg.png` | **VERIFIED** |
| 4 | `livingworld:zombie_pig_spawn_egg` | `ZombiePigEntity` | Decayed Pink (`#A06676`) | Necrotic Green (`#2E5339`) | `items/zombie_pig_spawn_egg.json` | `textures/item/zombie_pig_spawn_egg.png` | **VERIFIED** |
| 5 | `livingworld:zombie_sheep_spawn_egg` | `ZombieSheepEntity` | Dingy Wool (`#8C9E84`) | Dried Blood (`#78281F`) | `items/zombie_sheep_spawn_egg.json` | `textures/item/zombie_sheep_spawn_egg.png` | **VERIFIED** |
| 6 | `livingworld:end_void_stalker_spawn_egg` | `EndVoidStalkerEntity` | Obsidian Void (`#1A0933`) | Ender Cyan (`#00FFC8`) | `items/end_void_stalker_spawn_egg.json` | `textures/item/end_void_stalker_spawn_egg.png` | **VERIFIED** |
| 7 | `livingworld:mutant_commander_spawn_egg` | `MutantZombieCommanderEntity`| Toxic Green (`#1E4620`) | Crimson Red (`#E74C3C`) | `items/mutant_commander_spawn_egg.json` | `textures/item/mutant_commander_spawn_egg.png` | **VERIFIED** |
| 8 | `livingworld:illager_swordsman_spawn_egg` | `IllagerSwordsmanEntity` | Ash Gray (`#616A6B`) | Iron Blade (`#BDC3C7`) | `items/illager_swordsman_spawn_egg.json` | `textures/item/illager_swordsman_spawn_egg.png` | **VERIFIED** |
| 9 | `livingworld:illager_shieldbearer_spawn_egg` | `IllagerShieldbearerEntity` | Ash Gray (`#616A6B`) | Shield Oak (`#513E2E`) | `items/illager_shieldbearer_spawn_egg.json` | `textures/item/illager_shieldbearer_spawn_egg.png` | **VERIFIED** |
| 10 | `livingworld:village_guard_spawn_egg` | `VillageGuardEntity` | Villager Tan (`#93613E`) | Polished Iron (`#CFD8DC`) | `items/village_guard_spawn_egg.json` | `textures/item/village_guard_spawn_egg.png` | **VERIFIED** |
| 11 | `livingworld:village_archer_spawn_egg` | `VillageArcherEntity` | Villager Tan (`#93613E`) | Fletching Green (`#27AE60`) | `items/village_archer_spawn_egg.json` | `textures/item/village_archer_spawn_egg.png` | **VERIFIED** |

---

## 2. Asset Structure & Modern Item Model Pipeline

In Minecraft 26.3, the engine utilizes the unified Item Definition system. Each spawn egg requires two JSON definitions and one PNG texture:

1. **Item Definition (`assets/livingworld/items/<id>.json`)**:
   ```json
   {
     "model": {
       "type": "minecraft:model",
       "model": "livingworld:item/<id>"
     }
   }
   ```
2. **Item Model (`assets/livingworld/models/item/<id>.json`)**:
   ```json
   {
     "parent": "minecraft:item/generated",
     "textures": {
       "layer0": "livingworld:item/<id>"
     }
   }
   ```
3. **16×16 Pixel-Art Texture (`assets/livingworld/textures/item/<id>.png`)**:
   - Standard 16×16 egg silhouette mask.
   - Darkened outline (40% brightness of base color).
   - Base egg fill.
   - Curved highlight specular (135% brightness of base color) on top-left shoulder.
   - Distinctive colored spot patterns across the egg body.

---

## 3. Automation & Verification
- Generated via automated script: `scripts/generate_spawn_egg_textures.ps1`.
- Integrated directly into the compilation pipeline (`scripts/build.ps1`).
- Verified via acceptance test `Test 45` in `LivingWorldTests.java`: audits existence, non-emptiness, and registry binding for all 11 spawn eggs.
- Zero missing purple/black checkerboards in Creative inventory, hand holding, or `/give` commands.
