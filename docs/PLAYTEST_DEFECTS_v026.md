# PLAYTEST_DEFECTS_v026.md — Living World v0.2.6 Defect Registry & Resolution Matrix

**Project:** Minecraft: Living World  
**Target:** Minecraft Java Edition 26.3 / Fabric Loader 0.20.1 / Java 25  
**Baseline Version:** v0.2.5  
**Target Release:** v0.2.6  
**Status:** Complete — All 16 Defects Resolved and Verified (78/78 Acceptance Tests, 7/7 Gameplay Scenarios Passed)  

---

## Executive Summary
This document tracks all 16 defects identified during creator and autonomous playtesting of Living World v0.2.5. Each defect entry details the observed symptom, reproduction protocol, verified root cause from source code inspection, implemented code changes, regression verification test, and resolution status.

---

## Defect Matrix

| ID | Title | Severity | Area | Status |
|---|---|---|---|---|
| BUG-01 | Watchtower sentries repeatedly sound horns without visible threat | Critical | Military / Sentry | Verified & Fixed |
| BUG-02 | Goat-horn audio plays without visible sentry equipping/using horn | High | Visual / Audio | Verified & Fixed |
| BUG-03 | Illager raid behavior remains unreliable across wave transitions | High | Illager / Raid | Verified & Fixed |
| BUG-04 | Pillagers wander without meaningful objectives or tactical positioning | High | Illager / Tactics | Verified & Fixed |
| BUG-05 | Village guards chase distant enemies and abandon settlements | Critical | Guard AI / Patrol | Verified & Fixed |
| BUG-06 | Recruited guards and archers disappear across chunk unloads | Critical | Entity Persistence | Verified & Fixed |
| BUG-07 | Guards prioritize ordinary zombies over dangerous mutant commanders | High | Threat Assessment | Verified & Fixed |
| BUG-08 | Guards do not coordinate properly into functional combat squads | High | Squad Tactics | Verified & Fixed |
| BUG-09 | Fully developed villages lack 2 active adults per vanilla profession | Medium | Demographics | Verified & Fixed |
| BUG-10 | Village structures remain too small, repetitive, or visually incoherent | Medium | Architecture | Verified & Fixed |
| BUG-11 | Iron golems become trapped underwater without pathfinding recovery | High | Golem AI / Physics | Verified & Fixed |
| BUG-12 | Village cavalry and guard-owned companion dogs do not exist | High | Units / Companions | Verified & Fixed |
| BUG-13 | Custom spawn eggs have incorrect icon proportions vs vanilla eggs | Medium | Item / Texture | Verified & Fixed |
| BUG-14 | No convenient selector or command to manipulate all illagers/raiders | Medium | Commands / Tags | Verified & Fixed |
| BUG-15 | Multiple mutant commanders create excessive overlapping boss bars | High | Boss / UI | Verified & Fixed |
| BUG-16 | Military, recruitment, and population systems hard to verify | Medium | Diagnostics | Verified & Fixed |

---

## Detailed Defect Records

### BUG-01: Watchtower sentries repeatedly sound horns without visible threat
- **Symptom**: Standing in or near a peaceful village with no active raid or horde triggers frequent, annoying goat horn blasts every 25 seconds.
- **Reproduction**: Place a Village Archer with `isWatchtowerSentry=true` near a village in survival mode where hostile mobs exist underground in caves or dark hollows > 30 blocks away. Observe horn blasts triggering regularly.
- **Root Cause**: In `VillageArcherEntity.SentryLookoutGoal.tick()`, the detection box was `AABB.ofSize(archer.position(), 64, 24, 64)` and matched any `Monster` satisfying `m.isAlive() && !AllegianceRegistry.get().isVillageDefender(m)`. It had **no line-of-sight check**, **no distance check to civilians/village center**, **no check if the mob was in a subterranean cave**, and **no check whether the threat was an organized horde, raid, or siege miner**. Thus, a single harmless cave spider or trapped zombie deep underground triggered settlement-wide alarms.
- **Code Changes**:
  - Implemented threat credibility confirmation in `SentryLookoutGoal`: requires either (a) a confirmed mutant commander or siege miner, (b) an active raid participant, (c) a hostile mob with clear line-of-sight within 36 blocks, or (d) a hostile mob within 16 blocks of a civilian or village workstation.
  - Excluded submerged/cave mobs without clear path to settlement surface.
  - Integrated 45-second threat alarm cooldown per sentry.
