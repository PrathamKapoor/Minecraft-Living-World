package livingworld.faction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.entity.VillageArcherEntity;
import livingworld.entity.VillageGuardEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

/**
 * Centralized Allegiance & Friendly-Fire Protection Registry (Section 2, Living Village Update).
 * Eliminates internecine combat between village soldiers, archers, iron golems, snow golems,
 * and civilian villagers. Strictly filters retaliation logic and projectile damage.
 */
public class AllegianceRegistry {
    private static final AllegianceRegistry INSTANCE = new AllegianceRegistry();

    public static AllegianceRegistry get() {
        return INSTANCE;
    }

    public enum MilitaryRole {
        MELEE_GUARD,
        ARCHER,
        WATCHTOWER_SENTRY,
        GUARD_CAPTAIN,
        IRON_GOLEM_DEFENDER,
        SNOW_GOLEM_DEFENDER,
        CIVILIAN_SUPPORT
    }

    public enum CombatOrders {
        PATROL,
        RALLY_DEFENSE,
        SENTRY_LOOKOUT,
        LOCKDOWN_SHELTER,
        RETREAT_HEAL
    }

    public static class DefenderProfile {
        private final UUID entityUuid;
        private String settlementId;
        private String factionId;
        private MilitaryRole role;
        private CombatOrders orders;

        public DefenderProfile(UUID entityUuid, String settlementId, String factionId, MilitaryRole role) {
            this.entityUuid = entityUuid;
            this.settlementId = settlementId;
            this.factionId = factionId;
            this.role = role;
            this.orders = CombatOrders.PATROL;
        }

        public UUID getEntityUuid() {
            return entityUuid;
        }

        public String getSettlementId() {
            return settlementId;
        }

        public void setSettlementId(String settlementId) {
            this.settlementId = settlementId;
        }

        public String getFactionId() {
            return factionId;
        }

        public MilitaryRole getRole() {
            return role;
        }

        public void setRole(MilitaryRole role) {
            this.role = role;
        }

        public CombatOrders getOrders() {
            return orders;
        }

        public void setOrders(CombatOrders orders) {
            this.orders = orders;
        }
    }

    private final Map<UUID, DefenderProfile> registeredDefenders = new ConcurrentHashMap<>();

    public void register(UUID uuid, String settlementId, String factionId, MilitaryRole role) {
        registeredDefenders.put(uuid, new DefenderProfile(uuid, settlementId, factionId, role));
    }

    public void unregister(UUID uuid) {
        registeredDefenders.remove(uuid);
    }

    public DefenderProfile getProfile(UUID uuid) {
        return registeredDefenders.get(uuid);
    }

    public Map<UUID, DefenderProfile> getAllDefenders() {
        return java.util.Collections.unmodifiableMap(registeredDefenders);
    }

    /**
     * Determines whether an entity is a registered or intrinsic village defender.
     */
    public boolean isVillageDefender(LivingEntity entity) {
        if (entity == null) return false;
        if (registeredDefenders.containsKey(entity.getUUID())) return true;
        return entity instanceof VillageGuardEntity
            || entity instanceof VillageArcherEntity
            || entity instanceof IronGolem
            || entity instanceof SnowGolem
            || entity instanceof Villager;
    }

    /**
     * Evaluates whether two entities share friendly allegiance and must NEVER fight.
     */
    public boolean areAllies(LivingEntity a, LivingEntity b) {
        if (a == null || b == null || a == b) return true;

        boolean aIsDefender = isVillageDefender(a);
        boolean bIsDefender = isVillageDefender(b);

        // Both are village community members (guards, archers, golems, civilians)
        if (aIsDefender && bIsDefender) {
            DefenderProfile profA = registeredDefenders.get(a.getUUID());
            DefenderProfile profB = registeredDefenders.get(b.getUUID());
            // If both belong to different explicit enemy settlements/factions, evaluate faction relations
            if (profA != null && profB != null) {
                if (!profA.getSettlementId().isEmpty() && !profB.getSettlementId().isEmpty()
                    && !profA.getSettlementId().equals(profB.getSettlementId())) {
                    // Check if warring factions
                    return !FactionManager.get().isHostile(profA.getFactionId(), profB.getFactionId());
                }
            }
            return true; // Default village defenders are strictly allied
        }

        // Friendly player interaction
        if (a instanceof Player player && bIsDefender) {
            return !ReputationManager.get().isHostileToPlayer(player);
        }
        if (b instanceof Player player && aIsDefender) {
            return !ReputationManager.get().isHostileToPlayer(player);
        }

        return false;
    }

    /**
     * Evaluates if damage between entities should be cancelled to prevent friendly-fire.
     */
    public boolean shouldCancelFriendlyFire(LivingEntity victim, DamageSource source) {
        if (victim == null || source == null) return false;
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return false;
        }

        if (areAllies(victim, livingAttacker)) {
            // Stray arrow, projectile, or accidental sweep hit from ally
            if (source.isDirect() == false || livingAttacker instanceof VillageArcherEntity || livingAttacker instanceof SnowGolem) {
                return true; // Cancel projectile friendly fire
            }
            // Also cancel direct melee hit between allied guards/golems
            if (isVillageDefender(victim) && isVillageDefender(livingAttacker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears combat targets if an allied entity was mistakenly acquired.
     */
    public void sanitizeDefenderTarget(Mob defender) {
        if (defender == null) return;
        LivingEntity target = defender.getTarget();
        if (target != null && areAllies(defender, target)) {
            defender.setTarget(null);
            defender.setLastHurtByMob(null);
        }
    }

    public void clear() {
        registeredDefenders.clear();
    }
}
