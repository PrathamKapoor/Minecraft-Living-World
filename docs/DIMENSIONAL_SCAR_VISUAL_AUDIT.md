# Dimensional Scar Visual and Biome Incursion Audit — v0.2.8

## 1. Overview
In version 0.2.8, the environmental generation of dimensional scars (`DimensionalScarGenerator`) was overhauled to resolve visual defects:
- Flat floating slabs of netherrack/nylium over open water and rivers.
- Monotonous bone arches in Soul Sand Valleys.
- Uniform, short tree canopies in Crimson and Warped forests.
- Unnatural sharp circular boundaries.

---

## 2. Redesigned Biome Incursions

### 2.1 Soul Sand Valley Incursion
- **Bone Fossil Variations**:
  1. *Curving Rib Cages*: Spanning 5 blocks across with inward-curving bone blocks.
  2. *Spine & Vertebrae Segments*: 4–6 block linear rows with vertical vertebrae protrusions.
  3. *Protruding Tusks*: 4–7 block tall curved bone spires.
  4. *Tilted Bone Fragments*: Diagonal 3–4 block bone segments embedded in the soil.
- **Surface Soil Noise Blending**: Natural noise blending between `SOUL_SAND`, `SOUL_SOIL`, and eroded `BASALT`.
- **Scattered Soul Fire**: Non-uniform placement strictly on soul blocks.

### 2.2 Crimson & Warped Forest Incursions
- **Variable Tree Heights**: Tree trunks now range dynamically from **5 to 11 blocks** tall.
- **Diverse Cap Shapes**:
  - Stepped conical caps with radius 3 at base, radius 2 at mid-level, radius 1 at top.
  - Drooping fungus tendrils on perimeter corners.
  - Embedded `SHROOMLIGHT` blocks on the underside for organic illumination.
- **Multi-Stage Gradient Boundary Transitions**:
  - Edge factor `0.75..1.0`: Coarse dirt, dirt, and light netherrack blending.
  - Edge factor `0.50..0.75`: Netherrack patches with early nylium sprouts.
  - Edge factor `< 0.50`: Dense nylium forest floor with massive fungus canopies.

### 2.3 Strict Shoreline & Ocean Fluid Safeguard
- `DimensionalScarGenerator` explicitly inspects surface fluid states.
- When an incursion intersects rivers or oceans, it **never places floating netherrack slabs on the water surface**.
- Instead, it traces down to the actual ocean bed / lake floor, replacing the submerged bed with soul sand or gravel and leaving the liquid water above completely intact and naturally flowing.

### 2.4 Reverse Incursions in the Nether
- Overworld enclaves inside the Nether feature fertile grass/dirt topsoil, variations of oak and spruce trees, and abandoned rustic cottages with deeply anchored cobblestone foundations.
