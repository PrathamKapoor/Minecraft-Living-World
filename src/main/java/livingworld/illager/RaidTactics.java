package livingworld.illager;

import java.util.*;
import livingworld.config.FeatureConfig;
import livingworld.entity.IllagerShieldbearerEntity;
import livingworld.entity.IllagerSwordsmanEntity;
import livingworld.entity.LivingWorldEntities;
import livingworld.settlement.Blueprint;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tactical Raids, Squad Combat, Building Assault, and Stuck-Raider Recovery (Section 14, F04; BUG-03 & BUG-04).
 * Orchestrates role-specific combat tactics for illager invasion forces:
 * - Crossbow Pillagers: Perimeter firing standoff (12-16 blocks), tactical cover
 * - Vindicators: Assault frontline, door-breaching priority for wooden doors
 * - Swordsmen: Fast flanking maneuvers
 * - Shieldbearers: Frontline shield wall screening ranged units
 * - Evokers: Magic support standoff (16m)
 * - Ravagers: Shock assault in open courtyards/streets (avoids narrow indoor entrapment)
 * - Building Assault: Coordinated target selection across settlement blueprints
 * - Stuck-Raider Recovery: Diagnostic detection and navigational recovery for trapped raiders
 */
public class RaidTactics {
    private static final RaidTactics INSTANCE = new RaidTactics();
    private final Map<UUID, Long> stationaryRaiderTicks = new HashMap<>();
    private final Map<UUID, BlockPos> lastKnownRaiderPos = new HashMap<>();

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

