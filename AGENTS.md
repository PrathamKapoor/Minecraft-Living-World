# AGENTS.md — Autonomous Agent Operating Guidelines for Living World

## Primary Directives
1. **Act, Don't Just Plan**: Produce working, compilable code and playable `.jar` builds. Never output only proposed designs or placeholders.
2. **Prism Launcher Isolation**: Always operate on the dedicated instance `instances/LivingWorld/`. NEVER alter user instances `26.3`, `26.3-Modded`, or `Peak-1.16.5`.
3. **Vanilla Invariant**: NEVER override, recolor, upscale, or replace any vanilla texture or model. Existing textures must be sourced strictly by reference.
4. **Player & World Protection**: Protect player-built structures, chests, portals, and player-tamed/named animals from faction griefing, butchery, or zombification.
5. **Two-Scale Simulation**: Low-frequency strategic updates for unloaded settlements (population, trade, diplomacy); physical block-by-block building and tactical AI restricted strictly to loaded chunks near players.
6. **Journaling Protocol**:
   - Update `flow.md` CURRENT CHECKPOINT before and after every meaningful step.
   - Maintain chronological append-only journal in `flow.md`.
   - Log all consequential architecture, API, and balance decisions in `decisions.md`.
   - Update `docs/FEATURE_MATRIX.md` with true implementation status. Only label `TESTED` when verified in a running game world.
7. **Resume Protocol**: At session interruption or context limit, record in `flow.md`:
   - Exact UTC timestamp and milestone status.
   - Latest verified artifact path.
   - `NEXT ACTION:` with copy-pasteable command and target file.
   Upon resuming, read `AGENTS.md`, `flow.md`, `decisions.md`, and execute the `NEXT ACTION`.
