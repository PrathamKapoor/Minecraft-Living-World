# TEST_PLAN.md — Minecraft: Living World Verification & Acceptance Plan

## 1. Safety and Invariant Tests

### Test 1.1: Vanilla Texture Invariant
- **Rule**: No vanilla textures or models may be overridden.
- **Verification**: Run `tar -tf build/libs/livingworld-0.1.0.jar | Select-String "assets/minecraft"`.
- **Expected Outcome**: Zero matches. All vanilla assets are referenced directly by identifier.

### Test 1.2: Player Build & Animal Protection
- **Rule**: Factions and destructive mob behaviors must never damage player structures or kill/convert player-tamed/named animals.
- **Procedure**:
  1. Tame a wolf/cat or name a cow with a name tag.
  2. Spawn an undead horde or zombie nearby.
  3. Verify that `ProtectionManager.isProtectedAnimal()` prevents damage and infection.
  4. Build a player shelter with a wooden door.
  5. Verify that `ZombieBreachGoal` respects player-placed blocks and skips breaking protected player doors.

### Test 1.3: Autonomous Construction & Blueprint Realization
- **Rule**: Builders physically place blocks block-by-block only in loaded chunks, respecting stockpiles.
- **Procedure**:
  1. Stand in a settlement and run `/livingworld build house` or `/livingworld build farm`.
  2. Observe physical block placement (up to 4 blocks per tick).
  3. Verify project completion updates housing capacity and persists across world save and reload.

---

## 2. Feature Acceptance Matrix Scenarios

| Scenario | Feature | In-Game Command / Procedure | Expected Result |
|:---|:---:|:---|:---|
| **Door Breaching** | F01 | Place a zombie facing an unprotected wooden door on Easy/Normal/Hard | Zombie plays strike animations with crack stages, breaks door in bounded time |
| **Skeleton Kiting** | F02 | Rush an armed skeleton on flat terrain | Skeleton retreats backwards maintaining 6+ blocks spacing; seeks high ground |
| **Creeper Breaching** | F03 | Hide behind weak non-player block with creeper aggroed | Creeper evaluates breach and ignites tactical explosion |
| **Village Construction** | F06 | `/livingworld build house` | Builder places blocks one by one from modular blueprint; capacity increases |
| **Livestock Husbandry** | F07 | Spawn 4 cows in a settlement pen with food stockpile | Cows breed under cap; surplus adult culled; drops enter stockpile |
| **Undead Occupation** | F08 | Allow 6 zombies to defeat a village | Village changes to `[Occupied]`; undead garrison commander spawns; liberating grants Hero trust |
| **Livestock Infection** | F09 | Zombie attacks non-player horse | Converts to vanilla `ZombieHorse`; zombie mounts horse as cavalry |
| **Zombie Horde & Boss** | F10 | `/livingworld spawn_horde` & `/livingworld summon_giant` | Armored commander buffs followers; Colossal Dreadbeast engages with ground slams & enrage |
| **Piglin Tribes** | F11 | Enter Nether across Z=0 meridian | Crimson Tusks and Warped Shadows skirmish over border territory |
| **Ocean Sharks** | F15 | Swim in deep ocean while below 50% health | Aquatic predators hunt local fish and circle injured swimmer |
| **Minecart Traversal** | F17 | Ride minecart over powered rails | Reaches 2.5x speed safely without derailment |
| **Global Map & Waypoints**| F18 | `/livingworld waypoint add Home` & `/livingworld reroll_enchant` | Waypoint saved; enchanting offering rerolled for 1 Lapis Lazuli |
