package livingworld.faction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.entity.VillageArcherEntity;
import livingworld.entity.VillageGuardEntity;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Civilian Distress Signalling System (Section 5, v0.2.8 Stabilization).
 * When a civilian villager is attacked by a hostile entity:
 * 1. Emits ONE distress event per incident (strictly deduplicated with cooldown; no spam).
 * 2. Visual and audible panic reaction (panic vocalization, sweat/anger particles, fleeing towards shelter).
 * 3. Alerts VillageDefenseController to register a high-priority civilian-defense threat.
 * 4. Context-sensitively dispatches 1-2 nearest available defenders to intercept the threat.
 */
public class CivilianDistressManager {
    private static final CivilianDistressManager INSTANCE = new CivilianDistressManager();

    public static CivilianDistressManager get() {
        return INSTANCE;
    }

    public static class DistressIncident {
        private final UUID incidentId;
        private final UUID victimUuid;
        private final UUID attackerUuid;
        private final UUID settlementId;
        private final BlockPos incidentPos;
        private final long timestamp;
        private boolean acknowledged;

        public DistressIncident(UUID incidentId, UUID victimUuid, UUID attackerUuid, UUID settlementId, BlockPos incidentPos, long timestamp) {
            this.incidentId = incidentId;
            this.victimUuid = victimUuid;
            this.attackerUuid = attackerUuid;
            this.settlementId = settlementId;
            this.incidentPos = incidentPos;
            this.timestamp = timestamp;
            this.acknowledged = false;
        }

        public UUID getIncidentId() { return incidentId; }
        public UUID getVictimUuid() { return victimUuid; }
        public UUID getAttackerUuid() { return attackerUuid; }
        public UUID getSettlementId() { return settlementId; }
        public BlockPos getIncidentPos() { return incidentPos; }
        public long getTimestamp() { return timestamp; }
        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    }

    // Cooldown per victim to prevent repeated distress signals from multi-hit attacks (15 seconds = 300 ticks)
    private static final long DISTRESS_COOLDOWN_TICKS = 300;
    private final Map<UUID, Long> lastDistressTick = new ConcurrentHashMap<>();
    private final Map<UUID, DistressIncident> activeIncidents = new ConcurrentHashMap<>();
    private int totalDistressEmitted = 0;

    /**
     * Reports legitimate hostile damage to a villager.
     * Emits exactly ONE distress signal per continuing attack incident.
     */
    public boolean reportDistress(ServerLevel level, Villager victim, LivingEntity attacker) {
        if (level == null || victim == null || attacker == null) return false;
        if (!victim.isAlive() || !attacker.isAlive()) return false;

        // Friendly-fire check: never report distress from fellow village allies
        if (AllegianceRegistry.get().areAllies(victim, attacker)) return false;

        long currentTick = level.getGameTime();
        UUID victimUuid = victim.getUUID();

        // 1. Deduplication Check: Prevent spam during continuing attack
        Long lastTick = lastDistressTick.get(victimUuid);
        if (lastTick != null && (currentTick - lastTick) < DISTRESS_COOLDOWN_TICKS) {
            // Ongoing incident already registered; do not flood system
            return false;
        }

        lastDistressTick.put(victimUuid, currentTick);

        // 2. Locate Victim's Settlement
        Settlement settlement = SettlementManager.get().getNearestSettlement(victim.blockPosition());
        UUID settlementId = settlement != null ? settlement.getId() : UUID.randomUUID();

        // 3. Create Distress Incident
        UUID incidentId = UUID.randomUUID();
        DistressIncident incident = new DistressIncident(
            incidentId, victimUuid, attacker.getUUID(), settlementId, victim.blockPosition(), currentTick
        );
        activeIncidents.put(incidentId, incident);
        totalDistressEmitted++;

        // 4. Physical Civilian Signaling
        performCivilianReaction(level, victim, attacker);

        // 5. Inform Village Defense Controller
        if (settlement != null) {
            VillageDefenseController.get().handleCivilianDistress(level, settlement, incident, victim, attacker);
        } else {
            // Wilderness villager: alert immediate nearby defenders within 32 blocks
            dispatchEmergencyResponders(level, victim.blockPosition(), victim, attacker, 2);
        }

        incident.setAcknowledged(true);
        livingworld.LivingWorldMod.LOGGER.info(String.format(
            "[Living World] Civilian Distress Incident #%s: Villager %s attacked by %s at %s. Dispatched defenders.",
            incidentId.toString().substring(0, 8), victimUuid.toString().substring(0, 8),
            attacker.getType().getDescription().getString(), victim.blockPosition()
        ));
        return true;
    }

