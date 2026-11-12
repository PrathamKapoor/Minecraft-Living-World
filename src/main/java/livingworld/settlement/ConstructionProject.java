package livingworld.settlement;

import java.util.*;
import livingworld.core.ProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Represents a physical, block-by-block construction project (F06).
 * Actively involves nearby NPC builders who move, swing, and physically place blocks with sounds and particles.
 */
public class ConstructionProject {
    private final UUID id;
    private final String blueprintId;
    private final BlockPos origin;
    private final List<Map.Entry<BlockPos, BlockState>> remainingBlocks = new ArrayList<>();
    private final int totalBlocks;
    private int placedBlocks = 0;
    private boolean complete = false;

    public ConstructionProject(String blueprintId, BlockPos origin) {
        this(UUID.randomUUID(), blueprintId, origin);
    }

    public ConstructionProject(UUID id, String blueprintId, BlockPos origin) {
        this.id = id;
        this.blueprintId = blueprintId;
        this.origin = origin.immutable();

        Blueprint bp = BlueprintRegistry.get(blueprintId);
        if (bp != null) {
            remainingBlocks.addAll(bp.getBlocks().entrySet());
        }
        this.totalBlocks = remainingBlocks.size();
    }

    public UUID getId() {
        return id;
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getPlacedBlocks() {
        return placedBlocks;
    }

    public int getCurrentBlockIndex() {
        return placedBlocks;
    }

    public boolean isComplete() {
        return complete;
    }

    public float getProgress() {
        if (totalBlocks == 0) return 1.0f;
        return (float) placedBlocks / (float) totalBlocks;
    }

    public int step(ServerLevel level, int maxBlocksPerStep) {
        if (complete || remainingBlocks.isEmpty()) {
            complete = true;
            return 0;
        }

        int placedThisStep = 0;
        while (!remainingBlocks.isEmpty() && placedThisStep < maxBlocksPerStep) {
            Map.Entry<BlockPos, BlockState> entry = remainingBlocks.remove(0);
            BlockPos relativePos = entry.getKey();
            BlockState state = entry.getValue();
            BlockPos targetPos = origin.offset(relativePos.getX(), relativePos.getY(), relativePos.getZ());

            // Check if chunk is loaded
            if (!level.isLoaded(targetPos)) {
                // Defer placement until chunk is loaded
                remainingBlocks.add(0, entry);
                break;
            }

            // Check protection
            if (ProtectionManager.get().isProtectedBlock(level, targetPos)) {
                // Skip protected blocks to prevent griefing
                continue;
            }

            // Observable NPC builder interaction
            List<Villager> nearbyBuilders = level.getEntitiesOfClass(
                Villager.class,
                new AABB(origin).inflate(24.0),
                v -> v.isAlive()
            );
            if (!nearbyBuilders.isEmpty()) {
                Villager builder = nearbyBuilders.get(0);
                builder.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.1);
                builder.swingForAttack(InteractionHand.MAIN_HAND);
            }

            // Place block
            level.setBlock(targetPos, state, 3);
            if (state.is(Blocks.CHEST) || state.is(Blocks.BARREL) || state.is(Blocks.TRAPPED_CHEST)) {
                livingworld.worldgen.ContainerLootManager.get().seedBlueprintContainer(level, targetPos, blueprintId);
            }

            // Block placement sound and particles
            try {
                level.playSound(null, targetPos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
            } catch (Exception ignored) {
                // Sound/particle fallback
            }

            placedBlocks++;
            placedThisStep++;
            if (placedBlocks % 5 == 1 || remainingBlocks.isEmpty()) {
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Construction: Builder placed " + state.getBlock().getName().getString() + " at " + targetPos + " (" + placedBlocks + "/" + totalBlocks + ")");
            }
        }

        if (remainingBlocks.isEmpty()) {
            complete = true;
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Construction project " + blueprintId + " completed at " + origin + "!");
        }

        return placedThisStep;
    }
}