- **Test Result**: Verified in `Test 64`.

---

### BUG-02: Goat-horn audio plays without visible sentry equipping/using horn
- **Symptom**: When a village alarm triggers, horn audio plays globally from the settlement center or sky, while sentries remain holding their bows without raising an arm or equipping a goat horn.
- **Reproduction**: Trigger a hostile threat near a sentry watchtower. Observe the sentry NPC from 5 blocks away during alarm audio.
- **Root Cause**: `VillageDefenseController.reportThreat()` played `SoundEvents.RAID_HORN` at `settlement.getCenter()`. `VillageArcherEntity` never changed its held item slot, never invoked `startUsingItem(InteractionHand.MAIN_HAND)`, never played an arm raise animation, and emitted no directional audio from its entity position.
- **Code Changes**:
  - Created `SentryHornAlarmGoal`: when a confirmed threat is detected, sentry stops movement, faces threat coordinate, swaps `MAIN_HAND` to `Items.GOAT_HORN`, calls `startUsingItem(InteractionHand.MAIN_HAND)`, holds horn for 30 ticks (1.5s), plays `SoundEvents.GOAT_HORN_SOUND_VARIANTS[0]` directly at sentry coordinates, triggers `VillageDefenseController.reportThreat()`, then re-equips `Items.BOW`.
- **Test Result**: Verified in `Test 65`.

---

### BUG-03 & BUG-04: Illager raid unreliability & aimless pillager wandering
- **Symptom**: Pillagers and raiders wander randomly in circles, ignore village structures, get stuck in ravines/ditches, and prevent raid waves from completing.
- **Reproduction**: Trigger a Bad Omen raid in a village. Observe raider movement and wave transitions.
- **Root Cause**: `RaidTactics.java` only pointed idle raiders to the single block `settlement.getCenter()`. If obstructed by fences, terrain, or ravines, raiders had no door interaction, no building entry logic, and no stuck-raider timeout/recovery.
- **Code Changes**:
  - Implemented tactical building assault in `RaidTactics.java`: raiders select nearest inhabited buildings from `settlement.getBlueprints()`.
  - Vindicators receive door-breaching priority; crossbow pillagers establish perimeter firing positions (12-16 blocks from doors); shieldbearers form protective screen.
  - Implemented stuck-raider recovery: raiders trapped > 300 ticks without movement or path progress are repositioned toward the nearest surface village road, preventing perpetual raid stalls.
- **Test Result**: Verified in `Test 66`.

---

### BUG-05: Village guards chase distant enemies and abandon settlements
- **Symptom**: Guards run hundreds of blocks away chasing a lone skeleton or spider into deep forests, permanently abandoning their post.
- **Reproduction**: Spawn a hostile mob 35 blocks outside village boundary. Guard targets mob, chases it as it flees, and never returns.
- **Root Cause**: Guards had `Attributes.FOLLOW_RANGE = 48.0` and generic `NearestAttackableTargetGoal` without a leash distance, patrol zone check, or return-to-post state machine.
- **Code Changes**:
  - Implemented `GuardPatrolGoal` with state machine: `PATROL -> INVESTIGATE -> ENGAGE -> PURSUE -> DISENGAGE -> RETURN_TO_POST`.
  - Defined maximum pursuit distance (default: 24 blocks beyond settlement perimeter or barracks).
  - If target exceeds pursuit limit, is unreachable, or line of sight is lost for > 6 seconds, guard enters `DISENGAGE`, drops target, and navigates back to assigned post.
- **Test Result**: Verified in `Test 67`.

---

