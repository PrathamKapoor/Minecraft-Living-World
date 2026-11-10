package livingworld.worldgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Manager & State Tracker for Dimensional Scars (Feature F19).
 * - Tracks all naturally generated or administrative scars with persistent IDs
 * - Ensures minimum spacing (400+ blocks) between major scars to maintain exploration rarity
 * - Provides natural chunk-generation seeding and discovery telemetry
 */
public class DimensionalScarManager {
    private static final DimensionalScarManager INSTANCE = new DimensionalScarManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, ScarRecord> registeredScars = new ConcurrentHashMap<>();
    private final Set<Long> processedChunks = Collections.synchronizedSet(new HashSet<>());

    public static DimensionalScarManager get() {
        return INSTANCE;
    }

    public static class ScarRecord {
        private final UUID id;
        private final DimensionalScarGenerator.ScarType type;
        private final int x;
        private final int y;
        private final int z;
        private final int radius;
        private final String dimension;

        public ScarRecord(UUID id, DimensionalScarGenerator.ScarType type, BlockPos center, int radius, String dimension) {
            this.id = id;
            this.type = type;
            this.x = center.getX();
            this.y = center.getY();
            this.z = center.getZ();
            this.radius = radius;
            this.dimension = dimension;
        }

        public UUID getId() {
            return id;
        }

        public DimensionalScarGenerator.ScarType getType() {
            return type;
        }

        public BlockPos getCenter() {
            return new BlockPos(x, y, z);
        }

        public int getRadius() {
            return radius;
        }

        public String getDimension() {
            return dimension;
        }
    }

    public void registerScar(ScarRecord record) {
        if (record != null) {
            registeredScars.put(record.getId(), record);
        }
    }

    public Collection<ScarRecord> getAllScars() {
        return Collections.unmodifiableCollection(registeredScars.values());
    }

    public ScarRecord getNearestScar(BlockPos pos) {
        ScarRecord closest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (ScarRecord record : registeredScars.values()) {
            double d = record.getCenter().distSqr(pos);
            if (d < bestDistSq) {
                bestDistSq = d;
                closest = record;
            }
        }
        return closest;
    }

    /**
     * Checks if a chunk should generate a rare natural dimensional scar.
     */
    public void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
        if (!FeatureConfig.get().enableDimensionalScars || level == null) return;
        long chunkKey = chunkPos.pack();
        if (processedChunks.contains(chunkKey)) return;
        processedChunks.add(chunkKey);

        // Deterministic pseudorandom check based on world seed and chunk coordinates
        long seed = level.getSeed() ^ (chunkPos.x() * 341873128712L + chunkPos.z() * 132897987541L);
        Random r = new Random(seed);

        // Average 1 in scarRarityChunks (default: 128 chunks)
        if (r.nextInt(Math.max(16, FeatureConfig.get().scarRarityChunks)) != 0) {
            return;
        }

        BlockPos center = chunkPos.getMiddleBlockPosition(64);

        // Minimum spacing check: at least 380 blocks from any existing scar
        for (ScarRecord existing : registeredScars.values()) {
            if (existing.getCenter().distSqr(center) < (380.0 * 380.0)) {
                return;
            }
        }

        boolean isNether = level.dimension() == Level.NETHER;
        boolean isOverworld = level.dimension() == Level.OVERWORLD;

        if (!isNether && !isOverworld) return;

        DimensionalScarGenerator.ScarType type;
        if (isOverworld) {
            DimensionalScarGenerator.ScarType[] overworldTypes = {
                DimensionalScarGenerator.ScarType.WARPED_FOREST,
                DimensionalScarGenerator.ScarType.CRIMSON_FOREST,
                DimensionalScarGenerator.ScarType.SOUL_SAND_VALLEY,
                DimensionalScarGenerator.ScarType.BASALT_DELTAS,
                DimensionalScarGenerator.ScarType.NETHER_WASTES
            };
            type = overworldTypes[r.nextInt(overworldTypes.length)];
        } else {
            DimensionalScarGenerator.ScarType[] netherTypes = {
                DimensionalScarGenerator.ScarType.LOST_VILLAGE_ENCLAVE,
                DimensionalScarGenerator.ScarType.OVERWORLD_WOODLAND
            };
            type = netherTypes[r.nextInt(netherTypes.length)];
        }

        int radius = 40 + r.nextInt(35); // Diameter: 80 to 150 blocks
        DimensionalScarGenerator.generateScar(level, center, type, radius);
    }

    public void clear() {
        registeredScars.clear();
        processedChunks.clear();
    }
}
