package livingworld.entity;

import java.util.EnumSet;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Dedicated Village Guard Unit (Section 10, F05; v0.2.5 Living Village Rebuild).
 * Enforces minimum iron-tier defense standards; recruits, equips, and patrols settlement perimeters.
 * Features 3D visible iron/diamond/netherite armor, iron sword, active shield blocking defense,
 * and centralized allegiance eliminating friendly-fire with fellow guards and archers.
 */
public class VillageGuardEntity extends PathfinderMob {
    private GuardRank rank = GuardRank.SOLDIER;
    private String homeSettlementId = "";

    public enum GuardRank {
        RECRUIT, SOLDIER, VETERAN, ELITE_GUARD, ROYAL_GUARD
    }

    public VillageGuardEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        equipStandardIronTier();
        this.setCustomName(Component.literal("§9Village Guard"));
        this.setCustomNameVisible(false);

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
        // Priority 1: Defend settlement alarm and rally to active breach coordinates
        this.goalSelector.addGoal(1, new DefendSettlementGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Priority 1: Retaliate only against non-allied hostiles (friendly-fire filtering)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                LivingEntity attacker = VillageGuardEntity.this.getLastHurtByMob();
                return attacker != null && !AllegianceRegistry.get().areAllies(VillageGuardEntity.this, attacker);
            }
        });

        // Priority 2: Prioritize attacking mutant creatures and horde commanders threatening settlement
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, level) -> {
                if (target == null) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                return (id != null && "mutantmonsters".equals(id.getNamespace())) || target instanceof MutantZombieCommanderEntity;
            }));

        // Priority 3: Attack all standard hostile monsters targeting villagers/players
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
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
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

    /**
     * Tactical Settlement Defense Goal: Rallies soldier towards confirmed alarms or active breach zones.
     */
    public static class DefendSettlementGoal extends Goal {
        private final PathfinderMob guard;

        public DefendSettlementGoal(PathfinderMob guard) {
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
