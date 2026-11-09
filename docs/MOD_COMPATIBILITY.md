# MOD_COMPATIBILITY.md — Compatibility Matrix & Dependency Resolution

**Target Engine:** Minecraft Java Edition 26.3 | Fabric Loader 0.20.1 | Java 25 LTS  
**Instance Path:** `instances/LivingWorld/`  

---

## 1. Verified Dependency & Runtime Compatibility Matrix

| Mod Name | Artifact Filename | Loader | MC Target | Verified Source | License | Integration Status | Conflict Risk |
|---|---|---|---|---|---|---|---|
| **Fabric API** | `fabric-api-0.165.0+26.3.jar` | Fabric | 26.3 | Cached / Local | Apache-2.0 | **ACTIVE** | None |
| **Living World Core** | `livingworld-0.2.0.jar` | Fabric | 26.3 | Built In-Tree | Living World Project | **ACTIVE (PRIMARY)** | Coordinates all simulation layers |
| **Mielon's The Sift** | `mielon's-the-sift-2.0.2-mc26.3-fabric.jar` | Fabric | 26.3 | Modrinth (`j0IfTxBq`) | MIT | **VERIFIED CANDIDATE** | None; Ancient City portal dimension |
| **GeckoLib** | `geckolib-fabric-26.3-5.5.7.jar` | Fabric | 26.3 | Modrinth (`8BmcQJ2H`) | MIT | **VERIFIED CANDIDATE** | None; Animation API |
| **Xaero's World Map** | `xaeroworldmap-fabric-26.3-1.47.2.jar` | Fabric | 26.3 | Modrinth (`NcUtCpym`) | ARR | **VERIFIED CANDIDATE** | None; Read-only map display |
| **Xaero's Minimap** | `xaerominimap-fabric-26.3-26.6.2.jar` | Fabric | 26.3 | Modrinth (`1bokaNcj`) | ARR | **VERIFIED CANDIDATE** | None; Minimap HUD |
| **Regrowth: Revamped** | `regrowth-revamped-fabric-1.1.0+26.3.jar` | Fabric | 26.3 | Modrinth (`mqlEubgZ`) | MIT | **VERIFIED CANDIDATE** | Managed; Wall placement gated by Living World |
| **Structory** | `Structory_26.3_v1.3.18+26.3.jar` | Fabric | 26.3 | Modrinth (`aKCwCJlY`) | Stardust Labs | **VERIFIED CANDIDATE** | None; Data-driven jigsaw worldgen |
| **Explorify** | `Explorify v1.6.6.mod.jar` | Fabric | 26.3 | Modrinth (`HSfsxuTo`) | ARR | **VERIFIED CANDIDATE** | None; Data-driven jigsaw structures |

---

## 2. Incompatible or Rejected Candidates

| Candidate Name | Attempted Version | Target Loader | Evaluation Outcome | Failure Root Cause & Action Taken |
|---|---|---|---|---|
| **MineColonies** | 1.20.4/1.21.1 Forge | NeoForge/Forge | **REJECT AS BINARY** | Incompatible loader and Minecraft version; Section 2.2 strict rule applies. Logistics patterns adapted into native `ConstructionManager`. |
| **Guard Villagers (NeoForge)** | 5.0.0-26.3.0 | NeoForge | **REJECT AS BINARY** | NeoForge dependency cannot load on Fabric Loader. Orchestrated via Living World `MilitaryManager` with native guard classes. |
| **More Villagers** | 1.20.1 Fabric | Fabric | **REJECT AS BINARY** | Outdated API; resource files All Rights Reserved. Native professions registered in Living World. |
| **Illager Invasion** | 1.20.1 Fabric | Fabric | **REJECT AS BINARY** | Outdated API; native custom Illager entities registered in Living World. |

---

## 3. Dependency Interoperability & Event Conflict Resolution

1. **Ancient City Portal & The Sift:**
   - The Sift awakens Ancient City portal structures using a Singer entity and note blocks. Living World's `ExplorationManager` does not alter Ancient City structure bounds, avoiding generation collisions.
2. **GeckoLib Entity Rendering:**
   - Living World utilizes standard vanilla-style entity renderers for base mobs and links GeckoLib for complex multi-bone entities (e.g. Multi-Headed Zombie Commander) without overriding vanilla entity render factories.
3. **Regrowth Wall Construction Coordination:**
   - Regrowth evaluates village perimeters and builds protective cobblestone/wood fences. Living World's `SettlementManager` reads the village perimeter and ensures blueprint plots for houses, barracks, and farms reserve clearances within the inner boundary.
4. **Xaero Waypoint Synchronization:**
   - Living World waypoints created via Shift+Right-Click in the Cartography Atlas or via discovered settlements are mirrored to Xaero's World Map session data where the API is bound.
