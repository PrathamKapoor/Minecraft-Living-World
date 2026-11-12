package livingworld.worldgen;

import java.util.List;
import java.util.Random;
import livingworld.core.ProtectionManager;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Feature F19: Dimensional Scars Generator (Bidirectional Nether–Overworld Biome Incursions).
 * Generates substantial, contiguous environmental anomalies where fragments of one dimension
 * have breached into another across 80–180 block diameters:
 * - Overworld Nether Incursions: Warped Forest, Crimson Forest, Soul Sand Valley, Basalt Deltas, Nether Wastes
 * - Nether Overworld Enclaves: Lost Village Enclave, Woodland Fragment
 * - Noise-based irregular boundary blending, multi-stage gradient transitions, diverse vegetation
 * - Strict Shoreline / Water Safeguard: Never places flat floating nylium slabs on water surfaces;
 *   submerges roots and floor transitions to the actual ocean bed.
 * - Soul Sand Valley fossil variations: Multi-orientation rib cages, spine segments, tusks, and tilted fragments.
 * - Crimson and Warped Forest canopy diversity: Variable tree heights (5-11), stepped and drooping caps, embedded shroomlights.
 * - Protected world invariant: respects player claims and pre-existing villages.
 * - Chunk-bounded slicing: world alterations execute strictly within loaded chunk bounds to prevent deadlocks.
 */
public class DimensionalScarGenerator {

    public enum ScarType {
        WARPED_FOREST("Warped Forest Incursion", true),
        CRIMSON_FOREST("Crimson Forest Incursion", true),
        SOUL_SAND_VALLEY("Soul Sand Valley Incursion", true),
        BASALT_DELTAS("Basalt Deltas Incursion", true),
        NETHER_WASTES("Nether Wastes Incursion", true),
        LOST_VILLAGE_ENCLAVE("Lost Village Enclave", false),
        OVERWORLD_WOODLAND("Overworld Woodland Fragment", false);

        private final String title;
        private final boolean overworldTarget;

        ScarType(String title, boolean overworldTarget) {
            this.title = title;
            this.overworldTarget = overworldTarget;
        }

        public String getTitle() {
            return title;
        }

        public boolean isOverworldTarget() {
            return overworldTarget;
        }
    }

    /**
     * Generates a complete dimensional scar at the specified coordinates by registering
     * the scar record and triggering generation across all currently loaded chunks within radius.
     */
    public static boolean generateScar(ServerLevel level, BlockPos center, ScarType type, int radius) {
        if (level == null || center == null) return false;
        if (radius < 30) radius = 30; // Min radius 30 (diameter 60)
        if (radius > 100) radius = 100; // Max radius 100 (diameter 200)

        // Safety check: protect player structures and villages
        if (ProtectionManager.get().isProtectedBlock(level, center) || SettlementManager.get().getNearestSettlement(center) != null) {
            livingworld.LivingWorldMod.LOGGER.warn("[Living World] Dimensional scar generation aborted: Location " + center + " is in a protected zone.");
            return false;
        }

        DimensionalScarManager.ScarRecord record = new DimensionalScarManager.ScarRecord(
            java.util.UUID.randomUUID(), type, center, radius, level.dimension().identifier().toString()
        );
        DimensionalScarManager.get().registerScar(record);
        DimensionalScarManager.get().triggerScarGeneration(level, record);

        livingworld.LivingWorldMod.LOGGER.info(String.format("[Living World] Dimensional Scar registered & triggered: %s at (%d, %d, %d) with radius %d",
            type.getTitle(), center.getX(), center.getY(), center.getZ(), radius));
        return true;
    }

