package livingworld.entity;

import java.util.EnumSet;
import java.util.List;
import livingworld.faction.AllegianceRegistry;
import livingworld.faction.VillageDefenseController;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated Village Archer / Bow Guard Unit (Section 10, F05; v0.2.5 Living Village Rebuild).
 * Enforces ranged defense perimeter around settlements and elevated watchtowers:
 * - Wields Bow and Arrow with full 3D visible iron/diamond armor
 * - Tactical kiting AI maintaining standoff distance from melee threats
 * - Authentic bow drawing stance with startUsingItem(MAIN_HAND) and visible arrow releases
 * - Elevation defense and early-warning watchtower sentry reconnaissance
 * - Centralized allegiance eliminating friendly-fire with melee guards and golems
 */
public class VillageArcherEntity extends PathfinderMob implements RangedAttackMob {
    private VillageGuardEntity.GuardRank rank = VillageGuardEntity.GuardRank.SOLDIER;
    private String homeSettlementId = "";
    private boolean isWatchtowerSentry = false;

    public VillageArcherEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        equipStandardArcherGear();
        this.setCustomName(Component.literal("§9Village Archer"));
        this.setCustomNameVisible(false);

        if (!level.isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), homeSettlementId, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.ARCHER);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 28.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 48.0) // Expanded 48-block perception range
            .add(Attributes.ARMOR, 15.0)
            .add(Attributes.ARMOR_TOUGHNESS, 2.0);
    }

    private void equipStandardArcherGear() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05f);
    }

    public void promoteRank(VillageGuardEntity.GuardRank newRank) {
        this.rank = newRank;
        switch (newRank) {
            case VETERAN -> {
                this.setCustomName(Component.literal("§bVeteran Archer"));
            }
            case ELITE_GUARD -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setCustomName(Component.literal("§5Elite Sharpshooter"));
            }
            case ROYAL_GUARD -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                this.setCustomName(Component.literal("§6Royal Marksman"));
            }
            default -> {}
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Tactical Kiting Goal: backs away if target closes within 5 blocks
        this.goalSelector.addGoal(1, new TacticalKitingGoal(this, 1.25, 5.0));
        // Settlement Defense Rally: moves towards alarm coordinate if not in active combat
        this.goalSelector.addGoal(2, new DefendSettlementGoal(this));
        // Authentic Bow Combat Goal: manages bow draw, aiming, and tension curves
        this.goalSelector.addGoal(3, new VillageArcherBowAttackGoal(this, 1.0, 25, 32.0f));
        // Watchtower Sentry Lookout Goal: scans 48 blocks for hostiles and alerts settlement
        this.goalSelector.addGoal(4, new SentryLookoutGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Retaliate only against non-allied hostiles (friendly-fire filtering)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                LivingEntity attacker = VillageArcherEntity.this.getLastHurtByMob();
                return attacker != null && !AllegianceRegistry.get().areAllies(VillageArcherEntity.this, attacker);
            }
        });

        // Priority 2: Prioritize mutant threats from high ground / range
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, level) -> {
                if (target == null) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                return (id != null && "mutantmonsters".equals(id.getNamespace())) || target instanceof MutantZombieCommanderEntity;
            }));

        // Priority 3: Prioritize monsters threatening settlement
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, level) -> target != null && !AllegianceRegistry.get().areAllies(this, target)));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target == null) return false;
        if (AllegianceRegistry.get().areAllies(this, target)) return false;
        return super.canAttack(target);
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && AllegianceRegistry.get().areAllies(this, target)) {
            return; // Reject friendly allied target assignment
        }
        super.setTarget(target);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (target == null || AllegianceRegistry.get().areAllies(this, target)) return;

        ItemStack bow = this.getMainHandItem();
        ItemStack arrow = new ItemStack(Items.ARROW);
        AbstractArrow projectile = ProjectileUtil.getMobArrow(this, arrow, pullProgress, bow);

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333) - projectile.getY();
        double dz = target.getZ() - this.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Arc elevation calculation
        projectile.shoot(dx, dy + horizontalDist * 0.18, dz, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(projectile);
    }

    public VillageGuardEntity.GuardRank getRank() {
        return rank;
    }

    public String getHomeSettlementId() {
        return homeSettlementId;
    }

    public void setHomeSettlementId(String homeSettlementId) {
        this.homeSettlementId = homeSettlementId;
        if (homeSettlementId != null && !homeSettlementId.isEmpty() && !this.level().isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), homeSettlementId, "VILLAGE_ALLIANCE",
                isWatchtowerSentry ? AllegianceRegistry.MilitaryRole.WATCHTOWER_SENTRY : AllegianceRegistry.MilitaryRole.ARCHER);
        }
    }

    public boolean isWatchtowerSentry() {
        return isWatchtowerSentry;
    }

    public void setWatchtowerSentry(boolean watchtowerSentry) {
        this.isWatchtowerSentry = watchtowerSentry;
        if (this.homeSettlementId != null && !this.homeSettlementId.isEmpty() && !this.level().isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), homeSettlementId, "VILLAGE_ALLIANCE",
                watchtowerSentry ? AllegianceRegistry.MilitaryRole.WATCHTOWER_SENTRY : AllegianceRegistry.MilitaryRole.ARCHER);
        }
    }

    /**
     * Tactical Kiting AI: Maintains safe standoff distance when enemies close in.
     */
    public static class TacticalKitingGoal extends Goal {
        private final PathfinderMob mob;
        private final double speedModifier;
        private final double safeDistance;

        public TacticalKitingGoal(PathfinderMob mob, double speedModifier, double safeDistance) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.safeDistance = safeDistance;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return false;
            return mob.distanceToSqr(target) < (safeDistance * safeDistance);
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;

            // Compute retreat vector away from target
            Vec3 away = mob.position().subtract(target.position()).normalize().scale(3.0);
            Vec3 dest = mob.position().add(away);
            mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speedModifier);
            mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
    }

    /**
     * Sentry Early Warning Goal: Scans perimeter for hostiles and triggers goat horn alarms.
     */
    public static class SentryLookoutGoal extends Goal {
        private final VillageArcherEntity archer;
        private int scanCooldown = 0;

        public SentryLookoutGoal(VillageArcherEntity archer) {
            this.archer = archer;
        }

        @Override
        public boolean canUse() {
            return archer.isAlive() && archer.level() instanceof ServerLevel;
        }

        @Override
        public void tick() {
            if (++scanCooldown < 20) return; // Scan once per second
            scanCooldown = 0;

            if (!(archer.level() instanceof ServerLevel serverLevel)) return;
            Settlement settlement = SettlementManager.get().getNearestSettlement(archer.blockPosition());
            if (settlement == null) return;

            AABB scanArea = AABB.ofSize(archer.position(), 64, 24, 64);
            List<Monster> spottedHostiles = serverLevel.getEntitiesOfClass(Monster.class, scanArea,
                m -> m.isAlive() && !AllegianceRegistry.get().isVillageDefender(m));

            if (!spottedHostiles.isEmpty()) {
                Monster threat = spottedHostiles.get(0);
                VillageDefenseController.SettlementDefenseState state = VillageDefenseController.get().getState(settlement.getId());
                if (state.getThreatLevel() == VillageDefenseController.ThreatLevel.PEACEFUL || state.getThreatLevel() == VillageDefenseController.ThreatLevel.SUSPICIOUS) {
                    VillageDefenseController.get().reportThreat(serverLevel, settlement, threat.blockPosition(), threat, "Sentry Early-Warning Contact");
                }
            }
        }
    }

    /**
     * Settlement Defense Rally Goal: Guides archer toward elevated defense positions when village is attacked.
     */
    public static class DefendSettlementGoal extends Goal {
        private final PathfinderMob mob;

        public DefendSettlementGoal(PathfinderMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null && mob.getTarget().isAlive()) return false;
            if (!(mob.level() instanceof ServerLevel)) return false;
            Settlement settlement = SettlementManager.get().getNearestSettlement(mob.blockPosition());
            if (settlement == null) return false;
            VillageDefenseController.SettlementDefenseState state = VillageDefenseController.get().getState(settlement.getId());
            return state.getThreatLevel() == VillageDefenseController.ThreatLevel.UNDER_ATTACK && state.getActiveThreatPos() != null;
        }

        @Override
        public void tick() {
            Settlement settlement = SettlementManager.get().getNearestSettlement(mob.blockPosition());
            if (settlement != null) {
                VillageDefenseController.SettlementDefenseState state = VillageDefenseController.get().getState(settlement.getId());
                BlockPos threatPos = state.getActiveThreatPos();
                if (threatPos != null) {
                    mob.getNavigation().moveTo(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 1.2);
                }
            }
        }
    }

    /**
     * Dedicated Bow Combat AI for Village Archer (v0.2.4 & v0.2.5).
     * Replaces generic RangedAttackGoal with authentic bow drawing, aiming, and release mechanics.
     */
    public static class VillageArcherBowAttackGoal extends Goal {
        private final VillageArcherEntity archer;
        private final double speedModifier;
        private int attackIntervalMin;
        private final float attackRadiusSqr;
        private int attackTime = -1;
        private int seeTime;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        public VillageArcherBowAttackGoal(VillageArcherEntity archer, double speedModifier, int attackIntervalMin, float attackRadius) {
            this.archer = archer;
            this.speedModifier = speedModifier;
            this.attackIntervalMin = attackIntervalMin;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.archer.getTarget() != null && this.archer.isHolding(Items.BOW);
        }

        @Override
        public boolean canContinueToUse() {
            return (this.canUse() || !this.archer.getNavigation().isDone()) && this.archer.isHolding(Items.BOW);
        }

        @Override
        public void start() {
            super.start();
            this.archer.setAggressive(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.archer.setAggressive(false);
            this.seeTime = 0;
            this.attackTime = -1;
            this.archer.stopUsingItem();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.archer.getTarget();
            if (target == null) return;

            double distSqr = this.archer.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean hasLineOfSight = this.archer.getSensing().hasLineOfSight(target);

            if (hasLineOfSight) {
                ++this.seeTime;
            } else {
                this.seeTime = 0;
            }

            if (distSqr <= (double)this.attackRadiusSqr && this.seeTime >= 20) {
                this.archer.getNavigation().stop();
                ++this.strafingTime;
            } else {
                this.archer.getNavigation().moveTo(target, this.speedModifier);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if (this.archer.getRandom().nextFloat() < 0.3) {
                    this.strafingClockwise = !this.strafingClockwise;
                }
                if (this.archer.getRandom().nextFloat() < 0.3) {
                    this.strafingBackwards = !this.strafingBackwards;
                }
                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                if (distSqr > (double)(this.attackRadiusSqr * 0.75f)) {
                    this.strafingBackwards = false;
                } else if (distSqr < (double)(this.attackRadiusSqr * 0.25f)) {
                    this.strafingBackwards = true;
                }
                this.archer.getMoveControl().strafe(this.strafingBackwards ? -0.5f : 0.5f, this.strafingClockwise ? 0.5f : -0.5f);
                this.archer.lookAt(target, 30.0f, 30.0f);
            } else {
                this.archer.getLookControl().setLookAt(target, 30.0f, 30.0f);
            }

            // Authentic Bow Drawing and Firing
            if (this.archer.isUsingItem()) {
                if (!hasLineOfSight && this.seeTime < -60) {
                    this.archer.stopUsingItem();
                } else if (hasLineOfSight) {
                    int pullTicks = this.archer.getTicksUsingItem();
                    if (pullTicks >= 20) {
                        this.archer.stopUsingItem();
                        this.archer.performRangedAttack(target, BowItem.getPowerForTime(pullTicks));
                        this.attackTime = this.attackIntervalMin;
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.archer.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }
    }
}
