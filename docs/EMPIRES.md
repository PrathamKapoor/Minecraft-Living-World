# EMPIRES.md — Living World v0.2.0 Rare Villager Empires Specification

## 1. Overview
In standard Minecraft, all villages share an identical humble rural village aesthetic. In Living World v0.2.0, settlements have a deterministic **1-in-64** probability of generating as an **Imperial Capital** of a vast Villager Empire.

Imperial Capitals serve as political, commercial, and military hubs in Survival worlds, featuring sovereign monarchs, grand architectural monuments, standing elite royal sentinels, and vast resource stockpiles.

---

## 2. Deterministic Generation Logic

Empires are generated deterministically from the world seed and settlement origin coordinates using a 64-bit linear congruential generator (LCG) hash:

```java
public static boolean isEmpireCandidate(long seed, BlockPos center, int chance) {
    if (chance <= 1) return true;
    long hash = (seed ^ center.asLong()) * 6364136223846793005L + 1442695040888963407L;
    return (Math.abs(hash) % chance) == 0;
}
```

Because this calculation is 100% deterministic:
- The exact same villages will consistently become Empires across world reloads.
- No world seed corruption or duplicate generations can occur.
- Players can locate Empires naturally or via the `/livingworld debug empire locate` command.

---

## 3. Imperial Features & Royal Court

When an Imperial Capital is founded or discovered:

### A. Sovereign Monarchs (The Royal Court)
- **The Villager King**: Spawned at the settlement center with the royal title `§6King Reginald II` (or regional dynastic title). The King oversees state affairs and grants massive trade discounts to trusted heroes (+40 Reputation).
- **The Villager Queen**: Spawned beside the King as `§dQueen Seraphina`, managing internal settlement welfare, festival celebrations, and food distribution.

### B. Royal Sentinels (`VillageGuardEntity`)
- Stationed directly at the imperial throne room entrance and palace gates.
- Equipped with **Full Diamond Armor** (Helmet, Chestplate, Leggings, Boots), Diamond Swords, and reinforced shields.
- Power multiplier: `2.0×` (Rank: `Royal Sentinel`).

### C. The Imperial Palace (`palace`)
- Grand stone-brick architecture spanning **11×9×9** blocks (380 placed blocks).
- Elevated throne dais with red carpet runner, polished granite columns, gilded lantern chandeliers, and sovereign thrones.
- Automatically queued for physical block-by-block construction upon settlement promotion.

### D. Imperial Treasury Endowment
To jumpstart their empire, newly founded Imperial Capitals receive an initial treasury deposit:
- `+100 WOOD`
- `+80 STONE`
- `+100 FOOD`
- `+30 IRON`
- Housing capacity set to **28+** (accommodating up to 30 citizens and 8 guards).

---

## 4. In-Game Player Discovery

When a player enters an Imperial Capital for the first time, a server-wide fanfare sound (`UI_TOAST_CHALLENGE_COMPLETE`) plays, accompanied by a halo of Totem of Undying particles:

```
§6👑 [Living World] You have discovered Imperial Capital of Oakvale! (Imperial Dynasty Active)
```
