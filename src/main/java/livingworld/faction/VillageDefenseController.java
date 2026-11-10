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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Unified Settlement Threat-Response & Defense Controller (Section 1 & 5, Living Village Update).
 * Orchestrates central settlement threat state machine:
 * PEACEFUL -> SUSPICIOUS -> ALERT -> UNDER_ATTACK -> RECOVERY -> PEACEFUL.
 * Manages early-warning watchtower sentries, audible goat-horn alarms, defender rallies,
 * wake-up protocols for sleeping soldiers, and civilian lockdown.
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

    public static class SettlementDefenseState {
        private final UUID settlementId;
        private ThreatLevel threatLevel = ThreatLevel.PEACEFUL;
        private long lastThreatTimestamp = 0;
        private long lastAlarmTimestamp = 0;
        private BlockPos activeThreatPos = null;
        private String primaryThreatDescription = "None";
        private int activeHostileCount = 0;
        private boolean lockdownActive = false;

        public SettlementDefenseState(UUID settlementId) {
            this.settlementId = settlementId;
        }

        public UUID getSettlementId() {
            return settlementId;
        }

        public ThreatLevel getThreatLevel() {
            return threatLevel;
        }

        public void setThreatLevel(ThreatLevel threatLevel) {
            this.threatLevel = threatLevel;
        }

        public BlockPos getActiveThreatPos() {
            return activeThreatPos;
        }

        public boolean isLockdownActive() {
            return lockdownActive;
        }

        public String getPrimaryThreatDescription() {
            return primaryThreatDescription;
        }

        public int getActiveHostileCount() {
            return activeHostileCount;
        }
    }

    private final Map<UUID, SettlementDefenseState> defenseStates = new ConcurrentHashMap<>();
    private static final long ALARM_COOLDOWN_MS = 25_000; // Horn alarm sound cooldown: 25 seconds
    private static final long UNDER_ATTACK_TIMEOUT_MS = 15_000; // 15 seconds without hostiles -> Recovery

    public SettlementDefenseState getState(UUID settlementId) {
        return defenseStates.computeIfAbsent(settlementId, SettlementDefenseState::new);
    }

    /**
     * Reports a confirmed threat to the settlement.
     * Triggers goat horn alarm, awakens sleeping guards, orders defender rally and civilian lockdown.
     */
    public void reportThreat(ServerLevel level, Settlement settlement, BlockPos threatPos, LivingEntity threatSource, String description) {
        if (settlement == null || level == null) return;

        SettlementDefenseState state = getState(settlement.getId());
        long now = System.currentTimeMillis();
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

        // Deploy Defense Directives across village
        rallyDefenders(level, settlement, state.activeThreatPos, threatSource);
        executeCivilianLockdown(level, settlement, state.activeThreatPos);
    }

    /**
     * Plays authentic goat horn alarm sound audible across the settlement.
     */
    public void soundGoatHornAlarm(ServerLevel level, Settlement settlement, BlockPos alarmPos) {
        BlockPos center = alarmPos != null ? alarmPos : settlement.getCenter();

        // Authentic goat horn sound (ITEM_GOAT_HORN_SOUND_0)
        level.playSound(null, center, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.NEUTRAL, 3.5f, 0.9f);
        level.playSound(null, center, SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 2.0f, 1.0f);

        // Visual alarm particles over settlement center
        level.sendParticles(ParticleTypes.NOTE, center.getX() + 0.5, center.getY() + 3.0, center.getZ() + 0.5, 12, 1.0, 1.0, 1.0, 0.2);
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Horn Sounded in " + settlement.getName() + "! Threat: " + getState(settlement.getId()).primaryThreatDescription);
    }

    /**
     * Awakens sleeping guards, clears improper targets, and coordinates tactical defender rally.
     */
    private void rallyDefenders(ServerLevel level, Settlement settlement, BlockPos threatPos, LivingEntity threatEntity) {
        BlockPos center = settlement.getCenter();
        AABB defenseZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2.5, 32, settlement.getRadius() * 2.5);

        // 1. Melee Guards
        List<VillageGuardEntity> guards = level.getEntitiesOfClass(VillageGuardEntity.class, defenseZone, g -> g.isAlive());
        for (VillageGuardEntity guard : guards) {
            // Wake up if sleeping/resting
            if (guard.isSleeping()) {
                guard.stopSleeping();
            }
            AllegianceRegistry.get().sanitizeDefenderTarget(guard);

            // Assign threat target if available, or rally toward threat pos
            if (threatEntity != null && !AllegianceRegistry.get().areAllies(guard, threatEntity)) {
                guard.setTarget(threatEntity);
            } else if (guard.getTarget() == null || !guard.getTarget().isAlive()) {
                guard.getNavigation().moveTo(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 1.35);
            }
        }

        // 2. Archers & Sentries
        List<VillageArcherEntity> archers = level.getEntitiesOfClass(VillageArcherEntity.class, defenseZone, a -> a.isAlive());
        for (VillageArcherEntity archer : archers) {
            if (archer.isSleeping()) {
                archer.stopSleeping();
            }
            AllegianceRegistry.get().sanitizeDefenderTarget(archer);

            if (threatEntity != null && !AllegianceRegistry.get().areAllies(archer, threatEntity)) {
                archer.setTarget(threatEntity);
            } else if (archer.getTarget() == null || !archer.getTarget().isAlive()) {
                // Seek elevated position or move toward threat perimeter
                archer.getNavigation().moveTo(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 1.15);
            }
        }

        // 3. Iron Golems
        List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, defenseZone, g -> g.isAlive());
        for (IronGolem golem : golems) {
            AllegianceRegistry.get().sanitizeDefenderTarget(golem);
            if (threatEntity != null && !AllegianceRegistry.get().areAllies(golem, threatEntity)) {
                golem.setTarget(threatEntity);
            } else if (golem.getTarget() == null || !golem.getTarget().isAlive()) {
                golem.getNavigation().moveTo(threatPos.getX(), threatPos.getY(), threatPos.getZ(), 1.25);
            }
        }

        // 4. Snow Golems (Cold Biomes)
        List<SnowGolem> snowGolems = level.getEntitiesOfClass(SnowGolem.class, defenseZone, s -> s.isAlive());
        for (SnowGolem snowGolem : snowGolems) {
            AllegianceRegistry.get().sanitizeDefenderTarget(snowGolem);
            if (threatEntity != null && !AllegianceRegistry.get().areAllies(snowGolem, threatEntity)) {
                snowGolem.setTarget(threatEntity);
            }
        }
    }

    /**
     * Orders civilian villagers to seek shelter in nearest roofed residential structure and close doors.
     */
    private void executeCivilianLockdown(ServerLevel level, Settlement settlement, BlockPos threatPos) {
        BlockPos center = settlement.getCenter();
        AABB villageZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 24, settlement.getRadius() * 2);

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, villageZone, v -> v.isAlive());
        for (Villager villager : villagers) {
            // Cancel wandering and flee away from threat into village interior
            if (threatPos != null) {
                double dx = villager.getX() - threatPos.getX();
                double dz = villager.getZ() - threatPos.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 24.0) {
                    // Move towards village center
                    villager.getNavigation().moveTo(center.getX(), center.getY(), center.getZ(), 1.3);
                }
            }
        }
    }

    /**
     * Periodic defense controller tick: updates threat state, scans perimeter, and lifts lockdowns.
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
                    // Transition to Recovery
                    state.threatLevel = ThreatLevel.RECOVERY;
                    state.lockdownActive = false;
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Defense Victory! " + settlement.getName() + " transitioned to RECOVERY.");

                    // Sound victorious village chime
                    level.playSound(null, center, SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 1.2f, 1.1f);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX() + 0.5, center.getY() + 2.0, center.getZ() + 0.5, 20, 1.5, 1.0, 1.5, 0.1);
                }
            } else if (state.threatLevel == ThreatLevel.RECOVERY) {
                if (now - state.lastThreatTimestamp > (UNDER_ATTACK_TIMEOUT_MS + 10_000)) {
                    // Return to Peaceful
                    state.threatLevel = ThreatLevel.PEACEFUL;
                    state.activeThreatPos = null;
                    state.primaryThreatDescription = "None";
                }
            }
        }
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
