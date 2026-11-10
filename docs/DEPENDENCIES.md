# DEPENDENCIES.md — Minecraft: Living World Dependencies and Compatibility

| Dependency | Exact Version | License | Target MC | Loader | Role / Reason | Packaging Policy |
|:---|:---|:---:|:---:|:---:|:---|:---|
| **Minecraft Java Edition** | `26.3` | Proprietary (Mojang) | 26.3 | N/A | Base game runtime | Provided at runtime; never redistributed |
| **Fabric Loader** | `0.20.1` | Apache 2.0 | 26.3 | Fabric | Mod loading, mixin harness | Provided by launcher |
| **Fabric API** | `0.165.0+26.3` | Apache 2.0 | 26.3 | Fabric | Events, lifecycle, networking | Required mod dependency (`mods/fabric-api-...jar`) |
| **Sponge Mixin** | `0.17.5+mixin.0.8.7` | MIT | 26.3 | Fabric | Bytecode manipulation for mob goals/minecarts | Bundled with Fabric Loader |
| **Sodium (Optional)** | `0.9.2+mc26.3` | LGPL-3.0 | 26.3 | Fabric | Client rendering performance optimization | Optional drop-in mod |

### Asset Compliance
- All vanilla assets are referenced directly from the vanilla game index; no Mojang assets are copied, modified, or bundled into `livingworld.jar`.
- All newly added entities use clean, original pixel art and data definitions authored specifically for this mod.
