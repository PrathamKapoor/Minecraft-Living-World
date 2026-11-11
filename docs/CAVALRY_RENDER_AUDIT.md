# Cavalry Render and Mounted Navigation Audit — v0.2.8

## 1. Floating Cavalry Root Cause & Resolution
In modern Minecraft 26.3, biped entities mounted on horses require an explicit riding offset in their `EntityType.Builder`:
```java
EntityType.Builder.of(VillageCavalryEntity::new, MobCategory.CREATURE)
    .sized(0.6f, 1.95f)
    .eyeHeight(1.74f)
    .ridingOffset(-0.7f)
```
Without `.ridingOffset(-0.7f)`, the rider's pelvis and legs rendered floating approximately 0.7 blocks above the horse saddle. In v0.2.8, all mounted humanoid units (`VILLAGE_CAVALRY`, `VILLAGE_GUARD`, `VILLAGE_ARCHER`, `ILLAGER_SWORDSMAN`, and `ILLAGER_SHIELDBEARER`) have been aligned with `.ridingOffset(-0.7f)` and `.eyeHeight(1.74f)`.

---

## 2. Mounted Navigation Steering
Vanilla Minecraft's `AbstractHorse` AI only permits `Player` entities to act as controlling passengers. Mob passengers normally have their movement ignored by the horse, causing the horse to wander or graze while the rider attacks.

In `VillageCavalryEntity.aiStep()`:
- When mounted, the rider actively transfers its combat target and navigation path to the mount:
  ```java
  if (this.isPassenger() && this.getVehicle() instanceof Horse horse) {
      LivingEntity combatTarget = this.getTarget();
      if (combatTarget != null && combatTarget.isAlive()) {
          horse.getNavigation().moveTo(combatTarget, 1.35);
          horse.getLookControl().setLookAt(combatTarget, 30.0f, 30.0f);
          this.getLookControl().setLookAt(combatTarget, 30.0f, 30.0f);
      }
  }
  ```
- **Dismount Invariant**: If the horse is incapacitated or dies in combat, `stopRiding()` cleanly unseats the cavalry unit, which immediately draws its sword and engages hostiles on foot as standard infantry without crashing or hanging navigation states.
