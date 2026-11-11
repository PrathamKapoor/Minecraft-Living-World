package livingworld.faction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.entity.VillageArcherEntity;
import livingworld.entity.VillageGuardEntity;
import livingworld.settlement.Settlement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Unified Settlement Threat-Response & Authoritative Defense Controller (v0.2.8 Stabilization).
 * Replaces uncontrolled, uncoordinated target chasing with centralized situational awareness:
 * - Authoritative ThreatRecord per hostile entity tracking severity, victims, objectives, breach capability.
 * - Context-sensitive defender partitioning (Commander Response, Frontline Horde Intercept,
 *   Ranged Fire Support, Civilian Emergency Defense, Reserves).
 * - Target hysteresis preventing tick-by-tick target thrashing and random guard spinning.
 * - Dedicated Civilian Distress integration dispatching immediate protection for attacked villagers.
 * - Autonomous Civilian Shelter Coordination sealing residents inside uncompromised structures.
 */
public class VillageDefenseController {
    private static final VillageDefenseController INSTANCE = new VillageDefenseController();

    public static VillageDefenseController get() {
        return INSTANCE;
    }

    public enum ThreatLevel {
        PEACEFUL("§aPeaceful", 0),
        SUSPICIOUS("§eSuspicious", 1),
        ALERT("§6Alert", 2),
        UNDER_ATTACK("§cUnder Attack", 3),
        RECOVERY("§bRecovery", 1);

        private final String display;
        private final int severity;

        ThreatLevel(String display, int severity) {
            this.display = display;
            this.severity = severity;
        }

        public String getDisplay() {
            return display;
        }

        public int getSeverity() {
            return severity;
        }
    }

    /**
     * Authoritative Threat Record for Settlement Defense (Section 4, v0.2.8).
     */
    public static class ThreatRecord {
        private final UUID threatUuid;
        private final EntityType<?> entityType;
        private BlockPos location;
        private double severity;
        private UUID victimUuid; // Attacked villager UUID, if any
        private boolean isBreaching;
        private boolean isHordeMember;
        private boolean isCommander;
        private final Set<UUID> assignedDefenders = new HashSet<>();
        private final long firstObservedTick;
        private long lastConfirmedTick;

        public ThreatRecord(UUID threatUuid, EntityType<?> entityType, BlockPos location, double severity,
                            UUID victimUuid, boolean isBreaching, boolean isHordeMember, boolean isCommander, long tick) {
            this.threatUuid = threatUuid;
            this.entityType = entityType;
            this.location = location;
            this.severity = severity;
            this.victimUuid = victimUuid;
            this.isBreaching = isBreaching;
            this.isHordeMember = isHordeMember;
            this.isCommander = isCommander;
            this.firstObservedTick = tick;
            this.lastConfirmedTick = tick;
        }

        public UUID getThreatUuid() { return threatUuid; }
        public EntityType<?> getEntityType() { return entityType; }
        public BlockPos getLocation() { return location; }
        public void setLocation(BlockPos location) { this.location = location; }
        public double getSeverity() { return severity; }
        public void setSeverity(double severity) { this.severity = severity; }
        public UUID getVictimUuid() { return victimUuid; }
        public void setVictimUuid(UUID victimUuid) { this.victimUuid = victimUuid; }
        public boolean isBreaching() { return isBreaching; }
        public void setBreaching(boolean breaching) { isBreaching = breaching; }
        public boolean isHordeMember() { return isHordeMember; }
        public boolean isCommander() { return isCommander; }
        public Set<UUID> getAssignedDefenders() { return assignedDefenders; }
        public long getFirstObservedTick() { return firstObservedTick; }
        public long getLastConfirmedTick() { return lastConfirmedTick; }
        public void setLastConfirmedTick(long tick) { this.lastConfirmedTick = tick; }
    }

    public static class SettlementDefenseState {
        private final UUID settlementId;
        private ThreatLevel threatLevel = ThreatLevel.PEACEFUL;
        private long lastThreatTimestamp = 0;
        private long lastAlarmTimestamp = 0;
        private long lastRallyTick = 0;
        private BlockPos activeThreatPos = null;
        private String primaryThreatDescription = "None";
        private int activeHostileCount = 0;
        private boolean lockdownActive = false;
        private final Map<UUID, ThreatRecord> threatRecords = new ConcurrentHashMap<>();

