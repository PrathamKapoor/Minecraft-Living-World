package livingworld.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import livingworld.faction.AllegianceRegistry;
import livingworld.faction.VillageDefenseController;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated Village Archer / Bow Guard Unit (Section 10, F05; v0.2.6 Civilization Intelligence).
 * Enforces ranged perimeter defense and elevated watchtower reconnaissance:
 * - Wields Bow and Arrow with full 3D visible iron/diamond/netherite armor
 * - Tactical kiting AI maintaining standoff distance from melee threats
 * - Authentic bow drawing stance with startUsingItem(MAIN_HAND) and visible arrow releases
 * - Defensive Patrol Boundary State Machine: PATROL -> INVESTIGATE -> ENGAGE -> PURSUE -> DISENGAGE -> RETURN_TO_POST
 * - Persistence guarantee: setPersistenceRequired() + complete NBT serialization across chunk unloads
 * - Diagnostic removal telemetry distinguishing DEATH, DESPAWN, UNLOAD, DIMENSION_CHANGE
 * - Watchtower Sentry Horn Alarm: Sentry visibly equips goat horn, raises arm, plays localized audio, and alerts settlement
 * - Suppression of false alarms from subterranean or harmless distant mobs
 * - Centralized allegiance eliminating friendly-fire with melee guards and golems
 */
public class VillageArcherEntity extends PathfinderMob implements RangedAttackMob {
    private VillageGuardEntity.GuardRank rank = VillageGuardEntity.GuardRank.SOLDIER;
    private String homeSettlementId = "";
    private boolean isWatchtowerSentry = false;
    private BlockPos assignedPost = null;
    private double maxPursuitDistance = 32.0;
    private VillageGuardEntity.PatrolState patrolState = VillageGuardEntity.PatrolState.PATROL;
    private UUID companionDogUuid = null;
    private int lostSightTicks = 0;
    private int hornAnimationTicks = 0;
    private int hornCooldown = 0;

