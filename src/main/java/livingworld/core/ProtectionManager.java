package livingworld.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

public class ProtectionManager {
    private static final ProtectionManager INSTANCE = new ProtectionManager();
    private final Set<BlockPos> playerPlacedBlocks = Collections.synchronizedSet(new HashSet<>());

    // Critical block types that mobs/builders should never overwrite or destroy
    private static final Set<Block> CRITICAL_BLOCKS = Set.of(
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.BARREL,
        Blocks.FURNACE,
        Blocks.BLAST_FURNACE,
        Blocks.SMOKER,
        Blocks.ENCHANTING_TABLE,
        Blocks.CRAFTING_TABLE,
        Blocks.ENDER_CHEST,
        Blocks.RESPAWN_ANCHOR,
        Blocks.NETHER_PORTAL,
        Blocks.END_PORTAL,
        Blocks.END_PORTAL_FRAME,
        Blocks.COMMAND_BLOCK,
        Blocks.CHAIN_COMMAND_BLOCK,
        Blocks.REPEATING_COMMAND_BLOCK,
        Blocks.SPAWNER
    );

    public static ProtectionManager get() {
        return INSTANCE;
    }

    public void registerPlayerBlock(BlockPos pos) {
        playerPlacedBlocks.add(pos.immutable());
    }

    public void unregisterPlayerBlock(BlockPos pos) {
        playerPlacedBlocks.remove(pos);
    }

    public boolean isProtectedBlock(ServerLevel level, BlockPos pos) {
        if (!FeatureConfig.get().protectPlayerBlocks) {
            return false;
        }

        // Respect mobGriefing gamerule if configured
        if (FeatureConfig.get().respectMobGriefingRule && level != null) {
            Boolean mobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
            if (mobGriefing != null && !mobGriefing) {
                return true;
            }
        }

        // Check if explicitly recorded as player-placed
        if (playerPlacedBlocks.contains(pos)) {
            return true;
        }

        if (level == null || !level.isLoaded(pos)) {
            return false;
        }

        // Check if the block is a protected player amenity/container
        BlockState state = level.getBlockState(pos);
        if (CRITICAL_BLOCKS.contains(state.getBlock())) {
            return true;
        }

        // Protect beds
        if (state.getBlock() instanceof BedBlock || state.getBlock().getDescriptionId().contains("bed")) {
            return true;
        }

        return false;
    }

    public boolean isProtectedAnimal(Entity entity) {
        if (!FeatureConfig.get().protectPlayerAnimals) {
            return false;
        }

        // Named animals are protected
        if (entity.hasCustomName()) {
            return true;
        }

        // Tamed pets are protected
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return true;
        }

        // Leashed animals are protected
        if (entity instanceof Leashable leashable && leashable.isLeashed()) {
            return true;
        }

        return false;
    }

    public void clear() {
        playerPlacedBlocks.clear();
    }
}
