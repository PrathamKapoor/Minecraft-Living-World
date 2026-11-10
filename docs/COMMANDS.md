# COMMANDS.md — Living World v0.2.0 Command Reference

All Living World commands are registered under the root `/livingworld` command tree using standard Brigadier commands.

---

## 1. General & Survival Commands

| Command | Permission | Description |
|---|---|---|
| `/livingworld status` | All | Displays active mod status, target runtime, registered settlement count, player exploration chunks, and protection toggles. |
| `/livingworld config` | All | Displays the current live status of all 18 feature toggles. |
| `/livingworld help` | All | Prints an in-game directory of available commands. |
| `/livingworld map` | All | Opens the full-screen Cartography Atlas Book GUI with coordinates, local settlement telemetry, and 7x7 chunk radar. |
| `/livingworld trial_guide` | All | Gives the player a physical copy of the Explorer's Field Journal containing trial chamber clues and vault keys. |
| `/livingworld reroll_enchant` | All | Rerolls the player's enchanting table seed in exchange for 1 Lapis Lazuli. |

---

## 2. Settlement & Construction Commands

| Command | Permission | Description |
|---|---|---|
| `/livingworld villages` | All | Lists all known settlements, their factions, population, housing capacity, active projects, and stockpile resources. |
| `/livingworld blueprints` | All | Lists all 13 registered authentic architectural blueprints with dimensions and block counts. |
| `/livingworld create_village [name]` | OP (Level 2) | Founds an active settlement at the player's current coordinates. |
| `/livingworld place <blueprint>` | OP (Level 2) | Instantly places an authentic architectural building at the player's location with full ground surveying and downward foundation anchoring. |
| `/livingworld build <blueprint>` | OP (Level 2) | Manually queues a physical, block-by-block construction project for autonomous completion by village builders with prepared foundations. |

---

## 3. Cartography & Waypoint Commands

| Command | Permission | Description |
|---|---|---|
| `/livingworld waypoint add <name>` | All | Adds a named custom waypoint at the player's current coordinates. |
| `/livingworld waypoint list` | All | Lists all saved waypoints for the executing player with relative distances. |

---

## 4. Boss & Horde Encounter Triggers

| Command | Permission | Description |
|---|---|---|
| `/livingworld summon_giant` | OP (Level 2) | Summons the Colossal Dreadbeast boss with notched red boss bar and minion summons. |
| `/livingworld summon_nether_boss` | OP (Level 2) | Summons the Nether Flame Champion with explosive flame burst attacks. |
| `/livingworld spawn_horde` | OP (Level 2) | Summons a coordinated night horde led by an armored commander. |

---

## 5. Version 0.2 Debug & Verification Commands

### Tool-Wielding Zombies & Intelligent Siege Mining (Addendum 2)
| Command | Permission | Description |
|---|---|---|
| `/livingworld debug zombie miner spawn <pickaxe\|axe\|shovel>` | OP (Level 2) | Spawns a tool-wielding zombie miner targeting the player. |
| `/livingworld debug zombie miner inspect` | OP (Level 2) | Inspects all tool-bearing miners within 12 blocks, outputting held tool, block target, and AI state. |
| `/livingworld debug zombie mining toggle <true\|false>` | OP (Level 2) | Globally toggles zombie siege mining and barrier destruction. |
| `/livingworld debug zombie siege test` | OP (Level 2) | Builds a 3x2 cobblestone barrier and spawns an iron pickaxe miner to demonstrate progressive crack breaking and horde gathering. |
| `/livingworld debug zombie tool_spawn_rate [rate]` | OP (Level 2) | Gets or sets the natural tool-bearing zombie spawn rate (default: `0.05` / 5%). |

### Spawn Eggs & Mobs (Milestone 2 & Quality Rebuild)
| Command | Permission | Description |
|---|---|---|
| `/livingworld debug eggs list` | All | Lists all 11 registered custom mob spawn eggs with `/give` commands. |

### Military & Guard Pipeline (Milestone 4 & Quality Rebuild)
| Command | Permission | Description |
|---|---|---|
| `/livingworld debug guard recruit` | OP (Level 2) | Recruits an unemployed civilian into a military guard (60% melee swordsman, 40% bow archer). |
| `/livingworld debug guard spawn melee` | OP (Level 2) | Spawns a dedicated Village Melee Guard with full iron gear and shield. |
| `/livingworld debug guard spawn archer` | OP (Level 2) | Spawns a dedicated Village Archer (Bow Guard) with full iron armor, bow, and tactical kiting AI. |

### Villager Empires (Milestone 5)
| Command | Permission | Description |
|---|---|---|
| `/livingworld debug empire locate` | All | Locates the nearest deterministic 1-in-64 Villager Empire. |
| `/livingworld debug empire generate` | OP (Level 2) | Upgrades the current settlement into an Imperial Seat with Palace, King, Queen, and Royal Sentinels. |

