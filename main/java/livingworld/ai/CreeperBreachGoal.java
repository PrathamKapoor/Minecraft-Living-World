package livingworld.ai;

import java.util.EnumSet;
import livingworld.config.FeatureConfig;
import livingworld.core.ProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CreeperBreachGoal extends Goal {
    private final Creeper creeper;
    private Vec3 lastKnownTargetPos = null;
    private BlockPos breachPos = null;
    private int evalCooldown = 0;

    public CreeperBreachGoal(Creeper creeper) {
        this.creeper = creeper;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!FeatureConfig.get().enableCreeperBreach) {
            return false;
        }

        if (evalCooldown > 0) {
            evalCooldown--;
            return false;
        }

        LivingEntity target = creeper.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (creeper.hasLineOfSight(target)) {
            lastKnownTargetPos = target.position();
            return false; // Normal creeper pursuit handles visible targets
        }

        // Target recently broke line of sight
        if (lastKnownTargetPos != null && creeper.distanceToSqr(lastKnownTargetPos) < 25.0) {
            // Find obstacle between creeper and last known position
            BlockPos midPoint = BlockPos.containing(creeper.position().add(lastKnownTargetPos).scale(0.5));
            BlockState state = creeper.level().getBlockState(midPoint);

            if (!state.isAir()) {
                if (creeper.level() instanceof ServerLevel serverLevel) {
                    if (ProtectionManager.get().isProtectedBlock(serverLevel, midPoint)) {
                        // Protected structure: cannot breach
                        evalCooldown = 60;
                        return false;
                    }
                }
                breachPos = midPoint;
                return true;
            }
        }

        evalCooldown = 20;
        return false;
    }

    @Override
    public void start() {
        if (breachPos != null) {
            creeper.getNavigation().moveTo(breachPos.getX() + 0.5, breachPos.getY(), breachPos.getZ() + 0.5, 1.2);
        }
    }

    @Override
    public void tick() {
        if (breachPos == null) return;

        double distSq = creeper.distanceToSqr(breachPos.getX() + 0.5, breachPos.getY(), breachPos.getZ() + 0.5);
        if (distSq < 4.0 && !creeper.isIgnited()) {
            // Initiate tactical breach explosion
            creeper.ignite();
        }
    }

    @Override
    public void stop() {
        breachPos = null;
    }
}
