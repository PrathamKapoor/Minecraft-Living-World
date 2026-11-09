package livingworld.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Genuine End Void Stalker hostile encounter (F13).
 * Stalks outer End islands outside the central dragon arena (distance > 800 blocks).
 * Phases through void rifts, teleports aggressively, and drops void essence.
 */
public class EndVoidStalkerEntity extends Enderman {
    public EndVoidStalkerEntity(EntityType<? extends EndVoidStalkerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Enderman.createAttributes()
            .add(Attributes.MAX_HEALTH, 45.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            // High combat mobility: warp to new vantage point
            teleport();
            level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 20, 0.5, 0.8, 0.5, 0.1);
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // Guaranteed void rewards
        spawnAtLocation(level, new ItemStack(Items.ENDER_PEARL, 2 + level.getRandom().nextInt(2)));
        spawnAtLocation(level, new ItemStack(Items.AMETHYST_SHARD, 1)); // Void Shard
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_STARE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }
}
