package livingworld.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import livingworld.faction.AllegianceRegistry;
import livingworld.faction.VillageDefenseController;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Dedicated Village Guard Unit (Section 10, F05; v0.2.6 Civilization Intelligence).
 * Enforces iron/diamond-tier defense standards; recruits, equips, and patrols settlement perimeters.
 * Features:
 * - Visible 3D iron/diamond/netherite armor, iron sword, tactical shield blocking defense
 * - Defensive Patrol Boundary State Machine: PATROL -> INVESTIGATE -> ENGAGE -> PURSUE -> DISENGAGE -> RETURN_TO_POST
 * - Persistence guarantee: setPersistenceRequired() + complete NBT serialization across chunk unloads
 * - Diagnostic removal telemetry distinguishing DEATH, DESPAWN, UNLOAD, DIMENSION_CHANGE
 * - Context-sensitive threat prioritization (Mutants, Siege Miners, Civilian Defenders)
 * - Centralized allegiance eliminating friendly-fire with fellow guards, archers, and golems
 */
public class VillageGuardEntity extends PathfinderMob {
    private GuardRank rank = GuardRank.SOLDIER;
    private String homeSettlementId = "";
    private BlockPos assignedPost = null;
    private double maxPursuitDistance = 24.0;
    private PatrolState patrolState = PatrolState.PATROL;
    private UUID companionDogUuid = null;
    private int lostSightTicks = 0;
    private long lastTargetSwitchTick = 0;
    private int targetSwitchCount = 0;
    private String lastOrientationReason = "Patrol";

    public enum GuardRank {
        RECRUIT, SOLDIER, VETERAN, ELITE_GUARD, ROYAL_GUARD
    }

    public enum PatrolState {
        PATROL,
        INVESTIGATE,
        ENGAGE,
        PURSUE,
        DISENGAGE,
        RETURN_TO_POST
    }