    /**
     * Chunk-bounded scar generation slice.
     * Alters surface blocks strictly within the specified chunkPos bounds to avoid cascading chunk loads.
     */
    public static int generateScarChunk(ServerLevel level, ChunkPos chunkPos, DimensionalScarManager.ScarRecord scar) {
        if (level == null || chunkPos == null || scar == null) return 0;
        BlockPos samplePos = new BlockPos(chunkPos.x() << 4, 64, chunkPos.z() << 4);
        if (!level.isLoaded(samplePos)) return 0;

        int minX = chunkPos.x() << 4;
        int maxX = minX + 15;
        int minZ = chunkPos.z() << 4;
        int maxZ = minZ + 15;

        BlockPos center = scar.getCenter();
        int radius = scar.getRadius();
        ScarType type = scar.getType();

        int blocksPlaced = 0;

        // 1. Terrain Surface Replacement with Noise-Displaced Boundary for this chunk's columns
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int dx = x - center.getX();
                int dz = z - center.getZ();
                double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                double angle = Math.atan2(dz, dx);

                // Multi-frequency harmonic boundary distortion (creates natural organic fracture edges)
                double boundaryNoise = Math.sin(angle * 3.5) * 0.18 + Math.cos(angle * 6.2) * 0.12;
                double effectiveRadius = radius * (0.85 + boundaryNoise);

                if (dist > effectiveRadius) continue;

                BlockPos colPos = new BlockPos(x, center.getY(), z);
                if (!level.isLoaded(colPos)) continue;

                // Sample terrain surface safely
                BlockPos surfacePos = type.isOverworldTarget()
                    ? level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, colPos)
                    : findNetherSurface(level, colPos);

                if (surfacePos == null || surfacePos.getY() <= level.getMinY() + 5) continue;
                if (!level.isLoaded(surfacePos)) continue;

                // Safety: never overwrite player-protected blocks
                if (ProtectionManager.get().isProtectedBlock(level, surfacePos)) continue;

                Random colRandom = new Random(level.getSeed() ^ colPos.asLong());
                double edgeFactor = dist / effectiveRadius;

                // Probability of replacement decreases near boundary for smooth transition
                if (edgeFactor > 0.85 && colRandom.nextDouble() < (edgeFactor - 0.85) * 6.0) {
                    continue; // Natural gradient blend
                }

                // Shoreline & Open Water Safeguard (CRITICAL):
                // NEVER place a flat netherrack or nylium slab on top of water or open oceans!
                BlockState currentSurfaceState = level.getBlockState(surfacePos.below());
                if (currentSurfaceState.is(Blocks.WATER) || !level.getFluidState(surfacePos.below()).isEmpty()) {
                    // Trace down to the actual ocean floor / lake bed
                    BlockPos bedPos = surfacePos.below();
                    while (bedPos.getY() > level.getMinY() + 5 && (!level.getBlockState(bedPos).isSolid() || !level.getFluidState(bedPos).isEmpty())) {
                        bedPos = bedPos.below();
                    }
                    if (!level.isLoaded(bedPos) || ProtectionManager.get().isProtectedBlock(level, bedPos)) continue;

                    // Blend ocean floor bed submerged, leaving water surface naturally liquid
                    if (type == ScarType.WARPED_FOREST) {
                        setBlockSafe(level, bedPos, Blocks.SOUL_SAND.defaultBlockState());
                        if (colRandom.nextInt(5) == 0 && level.getBlockState(bedPos.above()).is(Blocks.WATER)) {
                            setBlockSafe(level, bedPos.above(), Blocks.SEAGRASS.defaultBlockState());
                        }
                    } else if (type == ScarType.SOUL_SAND_VALLEY) {
                        setBlockSafe(level, bedPos, Blocks.SOUL_SOIL.defaultBlockState());
                    } else if (type == ScarType.CRIMSON_FOREST) {
                        setBlockSafe(level, bedPos, Blocks.GRAVEL.defaultBlockState());
                    }
                    blocksPlaced++;
                    continue; // Fluid safeguard handled: never paste floating slabs on water!
                }

                replaceSurfaceColumn(level, surfacePos.below(), type, edgeFactor, colRandom);
                blocksPlaced++;

