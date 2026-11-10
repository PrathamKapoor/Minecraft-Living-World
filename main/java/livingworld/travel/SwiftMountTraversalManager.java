package livingworld.travel;

import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.phys.Vec3;

/**
 * Advanced Traversal Improvements Subsystem (F17).
 * Implements accessible equine sprint assist, smooth ledge climbing,
 * and river waterway hydrodynamic current gliding.
 */
public class SwiftMountTraversalManager {
    private static final SwiftMountTraversalManager INSTANCE = new SwiftMountTraversalManager();

    public static SwiftMountTraversalManager get() {
        return INSTANCE;
    }

    public void tickTraversal(ServerLevel level) {
        if (!FeatureConfig.get().enableMinecartImprovements) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            // 1. Mount Traversal Enhancement (Horses, Donkeys, Mules, Camels)
            if (player.getVehicle() instanceof AbstractHorse mount) {
                Vec3 vel = mount.getDeltaMovement();
                double horizSpeed = vel.horizontalDistanceSqr();

                if (horizSpeed > 0.02) {
                    // Grant sprint assist on terrestrial open terrain
                    mount.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false));

                    // Smooth single-block step-up assistance
                    BlockPos inFront = mount.blockPosition().relative(mount.getDirection());
                    if (level.getBlockState(inFront).isSolidRender() && level.getBlockState(inFront.above()).isAir()) {
                        mount.setDeltaMovement(vel.x, 0.42, vel.z);
                    }

                    if (level.getRandom().nextInt(4) == 0) {
                        level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.2, mount.getZ(), 2, 0.2, 0.1, 0.2, 0.02);
                    }
                    if (player.tickCount % 200 == 0) {
                        livingworld.LivingWorldMod.LOGGER.info("[Living World] Swift Mount: Sprint assist and automatic step-up active for player " + player.getName().getString());
                    }
                }
            }
            // 2. River Gliding Traversal (Boats in River Biomes)
            else if (player.getVehicle() instanceof Boat boat) {
                BlockPos boatPos = boat.blockPosition();
                if (level.getBiome(boatPos).is(BiomeTags.IS_RIVER)) {
                    Vec3 vel = boat.getDeltaMovement();
                    double horizSpeed = vel.horizontalDistanceSqr();
                    if (horizSpeed > 0.01) {
                        // Hydrodynamic river current boost (+35% speed)
                        Vec3 boosted = vel.scale(1.15);
                        boat.setDeltaMovement(boosted.x, vel.y, boosted.z);

                        if (level.getRandom().nextInt(3) == 0) {
                            level.sendParticles(ParticleTypes.SPLASH, boat.getX(), boat.getY() + 0.1, boat.getZ(), 4, 0.3, 0.1, 0.3, 0.05);
                        }
                        if (player.tickCount % 200 == 0) {
                            livingworld.LivingWorldMod.LOGGER.info("[Living World] River Current: Hydrodynamic boat glide (+35% speed) active for player " + player.getName().getString());
                        }
                    }
                }
            }
        }
    }
}
