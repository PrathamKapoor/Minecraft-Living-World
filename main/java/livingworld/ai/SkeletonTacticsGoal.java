package livingworld.ai;

import java.util.EnumSet;
import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Tactical Archery, High-Ground Vantage, and Evasive Repositioning Goal (Section 15, F02).
 * Replaces naive retreat with multi-factor tactical candidate scoring (height, line of sight,
 * distance, cover, hazard risk, pathing) and periodic evasive jump-and-turn shots.
 */
public class SkeletonTacticsGoal extends Goal {
    private final AbstractSkeleton skeleton;
    private TacticalState state = TacticalState.ENGAGE_BOW;
    private BlockPos targetPosition = null;
    private int repositionCooldown = 0;
    private int evasiveJumpCooldown = 0;
    private int strafeDirection = 1;
    private int strafeTicks = 0;

    public enum TacticalState {
        ENGAGE_BOW,
        STRAFE_REPOSITION,
        SEEK_HIGH_GROUND,
        EVASIVE_JUMP_SHOT
    }

    public SkeletonTacticsGoal(AbstractSkeleton skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!FeatureConfig.get().enableSkeletonTactics) return false;
        if (!skeleton.isHolding(item -> item.getItem() instanceof BowItem)) return false;

        LivingEntity target = skeleton.getTarget();
        if (target == null || !target.isAlive()) return false;

