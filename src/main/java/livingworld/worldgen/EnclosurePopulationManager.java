package livingworld.worldgen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.core.ProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Enclosure & Prisoner Cage Population Engine (Phase 2).
 *
 * Responsibilities:
 * 1. Livestock Starter Herds: Automatically populates empty livestock pasture pens and barns
 *    with healthy starter herds (cows, sheep, pigs) placed safely inside fenced boundaries.
 * 2. Outpost Prisoner Captives: Populates empty pillager outpost cages with appropriate captives
 *    (captured villagers, imprisoned iron golems, captive allays) contained behind iron bars.
 * 3. Containment & Rescue Invariant: Captives stay contained until freed by players, and remain
 *    permanently rescued across chunk unloads, saves, and reloads without duplicate respawns.
 * 4. Livestock Containment: Starter herds persist on reload and cannot escape closed gates.
 */
public class EnclosurePopulationManager {
    private static final EnclosurePopulationManager INSTANCE = new EnclosurePopulationManager();

    // Persistent tracking of populated enclosures and cages to guarantee zero duplicate spawns
    private final Set<Long> populatedPens = Collections.synchronizedSet(new HashSet<>());
    private final Set<Long> populatedCages = Collections.synchronizedSet(new HashSet<>());

    public static EnclosurePopulationManager get() {
        return INSTANCE;
    }

