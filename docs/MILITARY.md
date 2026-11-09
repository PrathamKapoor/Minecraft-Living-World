# MILITARY.md — Living World v0.2.0 Military Pipeline, Defense & Hostile Combat AI

## 1. Military Overview
In Living World v0.2.0, settlements are no longer defenseless clusters of passive pacifists dependent solely on lumbering Iron Golems. Settlements actively maintain, recruit, equip, and promote standing armies of **Village Guards**, while hostile factions (Illagers and Undead) employ coordinated tactical maneuvers, specialized combat roles, and siege mining.

---

## 2. Village Guard System (`VillageGuardEntity`)

### Base Attributes & Equipment Standards
- **Health**: 30.0 HP (15 Hearts)
- **Base Attack Damage**: 6.0 (Iron Sword strike)
- **Movement Speed**: 0.28
- **Armor Rating**: 15.0 (Full Iron Tier Equivalence)
- **Armor Toughness**: 2.0
- **Equipment Standard**: Iron Helmet, Iron Chestplate, Iron Leggings, Iron Boots, Iron Sword in Main Hand, Shield in Offhand.

### Guard Recruitment Pipeline (`VillageGuardManager`)
1. **Capacity Evaluation**: Settlements target 1 guard per 3 adult civilians (minimum 2 guards per village).
2. **Civilian Enlistment**: An able-bodied unemployed villager is selected.
3. **Ceremony**: The villager is transitioned into a `VillageGuardEntity` with full iron equipment, an iron armor equip sound (`ARMOR_EQUIP_IRON`), and celebratory enchanted particles (`ENCHANTED_HIT`).
4. **Preserved Identity**: The civilian's culturally generated name is retained (e.g. `[Guard Recruit] Rowan`).

### Rank Promotion & Experience System
Guards earn real in-world combat experience when defeating hostile mobs:

| Rank | Required Kills | Required Battles | Equipment Upgrades | Power Multiplier |
|---|---|---|---|---|
| **Recruit** | 0 | 0 | Full Iron Armor, Iron Sword, Shield | 1.0× |
| **Soldier** | 1 | 1 | Full Iron Armor, Shield | 1.2× |
| **Veteran** | 4 | 2 | Diamond Sword, Iron Armor, Shield | 1.4× |
| **Elite Protector** | 10 | 5 | Diamond Helmet & Chestplate, Diamond Sword, Shield | 1.7× |
| **Royal Sentinel** | 20 | 10 | Full Diamond Armor, Diamond Sword, Custom Shield | 2.0× |

Upon achieving promotion, a fanfare sound (`PLAYER_LEVELUP`) and green celebration particles emit from the guard, and their display name updates in real time.

---

## 3. Specialized Illager Units

| Entity | Health | Attack | Speed | Equipment | Tactical Behavior |
|---|---|---|---|---|---|
| `IllagerSwordsmanEntity` | 26 HP | 6.0 | 0.32 | Iron Sword, Chainmail Chestplate | Agile melee shocktrooper that conducts flanking sprints and circles around shield-bearing defenders. |
| `IllagerShieldbearerEntity` | 32 HP | 5.0 | 0.24 | Iron Axe, Offhand Shield | Defensive vanguard that actively raises its shield during combat, absorbing 75% of incoming physical damage and disabling player shields with axe strikes. |

---

## 4. Hostile Tactical Combat AI

### Skeleton Tactics Overhaul (`SkeletonTacticsGoal`)
Vanilla skeletons passively backpedal in straight lines. Living World skeletons employ multi-factor spatial candidate scoring:
- **High Ground Preference**: Candidates with higher Y elevation receive score bonuses (+3.0 per block height advantage).
- **Line of Sight**: Positions maintaining direct raycast visibility to player/villager receive priority.
- **Distance Brackets**: Optimal kiting distance is maintained between 8 and 14 blocks.
- **Mid-Air Evasive Jump Shot**: When engaged at close range (<5 blocks), skeletons execute a backward hop, rotate mid-air to face the target, and loose an evasive arrow before repositioning.

### Intelligent Zombie Siege Mining (`ZombieSiegeMiningGoal`)
- **Carrier Spawn Probability**: 5% of adult zombies spawn equipped with a mining tool (45% Pickaxe, 35% Axe, 20% Shovel).
- **Tool-to-Block Compatibility**:
  - Pickaxes break Cobblestone, Stone, Sandstone, Bricks, and Deepslate.
  - Axes break Wooden Planks, Logs, Wooden Doors, and Trapdoors.
  - Shovels break Dirt, Grass, Sand, Gravel, Mud, and Snow.
- **Progressive Cracks**: Sends authentic `destroyBlockProgress` (stages 0–9) packets with tool hit sounds and arm swings every 4 ticks.
- **Horde Gathering**: Minework broadcasts an alert to all zombies within 24 blocks, assembling a horde at the breach.
- **Breach Charge**: When the barrier breaks, the horde rushes through the opening with a +30% speed boost.
- **Safeguards**: Respects `mobGriefing` game rule and `ProtectionManager` (never breaks player-placed claimed blocks or unbreakable containers).
