package livingworld.illager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.config.FeatureConfig;
import livingworld.economy.Stockpile;
import livingworld.entity.IllagerShieldbearerEntity;
import livingworld.entity.IllagerSwordsmanEntity;
import livingworld.entity.LivingWorldEntities;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tactical Raids, Squad Combat, Village Bell Objective Machine & Stuck-Raider Recovery (v0.2.7).
 * Implements:
 * 1. Strategic Village Bell Discovery & Objective Registration
 * 2. 7-State Tactical Assault Machine: ASSEMBLE -> ADVANCE -> ENGAGE_DEFENDERS -> BREACH -> ATTACK_BELL -> SECURE_VILLAGE -> VICTORY
 * 3. Role-specific tactical coordination (Crossbow Standoff, Vindicator Breaching, Shield Wall, Flankers)
 * 4. Physical Bell Attack Mechanic: Strike animations, audio/visual cues, block destruction / silencing, and civilian alarm disruption
 * 5. 160m-Radius Stuck-Raider Navigation Diagnostics, Underground Cave Repositioning & Bounded Recovery / Glowing / Despawn
 * 6. Material-driven Bell Reconstruction by surviving villagers
 */
public class RaidTactics {
    private static final RaidTactics INSTANCE = new RaidTactics();

    public enum RaidAssaultState {
        ASSEMBLE("Gathering Invasion Forces"),
        ADVANCE("Advancing on Village Roads"),
        ENGAGE_DEFENDERS("Clashing with Defenders"),
        BREACH("Breaching Fortifications & Doors"),
        ATTACK_BELL("Besieging Village Bell"),
        SECURE_VILLAGE("Securing Settlement Center"),
        VICTORY("Raid Victory");

        private final String display;

