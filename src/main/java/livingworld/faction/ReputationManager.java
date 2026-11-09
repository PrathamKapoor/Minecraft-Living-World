package livingworld.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.golem.IronGolem;

public class ReputationManager {
    private static final ReputationManager INSTANCE = new ReputationManager();

    // Map: Player UUID -> Map: Settlement ID -> Hit count in current window
    private final Map<UUID, Map<UUID, Integer>> accidentalHits = new HashMap<>();
    // Map: Player UUID -> Last support timestamp (ticks)
    private final Map<UUID, Long> lastSupportTick = new HashMap<>();
    // Map: Player UUID -> Last warning timestamp (ticks)
    private final Map<UUID, Long> lastWarningTick = new HashMap<>();
    // Map: Player UUID -> Last reputation loss timestamp (ticks)
    private final Map<UUID, Long> lastReputationLossTick = new HashMap<>();

    public static ReputationManager get() {
        return INSTANCE;
    }

    public boolean handleGolemHit(ServerPlayer player, IronGolem golem, Settlement settlement) {
        UUID playerUuid = player.getUUID();
        long now = player.level().getGameTime();

        // 1. If golem is already actively hostile to this player, permit combat without damage cancellation or chat spam
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
            // Accidental hit forgiven: keep golem neutral
            golem.setTarget(null);
            long lastWarn = lastWarningTick.getOrDefault(playerUuid, 0L);
            if (now - lastWarn > 100) { // 5-second cooldown prevents chat spam
                player.sendSystemMessage(Component.literal("§e[Living World] Iron Golem forgave accidental strike (Trust: " + rep + "). Be careful!"));
                lastWarningTick.put(playerUuid, now);
            }
            return true;
        } else {
            // Intentional attack: provoke golem and apply throttled reputation deduction
            golem.setTarget(player);

            long lastRepLoss = lastReputationLossTick.getOrDefault(playerUuid, 0L);
            if (now - lastRepLoss > 100) { // 5-second cooldown prevents cascading reputation drop
                if (villagerFaction != null) {
                    villagerFaction.adjustReputation(playerUuid, -15);
                }
                player.sendSystemMessage(Component.literal("§c[Living World] Hostile action against village defenders! Iron Golem provoked."));
                lastReputationLossTick.put(playerUuid, now);
            }
            // CRITICAL FIX 5: Golem is NEVER invulnerable! Always return true so player strikes land and golem is killable
            return true;
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
            // Cleric provides aid
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0));
            player.sendSystemMessage(Component.literal("§6[Living World] A village cleric provided you with healing aid!"));
            lastSupportTick.put(playerUuid, currentTick);
        }
    }

    public void resetHits(UUID playerUuid) {
        accidentalHits.remove(playerUuid);
    }
}
