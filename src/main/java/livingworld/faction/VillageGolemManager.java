package livingworld.faction;

import java.util.List;
import livingworld.settlement.Settlement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Golem Population & Biome-Specific Defense Manager (Section 10, Living Village Update).
 * - Plains and ordinary villages: Maintains doubled Iron Golem defense quotas (1 per 8 villagers, min 2).
 * - Snowy and cold-climate villages: Maintains 2x Iron Golems plus a dedicated Snow Golem sentry force (2-4 snow golems).
 * - Coordinates snow golem targeting of hostile monsters, friendly-fire immunity, and alarm responses.
 */
public class VillageGolemManager {
    private static final VillageGolemManager INSTANCE = new VillageGolemManager();

    public static VillageGolemManager get() {
        return INSTANCE;
    }

    public void tickGolems(ServerLevel level, Settlement settlement) {
        if (level == null || settlement == null) return;

        BlockPos center = settlement.getCenter();
        AABB villageZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 24, settlement.getRadius() * 2);

        boolean isColdClimate = isColdOrSnowyBiome(level, center);

        // 1. Iron Golem Maintenance (Doubled target: 1 per 8 villagers, min 2)
        int desiredIronGolems = Math.max(2, settlement.getPopulation() / 8);
        if (isColdClimate) {
            desiredIronGolems = Math.max(3, desiredIronGolems + 1);
        }

        List<IronGolem> currentIronGolems = level.getEntitiesOfClass(IronGolem.class, villageZone, g -> g.isAlive());
        for (IronGolem golem : currentIronGolems) {
            AllegianceRegistry.get().register(golem.getUUID(), settlement.getId().toString(), "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.IRON_GOLEM_DEFENDER);
            AllegianceRegistry.get().sanitizeDefenderTarget(golem);

            // Water Recovery & Buoyancy Navigation Assist (BUG-11)
            handleGolemWaterRecovery(level, golem, center);
        }

        if (currentIronGolems.size() < desiredIronGolems) {
            // Check settlement stone/iron stockpile or natural village defense condition
            IronGolem newGolem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
            if (newGolem != null) {
                newGolem.setPos(center.getX() + level.getRandom().nextInt(7) - 3, center.getY() + 1, center.getZ() + level.getRandom().nextInt(7) - 3);
                level.addFreshEntity(newGolem);

                AllegianceRegistry.get().register(newGolem.getUUID(), settlement.getId().toString(), "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.IRON_GOLEM_DEFENDER);

                level.playSound(null, newGolem.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, newGolem.getX(), newGolem.getY() + 1.5, newGolem.getZ(), 16, 0.6, 0.6, 0.6, 0.1);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Golem defense mobilized: Iron Golem stationed in " + settlement.getName() + " (" + (currentIronGolems.size() + 1) + "/" + desiredIronGolems + ")");
            }
        }

        // 2. Snow Golem Defense in Cold/Snowy Climates (Baseline: 2 to 4 Snow Golems)
        if (isColdClimate) {
            int desiredSnowGolems = Math.max(2, Math.min(4, settlement.getPopulation() / 6));
            List<SnowGolem> currentSnowGolems = level.getEntitiesOfClass(SnowGolem.class, villageZone, s -> s.isAlive());

            for (SnowGolem snowGolem : currentSnowGolems) {
                AllegianceRegistry.get().register(snowGolem.getUUID(), settlement.getId().toString(), "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.SNOW_GOLEM_DEFENDER);
                AllegianceRegistry.get().sanitizeDefenderTarget(snowGolem);
            }

            if (currentSnowGolems.size() < desiredSnowGolems) {
                SnowGolem snowGolem = EntityTypes.SNOW_GOLEM.create(level, EntitySpawnReason.EVENT);
                if (snowGolem != null) {
                    snowGolem.setPos(center.getX() + level.getRandom().nextInt(9) - 4, center.getY() + 1, center.getZ() + level.getRandom().nextInt(9) - 4);
                    level.addFreshEntity(snowGolem);

                    AllegianceRegistry.get().register(snowGolem.getUUID(), settlement.getId().toString(), "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.SNOW_GOLEM_DEFENDER);

                    level.playSound(null, snowGolem.blockPosition(), SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.NEUTRAL, 1.0f, 1.1f);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, snowGolem.getX(), snowGolem.getY() + 1.0, snowGolem.getZ(), 16, 0.4, 0.4, 0.4, 0.05);
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Cold-climate defense mobilized: Snow Golem sentry stationed in " + settlement.getName());
                }
            }
        }
    }

    public String getGolemSummary(ServerLevel level, Settlement settlement) {
        if (settlement == null || level == null) return "No settlement available.";
        BlockPos center = settlement.getCenter();
        AABB villageZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 24, settlement.getRadius() * 2);
        List<IronGolem> ironGolems = level.getEntitiesOfClass(IronGolem.class, villageZone, g -> g.isAlive());
        List<SnowGolem> snowGolems = level.getEntitiesOfClass(SnowGolem.class, villageZone, s -> s.isAlive());
        int targetIron = Math.max(2, settlement.getPopulation() / 8);
        boolean cold = isColdOrSnowyBiome(level, center);
        int targetSnow = cold ? Math.max(2, Math.min(4, settlement.getPopulation() / 6)) : 0;
        return String.format("Iron Golems: %d (Quota: %d) | Snow Golems: %d (Target: %d) | Climate: %s",
            ironGolems.size(), targetIron, snowGolems.size(), targetSnow, cold ? "Cold/Snowy" : "Temperate/Warm");
    }

    private boolean isColdOrSnowyBiome(ServerLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        if (biome.is(BiomeTags.HAS_VILLAGE_SNOWY) || biome.is(BiomeTags.IS_TAIGA)) {
            return true;
        }
        return level.getBiome(pos).value().getBaseTemperature() < 0.2f;
    }

    /**
     * Golem Water Recovery & Buoyancy Assistance (BUG-11):
     * Detects when an Iron Golem is submerged, applies buoyant impulse,
     * navigates toward the closest dry shoreline within 16 blocks.
     */
    public void handleGolemWaterRecovery(ServerLevel level, IronGolem golem, BlockPos villageCenter) {
        if (golem == null || !golem.isAlive() || level == null) return;
        if (!golem.isInWater()) return;

        // 1. Upward buoyancy impulse to allow swimming and step-ups
        Vec3 delta = golem.getDeltaMovement();
        golem.setDeltaMovement(delta.x, Math.max(0.12, delta.y + 0.08), delta.z);

        // 2. Find closest dry, solid ground within 16 blocks
        BlockPos golemPos = golem.blockPosition();
        BlockPos nearestDry = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = -2; dy <= 4; dy++) {
                    BlockPos p = golemPos.offset(dx, dy, dz);
                    if (level.isLoaded(p) && !level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.WATER) &&
                        !level.getBlockState(p.above()).is(net.minecraft.world.level.block.Blocks.WATER) &&
                        level.getBlockState(p.below()).isSolid()) {
                        double d = p.distSqr(golemPos);
                        if (d < bestDistSq) {
                            bestDistSq = d;
                            nearestDry = p;
                        }
                    }
                }
            }
        }

        if (nearestDry != null) {
            golem.getNavigation().moveTo(nearestDry.getX() + 0.5, nearestDry.getY(), nearestDry.getZ() + 0.5, 1.25);
        } else {
            golem.getNavigation().moveTo(villageCenter.getX(), villageCenter.getY(), villageCenter.getZ(), 1.25);
        }
    }
}
