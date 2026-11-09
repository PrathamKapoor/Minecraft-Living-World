# ANIMATIONS.md — Living World v0.2.0 Visual Effects, Audio & Motion Design

## 1. Design Invariant
All animations, particle systems, and audio effects in Minecraft: Living World v0.2.0 utilize native vanilla particle types, entity animations, and sound events. Zero external proprietary animation engines or overriding resource packs are used, ensuring 100% compatibility with vanilla clients, shaders, and OptiFine/Iris environments.

---

## 2. Construction & Logistics Animations

### Visible Builder Hand-Swinging
- **Trigger**: Every 4 ticks during block placement in `ConstructionProject.step`.
- **Implementation**: `builder.swingForAttack(InteractionHand.MAIN_HAND)`.
- **Audio Feedback**: `level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F)`.
- **Particle System**: `new BlockParticleOption(ParticleTypes.BLOCK, state)` emitted at block center.

### Courier Item Collection
- **Trigger**: When an unemployed or apprentice villager walks within 2.0 blocks of a dropped crop or material.
- **Visuals**: Courier executes hand-swing gesture, item entity is consumed, and neutral `ITEM_PICKUP` pitch-shifted chime (`1.2f`) plays.

---

## 3. Zombie Siege Mining Cracks (Addendum Requirement 2)

### Progressive Block Fracture Stages (0–9)
- **Networking**: `serverLevel.destroyBlockProgress(zombie.getId(), miningBlockPos, crackStage)`.
- **Stage Progression**:
  $$\text{crackStage} = \min\left(9, \left\lfloor \frac{\text{miningTicks}}{\text{maxMiningTicks}} \times 10 \right\rfloor\right)$$
- **Impact Cadence**: Every 4 ticks, the miner swings their held tool (pickaxe, axe, or shovel) and plays `state.getSoundType().getHitSound()` with `HOSTILE` audio category.
- **Breach Cleanup**: When the block breaks, `destroyBlockProgress(zombie.getId(), miningBlockPos, -1)` clears the overlay before the block transforms into breaking particles and drops items.

---

## 4. Entity Combat & Boss Animations

### Multi-Headed Mutant Zombie Commander
- **Triple-Headed Visage**: 3 independently aligned zombie head cubes on a widened 12-pixel collar.
- **Rallying Roar**: Plays `ENTITY_WARDEN_ROAR` (pitch `0.75f`), emits a ring of `ANGRY_VILLAGER` and `SWEEP_ATTACK` particles, and grants Strength and Speed potion swirls to all zombies within 18 blocks.
- **Death Panic Scatter**: When defeated, a shockwave of `POOF` smoke particles erupts, and nearby horde minions lose their attack targets, scattering in random directions for 6 seconds.

### Colossal Dreadbeast Boss
- **Ground Slam**: When landing or executing a melee strike, triggers a shockwave of `EXPLOSION` particles and screen shake within 8 blocks.
- **Phase 2 Enrage Summon**: Erupts in purple dragon breath flares (`DRAGON_BREATH`) while summoning armored minion reinforcements.

### Nether Flame Champion
- **Flame Spiral**: Constantly emits a hovering helix of `FLAME` and `SMOKE` particles.
- **Fireball Barrage**: Replaces standard melee attacks with rapid multi-target blaze fireball bursts.

### Gargoyle Swooper (Aerial Threat)
- **High-Altitude Banking**: Patrols at Y=120+ in looping sine-wave circles.
- **Terminal Dive**: Accelerates to 1.8× speed with wings folded, emitting white wind trail particles (`CLOUD`) until striking or colliding.
- **Shield Stun Rebound**: Striking a raised player shield knocks the swooper backwards 6 blocks with an anvil clang sound (`ANVIL_LAND`).
