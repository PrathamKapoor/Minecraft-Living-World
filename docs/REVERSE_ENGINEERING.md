# REVERSE_ENGINEERING.md — Living World Toolchain & Bytecode Analysis

## 1. Toolchain Overview & Verification

Version 0.2.4 mandates a **Source-First, Research-First, Integration-First** engineering methodology. Rather than guessing Minecraft internals or creating simplistic placeholder imitations, Living World directly analyzes real compiled bytecode, mod JARs, and Minecraft client internals using industry-standard reverse-engineering and decompilation tools.

### Integrated Toolchain Components

| Tool | Version | Path / Invocation | Purpose & Capabilities |
| :--- | :--- | :--- | :--- |
| **REA (Reverse Engineering Agent)** | 6.7.0 | `npx -y rea-agents@6.7.0` | Comprehensive RE automation tool suite with 207 MCP tools, disassembly, decompilation, binary triage, and cross-reference indexing. Configured in local MCP environment. |
| **CFR Decompiler** | 0.152 | `tools/cfr.jar` | Modern Java bytecode decompiler capable of reconstructing Java 21-25 language features (records, sealed interfaces, pattern matching, switch expressions). |
| **Vineflower** | 1.11.1 | `tools/vineflower.jar` | High-accuracy Fernflower fork optimized for modern Minecraft and Fabric mod bytecode decompilation. |
| **JDK 25 javap** | 25.0.2 | `C:\Program Files\Java\jdk-25.0.2\bin\javap.exe` | Bytecode disassembler, method signature inspector, and constant pool analyzer. |
| **JDK 25 jar / jdeps**| 25.0.2 | `C:\Program Files\Java\jdk-25.0.2\bin\jar.exe` | Archive inspection, class file extraction, and modular dependency graph analysis. |

### Verification Record

1. **REA Setup & Health Check**:
   ```pwsh
   npx --yes rea-agents setup --yes
   npx --yes rea-agents doctor
   ```
   - **Result**: `installed_tool_count: 207`, `state: aligned`, `doctor: healthy: true`, local MCP server aligned and verified.
2. **CFR Decompiler Verification**:
   ```pwsh
   & "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -jar tools/cfr.jar --version
   ```
   - **Result**: CFR version 0.152 confirmed operational under Java 25.0.2 runtime.
3. **Vineflower Decompiler Verification**:
   ```pwsh
   & "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -jar tools/vineflower.jar --version
   ```
   - **Result**: Vineflower 1.11.1 confirmed operational.

---

## 2. Bytecode Analysis: Minecraft 26.3 Humanoid Rendering Pipeline

To understand why village guards and archers previously rendered without armor, swords, or bows, we decompiled and analyzed the core rendering classes from the official Minecraft 26.3 client runtime (`libraries/com/mojang/minecraft/26.3/minecraft-26.3-client.jar`).

### 2.1 The 26.3 RenderState Architectural Paradigm

In Minecraft 26.3, Mojang refactored entity rendering into a strict two-phase pipeline to decouple entity simulation ticks from render thread execution:

1. **Phase 1: Render State Extraction (`extractRenderState`)**:
   - Executes on the client game thread.
   - Extracts all visual parameters (coordinates, rotation, animations, equipment, held items, glowing/tint states) from the live `Entity` into a lightweight, data-only `EntityRenderState` subclass.
2. **Phase 2: Render Submission (`submit` / `render`)**:
   - Executes asynchronously on the graphics pipeline.
   - Operates strictly on the extracted `RenderState` object. It **never** reads from the `Entity` instance.
   - Any property not copied into the `RenderState` is completely invisible during rendering.

### 2.2 `HumanoidMobRenderer` Decompilation Findings

Decompilation of `net.minecraft.client.renderer.entity.HumanoidMobRenderer` revealed the fundamental structure:

```java
public abstract class HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>>
extends AgeableMobRenderer<T, S, M> {
    public HumanoidMobRenderer(EntityRendererProvider.Context context, M model, M babyModel, float shadow, CustomHeadLayer.Transforms customHeadTransforms) {
        super(context, model, babyModel, shadow);
        this.addLayer(new CustomHeadLayer(this, context.getModelSet(), context.getPlayerSkinRenderCache(), customHeadTransforms));
        this.addLayer(new WingsLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer(this)); // <--- ItemInHandLayer is automatically included!
    }

    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
        state.leftArmPose = this.getArmPose(entity, HumanoidArm.LEFT);
        state.rightArmPose = this.getArmPose(entity, HumanoidArm.RIGHT);
    }
}
```