    public VillageArcherEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        equipStandardArcherGear();
        this.setCustomName(Component.literal("§9Village Archer"));
        this.setCustomNameVisible(false);
        this.setPersistenceRequired(); // Critical persistence: never despawn naturally (BUG-06)

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
        // Watchtower Sentry Alarm Goal: stops, faces threat, equips horn, plays localized audio
        this.goalSelector.addGoal(1, new SentryLookoutGoal(this));
        // Tactical Kiting Goal: backs away if melee target closes within 5 blocks
        this.goalSelector.addGoal(2, new TacticalKitingGoal(this, 1.25, 5.0));
        // Settlement Defense Rally: moves towards alarm coordinate if not in active combat
        this.goalSelector.addGoal(3, new DefendSettlementGoal(this));
        // Authentic Bow Combat Goal: manages bow draw, aiming, and tension curves
        this.goalSelector.addGoal(4, new VillageArcherBowAttackGoal(this, 1.0, 25, 32.0f));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Priority 1: Retaliate only against non-allied hostiles (friendly-fire filtering)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                LivingEntity attacker = VillageArcherEntity.this.getLastHurtByMob();
                return attacker != null && !AllegianceRegistry.get().areAllies(VillageArcherEntity.this, attacker);
            }
        });

        // Priority 2: Prioritize high-threat Mutant Monsters / Mutant Zombie Commander (BUG-07)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, lvl) -> {
                if (target == null || !target.isAlive()) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                if (!isWithinDefensivePerimeter(target.blockPosition())) return false;
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                return (id != null && "mutantmonsters".equals(id.getNamespace())) || target instanceof MutantZombieCommanderEntity;
            }));

        // Priority 3: Prioritize siege miners or hostiles threatening civilians
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, lvl) -> {
                if (target == null || !target.isAlive()) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                if (!isWithinDefensivePerimeter(target.blockPosition())) return false;
                if (target.getMainHandItem().is(Items.IRON_PICKAXE) || target.getMainHandItem().is(Items.DIAMOND_PICKAXE)) return true;
                return (target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() instanceof Villager);
            }));

        // Priority 4: Prioritize general monsters within defensive perimeter
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, lvl) -> {
                if (target == null || !target.isAlive()) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                return isWithinDefensivePerimeter(target.blockPosition());
            }));
    }

    private boolean isWithinDefensivePerimeter(BlockPos targetPos) {
        BlockPos post = getEffectivePost();
        double distSq = post.distToCenterSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        return distSq <= (maxPursuitDistance * maxPursuitDistance);
    }

    public BlockPos getEffectivePost() {
        if (assignedPost != null) return assignedPost;
        if (homeSettlementId != null && !homeSettlementId.isEmpty()) {
            Settlement settlement = SettlementManager.get().getSettlement(homeSettlementId);
            if (settlement != null) return settlement.getCenter();
        }
        return this.blockPosition();
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
        if (target != null && target.isAlive()) {
            this.patrolState = VillageGuardEntity.PatrolState.ENGAGE;
            this.lostSightTicks = 0;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            if (hornCooldown > 0) hornCooldown--;

            // Handle active horn blowing animation
            if (hornAnimationTicks > 0) {
                hornAnimationTicks--;
                if (hornAnimationTicks == 0) {
                    // Restore Bow after blowing horn
                    this.stopUsingItem();
                    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                }
            }

            // Patrol Boundary & Pursuit State Machine (BUG-05)
            updatePatrolStateMachine(this.getTarget());
        }
    }

    private void updatePatrolStateMachine(LivingEntity target) {
        BlockPos post = getEffectivePost();
        double distFromPost = Math.sqrt(this.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5));

        if (target != null && target.isAlive()) {
            if (this.hasLineOfSight(target)) {
                this.lostSightTicks = 0;
            } else {
                this.lostSightTicks++;
            }

            if (distFromPost > maxPursuitDistance) {
                disengageAndReturn("Pursuit distance limit exceeded (" + String.format("%.1f", distFromPost) + "m > " + maxPursuitDistance + "m)");
                return;
            }

            if (this.lostSightTicks > 120) {
                disengageAndReturn("Target lost from sight for > 6 seconds");
                return;
            }

            if (this.getHealth() < this.getMaxHealth() * 0.2f && distFromPost > 8.0) {
                disengageAndReturn("Critical injuries sustained, falling back to defensive perimeter");
                return;
            }

            this.patrolState = VillageGuardEntity.PatrolState.PURSUE;
        } else {
            if (this.patrolState == VillageGuardEntity.PatrolState.PURSUE || this.patrolState == VillageGuardEntity.PatrolState.ENGAGE) {
                this.patrolState = VillageGuardEntity.PatrolState.RETURN_TO_POST;
            }

            if (this.patrolState == VillageGuardEntity.PatrolState.RETURN_TO_POST) {
                if (distFromPost <= 4.0) {
                    this.patrolState = VillageGuardEntity.PatrolState.PATROL;
                    this.getNavigation().stop();
                } else if (this.tickCount % 20 == 0) {
                    this.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.2);
                }
            }
        }
    }

    private void disengageAndReturn(String reason) {
        this.setTarget(null);
        this.patrolState = VillageGuardEntity.PatrolState.DISENGAGE;
        this.lostSightTicks = 0;
        BlockPos post = getEffectivePost();
        this.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.25);
        this.patrolState = VillageGuardEntity.PatrolState.RETURN_TO_POST;
        livingworld.LivingWorldMod.LOGGER.debug("[Living World] Archer " + this.getUUID() + " disengaged: " + reason);
    }

    /**
     * Performs authentic visible horn alarm (BUG-01 & BUG-02):
     * Equips Goat Horn, plays arm raising animation, sounds horn from sentry's position, and alerts settlement.
     */
    public void performHornAlarm(BlockPos threatPos, LivingEntity threatSource, String description) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Settlement settlement = SettlementManager.get().getNearestSettlement(this.blockPosition());
        if (settlement == null) return;

        this.hornCooldown = 900; // 45 seconds cooldown to prevent alarm spam
        this.hornAnimationTicks = 30; // 1.5 seconds visible horn raise
        this.getNavigation().stop();
        this.getLookControl().setLookAt(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 60.0f, 60.0f);

        // Visibly equip goat horn in main hand and start using it
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOAT_HORN));
        this.startUsingItem(InteractionHand.MAIN_HAND);

        // Sound originates directly from this Sentry's world coordinate
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.NEUTRAL, 3.5f, 0.95f);
        serverLevel.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 2.2, this.getZ(), 10, 0.6, 0.6, 0.6, 0.15);

        // Notify Village Defense Controller
        VillageDefenseController.get().reportThreat(serverLevel, settlement, threatPos, threatSource, description);
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Watchtower Sentry at " + this.blockPosition() + " visibly sounded goat-horn alarm! Threat: " + description);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (target == null || AllegianceRegistry.get().areAllies(this, target)) return;

        ItemStack bow = this.getMainHandItem();
        if (!bow.is(Items.BOW)) return; // Guard against shooting while holding goat horn

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

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("HomeSettlementId", this.homeSettlementId != null ? this.homeSettlementId : "");
        output.putString("GuardRank", this.rank.name());
        output.putBoolean("IsWatchtowerSentry", this.isWatchtowerSentry);
        output.putString("PatrolState", this.patrolState.name());
        output.putDouble("MaxPursuitDistance", this.maxPursuitDistance);
        output.putBoolean("HasAssignedPost", this.assignedPost != null);
        if (this.assignedPost != null) {
            output.putInt("PostX", this.assignedPost.getX());
            output.putInt("PostY", this.assignedPost.getY());
            output.putInt("PostZ", this.assignedPost.getZ());
        }
        output.putString("CompanionDogUuid", this.companionDogUuid != null ? this.companionDogUuid.toString() : "");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.homeSettlementId = input.getStringOr("HomeSettlementId", "");
        String rankStr = input.getStringOr("GuardRank", "SOLDIER");
        try {
            this.rank = VillageGuardEntity.GuardRank.valueOf(rankStr);
        } catch (Exception ignored) {}

        this.isWatchtowerSentry = input.getBooleanOr("IsWatchtowerSentry", false);
        String stateStr = input.getStringOr("PatrolState", "PATROL");
        try {
            this.patrolState = VillageGuardEntity.PatrolState.valueOf(stateStr);
        } catch (Exception ignored) {}

        this.maxPursuitDistance = input.getDoubleOr("MaxPursuitDistance", 32.0);

        if (input.getBooleanOr("HasAssignedPost", false)) {
            int px = input.getIntOr("PostX", 0);
            int py = input.getIntOr("PostY", 0);
            int pz = input.getIntOr("PostZ", 0);
            this.assignedPost = new BlockPos(px, py, pz);
        }

        String dogStr = input.getStringOr("CompanionDogUuid", "");
        if (!dogStr.isEmpty()) {
            try {
                this.companionDogUuid = UUID.fromString(dogStr);
            } catch (Exception ignored) {}
        }

        if (!this.homeSettlementId.isEmpty() && !this.level().isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), this.homeSettlementId, "VILLAGE_ALLIANCE",
                isWatchtowerSentry ? AllegianceRegistry.MilitaryRole.WATCHTOWER_SENTRY : AllegianceRegistry.MilitaryRole.ARCHER);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        // Telemetry distinguishing DEATH, DESPAWN, UNLOAD, DIMENSION_CHANGE (BUG-06)
        if (reason == Entity.RemovalReason.KILLED) {
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Military Casualty: Archer " + this.getUUID() + " (" + this.rank + ") killed in combat at " + this.blockPosition() + " (DEATH)");
            if (this.homeSettlementId != null && !this.homeSettlementId.isEmpty()) {
                AllegianceRegistry.get().unregister(this.getUUID());
            }
        } else if (reason == Entity.RemovalReason.DISCARDED) {
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Archer " + this.getUUID() + " discarded (DESPAWN/DISCARD)");
            if (this.homeSettlementId != null && !this.homeSettlementId.isEmpty()) {
                AllegianceRegistry.get().unregister(this.getUUID());
            }
        } else if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            livingworld.LivingWorldMod.LOGGER.debug("[Living World] Archer " + this.getUUID() + " safely serialized to chunk at " + this.blockPosition() + " (UNLOAD)");
        } else if (reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Archer " + this.getUUID() + " transferred dimension (DIMENSION_CHANGE)");
        }
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

    public BlockPos getAssignedPost() {
        return assignedPost;
    }

    public void setAssignedPost(BlockPos post) {
        this.assignedPost = post;
    }

    public double getMaxPursuitDistance() {
        return maxPursuitDistance;
    }

    public void setMaxPursuitDistance(double dist) {
        this.maxPursuitDistance = dist;
    }

    public VillageGuardEntity.PatrolState getPatrolState() {
        return patrolState;
    }

    public void setPatrolState(VillageGuardEntity.PatrolState state) {
        this.patrolState = state;
    }

    public UUID getCompanionDogUuid() {
        return companionDogUuid;
    }

    public void setCompanionDogUuid(UUID dogUuid) {
        this.companionDogUuid = dogUuid;
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
     * Sentry Early Warning Goal (BUG-01 & BUG-02):
     * Performs strict threat confirmation before sounding alarm. Suppresses alarms for subterranean/harmless mobs.
     */
    public static class SentryLookoutGoal extends Goal {
        private final VillageArcherEntity archer;
        private int scanCooldown = 0;

        public SentryLookoutGoal(VillageArcherEntity archer) {
            this.archer = archer;
        }

        @Override
        public boolean canUse() {
            return archer.isAlive() && archer.hornCooldown <= 0 && archer.level() instanceof ServerLevel;
        }

        @Override
        public void tick() {
            if (++scanCooldown < 25) return; // Scan once every 1.25 seconds
            scanCooldown = 0;

            if (!(archer.level() instanceof ServerLevel serverLevel)) return;
            Settlement settlement = SettlementManager.get().getNearestSettlement(archer.blockPosition());
            if (settlement == null) return;

            // Strict line of sight and credible threat confirmation
            AABB scanArea = AABB.ofSize(archer.position(), 48, 20, 48);
            List<Monster> candidates = serverLevel.getEntitiesOfClass(Monster.class, scanArea,
                m -> m.isAlive() && !AllegianceRegistry.get().isVillageDefender(m));

            for (Monster monster : candidates) {
                // Must have line of sight to confirm threat
                if (!archer.hasLineOfSight(monster)) continue;

                // Eliminate subterranean/cave mobs (> 8 blocks below sentry or in dark pits)
                if (monster.getY() < archer.getY() - 8.0) continue;

                // Check credible threat criteria:
                boolean isMutant = monster instanceof MutantZombieCommanderEntity;
                if (!isMutant) {
                    Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(monster.getType());
                    isMutant = (id != null && "mutantmonsters".equals(id.getNamespace()));
                }

                boolean isSiegeMiner = monster.getMainHandItem().is(Items.IRON_PICKAXE) || monster.getMainHandItem().is(Items.DIAMOND_PICKAXE);
                boolean threatensCivilian = monster.getTarget() instanceof Villager;
                double distSq = archer.distanceToSqr(monster);

                if (isMutant || isSiegeMiner || threatensCivilian || distSq <= (20.0 * 20.0)) {
                    // Confirmed credible threat: visibly blow the horn
                    String desc = isMutant ? "Mutant Boss Incursion" : (isSiegeMiner ? "Siege Breaching Squad" : "Approaching Hostile Force");
                    archer.performHornAlarm(monster.blockPosition(), monster, desc);
                    break;
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
     * Dedicated Bow Combat AI for Village Archer.
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
            return this.archer.getTarget() != null && this.archer.isHolding(Items.BOW) && this.archer.hornAnimationTicks <= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return (this.canUse() || !this.archer.getNavigation().isDone()) && this.archer.isHolding(Items.BOW) && this.archer.hornAnimationTicks <= 0;
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
