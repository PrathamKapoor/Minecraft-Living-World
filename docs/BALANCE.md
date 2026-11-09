# BALANCE.md — Minecraft: Living World Systems Balance & Tuning Reference

This document formalizes the mathematical models, spawn rates, resource costs, combat scaling, and tuning constants for the Living World overhaul.

---

## 1. Economy & Autonomous Construction (F06, F07)

### Stockpile Resource Metrics
Villages track 6 physical resource types:
- `FOOD`: Wheat, carrots, potatoes, bread, cooked meats.
- `WOOD`: Logs and planks.
- `STONE`: Cobblestone, stone bricks, deepslate.
- `IRON`: Iron ingots, nuggets.
- `WOOL`: Wool blocks from sheep shearing.
- `LEATHER`: Cow drops from sustainable culling.

### Blueprint Construction Costs
| Structure | Wood Cost | Stone Cost | Wool/Iron Cost | Dimensions (W x H x D) | Benefit |
|:---|:---:|:---:|:---:|:---:|:---|
| **Small Oak Cottage** | 20 | 12 | 0 | 5 x 4 x 5 | +2 Villager Housing Capacity |
| **Irrigated Wheat Farm** | 12 | 0 | 0 | 7 x 1 x 7 | +8 Food per strategic cycle |
| **Village Watchtower** | 16 | 28 | 0 | 5 x 9 x 5 | +1 Iron Golem spawn cap; 32-block early detection |
| **Livestock Pen** | 24 | 0 | 0 | 7 x 2 x 7 | Enables husbandry; holds up to 12 animals |

### Construction Rate
- `maxConstructionBlocksPerTick`: 4 blocks/tick maximum (80 blocks/sec).
- Physical construction occurs only in loaded chunks.

### Animal Husbandry Parameters (F07)
- **Breeding Requirement**: Herd size >= 2, stockpile food >= 2 per pair.
- **Sustainable Culling Threshold**: Herd size >= 4 adults.
- **Breeding Stock Reserve**: Culling stops when remaining herd reaches 3 adults to prevent extinction.
- **Harvest Yield per Cull**:
  - Cow: +3 Food, +1 Leather
  - Pig: +4 Food
  - Sheep: +2 Food, +1 Wool
  - Chicken: +1 Food

---

## 2. Combat Intelligence & Squad Tactics (F01 - F04)

### Zombie Door Breaching (F01)
- **Hardness Progression**: 240 ticks (12.0 seconds of continuous battering).
- **Damage Stages**: 10 distinct crack visuals (Stage 0 to Stage 9 via `ServerLevel.destroyBlockProgress`).
- **Protection Check**: Checks `ProtectionManager.isProtectedBlock()`. Player placed doors and doors inside player claimed perimeters are completely immune.

### Skeleton Tactical Retreat (F02)
- **Retreat Trigger Distance**: When target player is within 6.0 blocks.
- **Retreat Speed**: 1.25x base movement speed, moving backwards away from target.
- **High Ground Seeking**: When terrain permits, skeleton prioritizes shooting positions at `Y >= player.Y + 2.0`.

### Creeper Obstacle Breaching (F03)
- **Target Memory**: Remembers last known player target position for up to 10 seconds through obstacles.
- **Breach Decision**: If target is behind destructible non-player blocks (e.g. dirt, cobblestone, wooden barrier) and path is blocked for > 40 ticks, creeper ignites tactical breach explosion.
- **Protection Check**: Never breaches player-placed blocks or player containers.

### Illager Squad Formation (F04)
- **Melee Shocktroopers (Vindicators)**: Advance in vanguard with iron axes, prioritizing player shield breaking (`axe.disableShield`).
- **Ranged Support (Pillagers)**: Maintain 12–16 block perimeter, strafing and shooting crossbow volleys.

---

## 3. Boss Encounters & Threat Scaling (F10, F14)

### Colossal Dreadbeast (Giant Zombie Boss) (F10)
- **Max Health**: 200.0 HP (100 Hearts).
- **Base Attack Damage**: 14.0 HP (7 Hearts unarmored).
- **Movement Speed**: 0.28 (comparable to player sprint).
- **Ground Slam Attack**:
  - Cooldown: 100 ticks (5 seconds).
  - Radius: 7.0 blocks.
  - Damage: 8.0 physical damage + vertical launch (`Y += 0.45`).
- **Phase 2 Enrage (Health <= 50% / 100 HP)**:
  - Movement Speed increases to 0.35.
  - Summons 3 armored Zombie Minions with iron swords.
  - Gains passive Strength I.
- **Deterministic Loot Drop**:
  - 1x Netherite Scrap (100%).
  - 2-4x Diamonds.
  - 8-16x Rotten Flesh.

### Nether Flame Champion (Wither Skeleton Boss) (F14)
- **Max Health**: 250.0 HP (125 Hearts).
- **Base Attack Damage**: 16.0 HP (8 Hearts unarmored).
- **Movement Speed**: 0.32.
- **Equipment**: Netherite Helmet + Netherite Sword.
- **Flame Burst Attack**:
  - Cooldown: 80 ticks (4 seconds).
  - Radius: 10.0 blocks.
  - Effect: Ignites all hostile targets within radius for 5 seconds + 6.0 fire damage.
- **Deterministic Loot Drop**:
  - 1x Ancient Debris (100%).
  - 1x Wither Skeleton Skull (100%).
  - 2-3x Netherite Scraps.

---

## 4. Traversals, Maps & Enchanting (F17, F18)

### Minecart High-Speed Transit (F17)
- **Powered Rail Speed Multiplier**: 2.5x base minecart velocity (reaches up to 20 m/s from vanilla 8 m/s).
- **Curvature Damping**: Automatically decelerates to 1.0x on rail turns to prevent derailment physics glitch.

### Lapis Lazuli Enchanting Reroll (F18)
- **Cost**: 1 Lapis Lazuli item consumed per seed reroll.
- **Effect**: Changes player's internal enchanting seed without requiring dummy enchanting of cheap items.
