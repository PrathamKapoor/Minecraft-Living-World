package livingworld.ai;

import java.util.EnumSet;
import java.util.List;
import livingworld.config.FeatureConfig;
import livingworld.core.ProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Intelligent Zombie Siege Mining AI (Addendum Requirement 2).
 * Allows tool-bearing zombies (pickaxe, axe, shovel) to detect barriers,
 * mine path obstacles with block crack animations/sounds, coordinate hordes,
 * and pursue players/villagers through the created breach.
 */
public class ZombieSiegeMiningGoal extends Goal {
    private final Zombie zombie;
    private BlockPos miningBlockPos = null;
    private int miningTicks = 0;
    private int maxMiningTicks = 80;
    private int pathFailTicks = 0;
    private int scanCooldown = 0;

    public ZombieSiegeMiningGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public static boolean isEligibleMiner(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) return false;
        ItemStack mainHand = zombie.getItemBySlot(EquipmentSlot.MAINHAND);
        if (mainHand.isEmpty()) return false;
        return mainHand.is(ItemTags.PICKAXES)
            || mainHand.is(ItemTags.AXES)
            || mainHand.is(ItemTags.SHOVELS);
    }

    public static ToolType getToolType(Zombie zombie) {
        ItemStack mainHand = zombie.getItemBySlot(EquipmentSlot.MAINHAND);
        if (mainHand.is(ItemTags.PICKAXES)) return ToolType.PICKAXE;
        if (mainHand.is(ItemTags.AXES)) return ToolType.AXE;
        if (mainHand.is(ItemTags.SHOVELS)) return ToolType.SHOVEL;
        return ToolType.NONE;
    }

    public enum ToolType {
        NONE, PICKAXE, AXE, SHOVEL
    }

    @Override
    public boolean canUse() {
        if (!FeatureConfig.get().zombieMiningEnabled) return false;
        if (!isEligibleMiner(zombie)) return false;
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return false;

        // Respect mobGriefing rule
        if (FeatureConfig.get().respectMobGriefingRule && !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }

        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) return false;

        double distSq = zombie.distanceToSqr(target);
        int maxDist = FeatureConfig.get().maxMiningDistance;
        if (distSq > (maxDist * maxDist)) return false;

        // Try standard navigation first
        if (zombie.getNavigation().getPath() != null && zombie.getNavigation().getPath().canReach()) {
            pathFailTicks = 0;
            return false;
        }

        pathFailTicks++;
        if (pathFailTicks < 15) {
            return false; // Allow normal pathfinding attempts
        }

        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }
        scanCooldown = 10;

        BlockPos candidate = findBestMiningBlock(serverLevel, target);
        if (candidate != null) {
            miningBlockPos = candidate;
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (miningBlockPos == null) return false;
        if (!zombie.isAlive()) return false;
        if (!isEligibleMiner(zombie)) return false;
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return false;

        if (FeatureConfig.get().respectMobGriefingRule && !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }

        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) return false;

        BlockState state = serverLevel.getBlockState(miningBlockPos);
        if (state.isAir()) return false;

        // Check distance to mining block
        return zombie.distanceToSqr(miningBlockPos.getX() + 0.5, miningBlockPos.getY() + 0.5, miningBlockPos.getZ() + 0.5) <= 12.0;
    }

    @Override
    public void start() {
        miningTicks = 0;
        if (miningBlockPos == null || !(zombie.level() instanceof ServerLevel serverLevel)) return;

        BlockState state = serverLevel.getBlockState(miningBlockPos);
        maxMiningTicks = calculateBreakDuration(state, getToolType(zombie));

        zombie.getNavigation().moveTo(miningBlockPos.getX() + 0.5, miningBlockPos.getY(), miningBlockPos.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        if (miningBlockPos == null || !(zombie.level() instanceof ServerLevel serverLevel)) return;

        BlockState state = serverLevel.getBlockState(miningBlockPos);
        if (state.isAir()) {
            stop();
            return;
        }

        // Face the block being mined
        zombie.getLookControl().setLookAt(miningBlockPos.getX() + 0.5, miningBlockPos.getY() + 0.5, miningBlockPos.getZ() + 0.5, 30.0f, 30.0f);
        miningTicks++;

        // Render progressive crack stage (0 to 9)
        int crackStage = Math.min(9, (int) ((float) miningTicks / (float) maxMiningTicks * 10.0f));
        serverLevel.destroyBlockProgress(zombie.getId(), miningBlockPos, crackStage);

        // Sound effects and arm swinging every 4 ticks
        if (miningTicks % 4 == 0) {
            zombie.swingForAttack(InteractionHand.MAIN_HAND);
            serverLevel.playSound(null, miningBlockPos, state.getSoundType().getHitSound(), SoundSource.HOSTILE, 0.75f, 1.0f);

            // Broadcast mining alert to gather nearby zombies
            coordinateNearbyHorde(serverLevel, miningBlockPos);
        }

        // Block destroyed
        if (miningTicks >= maxMiningTicks) {
            serverLevel.destroyBlockProgress(zombie.getId(), miningBlockPos, -1);
            serverLevel.destroyBlock(miningBlockPos, true, zombie);

            // Damage held tool
            ItemStack tool = zombie.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!tool.isEmpty()) {
                tool.hurtAndBreak(1, zombie, EquipmentSlot.MAINHAND);
            }

            // Tactical opening breach: alert nearby horde to rush through opening
            LivingEntity target = zombie.getTarget();
            if (target != null) {
                hordeBreachCharge(serverLevel, miningBlockPos, target);
                zombie.getNavigation().moveTo(target, 1.25);
            }

            stop();
        }
    }

    @Override
    public void stop() {
        if (miningBlockPos != null && zombie.level() instanceof ServerLevel serverLevel) {
            serverLevel.destroyBlockProgress(zombie.getId(), miningBlockPos, -1);
        }
        miningBlockPos = null;
        miningTicks = 0;
        pathFailTicks = 0;
    }

    public BlockPos getMiningBlockPos() {
        return miningBlockPos;
    }

    public int getMiningTicks() {
        return miningTicks;
    }

    public int getMaxMiningTicks() {
        return maxMiningTicks;
    }

    private BlockPos findBestMiningBlock(ServerLevel serverLevel, LivingEntity target) {
        ToolType tool = getToolType(zombie);
        if (tool == ToolType.NONE) return null;

        Vec3 zombiePos = zombie.position();
        Vec3 targetPos = target.position();
        Vec3 toTarget = targetPos.subtract(zombiePos).normalize();

        BlockPos feetPos = zombie.blockPosition();
        Direction faceDir = zombie.getDirection();

        BlockPos[] candidates = new BlockPos[] {
            feetPos.relative(faceDir),
            feetPos.relative(faceDir).above(),
            feetPos.above(),
            feetPos.relative(faceDir, 2),
            feetPos.relative(faceDir.getClockWise()),
            feetPos.relative(faceDir.getCounterClockWise())
        };

        BlockPos bestBlock = null;
        float lowestHardness = Float.MAX_VALUE;

        for (BlockPos pos : candidates) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.isAir()) continue;

            // Protection check: never break claimed or protected infrastructure
            if (ProtectionManager.get().isProtectedBlock(serverLevel, pos)) continue;

            // Never break unbreakable blocks
            float hardness = state.getDestroySpeed(serverLevel, pos);
            if (hardness < 0.0f) continue;

            // Check if tool is compatible with this block
            if (isToolCompatible(state, tool)) {
                if (hardness < lowestHardness) {
                    lowestHardness = hardness;
                    bestBlock = pos.immutable();
                }
            }
        }

        return bestBlock;
    }

    public static boolean isToolCompatible(BlockState state, ToolType tool) {
        if (tool == ToolType.PICKAXE) {
            return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)
                || state.is(net.minecraft.world.level.block.Blocks.STONE)
                || state.is(net.minecraft.world.level.block.Blocks.SANDSTONE)
                || state.is(net.minecraft.world.level.block.Blocks.BRICKS)
                || state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE);
        } else if (tool == ToolType.AXE) {
            return state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)
                || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS)
                || state.is(net.minecraft.world.level.block.Blocks.BIRCH_PLANKS)
                || state.is(net.minecraft.world.level.block.Blocks.OAK_DOOR)
                || state.is(net.minecraft.world.level.block.Blocks.OAK_TRAPDOOR)
                || state.is(net.minecraft.world.level.block.Blocks.OAK_LOG);
        } else if (tool == ToolType.SHOVEL) {
            return state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(net.minecraft.world.level.block.Blocks.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.SAND)
                || state.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                || state.is(net.minecraft.world.level.block.Blocks.MUD)
                || state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK);
        }
        return false;
    }

    private int calculateBreakDuration(BlockState state, ToolType tool) {
        float hardness = state.getDestroySpeed(zombie.level(), miningBlockPos);
        if (hardness <= 0.5f) {
            return 30; // 1.5 seconds for soft blocks (dirt, sand)
        } else if (hardness <= 2.0f) {
            return 60; // 3 seconds for wood / cobblestone with appropriate tool
        } else {
            return 100; // 5 seconds for hard stone
        }
    }

    private void coordinateNearbyHorde(ServerLevel level, BlockPos breachPos) {
        AABB gatherArea = AABB.ofSize(new Vec3(breachPos.getX() + 0.5, breachPos.getY() + 0.5, breachPos.getZ() + 0.5), 24, 8, 24);
        List<Zombie> nearbyZombies = level.getEntitiesOfClass(Zombie.class, gatherArea, z -> z.isAlive() && z != zombie && !isEligibleMiner(z));

        for (Zombie z : nearbyZombies) {
            if (z.getTarget() == zombie.getTarget()) {
                // Move towards breach point
                if (z.distanceToSqr(breachPos.getX() + 0.5, breachPos.getY(), breachPos.getZ() + 0.5) > 6.0) {
                    z.getNavigation().moveTo(breachPos.getX() + 0.5, breachPos.getY(), breachPos.getZ() + 0.5, 1.15);
                }
            }
        }
    }

    private void hordeBreachCharge(ServerLevel level, BlockPos breachPos, LivingEntity target) {
        AABB chargeArea = AABB.ofSize(new Vec3(breachPos.getX() + 0.5, breachPos.getY() + 0.5, breachPos.getZ() + 0.5), 32, 12, 32);
        List<Zombie> nearbyZombies = level.getEntitiesOfClass(Zombie.class, chargeArea, z -> z.isAlive());

        for (Zombie z : nearbyZombies) {
            z.setTarget(target);
            z.getNavigation().moveTo(target, 1.3);
        }
    }
}
