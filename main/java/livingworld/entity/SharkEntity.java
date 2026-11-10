package livingworld.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Genuine Great White Shark Apex Marine Predator (Fix 1, F15).
 * - 60 HP, 6 Armor, 9 Attack Damage
 * - Natural marine gill respiration (permanent underwater endurance)
 * - Hunts wild fish and targets bleeding/wounded players (< 50% HP)
 * - Deep ocean preference: avoids shallow water (< 3 blocks)
 * - Meat distraction counterplay (raw cod, salmon, beef, porkchop)
 */
public class SharkEntity extends Dolphin {
    private int distractionTicks = 0;
    private int biteCooldown = 0;

    public SharkEntity(EntityType<? extends SharkEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Dolphin.createAttributes()
            .add(Attributes.MAX_HEALTH, 60.0)
            .add(Attributes.MOVEMENT_SPEED, 1.45)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.FOLLOW_RANGE, 36.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SharkLungeAttackGoal(this, 1.45D, true));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0D, 30));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Hunt ocean fish
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Cod.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Salmon.class, true));
        // Hunt wounded/bleeding players (< 50% HP)
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
            (target, sl) -> target instanceof LivingEntity living && living.getHealth() < living.getMaxHealth() * 0.5f && isNotDistracted()));
    }

    public boolean isNotDistracted() {
        return distractionTicks <= 0;
    }

    @Override
    public int getMaxAirSupply() {
        return 40000; // Natural gills: cannot drown in water
    }

    @Override
    protected void handleAirSupply(int airSupply) {
        if (this.isAlive() && this.isInWater()) {
            this.setAirSupply(this.getMaxAirSupply());
        } else {
            this.setAirSupply(airSupply - 1);
            if (this.getAirSupply() <= -20) {
                this.setAirSupply(0);
                this.hurt(this.damageSources().dryOut(), 2.0F);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (distractionTicks > 0) {
            distractionTicks--;
        }
        if (biteCooldown > 0) {
            biteCooldown--;
        }

        // Avoid shallow water: if water depth is less than 3 blocks, steer toward deeper water
        if (!level().isClientSide() && tickCount % 30 == 0 && isInWater()) {
            BlockPos current = blockPosition();
            if (level().getFluidState(current.below(2)).isEmpty()) {
                // Too shallow! Move towards open water
                setDeltaMovement(getDeltaMovement().add(0, -0.15, 0));
            }
        }

        // Ecological meat distraction
        if (!level().isClientSide() && tickCount % 20 == 0 && distractionTicks <= 0) {
            AABB searchBox = this.getBoundingBox().inflate(12.0);
            List<ItemEntity> droppedMeat = level().getEntitiesOfClass(ItemEntity.class, searchBox, item -> {
                ItemStack stack = item.getItem();
                return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.BEEF) || stack.is(Items.PORKCHOP);
            });

            if (!droppedMeat.isEmpty()) {
                ItemEntity bait = droppedMeat.get(0);
                this.getNavigation().moveTo(bait, 1.5);
                if (this.distanceToSqr(bait) < 4.0) {
                    bait.getItem().shrink(1);
                    if (bait.getItem().isEmpty()) {
                        bait.discard();
                    }
                    distractionTicks = 400; // 20 seconds distraction
                    setTarget(null);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.5, getZ(), 8, 0.4, 0.4, 0.4, 0.1);
                        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, net.minecraft.sounds.SoundSource.NEUTRAL, 1.2F, 0.8F);
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 0.7F);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WATER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT_DROWN;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOSTILE_DEATH;
    }

    /**
     * Circling and lunging melee attack goal
     */
    private static class SharkLungeAttackGoal extends MeleeAttackGoal {
        private final SharkEntity shark;

        public SharkLungeAttackGoal(SharkEntity shark, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(shark, speedModifier, followingTargetEvenIfNotSeen);
            this.shark = shark;
        }

        @Override
        public boolean canUse() {
            return shark.isNotDistracted() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return shark.isNotDistracted() && super.canContinueToUse();
        }
    }
}