        public SettlementDefenseState(UUID settlementId) {
            this.settlementId = settlementId;
        }

        public UUID getSettlementId() { return settlementId; }
        public ThreatLevel getThreatLevel() { return threatLevel; }
        public void setThreatLevel(ThreatLevel threatLevel) { this.threatLevel = threatLevel; }
        public BlockPos getActiveThreatPos() { return activeThreatPos; }
        public boolean isLockdownActive() { return lockdownActive; }
        public String getPrimaryThreatDescription() { return primaryThreatDescription; }
        public int getActiveHostileCount() { return activeHostileCount; }
        public Map<UUID, ThreatRecord> getThreatRecords() { return threatRecords; }
    }

    private final Map<UUID, SettlementDefenseState> defenseStates = new ConcurrentHashMap<>();
    private static final long ALARM_COOLDOWN_MS = 25_000; // Horn alarm sound cooldown: 25 seconds
    private static final long UNDER_ATTACK_TIMEOUT_MS = 15_000; // 15 seconds without hostiles -> Recovery

    public SettlementDefenseState getState(UUID settlementId) {
        return defenseStates.computeIfAbsent(settlementId, SettlementDefenseState::new);
    }

    /**
     * Reports a confirmed threat to the settlement.
     * Triggers goat horn alarm, awakens sleeping guards, and coordinates defense rally.
     */
    public void reportThreat(ServerLevel level, Settlement settlement, BlockPos threatPos, LivingEntity threatSource, String description) {
        if (settlement == null || level == null) return;

        SettlementDefenseState state = getState(settlement.getId());
        long now = System.currentTimeMillis();
        long currentTick = level.getGameTime();

        state.lastThreatTimestamp = now;
        state.activeThreatPos = threatPos != null ? threatPos : settlement.getCenter();
        state.primaryThreatDescription = description != null ? description : "Hostile Incursion";
        state.threatLevel = ThreatLevel.UNDER_ATTACK;
        state.lockdownActive = true;

        // Audible Goat-Horn Alarm with rate-limiting
        if (now - state.lastAlarmTimestamp > ALARM_COOLDOWN_MS) {
            state.lastAlarmTimestamp = now;
            soundGoatHornAlarm(level, settlement, state.activeThreatPos);
        }

        // Rate-limit defender allocation to once every 20 ticks (1s) to prevent rapid target oscillation
        if (currentTick - state.lastRallyTick >= 20 || threatSource != null) {
            state.lastRallyTick = currentTick;
            rallyDefenders(level, settlement, state.activeThreatPos, threatSource);
        }
    }

    /**
     * Handles an emergency Civilian Distress signal from an attacked villager (Section 5).
     */
    public void handleCivilianDistress(ServerLevel level, Settlement settlement, CivilianDistressManager.DistressIncident incident,
                                       Villager victim, LivingEntity attacker) {
        if (settlement == null || level == null || victim == null || attacker == null) return;

        SettlementDefenseState state = getState(settlement.getId());
        state.threatLevel = ThreatLevel.UNDER_ATTACK;
        state.lockdownActive = true;
        state.activeThreatPos = victim.blockPosition();
        state.primaryThreatDescription = "Civilian Attacked: " + attacker.getType().getDescription().getString();

        // Register attacker as high-priority ThreatRecord
        ThreatRecord record = state.getThreatRecords().computeIfAbsent(attacker.getUUID(), id ->
            new ThreatRecord(id, attacker.getType(), attacker.blockPosition(), 18.0,
                victim.getUUID(), false, false, false, level.getGameTime())
        );
        record.setSeverity(18.0);
        record.setVictimUuid(victim.getUUID());
        record.setLocation(attacker.blockPosition());
        record.setLastConfirmedTick(level.getGameTime());

        // Dispatch immediate emergency responders to save civilian
        CivilianDistressManager.get().dispatchEmergencyResponders(level, victim.blockPosition(), victim, attacker, 2);
    }

