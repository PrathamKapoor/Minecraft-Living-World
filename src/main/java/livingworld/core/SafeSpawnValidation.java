package livingworld.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Unified Safe-Spawn and Collision Validation System (Section 10, v0.2.8 Stabilization).
 * Eliminates large entity clipping, room suffocation, and invalid placement for custom mobs,
 * iron golems, mutant commanders, and mounted cavalry units:
 * - Hitbox width, height, and vertical headroom checks.
 * - Solid ground confirmation (dry, stable footing; no lava, deep water, or void).
 * - Multi-block clearance validation (rejects cramped 1-block spaces and low residential ceilings).
 * - Graceful fallback spiral search within safe perimeters.
 */
public class SafeSpawnValidation {

    /**
     * Evaluates whether the specified position provides safe footing and sufficient clearance
     * for the given EntityType's full bounding box and vertical height.
     */
    public static boolean isValidSpawnLocation(ServerLevel level, EntityType<?> type, BlockPos pos) {
        if (level == null || type == null || pos == null) return false;
        if (!level.isLoaded(pos)) return false;

        EntityDimensions dims = type.getDimensions();
        float width = dims.width();
        float height = dims.height();

        // 1. Solid Ground Footing Check
        BlockPos groundPos = pos.below();
        if (!level.isLoaded(groundPos)) return false;
        BlockState groundState = level.getBlockState(groundPos);
        if (!groundState.isSolid() || !level.getFluidState(groundPos).isEmpty()) {
            return false; // Not solid or submerged
        }

        // 2. Full Bounding Box Clearance Check
        int radH = (int) Math.ceil(width / 2.0f);
        int ceilH = (int) Math.ceil(height);

        for (int dx = -radH; dx <= radH; dx++) {
            for (int dz = -radH; dz <= radH; dz++) {
                for (int dy = 0; dy < ceilH; dy++) {
                    BlockPos probe = pos.offset(dx, dy, dz);
                    if (!level.isLoaded(probe)) return false;
                    BlockState probeState = level.getBlockState(probe);
                    // Bounding box must be free of solid blocks and damaging fluids
                    if (probeState.isSolid() || !level.getFluidState(probe).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        // 3. Headroom & Interior Clearance for Large Mobs (height >= 2.2 or width >= 1.0)
        boolean isLargeMob = height >= 2.2f || width >= 1.0f;
        if (isLargeMob) {
            // Large mobs require at least 1.0 block extra vertical headroom above head
            for (int dy = ceilH; dy <= ceilH + 1; dy++) {
                BlockPos overhead = pos.above(dy);
                if (!level.isLoaded(overhead)) return false;
                if (level.getBlockState(overhead).isSolid()) {
                    return false; // Cramped ceiling (e.g. small house room)
                }
            }

            // Reject placements inside small enclosed buildings (must have open sky or >= 4 block clear headroom)
            if (!level.canSeeSky(pos)) {
                int clearVertical = 0;
                for (int y = 0; y < 6; y++) {
                    BlockPos p = pos.above(y);
                    if (level.isLoaded(p) && !level.getBlockState(p).isSolid()) {
                        clearVertical++;
                    } else {
                        break;
                    }
                }
                if (clearVertical < ceilH + 2) {
                    return false; // Insufficient indoor clearance for large mob
                }
            }
        }

        return true;
    }

    /**
     * Searches for a valid safe spawn position within a search radius around the desired origin.
     * Returns a validated BlockPos, or null if no valid position exists (fails gracefully).
     */
    public static BlockPos findSafeSpawnPosition(ServerLevel level, EntityType<?> type, BlockPos origin, int searchRadius) {
        if (level == null || type == null || origin == null) return null;

        // Try exact origin first
        if (isValidSpawnLocation(level, type, origin)) {
            return origin;
        }

        // Try surface height at origin
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        BlockPos surfaceOrigin = new BlockPos(origin.getX(), surfaceY, origin.getZ());
        if (isValidSpawnLocation(level, type, surfaceOrigin)) {
            return surfaceOrigin;
        }

        // Spiral outward in search radius
        for (int r = 2; r <= searchRadius; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos cand = new BlockPos(x, y, z);
                    if (isValidSpawnLocation(level, type, cand)) {
                        return cand;
                    }
                }
            }
        }

        // Graceful failure: do not force spawn inside walls
        return null;
    }
}
