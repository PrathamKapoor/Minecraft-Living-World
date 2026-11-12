package livingworld.ai;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/**
 * Intelligent Fence Gate Navigation Goal for Village Guards, Archers, and Cavalry (Phase 2).
 *
 * Responsibilities:
 * 1. Path Interception: Detects closed fence gates obstructing the NPC's movement path.
 * 2. Bi-directional Passage: Opens the gate when approaching from either direction.
 * 3. Multi-NPC Coordination: Keeps gate open while fellow defenders are actively crossing,
 *    preventing door-slamming and thrashing when multiple NPCs use the same gate.
 * 4. Automatic Closure: Closes the gate after all NPCs have completely cleared the passage.
 * 5. Livestock Protection: Identifies and strictly avoids opening livestock pen gates,
 *    ensuring cattle, sheep, and pigs remain secure inside enclosures.
 * 6. Player Invariant: Does not lock gates or interfere with player manual interactions.
 */
public class FenceGateInteractGoal extends Goal {
    private final PathfinderMob mob;
    private BlockPos currentGatePos = null;
    private int ticksOpen = 0;
    private boolean openedByThis = false;

    // Shared registry of active gate users across all NPCs: GatePos -> Set of Mob UUIDs
    private static final Map<BlockPos, Set<UUID>> ACTIVE_GATE_USERS = new ConcurrentHashMap<>();

    public FenceGateInteractGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!mob.isAlive() || mob.getNavigation().isDone()) {
            return false;
        }

        Path path = mob.getNavigation().getPath();
        if (path == null || path.isDone()) {
            return false;
        }

        Level level = mob.level();
        BlockPos mobPos = mob.blockPosition();

        // Check path nodes ahead (current node, next node, up to 2 nodes ahead)
        int currentIdx = path.getNextNodeIndex();
        int maxIdx = Math.min(path.getNodeCount(), currentIdx + 3);

        for (int i = currentIdx; i < maxIdx; i++) {
            Node node = path.getNode(i);
            BlockPos nodePos = new BlockPos(node.x, node.y, node.z);

            if (isFenceGate(level, nodePos)) {
                if (isLivestockGate(level, nodePos)) {
                    return false; // Strictly protect livestock enclosures!
                }
                this.currentGatePos = nodePos.immutable();
                return true;
            }
        }

        // Fallback: check 1 block directly ahead in direction of movement
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos probe = mobPos.offset(dx, 0, dz);
                if (isFenceGate(level, probe)) {
                    if (isLivestockGate(level, probe)) {
                        continue;
                    }
                    this.currentGatePos = probe.immutable();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (currentGatePos == null || !mob.isAlive()) {
            return false;
        }
        Level level = mob.level();
        if (!isFenceGate(level, currentGatePos)) {
            return false;
        }
        // Keep active until mob has walked past the gate (distance > 2.5 blocks) or timed out (60 ticks)
        double distSq = mob.distanceToSqr(currentGatePos.getX() + 0.5, currentGatePos.getY(), currentGatePos.getZ() + 0.5);
        return distSq <= 9.0 && ticksOpen < 60;
    }

    @Override
    public void start() {
        ticksOpen = 0;
        openedByThis = false;
        if (currentGatePos == null) return;

        Level level = mob.level();
        BlockState state = level.getBlockState(currentGatePos);

        if (state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN)) {
            // Register this mob as an active user of the gate
            ACTIVE_GATE_USERS.computeIfAbsent(currentGatePos, k -> ConcurrentHashMap.newKeySet()).add(mob.getUUID());

            // Open the gate
            level.setBlock(currentGatePos, state.setValue(FenceGateBlock.OPEN, true), 10);
            level.gameEvent(mob, GameEvent.BLOCK_OPEN, currentGatePos);
            level.playSound(null, currentGatePos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
            openedByThis = true;
        } else {
            // Gate already open: register this mob as an active user
            ACTIVE_GATE_USERS.computeIfAbsent(currentGatePos, k -> ConcurrentHashMap.newKeySet()).add(mob.getUUID());
        }
    }

    @Override
    public void tick() {
        ticksOpen++;
        if (currentGatePos == null) return;

        // Ensure mob keeps moving through the gate
        mob.getLookControl().setLookAt(currentGatePos.getX() + 0.5, currentGatePos.getY() + 1.0, currentGatePos.getZ() + 0.5);
    }

    @Override
    public void stop() {
        if (currentGatePos == null) return;
        Level level = mob.level();

        // Deregister this mob
        Set<UUID> users = ACTIVE_GATE_USERS.get(currentGatePos);
        if (users != null) {
            users.remove(mob.getUUID());
        }

        // Check if other friendly NPCs are still actively using or approaching this gate
        boolean otherUsersPresent = false;
        if (users != null && !users.isEmpty()) {
            otherUsersPresent = true;
        } else {
            // Also scan for any other nearby guards/archers/cavalry within 3 blocks
            AABB gateArea = new AABB(currentGatePos).inflate(3.0);
            List<PathfinderMob> nearbyNPCs = level.getEntitiesOfClass(PathfinderMob.class, gateArea,
                other -> other.isAlive() && other != mob && (
                    other instanceof livingworld.entity.VillageGuardEntity ||
                    other instanceof livingworld.entity.VillageArcherEntity ||
                    other instanceof livingworld.entity.VillageCavalryEntity));
            if (!nearbyNPCs.isEmpty()) {
                otherUsersPresent = true;
            }
        }

        // If no other NPCs are using the gate, cleanly close it!
        if (!otherUsersPresent) {
            BlockState state = level.getBlockState(currentGatePos);
            if (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN)) {
                level.setBlock(currentGatePos, state.setValue(FenceGateBlock.OPEN, false), 10);
                level.gameEvent(mob, GameEvent.BLOCK_CLOSE, currentGatePos);
                level.playSound(null, currentGatePos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            ACTIVE_GATE_USERS.remove(currentGatePos);
        }

        currentGatePos = null;
        ticksOpen = 0;
        openedByThis = false;
    }

    private static boolean isFenceGate(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FenceGateBlock;
    }

    /**
     * Determines whether a fence gate belongs to an animal enclosure / livestock pen.
     * Guards and scouts must NEVER open livestock gates during patrols.
     */
    public static boolean isLivestockGate(Level level, BlockPos gatePos) {
        if (level == null || gatePos == null) return false;

        // 1. Check for nearby livestock animals within 6 blocks of the gate
        AABB searchBox = new AABB(gatePos).inflate(6.0);
        List<Animal> nearbyAnimals = level.getEntitiesOfClass(Animal.class, searchBox,
            a -> a.isAlive() && !a.isVehicle());
        if (!nearbyAnimals.isEmpty()) {
            return true; // Enclosure holds livestock!
        }

        // 2. Check for signature livestock pen blueprint blocks (hay bales, feeding troughs, cauldrons)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos p = gatePos.offset(dx, dy, dz);
                    BlockState bs = level.getBlockState(p);
                    if (bs.is(Blocks.HAY_BLOCK) || bs.is(Blocks.WATER_CAULDRON) || bs.is(Blocks.FARMLAND)) {
                        return true; // Part of a designated agricultural livestock enclosure
                    }
                }
            }
        }

        return false;
    }
}