    /**
     * Plays authentic goat horn alarm sound audible across the settlement.
     */
    public void soundGoatHornAlarm(ServerLevel level, Settlement settlement, BlockPos alarmPos) {
        BlockPos center = alarmPos != null ? alarmPos : settlement.getCenter();

        level.playSound(null, center, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.NEUTRAL, 3.5f, 0.9f);
        level.playSound(null, center, SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 2.0f, 1.0f);

        level.sendParticles(ParticleTypes.NOTE, center.getX() + 0.5, center.getY() + 3.0, center.getZ() + 0.5, 12, 1.0, 1.0, 1.0, 0.2);
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Horn Sounded in " + settlement.getName() + "! Threat: " + getState(settlement.getId()).primaryThreatDescription);
    }

    /**
     * Context-sensitive threat scoring function (Section 4.4).
     */
    public double calculateThreatScore(LivingEntity threat, BlockPos villageCenter, List<Villager> nearbyCivilians) {
        if (threat == null || !threat.isAlive()) return 0.0;
        double score = 1.0;

        // 1. Boss / Commander Tier
        if (threat instanceof livingworld.entity.MutantZombieCommanderEntity) {
            score += 15.0;
        } else {
            net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(threat.getType());
            if (id != null && "mutantmonsters".equals(id.getNamespace())) {
                score += 12.0;
            }
        }

        // 2. Siege & Breaching Capability (Tool miners)
        if (threat.getMainHandItem().is(net.minecraft.world.item.Items.IRON_PICKAXE) || threat.getMainHandItem().is(net.minecraft.world.item.Items.DIAMOND_PICKAXE)) {
            score += 6.0;
        }

        // 3. Civilian proximity / Active civilian attack
        if (threat instanceof Monster m && m.getTarget() instanceof Villager) {
            score += 12.0; // Actively attacking civilian
        } else {
            double closestCivilianDist = Double.MAX_VALUE;
            for (Villager v : nearbyCivilians) {
                double d = v.distanceToSqr(threat);
                if (d < closestCivilianDist) closestCivilianDist = d;
            }
            if (closestCivilianDist < 64.0) {
                score += 8.0;
            } else if (closestCivilianDist < 256.0) {
                score += 4.0;
            }
        }

        // 4. Proximity to village center
        double distToCenter = Math.sqrt(threat.distanceToSqr(villageCenter.getX(), villageCenter.getY(), villageCenter.getZ()));
        if (distToCenter < 24.0) {
            score += 5.0;
        } else if (distToCenter < 48.0) {
            score += 2.0;
        }

        return score;
    }

    /**
     * Centralized Defender Allocation Machine (Section 4 & 5).
     * Distributes available defenders intelligently with target hysteresis.
     */
    private void rallyDefenders(ServerLevel level, Settlement settlement, BlockPos threatPos, LivingEntity threatEntity) {
        BlockPos center = settlement.getCenter();
        AABB defenseZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2.5, 32, settlement.getRadius() * 2.5);

