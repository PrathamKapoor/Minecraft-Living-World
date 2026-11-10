package livingworld.entity;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Multi-Headed Mutant Zombie Commander Entity (Section 16, F10).
 * Distinctive 3-headed mutant zombie with custom animations, rallying roar,
 * and death scatter effects that disperse follower zombies.
 */
public class MutantZombieCommanderEntity extends Monster {
    private final ServerBossEvent bossEvent;
    private int roarCooldown = 100;
    private int animationTicks = 0;

    public MutantZombieCommanderEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.bossEvent = (ServerBossEvent) new ServerBossEvent(
            java.util.UUID.randomUUID(),
            Component.literal("§4Mutant Zombie Commander"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
        ).setDarkenScreen(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 180.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 10.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        animationTicks++;

        if (this.level() instanceof ServerLevel serverLevel) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            // Periodic Rallying Roar: buffs nearby zombies within 18 blocks
            if (roarCooldown > 0) {
                roarCooldown--;
            } else {
                executeRallyingRoar(serverLevel);
                roarCooldown = 160; // 8 seconds
            }
        }
    }

    private void executeRallyingRoar(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.5f, 0.6f);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 2.0, this.getZ(), 12, 0.5, 0.5, 0.5, 0.05);

        AABB rallyBox = AABB.ofSize(this.position(), 36, 12, 36);
        List<Zombie> nearbyZombies = level.getEntitiesOfClass(Zombie.class, rallyBox, z -> z.isAlive());

        for (Zombie z : nearbyZombies) {
            z.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 0));
            z.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 0));
            if (this.getTarget() != null && z.getTarget() == null) {
                z.setTarget(this.getTarget());
            }
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.bossEvent.removeAllPlayers(); // Clear boss bar on death (BUG-15)

        if (this.level() instanceof ServerLevel serverLevel) {
            // Disperse and panic all follower zombies in a 24-block radius
            AABB panicBox = AABB.ofSize(this.position(), 48, 16, 48);
            List<Zombie> followers = serverLevel.getEntitiesOfClass(Zombie.class, panicBox, z -> z.isAlive());

            for (Zombie z : followers) {
                z.setTarget(null);
                z.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
                // Move away from commander death location
                double dx = z.getX() - this.getX();
                double dz = z.getZ() - this.getZ();
                z.getNavigation().moveTo(z.getX() + dx * 2.0, z.getY(), z.getZ() + dz * 2.0, 1.4);
            }

            serverLevel.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.8, 0.8, 0.8, 0.1);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers(); // Clear boss bar on unload or removal (BUG-15)
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    public int getAnimationTicks() {
        return animationTicks;
    }
}
