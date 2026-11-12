package livingworld.worldgen;

import java.util.*;
import livingworld.core.ProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Container Loot & Badlands Temple Trap Engine (Phase 2).
 *
 * Responsibilities:
 * 1. Empty Container Resolution: Ensures every intended loot container (chests, barrels)
 *    in village buildings, workshops, inns, and dimensional scars receives a valid loot table
 *    yielding at least one item.
 * 2. Badlands Temple Trap Invariant: Enforces exactly ONE trapped chest and THREE regular
 *    chests in Badlands and Desert pyramids. The single trapped chest detonates the TNT trap below,
 *    while the three regular chests can be safely looted without triggering the trap.
 * 3. Anti-Refill Protection: Never refills containers already opened, looted, or placed by players.
 */
public class ContainerLootManager {
    private static final ContainerLootManager INSTANCE = new ContainerLootManager();

    // Cache of processed pyramid chambers to prevent redundant evaluations
    private final Set<Long> processedPyramids = Collections.synchronizedSet(new HashSet<>());

    public static ContainerLootManager get() {
        return INSTANCE;
    }

    /**
     * Seeds a container placed by a blueprint or dimensional scar with an appropriate loot table.
     */
    public void seedBlueprintContainer(LevelAccessor level, BlockPos pos, String blueprintOrContextId) {
        if (level == null || pos == null) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RandomizableContainerBlockEntity container)) {
            return;
        }

        // If already has a loot table, do not overwrite
        if (container.getLootTable() != null) {
            return;
        }

        ResourceKey<LootTable> lootTable = selectLootTable(blueprintOrContextId);
        container.setLootTable(lootTable, level.getRandom().nextLong());
        be.setChanged();
    }

    /**
     * Maps building types and environmental contexts to thematic vanilla loot tables.
     */
    public ResourceKey<LootTable> selectLootTable(String contextId) {
        if (contextId == null) return BuiltInLootTables.VILLAGE_PLAINS_HOUSE;
        String id = contextId.toLowerCase();

        if (id.contains("fish") || id.contains("dock")) {
            return BuiltInLootTables.VILLAGE_FISHER;
        } else if (id.contains("barracks") || id.contains("armory") || id.contains("watchtower")) {
            return BuiltInLootTables.VILLAGE_WEAPONSMITH;
        } else if (id.contains("bakery") || id.contains("farm") || id.contains("barn") || id.contains("pen")) {
            return BuiltInLootTables.VILLAGE_BUTCHER;
        } else if (id.contains("clinic") || id.contains("cleric") || id.contains("temple")) {
            return BuiltInLootTables.VILLAGE_TEMPLE;
        } else if (id.contains("mason") || id.contains("workshop") || id.contains("blacksmith")) {
            return BuiltInLootTables.VILLAGE_TOOLSMITH;
        } else if (id.contains("library") || id.contains("cartograph")) {
            return BuiltInLootTables.VILLAGE_CARTOGRAPHER;
        } else if (id.contains("palace") || id.contains("treasury") || id.contains("vault")) {
            return BuiltInLootTables.VILLAGE_ARMORER;
        } else if (id.contains("scar") || id.contains("nether") || id.contains("relic") || id.contains("portal")) {
            return BuiltInLootTables.RUINED_PORTAL;
        } else {
            return BuiltInLootTables.VILLAGE_PLAINS_HOUSE;
        }
    }

    /**
     * Inspects a pyramid chamber (Explorify or Vanilla) and enforces exactly 1 trapped chest and 3 regular chests.
     * All 4 receive the desert pyramid loot table.
     */
    public void validateAndFixPyramidTrap(ServerLevel level, BlockPos pressurePlatePos) {
        if (level == null || pressurePlatePos == null) return;
        long chamberKey = pressurePlatePos.asLong();
        if (processedPyramids.contains(chamberKey)) return;
        processedPyramids.add(chamberKey);

        // Surrounding chest positions at radius 2 (North, South, East, West)
        BlockPos[] chestPositions = new BlockPos[] {
            pressurePlatePos.north(2),
            pressurePlatePos.south(2),
            pressurePlatePos.east(2),
            pressurePlatePos.west(2)
        };

        List<BlockPos> detectedChests = new ArrayList<>();
        for (BlockPos p : chestPositions) {
            BlockState state = level.getBlockState(p);
            if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
                detectedChests.add(p);
            }
        }

        if (detectedChests.size() < 4) {
            return; // Not a standard 4-chest pyramid vault
        }

        // Exactly one chest must be TRAPPED_CHEST; the other three must be regular CHEST
        // Pick index 0 (e.g. North chest or East chest) as the single trapped chest
        for (int i = 0; i < detectedChests.size(); i++) {
            BlockPos cPos = detectedChests.get(i);
            BlockState curState = level.getBlockState(cPos);
            Direction facing = curState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? curState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;

            boolean shouldBeTrapped = (i == 0); // Exactly index 0 is trapped!

            if (shouldBeTrapped && !curState.is(Blocks.TRAPPED_CHEST)) {
                BlockState newState = Blocks.TRAPPED_CHEST.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                level.setBlock(cPos, newState, 3);
                assignPyramidLoot(level, cPos);
            } else if (!shouldBeTrapped && !curState.is(Blocks.CHEST)) {
                BlockState newState = Blocks.CHEST.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                level.setBlock(cPos, newState, 3);
                assignPyramidLoot(level, cPos);
            } else {
                // Ensure loot table is assigned
                assignPyramidLoot(level, cPos);
            }
        }

        livingworld.LivingWorldMod.LOGGER.info("[Living World] Pyramid vault validated at " + pressurePlatePos
            + ": configured exactly 1 trapped chest and 3 regular chests.");
    }

    private void assignPyramidLoot(ServerLevel level, BlockPos chestPos) {
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof RandomizableContainerBlockEntity container) {
            if (container.getLootTable() == null && container.isEmpty()) {
                container.setLootTable(BuiltInLootTables.DESERT_PYRAMID, level.getRandom().nextLong());
                be.setChanged();
            }
        }
    }

    /**
     * Inspects a newly loaded chunk for desert/badlands pyramids and validates their chambers.
     */
    public void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
        if (level == null || chunkPos == null) return;
        // Search chunk for stone pressure plates with TNT underneath (pyramid trap signatures)
        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Search around standard pyramid floor levels (y=10 to y=65)
                for (int y = 15; y <= 65; y++) {
                    BlockPos probe = new BlockPos(x, y, z);
                    BlockState bs = level.getBlockState(probe);
                    if (bs.is(Blocks.STONE_PRESSURE_PLATE)) {
                        // Check if TNT is below at y - 2
                        BlockPos tntPos = probe.below(2);
                        if (level.getBlockState(tntPos).is(Blocks.TNT)) {
                            validateAndFixPyramidTrap(level, probe);
                        }
                    }
                }
            }
        }
    }

    public void clear() {
        processedPyramids.clear();
    }
}