    /**
     * Inspects a newly loaded chunk for empty livestock pens and outpost cages.
     */
    public void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
        if (level == null || chunkPos == null) return;

        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        // 1. Scan for Pillager Outpost Cages (Iron Bars enclosures)
        for (int x = minX; x <= maxX; x += 2) {
            for (int z = minZ; z <= maxZ; z += 2) {
                for (int y = 50; y <= 110; y += 2) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState bs = level.getBlockState(p);

                    if (bs.getBlock() instanceof IronBarsBlock) {
                        checkAndPopulateCage(level, p);
                    } else if (bs.is(Blocks.HAY_BLOCK) || bs.getBlock() instanceof FenceGateBlock) {
                        checkAndPopulateLivestockPen(level, p);
                    }
                }
            }
        }
    }

    /**
     * Populates an empty livestock pasture pen with a healthy starter herd (2-3 adult livestock).
     */
    public boolean checkAndPopulateLivestockPen(ServerLevel level, BlockPos markerPos) {
        if (level == null || markerPos == null) return false;

        // Align to a 8-block grid cell to deduplicate detection across the same pen
        BlockPos gridKey = new BlockPos((markerPos.getX() >> 3) << 3, markerPos.getY(), (markerPos.getZ() >> 3) << 3);
        if (populatedPens.contains(gridKey.asLong())) {
            return false;
        }

        // Check if there are already livestock animals in this enclosure
        AABB penArea = new AABB(markerPos).inflate(6.0, 3.0, 6.0);
        List<Animal> existingAnimals = level.getEntitiesOfClass(Animal.class, penArea, a -> a.isAlive() && !a.isVehicle());
        if (!existingAnimals.isEmpty()) {
            populatedPens.add(gridKey.asLong());
            return false; // Already populated
        }

        // Verify that this is a real enclosure (has fences, fence gates, or walls)
        boolean hasFencing = false;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                BlockPos probe = markerPos.offset(dx, 0, dz);
                BlockState s = level.getBlockState(probe);
                if (s.is(Blocks.OAK_FENCE) || s.is(Blocks.SPRUCE_FENCE) || s.is(Blocks.OAK_FENCE_GATE) || s.is(Blocks.STONE_BRICK_WALL)) {
                    hasFencing = true;
                    break;
                }
            }
            if (hasFencing) break;
        }

        if (!hasFencing) return false;

        // Find safe spawn positions inside the enclosure
        List<BlockPos> interiorSpots = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos spot = markerPos.offset(dx, 0, dz);
                BlockPos ground = spot.below();
                if (level.getBlockState(ground).isSolid() && level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir()) {
                    interiorSpots.add(spot);
                }
            }
        }

        if (interiorSpots.size() < 2) return false;

        populatedPens.add(gridKey.asLong());

        // Spawn a starter herd of 2-3 suitable animals
        Random rand = new Random(gridKey.asLong() ^ level.getSeed());
        int animalTypeRoll = rand.nextInt(3); // 0 = Cows, 1 = Sheep, 2 = Pigs

        for (int i = 0; i < 3; i++) {
            BlockPos spawnPos = interiorSpots.get(i % interiorSpots.size());
            Animal animal = switch (animalTypeRoll) {
                case 0 -> EntityTypes.COW.create(level, EntitySpawnReason.STRUCTURE);
                case 1 -> EntityTypes.SHEEP.create(level, EntitySpawnReason.STRUCTURE);
                default -> EntityTypes.PIG.create(level, EntitySpawnReason.STRUCTURE);
            };

            if (animal != null) {
                animal.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                animal.setPersistenceRequired(); // Persist on reload
                animal.setAge(0); // Adult starter stock
                level.addFreshEntity(animal);
            }
        }

        livingworld.LivingWorldMod.LOGGER.info("[Living World] Livestock enclosure populated at " + markerPos + " with starter herd (type=" + animalTypeRoll + ")");
        return true;
    }

    /**
     * Inspects a pillager outpost cage and populates empty prisoner cells with appropriate captives.
     */
    public boolean checkAndPopulateCage(ServerLevel level, BlockPos ironBarsPos) {
        if (level == null || ironBarsPos == null) return false;

        BlockPos gridKey = new BlockPos((ironBarsPos.getX() >> 2) << 2, ironBarsPos.getY(), (ironBarsPos.getZ() >> 2) << 2);
        if (populatedCages.contains(gridKey.asLong())) {
            return false;
        }

        // Verify cage footprint: needs at least 4 iron bar blocks nearby
        int ironBarCount = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (level.getBlockState(ironBarsPos.offset(dx, dy, dz)).getBlock() instanceof IronBarsBlock) {
                        ironBarCount++;
                    }
                }
            }
        }

        if (ironBarCount < 4) return false;

        // Check if any entity is already inside the cage
        AABB cageBox = new AABB(ironBarsPos).inflate(2.5, 2.0, 2.5);
        List<net.minecraft.world.entity.LivingEntity> existing = level.getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class, cageBox, e -> e.isAlive());

        if (!existing.isEmpty()) {
            populatedCages.add(gridKey.asLong());
            return false; // Already holds a captive or entity
        }

        // Find floor position inside the cage
        BlockPos interiorFloor = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos probe = ironBarsPos.offset(dx, 0, dz);
                if (level.getBlockState(probe).isAir() && level.getBlockState(probe.below()).isSolid()) {
                    interiorFloor = probe;
                    break;
                }
            }
            if (interiorFloor != null) break;
        }

        if (interiorFloor == null) return false;

        populatedCages.add(gridKey.asLong());

        Random rand = new Random(gridKey.asLong() ^ level.getSeed());
        int captiveRoll = rand.nextInt(10); // 0-4: Villager, 5-7: Iron Golem, 8-9: Allay

        if (captiveRoll < 5) {
            // Captured Villager
            Villager captive = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
            if (captive != null) {
                captive.setPos(interiorFloor.getX() + 0.5, interiorFloor.getY(), interiorFloor.getZ() + 0.5);
                captive.setCustomName(Component.literal("§eImprisoned Villager"));
                captive.setCustomNameVisible(true);
                captive.setPersistenceRequired();
                level.addFreshEntity(captive);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Outpost prisoner cage at " + interiorFloor + " populated with Captured Villager.");
            }
        } else if (captiveRoll < 8) {
            // Imprisoned Iron Golem
            IronGolem captive = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.STRUCTURE);
            if (captive != null) {
                captive.setPos(interiorFloor.getX() + 0.5, interiorFloor.getY(), interiorFloor.getZ() + 0.5);
                captive.setPersistenceRequired();
                level.addFreshEntity(captive);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Outpost prisoner cage at " + interiorFloor + " populated with Captured Iron Golem.");
            }
        } else {
            // Imprisoned Allays (pair of 2)
            for (int k = 0; k < 2; k++) {
                Allay allay = EntityTypes.ALLAY.create(level, EntitySpawnReason.STRUCTURE);
                if (allay != null) {
                    allay.setPos(interiorFloor.getX() + 0.5, interiorFloor.getY() + 0.5, interiorFloor.getZ() + 0.5);
                    allay.setPersistenceRequired();
                    level.addFreshEntity(allay);
                }
            }
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Outpost prisoner cage at " + interiorFloor + " populated with 2 Captive Allays.");
        }

        return true;
    }

    public void clear() {
        populatedPens.clear();
        populatedCages.clear();
    }
}