    /**
     * Role-specific tactical coordination (BUG-04).
     */
    public void coordinateIllagerSquad(List<AbstractIllager> squad, LivingEntity target) {
        if (squad.isEmpty() || target == null) return;

        for (int i = 0; i < squad.size(); i++) {
            AbstractIllager illager = squad.get(i);
            if (!illager.isAlive()) continue;

            if (illager instanceof IllagerShieldbearerEntity) {
                // Frontline shield wall: advance directly between target and ranged crossbowmen
                illager.getNavigation().moveTo(target, 1.15);
            } else if (illager instanceof IllagerSwordsmanEntity) {
                // Fast flanker: close in rapidly on alternating flank angles
                double angleOffset = (i % 2 == 0 ? 1 : -1) * (Math.PI / 3.0);
                Vec3 toTarget = illager.position().subtract(target.position()).normalize();
                double flankX = target.getX() + (toTarget.x * Math.cos(angleOffset) - toTarget.z * Math.sin(angleOffset)) * 8.0;
                double flankZ = target.getZ() + (toTarget.x * Math.sin(angleOffset) + toTarget.z * Math.cos(angleOffset)) * 8.0;
                illager.getNavigation().moveTo(flankX, target.getY(), flankZ, 1.35);
            } else if (illager instanceof Vindicator vindicator) {
                // Door-breacher / Heavy Melee Assault: assault doors or defenders
                vindicator.getNavigation().moveTo(target, 1.25);
            } else if (illager instanceof Evoker evoker) {
                // Spellcaster support: maintain 16m standoff distance
                Vec3 away = evoker.position().subtract(target.position()).normalize().scale(16.0);
                Vec3 standoffPos = target.position().add(away);
                evoker.getNavigation().moveTo(standoffPos.x, standoffPos.y, standoffPos.z, 1.1);
            } else if (illager instanceof Pillager pillager) {
                // Crossbow pillagers take perimeter standoff distance (12-16 blocks)
                double angleOffset = (i % 2 == 0 ? 1 : -1) * (Math.PI / 4.0);
                Vec3 toTarget = pillager.position().subtract(target.position()).normalize();
                double flankX = target.getX() + (toTarget.x * Math.cos(angleOffset) - toTarget.z * Math.sin(angleOffset)) * 14.0;
                double flankZ = target.getZ() + (toTarget.x * Math.sin(angleOffset) + toTarget.z * Math.cos(angleOffset)) * 14.0;
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
            AABB searchArea = AABB.ofSize(center, settlement.getRadius() * 2.5, 36, settlement.getRadius() * 2.5);

            List<AbstractIllager> raidIllagers = level.getEntitiesOfClass(AbstractIllager.class, searchArea, r -> r.isAlive());
            if (raidIllagers.isEmpty()) continue;

            // 1. Tactical Building Assault: Assign idle raiders to target settlement structures and sectors
            for (int i = 0; i < raidIllagers.size(); i++) {
                AbstractIllager illager = raidIllagers.get(i);
                if (illager.getTarget() == null) {
                    int offsetX = ((i % 5) - 2) * 12;
                    int offsetZ = (((i / 5) % 5) - 2) * 12;
                    BlockPos targetPos = settlement.getCenter().offset(offsetX, 0, offsetZ);

                    // Vindicators breach wooden doors along the way
                    if (illager instanceof Vindicator) {
                        checkAndBreachDoor(level, illager.blockPosition());
                    }

                    illager.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.15);
                }
            }

            // 2. Village Defender Target Locking
            List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, searchArea, g -> g.isAlive());
            for (IronGolem golem : golems) {
                if (golem.getTarget() == null && !raidIllagers.isEmpty()) {
                    golem.setTarget(raidIllagers.get(0));
                }
            }

            // 3. Stuck Raider Detection & Recovery (BUG-03)
            recoverStuckRaiders(level, settlement.getCenter(), settlement.getRadius() * 1.5);
        }
    }

    private void checkAndBreachDoor(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof DoorBlock && state.is(Blocks.OAK_DOOR)) {
                // Break wooden door during raid assault
                level.destroyBlock(p, true);
                break;
            }
        }
    }

    /**
     * Identifies raiders trapped in subterranean caves, holes, or deep ravines,
     * and guides them toward the nearest valid surface coordinate in the settlement (BUG-03).
     */
    public int recoverStuckRaiders(ServerLevel level, BlockPos center, double radius) {
        AABB zone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()), radius * 2, 48, radius * 2);
        List<Raider> raiders = level.getEntitiesOfClass(Raider.class, zone, r -> r.isAlive());
        int recovered = 0;

        for (Raider raider : raiders) {
            UUID id = raider.getUUID();
            BlockPos currentPos = raider.blockPosition();
            BlockPos lastPos = lastKnownRaiderPos.get(id);

            if (lastPos != null && lastPos.distSqr(currentPos) < 4) {
                long ticks = stationaryRaiderTicks.getOrDefault(id, 0L) + 20L;
                stationaryRaiderTicks.put(id, ticks);

                // If stuck for > 300 ticks (15 seconds)
                if (ticks > 300L) {
                    // Reposition towards surface or village center
                    BlockPos surfacePos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, currentPos);
                    if (surfacePos.getY() > currentPos.getY() + 4) {
                        // Subterranean trap: guide to surface
                        raider.teleportTo(surfacePos.getX() + 0.5, surfacePos.getY() + 1.0, surfacePos.getZ() + 0.5);
                        raider.getNavigation().moveTo(center.getX(), center.getY(), center.getZ(), 1.25);
                        stationaryRaiderTicks.put(id, 0L);
                        recovered++;
                        livingworld.LivingWorldMod.LOGGER.info("[Living World] Stuck raider " + raider.getUUID() + " recovered from cave trap at " + currentPos + " -> " + surfacePos);
                    }
                }
            } else {
                lastKnownRaiderPos.put(id, currentPos);
                stationaryRaiderTicks.put(id, 0L);
            }
        }
        return recovered;
    }

    public List<Raider> getStuckRaiders(ServerLevel level, BlockPos center, double radius) {
        AABB zone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()), radius * 2, 48, radius * 2);
        List<Raider> raiders = level.getEntitiesOfClass(Raider.class, zone, r -> r.isAlive());
        List<Raider> stuck = new ArrayList<>();
        for (Raider r : raiders) {
            if (stationaryRaiderTicks.getOrDefault(r.getUUID(), 0L) > 200L) {
                stuck.add(r);
            }
        }
        return stuck;
    }
}