**Key Discoveries**:
1. `HumanoidMobRenderer` automatically attaches `ItemInHandLayer`. Any entity renderer inheriting from `HumanoidMobRenderer` immediately gains support for main hand and offhand held items (swords, shields, bows, tools).
2. `HumanoidMobRenderer.extractHumanoidRenderState` automatically populates:
   - `headEquipment`, `chestEquipment`, `legsEquipment`, `feetEquipment` (from `entity.getItemBySlot(...)`).
   - `rightHandItemStack`, `rightHandItemState`, `leftHandItemStack`, `leftHandItemState`.
   - `isUsingItem`, `ticksUsingItem`, `useItemHand`.
   - `swingAnimation`, `currentSwing`.
   - `isCrouching`, `isFallFlying`, `isVisuallySwimming`.

### 2.3 `HumanoidArmorLayer` Decompilation Findings

Decompilation of `net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer` revealed how 3D armor is rendered:

```java
public class HumanoidArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>>
extends RenderLayer<S, M> {
    private final ArmorModelSet<A> modelSet;
    private final ArmorModelSet<A> babyModelSet;
    private final EquipmentLayerRenderer equipmentRenderer;

    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        this.renderArmorPiece(poseStack, submitNodeCollector, state.chestEquipment, EquipmentSlot.CHEST, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, state.legsEquipment, EquipmentSlot.LEGS, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, state.feetEquipment, EquipmentSlot.FEET, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, state.headEquipment, EquipmentSlot.HEAD, lightCoords, state);
    }
}
```

**Key Discoveries**:
1. `HumanoidArmorLayer` iterates over all four equipment slots (`CHEST`, `LEGS`, `FEET`, `HEAD`).
2. For each slot, it inspects `state.<slot>Equipment`. If the item possesses the `DataComponents.EQUIPPABLE` component and matches the slot, it retrieves the corresponding armor model from `modelSet.get(slot)` and renders the layer through `equipmentRenderer.renderLayers(...)`.
3. If an entity renderer does not add `HumanoidArmorLayer`, armor items in the entity's inventory will **never** be rendered, regardless of what items are equipped on the server.

### 2.4 `HumanoidModel.ArmPose` and Animation Mechanics

Disassembly of `net.minecraft.client.model.HumanoidModel$ArmPose` and `HumanoidModel` revealed the animation triggers:

| `ArmPose` Enum | Visual Behavior | Trigger Condition |
| :--- | :--- | :--- |
| `EMPTY` | Arm hangs naturally at side; swings while walking. | Hand is empty. |
| `ITEM` | Arm holds item forward at ~30° angle. | Holding sword, tool, torch, or non-blocking shield. |
| `BLOCK` | Arm raises shield across body/chest facing incoming direction (`poseBlockingArm`). | Guard is actively blocking with shield (`isBlocking() == true`). |
| `BOW_AND_ARROW` | Dual-arm draw pose; main arm raises bow forward, off arm pulls string back (`animateCrossbowHold` / bow tension curve). | Archer aiming bow at target or actively pulling (`isUsingItem()`). |
| `SPEAR` | Arm thrusts forward along attack trajectory. | Trident / spear attack animation. |

---

## 3. Comparative Architecture: Established Mod Techniques

### 3.1 Guard Villagers by TallestEgg
- **Model Design**: Custom bipedal model extending `HumanoidModel` with a villager head (3D nose, large brow) and humanoid limbs (`rightArm`, `leftArm`, `rightLeg`, `leftLeg`).
- **Armor Handling**: Uses `HumanoidArmorLayer` with `ModelLayers.PLAYER_ARMOR` or custom armor layer mappings. Helmets render over the villager head geometry cleanly.
- **Weapon Handling**: Overrides `getArmPose` to return `BLOCK` during shield defense and `BOW_AND_ARROW` / `CROSSBOW_HOLD` for archer variants.

### 3.2 ChoiceTheorem's Overhauled Village (CTOV)
- **Structure Architecture**: Uses data-driven Jigsaw templates with terrain-adaptive foundation columns (`minecraft:gravity` and downward stone pillars) to prevent structures floating above uneven terrain.

### 3.3 Mutant Monsters by Fuzss
- **Layer Stacking**: Implements custom models with multi-pass renderers, combining custom base models with vanilla equipment layers for weapons and boss glow overlays.

---

## 4. Reverse-Engineering Cheatsheet for Living World

```pwsh
# 1. Decompile any class from a mod JAR using CFR:
& "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -jar tools/cfr.jar <jar-path> <fully-qualified-class-name>

# 2. Decompile with Vineflower to an output directory:
& "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -jar tools/vineflower.jar <jar-path> <output-dir>

# 3. Disassemble methods and signatures with javap:
& "C:\Program Files\Java\jdk-25.0.2\bin\javap.exe" -p -c <class-file-or-archive>

# 4. Search class members in Minecraft client jar:
& "C:\Program Files\Java\jdk-25.0.2\bin\jar.exe" tf libraries/com/mojang/minecraft/26.3/minecraft-26.3-client.jar | Select-String "<ClassName>"
```
