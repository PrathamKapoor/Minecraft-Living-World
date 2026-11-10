package livingworld.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;

/**
 * Guest-Friendly Village Hospitality & Reputation Management Engine (Section 9, Living Village Update).
 * Manages visitor greetings, trade benefits for trusted allies, sensible accidental hit forgiveness,
 * and cleric emergency support for players defending settlements.
 */
public class ReputationManager {
    private static final ReputationManager INSTANCE = new ReputationManager();

    // Map: Player UUID -> Map: Settlement ID -> Hit count in current window
    private final Map<UUID, Map<UUID, Integer>> accidentalHits = new HashMap<>();
    private final Map<UUID, Long> lastSupportTick = new HashMap<>();
    private final Map<UUID, Long> lastWarningTick = new HashMap<>();
    private final Map<UUID, Long> lastReputationLossTick = new HashMap<>();
    private final Map<UUID, Long> lastWelcomeTick = new HashMap<>();

    public static ReputationManager get() {
        return INSTANCE;
    }

    public boolean isHostileToPlayer(Player player) {
        if (player == null) return false;
        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        if (villagerFaction == null) return false;
        return villagerFaction.getReputation(player.getUUID()) < -25;
    }

    public void addReputation(UUID playerUuid, UUID settlementId, int delta) {
        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        if (villagerFaction != null) {
            villagerFaction.adjustReputation(playerUuid, delta);
        }
    }

    public int getReputation(UUID playerUuid, UUID settlementId) {
        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        return (villagerFaction != null) ? villagerFaction.getReputation(playerUuid) : 0;
    }

    public void clear() {
        accidentalHits.clear();
        lastSupportTick.clear();
        lastWarningTick.clear();
        lastReputationLossTick.clear();
        lastWelcomeTick.clear();
    }

    public boolean handleGuardHit(ServerPlayer player, Mob guard, Settlement settlement) {
        UUID playerUuid = player.getUUID();
        long now = player.level().getGameTime();

        if (guard.getTarget() == player) {
            return true;
        }

        UUID settlementId = settlement != null ? settlement.getId() : UUID.randomUUID();
        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        int rep = (villagerFaction != null) ? villagerFaction.getReputation(playerUuid) : 0;

        accidentalHits.computeIfAbsent(playerUuid, k -> new HashMap<>());
        int hits = accidentalHits.get(playerUuid).getOrDefault(settlementId, 0) + 1;
        accidentalHits.get(playerUuid).put(settlementId, hits);

        if (rep >= 0 && hits <= 2) {
            guard.setTarget(null);
            guard.setLastHurtByMob(null);
            long lastWarn = lastWarningTick.getOrDefault(playerUuid, 0L);
            if (now - lastWarn > 100) {
                player.sendSystemMessage(Component.literal("§e[Living World] Village Guard forgave accidental strike (Trust: " + rep + "). Watch your fire!"));
                lastWarningTick.put(playerUuid, now);
            }
            return true;
        } else {
            guard.setTarget(player);
            long lastRepLoss = lastReputationLossTick.getOrDefault(playerUuid, 0L);
            if (now - lastRepLoss > 100) {
                if (villagerFaction != null) {
                    villagerFaction.adjustReputation(playerUuid, -10);
                }
                player.sendSystemMessage(Component.literal("§c[Living World] Hostile aggression against village guard! Defender provoked."));
                lastReputationLossTick.put(playerUuid, now);
            }
            return true;
        }
    }

    public boolean handleGolemHit(ServerPlayer player, IronGolem golem, Settlement settlement) {
        UUID playerUuid = player.getUUID();
        long now = player.level().getGameTime();

        if (golem.getTarget() == player) {
            return true;
        }

        UUID settlementId = settlement != null ? settlement.getId() : UUID.randomUUID();
        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        int rep = (villagerFaction != null) ? villagerFaction.getReputation(playerUuid) : 0;

        accidentalHits.computeIfAbsent(playerUuid, k -> new HashMap<>());
        int hits = accidentalHits.get(playerUuid).getOrDefault(settlementId, 0) + 1;
        accidentalHits.get(playerUuid).put(settlementId, hits);

        if (rep >= 0 && hits <= 2) {
            golem.setTarget(null);
            golem.setLastHurtByMob(null);
            long lastWarn = lastWarningTick.getOrDefault(playerUuid, 0L);
            if (now - lastWarn > 100) {
                player.sendSystemMessage(Component.literal("§e[Living World] Iron Golem forgave accidental strike (Trust: " + rep + "). Be careful!"));
                lastWarningTick.put(playerUuid, now);
            }
            return true;
        } else {
            golem.setTarget(player);
            long lastRepLoss = lastReputationLossTick.getOrDefault(playerUuid, 0L);
            if (now - lastRepLoss > 100) {
                if (villagerFaction != null) {
                    villagerFaction.adjustReputation(playerUuid, -15);
                }
                player.sendSystemMessage(Component.literal("§c[Living World] Hostile action against village defenders! Iron Golem provoked."));
                lastReputationLossTick.put(playerUuid, now);
            }
            return true;
        }
    }

    public void welcomeVisitor(ServerPlayer player, Settlement settlement) {
        if (player == null || settlement == null) return;
        UUID playerUuid = player.getUUID();
        long now = player.level().getGameTime();

        Long lastWelcome = lastWelcomeTick.get(playerUuid);
        if (lastWelcome != null && (now - lastWelcome) < 2400) { // Welcome at most once per 2 minutes
            return;
        }
        lastWelcomeTick.put(playerUuid, now);

        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        int rep = (villagerFaction != null) ? villagerFaction.getReputation(playerUuid) : 0;

        if (rep >= 0) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9f, 1.1f);
            player.sendSystemMessage(Component.literal("§a[Living World] Welcome to §6" + settlement.getName() + "§a, traveler! (Trust: " + rep + ")"));
        }
    }

    public void checkRaidSupport(ServerPlayer player, long currentTick) {
        UUID playerUuid = player.getUUID();
        long last = lastSupportTick.getOrDefault(playerUuid, 0L);
        if (currentTick - last < 400) { // 20-second cooldown
            return;
        }

        BlockPos playerPos = player.blockPosition();
        Settlement settlement = SettlementManager.get().getSettlementAt(playerPos);
        if (settlement == null) return;

        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        int rep = (villagerFaction != null) ? villagerFaction.getReputation(playerUuid) : 0;

        if (rep >= 25 && player.getHealth() < player.getMaxHealth() * 0.75f) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0));
            player.sendSystemMessage(Component.literal("§6[Living World] A village cleric provided you with healing aid!"));
            lastSupportTick.put(playerUuid, currentTick);
        }
    }

    public void resetHits(UUID playerUuid) {
        accidentalHits.remove(playerUuid);
    }
}
