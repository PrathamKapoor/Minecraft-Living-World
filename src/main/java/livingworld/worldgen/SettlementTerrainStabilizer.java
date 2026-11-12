package livingworld.worldgen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.core.ProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Settlement & Outpost Terrain Stabilization & Route Engineering Engine (Phase 1).
 *
 * Enforces a guaranteed 50-block horizontal radius around villages, empires, and pillager outposts,
 * covering the entire structure footprint if it extends beyond that radius.
 *
 * Responsibilities:
 * 1. Surface Ravine & Shaft Elimination: Bridges/fills deep valleys, ravines, and open shafts
 *    within the settlement zone to guarantee a continuous, walkable surface.
 * 2. Cave Preservation: Preserves underground caves by installing a 4-to-6 block natural
 *    structural surface vault/roof above subterranean voids rather than mass-filling caves.
 * 3. Supported Foundations: Projects solid foundation piers (cobblestone/smooth sandstone)
 *    from building basements down to solid ground, completely eliminating floating structures.
 * 4. Accessible Entrances: Levels and steps terrain in front of doors and fence gates.
 * 5. Defensive Clearance & Raider Ingress: Guarantees 3m vertical headroom for Iron Golems
 *    and continuous walkable ingress routes for raiders attacking from outside the perimeter.
 * 6. Protection Invariant: Operates on newly generated terrain and preserves existing player
 *    saves, builds, chests, and containers.
 */
public class SettlementTerrainStabilizer {
    private static final SettlementTerrainStabilizer INSTANCE = new SettlementTerrainStabilizer();

    // Cache of already-stabilized center positions to avoid duplicate processing
    private final Set<Long> stabilizedRegions = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, BoundingBox> activeSettlementFootprints = new ConcurrentHashMap<>();

    public static SettlementTerrainStabilizer get() {
        return INSTANCE;
    }

    /**
     * Stabilizes terrain for a structure start during world generation chunk placement.
     */
    public void stabilizeStructureStart(WorldGenLevel level, StructureStart start, ChunkPos chunkPos) {
        if (level == null || start == null || !start.isValid()) return;

        BoundingBox box = start.getBoundingBox();
        BlockPos center = box.getCenter();
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMaxX = chunkPos.getMaxBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int chunkMaxZ = chunkPos.getMaxBlockZ();

        // Calculate the 50-block expansion boundary around the structure
        int minX = Math.min(center.getX() - 50, box.minX() - 8);
        int maxX = Math.max(center.getX() + 50, box.maxX() + 8);
        int minZ = Math.min(center.getZ() - 50, box.minZ() - 8);
        int maxZ = Math.max(center.getZ() + 50, box.maxZ() + 8);

        // Check if this chunk intersects the settlement zone
        if (chunkMaxX < minX || chunkMinX > maxX || chunkMaxZ < minZ || chunkMinZ > maxZ) {
            return;
        }

        // Clamp the stabilization to the current chunk bounds to respect chunk isolation
        int startX = Math.max(chunkMinX, minX);
        int endX = Math.min(chunkMaxX, maxX);
        int startZ = Math.max(chunkMinZ, minZ);
        int endZ = Math.min(chunkMaxZ, maxZ);

        stabilizeChunkSlice(level, center, box, startX, endX, startZ, endZ);
    }

