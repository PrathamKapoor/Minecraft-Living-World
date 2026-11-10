package livingworld.worldgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import livingworld.config.FeatureConfig;
import livingworld.core.ProtectionManager;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Manager & State Tracker for Dimensional Scars (Feature F19).
 * - Tracks all naturally generated or administrative scars with persistent IDs
 * - Ensures minimum spacing (380+ blocks) between major scars to maintain exploration rarity
 * - Fully decoupled asynchronous chunk processing: onChunkLoad enqueues in O(1) time
 * - Tick scheduler processes chunk slices strictly within loaded chunk boundaries, eliminating deadlocks
 */
public class DimensionalScarManager {
    private static final DimensionalScarManager INSTANCE = new DimensionalScarManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, ScarRecord> registeredScars = new ConcurrentHashMap<>();
    private final Set<Long> processedChunks = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> generatedSlices = Collections.synchronizedSet(new HashSet<>());
    private final Map<ResourceKey<Level>, ConcurrentLinkedQueue<ChunkPos>> pendingQueues = new ConcurrentHashMap<>();

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
     * Non-blocking O(1) chunk load callback.
     * Enqueues the loaded chunk for tick processing without touching any world blocks or neighboring chunks.
     */
    public void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
        if (!FeatureConfig.get().enableDimensionalScars || level == null || chunkPos == null) return;
        pendingQueues.computeIfAbsent(level.dimension(), k -> new ConcurrentLinkedQueue<>()).add(chunkPos);
    }

    /**
     * Level tick handler: processes a modest budget of chunks per tick safely on the level thread.
     */
    public void tick(ServerLevel level) {
        if (!FeatureConfig.get().enableDimensionalScars || level == null) return;
        ConcurrentLinkedQueue<ChunkPos> queue = pendingQueues.get(level.dimension());
        if (queue == null || queue.isEmpty()) return;

        int budget = 4; // Process up to 4 chunk slices per tick
        while (budget > 0 && !queue.isEmpty()) {
            ChunkPos chunkPos = queue.poll();
            if (chunkPos == null) break;

            BlockPos samplePos = new BlockPos(chunkPos.x() << 4, 64, chunkPos.z() << 4);
            if (!level.isLoaded(samplePos)) {
                continue; // Skip chunk if already unloaded
            }

            budget--;
            processChunk(level, chunkPos);
        }
    }

    private void processChunk(ServerLevel level, ChunkPos chunkPos) {
        long chunkKey = chunkPos.pack();
        boolean firstVisit = processedChunks.add(chunkKey);

        // 1. Rare natural scar seeding check (evaluated only on first visit of chunk)
        if (firstVisit) {
            long seed = level.getSeed() ^ (chunkPos.x() * 341873128712L + chunkPos.z() * 132897987541L);
            Random r = new Random(seed);
            int rarity = Math.max(16, FeatureConfig.get().scarRarityChunks);
            if (r.nextInt(rarity) == 0) {
                BlockPos center = chunkPos.getMiddleBlockPosition(64);
                boolean spacingOk = true;
                for (ScarRecord existing : registeredScars.values()) {
                    if (existing.getCenter().distSqr(center) < (380.0 * 380.0)) {
                        spacingOk = false;
                        break;
                    }
                }
                if (spacingOk && !ProtectionManager.get().isProtectedBlock(level, center) && SettlementManager.get().getNearestSettlement(center) == null) {
                    boolean isNether = level.dimension() == Level.NETHER;
                    boolean isOverworld = level.dimension() == Level.OVERWORLD;
                    if (isOverworld || isNether) {
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
                        int radius = 40 + r.nextInt(35);
                        ScarRecord newScar = new ScarRecord(UUID.randomUUID(), type, center, radius, level.dimension().identifier().toString());
                        registerScar(newScar);
                        triggerScarGeneration(level, newScar);
                    }
                }
            }
        }

        // 2. Generate chunk slice for any registered scar intersecting this chunk
        String dimId = level.dimension().identifier().toString();
        for (ScarRecord scar : registeredScars.values()) {
            if (!scar.getDimension().equals(dimId)) continue;
            if (scarIntersectsChunk(scar, chunkPos)) {
                String sliceKey = scar.getId().toString() + "_" + chunkKey;
                if (!generatedSlices.contains(sliceKey)) {
                    generatedSlices.add(sliceKey);
                    DimensionalScarGenerator.generateScarChunk(level, chunkPos, scar);
                }
            }
        }
    }

    /**
     * Triggers scar slice generation across all currently loaded chunks intersecting the scar.
     * Unloaded chunks will generate their slices on demand as they load.
     */
    public void triggerScarGeneration(ServerLevel level, ScarRecord scar) {
        if (level == null || scar == null) return;
        int radius = scar.getRadius();
        BlockPos center = scar.getCenter();
        int minChunkX = (center.getX() - radius - 8) >> 4;
        int maxChunkX = (center.getX() + radius + 8) >> 4;
        int minChunkZ = (center.getZ() - radius - 8) >> 4;
        int maxChunkZ = (center.getZ() + radius + 8) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkPos cp = new ChunkPos(cx, cz);
                if (scarIntersectsChunk(scar, cp)) {
                    BlockPos sample = new BlockPos(cx << 4, 64, cz << 4);
                    if (level.isLoaded(sample)) {
                        String sliceKey = scar.getId().toString() + "_" + cp.pack();
                        if (!generatedSlices.contains(sliceKey)) {
                            generatedSlices.add(sliceKey);
                            DimensionalScarGenerator.generateScarChunk(level, cp, scar);
                        }
                    }
                }
            }
        }
    }

    public static boolean scarIntersectsChunk(ScarRecord scar, ChunkPos chunkPos) {
        int minX = chunkPos.x() << 4;
        int maxX = minX + 15;
        int minZ = chunkPos.z() << 4;
        int maxZ = minZ + 15;

        int cx = scar.getCenter().getX();
        int cz = scar.getCenter().getZ();
        int closestX = Math.max(minX, Math.min(cx, maxX));
        int closestZ = Math.max(minZ, Math.min(cz, maxZ));
        int dx = closestX - cx;
        int dz = closestZ - cz;
        int effR = scar.getRadius() + 6;
        return ((long) dx * dx + (long) dz * dz) <= ((long) effR * effR);
    }

    public void clear() {
        registeredScars.clear();
        processedChunks.clear();
        generatedSlices.clear();
        pendingQueues.clear();
    }
}