    public VillageGuardEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        equipStandardIronTier();
        this.setCustomName(Component.literal("§9Village Guard"));
        this.setCustomNameVisible(false);
        this.setPersistenceRequired(); // Critical persistence: never despawn naturally (BUG-06)

        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation gNav) {
            gNav.setCanOpenDoors(true);
        }

        if (!level.isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), homeSettlementId, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.MELEE_GUARD);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FOLLOW_RANGE, 48.0) // Expanded 48-block perception range for active perimeter defense
            .add(Attributes.ARMOR, 15.0) // Full Iron armor equivalence
            .add(Attributes.ARMOR_TOUGHNESS, 2.0);
    }

    private void equipStandardIronTier() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05f);
    }

    public void promoteRank(GuardRank newRank) {
        this.rank = newRank;
        switch (newRank) {
            case VETERAN -> {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                this.setCustomName(Component.literal("§bVeteran Guard"));
            }
            case ELITE_GUARD -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setCustomName(Component.literal("§5Elite Village Protector"));
            }
            case ROYAL_GUARD -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                this.setCustomName(Component.literal("§6Royal Guard"));
            }
            default -> {}
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new livingworld.ai.FenceGateInteractGoal(this));
        // Tactical Settlement Defense Rally: moves towards alarm coordinate if settlement under attack
        this.goalSelector.addGoal(1, new DefendSettlementGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.75) {
            @Override
            public boolean canUse() {
                return VillageGuardEntity.this.getTarget() == null && VillageGuardEntity.this.patrolState == PatrolState.PATROL && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f) {
            @Override
            public boolean canUse() {
                return VillageGuardEntity.this.getTarget() == null && VillageGuardEntity.this.patrolState == PatrolState.PATROL && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return VillageGuardEntity.this.getTarget() == null && VillageGuardEntity.this.patrolState == PatrolState.PATROL && super.canUse();
            }
        });

        // Priority 1 Target: Retaliate only against non-allied hostiles (friendly-fire filtering)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                LivingEntity attacker = VillageGuardEntity.this.getLastHurtByMob();
                return attacker != null && !AllegianceRegistry.get().areAllies(VillageGuardEntity.this, attacker);
            }
        });

        // Priority 2 Target: Dangerous Mutant Commanders & Boss Incursions (BUG-07)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, lvl) -> {
                if (target == null || !target.isAlive()) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                if (!isWithinDefensivePerimeter(target.blockPosition())) return false;
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                return (id != null && "mutantmonsters".equals(id.getNamespace())) || target instanceof MutantZombieCommanderEntity;
            }));

        // Priority 3 Target: Siege Miners breaching walls or buildings
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, lvl) -> {
                if (target == null || !target.isAlive()) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                if (!isWithinDefensivePerimeter(target.blockPosition())) return false;
                return target.getMainHandItem().is(Items.IRON_PICKAXE) || target.getMainHandItem().is(Items.DIAMOND_PICKAXE);
            }));

        // Priority 4 Target: Hostiles actively threatening civilians
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, lvl) -> {
                if (target == null || !target.isAlive()) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                if (!isWithinDefensivePerimeter(target.blockPosition())) return false;
                if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() instanceof Villager) return true;
                return isNearCivilian(target);
            }));

        // Priority 5 Target: Standard hostile monsters approaching settlement
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
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

    private boolean isNearCivilian(LivingEntity monster) {
        if (!(monster.level() instanceof ServerLevel serverLevel)) return false;
        AABB box = AABB.ofSize(monster.position(), 24, 12, 24);
        List<Villager> villagers = serverLevel.getEntitiesOfClass(Villager.class, box, v -> v.isAlive());
        return !villagers.isEmpty();
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
        LivingEntity current = this.getTarget();
        if (current != null && current.isAlive() && target != null && target != current) {
            long now = this.level().getGameTime();
            // Target Hysteresis: retain current target unless new target is high-priority emergency (civilian under attack)
            boolean emergency = (target instanceof Monster m && m.getTarget() instanceof Villager);
            if (!emergency && (now - lastTargetSwitchTick) < 30 && this.distanceToSqr(current) <= (maxPursuitDistance * maxPursuitDistance)) {
                return; // Maintain hysteresis; suppress rapid oscillation and spinning
            }
            this.lastTargetSwitchTick = now;
            this.targetSwitchCount++;
        }
        super.setTarget(target);
        if (target != null && target.isAlive()) {
            this.patrolState = PatrolState.ENGAGE;
            this.lostSightTicks = 0;
            this.lastOrientationReason = "Engaging " + target.getType().getDescription().getString();
        } else {
            this.lastOrientationReason = "Idle Patrol";
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            // Shield Blocking Mechanics
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && this.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.SHIELD)) {
                double distSqr = this.distanceToSqr(target);
                // When in combat range (< 16 blocks), guard raises shield periodically to block blows
                if (distSqr < 16.0 && !this.isUsingItem() && this.getRandom().nextFloat() < 0.08f) {
                    this.startUsingItem(InteractionHand.OFF_HAND);
                } else if (this.isUsingItem() && this.getTicksUsingItem() > 25) {
                    // Lower shield to perform sword counter-strike
                    this.stopUsingItem();
                }
            } else if (this.isUsingItem() && this.getUsedItemHand() == InteractionHand.OFF_HAND) {
                this.stopUsingItem();
            }

            // Patrol Boundary & Pursuit State Machine (BUG-05)
            updatePatrolStateMachine(target);
        }
    }

    private void updatePatrolStateMachine(LivingEntity target) {
        BlockPos post = getEffectivePost();
        double distFromPost = Math.sqrt(this.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5));

        if (target != null && target.isAlive()) {
            // Check line of sight
            if (this.hasLineOfSight(target)) {
                this.lostSightTicks = 0;
            } else {
                this.lostSightTicks++;
            }

            // Pursuit limit check
            if (distFromPost > maxPursuitDistance) {
                // Exceeded allowable pursuit boundary -> Disengage and return to post
                disengageAndReturn("Pursuit distance limit exceeded (" + String.format("%.1f", distFromPost) + "m > " + maxPursuitDistance + "m)");
                return;
            }

            // Lost sight check (6 seconds without line of sight)
            if (this.lostSightTicks > 120) {
                disengageAndReturn("Target lost in terrain for > 6 seconds");
                return;
            }

            // Critical injury retreat (health < 20%)
            if (this.getHealth() < this.getMaxHealth() * 0.2f && distFromPost > 8.0) {
                disengageAndReturn("Critical injuries sustained, retreating to defensive line");
                return;
            }

            this.patrolState = PatrolState.PURSUE;
        } else {
            // No active target
            if (this.patrolState == PatrolState.PURSUE || this.patrolState == PatrolState.ENGAGE) {
                this.patrolState = PatrolState.RETURN_TO_POST;
            }

            if (this.patrolState == PatrolState.RETURN_TO_POST) {
                if (distFromPost <= 4.0) {
                    this.patrolState = PatrolState.PATROL;
                    this.getNavigation().stop();
                } else if (this.tickCount % 20 == 0) {
                    this.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.25);
                }
            }
        }
    }

    private void disengageAndReturn(String reason) {
        this.setTarget(null);
        this.patrolState = PatrolState.DISENGAGE;
        this.lostSightTicks = 0;
        BlockPos post = getEffectivePost();
        this.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.3);
        this.patrolState = PatrolState.RETURN_TO_POST;
        livingworld.LivingWorldMod.LOGGER.debug("[Living World] Guard " + this.getUUID() + " disengaged: " + reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("HomeSettlementId", this.homeSettlementId != null ? this.homeSettlementId : "");
        output.putString("GuardRank", this.rank.name());
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
            this.rank = GuardRank.valueOf(rankStr);
        } catch (Exception ignored) {}

        String stateStr = input.getStringOr("PatrolState", "PATROL");
        try {
            this.patrolState = PatrolState.valueOf(stateStr);
        } catch (Exception ignored) {}

        this.maxPursuitDistance = input.getDoubleOr("MaxPursuitDistance", 24.0);

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
            AllegianceRegistry.get().register(this.getUUID(), this.homeSettlementId, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.MELEE_GUARD);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        // Telemetry distinguishing DEATH, DESPAWN, UNLOAD, DIMENSION_CHANGE (BUG-06)
        if (reason == Entity.RemovalReason.KILLED) {
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Military Casualty: Guard " + this.getUUID() + " (" + this.rank + ") killed in combat at " + this.blockPosition() + " (DEATH)");
            if (this.homeSettlementId != null && !this.homeSettlementId.isEmpty()) {
                AllegianceRegistry.get().unregister(this.getUUID());
            }
        } else if (reason == Entity.RemovalReason.DISCARDED) {
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Guard " + this.getUUID() + " discarded (DESPAWN/DISCARD)");
            if (this.homeSettlementId != null && !this.homeSettlementId.isEmpty()) {
                AllegianceRegistry.get().unregister(this.getUUID());
            }
        } else if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            livingworld.LivingWorldMod.LOGGER.debug("[Living World] Guard " + this.getUUID() + " safely serialized to chunk at " + this.blockPosition() + " (UNLOAD)");
            // Crucial: do NOT unregister from AllegianceRegistry on chunk unload! Entity remains on persistent roster.
        } else if (reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Guard " + this.getUUID() + " transferred dimension (DIMENSION_CHANGE)");
        }
    }

    public GuardRank getRank() {
        return rank;
    }

    public String getHomeSettlementId() {
        return homeSettlementId;
    }

    public void setHomeSettlementId(String id) {
        this.homeSettlementId = id;
        if (id != null && !id.isEmpty() && !this.level().isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), id, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.MELEE_GUARD);
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

    public PatrolState getPatrolState() {
        return patrolState;
    }

    public void setPatrolState(PatrolState state) {
        this.patrolState = state;
    }

    public UUID getCompanionDogUuid() {
        return companionDogUuid;
    }

    public void setCompanionDogUuid(UUID companionDogUuid) {
        this.companionDogUuid = companionDogUuid;
    }

    public int getTargetSwitchCount() {
        return targetSwitchCount;
    }

    public String getLastOrientationReason() {
        return lastOrientationReason;
    }

    /**
     * Tactical Settlement Defense Goal: Rallies soldier towards confirmed alarms or active breach zones.
     */
    public static class DefendSettlementGoal extends Goal {
        private final VillageGuardEntity guard;

        public DefendSettlementGoal(VillageGuardEntity guard) {
            this.guard = guard;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (guard.getTarget() != null && guard.getTarget().isAlive()) return false;
            if (!(guard.level() instanceof ServerLevel)) return false;
            Settlement settlement = SettlementManager.get().getNearestSettlement(guard.blockPosition());
            if (settlement == null) return false;
            VillageDefenseController.SettlementDefenseState state = VillageDefenseController.get().getState(settlement.getId());
            return state.getThreatLevel() == VillageDefenseController.ThreatLevel.UNDER_ATTACK && state.getActiveThreatPos() != null;
        }

        @Override
        public void tick() {
            Settlement settlement = SettlementManager.get().getNearestSettlement(guard.blockPosition());
            if (settlement != null) {
                VillageDefenseController.SettlementDefenseState state = VillageDefenseController.get().getState(settlement.getId());
                BlockPos threatPos = state.getActiveThreatPos();
                if (threatPos != null) {
                    guard.getNavigation().moveTo(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 1.3);
                }
            }
        }
    }
}