        RaidAssaultState(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    public static class RaidObjective {
        private final int raidId;
        private final UUID settlementId;
        private BlockPos bellPos;
        private boolean hasPhysicalBell;
        private RaidAssaultState state = RaidAssaultState.ADVANCE;
        private int bellSiegeTicks = 0;
        private boolean bellDestroyedOrCaptured = false;
        private int activeRaiderCount = 0;

        public RaidObjective(int raidId, UUID settlementId, BlockPos bellPos, boolean hasPhysicalBell) {
            this.raidId = raidId;
            this.settlementId = settlementId;
            this.bellPos = bellPos;
            this.hasPhysicalBell = hasPhysicalBell;
        }

        public int getRaidId() { return raidId; }
        public UUID getSettlementId() { return settlementId; }
        public BlockPos getBellPos() { return bellPos; }
        public void setBellPos(BlockPos pos) { this.bellPos = pos; }
        public boolean hasPhysicalBell() { return hasPhysicalBell; }
        public void setHasPhysicalBell(boolean has) { this.hasPhysicalBell = has; }
        public RaidAssaultState getState() { return state; }
        public void setState(RaidAssaultState state) { this.state = state; }
        public int getBellSiegeTicks() { return bellSiegeTicks; }
        public void addBellSiegeTicks(int ticks) { this.bellSiegeTicks += ticks; }
        public void resetBellSiegeTicks() { this.bellSiegeTicks = 0; }
        public boolean isBellDestroyedOrCaptured() { return bellDestroyedOrCaptured; }
        public void setBellDestroyedOrCaptured(boolean val) { this.bellDestroyedOrCaptured = val; }
        public int getActiveRaiderCount() { return activeRaiderCount; }
        public void setActiveRaiderCount(int count) { this.activeRaiderCount = count; }
    }

    public static class RaiderDiagnostic {
        private final UUID uuid;
        private final String entityType;
        private BlockPos currentPos;
        private final int raidId;
        private String currentObjective = "ADVANCE_TO_BELL";
        private double distanceToObjective = 0.0;
        private long stationaryTicks = 0;
        private boolean underground = false;
        private boolean trapped = false;
        private String currentTargetName = "None";

        public RaiderDiagnostic(UUID uuid, String entityType, BlockPos pos, int raidId) {
            this.uuid = uuid;
            this.entityType = entityType;
            this.currentPos = pos;
            this.raidId = raidId;
        }

        public UUID getUuid() { return uuid; }
        public String getEntityType() { return entityType; }
        public BlockPos getCurrentPos() { return currentPos; }
        public void setCurrentPos(BlockPos pos) { this.currentPos = pos; }
        public int getRaidId() { return raidId; }
        public String getCurrentObjective() { return currentObjective; }
        public void setCurrentObjective(String obj) { this.currentObjective = obj; }
        public double getDistanceToObjective() { return distanceToObjective; }
        public void setDistanceToObjective(double dist) { this.distanceToObjective = dist; }
        public long getStationaryTicks() { return stationaryTicks; }
        public void setStationaryTicks(long ticks) { this.stationaryTicks = ticks; }
        public boolean isUnderground() { return underground; }
        public void setUnderground(boolean ug) { this.underground = ug; }
        public boolean isTrapped() { return trapped; }
        public void setTrapped(boolean tr) { this.trapped = tr; }
        public String getCurrentTargetName() { return currentTargetName; }
        public void setCurrentTargetName(String target) { this.currentTargetName = target; }
    }

    private final Map<Integer, RaidObjective> raidObjectives = new ConcurrentHashMap<>();
    private final Map<UUID, RaiderDiagnostic> raiderDiagnostics = new ConcurrentHashMap<>();
    private final Map<UUID, BlockPos> lastKnownPositions = new ConcurrentHashMap<>();

    public static RaidTactics get() {
        return INSTANCE;
    }

    public RaidObjective getRaidObjective(int raidId) {
        return raidObjectives.get(raidId);
    }

    public Collection<RaidObjective> getAllRaidObjectives() {
        return Collections.unmodifiableCollection(raidObjectives.values());
    }

    public RaiderDiagnostic getDiagnostic(UUID raiderUuid) {
        return raiderDiagnostics.get(raiderUuid);
    }

    public Collection<RaiderDiagnostic> getAllDiagnostics() {
        return Collections.unmodifiableCollection(raiderDiagnostics.values());
    }

    public void setupPillagerTactics(Pillager pillager) {
        if (!FeatureConfig.get().enableIllagerTactics) {
            return;
        }

        if (pillager.level() instanceof ServerLevel level) {
            // 1 in 4 patrol encounters converts into a specialized illager warrior
            if (pillager.getRandom().nextInt(4) == 0 && !pillager.isPatrolLeader()) {
                BlockPos pos = pillager.blockPosition();
                boolean isShield = pillager.getRandom().nextBoolean();
                if (isShield) {
                    IllagerShieldbearerEntity shieldbearer = LivingWorldEntities.ILLAGER_SHIELDBEARER.create(level, EntitySpawnReason.EVENT);
                    if (shieldbearer != null) {
                        shieldbearer.setPos(pillager.getX(), pillager.getY(), pillager.getZ());
                        level.addFreshEntity(shieldbearer);
                        pillager.discard();
                        livingworld.LivingWorldMod.LOGGER.info("[Living World] Mobilized Illager Shieldbearer at " + pos);
                        return;
                    }
                } else {
                    IllagerSwordsmanEntity swordsman = LivingWorldEntities.ILLAGER_SWORDSMAN.create(level, EntitySpawnReason.EVENT);
                    if (swordsman != null) {
                        swordsman.setPos(pillager.getX(), pillager.getY(), pillager.getZ());
                        level.addFreshEntity(swordsman);
                        pillager.discard();
                        livingworld.LivingWorldMod.LOGGER.info("[Living World] Mobilized Illager Swordsman at " + pos);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Primary tick loop: orchestrates raid bell discovery, tactical movement, bell siege, and stuck recovery.
     */
    public void tickRaidTactics(ServerLevel level) {
        if (!FeatureConfig.get().enableIllagerTactics || level == null) {
            return;
        }

        for (Settlement settlement : SettlementManager.get().getAllSettlements()) {
            BlockPos villageCenter = settlement.getCenter();
            Raid raid = level.getRaidAt(villageCenter);

            // If no active raid at center, also probe active raids nearby
            if (raid == null || raid.isStopped()) {
                cleanUpFinishedRaid(level, settlement);
                continue;
            }

            int raidId = settlement.getId().hashCode();
            RaidObjective objective = raidObjectives.computeIfAbsent(raidId, id -> {
                BlockPos bell = findVillageBell(level, villageCenter, settlement.getRadius());
                boolean hasBell = (bell != null);
                BlockPos targetPos = hasBell ? bell : villageCenter;
                livingworld.LivingWorldMod.LOGGER.info(String.format("[Living World] Raid %d registered objective: %s at %s",
                    raidId, (hasBell ? "Village Bell" : "Village Center"), targetPos));
                return new RaidObjective(raidId, settlement.getId(), targetPos, hasBell);
            });

            // Ensure bell position is still verified
            if (objective.hasPhysicalBell() && !objective.isBellDestroyedOrCaptured()) {
                if (!level.isLoaded(objective.getBellPos()) || !level.getBlockState(objective.getBellPos()).is(Blocks.BELL)) {
                    objective.setHasPhysicalBell(false);
                }
            }

            BlockPos targetBellPos = objective.getBellPos();

            // Broad 160m radius search for all active raiders participating in this raid
            AABB raidArea = AABB.ofSize(new Vec3(targetBellPos.getX(), targetBellPos.getY(), targetBellPos.getZ()), 320, 96, 320);
            List<Raider> activeRaiders = level.getEntitiesOfClass(Raider.class, raidArea, r -> r.isAlive());
            objective.setActiveRaiderCount(activeRaiders.size());

            boolean anyEngagingDefenders = false;
            boolean anyBreaching = false;
            boolean anyAttackingBell = false;

            for (Raider raider : activeRaiders) {
                UUID rId = raider.getUUID();
                BlockPos rPos = raider.blockPosition();
                double distToObjective = Math.sqrt(rPos.distSqr(targetBellPos));

                // 1. Navigation Diagnostics Update
                RaiderDiagnostic diag = raiderDiagnostics.computeIfAbsent(rId,
                    id -> new RaiderDiagnostic(id, raider.getType().toShortString(), rPos, raidId));
                diag.setCurrentPos(rPos);
                diag.setDistanceToObjective(distToObjective);

                BlockPos lastPos = lastKnownPositions.get(rId);
                if (lastPos != null && lastPos.distSqr(rPos) < 2.0) {
                    diag.setStationaryTicks(diag.getStationaryTicks() + 1L);
                } else {
                    diag.setStationaryTicks(0L);
                    lastKnownPositions.put(rId, rPos);
                }

                // Check underground / trapped status
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, rPos.getX(), rPos.getZ());
                boolean isUnderground = (surfaceY > rPos.getY() + 4);
                diag.setUnderground(isUnderground);
                diag.setTrapped(isUnderground || (diag.getStationaryTicks() > 100L));

                LivingEntity target = raider.getTarget();
                diag.setCurrentTargetName(target != null ? target.getName().getString() : "None");

                // 2. Role-specific tactical movement toward the bell
                if (target == null || !target.isAlive()) {
                    diag.setCurrentObjective("ADVANCE_TO_BELL");

                    // Door breaching behavior for Vindicators along the route
                    if (raider instanceof Vindicator) {
                        boolean breached = checkAndBreachDoor(level, rPos);
                        if (breached) anyBreaching = true;
                    }

                    // Move toward village bell / center
                    if (distToObjective > 3.0) {
                        if (raider instanceof Pillager) {
                            // Pillagers take calculated approach along terrain
                            raider.getNavigation().moveTo(targetBellPos.getX(), targetBellPos.getY(), targetBellPos.getZ(), 1.15);
                        } else if (raider instanceof IllagerShieldbearerEntity) {
                            // Shieldbearer advances steadily at frontline
                            raider.getNavigation().moveTo(targetBellPos.getX(), targetBellPos.getY(), targetBellPos.getZ(), 1.10);
                        } else {
                            raider.getNavigation().moveTo(targetBellPos.getX(), targetBellPos.getY(), targetBellPos.getZ(), 1.25);
                        }
                    }
                } else {
                    anyEngagingDefenders = true;
                    diag.setCurrentObjective("ENGAGE_DEFENDER: " + target.getName().getString());
                }

                // 3. Physical Bell Assault Mechanic
                if (distToObjective <= 3.5 && !objective.isBellDestroyedOrCaptured()) {
                    if (raider instanceof Vindicator || raider instanceof IllagerSwordsmanEntity || raider instanceof Ravager) {
                        anyAttackingBell = true;
                        diag.setCurrentObjective("ATTACKING_BELL");
                        performBellAssault(level, raider, targetBellPos, objective);
                    }
                }

                // 4. Stuck Detection, Underground Recovery & Bounded Completion Guard
                handleStuckRaider(level, raider, diag, targetBellPos, raid.getTotalRaidersAlive());
            }

            // Update tactical state machine
            if (objective.isBellDestroyedOrCaptured()) {
                objective.setState(RaidAssaultState.SECURE_VILLAGE);
            } else if (anyAttackingBell) {
                objective.setState(RaidAssaultState.ATTACK_BELL);
            } else if (anyBreaching) {
                objective.setState(RaidAssaultState.BREACH);
            } else if (anyEngagingDefenders) {
                objective.setState(RaidAssaultState.ENGAGE_DEFENDERS);
            } else if (activeRaiders.isEmpty()) {
                objective.setState(raid.isVictory() ? RaidAssaultState.VICTORY : RaidAssaultState.ASSEMBLE);
            } else {
                objective.setState(RaidAssaultState.ADVANCE);
            }

            // Village Golem target locking
            AABB golemZone = AABB.ofSize(new Vec3(villageCenter.getX(), villageCenter.getY(), villageCenter.getZ()), settlement.getRadius() * 2, 36, settlement.getRadius() * 2);
            List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, golemZone, g -> g.isAlive());
            for (IronGolem golem : golems) {
                if (golem.getTarget() == null && !activeRaiders.isEmpty()) {
                    golem.setTarget(activeRaiders.get(0));
                }
            }
        }
    }

    /**
     * Executes physical bell attack by melee raider: strike animation, metallic sounds, particles, and destruction/silencing.
     */
    private void performBellAssault(ServerLevel level, Raider raider, BlockPos bellPos, RaidObjective objective) {
        raider.swingForAttack(InteractionHand.MAIN_HAND);
        objective.addBellSiegeTicks(5);

        // Audible and visual impact
        level.playSound(null, bellPos, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.2F, 0.85F);
        level.sendParticles(ParticleTypes.CRIT, bellPos.getX() + 0.5, bellPos.getY() + 0.5, bellPos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);

        if (objective.getBellSiegeTicks() >= 100) { // Approx 5 seconds of sustained siege
            objective.setBellDestroyedOrCaptured(true);
            Boolean griefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
            boolean canGrief = (griefing == null || griefing);

            if (canGrief && level.getBlockState(bellPos).is(Blocks.BELL)) {
                // Physically destroy bell block with realistic debris drop
                level.destroyBlock(bellPos, true);
                level.playSound(null, bellPos, SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 1.5F, 0.7F);
                level.sendParticles(ParticleTypes.EXPLOSION, bellPos.getX() + 0.5, bellPos.getY() + 0.5, bellPos.getZ() + 0.5, 1, 0, 0, 0, 0);
            } else {
                // Silenced / captured
                level.playSound(null, bellPos, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.8F, 0.5F);
            }

            livingworld.LivingWorldMod.LOGGER.info("[Living World] Raid objective accomplished: Village Bell at " + bellPos + " neutralized by " + raider.getName().getString());
        }
    }

    /**
     * Handles stuck or lost raiders with bounded recovery, glowing pulses, and failsafe resolution.
     */
    private void handleStuckRaider(ServerLevel level, Raider raider, RaiderDiagnostic diag, BlockPos targetBellPos, int totalRaidersAlive) {
        long stuckTicks = diag.getStationaryTicks();

        // 1. Stuck for > 200 ticks (10 seconds)
        if (stuckTicks > 200L && stuckTicks % 40 == 0) {
            BlockPos currentPos = raider.blockPosition();
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, currentPos.getX(), currentPos.getZ());

            if (surfaceY > currentPos.getY() + 4) {
                // Underground cave/ravine trap: guide to surface
                raider.teleportTo(currentPos.getX() + 0.5, surfaceY + 1.0, currentPos.getZ() + 0.5);
                raider.getNavigation().moveTo(targetBellPos.getX(), targetBellPos.getY(), targetBellPos.getZ(), 1.25);
                diag.setStationaryTicks(0L);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Recovered subterranean raider " + raider.getUUID() + " from Y=" + currentPos.getY() + " to surface Y=" + surfaceY);
                return;
            } else {
                // Re-calculate route to bell
                raider.getNavigation().moveTo(targetBellPos.getX(), targetBellPos.getY(), targetBellPos.getZ(), 1.2);
            }
        }

        // 2. Stuck for > 600 ticks (30 seconds): emit glowing effect and warning audio so players/guards can locate
        if (stuckTicks > 600L && stuckTicks % 100 == 0) {
            raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            level.playSound(null, raider.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.8F, 0.8F);
        }

        // 3. Failsafe resolution for lingering/inaccessible last raider:
        // If <= 3 raiders remain and this raider is stuck > 1200 ticks (60s) or > 120 blocks away without progress
        if (totalRaidersAlive <= 3 && stuckTicks > 1200L) {
            livingworld.LivingWorldMod.LOGGER.warn("[Living World] Lingering inaccessible raider " + raider.getUUID() + " failsafe resolved to prevent hanging raid boss bar.");
            raider.discard();
        }
    }

    /**
     * Finds the primary accessible village bell in the settlement.
     */
    public BlockPos findVillageBell(ServerLevel level, BlockPos center, int radius) {
        int r = Math.min(64, radius);
        for (int y = -8; y <= 16; y++) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    BlockPos p = center.offset(dx, y, dz);
                    if (level.isLoaded(p) && level.getBlockState(p).is(Blocks.BELL)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private boolean checkAndBreachDoor(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
            if (!level.isLoaded(p)) continue;
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof DoorBlock) {
                // Destroy wooden door to open assault route
                level.destroyBlock(p, true);
                level.playSound(null, p, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    /**
     * Cleans up finished raid and restores lost bells through community reconstruction.
     */
    private void cleanUpFinishedRaid(ServerLevel level, Settlement settlement) {
        Iterator<Map.Entry<Integer, RaidObjective>> it = raidObjectives.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RaidObjective> entry = it.next();
            RaidObjective obj = entry.getValue();
            if (obj.getSettlementId().equals(settlement.getId())) {
                // If bell was destroyed and settlement survived, reconstruct bell using stockpiled materials
                if (obj.isBellDestroyedOrCaptured() && obj.getBellPos() != null) {
                    BlockPos bPos = obj.getBellPos();
                    if (level.isLoaded(bPos) && !level.getBlockState(bPos).is(Blocks.BELL)) {
                        Stockpile stock = settlement.getStockpile();
                        if (stock.get(Stockpile.ResourceType.STONE) >= 5) {
                            stock.consume(Stockpile.ResourceType.STONE, 5);
                            level.setBlock(bPos, Blocks.BELL.defaultBlockState(), 3);
                            level.playSound(null, bPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                            livingworld.LivingWorldMod.LOGGER.info("[Living World] Village " + settlement.getName() + " reconstructed destroyed bell with stone at " + bPos);
                        } else if (stock.get(Stockpile.ResourceType.IRON) >= 1) {
                            stock.consume(Stockpile.ResourceType.IRON, 1);
                            level.setBlock(bPos, Blocks.BELL.defaultBlockState(), 3);
                            level.playSound(null, bPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                            livingworld.LivingWorldMod.LOGGER.info("[Living World] Village " + settlement.getName() + " reconstructed destroyed bell with iron at " + bPos);
                        }
                    }
                }
                it.remove();
            }
        }
    }

    public int recoverStuckRaiders(ServerLevel level, BlockPos center, double radius) {
        AABB zone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()), radius * 2, 64, radius * 2);
        List<Raider> raiders = level.getEntitiesOfClass(Raider.class, zone, r -> r.isAlive());
        int recovered = 0;

        for (Raider raider : raiders) {
            UUID id = raider.getUUID();
            BlockPos currentPos = raider.blockPosition();
            BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, currentPos);

            if (surfaceYIsSubterranean(currentPos, surfaceY(level, currentPos))) {
                raider.teleportTo(surfacePos.getX() + 0.5, surfacePos.getY() + 1.0, surfacePos.getZ() + 0.5);
                raider.getNavigation().moveTo(center.getX(), center.getY(), center.getZ(), 1.25);
                recovered++;
            } else {
                raider.getNavigation().moveTo(center.getX(), center.getY(), center.getZ(), 1.25);
                recovered++;
            }
        }
        return recovered;
    }

    private static int surfaceY(ServerLevel level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
    }

    private static boolean surfaceYIsSubterranean(BlockPos pos, int surfaceY) {
        return surfaceY > pos.getY() + 4;
    }

    public List<Raider> getStuckRaiders(ServerLevel level, BlockPos center, double radius) {
        AABB zone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()), radius * 2, 64, radius * 2);
        List<Raider> raiders = level.getEntitiesOfClass(Raider.class, zone, r -> r.isAlive());
        List<Raider> stuck = new ArrayList<>();
        for (Raider r : raiders) {
            RaiderDiagnostic diag = raiderDiagnostics.get(r.getUUID());
            if (diag != null && (diag.isTrapped() || diag.getStationaryTicks() > 100L)) {
                stuck.add(r);
            }
        }
        return stuck;
    }

    public void clear() {
        raidObjectives.clear();
        raiderDiagnostics.clear();
        lastKnownPositions.clear();
    }
}
