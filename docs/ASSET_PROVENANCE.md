# ASSET_PROVENANCE.md — Asset Provenance & Integrity Register

**Project:** Minecraft: Living World v0.2.0  
**Verification Date:** 2026-11-10  
**Primary Invariant:** Zero Vanilla Asset Overrides | Strictly Original Pixel-Art & Referenced Vanilla Models  

---

## 1. Asset Provenance Registry

| Asset Identifier | Asset Type | Source / Origin | License | Modification / Adaptation | Collision with `minecraft:*` |
|---|---|---|---|---|---|
| `livingworld:textures/entity/shark/shark.png` | Entity Texture | Original Living World Pixel-Art (64x64) | MIT | Authored in-tree | NO (Isolated namespace) |
| `livingworld:textures/entity/swooper/gargoyle_swooper.png` | Entity Texture | Original Living World Pixel-Art (64x32) | MIT | Authored in-tree | NO (Isolated namespace) |
| `livingworld:textures/entity/zombie/zombie_cow.png` | Entity Texture | Original Living World Pixel-Art (64x32) | MIT | Authored in-tree | NO (Isolated namespace) |
| `livingworld:textures/entity/zombie/zombie_pig.png` | Entity Texture | Original Living World Pixel-Art (64x32) | MIT | Authored in-tree | NO (Isolated namespace) |
| `livingworld:textures/entity/zombie/zombie_sheep.png` | Entity Texture | Original Living World Pixel-Art (64x32) | MIT | Authored in-tree | NO (Isolated namespace) |
| `livingworld:textures/entity/end/end_void_stalker.png` | Entity Texture | Original Living World Pixel-Art (64x32) | MIT | Authored in-tree | NO (Isolated namespace) |
| `livingworld:textures/entity/zombie/mutant_commander.png` | Entity Texture | Original Living World Pixel-Art (128x64) | MIT | Authored in-tree for 3-Headed Commander | NO (Isolated namespace) |
| `livingworld:textures/models/armor/overlay_*` | Armor Wear Overlays | Original Greyscale Transparency Overlays | MIT | Additive render layers (scratches, dents) | NO (Rendered via custom layer) |
| Vanilla Blocks & Items | Models / Textures | Minecraft 26.3 Client | Mojang EULA | Sourced strictly by reference | ZERO overrides |

---

## 2. Invariant Audit Procedure

Automated test `Test 01` in [`LivingWorldTests.java`](file:///C:/Users/LENOVO/Downloads/PrismLauncher-Windows-MinGW-w64-Portable-11.0.3/src/test/java/livingworld/LivingWorldTests.java) performs recursive archive inspection of the compiled mod JAR, asserting that:
1. `entry.getName().startsWith("assets/minecraft/")` count is **strictly 0**.
2. All custom assets reside exclusively under `assets/livingworld/*`.
3. All vanilla texture and model references use official namespace identifiers.
