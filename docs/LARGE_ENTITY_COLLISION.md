# Large Entity Collision and Headroom Audit — v0.2.8

## 1. Problem Statement
In previous updates, large entities such as Iron Golems (width 1.4m, height 2.7m) and Mutant Zombie Commanders (width 1.2m, height 2.4m) were occasionally observed spawning inside building walls, under low residential ceilings (2-block height), or in narrow doorways, resulting in immediate suffocation damage or entities getting stuck in structures.

---

## 2. Engineering Solution: Unified Safe Spawn Engine (`SafeSpawnValidation`)

`SafeSpawnValidation` establishes multi-tiered clearance criteria:
1. **Solid Ground Footing**: Confirms the underlying block is fully solid and non-fluid (rejects lava, deep water, and air voids).
2. **Hitbox Bounding Box Clearance**: Checks a radial box `[-radH..radH, 0..ceilH, -radH..radH]` around the proposed position. All blocks within the bounding volume must be non-solid and non-fluid.
3. **Large Mob Headroom Validation**:
   - Mobs with `height >= 2.2f` or `width >= 1.0f` require at least **1.0 block of clear vertical headroom** above their maximum standing height.
   - Enclosed indoor spaces require open sky or a minimum interior ceiling height of at least `ceilH + 2` blocks (e.g., >= 5 blocks clear height for an Iron Golem).
4. **Spiral Search Fallback**:
   - Searches outward in a spiral up to a designated radius (typically 8–14 blocks).
   - If no valid location is found that meets the clearance criteria, spawning fails gracefully rather than placing an entity into a wall.

---

## 3. Integration Call Sites
- `VillageGolemManager.tickSettlementGolems()`: Golems validate a 14-block perimeter around settlement center before spawning.
- `ZombieHordeManager.spawnHordeNear()`: Mutant Commander validates an 8-block clearance around the horde epicenter before spawning.
