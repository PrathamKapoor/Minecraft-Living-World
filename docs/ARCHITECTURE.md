# Architecture Specification — Minecraft: Living World

## 1. System Overview

```mermaid
graph TD
    subgraph Core
        LWMod[LivingWorldMod] --> CoreEvents[Server Lifecycle & Ticks]
        LWMod --> Config[FeatureConfig]
        LWMod --> Protect[ProtectionManager]
    end

    subgraph WorldSimulation[Two-Scale Simulation]
        CoreEvents --> SimScheduler[SimulationScheduler]
        SimScheduler -->|Strategic: 100-1200 ticks| StratSim[Strategic Layer: Stocks, Diplomacy, Territory]
        SimScheduler -->|Tactical: 1-20 ticks| TacSim[Tactical Layer: Loaded NPC AI, Building, Combat]
    end

    subgraph Factions[Faction & Settlement Framework]
        StratSim --> FactionMgr[FactionManager]
        FactionMgr --> VillagerFaction[Villager Settlements]
        FactionMgr --> UndeadFaction[Undead Occupations & Hordes]
        FactionMgr --> IllagerFaction[Illager Outposts & Raids]
        FactionMgr --> PiglinFaction[Piglin Tribes]
    end

    subgraph SettlementSubsystems[Settlement Lifecycle & Economy]
        VillagerFaction --> Stockpile[Stockpile & Economy]
        VillagerFaction --> ConstrMgr[ConstructionManager: Blueprints & Physical Builders]
        VillagerFaction --> HerdMgr[Animal Husbandry & Herders/Butchers]
        VillagerFaction --> GuardMgr[Guard Patrols & Golem Trust]
    end

    subgraph CombatAI[Combat & Hostile Intelligence]
        TacSim --> SquadBlackboard[Squad Coordination Blackboard]
        SquadBlackboard --> ZombieAI[Zombies: Door Breach & Flanking]
        SquadBlackboard --> SkeletonAI[Skeletons: Kiting & High Ground]
        SquadBlackboard --> CreeperAI[Creepers: Cover Breaching]
        SquadBlackboard --> IllagerAI[Illagers: Melee + Ranged Synergy]
    end
```

## 2. Package Structure
```text
src/main/java/livingworld/
├── LivingWorldMod.java            # Mod entrypoint & registry initializers
├── config/FeatureConfig.java      # Configuration flags, rates, difficulty
├── core/
│   ├── ProtectionManager.java    # Player claims, base protection, pet immunity
│   ├── SimulationScheduler.java  # Staggered tick scheduler for strategic/tactical tasks
│   └── EventHooks.java           # Server lifecycle and entity event wiring
├── faction/
│   ├── Faction.java              # Faction identity, claims, reputation, relations
│   ├── FactionManager.java       # Global faction registry and persistence
│   └── FactionType.java          # VILLAGER, UNDEAD, ILLAGER, PIGLIN_TRIBE
├── settlement/
│   ├── Settlement.java           # Settlement state machine, boundary, stockpile
│   ├── SettlementManager.java    # Spatial index & lifecycle for settlements
│   ├── BlueprintRegistry.java    # Modular vanilla-block building blueprints
│   ├── ConstructionManager.java  # Physical loaded block placement queue
│   └── ReputationManager.java    # Player trust, trade discounts, golem forgiveness
├── economy/
│   ├── Stockpile.java            # Food, wood, stone, iron inventories
│   ├── HerderManager.java        # Livestock breeding and pasture maintenance
│   └── ButcherManager.java       # Sustainable adult harvest & resource processing
├── ai/
│   ├── SquadController.java      # Shared blackboard, squad leaders, rally points
│   ├── ZombieBreachGoal.java     # Door/trapdoor destruction on all difficulties
│   ├── SkeletonTacticsGoal.java  # Distance maintenance, kiting, high ground
│   ├── CreeperBreachGoal.java    # Targeted obstruction breaching
│   └── MeleeIllagerGoal.java     # Melee combat role for illagers
├── undead/
│   ├── UndeadOccupationManager.java # Capture, occupation state, zombie jobs
│   ├── ZombieHordeManager.java      # Capped horde spawns & armored commanders
│   └── InfectionManager.java        # Conversion of eligible livestock to zombie variants
├── bosses/
│   ├── GiantZombieBoss.java      # Multi-phase giant zombie encounter
│   └── DragonPhaseController.java# Ender dragon phase state machine & counterplay
├── ocean/
│   └── SharkEntity.java          # Ocean predator ecology, hunting, neutral-until-provoked
├── travel/
│   └── MinecartSpeedController.java # Safe high-speed minecarts & rail UX
└── exploration/
    ├── GlobalMapManager.java     # Explored chunks fog-of-war map & waypoints
    └── TrialChamberClues.java    # In-game hints & upgraded exploration loot
```

## 3. Data Persistence & Save Strategy
- Data stored per-world in `world/data/livingworld_settlements.dat` and `world/data/livingworld_factions.dat` using vanilla `SavedData` / NBT compound tags.
- Safe schema versioning allows seamless migration and graceful removal without corrupting saves.
