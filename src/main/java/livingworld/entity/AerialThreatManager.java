package livingworld.entity;

import java.util.List;
import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * Manages high-altitude aerial threats and genuine Gargoyle Swooper spawning (F12).
 */
public class AerialThreatManager {
    private static final AerialThreatManager INSTANCE = new AerialThreatManager();
    private long lastAerialCheck = 0;

    public static AerialThreatManager get() {
        return INSTANCE;
    }

    public void tickAerialThreats(ServerLevel level, long currentTicks) {
        if (!FeatureConfig.get().enableAerialPredators) {
            return;
        }

        // 1. Natural Mountain & Badlands High-Altitude Spawning
        if (currentTicks - lastAerialCheck > 600) { // Every 30 seconds
            lastAerialCheck = currentTicks;
            for (ServerPlayer player : level.players()) {
                BlockPos playerPos = player.blockPosition();

                // High-altitude mountain / badlands encounters (above Y=100)
                if (playerPos.getY() >= 100 && (level.getBiome(playerPos).is(BiomeTags.IS_MOUNTAIN) || level.getBiome(playerPos).is(BiomeTags.IS_BADLANDS))) {
                    int activeSwoopers = level.getEntitiesOfClass(GargoyleSwooperEntity.class, player.getBoundingBox().inflate(64.0),
                        s -> s.isAlive()).size();

                    if (activeSwoopers < 2 && level.getRandom().nextInt(2) == 0) {
                        spawnAerialHunter(level, playerPos.above(18));
                    }
                }
            }
        }

        // 2. Tactical Dive Mechanics
        for (ServerPlayer player : level.players()) {
            if (player.getY() < 80) continue;

            List<GargoyleSwooperEntity> swoopers = level.getEntitiesOfClass(GargoyleSwooperEntity.class, player.getBoundingBox().inflate(32.0),
                s -> s.isAlive());

            for (GargoyleSwooperEntity swooper : swoopers) {
                Vec3 toPlayer = player.position().subtract(swooper.position());
                double dist = toPlayer.length();

                // Dive Acceleration when diving toward player
                if (dist > 4.0 && dist < 24.0 && swooper.getY() > player.getY()) {
                    Vec3 diveVec = toPlayer.normalize().scale(0.85);
                    swooper.setDeltaMovement(diveVec);
                    level.sendParticles(ParticleTypes.CLOUD, swooper.getX(), swooper.getY(), swooper.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
                }

                // Impact & Knockback
                if (dist <= 3.0) {
                    if (player.isBlocking()) {
                        // Shield blocks dive, stunning the swooper!
                        swooper.setDeltaMovement(0, 0.6, 0);
                        player.sendSystemMessage(Component.literal("§6[Living World] §aShield blocked the Gargoyle dive attack!"));
                    } else {
                        player.hurtServer(level, level.damageSources().mobAttack(swooper), 7.0f);
                        Vec3 knockback = toPlayer.normalize().scale(0.6);
                        player.setDeltaMovement(knockback.x, 0.35, knockback.z);
                    }
                }
            }
        }
    }

    public void spawnAerialHunter(ServerLevel level, BlockPos pos) {
        GargoyleSwooperEntity hunter = LivingWorldEntities.GARGOYLE_SWOOPER.create(level, EntitySpawnReason.NATURAL);
        if (hunter != null) {
            hunter.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            hunter.setCustomName(Component.literal("§5Mountain Gargoyle Swooper"));
            hunter.setCustomNameVisible(true);
            level.addFreshEntity(hunter);
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Aerial Threat: Mountain Gargoyle Swooper spawned at " + pos);
        }
    }
}
