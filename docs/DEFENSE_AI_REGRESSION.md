# Defense AI Regression and Anti-Spinning Audit — v0.2.8

## 1. Root Cause Analysis: Guard & Archer Spinning

Prior playtests observed soldiers spinning uncontrollably or executing rapid 180-degree jitter during combat. Investigation revealed four distinct root causes:

1. **Tick-by-Tick Target Thrashing in VillageDefenseController**:
   `SettlementManager.tick()` invoked `VillageDefenseController.tickDefense()` every game tick (50ms). When multiple hostiles were in range, the controller forcefully sorted hostiles dynamically and reassigned each guard's target on every tick (`guard.setTarget(hostiles.get(i % size))`), causing target oscillation when hostiles moved on opposite sides.
2. **Ungated Idle Goals Concurrent with Combat**:
   `LookAtPlayerGoal` and `RandomLookAroundGoal` lacked target guards. During active combat, when an archer or guard aimed at a monster, nearby player proximity triggered `LookAtPlayerGoal`, fighting the combat target look controller and causing spinning head/body orientation.
3. **Missing Goal Flags on Tactical Kiting**:
   `TacticalKitingGoal` lacked goal flag declarations (`MOVE`, `LOOK`), allowing it to execute simultaneously with `VillageArcherBowAttackGoal`.
4. **Watchtower Sentry Mobility Interference**:
   Sentries on elevated 3x3 platforms were attempting to stroll and strafe, constantly colliding with parapet fences and recalculating navigation paths every tick.

---

## 2. Engineering Corrections Applied

1. **Target Selection Hysteresis & 30-Tick Cooldown**:
   In `VillageGuardEntity.setTarget()` and `VillageArcherEntity.setTarget()`, an assigned target cannot be superseded by a lower-priority target within 30 ticks (1.5 seconds) unless the current target dies, goes out of range, or an emergency civilian distress call overrides it.
2. **Defense Evaluation Rate Limiting (20-Tick Windows)**:
   In `VillageDefenseController.tickDefense()`, threat re-evaluation is throttled to once every 20 ticks (1.0s). If an assigned defender already holds a living, valid target in proximity, that assignment is strictly preserved.
3. **Strict Idle Goal Gating**:
   `WaterAvoidingRandomStrollGoal`, `LookAtPlayerGoal`, and `RandomLookAroundGoal` are now explicitly gated with:
   `canUse() { return this.getTarget() == null && this.getPatrolState() == PatrolState.PATROL; }`
4. **Watchtower Sentry Mobility Lock**:
   When `isWatchtowerSentry()` is true, kiting and strafing are suppressed; sentries anchor their position at `(3, 10, 2)` and aim smoothly at threats.
5. **Telemetry & Diagnostic Verification**:
   Added `targetSwitchCount` and `lastOrientationReason` tracking fields, accessible via `/livingworld debug defense threats`.
