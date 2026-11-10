package livingworld.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Genuine Gargoyle Swooper aerial threat.
 * Spawns naturally in mountain peaks, jagged ridges, and high-altitude badlands (Y >= 120).
 * Executes steep swooping dive attacks with shield stun counterplay and authentic stone-winged visuals.
 */
public class GargoyleSwooperEntity extends Phantom {
    private int stunTicks = 0;

    public GargoyleSwooperEntity(EntityType<? extends GargoyleSwooperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Phantom.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 28.0)
            .add(Attributes.FLYING_SPEED, 0.85)
            .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof Player player && player.isBlocking()) {
            // Shield stun counterplay: swooper is deflected and stunned
            this.stunTicks = 80; // 4 seconds stun
            this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3, false, false));
            this.setDeltaMovement(this.getDeltaMovement().reverse().scale(1.5));
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.5, getZ(), 10, 0.4, 0.4, 0.4, 0.2);
            level.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), player.getSoundSource(), 1.0F, 0.8F);
            return false;
        }

        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && target instanceof LivingEntity living) {
            // Inflict heavy swooping knockback
            living.knockback(0.6, getX() - living.getX(), getZ() - living.getZ(), level.damageSources().mobAttack(this), 1.0f);
        }
        return hurt;
    }

    @Override
    public void tick() {
        if (stunTicks > 0) {
            stunTicks--;
            if (level() instanceof ServerLevel sl && tickCount % 5 == 0) {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 3, 0.2, 0.2, 0.2, 0.02);
            }
        }
        super.tick();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_IMITATE_PHANTOM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STONE_FALL;
    }
}
