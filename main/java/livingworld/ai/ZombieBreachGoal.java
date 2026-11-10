package livingworld.ai;

import java.util.EnumSet;
import livingworld.config.FeatureConfig;
import livingworld.core.ProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ZombieBreachGoal extends Goal {
    private final Zombie zombie;
    private BlockPos doorPos = null;
    private int breakProgress = 0;
    private int maxProgress = 160;

    public ZombieBreachGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!FeatureConfig.get().enableZombieDoorBreach || !FeatureConfig.get().enableZombieIntelligence) {
            return false;
        }

        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (zombie.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }

        // Look for doors/trapdoors directly adjacent to zombie blocking path
        BlockPos feetPos = zombie.blockPosition();
        BlockPos[] checkPositions = new BlockPos[] {
            feetPos.relative(zombie.getDirection()),
            feetPos.above().relative(zombie.getDirection()),
            feetPos
        };

        for (BlockPos pos : checkPositions) {
            BlockState state = zombie.level().getBlockState(pos);
            if (isEligibleBreachObstacle(state)) {
                if (zombie.level() instanceof ServerLevel serverLevel && ProtectionManager.get().isProtectedBlock(serverLevel, pos)) {
                    // Respect player protected structures
                    return false;
                }
                doorPos = pos.immutable();
                return true;
            }
        }

        return false;
    }

    private boolean isEligibleBreachObstacle(BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            return !state.getValue(DoorBlock.OPEN);
        }
        if (state.getBlock() instanceof TrapDoorBlock) {
            return !state.getValue(TrapDoorBlock.OPEN);
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (doorPos == null) return false;
        if (!zombie.isAlive()) return false;
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (zombie.distanceToSqr(doorPos.getX() + 0.5, doorPos.getY(), doorPos.getZ() + 0.5) > 6.0) return false;

        BlockState state = zombie.level().getBlockState(doorPos);
        return isEligibleBreachObstacle(state);
    }

    @Override
    public void start() {
        breakProgress = 0;
        Difficulty diff = zombie.level().getDifficulty();
        if (diff == Difficulty.EASY) {
            maxProgress = 240; // 12 seconds
        } else if (diff == Difficulty.NORMAL) {
            maxProgress = 160; // 8 seconds
        } else {
            maxProgress = 100; // 5 seconds
        }
        zombie.getNavigation().moveTo(doorPos.getX() + 0.5, doorPos.getY(), doorPos.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        if (doorPos == null) return;

        zombie.getLookControl().setLookAt(doorPos.getX() + 0.5, doorPos.getY() + 0.5, doorPos.getZ() + 0.5, 30.0f, 30.0f);
        breakProgress++;

        int crackIndex = (int) ((float) breakProgress / (float) maxProgress * 10.0f);
        zombie.level().destroyBlockProgress(zombie.getId(), doorPos, crackIndex);

        // Periodic door strike sound
        if (breakProgress % 20 == 0) {
            zombie.level().levelEvent(1019, doorPos, 0); // Zombie hitting door sound
            zombie.swingForAttack(InteractionHand.MAIN_HAND);
        }

        if (breakProgress >= maxProgress) {
            zombie.level().destroyBlockProgress(zombie.getId(), doorPos, -1);
            zombie.level().levelEvent(1021, doorPos, 0); // Zombie broken door sound
            zombie.level().destroyBlock(doorPos, false, zombie, 512);
            stop();
        }
    }

    @Override
    public void stop() {
        if (doorPos != null) {
            zombie.level().destroyBlockProgress(zombie.getId(), doorPos, -1);
            doorPos = null;
        }
        breakProgress = 0;
    }
}
