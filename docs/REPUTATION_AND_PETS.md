# Reputation, Defense Rewards, and Village Pets Audit — v0.2.8

## 1. Village Defense Reputation Rewards (`ReputationManager`)

### 1.1 Silent Progression & Milestone Announcements
In v0.2.8, players gain village trust organically by defending settlements against monsters:
- **Base Hostile Monster Kill**: `+1 Trust`
- **Armored Zombie / Tool Miner Kill**: `+2 Trust`
- **Active Raider (Pillager/Vindicator/Ravager)**: `+3 Trust`
- **Saving a Villager Under Attack**: `+5 Trust`
- **Mutant Zombie Commander Defeat**: `+10 Trust`

### 1.2 Anti-Farming & Proximity Constraints
- Player must be within the settlement defense boundary (`radius + 24m`).
- Kills are rate-limited with a 15-tick anti-farming cooldown.
- Individual point gains are silent (no chat spam).
- Quiet, dignified title announcements and `PLAYER_LEVELUP` audio trigger **only upon crossing milestone thresholds**:
  - `25 Trust`: **Recognized Visitor**
  - `50 Trust`: **Trusted Friend**
  - `100 Trust`: **Village Ally**
  - `200 Trust`: **Honored Defender**

---

## 2. Village Dog Ownership, Taming, and Territory (`VillageDogEntity`)

### 2.1 Adult Village Dogs
- Spawned with `villageBound = true` by default.
- If an adult dog is led outside settlement boundaries, it pauses and navigates back to the village center.
- The player receives a one-time gentle chat notice: *"This village dog is tied to its community territory."*

### 2.2 Guard Companion Loyalty
- Guard companion dogs tethered to military handlers cannot be tamed by players while their handler lives.
- Interacting with the dog produces: *"This guard dog is loyal to its military handler."*
- If the handler dies in battle, the companion transitions to a civilian status and may be adopted.

### 2.3 Free-Roaming Puppies
- In `VillageDogEntity.getBreedOffspring()`, puppies born from village dogs inherit `villageBound = false`.
- Puppies are completely free-roaming, can be tamed normally with bones, and will travel anywhere with the player without territorial restrictions.
