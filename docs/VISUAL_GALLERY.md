# VISUAL_GALLERY.md — Minecraft: Living World Visual Asset & Render Gallery

## Executive Overview
In Minecraft: Living World v0.2.1, visual quality is an absolute acceptance requirement. Simplistic placeholders, single-polygon models, flat stretched textures, and single-hut "empires" have been completely eradicated. This gallery provides the technical specifications, geometric structures, texture mappings, and animation curves for all custom visual assets.

---

## 1. Great White Shark (`livingworld:shark`)
- **Category:** Apex Marine Predator (F15)
- **Model Class:** [`livingworld.client.model.SharkModel`](file:///C:/Users/LENOVO/Downloads/PrismLauncher-Windows-MinGW-w64-Portable-11.0.3/src/main/java/livingworld/client/model/SharkModel.java)
- **Renderer:** [`livingworld.client.SharkRenderer`](file:///C:/Users/LENOVO/Downloads/PrismLauncher-Windows-MinGW-w64-Portable-11.0.3/src/main/java/livingworld/client/SharkRenderer.java)
- **Texture:** `assets/livingworld/textures/entity/shark/shark.png` (64×64 PNG)
- **Combat Stats:** 60.0 HP | 6.0 Natural Armor | 9.0 Attack Damage | 1.45 Swim Speed

### 3D Model Architecture & Anatomy
1. **Fuselage / Torso (`body`):** 8×8×16 box (`texOffs(0, 0)`). Streamlined torpedo fuselage featuring countershading: dark slate-gray dorsal spine (`#3B4450`), silver lateral flank gradient, and pale off-white ventral belly (`#E8ECEF`). Includes 5 distinct dark gill slits on each flank (`#1E242C`).
2. **Pointed Snout Head (`head`):** 3-stage tapered hydrodynamic rostrum:
   - Base cranial block: 7×6×7 (`texOffs(0, 24)`)
   - Tapered snout wedge: 5×4×4 (`texOffs(0, 37)`)
   - Sharp apex tip: 3×2×2 (`texOffs(18, 37)`)
   - Black predatory eyes (`#0F0F14`) with pale specular highlight (`#DCE6F5`) positioned laterally.
3. **Articulated Underside Jaw (`jaw`):** 6×2×6 box (`texOffs(28, 24)`). Underside sub-terminal mouth lined with crisp white serrated fangs (`#FFFFFF`) against a deep crimson oral cavity (`#5A1414`).
4. **Iconic Triangular Dorsal Fin (`dorsal_fin`):** 1×7×6 triangular fin (`texOffs(32, 0)`) standing tall on the upper dorsal midline with a swept trailing edge.
5. **Swept Pectoral Fins (`pectoral_fin_left`, `pectoral_fin_right`):** 7×1×5 hydrodynamic wing foils (`texOffs(40, 13)`), angled laterally at -20° roll and 20° yaw.
6. **Vertical Heterocercal Caudal Tail Fin (`tail`, `tail_fin`):** 5×5×8 caudal peduncle with authentic vertical shark tail fin:
   - Upper lobe: 1×8×6 (`texOffs(26, 32)`)
   - Lower lobe: 1×6×4 (`texOffs(40, 32)`)

### Dynamic Animation Curves
- **Tail Oscillation:** `tail.yRot = sin(walkPos * 0.5) * 0.45 rad` (authentic side-to-side thrust).
- **Secondary Caudal Whip:** `tailFin.yRot = sin(walkPos * 0.5 - 0.5) * 0.35 rad`.
- **Serpentine Body Sway:** `body.yRot = sin(walkPos * 0.5 - 1.0) * 0.10 rad`.
- **Articulated Jaw Bite Snap:** `jaw.xRot = |sin(walkPos * 0.4)| * 0.28 rad` (snapping jaw motion during lunges and feeding).

---

## 2. 3-Headed Mutant Zombie Commander (`livingworld:mutant_commander`)
- **Category:** Undead Horde Boss & Field Marshal (F10)
- **Model Class:** [`livingworld.client.model.MutantZombieCommanderModel`](file:///C:/Users/LENOVO/Downloads/PrismLauncher-Windows-MinGW-w64-Portable-11.0.3/src/main/java/livingworld/client/model/MutantZombieCommanderModel.java)
- **Renderer:** [`livingworld.client.MutantZombieCommanderRenderer`](file:///C:/Users/LENOVO/Downloads/PrismLauncher-Windows-MinGW-w64-Portable-11.0.3/src/main/java/livingworld/client/MutantZombieCommanderRenderer.java)
- **Texture:** `assets/livingworld/textures/entity/zombie/mutant_commander.png` (64×64 PNG)
- **Combat Stats:** 80.0 HP | 6.0 Armor | 8.0 Attack Damage | Boss Event Bar (Red, Notched 10)

### 3D Model Architecture & Anatomy
1. **Dominant Center Head (`head_center`):** 8×8×8 mutated cranial block (`texOffs(0, 0)`). Decayed olive-green skin (`#4A6B3D`), exposed cranial bone fracture (`#DCD7C3`), piercing glowing crimson eyes (`#E62828`) with yellow sulfur irises (`#FFC81E`), and snarling necrotic fangs. Tracks player look pitch and yaw dynamically.
2. **Left Secondary Mutated Head (`head_left`):** 7×7×7 mutated head (`texOffs(32, 0)`) fused to the left collar, angled at +23° yaw and +6° pitch with independent idle gaze sway.
3. **Right Secondary Mutated Head (`head_right`):** 7×7×7 mirrored mutated head (`texOffs(32, 0)`) fused to the right collar, angled at -23° yaw and +6° pitch.
4. **Hulking Broad Torso (`body`):** 12×14×6 massive chest (`texOffs(16, 16)`). Tattered dark commander's tunic with royal purple accents, rusted iron pauldrons and gorget (`#8C9196`, `#824B2D`), and exposed mutated ribcage.
5. **Heavy Spiked Arms (`left_arm`, `right_arm`):** 5×14×5 bulging muscular limbs (`texOffs(40, 16)`), reaching menacingly forward in classic zombie attack posture.
6. **Massive Mutated Legs (`left_leg`, `right_leg`):** 5×14×5 reinforced legs (`texOffs(0, 16)`) with iron greaves and dark boots.

### Tactical AI & VFX
- **Rallying Roar:** Every 8 seconds, emits an audible shockwave roar (`ZOMBIE_VILLAGER_CONVERTED`), sends `ANGRY_VILLAGER` particles, and bestows Speed and Strength aura buffs to all horde followers within 18 blocks.
- **Death Scatter Panic:** Upon commander defeat, nearby followers receive Weakness and Slowness for 15 seconds, lose their attack targets, and scatter into the wilderness.

---

## 3. Atomic Zombie Hordes (8–16 Verified Entities)
- **Manager:** [`livingworld.undead.ZombieHordeManager`](file:///C:/Users/LENOVO/Downloads/PrismLauncher-Windows-MinGW-w64-Portable-11.0.3/src/main/java/livingworld/undead/ZombieHordeManager.java)
- **Encounter Design:** No more 1-zombie "hordes". Night hordes spawn atomically with 8 to 16 members on verified dry land:
  - **1× Leader:** 3-Headed Mutant Zombie Commander (80 HP, Boss Bar).
  - **2–3× Armored Soldiers:** Iron helmets, chainmail chestplates, iron swords.
  - **2–3× Zombie Siege Miners:** Equipped with iron pickaxes/axes/shovels, tagged `livingworld:tool_miner`, executing `ZombieSiegeMiningGoal` block breach AI.
  - **4–8× Regular Zombies:** Cohesive flanking swarm.
- **Dry Land Safety:** Automatically queries heightmap and fluid state. If fewer than 8 valid dry land positions exist around the player, the encounter cleanly aborts without false chat announcements.

---

## 4. Fortified Imperial Capitals & Multi-Building Layouts
- **Manager:** [`livingworld.settlement.EmpireManager`](file:///C:/Users/LENOVO/Downloads/PrismLauncher-Windows-MinGW-w64-Portable-11.0.3/src/main/java/livingworld/settlement/EmpireManager.java)
- **Civilization Footprint:** Radius expanded to 110 blocks; population scaled to 24+; housing capacity to 64+.
- **Multi-Building Architectural Layout:**
  1. **Imperial Royal Palace (`palace`):** 11×8×9 grand stone brick palace with throne room, polished granite floor, vaulted columns, red banners, and royal lanterns.
  2. **Fortified Gatehouse (`gatehouse`):** 9×7×5 stone brick gatehouse with iron portcullis archway and flanking crenellated watchturrets.
  3. **Imperial Barracks (`barracks`):** 7×6×7 garrison hall equipped with bunk quarters, weapon racks, and training dummies.
  4. **Royal Bazaar Market (`market`):** 7×6×7 colorful striped wool market plaza with merchant stalls.
  5. **Watchtower (`watchtower`):** 5×9×5 defensive bastion with arrow slits and stone-brick parapets.
- **Standing Imperial Army:**
  - **Monarchs:** King Reginald II & Queen Seraphina in the royal palace.
  - **Royal Captain:** Valerius in enchanted diamond gear.
  - **2× Royal Sentinels:** Theron and Garrick in full diamond gear with shields.
  - **4× Imperial Heavy Infantry:** Julian, Cassian, Marcus, Tiberius in iron armor guarding gates and perimeters.

---

## 5. Ecosystem & Structure Integrations
1. **ChoiceTheorem's Overhauled Village (CTOV 4.2.2a):** Massive, breathtaking medieval town generation replacing vanilla village layouts with multi-tiered hill towns, castles, fortified ports, and specialized trade quarters.
2. **Lithostitched (2.0.6):** High-performance world generation stitcher ensuring structure bounding boxes blend seamlessly into surrounding biomes.
3. **Moog's End Structures (2.1.1):** End tower citadels, void monoliths, and chorus spires scattered across outer End islands.
4. **Moog's Nether Structures (3.1.1):** Nether fortresses, piglin bastions, and magma citadels with expanded traversal pathways.
5. **Better Vanilla Dragon Fight (1.0.1):** Crystal cage fortifications, strafe projectile waves, and perching shockwaves while strictly preserving 100% of vanilla End ambience and sound design.
