# GUARD_RENDERING_ROOT_CAUSE.md — Forensic Audit & Architectural Remediation

## 1. Executive Summary & Forensic Findings

In Minecraft: Living World v0.2.0 through v0.2.3, player feedback and in-game screenshots repeatedly showed that **Village Guards** and **Village Archers** rendered as ordinary, bare-handed, gray-robed villagers without visible armor, swords, shields, or bows.

Despite `VillageGuardEntity.java` and `VillageArcherEntity.java` correctly equipping full iron armor and weapons on the server side (`setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET))`, etc.), **zero equipment appeared visually on the client**.

Using CFR decompilation of `livingworld-0.2.3.jar`, we performed a forensic audit of the compiled client classes and identified four fatal structural flaws in the rendering architecture.

---

## 2. Bytecode Evidence from `livingworld-0.2.3.jar`

Decompilation of `livingworld.client.VillageGuardRenderer` and `livingworld.client.VillageArcherRenderer` from the release JAR revealed the exact implementation:

```java
/*
 * Decompiled with CFR 0.152 from livingworld-0.2.3.jar
 */
package livingworld.client;

import livingworld.entity.VillageGuardEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

public class VillageGuardRenderer
extends MobRenderer<VillageGuardEntity, VillagerRenderState, VillagerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/villager/villager.png");

    public VillageGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState villagerRenderState) {
        return TEXTURE;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }
}
```

---

## 3. The Four Fatal Flaws Explained

### Fatal Flaw 1: Using Vanilla `VillagerModel` (Fused Arms, Zero Armor Slots)
Vanilla `VillagerModel` represents peaceful NPC traders. Its model geometry:
- Contains a single monolithic fused box for arms (`PartDefinition arms = root.addOrReplaceChild("arms", ...)`).
- Has **no** `right_arm` or `left_arm` parts.
- Has **no** attachment points for held weapons, shields, or tools.
- Does not implement `ArmedModel` or extend `HumanoidModel`.

Because of this, vanilla `VillagerModel` is physically incapable of holding items or swinging swords.

### Fatal Flaw 2: Zero Armor Layer Registration
In Minecraft's rendering pipeline, entity models do not render armor items themselves. Armor rendering is delegated to render layers (specifically `HumanoidArmorLayer`).
- Neither `VillageGuardRenderer` nor `VillageArcherRenderer` ever called `this.addLayer(new HumanoidArmorLayer(...))`.
- Without an armor layer registered, the renderer simply ignores any armor equipped in the entity's inventory.

### Fatal Flaw 3: Using `VillagerRenderState` (Equipment Data Dropped)
In Minecraft 26.3's two-phase render architecture, renderers only see data stored in the `EntityRenderState`.
- `VillagerRenderState` is designed strictly for vanilla villagers. It contains fields for villager profession, biome type, and head shaking.
- `VillagerRenderState` contains **zero fields** for `headEquipment`, `chestEquipment`, `legsEquipment`, `feetEquipment`, `rightHandItemState`, or `leftHandItemState`.
- Neither renderer implemented `extractRenderState(T entity, S state, float partialTicks)`.
- Consequently, 100% of the equipment data present on the entity was dropped before the graphics pipeline could execute.

### Fatal Flaw 4: Using Generic `RangedAttackGoal` Instead of Bow-Drawing Mechanics
In `VillageArcherEntity.java`:
- The entity registered `RangedAttackGoal` instead of an active bow goal like `RangedBowAttackGoal`.
- `RangedAttackGoal` simply counts down a tick timer and calls `performRangedAttack()`. It **never** invokes `entity.startUsingItem(InteractionHand.MAIN_HAND)`.
- Because `startUsingItem` was never invoked:
  - `entity.isUsingItem()` was always `false`.
  - `entity.getTicksUsingItem()` was always `0`.
  - The client bow model never entered pulling animation frames (`bow_pulling_0`, `bow_pulling_1`, `bow_pulling_2`).
  - No bow draw pose could be triggered.

---

## 4. The Architectural Remediation (v0.2.4)

To permanently resolve these defects and elevate Living World to professional mod engineering standards, we implement a four-pillar solution:

```
+-----------------------------------------------------------------------------------------+
|                                LIVING WORLD v0.2.4 GUARD ARCHITECTURE                   |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|  1. VillageGuardModel                                                                   |
|     - Extends HumanoidModel<VillageGuardRenderState>                                    |
|     - Head with 3D Villager nose (-1, -3, -6, 2, 4, 2) + hair/hat layer                |
|     - Independent humanoid limbs: rightArm, leftArm, rightLeg, leftLeg                  |
|     - Implements ArmedModel & HeadedModel                                               |
|                                                                                         |
|  2. VillageGuardRenderState                                                             |
|     - Extends HumanoidRenderState (ArmedEntityRenderState)                              |
|     - Captures all 4 armor slots: headEquipment, chestEquipment, legs, feet             |
|     - Captures held items: rightHandItemState, leftHandItemState                        |
|     - Captures use item ticks and animation states for bow drawing & shield blocking    |
|     - Stores GuardRank for visual rank progression                                      |
|                                                                                         |
|  3. Overhauled VillageGuardRenderer & VillageArcherRenderer                             |
|     - Extends HumanoidMobRenderer<T, VillageGuardRenderState, VillageGuardModel>        |
|     - Inherits automatic ItemInHandLayer for weapons and shields                        |
|     - Adds HumanoidArmorLayer with ArmorModelSet (iron, diamond, netherite 3D armor)    |
|     - Overrides getArmPose: returns BLOCK for shields, BOW_AND_ARROW for bows           |
|                                                                                         |
|  4. VillageArcherBowAttackGoal                                                          |
|     - Standoff aiming and strafing movement                                             |
|     - Invokes startUsingItem(MAIN_HAND) during target acquisition                       |
|     - Dynamic pull progress linked to bow tension curves and projectile release         |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

---

## 5. Verification Matrix

| Defect Observed in Screenshots | Root Cause | v0.2.4 Fix | Verification Standard |
| :--- | :--- | :--- | :--- |
| **No armor visible on guards** | Missing `HumanoidArmorLayer` + `VillagerRenderState` dropped equipment | Registered `HumanoidArmorLayer` with `ArmorModelSet` + `HumanoidRenderState` | Full iron/diamond/netherite armor renders visibly in all 4 slots. |
| **Guards hold no weapons** | `VillagerModel` has fused arms box and no item layer | `VillageGuardModel` humanoid limbs + `ItemInHandLayer` | Iron sword visible in right hand, shield in left hand. |
| **Archers look like villagers** | Bare villager model + no bow layer | `VillageArcherRenderer` with `ItemInHandLayer` + `BOW_AND_ARROW` arm pose | Bow held in hand, arrows equipped in offhand. |
| **No bow drawing animation** | `RangedAttackGoal` never called `startUsingItem` | Replaced with `VillageArcherBowAttackGoal` invoking `startUsingItem` | Visible bowstring draw, aiming stance, and arrow release. |
| **Shields don't block** | No `ArmPose.BLOCK` handling | `getArmPose` returns `BLOCK` when `isBlocking()` is true | Shield raised across chest to deflect incoming attacks. |