    /**
     * Physical signaling: panic sound, exclamation particles, orientation toward attacker, fleeing to shelter.
     */
    private void performCivilianReaction(ServerLevel level, Villager victim, LivingEntity attacker) {
        BlockPos pos = victim.blockPosition();

        // Authentic villager distress audio (screaming / frightened vocalization)
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.4f, 1.3f);
        level.playSound(null, pos, SoundEvents.VILLAGER_HURT, SoundSource.NEUTRAL, 1.2f, 1.1f);

        // Distress particles (angry/frightened exclamation)
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, victim.getX(), victim.getY() + 1.2, victim.getZ(), 5, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.SPLASH, victim.getX(), victim.getY() + 1.5, victim.getZ(), 8, 0.2, 0.4, 0.2, 0.1);

        // Face the attacker momentarily
        victim.getLookControl().setLookAt(attacker, 45.0f, 45.0f);

        // Initiate immediate panic flee navigation away from attacker
        Vec3 awayFromAttacker = victim.position().subtract(attacker.position()).normalize().scale(16.0);
        Vec3 fleeTarget = victim.position().add(awayFromAttacker);
        victim.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 1.35);
    }

    /**
     * Dispatches 1-2 nearest available defenders to protect a victim.
     */
    public int dispatchEmergencyResponders(ServerLevel level, BlockPos origin, Villager victim, LivingEntity attacker, int maxResponders) {
        if (level == null || origin == null || attacker == null) return 0;
        AABB responseZone = AABB.ofSize(new Vec3(origin.getX(), origin.getY(), origin.getZ()), 64, 24, 64);

        int assigned = 0;

        // 1. Closest Melee Guards
        List<VillageGuardEntity> guards = level.getEntitiesOfClass(VillageGuardEntity.class, responseZone,
            g -> g.isAlive() && (g.getTarget() == null || !g.getTarget().isAlive() || g.distanceToSqr(victim) < 144.0));
        guards.sort((a, b) -> Double.compare(a.distanceToSqr(victim), b.distanceToSqr(victim)));

        for (VillageGuardEntity guard : guards) {
            if (assigned >= maxResponders) break;
            guard.setTarget(attacker);
            guard.getNavigation().moveTo(attacker, 1.35);
            assigned++;
        }

        // 2. Closest Archers (if melee guards didn't fill quota)
        if (assigned < maxResponders) {
            List<VillageArcherEntity> archers = level.getEntitiesOfClass(VillageArcherEntity.class, responseZone,
                a -> a.isAlive() && !a.isWatchtowerSentry() && (a.getTarget() == null || !a.getTarget().isAlive()));
            archers.sort((a, b) -> Double.compare(a.distanceToSqr(victim), b.distanceToSqr(victim)));

            for (VillageArcherEntity archer : archers) {
                if (assigned >= maxResponders) break;
                archer.setTarget(attacker);
                assigned++;
            }
        }

        // 3. Closest Iron Golem
        if (assigned == 0) {
            List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, responseZone, g -> g.isAlive());
            if (!golems.isEmpty()) {
                golems.sort((a, b) -> Double.compare(a.distanceToSqr(victim), b.distanceToSqr(victim)));
                IronGolem golem = golems.get(0);
                golem.setTarget(attacker);
                assigned++;
            }
        }

        return assigned;
    }

    public int getTotalDistressEmitted() {
        return totalDistressEmitted;
    }

    public Map<UUID, DistressIncident> getActiveIncidents() {
        return activeIncidents;
    }

    public void clear() {
        lastDistressTick.clear();
        activeIncidents.clear();
        totalDistressEmitted = 0;
    }
}
