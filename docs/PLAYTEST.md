# PLAYTEST.md — Minecraft: Living World In-World Playtest Guide

## Setup Instructions
1. Open Prism Launcher.
2. Select the dedicated instance: **LivingWorld**.
3. Click **Launch** (or execute `.\prismlauncher.exe -l LivingWorld`).
4. Create a new singleplayer Survival world (Seed: `LIVING_WORLD_TEST`).

---

## 30-Minute Starter Gameplay Walkthrough

### Part 1: Finding & Supporting a Village (Minutes 0–10)
- Locate a village or use `/livingworld create_village "Suncrest"`.
- Check status: `/livingworld status` and `/livingworld villages`.
- Observe the resource stockpile (food, wood, stone).
- If night falls and zombies approach, defend the perimeter alongside the iron golem.
- If injured near village clerics, notice they grant regeneration aid to heroes.

### Part 2: Autonomous Construction (Minutes 10–18)
- Test village growth: `/livingworld build house` or `/livingworld build farm`.
- Watch the physical block-by-block construction in front of you.
- Confirm the new house increases the village's housing capacity.
- Save and reload the world (`Save and Quit to Title` -> Re-enter).
- Verify the constructed building and settlement stockpile persist perfectly!

### Part 3: The Undead Threat & Horde Encounter (Minutes 18–25)
- Encounter a natural night horde or test with `/livingworld spawn_horde`.
- Observe the armored Zombie Horde Commander with his Strength and Speed aura.
- Defeat the Commander: observe nearby followers gain Slowness and Weakness.
- Test the Colossal Dreadbeast boss encounter: `/livingworld summon_giant`.
- Dodge ground shockwaves, navigate Phase 2 enrage with summoned bodyguards, and claim the Netherite scrap and diamond rewards!

### Part 4: Exploration & Traversal (Minutes 25–30)
- Save a waypoint at your base: `/livingworld waypoint add BaseCamp`.
- Lay a powered rail line, ride a minecart, and experience 2.5x high-speed transit.
- Hold Lapis Lazuli and run `/livingworld reroll_enchant` to refresh your enchanting offering.
- Read `/livingworld trial_guide` to prepare for Trial Chamber vaults.

---

## v0.2.8 Stabilization & Quality Verification Guide

### 1. Watchtower Defensive Architecture & Sentries
- Run `/livingworld debug village towers` or visit an established settlement.
- Climb to the watchtower lookout platform at y=10.
- Verify 2 full blocks of clear standing headroom (y=11..12) under the oak slab roof (y=13).
- Confirm the lantern is suspended high from the ceiling with zero floor obstruction.
- Verify the sentry archer stands firmly at the firing platform, does not get trapped in fences or trapdoors, and maintains 48-64m outward sightlines.

### 2. Guard Orientation & Anti-Spinning AI
- Spawn or recruit guards: `/livingworld debug guard spawn melee` and `/livingworld debug guard spawn archer`.
- Spawn enemy targets: `/livingworld debug zombie miner spawn pickaxe`.
- Observe combat behavior: guards face their target smoothly, archers maintain orientation while aiming, and target hysteresis (30 ticks) prevents rapid 180° rotation or jitter.
- Run `/livingworld defense threats` to verify authoritative threat tracking and defender assignment.

### 3. Civilian Distress Signalling & Shelter Lifecycle
- Strike a hostile mob near a civilian or let a zombie approach.
- Villagers emit 1 distress signal per incident (300-tick deduplication) with panic audio (`VILLAGER_NO` / `VILLAGER_HURT`) and particles.
- Check `/livingworld defense distress`: verify the incident is logged and 1–2 closest defenders are dispatched to intercept the attacker.
- Run `/livingworld defense shelters`: observe civilians transitioning through `SEEKING_SHELTER -> SHELTERED` and closing doors behind them with anti-thrashing cooldowns.

### 4. Mounted Cavalry Riding Offset
- Run `/livingworld debug cavalry status` to locate or spawn mounted cavalry.
- Observe the rider: sits firmly inside the horse saddle with correct `-0.7f` offset (zero vertical floating above mount).
- Observe mounted combat: horse navigates toward rider's target, and rider dismounts seamlessly if the horse is incapacitated.

### 5. Village Dogs & Reputation
- Feed and breed adult village dogs: verify offspring puppies have `villageBound = false` and roam freely with the player.
- Defend a settlement against monsters: verify trust accumulates silently.
- Run `/livingworld reputation`: observe trust points and standing titles (`Recognized Visitor` at 25, `Trusted Ally` at 50, `Village Protector` at 100, `Honored Defender` at 200).

### 6. Dimensional Scars & Worldgen
- Locate a Soul Sand Valley scar: observe varied fossils (rib cages, vertebrae spines, protruding tusks) and noise-blended soul sand/soil.
- Locate Crimson or Warped forest scars: observe varied mushroom tree heights (5–11 blocks) with stepped drooping caps.
- Locate an aquatic/river scar boundary: verify zero flat stone slabs floating on water surfaces. Riverbeds and seabeds are converted smoothly underwater while water flows naturally.