        List<Villager> civilians = level.getEntitiesOfClass(Villager.class, defenseZone, v -> v.isAlive());
        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class, defenseZone,
            m -> m.isAlive() && !AllegianceRegistry.get().isVillageDefender(m));

        SettlementDefenseState state = getState(settlement.getId());
        long currentTick = level.getGameTime();

        // Update active threat records
        Set<UUID> activeHostileUuids = new HashSet<>();
        for (Monster m : hostiles) {
            activeHostileUuids.add(m.getUUID());
            double score = calculateThreatScore(m, center, civilians);
            boolean isMiner = m.getMainHandItem().is(net.minecraft.world.item.Items.IRON_PICKAXE) || m.getMainHandItem().is(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
            boolean isCommander = m instanceof livingworld.entity.MutantZombieCommanderEntity;

            ThreatRecord rec = state.getThreatRecords().computeIfAbsent(m.getUUID(), id ->
                new ThreatRecord(id, m.getType(), m.blockPosition(), score, null, isMiner, hostiles.size() > 4, isCommander, currentTick)
            );
            rec.setLocation(m.blockPosition());
            rec.setSeverity(score);
            rec.setLastConfirmedTick(currentTick);
            if (m.getTarget() instanceof Villager v) {
                rec.setVictimUuid(v.getUUID());
            }
        }
        // Purge records for dead/vanished hostiles
        state.getThreatRecords().keySet().removeIf(id -> !activeHostileUuids.contains(id));

        // Sort hostiles by severity descending
        hostiles.sort((a, b) -> Double.compare(
            calculateThreatScore(b, center, civilians),
            calculateThreatScore(a, center, civilians)
        ));

        LivingEntity primaryBoss = null;
        for (Monster m : hostiles) {
            if (m instanceof livingworld.entity.MutantZombieCommanderEntity || calculateThreatScore(m, center, civilians) >= 12.0) {
                primaryBoss = m;
                break;
            }
        }

        // 1. Melee Guards: Allocate with hysteresis
        List<VillageGuardEntity> guards = level.getEntitiesOfClass(VillageGuardEntity.class, defenseZone, g -> g.isAlive());
        int guardCount = guards.size();

        for (int i = 0; i < guardCount; i++) {
            VillageGuardEntity guard = guards.get(i);
            if (guard.isSleeping()) {
                guard.stopSleeping();
            }
            AllegianceRegistry.get().sanitizeDefenderTarget(guard);

            // Target Hysteresis: If guard already has a living target that is in range and valid, keep it!
            LivingEntity currentTarget = guard.getTarget();
            if (currentTarget != null && currentTarget.isAlive() &&
                !AllegianceRegistry.get().areAllies(guard, currentTarget) &&
                guard.distanceToSqr(currentTarget) <= (guard.getMaxPursuitDistance() * guard.getMaxPursuitDistance())) {
                continue; // Do not switch targets! (Prevents rapid rotation and spinning)
            }

            // Allocate new target
            if (primaryBoss != null && i < 2) {
                // Commander Response Squad
                guard.setTarget(primaryBoss);
            } else if (!hostiles.isEmpty()) {
                // Frontline Horde Intercept: distribute evenly among advancing hostiles
                Monster assignedThreat = hostiles.get(i % hostiles.size());
                guard.setTarget(assignedThreat);
            } else if (guard.getTarget() == null || !guard.getTarget().isAlive()) {
                guard.getNavigation().moveTo(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 1.3);
            }
        }

        // 2. Archers & Sentries: Ranged Standoff
        List<VillageArcherEntity> archers = level.getEntitiesOfClass(VillageArcherEntity.class, defenseZone, a -> a.isAlive());
        for (int i = 0; i < archers.size(); i++) {
            VillageArcherEntity archer = archers.get(i);
            if (archer.isSleeping()) {
                archer.stopSleeping();
            }
            AllegianceRegistry.get().sanitizeDefenderTarget(archer);

            // Target Hysteresis for archers
            LivingEntity currentTarget = archer.getTarget();
            if (currentTarget != null && currentTarget.isAlive() &&
                !AllegianceRegistry.get().areAllies(archer, currentTarget) &&
                archer.distanceToSqr(currentTarget) <= (archer.getMaxPursuitDistance() * archer.getMaxPursuitDistance())) {
                continue; // Keep current target!
            }

            if (!hostiles.isEmpty()) {
                // Archers target secondary flanking hostiles or boss
                Monster assignedThreat = hostiles.get((i + 1) % hostiles.size());
                archer.setTarget(assignedThreat);
            } else if (archer.getTarget() == null || !archer.getTarget().isAlive()) {
                if (!archer.isWatchtowerSentry()) {
                    archer.getNavigation().moveTo(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 1.15);
                }
            }
        }

        // 3. Iron Golems: Heavy Shock Support
        List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, defenseZone, g -> g.isAlive());
        for (IronGolem golem : golems) {
            AllegianceRegistry.get().sanitizeDefenderTarget(golem);
            LivingEntity currentTarget = golem.getTarget();
            if (currentTarget != null && currentTarget.isAlive() && !AllegianceRegistry.get().areAllies(golem, currentTarget)) {
                continue;
            }

            if (primaryBoss != null && !AllegianceRegistry.get().areAllies(golem, primaryBoss)) {
                golem.setTarget(primaryBoss);
            } else if (!hostiles.isEmpty()) {
                golem.setTarget(hostiles.get(0));
            } else if (golem.getTarget() == null || !golem.getTarget().isAlive()) {
                golem.getNavigation().moveTo(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 1.25);
            }
        }

        // 4. Snow Golems: Harassment
        List<SnowGolem> snowGolems = level.getEntitiesOfClass(SnowGolem.class, defenseZone, s -> s.isAlive());
        for (SnowGolem snowGolem : snowGolems) {
            AllegianceRegistry.get().sanitizeDefenderTarget(snowGolem);
            if (!hostiles.isEmpty() && (snowGolem.getTarget() == null || !snowGolem.getTarget().isAlive())) {
                snowGolem.setTarget(hostiles.get(0));
            }
        }
    }

    /**
     * Periodic defense controller tick: updates threat state, scans perimeter, coordinates shelters, lifts lockdowns.
     */
    public void tickDefense(ServerLevel level, Settlement settlement) {
        if (settlement == null || level == null) return;

        SettlementDefenseState state = getState(settlement.getId());
        long now = System.currentTimeMillis();

        BlockPos center = settlement.getCenter();
        AABB villageZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2.2, 28, settlement.getRadius() * 2.2);

        // Count active hostile monsters in perimeter
        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class, villageZone,
            m -> m.isAlive() && !AllegianceRegistry.get().isVillageDefender(m));
        state.activeHostileCount = hostiles.size();

        if (state.activeHostileCount > 0) {
            Monster primaryThreat = hostiles.get(0);
            reportThreat(level, settlement, primaryThreat.blockPosition(), primaryThreat,
                primaryThreat.getType().getDescription().getString() + " Siege (" + hostiles.size() + " hostiles)");
        } else {
            // No active hostiles in village zone
            if (state.threatLevel == ThreatLevel.UNDER_ATTACK) {
                if (now - state.lastThreatTimestamp > UNDER_ATTACK_TIMEOUT_MS) {
                    state.threatLevel = ThreatLevel.RECOVERY;
                    state.lockdownActive = false;
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Defense Victory! " + settlement.getName() + " transitioned to RECOVERY.");

                    level.playSound(null, center, SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 1.2f, 1.1f);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX() + 0.5, center.getY() + 2.0, center.getZ() + 0.5, 20, 1.5, 1.0, 1.5, 0.1);
                }
            } else if (state.threatLevel == ThreatLevel.RECOVERY) {
                if (now - state.lastThreatTimestamp > (UNDER_ATTACK_TIMEOUT_MS + 10_000)) {
                    state.threatLevel = ThreatLevel.PEACEFUL;
                    state.activeThreatPos = null;
                    state.primaryThreatDescription = "None";
                }
            }
        }

        // Coordinate Civilian Shelter Machine
        CivilianShelterManager.get().tickCivilianShelters(level, settlement,
            state.threatLevel == ThreatLevel.UNDER_ATTACK, state.activeThreatPos);
    }

    public void triggerThreatAlarm(ServerLevel level, Settlement settlement, BlockPos alarmPos, String description) {
        if (settlement == null || level == null) return;
        reportThreat(level, settlement, alarmPos, null, description != null ? description : "Manual Tactical Alert");
    }

    public void clearAlarm(Settlement settlement) {
        if (settlement == null) return;
        SettlementDefenseState state = getState(settlement.getId());
        state.threatLevel = ThreatLevel.PEACEFUL;
        state.lockdownActive = false;
        state.activeThreatPos = null;
        state.primaryThreatDescription = "None";
        state.activeHostileCount = 0;
        state.getThreatRecords().clear();
    }

    public boolean isOnAlarmCooldown(Settlement settlement) {
        if (settlement == null) return false;
        SettlementDefenseState state = getState(settlement.getId());
        return (System.currentTimeMillis() - state.lastAlarmTimestamp) < ALARM_COOLDOWN_MS;
    }

    public List<VillageArcherEntity> getActiveSentries(ServerLevel level, Settlement settlement) {
        if (settlement == null || level == null) return Collections.emptyList();
        BlockPos center = settlement.getCenter();
        AABB zone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2.5, 32, settlement.getRadius() * 2.5);
        return level.getEntitiesOfClass(VillageArcherEntity.class, zone, a -> a.isAlive() && a.isWatchtowerSentry());
    }

    public void clear() {
        defenseStates.clear();
    }
}
