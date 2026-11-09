# MUTANT_RENDER_AUDIT.md — 3-Headed Mutant Zombie Commander Visual & Model Audit

## Overview
This audit verifies the visual fidelity, custom 3D geometry, model rigging, texture resolution, and client renderer bindings for the **3-Headed Mutant Zombie Commander** (`livingworld:mutant_commander`).

---

## 1. Custom 3D Model Architecture (`MutantZombieCommanderModel.java`)

The Mutant Zombie Commander features completely bespoke 3D geometry authored specifically for boss-tier presence:

| Model Component | Dimensions (W×H×D) | Pivot Offset | Rotation / Articulation | Visual Purpose |
|---|---|---|---|---|
| **Center Head** | 8.0 × 8.0 × 8.0 | `(0.0F, -4.0F, 0.0F)` | Dynamic pitch & yaw tracking | Dominant commander head with exposed cranial bone, piercing red eyes, and snarl fangs. |
| **Left Mutated Head** | 7.0 × 7.0 × 7.0 | `(5.5F, -2.5F, 0.5F)` | Pitch: +0.10, Yaw: +0.40, Roll: +0.15 | Mutated lateral head with independent animated sway and red glowing eyes. |
| **Right Mutated Head** | 7.0 × 7.0 × 7.0 | `(-5.5F, -2.5F, 0.5F)` | Pitch: +0.10, Yaw: -0.40, Roll: -0.15 | Mirrored mutated lateral head scanning the battlefield with periodic sinus-wave offset. |
| **Mutated Torso** | **14.0 × 15.0 × 7.0** | `(0.0F, -4.0F, 0.0F)` | Static hunched | Broadened hulking chest with exposed necrotic ribs, rusted iron plates, and dorsal spine ridge. |
| **Left Arm (Clawed)** | **4.5 × 13.0 × 4.5** | `(7.5F, -2.0F, 0.0F)` | Forward menacing reach | Mutated sinewy limb with sharp claw articulation. |
| **Right Arm (Club)** | **6.0 × 16.0 × 6.0** | `(-7.5F, -3.0F, 0.0F)` | Forward heavy swing | Oversized mutated club smasher arm; 33% thicker and longer than the left arm. |
| **Left Leg** | 5.0 × 14.0 × 5.0 | `(3.5F, 11.0F, 0.0F)` | Leg swing animation | Thick, muscular mutated limb supporting massive torso weight. |
| **Right Leg** | 5.0 × 14.0 × 5.0 | `(-3.5F, 11.0F, 0.0F)` | Mirrored leg swing | Heavy mutated limb with iron plate greaves. |

---

## 2. Boss Scale & Matrix Transformations (`MutantZombieCommanderRenderer.java`)

In `MutantZombieCommanderRenderer.java`, a custom `scale` matrix transformation is registered:
```java
@Override
protected void scale(ZombieRenderState state, PoseStack poseStack) {
    super.scale(state, poseStack);
    poseStack.scale(1.45f, 1.45f, 1.45f);
}
```
- **Scale Multiplier**: **1.45×** uniform scale.
- **In-Game Height**: Towering at ~2.9 blocks tall (compared to standard 1.95-block zombies).
- **Shadow Radius**: Set to `1.1f` for proportional boss ground shadows.
- **Client Initializer Binding**: Cleanly registered in `LivingWorldClientMod.java` via `EntityRendererRegistry.register(LivingWorldEntities.MUTANT_COMMANDER, MutantZombieCommanderRenderer::new)`.

---

## 3. High-Detail Pixel Art Texture (`mutant_commander.png`)

- **File Path**: `src/main/resources/assets/livingworld/textures/entity/zombie/mutant_commander.png`
- **Resolution**: 64×64 RGBA PNG.
- **Pixel Art Elements**:
  - **Rotting Skin Palette**: Layered dark forest green (`#2D4426`), moss green (`#4A6B3D`), and pale sickly olive (`#60874E`).
  - **Exposed Bone & Cranial Plates**: Ivory white (`#DCD7C3`) on forehead and exposed chest ribcage.
  - **Glowing Eyes**: Luminous crimson (`#E62828`) pupils with piercing golden iris highlights (`#FFC81E`).
  - **Tattered Armor & Clothing**: Corroded iron plating (`#8C9196`), dark rust accents (`#824B2D`), and tattered imperial purple tunic remnants (`#371E41`).

---

## 4. Verification Evidence
- Automated test: `Test 30` in `LivingWorldTests.java` validates 180 HP, 12 ATK, 10 Armor, and knockback resistance.
- Model geometry verified in `src/main/java/livingworld/client/model/MutantZombieCommanderModel.java`.
- Matrix scaling and renderer registration verified in `src/main/java/livingworld/client/MutantZombieCommanderRenderer.java`.