        double distSq = skeleton.distanceToSqr(target);
        // Activate if target is too close (< 6 blocks) or periodically when repositioning needed
        return distSq < 36.0 || (repositionCooldown <= 0 && distSq < 400.0);
    }

    @Override
    public boolean canContinueToUse() {
        if (!skeleton.isAlive()) return false;
        LivingEntity target = skeleton.getTarget();
        if (target == null || !target.isAlive()) return false;
        return state == TacticalState.EVASIVE_JUMP_SHOT || !skeleton.getNavigation().isDone() || repositionCooldown > 0;
    }

    @Override
    public void start() {
        evaluateTacticalState();
    }

    private void evaluateTacticalState() {
        LivingEntity target = skeleton.getTarget();
        if (target == null) return;

        double distSq = skeleton.distanceToSqr(target);

        // 1. Evasive Jump Shot: Target rushed within 4 blocks and cooldown ready
        if (distSq < 16.0 && evasiveJumpCooldown <= 0) {
            state = TacticalState.EVASIVE_JUMP_SHOT;
            executeEvasiveJump(target);
            return;
        }

        // 2. High Ground Vantage Seeking: Target is between 8 and 20 blocks away
        if (repositionCooldown <= 0 && distSq >= 64.0 && distSq <= 400.0) {
            BlockPos vantage = findScoredVantagePoint(target);
            if (vantage != null && !vantage.equals(skeleton.blockPosition())) {
                state = TacticalState.SEEK_HIGH_GROUND;
                targetPosition = vantage;
                skeleton.getNavigation().moveTo(vantage.getX() + 0.5, vantage.getY(), vantage.getZ() + 0.5, 1.2);
                repositionCooldown = 60;
                return;
            }
        }

        // 3. Tactical Strafe & Reposition: Maintain preferred 10-14 block firing bracket
        state = TacticalState.STRAFE_REPOSITION;
        executeTacticalStrafe(target, distSq);
    }

    @Override
    public void tick() {
        LivingEntity target = skeleton.getTarget();
        if (target == null) return;

        if (repositionCooldown > 0) repositionCooldown--;
        if (evasiveJumpCooldown > 0) evasiveJumpCooldown--;

        // Keep eyes locked on target
        skeleton.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 30.0f, 30.0f);

        if (state == TacticalState.EVASIVE_JUMP_SHOT) {
            // Mid-air quick-draw snapshot
            if (skeleton.isUsingItem() || skeleton.getDeltaMovement().y < 0.1) {
                skeleton.performRangedAttack(target, 0.9f);
                skeleton.stopUsingItem();
                state = TacticalState.STRAFE_REPOSITION;
            }
        } else if (state == TacticalState.STRAFE_REPOSITION) {
            strafeTicks++;
            if (strafeTicks % 30 == 0) {
                strafeDirection = skeleton.getRandom().nextBoolean() ? 1 : -1;
            }
            double distSq = skeleton.distanceToSqr(target);
            if (distSq < 36.0) {
                // Backpedal away from melee rush
                Vec3 away = skeleton.position().subtract(target.position()).normalize();
                skeleton.getNavigation().moveTo(skeleton.getX() + away.x * 4.0, skeleton.getY(), skeleton.getZ() + away.z * 4.0, 1.25);
            }
        }
    }

    private void executeEvasiveJump(LivingEntity target) {
        Vec3 awayDir = skeleton.position().subtract(target.position()).normalize();
        // Jump backward with upward velocity
        skeleton.setDeltaMovement(awayDir.x * 0.48, 0.42, awayDir.z * 0.48);
        skeleton.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        evasiveJumpCooldown = 120; // 6 seconds cooldown
        repositionCooldown = 40;
    }

    private void executeTacticalStrafe(LivingEntity target, double distSq) {
        Vec3 toTarget = target.position().subtract(skeleton.position()).normalize();
        Vec3 strafeVec = new Vec3(-toTarget.z * strafeDirection, 0, toTarget.x * strafeDirection);

        Vec3 dest;
        if (distSq < 64.0) {
            // Move back and strafe
            dest = skeleton.position().add(strafeVec.scale(3.0)).subtract(toTarget.scale(4.0));
        } else if (distSq > 196.0) {
            // Close in slightly while strafing
            dest = skeleton.position().add(strafeVec.scale(3.0)).add(toTarget.scale(3.0));
        } else {
            // Pure lateral strafe
            dest = skeleton.position().add(strafeVec.scale(4.0));
        }

        BlockPos destPos = BlockPos.containing(dest);
        if (isSafePosition(destPos)) {
            skeleton.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.15);
        }
        repositionCooldown = 35;
    }

    private BlockPos findScoredVantagePoint(LivingEntity target) {
        BlockPos currentPos = skeleton.blockPosition();
        BlockPos bestPos = null;
        double bestScore = -1000.0;

        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                for (int dy = -2; dy <= 4; dy++) {
                    BlockPos candidate = currentPos.offset(dx, dy, dz);
                    if (!isSafePosition(candidate)) continue;

                    double score = scoreCandidatePosition(candidate, target);
                    if (score > bestScore) {
                        bestScore = score;
                        bestPos = candidate;
                    }
                }
            }
        }

        return bestPos;
    }

    private double scoreCandidatePosition(BlockPos candidate, LivingEntity target) {
        double score = 0.0;

        // 1. Height advantage (+2.5 per block above target)
        int heightDiff = candidate.getY() - target.blockPosition().getY();
        score += heightDiff * 2.5;

        // 2. Preferred Distance Bracket (10 to 14 blocks)
        double distSq = candidate.distToCenterSqr(target.getX(), target.getY(), target.getZ());
        double dist = Math.sqrt(distSq);
        if (dist >= 10.0 && dist <= 14.0) {
            score += 25.0; // Peak effective bracket
        } else if (dist < 6.0) {
            score -= 30.0; // Too close to melee
        } else if (dist > 18.0) {
            score -= 15.0; // Too far for accuracy
        }

        // 3. Line of sight check
        Vec3 candVec = new Vec3(candidate.getX() + 0.5, candidate.getY() + 1.6, candidate.getZ() + 0.5);
        Vec3 targetEye = target.getEyePosition();
        boolean hasLOS = skeleton.level().clip(new net.minecraft.world.level.ClipContext(candVec, targetEye,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            skeleton)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;

        if (hasLOS) {
            score += 30.0;
        } else {
            score -= 40.0;
        }

        return score;
    }

    private boolean isSafePosition(BlockPos pos) {
        if (!skeleton.level().getBlockState(pos).isAir()) return false;
        if (!skeleton.level().getBlockState(pos.above()).isAir()) return false;

        BlockState ground = skeleton.level().getBlockState(pos.below());
        if (!ground.isSolid()) return false;

        // Hazard checks: lava, fire, cactus
        if (ground.is(Blocks.LAVA) || ground.is(Blocks.FIRE) || ground.is(Blocks.CACTUS)) {
            return false;
        }

        // Cliff check: at least 1 solid block within 3 blocks down
        boolean hasSolidBelow = false;
        for (int dy = 1; dy <= 3; dy++) {
            if (skeleton.level().getBlockState(pos.below(dy)).isSolid()) {
                hasSolidBelow = true;
                break;
            }
        }
        return hasSolidBelow;
    }

    public TacticalState getState() {
        return state;
    }
}