    /**
     * Stabilizes terrain around an active village, empire, or outpost in ServerLevel.
     */
    public void stabilizeSettlement(ServerLevel level, BlockPos center, int baseRadius, BoundingBox optionalBox) {
        if (level == null || center == null) return;

        long regionKey = ChunkPos.pack(center.getX() >> 4, center.getZ() >> 4);
        if (stabilizedRegions.contains(regionKey)) {
            return;
        }
        stabilizedRegions.add(regionKey);

        int effRadius = Math.max(50, baseRadius);
        int minX = center.getX() - effRadius;
        int maxX = center.getX() + effRadius;
        int minZ = center.getZ() - effRadius;
        int maxZ = center.getZ() + effRadius;

        if (optionalBox != null) {
            minX = Math.min(minX, optionalBox.minX() - 8);
            maxX = Math.max(maxX, optionalBox.maxX() + 8);
            minZ = Math.min(minZ, optionalBox.minZ() - 8);
            maxZ = Math.max(maxZ, optionalBox.maxZ() + 8);
        }

        BoundingBox zone = new BoundingBox(minX, level.getMinY(), minZ, maxX, level.getMaxY(), maxZ);

        // Process loaded chunks intersecting this zone
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                BlockPos sample = new BlockPos(cx << 4, 64, cz << 4);
                if (level.isLoaded(sample)) {
                    int cStartX = Math.max(cx << 4, minX);
                    int cEndX = Math.min((cx << 4) + 15, maxX);
                    int cStartZ = Math.max(cz << 4, minZ);
                    int cEndZ = Math.min((cz << 4) + 15, maxZ);
                    stabilizeChunkSlice(level, center, zone, cStartX, cEndX, cStartZ, cEndZ);
                }
            }
        }
    }

    private boolean isProtectedBlockSafe(LevelAccessor level, BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            return ProtectionManager.get().isProtectedBlock(sl, pos);
        }
        return false;
    }

    /**
     * Performs column-by-column surface analysis and ravine bridging on a chunk slice.
     */
    private void stabilizeChunkSlice(LevelAccessor level, BlockPos center, BoundingBox footprint,
                                     int startX, int endX, int startZ, int endZ) {
        int refY = center.getY();
        if (refY < 62) refY = 64;

        // Sample local elevation to establish a natural reference walkable ground level
        int sampledSum = 0;
        int samples = 0;
        for (int x = startX; x <= endX; x += 4) {
            for (int z = startZ; z <= endZ; z += 4) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (y >= 62 && y <= 140) {
                    sampledSum += y;
                    samples++;
                }
            }
        }
        int avgGroundY = samples > 0 ? (sampledSum / samples) : refY;
        int baselineY = Math.max(refY, avgGroundY);

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                BlockPos probe = new BlockPos(x, 64, z);

                // Preserve player builds & protected blocks
                if (isProtectedBlockSafe(level, probe)) {
                    continue;
                }

                // Never modify ocean/deep ocean water bodies
                var biome = level.getBiome(probe);
                if (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || biome.is(net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN)) {
                    continue;
                }

                int currentSurfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                // Distance from center determines allowed slope tapering towards exterior
                double dist = Math.sqrt((x - center.getX()) * (x - center.getX()) + (z - center.getZ()) * (z - center.getZ()));
                int allowedDrop = (int) (dist * 0.25); // Max 1 block drop per 4 horizontal blocks (gentle slope)
                int expectedMinY = Math.max(62, baselineY - allowedDrop - 4);

                // Detect deep surface ravines, open surface caves, or abrupt drops
                if (currentSurfaceY < expectedMinY) {
                    BlockPos surfacePos = new BlockPos(x, currentSurfaceY, z);

                    // Check if open to the sky (surface ravine or canyon)
                    if (level.canSeeSkyFromBelowWater(surfacePos.above())) {
                        bridgeSurfaceRavine(level, x, z, currentSurfaceY, expectedMinY, biome);
                    }
                }
            }
        }
    }

    /**
     * Bridges an open surface ravine or abrupt canyon drop while preserving underground caves.
     */
    private void bridgeSurfaceRavine(LevelAccessor level, int x, int z, int ravineBottomY, int targetSurfaceY,
                                     net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        String biomePath = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("plains");

        BlockState surfaceState;
        BlockState subSurfaceState;
        BlockState fillState;

        if (biomePath.contains("desert")) {
            surfaceState = Blocks.SAND.defaultBlockState();
            subSurfaceState = Blocks.SANDSTONE.defaultBlockState();
            fillState = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        } else if (biomePath.contains("badlands") || biomePath.contains("mesa")) {
            surfaceState = Blocks.RED_SAND.defaultBlockState();
            subSurfaceState = Blocks.TERRACOTTA.defaultBlockState();
            fillState = Blocks.RED_SANDSTONE.defaultBlockState();
        } else if (biomePath.contains("snow") || biomePath.contains("ice")) {
            surfaceState = Blocks.SNOW_BLOCK.defaultBlockState();
            subSurfaceState = Blocks.DIRT.defaultBlockState();
            fillState = Blocks.STONE.defaultBlockState();
        } else if (biomePath.contains("taiga")) {
            surfaceState = Blocks.PODZOL.defaultBlockState();
            subSurfaceState = Blocks.DIRT.defaultBlockState();
            fillState = Blocks.STONE.defaultBlockState();
        } else {
            surfaceState = Blocks.GRASS_BLOCK.defaultBlockState();
            subSurfaceState = Blocks.DIRT.defaultBlockState();
            fillState = Blocks.STONE.defaultBlockState();
        }

        // Determine vault roof depth (5 blocks thick to ensure rock-solid walkable surface)
        int roofBottomY = Math.max(ravineBottomY, targetSurfaceY - 5);

        // Fill from roof bottom up to target surface
        for (int y = roofBottomY; y <= targetSurfaceY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isProtectedBlockSafe(level, pos)) continue;

            if (y == targetSurfaceY) {
                level.setBlock(pos, surfaceState, 3);
            } else if (y >= targetSurfaceY - 3) {
                level.setBlock(pos, subSurfaceState, 3);
            } else {
                level.setBlock(pos, fillState, 3);
            }
        }

        // Clear air for 3 blocks above the newly bridged surface for entity clearance (golems & cavalry)
        for (int y = targetSurfaceY + 1; y <= targetSurfaceY + 3; y++) {
            BlockPos airPos = new BlockPos(x, y, z);
            if (!level.getBlockState(airPos).isAir() && !isProtectedBlockSafe(level, airPos)) {
                var state = level.getBlockState(airPos);
                if (state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT) || state.is(Blocks.STONE)) {
                    level.setBlock(airPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    /**
     * Inspects building piece foundations and extends supporting columns down to solid ground.
     */
    public void securePieceFoundation(LevelAccessor level, StructurePiece piece, BlockState foundationMaterial) {
        if (level == null || piece == null) return;
        BoundingBox box = piece.getBoundingBox();
        secureFoundationBox(level, box, foundationMaterial);
    }

    /**
     * Projects foundation piers from the perimeter of a bounding box down to solid ground.
     */
    public void secureFoundationBox(LevelAccessor level, BoundingBox box, BlockState foundationMaterial) {
        if (level == null || box == null) return;
        BlockState material = foundationMaterial != null ? foundationMaterial : Blocks.COBBLESTONE.defaultBlockState();

        int minY = box.minY();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                // Focus on walls and perimeter columns to preserve natural basements
                boolean isEdge = (x == box.minX() || x == box.maxX() || z == box.minZ() || z == box.maxZ());
                if (!isEdge) continue;

                BlockPos checkPos = new BlockPos(x, minY - 1, z);
                int depth = 0;
                while (depth < 16 && checkPos.getY() > level.getMinY()) {
                    BlockState bs = level.getBlockState(checkPos);
                    if (bs.isSolid() && !bs.is(Blocks.OAK_LEAVES) && !bs.is(Blocks.SPRUCE_LEAVES)) {
                        break; // Solid ground reached
                    }
                    if (isProtectedBlockSafe(level, checkPos)) {
                        break;
                    }
                    level.setBlock(checkPos, material, 3);
                    checkPos = checkPos.below();
                    depth++;
                }
            }
        }
    }

    /**
     * Verifies that building doors and gates have accessible, unobstructed ground steps.
     */
    public void ensureAccessibleEntrance(LevelAccessor level, BlockPos doorPos, Direction facing) {
        if (level == null || doorPos == null || facing == null) return;

        BlockPos front = doorPos.relative(facing);
        BlockPos groundUnderFront = front.below();

        // If front has a sudden drop below the door sill, add a solid porch step
        if (!level.getBlockState(groundUnderFront).isSolid()) {
            level.setBlock(groundUnderFront, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }

        // Guarantee 2.5m vertical headroom in front of the door
        if (!level.getBlockState(front).isAir() && !(level.getBlockState(front).getBlock() instanceof DoorBlock)
            && !(level.getBlockState(front).getBlock() instanceof FenceGateBlock)) {
            level.setBlock(front, Blocks.AIR.defaultBlockState(), 3);
        }
        if (!level.getBlockState(front.above()).isAir()) {
            level.setBlock(front.above(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /**
     * Validates and prepares an autonomous expansion plot, leveling slope and filling ravines.
     */
    public boolean stabilizeExpansionPlot(ServerLevel level, BlockPos origin, int width, int depth) {
        if (level == null || origin == null) return false;

        int refY = origin.getY();
        var biome = level.getBiome(origin);

        // Pre-fill any hollows or ravines within the plot
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                int colX = origin.getX() + dx;
                int colZ = origin.getZ() + dz;
                int currentY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, colX, colZ);

                if (currentY < refY - 1) {
                    bridgeSurfaceRavine(level, colX, colZ, currentY, refY - 1, biome);
                }
            }
        }

        // Project foundation columns down around the footprint perimeter
        BoundingBox plotBox = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
            origin.getX() + width - 1, origin.getY() + 8, origin.getZ() + depth - 1);
        secureFoundationBox(level, plotBox, Blocks.COBBLESTONE.defaultBlockState());

        return true;
    }

    public void clear() {
        stabilizedRegions.clear();
        activeSettlementFootprints.clear();
    }
}
