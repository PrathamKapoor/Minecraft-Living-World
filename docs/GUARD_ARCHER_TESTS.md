# GUARD_ARCHER_TESTS.md — Village Archer (Bow Guard) Implementation & Combat Tests

## Overview
This document specifies the implementation, tactical AI behaviors, equipment standards, and test results for the **Village Archer** (`livingworld:village_archer`), introduced to enforce ranged perimeter defense for autonomous settlements, ramparts, and fortified watchtowers.

---

## 1. Unit Specification & Combat Attributes

| Property | Value | Notes |
|---|---|---|
| **Entity ID** | `livingworld:village_archer` | Distinct `EntityType<VillageArcherEntity>` extending `PathfinderMob` & implementing `RangedAttackMob`. |
| **Category** | `CREATURE` | Non-hostile to villagers, iron golems, and players. Protected by `ProtectionManager`. |
| **Max Health** | **28.0 HP** | Standard defense guard vitality. |
| **Natural Armor** | **15.0 Armor** | Full Iron tier armor equivalent (Iron Helmet, Chestplate, Leggings, Boots). |
| **Armor Toughness** | **2.0** | Increased mitigation against heavy strikes. |
| **Movement Speed** | **0.28** | Agile tactical repositioning. |
| **Follow Range** | **30.0 blocks** | Extended perimeter detection range. |
| **Main Hand** | `Items.BOW` | Ranged bow weapon with pull progress animation. |
| **Off Hand** | `Items.ARROW` | Infinite quiver standard ammunition. |

---

## 2. Tactical Ranged AI & Behaviors

1. **Tactical Kiting AI (`TacticalKitingGoal`)**:
   - If an enemy closes within **5.0 blocks**, the archer automatically retreats backward or laterally at 1.25× speed while maintaining line-of-sight and aiming at the target.
   - Prevents archers from being swarmed by melee zombies and illagers.
2. **Elevated Wall & Tower Defense**:
   - Integrates with watchtower parapets (Y=14–15) and gatehouse battlements (Y=8).
   - Prioritizes high ground sniping positions with 24-block maximum projectile engagement range.
3. **Iron Golem & Villager Support Fire**:
   - Acquires hostile monsters attacking villagers, players, or friendly Iron Golems.
   - Provides suppressing fire to soften targets before melee contact.
4. **Promotion Pipeline (`VillageGuardManager.java`)**:
   - Enlisted directly from unemployed villagers to maintain a balanced garrison ratio: **~40% Archers, ~60% Melee Swordsmen**.
   - Gains combat experience from kills and survived battles:
     - **Recruit** -> **Soldier** -> **Veteran** -> **Elite Sharpshooter** (Diamond Helmet & Chestplate) -> **Royal Marksman** (Netherite Helmet & Chestplate).

---

## 3. Commands & Spawning
- **Creative Spawn Egg**: `livingworld:village_archer_spawn_egg` (Warm villager tan base `#93613E` with emerald fletching green spots `#27AE60`).
- **Debug Command**:
  - `/livingworld debug guard spawn archer` — Spawns a fully equipped Village Archer in front of the player.
  - `/livingworld debug guard spawn melee` — Spawns an iron-equipped melee swordsman guard.
  - `/livingworld debug guard recruit` — Enlists an unemployed village civilian into the military garrison.

---

## 4. Verification Evidence
- Automated test: `Test 42` in `LivingWorldTests.java` validates 28.0 HP, 15.0 Armor, 4.0 ATK, 0.28 Speed, 30.0 Follow Range.
- Enlistment and garrison ratio verified in `VillageGuardManager.java`.
- Live in-game client verification: Spawned via command and observed drawing bows and engaging hostile targets.