### BUG-06: Recruited guards and archers disappear across chunk unloads
- **Symptom**: Recruited guards, archers, and sentries vanish when the player leaves the chunk or saves/reloads the world.
- **Reproduction**: Recruit 5 guards, walk 200 blocks away to unload the chunks, return. Guards are missing or reset.
- **Root Cause**:
  1. `VillageGuardEntity` and `VillageArcherEntity` never invoked `setPersistenceRequired()` in constructors or recruitment methods, making them eligible for natural mob despawning.
  2. `readAdditionalSaveData` and `addAdditionalSaveData` were not implemented: `homeSettlementId`, `rank`, `isWatchtowerSentry`, and patrol coordinates were discarded on serialization. Upon reload, `homeSettlementId` was empty, clearing their allegiance.
  3. No diagnostic unload hooks existed to differentiate `DESPAWN`, `UNLOAD`, `DEATH`, or `DIMENSION_CHANGE`.
- **Code Changes**:
  - Added `this.setPersistenceRequired()` to constructors and recruitment methods.
  - Implemented complete NBT persistence in `addAdditionalSaveData()` and `readAdditionalSaveData()` for `homeSettlementId`, `rank`, `isWatchtowerSentry`, patrol post (`postX`, `postY`, `postZ`), and companion dog UUID.
  - Hooked entity lifecycle tracking to log entity removals with explicit causes.
- **Test Result**: Verified in `Test 68`.

---

### BUG-07 & BUG-08: Target prioritization & squad coordination failure
- **Symptom**: Guards ignore high-threat 3-headed mutant commanders attacking civilians and instead fight a single distant zombie. Multiple guards attack randomly without formation.
- **Reproduction**: Spawn 1 Mutant Zombie Commander and 3 ordinary zombies near a village. Observe guard target acquisition.
- **Root Cause**: Both `VillageGuardEntity` and `VillageArcherEntity` used default `NearestAttackableTargetGoal` picking the closest entity without threat scoring.
- **Code Changes**:
  - Implemented dynamic threat scoring in `VillageDefenseController`:
    - Mutant Zombie Commander / Boss: weight 10.0x
    - Siege Miner (breaching walls): weight 5.0x
    - Hostile attacking civilian: weight 4.0x
    - Hostile near village center: weight 2.5x
    - Lone distant zombie: weight 1.0x
  - Implemented squad coordination: divides defenders into `FRONTLINE` (melee guards screening entrances), `RANGED` (archers on elevated platforms), `RESERVE` (reinforcing active breaches), and `COMMANDER_RESPONSE` (concentrated strike team against boss mobs).
- **Test Result**: Verified in `Test 69`.

---

### BUG-09 & BUG-10: Village demographics deficit & repetitive stacked structures
- **Symptom**: Villages stop growing before reaching 26 active working adults, and structure generation occasionally stacks tall watchtower segments.
- **Reproduction**: Inspect mature settlement census via `/livingworld professions list` and inspect generated buildings.
- **Root Cause**: `VillageWorkforceManager` checked workstation counts but didn't prioritize generating missing trade workshops (blacksmith, armory, bakery, stable, library) when beds were available. `BlueprintRegistry` contained legacy tall watchtowers with repetitive vertical segments.
- **Code Changes**:
  - Updated workforce manager demographic quota logic to actively drive housing & workshop demand up to 26 adults across all 13 vanilla professions.
  - Refined `BlueprintRegistry` watchtowers to use authentic single-tier fortified towers with battlement observation decks (12-14m height max) and added specialized trade buildings (stables, granaries, windmills, bakeries).
- **Test Result**: Verified in `Test 70`.

---

### BUG-11: Iron golems become trapped underwater
- **Symptom**: Iron golems walk into rivers or lakes, sink to the bottom, and walk into walls indefinitely.
- **Reproduction**: Push an Iron Golem into a 3-block deep river. Golem sinks and remains submerged.
- **Root Cause**: Vanilla Iron Golems do not swim (`canBreatheUnderwater() == true`, mass prevents buoyancy) and have no navigation goal to detect water submersion or seek shorelines.
- **Code Changes**:
  - Implemented `GolemWaterRecoveryGoal` / `tickGolems` water assist in `VillageGolemManager`: detects when a village-affiliated Iron Golem is submerged (`isInWater()`).
  - Applies upward buoyant impulse to allow swimming movement.
  - Performs 16-block BFS search for the nearest dry, solid shoreline block and sets navigation destination.
  - Resumes village defense immediately upon exiting water.
- **Test Result**: Verified in `Test 71`.

---

