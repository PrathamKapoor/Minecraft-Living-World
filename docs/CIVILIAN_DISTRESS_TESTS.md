# Civilian Distress Signalling and Shelter Machine Audit — v0.2.8

## 1. Overview
In living medieval villages, non-combatant civilians must react realistically when assaulted by monsters rather than standing still or continuing their trade routines. Version 0.2.8 introduces:
1. Deduplicated physical civilian distress calls.
2. Immediate military responder dispatch.
3. 5-state autonomous civilian shelter and lockdown lifecycle.

---

## 2. Distress Signalling Lifecycle (`CivilianDistressManager`)

### 2.1 Deduplication & Physical Reaction
- Hooked to `ServerLivingEntityEvents.ALLOW_DAMAGE`: When a villager takes damage from any living attacker:
  1. A 300-tick (15-second) per-villager cooldown deduplicates signals to prevent notification spam during multi-hit assaults.
  2. The villager emits panic audio (`VILLAGER_NO` and `VILLAGER_HURT`), particles (`ANGRY_VILLAGER` and `SPLASH`), and faces the attacker.
  3. A high-priority distress incident is logged with the settlement defense controller with priority `18.0`.

### 2.2 Defender Dispatch
- Automatically searches for the 1–2 closest available defenders within a 64m response sphere:
  - First preference: Unengaged melee `VillageGuardEntity` units.
  - Second preference: Non-sentry `VillageArcherEntity` units.
  - Third preference: `IronGolem` defenders.
- Responders immediately drop idle strolls and sprint at 1.35x speed directly to the attacker.

---

## 3. Civilian Shelter Machine (`CivilianShelterManager`)

### 3.1 5-State Lifecycle
```
WORKING -> THREAT_DETECTED -> SEEKING_SHELTER -> SHELTERED -> RECOVERY -> WORKING
```
- **WORKING**: Villagers conduct daytime farm, market, and trade duties.
- **THREAT_DETECTED**: Horn sounds or distress received; cancels trading/farming and queries safe indoor structures.
- **SEEKING_SHELTER**: Pathfinds to interior coords of uncompromised buildings, explicitly routing away from threat vectors.
- **SHELTERED**: Inside building; triggers door closure.
- **RECOVERY**: Threat eliminated; waits 100 ticks before resuming normal duties.
- **Door Thrashing Safeguard**: Tracks door toggles with a 40-tick (2.0s) cooldown per door block.
- **Emergency Work Suspension**: Normal reproduction, work, and construction are halted while civilians are under threat.