### Civilizations, Economy & Demographics (Milestone 3)
| Command | Permission | Description |
|---|---|---|
| `/livingworld debug villager inspect` | All | Inspects the nearest villager within 8 blocks, displaying cultural name, title, profession, work contributions, and UUID. |
| `/livingworld debug economy inspect` | All | Displays the recent economic transaction ledger (courier pickups, material deposits, player donations). |

---

## 6. Version 0.2.3 Ecosystem Integration & Visual Catalog Commands

### Ecosystem & Mod Integration Inspection
| Command | Permission | Description |
|---|---|---|
| `/livingworld integrations list` | All | Lists all 18 tracked ecosystem mods (Mutant Monsters, Puzzles Lib, Forge Config API Port, GeckoLib, CTOV, Lithostitched, Moog's Structures, The Sift, Xaero Maps, Regrowth, Structory, Explorify, etc.), their loaded status, versions, and licenses. |
| `/livingworld integrations mutant_monsters status` | All | Displays detailed verification status of Mutant Monsters 26.3: version, official source, all registered mutant EntityTypes, Chemical X item, and coexistence architecture with Living World. |

### Mutant Creatures & Encounters
| Command | Permission | Description |
|---|---|---|
| `/livingworld debug mutants showcase` | OP (Level 2) | Spawns all 7 Mutant Monsters (Mutant Zombie, Mutant Skeleton, Mutant Creeper, Mutant Enderman, Mutant Snow Golem, Spider Pig, Creeper Minion) in an orderly line in front of the player for visual and animation inspection. |
| `/livingworld debug mutants encounter` | OP (Level 2) | Triggers an immediate tactical combat encounter against an ancient mutant monster (Mutant Zombie, Skeleton, Creeper, or Enderman) spawned on safe terrain with chat telegraph. |

### Horde Inspection & Entity Separation
| Command | Permission | Description |
|---|---|---|
| `/livingworld debug horde inspect` | All | Inspects active zombie hordes in the world: lists the 3-headed Horde Commander (`livingworld:mutant_commander`), position, health, follower count, and explicitly verifies registry separation from `mutantmonsters:mutant_zombie`. |

### Professional Architecture & Visual Catalog
| Command | Permission | Description |
|---|---|---|
| `/livingworld debug visual catalog` | All | Outputs the comprehensive visual catalog of hand-crafted architectural blueprints (Palace, Watchtower, Cottage, Barracks, Gatehouse, Market, Workshop, Barn, Warehouse) and custom 3D mob models (Mutant Commander, Shark, Gargoyle, Zombie Livestock, End Void Stalker, Village Archer). |

---

## 7. Version 0.2.5 Living Village & Autonomous Defense Commands

### Village Defense & Alarm Control
| Command | Permission | Description |
|---|---|---|
| `/livingworld defense status` | All | Displays current threat level (Peaceful, Suspicious, Alert, Under Attack, Recovery), lockdown status, active hostile count, primary threat, active sentries, and horn cooldown for the local settlement. |
| `/livingworld defense alarm <on\|off>` | OP (Level 2) | Manually triggers a settlement-wide tactical goat horn alarm (rallying defenders, waking sleeping soldiers, ordering civilian lockdown) or silences the alarm and clears active threats. |
| `/livingworld defense sentries` | All | Lists all active archery watchtower sentries, their coordinates, health, and perimeter sightline targets (48–64 blocks). |

### Military Allegiance & Recruitment
| Command | Permission | Description |
|---|---|---|
| `/livingworld military allegiance` | All | Displays registered village defenders count, active friendly-fire cancellation status (0 damage between allied guards, archers, golems, civilians), and the complete ally matrix. |
| `/livingworld military recruit <melee\|archer>` | OP (Level 2) | Force-recruits an authentic Village Guard or Village Archer for the local settlement, equipping them from Armory stock with armor, weapons, and visible enlistment ceremony. |

### Armory, Professions & Golems
| Command | Permission | Description |
|---|---|---|
| `/livingworld armory stock` | All | Displays physical armory inventory (iron/diamond swords, bows, arrows, shields, armor pieces, splash healing potions) and gear availability for new recruits. |
| `/livingworld professions list` | All | Displays the complete 13-profession demographic census (Farmer, Fisherman, Fletcher, Shepherd, Butcher, Leatherworker, Librarian, Cartographer, Cleric, Armorer, Weaponsmith, Toolsmith, Mason) with staffed/deficit statuses. |
| `/livingworld golems status` | All | Displays active Iron Golem and Snow Golem defense forces, calculated quotas, climate conditions, and health status for the settlement. |