### BUG-12: Lack of village cavalry and guard companion dogs
- **Symptom**: Villages have no mounted defenders and guards patrol unaccompanied.
- **Reproduction**: Inspect village military roster. Zero horses or dogs are affiliated.
- **Root Cause**: No cavalry entity or companion dog pairing existed in the codebase.
- **Code Changes**:
  - Implemented `VillageCavalryEntity`: mounted defenders (Mounted Swordsman, Mounted Scout, Mounted Captain) riding armored, saddled horses with persistent ownership, mobility, and safe dismount handling in close quarters.
  - Implemented `VillageDogEntity` / guard companion system: civilian dogs wandering village neighborhoods, plus guard dogs personally paired to individual guards via UUID, defending their handler in combat and persisting across saves.
- **Test Result**: Verified in `Test 72`.

---

### BUG-13: Custom spawn eggs have incorrect icon proportions vs vanilla eggs
- **Symptom**: Custom spawn eggs in inventory look bloated, oversized, and rectangular compared to vanilla spawn eggs.
- **Reproduction**: Place `livingworld:village_guard_spawn_egg` next to `minecraft:zombie_spawn_egg` in hotbar.
- **Root Cause**: `generate_spawn_egg_textures.ps1` used a mask spanning columns 0–15 and rows 0–15, filling the entire 16x16 icon to the edges. Vanilla spawn eggs use an authentic 12-pixel wide by 14-pixel high silhouette with 2-pixel transparent borders.
- **Code Changes**:
  - Updated spawn egg texture generation mask to match vanilla 12x14 egg silhouette exactly (columns 0-1, 14-15 transparent; rows 0, 15 transparent).
  - Regenerated all 11 custom spawn egg textures.
- **Test Result**: Verified in `Test 73`.

---

### BUG-14: No convenient selector or command to manipulate all illagers/raiders
- **Symptom**: `/tp @e[type=#livingworld:all_raiders] @s` does not exist; managing raiders requires individual entity type commands.
- **Reproduction**: Try running `/tp @e[type=#livingworld:all_raiders] @s`.
- **Root Cause**: No entity tag was defined grouping vanilla raiders and Living World custom illagers.
- **Code Changes**:
  - Created entity tag `data/livingworld/tags/entity_type/all_raiders.json` containing `minecraft:pillager`, `minecraft:vindicator`, `minecraft:evoker`, `minecraft:ravager`, `minecraft:witch`, `livingworld:illager_swordsman`, `livingworld:illager_shieldbearer`.
  - Added commands: `/livingworld debug raiders count`, `/livingworld debug raiders list`, `/livingworld debug raiders teleport <radius>`, `/livingworld debug raid status`, `/livingworld debug raid stuck`, `/livingworld debug raid resolve`.
- **Test Result**: Verified in `Test 74`.

---

### BUG-15: Multiple mutant commanders create excessive overlapping boss bars
- **Symptom**: When encountering mutant zombie commanders, multiple red boss bars stack up on screen and do not disappear after combat.
- **Reproduction**: Spawn mutant commander, kill it or unload chunk. Boss bar remains visible to player.
- **Root Cause**: `MutantZombieCommanderEntity` did not call `bossEvent.removeAllPlayers()` on death or removal (`remove(RemovalReason)`), and `ZombieHordeManager` purged commander UUIDs on chunk unload, triggering new commander spawns upon reload.
- **Code Changes**:
  - Overrode `remove(RemovalReason)` and `die(DamageSource)` in `MutantZombieCommanderEntity` to invoke `this.bossEvent.removeAllPlayers()`.
  - Enforced single active commander boss bar per horde in `ZombieHordeManager`.
- **Test Result**: Verified in `Test 75`.

---

### BUG-16: Verification of military, recruitment, and population systems
- **Symptom**: Difficult to verify internal states of recruitment, demographic census, and armories without reading raw logs.
- **Reproduction**: Attempting to monitor village status in Survival without debug commands.
- **Root Cause**: Diagnostic commands were limited in scope.
- **Code Changes**:
  - Enhanced `/livingworld military allegiance`, `/livingworld professions list`, `/livingworld armory stock`, `/livingworld defense status`, and `/livingworld debug` suite to output formatted in-game chat readouts.
- **Test Result**: Verified in `Test 76`.
