# GAP_BACKLOG.md — Living World Remediation Backlog

This document establishes the actionable, prioritized remediation tasks required to elevate Minecraft: Living World from a debug-command-reliant prototype to an authentic, fully functional Survival overhaul.

---

## Priority 1: Core Civilization Engine Activation (Unblocks F05, F06, F07, F08)

### GAP-01: Automatic Village & Settlement Auto-Discovery
- **Problem**: `SettlementManager` is only populated when an admin types `/livingworld create_village`. Naturally discovered vanilla villages never register as `Settlement` instances, completely disabling animal husbandry, autonomous construction, golem forgiveness, and undead occupation in normal Survival.
- **Remediation**:
  - Hook into `ServerEntityEvents.ENTITY_LOAD` when a `Villager` is loaded.
  - Check if `SettlementManager.get().getSettlementAt(villager.blockPosition())` is null.
  - If null, query surrounding area for village beds / bell POI / clustered villagers (>= 2 villagers).
  - Automatically call `getOrCreateSettlement()` with a dynamic regional name (e.g. "Suncrest Village", "Plains Hamlet") and register the settlement.
  - Add periodic ambient village check in `ServerTickEvents.END_LEVEL_TICK` near active players.
- **Target Files**: `src/main/java/livingworld/settlement/SettlementManager.java`, `src/main/java/livingworld/LivingWorldMod.java`.
- **Acceptance Criteria**: In a brand-new Survival world, entering a natural village immediately registers it in `SettlementManager`, enabling stockpile growth, golem trust, and construction without typing any command.

---

## Priority 2: Natural Survival Boss Encounters (F10, F14)

### GAP-02: Colossal Dreadbeast Natural Lunar & Siege Spawning (F10)
- **Problem**: `GiantZombieBoss` can only be summoned via `/livingworld summon_giant`.
- **Remediation**:
  - In `ZombieHordeManager.java`:
    - Add a natural boss spawn chance (15%) when a night horde spawns during a Full Moon (`level.getMoonPhase() == 0`).
    - Add natural boss spawn when an Occupied Undead Settlement reaches high threat level, appointing the Dreadbeast as its Warlord.
    - Provide a natural environmental summoning mechanism: burning a Cursed Totem (carved pumpkin surrounded by rotten flesh and bone blocks) at midnight.
- **Target Files**: `src/main/java/livingworld/undead/ZombieHordeManager.java`, `src/main/java/livingworld/bosses/GiantZombieBoss.java`.
- **Acceptance Criteria**: In Survival, player encounters the Colossal Dreadbeast during a Full Moon night horde or at an occupied village without admin intervention.

### GAP-03: Nether Flame Champion Natural Altar & Fortress Encounter (F14)
- **Problem**: `NetherBehemothBoss` is only spawned via `/livingworld summon_nether_boss`.
- **Remediation**:
  - In `NetherBehemothBoss.java`:
    - Add a natural ambient spawn chance (8%) near Nether Fortress crossroads / Bastion remnant treasures.
    - Add an environmental altar activation: using a Fire Charge or Wither Skeleton Skull on a Crying Obsidian block in the Nether triggers the Nether Flame Champion with boss bar, flame burst, and debris rewards.
- **Target Files**: `src/main/java/livingworld/bosses/NetherBehemothBoss.java`, `src/main/java/livingworld/LivingWorldMod.java`.
- **Acceptance Criteria**: In Survival, exploring a Nether Fortress or igniting a Crying Obsidian altar awakens the Nether Flame Champion naturally.

---

## Priority 3: Authentic Marine Predators & Aerial Threats (F12, F15)

### GAP-04: Genuine Oceanic Shark Ecology & Aquatic Navigation (F15)
- **Problem**: `SharkManager` merely iterated over vanilla Drowned mobs with custom names.
- **Remediation**:
  - Implement a dedicated `SharkManager` predator system:
    - Dedicated aquatic apex predator instances (using enhanced aquatic predator mobs with fast swimming speeds, dorsal fin visuals, underwater biting attacks).
    - Natural spawning in deep ocean biomes.
    - Active hunting of local cod/salmon.
    - "Blood in the water" tracking: circling players when wounded (< 50% HP) with audio/particle feedback and counterplay (distracted by raw meat / fish).
- **Target Files**: `src/main/java/livingworld/ocean/SharkManager.java`.
- **Acceptance Criteria**: In deep oceans, aquatic predators swim naturally, hunt fish, and circle bleeding players.

### GAP-05: High-Altitude Swooping Aerial Threats (F12)
- **Problem**: `AerialThreatManager` spawned standard Phantoms with a renamed tag.
- **Remediation**:
  - Equip aerial predators with distinctive swoop dive physics: accelerating on downward dive, inflicting knockback, retreating to high altitudes.
  - Natural spawning strictly above Y=120 in mountain and badland peaks.
  - Counterplay: ranged archery or shield blocking interrupts dive attack.
- **Target Files**: `src/main/java/livingworld/entity/AerialThreatManager.java`.
- **Acceptance Criteria**: Climbing mountain peaks triggers swooping aerial threats with tactical dive behaviors.

---

## Priority 4: Zombified Animals & Undead Domestic Economy (F09)

### GAP-06: Distinct Zombified Livestock & Dark Spoils (F09)
- **Problem**: Cows/sheep converted into baby vanilla zombies instead of authentic undead livestock.
- **Remediation**:
  - Implement true zombified animal behaviors:
    - Retain animal form with green rot particles / slowness effects, or zombified mounts.
    - When slain, drop rotten flesh, dark leather, and bone shards.
    - Undead settlements corral infected livestock for dark stockpiles.
- **Target Files**: `src/main/java/livingworld/undead/InfectionManager.java`.
- **Acceptance Criteria**: Zombies infect non-player cows/sheep into rot-carrying livestock that drop dark spoils.

---

## Priority 5: Exploration Overhaul (F16, F18)

### GAP-07: Abandoned Mineshaft Overhaul & Physical Explorer's Guide (F16)
- **Problem**: Zero lines of code modified mineshafts. Trial guides were only chat text.
- **Remediation**:
  - In `ExplorationManager.java`:
    - Add custom mineshaft rail loot rooms: when players enter mineshafts, bonus chests contain miner supplies, powered rails, and ancient trial fragments.
    - Provide a physical written book item ("Explorer's Field Journal: The Trial Vaults") obtained naturally from wandering traders or cartographer villagers.
- **Target Files**: `src/main/java/livingworld/exploration/ExplorationManager.java`.
- **Acceptance Criteria**: Wandering traders and mineshaft chests provide physical Explorer Guide journals and enhanced mining rewards.

### GAP-08: In-Game Usable Survey Map (F18)
- **Problem**: Global map was a headless coordinate set accessible only via chat commands.
- **Remediation**:
  - In `GlobalMapManager.java`:
    - Hook into player holding an Empty Map or Filled Map to trigger a Survey Map overview in action bar / boss bar / book view showing total explored chunks, current biome, active settlement directions, and saved waypoints.
    - Allow right-clicking with a Compass or Map to open/update survey waypoints.
- **Target Files**: `src/main/java/livingworld/exploration/GlobalMapManager.java`, `src/main/java/livingworld/LivingWorldMod.java`.
- **Acceptance Criteria**: Holding or using a map in Survival displays active survey telemetry, explored chunk metrics, and waypoint navigation.
