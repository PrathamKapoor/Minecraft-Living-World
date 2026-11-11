# Watchtower Visual and Architectural Audit — v0.2.8

## 1. Executive Summary
During prior gameplay evaluations, the user observed watchtowers functioning as cramped, decorative objects rather than militarily sound defense platforms. Archers were observed spawning inside roof slabs and hanging lanterns, unable to aim downwards, and constrained within 1-block vertical clearances.

In version 0.2.8 (**Operation: Make the World Actually Alive**), the entire watchtower structural and tactical AI architecture was overhauled.

---

## 2. Structural & Headroom Geometry Verification

### 2.1 `archer_watchtower` (7 × 13 × 7)
- **Ascent Backing Column**: Continuous solid cobblestone pillar from ground level up to `y=10` with attached wooden ladders.
- **Platform Elevation**: Solid oak plank battle floor at `y=10` (usable footprint 5×5 overall, 3×3 clear inner standing floor).
- **Parapets**: Oak fences along perimeter edges at `y=11` with clear firing ports in all 8 compass directions.
- **Standing Clearance & Headroom**:
  - `y=11`: 100% unobstructed air across the 3×3 inner floor.
  - `y=12`: 100% unobstructed air across the 3×3 inner floor (providing **2 full blocks of clear vertical headroom** above the standing floor).
- **Shelter Roof**: Oak slabs placed at `y=13`, providing full overhead weather protection while preserving open 360-degree outward sightlines.
- **Lighting Elevation**: The illumination lantern is hung from the ceiling slab at `(3, 13, 3)`. It does NOT rest on or obstruct the platform floor `y=10` or eye level `y=11..12`.

### 2.2 Fortified `watchtower` (9 × 16 × 9)
- Fortified stone brick corner bastions with central ladder well.
- Observation battlements at `y=14` spanning 7×7 clear battle floor.
- Obstructing decorative ground campfires and lanterns on the floor have been completely removed.
- Parapet lanterns are mounted on elevated stone brick posts at `y=16`, eliminating tripping or firing obstruction.

---

## 3. Automated Line-of-Sight & Ballistics Verification
- Automated line-of-sight validation in `SettlementManager.validateWatchtowerSightlines()` raycasts 8 outward vectors at sentry eye height (`y=1.74f`).
- Guard and archer target detection sectors span 48–64m outward without hitting parapets or decorative geometry.
- Watchtower archers have strafing and random strolling goals suppressed (`isWatchtowerSentry() == true`), ensuring they remain stationed firmly on the platform aiming cleanly downward at approaching threats.
