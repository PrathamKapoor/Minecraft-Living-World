package livingworld.ocean;

import java.util.List;
import livingworld.config.FeatureConfig;
import livingworld.entity.LivingWorldEntities;
import livingworld.entity.SharkEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Manages ocean apex predator ecology and genuine Shark spawning (F15).
 */
public class SharkManager {
    private static final SharkManager INSTANCE = new SharkManager();
    private long lastSharkSpawnCheck = 0;

    public static SharkManager get() {
        return INSTANCE;
    }

    public void tickMarineEcology(ServerLevel level) {
        if (!FeatureConfig.get().enableSharks) {
            return;
        }

        long gameTime = level.getGameTime();

        // 1. Natural Deep Ocean Spawning near players
        if (gameTime - lastSharkSpawnCheck > 600) { // Every 30 seconds
            lastSharkSpawnCheck = gameTime;
            for (ServerPlayer player : level.players()) {
                BlockPos pPos = player.blockPosition();
                if (level.getBiome(pPos).is(BiomeTags.IS_OCEAN) || level.getBiome(pPos).is(BiomeTags.IS_DEEP_OCEAN)) {
                    if (player.isInWater() || player.isPassenger()) {
                        int currentSharks = level.getEntitiesOfClass(SharkEntity.class, player.getBoundingBox().inflate(64.0),
                            s -> s.isAlive()).size();
                        if (currentSharks < 2 && level.getRandom().nextInt(2) == 0) {
                            spawnOceanHunter(level, pPos.offset(level.getRandom().nextInt(30) - 15, -4, level.getRandom().nextInt(30) - 15));
                        }
                    }
                }
            }
        }

        // 2. Apex Predator Behavior & Scent of Blood
        for (ServerPlayer player : level.players()) {
            if (!player.isInWater()) continue;

            BlockPos playerPos = player.blockPosition();
            if (!level.getBiome(playerPos).is(BiomeTags.IS_OCEAN) && !level.getBiome(playerPos).is(BiomeTags.IS_DEEP_OCEAN)) continue;

            AABB oceanBox = AABB.ofSize(player.position(), 48, 24, 48);
            List<SharkEntity> predators = level.getEntitiesOfClass(SharkEntity.class, oceanBox,
                s -> s.isAlive() && s.isInWater());

            for (SharkEntity shark : predators) {
                // Scent of Blood: If player is bleeding/injured (under 50% health), shark circles and strikes
                if (player.getHealth() < player.getMaxHealth() * 0.5f && shark.isNotDistracted() && shark.distanceToSqr(player) < 576.0) {
                    shark.setTarget(player);
                    shark.getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), 1.4);

                    // Audio-visual blood frenzy particles
                    level.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY(), player.getZ(), 5, 0.3, 0.3, 0.3, 0.05);

                    if (shark.distanceToSqr(player) < 4.0) {
                        player.hurtServer(level, level.damageSources().mobAttack(shark), 7.0f);
                        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(), player.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
        }
    }

    public void spawnOceanHunter(ServerLevel level, BlockPos pos) {
        // Find safe water position
        int safeY = Math.max(30, Math.min(60, pos.getY()));
        BlockPos waterPos = new BlockPos(pos.getX(), safeY, pos.getZ());
        if (!level.getBlockState(waterPos).getFluidState().isSource()) {
            return;
        }

        SharkEntity shark = LivingWorldEntities.SHARK.create(level, EntitySpawnReason.NATURAL);
        if (shark != null) {
            shark.setPos(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5);
            shark.setCustomName(Component.literal("§3Great White Shark"));
            shark.setCustomNameVisible(true);
            level.addFreshEntity(shark);
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Ocean Hunter: Natural Great White Shark spawned at " + waterPos);
        }
    }
}
