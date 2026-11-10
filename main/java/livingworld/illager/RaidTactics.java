package livingworld.illager;

import java.util.List;
import livingworld.config.FeatureConfig;
import livingworld.entity.IllagerShieldbearerEntity;
import livingworld.entity.IllagerSwordsmanEntity;
import livingworld.entity.LivingWorldEntities;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tactical Raids, Squad Combat, and Building Occupation (Section 14, F04).
 * Coordinates Swordsmen, Shieldbearers, and Crossbowmen during village attacks.
 */
public class RaidTactics {
    private static final RaidTactics INSTANCE = new RaidTactics();

    public static RaidTactics get() {
        return INSTANCE;
    }

    public void setupPillagerTactics(Pillager pillager) {
        if (!FeatureConfig.get().enableIllagerTactics) {
            return;
        }

        if (pillager.level() instanceof ServerLevel level) {
            // 1 in 4 patrol encounters converts into a genuine specialized illager warrior
            if (pillager.getRandom().nextInt(4) == 0 && !pillager.isPatrolLeader()) {
                BlockPos pos = pillager.blockPosition();
                boolean isShield = pillager.getRandom().nextBoolean();
                if (isShield) {
                    IllagerShieldbearerEntity shieldbearer = LivingWorldEntities.ILLAGER_SHIELDBEARER.create(level, EntitySpawnReason.EVENT);
                    if (shieldbearer != null) {
                        shieldbearer.setPos(pillager.getX(), pillager.getY(), pillager.getZ());
                        level.addFreshEntity(shieldbearer);
                        pillager.discard();
                        livingworld.LivingWorldMod.LOGGER.info("[Living World] Spawned genuine Illager Shieldbearer at " + pos);
                        return;
                    }
                } else {
                    IllagerSwordsmanEntity swordsman = LivingWorldEntities.ILLAGER_SWORDSMAN.create(level, EntitySpawnReason.EVENT);
                    if (swordsman != null) {
                        swordsman.setPos(pillager.getX(), pillager.getY(), pillager.getZ());
                        level.addFreshEntity(swordsman);
                        pillager.discard();
                        livingworld.LivingWorldMod.LOGGER.info("[Living World] Spawned genuine Illager Swordsman at " + pos);
                        return;
                    }
                }
            }
        }
    }

    public void coordinateIllagerSquad(List<AbstractIllager> squad, LivingEntity target) {
        if (squad.isEmpty() || target == null) return;

        for (int i = 0; i < squad.size(); i++) {
            AbstractIllager illager = squad.get(i);
            if (!illager.isAlive()) continue;

            if (illager instanceof IllagerShieldbearerEntity) {
                // Frontline shield wall: advance directly between target and crossbowmen
                illager.getNavigation().moveTo(target, 1.15);
            } else if (illager instanceof IllagerSwordsmanEntity) {
                // Fast flanker: close in rapidly on flank angles
                double angleOffset = (i % 2 == 0 ? 1 : -1) * (Math.PI / 3.0);
                Vec3 toTarget = illager.position().subtract(target.position()).normalize();
                double flankX = target.getX() + (toTarget.x * Math.cos(angleOffset) - toTarget.z * Math.sin(angleOffset)) * 8.0;
                double flankZ = target.getZ() + (toTarget.x * Math.sin(angleOffset) + toTarget.z * Math.cos(angleOffset)) * 8.0;
                illager.getNavigation().moveTo(flankX, target.getY(), flankZ, 1.35);
            } else if (illager instanceof Pillager pillager) {
                // Crossbow pillagers take perimeter standoff distance (10-14 blocks)
                double angleOffset = (i % 2 == 0 ? 1 : -1) * (Math.PI / 4.0);
                Vec3 toTarget = pillager.position().subtract(target.position()).normalize();
                double flankX = target.getX() + (toTarget.x * Math.cos(angleOffset) - toTarget.z * Math.sin(angleOffset)) * 12.0;
                double flankZ = target.getZ() + (toTarget.x * Math.sin(angleOffset) + toTarget.z * Math.cos(angleOffset)) * 12.0;
                pillager.getNavigation().moveTo(flankX, target.getY(), flankZ, 1.1);
            }
        }
    }

    public void tickRaidTactics(ServerLevel level) {
        if (!FeatureConfig.get().enableIllagerTactics) {
            return;
        }

        for (Settlement settlement : SettlementManager.get().getAllSettlements()) {
            Vec3 center = new Vec3(settlement.getCenter().getX(), settlement.getCenter().getY(), settlement.getCenter().getZ());
            AABB searchArea = AABB.ofSize(center, settlement.getRadius() * 2, 30, settlement.getRadius() * 2);

            List<AbstractIllager> raidIllagers = level.getEntitiesOfClass(AbstractIllager.class, searchArea, r -> r.isAlive());
            if (raidIllagers.isEmpty()) continue;

            // 1. Building Seizure: Squad moves to seize settlement buildings and center
            for (AbstractIllager illager : raidIllagers) {
                if (illager.getTarget() == null) {
                    BlockPos targetHouse = settlement.getCenter();
                    illager.getNavigation().moveTo(targetHouse.getX(), targetHouse.getY(), targetHouse.getZ(), 1.15);
                }
            }

            // 2. Village Golem / Guard Defense Response
            List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, searchArea, g -> g.isAlive());
            for (IronGolem golem : golems) {
                if (golem.getTarget() == null && !raidIllagers.isEmpty()) {
                    golem.setTarget(raidIllagers.get(0));
                }
            }
        }
    }
}