                // Thematic Vegetation & Features (1 in 14 blocks, within interior 75% zone)
                if (colRandom.nextInt(14) == 0 && edgeFactor < 0.75) {
                    placeThematicFeature(level, surfacePos, type, colRandom);
                }
            }
        }

        // 2. Central Monumental Ruin (Ruined Gateway Portal or Lost Village Cottage)
        // Generated strictly when the center coordinates reside within this chunk
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        if (chunkPos.x() == centerChunkX && chunkPos.z() == centerChunkZ) {
            if (level.isLoaded(center)) {
                if (type.isOverworldTarget()) {
                    generateRuinedNetherGateway(level, center);
                } else {
                    generateAbandonedOverworldCottage(level, center);
                }
            }
        }

        return blocksPlaced;
    }

    private static void setBlockSafe(ServerLevel level, BlockPos pos, BlockState state) {
        if (level != null && pos != null && level.isLoaded(pos)) {
            level.setBlock(pos, state, 3);
        }
    }

    private static boolean isAirSafe(ServerLevel level, BlockPos pos) {
        return level != null && pos != null && level.isLoaded(pos) && level.getBlockState(pos).isAir();
    }

    private static BlockPos findNetherSurface(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) return null;
        // In the Nether, scan down from y=90 for solid floor
        for (int y = 90; y >= 32; y--) {
            BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
            if (!level.isLoaded(p)) return null;
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && state.isSolid()) {
                BlockPos above = p.above();
                if (level.isLoaded(above) && level.getBlockState(above).isAir()) {
                    return above;
                }
            }
        }
        return null;
    }

    private static void replaceSurfaceColumn(ServerLevel level, BlockPos surfaceBlock, ScarType type, double edgeFactor, Random random) {
        if (level == null || surfaceBlock == null || !level.isLoaded(surfaceBlock)) return;

        BlockState topState;
        BlockState subState;

        switch (type) {
            case WARPED_FOREST -> {
                if (edgeFactor > 0.75) {
                    topState = random.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
                    subState = Blocks.DIRT.defaultBlockState();
                } else if (edgeFactor > 0.50) {
                    topState = random.nextInt(3) == 0 ? Blocks.NETHERRACK.defaultBlockState() : Blocks.WARPED_NYLIUM.defaultBlockState();
                    subState = Blocks.NETHERRACK.defaultBlockState();
                } else {
                    topState = Blocks.WARPED_NYLIUM.defaultBlockState();
                    subState = Blocks.NETHERRACK.defaultBlockState();
                }
            }
            case CRIMSON_FOREST -> {
                if (edgeFactor > 0.75) {
                    topState = random.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
                    subState = Blocks.DIRT.defaultBlockState();
                } else if (edgeFactor > 0.50) {
                    topState = random.nextInt(3) == 0 ? Blocks.NETHERRACK.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState();
                    subState = Blocks.NETHERRACK.defaultBlockState();
                } else {
                    topState = Blocks.CRIMSON_NYLIUM.defaultBlockState();
                    subState = Blocks.NETHERRACK.defaultBlockState();
                }
            }
            case SOUL_SAND_VALLEY -> {
                if (edgeFactor > 0.80) {
                    topState = random.nextBoolean() ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                    subState = Blocks.DIRT.defaultBlockState();
                } else {
                    int roll = random.nextInt(10);
                    if (roll < 5) topState = Blocks.SOUL_SAND.defaultBlockState();
                    else if (roll < 8) topState = Blocks.SOUL_SOIL.defaultBlockState();
                    else topState = Blocks.BASALT.defaultBlockState();
                    subState = Blocks.SOUL_SOIL.defaultBlockState();
                }
            }
            case BASALT_DELTAS -> {
                topState = random.nextInt(3) == 0 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
                subState = Blocks.SMOOTH_BASALT.defaultBlockState();
            }
            case NETHER_WASTES -> {
                topState = Blocks.NETHERRACK.defaultBlockState();
                subState = random.nextInt(10) == 0 ? Blocks.NETHER_QUARTZ_ORE.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
            }
            case LOST_VILLAGE_ENCLAVE, OVERWORLD_WOODLAND -> {
                // In Nether: replace netherrack with fertile Overworld topsoil
                topState = Blocks.GRASS_BLOCK.defaultBlockState();
                subState = Blocks.DIRT.defaultBlockState();
            }
            default -> {
                topState = Blocks.NETHERRACK.defaultBlockState();
                subState = Blocks.NETHERRACK.defaultBlockState();
            }
        }

        setBlockSafe(level, surfaceBlock, topState);
        setBlockSafe(level, surfaceBlock.below(), subState);
        if (random.nextBoolean()) {
            setBlockSafe(level, surfaceBlock.below(2), subState);
        }
    }

    private static void placeThematicFeature(ServerLevel level, BlockPos pos, ScarType type, Random random) {
        if (!isAirSafe(level, pos)) return;

        switch (type) {
            case WARPED_FOREST -> {
                int roll = random.nextInt(6);
                if (roll == 0) setBlockSafe(level, pos, Blocks.WARPED_FUNGUS.defaultBlockState());
                else if (roll == 1) setBlockSafe(level, pos, Blocks.WARPED_ROOTS.defaultBlockState());
                else if (roll == 2) setBlockSafe(level, pos, Blocks.NETHER_SPROUTS.defaultBlockState());
                else if (roll == 3) generateWarpedFungusTree(level, pos, random);
            }
            case CRIMSON_FOREST -> {
                int roll = random.nextInt(5);
                if (roll == 0) setBlockSafe(level, pos, Blocks.CRIMSON_FUNGUS.defaultBlockState());
                else if (roll == 1) setBlockSafe(level, pos, Blocks.CRIMSON_ROOTS.defaultBlockState());
                else if (roll == 2) generateCrimsonFungusTree(level, pos, random);
            }
            case SOUL_SAND_VALLEY -> {
                BlockState ground = level.getBlockState(pos.below());
                if (ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.SOUL_SOIL)) {
                    if (random.nextInt(3) == 0) {
                        setBlockSafe(level, pos, Blocks.SOUL_FIRE.defaultBlockState());
                    } else if (random.nextInt(3) == 0) {
                        generateFossilStructure(level, pos, random);
                    }
                }
            }
            case BASALT_DELTAS -> {
                int height = 2 + random.nextInt(4);
                for (int y = 0; y < height; y++) {
                    setBlockSafe(level, pos.above(y), Blocks.BASALT.defaultBlockState());
                }
            }
            case NETHER_WASTES -> {
                if (random.nextInt(4) == 0) {
                    setBlockSafe(level, pos, Blocks.FIRE.defaultBlockState());
                }
            }
            case LOST_VILLAGE_ENCLAVE, OVERWORLD_WOODLAND -> {
                // Generate varied Oak and Spruce trees inside Nether enclave
                if (random.nextInt(3) == 0) {
                    if (random.nextBoolean()) {
                        generateSmallOakTree(level, pos, random);
                    } else {
                        generateSpruceTree(level, pos, random);
                    }
                } else {
                    setBlockSafe(level, pos, Blocks.SHORT_GRASS.defaultBlockState());
                }
            }
        }
    }

    private static void generateWarpedFungusTree(ServerLevel level, BlockPos base, Random random) {
        // Variable height: 5 to 11 blocks (Section 12)
        int height = 5 + random.nextInt(7);
        for (int y = 0; y < height; y++) {
            setBlockSafe(level, base.above(y), Blocks.WARPED_STEM.defaultBlockState());
        }
        BlockPos capCenter = base.above(height);

        // Diverse Stepped Cap with Shroomlights and Drooping Overhangs
        for (int dy = -1; dy <= 2; dy++) {
            int radius = (dy == 2) ? 1 : ((dy == 1) ? 2 : 3);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 1) continue;
                    BlockPos p = capCenter.offset(dx, dy, dz);
                    if (isAirSafe(level, p)) {
                        boolean isShroomlight = (dy <= 0 && random.nextInt(5) == 0);
                        setBlockSafe(level, p, (isShroomlight ? Blocks.SHROOMLIGHT : Blocks.WARPED_WART_BLOCK).defaultBlockState());
                    }
                }
            }
        }
        // Drooping tendrils on edge corners
        int[][] droopOffsets = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        for (int[] off : droopOffsets) {
            if (random.nextBoolean()) {
                BlockPos dp = capCenter.offset(off[0], -2, off[1]);
                if (isAirSafe(level, dp)) {
                    setBlockSafe(level, dp, Blocks.WARPED_WART_BLOCK.defaultBlockState());
                }
            }
        }
    }

    private static void generateCrimsonFungusTree(ServerLevel level, BlockPos base, Random random) {
        // Variable height: 5 to 11 blocks (Section 12)
        int height = 5 + random.nextInt(7);
        for (int y = 0; y < height; y++) {
            setBlockSafe(level, base.above(y), Blocks.CRIMSON_STEM.defaultBlockState());
        }
        BlockPos capCenter = base.above(height);

        // Diverse Stepped Conical Cap with Shroomlights
        for (int dy = -1; dy <= 2; dy++) {
            int radius = (dy == 2) ? 1 : ((dy == 1) ? 2 : 3);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 1) continue;
                    BlockPos p = capCenter.offset(dx, dy, dz);
                    if (isAirSafe(level, p)) {
                        boolean isShroomlight = (dy <= 0 && random.nextInt(5) == 0);
                        setBlockSafe(level, p, (isShroomlight ? Blocks.SHROOMLIGHT : Blocks.NETHER_WART_BLOCK).defaultBlockState());
                    }
                }
            }
        }
        // Drooping tendrils on edge corners
        int[][] droopOffsets = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        for (int[] off : droopOffsets) {
            if (random.nextBoolean()) {
                BlockPos dp = capCenter.offset(off[0], -2, off[1]);
                if (isAirSafe(level, dp)) {
                    setBlockSafe(level, dp, Blocks.NETHER_WART_BLOCK.defaultBlockState());
                }
            }
        }
    }

    /**
     * Varied Soul Sand Valley Bone Fossil Structures (Section 12).
     * 1. Curving Rib Cage (spans 5 blocks with inward arching ribs)
     * 2. Spine / Vertebrae Fragment (linear row with vertical protrusions)
     * 3. Protruding Tusk / Spire (4 to 8 blocks tall with curve)
     * 4. Tilted Fossil Fragment (embedded diagonally into ground)
     */
    private static void generateFossilStructure(ServerLevel level, BlockPos base, Random random) {
        int variant = random.nextInt(4);
        boolean alignX = random.nextBoolean();

        switch (variant) {
            case 0 -> {
                // 1. Curving Rib Cage
                int ribHeight = 3 + random.nextInt(3);
                for (int y = 0; y < ribHeight; y++) {
                    if (alignX) {
                        setBlockSafe(level, base.offset(-2, y, 0), Blocks.BONE_BLOCK.defaultBlockState());
                        setBlockSafe(level, base.offset(2, y, 0), Blocks.BONE_BLOCK.defaultBlockState());
                    } else {
                        setBlockSafe(level, base.offset(0, y, -2), Blocks.BONE_BLOCK.defaultBlockState());
                        setBlockSafe(level, base.offset(0, y, 2), Blocks.BONE_BLOCK.defaultBlockState());
                    }
                }
                // Overarching rib connector
                if (alignX) {
                    setBlockSafe(level, base.offset(-1, ribHeight, 0), Blocks.BONE_BLOCK.defaultBlockState());
                    setBlockSafe(level, base.offset(0, ribHeight, 0), Blocks.BONE_BLOCK.defaultBlockState());
                    setBlockSafe(level, base.offset(1, ribHeight, 0), Blocks.BONE_BLOCK.defaultBlockState());
                } else {
                    setBlockSafe(level, base.offset(0, ribHeight, -1), Blocks.BONE_BLOCK.defaultBlockState());
                    setBlockSafe(level, base.offset(0, ribHeight, 0), Blocks.BONE_BLOCK.defaultBlockState());
                    setBlockSafe(level, base.offset(0, ribHeight, 1), Blocks.BONE_BLOCK.defaultBlockState());
                }
            }
            case 1 -> {
                // 2. Spine / Vertebrae Segment
                int length = 4 + random.nextInt(3);
                for (int i = 0; i < length; i++) {
                    BlockPos spine = alignX ? base.offset(i - length / 2, 0, 0) : base.offset(0, 0, i - length / 2);
                    setBlockSafe(level, spine, Blocks.BONE_BLOCK.defaultBlockState());
                    if (i % 2 == 0) {
                        setBlockSafe(level, spine.above(), Blocks.BONE_BLOCK.defaultBlockState());
                    }
                }
            }
            case 2 -> {
                // 3. Protruding Tusk / Spire (4 to 7 blocks tall)
                int tuskHeight = 4 + random.nextInt(4);
                for (int y = 0; y < tuskHeight; y++) {
                    int xOff = (y > 3) ? (alignX ? 1 : 0) : 0;
                    int zOff = (y > 3) ? (!alignX ? 1 : 0) : 0;
                    setBlockSafe(level, base.offset(xOff, y, zOff), Blocks.BONE_BLOCK.defaultBlockState());
                }
            }
            case 3 -> {
                // 4. Tilted Diagonal Bone Fragment
                int diagLen = 3 + random.nextInt(3);
                for (int d = 0; d < diagLen; d++) {
                    BlockPos diag = alignX ? base.offset(d, d, 0) : base.offset(0, d, d);
                    setBlockSafe(level, diag, Blocks.BONE_BLOCK.defaultBlockState());
                }
            }
        }
    }

    private static void generateSmallOakTree(ServerLevel level, BlockPos base, Random random) {
        int trunkHeight = 4 + random.nextInt(2);
        for (int y = 0; y < trunkHeight; y++) {
            setBlockSafe(level, base.above(y), Blocks.OAK_LOG.defaultBlockState());
        }
        BlockPos leafCenter = base.above(trunkHeight);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy == 1) continue;
                    BlockPos p = leafCenter.offset(dx, dy, dz);
                    if (isAirSafe(level, p)) {
                        setBlockSafe(level, p, Blocks.OAK_LEAVES.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void generateSpruceTree(ServerLevel level, BlockPos base, Random random) {
        int trunkHeight = 6 + random.nextInt(3);
        for (int y = 0; y < trunkHeight; y++) {
            setBlockSafe(level, base.above(y), Blocks.SPRUCE_LOG.defaultBlockState());
        }
        // Conical spruce needle layers
        for (int y = 2; y <= trunkHeight; y++) {
            int radius = (trunkHeight - y) % 2 == 0 ? 2 : 1;
            if (y == trunkHeight) radius = 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 1) continue;
                    BlockPos p = base.offset(dx, y, dz);
                    if (isAirSafe(level, p)) {
                        setBlockSafe(level, p, Blocks.SPRUCE_LEAVES.defaultBlockState());
                    }
                }
            }
        }
        setBlockSafe(level, base.above(trunkHeight + 1), Blocks.SPRUCE_LEAVES.defaultBlockState());
    }

    /**
     * Generates a massive ruined dimensional gateway at the center of the Overworld scar.
     */
    private static void generateRuinedNetherGateway(ServerLevel level, BlockPos center) {
        if (level == null || center == null || !level.isLoaded(center)) return;
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, center);
        if (ground == null || !level.isLoaded(ground)) return;

        // Obsidian Portal Remnant (4x5 frame with crying obsidian, cracked obsidian, and fire)
        for (int y = 0; y < 5; y++) {
            setBlockSafe(level, ground.offset(-2, y, 0), y % 2 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
            setBlockSafe(level, ground.offset(2, y, 0), Blocks.OBSIDIAN.defaultBlockState());
        }
        setBlockSafe(level, ground.offset(-1, 0, 0), Blocks.OBSIDIAN.defaultBlockState());
        setBlockSafe(level, ground.offset(0, 0, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        setBlockSafe(level, ground.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState());

        setBlockSafe(level, ground.offset(-1, 4, 0), Blocks.OBSIDIAN.defaultBlockState());
        setBlockSafe(level, ground.offset(0, 4, 0), Blocks.OBSIDIAN.defaultBlockState());

        // Gold Block Offering & Chest Platform
        setBlockSafe(level, ground.offset(0, 0, 2), Blocks.GOLD_BLOCK.defaultBlockState());
        BlockPos altarChestPos = ground.offset(1, 0, 2);
        setBlockSafe(level, altarChestPos, Blocks.CHEST.defaultBlockState());
        ContainerLootManager.get().seedBlueprintContainer(level, altarChestPos, "scar_relic_altar");

        // Surrounding magma and crying obsidian scatter
        setBlockSafe(level, ground.offset(-3, -1, 1), Blocks.MAGMA_BLOCK.defaultBlockState());
        setBlockSafe(level, ground.offset(3, -1, -1), Blocks.MAGMA_BLOCK.defaultBlockState());
        setBlockSafe(level, ground.offset(1, 1, 0), Blocks.FIRE.defaultBlockState());
    }

    /**
     * Generates an abandoned rustic Overworld village cottage inside the Nether enclave.
     * Well-anchored deep foundation to withstand netherrack erosion.
     */
    private static void generateAbandonedOverworldCottage(ServerLevel level, BlockPos center) {
        if (level == null || center == null || !level.isLoaded(center)) return;
        BlockPos ground = findNetherSurface(level, center);
        if (ground == null) ground = center;
        if (!level.isLoaded(ground)) return;

        // Solid anchored foundation extending 2 blocks deep into netherrack
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -2; y <= -1; y++) {
                    setBlockSafe(level, ground.offset(x, y, z), Blocks.COBBLESTONE.defaultBlockState());
                }
                for (int y = 0; y <= 3; y++) {
                    boolean isCorner = (Math.abs(x) == 2 && Math.abs(z) == 2);
                    boolean isWall = (Math.abs(x) == 2 || Math.abs(z) == 2);
                    if (isCorner) {
                        setBlockSafe(level, ground.offset(x, y, z), Blocks.OAK_LOG.defaultBlockState());
                    } else if (isWall) {
                        if (x == 0 && z == -2 && (y == 0 || y == 1)) {
                            // Door entrance
                            continue;
                        }
                        setBlockSafe(level, ground.offset(x, y, z), Blocks.OAK_PLANKS.defaultBlockState());
                    }
                }
            }
        }

        // Roof (Cobblestone)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                setBlockSafe(level, ground.offset(x, 4, z), Blocks.COBBLESTONE.defaultBlockState());
            }
        }

        // Interior relics: Crafting table, abandoned chest, cobwebs, lantern
        setBlockSafe(level, ground.offset(1, 0, 1), Blocks.CRAFTING_TABLE.defaultBlockState());
        BlockPos cottageChestPos = ground.offset(-1, 0, 1);
        setBlockSafe(level, cottageChestPos, Blocks.CHEST.defaultBlockState());
        ContainerLootManager.get().seedBlueprintContainer(level, cottageChestPos, "scar_enclave_cottage");
        setBlockSafe(level, ground.offset(1, 2, -1), Blocks.COBWEB.defaultBlockState());
        setBlockSafe(level, ground.offset(0, 3, 0), Blocks.LANTERN.defaultBlockState());
    }
}
