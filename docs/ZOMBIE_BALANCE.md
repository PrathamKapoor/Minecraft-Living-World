# ZOMBIE_BALANCE.md — Undead Combat Balancing & Statistics

## Overview
This document specifies the revised health points, armor ratings, equipment tiers, and combat roles for all undead entities in Living World v0.2.2, fulfilling the combat rebalancing mandate.

---

## 1. Undead Hierarchy & Stat Balancing

| Zombie Archetype | Entity ID / Tag | Max Health | Armor Rating | Main Hand | Armor Worn | Special AI & Combat Role |
|---|---|---|---|---|---|---|
| **Ordinary Zombie** | `minecraft:zombie` | **24.0 HP** *(Target: 20–28)* | 2.0 | None / Random | None | Swarming infantry. Buffed by Commander aura (+Speed, +Strength). Disperses in panic when Commander falls. |
| **Tool Miner** | `livingworld:tool_miner` | **32.0 HP** *(Target: 28–36)* | **2.0** | Iron Pickaxe (45%), Iron Axe (35%), Iron Shovel (20%) | Leather Helmet | Siege obstacle destruction. Actively evaluates block hardness, emits destruction stages 0–9, cracks perimeter defenses. |
| **Armored Soldier** | `minecraft:zombie` | **40.0 HP** *(Target: 35–45)* | **4.0** (Gear: +15) | Iron Sword | Iron Helmet, Chainmail Chestplate | Heavy frontline shocktroopers. Flanks defenders and engages village guards. |
| **Mutant Commander** | `livingworld:mutant_commander` | **180.0 HP** *(Target: 160–220)* | **10.0** | None (Fists) | None (Mutated Hide) | Horde Supreme Leader. 3 heads, boss health bar, 12.0 attack damage, 0.75 knockback resistance, 1.45× boss scale, Rallying Roar aura. |

---

## 2. Statistical Comparison: v0.2.1 vs v0.2.2

| Parameter | v0.2.1 Alpha | v0.2.2 Balanced Build | Change Details |
|---|---|---|---|
| **Mutant Commander Health** | 80.0 HP | **180.0 HP** | **+125% increase**. Properly scaled as a true multi-phase horde boss. |
| **Mutant Commander Armor** | 6.0 | **10.0** | **+66% increase**. Enhanced mitigation against rapid player crits. |
| **Mutant Commander Attack** | 8.0 | **12.0** | **+50% increase**. Hits like a devastating mutant brute. |
| **Mutant Commander Knockback Resist** | 0.50 | **0.75** | Greatly reduces combat stunlock. |
| **Tool Miner Health** | 20.0 HP | **32.0 HP** | **+60% increase**. Survives long enough to conduct tactical breaches. |
| **Armored Soldier Health** | 20.0 HP | **40.0 HP** | **+100% increase**. Forms a durable vanguard protecting miners. |
| **Ordinary Zombie Health** | 20.0 HP | **24.0 HP** | **+20% increase**. Tougher swarming presence. |

---

## 3. Horde Mechanics & Spawning Rules (`ZombieHordeManager.java`)
1. **Atomic Spawning Invariant**:
   - Hordes spawn as cohesive 10–14 member units:
     - 1× Mutant Zombie Commander (180 HP, Leader)
     - 2–3× Armored Soldiers (40 HP each)
     - 2–3× Tool Miners (32 HP each)
     - 4–8× Regular Zombies (24 HP each)
   - If 8 dry land spawn positions cannot be found simultaneously, the horde spawn is cleanly aborted. Zero 1-zombie hordes.
2. **Commander Synergy & Aura**:
   - Nearby followers within 16 blocks receive Speed I and Strength I.
   - Followers synchronize attack targets with the Commander.
   - Upon Commander defeat, followers are afflicted with Slowness II and Weakness II and scatter in panic.

---

## 4. Verification Evidence
- Automated test: `Test 44` in `LivingWorldTests.java` validates:
  - Commander HP = 180.0 (within 160–220 range) and Armor = 10.0.
  - Ordinary Zombie target HP = 24.0 (within 20–28 range).
  - Tool Miner target HP = 32.0 (within 28–36 range).
  - Armored Soldier target HP = 40.0 (within 35–45 range).
- 45/45 automated acceptance tests passed.
