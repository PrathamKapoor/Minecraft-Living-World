package livingworld.travel;

import java.util.List;
import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MinecartSpeedManager {
    private static final MinecartSpeedManager INSTANCE = new MinecartSpeedManager();

    public static MinecartSpeedManager get() {
        return INSTANCE;
    }

    public void tickMinecartBoosts(ServerLevel level) {
        if (!FeatureConfig.get().enableMinecartImprovements) {
            return;
        }

        // Ticking loaded minecarts with passengers
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (player.getVehicle() instanceof AbstractMinecart cart) {
                BlockPos underPos = cart.blockPosition();
                BlockState railState = level.getBlockState(underPos);

                // If on powered rail that is actively powered
                if (railState.is(Blocks.POWERED_RAIL) && railState.getValue(PoweredRailBlock.POWERED)) {
                    Vec3 vel = cart.getDeltaMovement();
                    double speedSq = vel.horizontalDistanceSqr();
                    if (speedSq > 0.01) {
                        double multiplier = FeatureConfig.get().minecartSpeedMultiplier;
                        double targetMaxSpeed = 0.4 * multiplier;
                        double currentSpeed = Math.sqrt(speedSq);

                        if (currentSpeed < targetMaxSpeed) {
                            Vec3 boosted = vel.scale(1.15);
                            cart.setDeltaMovement(boosted.x, vel.y, boosted.z);
                            if (player.tickCount % 200 == 0) {
                                livingworld.LivingWorldMod.LOGGER.info("[Living World] Minecart Transit: 2.5x high-speed powered rail boost active for player " + player.getName().getString());
                            }
                        }
                    }
                }
            }
        }
    }
}
