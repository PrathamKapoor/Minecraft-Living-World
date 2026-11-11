package livingworld.faction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

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
    private final Map<UUID, Long> lastKillRewardTick = new HashMap<>();
    private final Map<UUID, Set<Integer>> crossedMilestones = new HashMap<>();

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
            int oldRep = villagerFaction.getReputation(playerUuid);
            villagerFaction.adjustReputation(playerUuid, delta);
            int newRep = villagerFaction.getReputation(playerUuid);
            checkMilestoneCrossing(playerUuid, settlementId, oldRep, newRep);
        }
    }

    public int getReputation(UUID playerUuid, UUID settlementId) {
        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        return (villagerFaction != null) ? villagerFaction.getReputation(playerUuid) : 0;
    }

    public String getMilestoneTitle(int rep) {
        if (rep >= 200) return "Honored Defender";
        if (rep >= 100) return "Village Ally";
        if (rep >= 50) return "Trusted Friend";
        if (rep >= 25) return "Recognized Visitor";
        if (rep >= 0) return "Neutral Guest";
        return "Untrusted Outcast";
    }

    private void checkMilestoneCrossing(UUID playerUuid, UUID settlementId, int oldRep, int newRep) {
        if (newRep <= oldRep) return;
        int[] milestones = {25, 50, 100, 200};
        Set<Integer> achieved = crossedMilestones.computeIfAbsent(playerUuid, k -> new HashSet<>());

        for (int m : milestones) {
            if (oldRep < m && newRep >= m && !achieved.contains(m)) {
                achieved.add(m);
                // Milestone achieved: emit one quiet, dignified notification
                Settlement settlement = settlementId != null ? SettlementManager.get().getSettlement(settlementId.toString()) : null;
                String villageName = settlement != null ? settlement.getName() : "the Settlement";
                String title = getMilestoneTitle(newRep);

                // Look up online player
                net.minecraft.server.MinecraftServer server = livingworld.LivingWorldMod.getCurrentServer();
                if (server != null) {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                    if (player != null) {
                        player.sendSystemMessage(Component.literal(String.format(
                            "§a[Village Trust] §6You have attained a new standing in %s: §l§e%s §r§6(Reputation: %d)",
                            villageName, title, newRep
                        )));
                        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.25f);
                    }
                }
            }
        }
    }

    /**
     * Rewards player reputation for defending a settlement against hostile threats (Section 9).
     * Recorded silently; milestone notifications fire only on crossing major thresholds.
     */
    public void onMonsterDefeated(ServerPlayer player, LivingEntity threat, ServerLevel level) {
        if (player == null || threat == null || level == null) return;
        BlockPos pos = threat.blockPosition();
        Settlement settlement = SettlementManager.get().getNearestSettlement(pos);
        if (settlement == null) return;

        // Proximity validation: must be within settlement defense boundary (radius + 24m)
        double distSq = pos.distToCenterSqr(settlement.getCenter().getX(), settlement.getCenter().getY(), settlement.getCenter().getZ());
        double maxDist = settlement.getRadius() + 24.0;
        if (distSq > (maxDist * maxDist)) return;

        long now = level.getGameTime();
        UUID playerUuid = player.getUUID();

        // Anti-farming cooldown (at least 15 ticks between rewarded kills)
        Long lastReward = lastKillRewardTick.get(playerUuid);
        if (lastReward != null && (now - lastReward) < 15) return;
        lastKillRewardTick.put(playerUuid, now);

        int repBonus = 1; // Base hostile kill: +1

        // 1. Mutant Commander or Boss threat: +10
        if (threat instanceof livingworld.entity.MutantZombieCommanderEntity) {
            repBonus = 10;
        }
        // 2. Mob actively threatening/attacking a villager: +5
        else if (threat instanceof Mob mob && mob.getTarget() instanceof net.minecraft.world.entity.npc.villager.Villager) {
            repBonus = 5;
        }
        // 3. Active raider (Pillager/Vindicator/Ravager): +3
        else if (threat instanceof net.minecraft.world.entity.monster.illager.AbstractIllager || threat instanceof net.minecraft.world.entity.monster.Ravager) {
            repBonus = 3;
        }
        // 4. Armored or specialized tool miner: +2
        else if (threat.getMainHandItem().is(Items.IRON_PICKAXE) || threat.getMainHandItem().is(Items.DIAMOND_PICKAXE) ||
                 threat.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)) {
            repBonus = 2;
        }

        addReputation(playerUuid, settlement.getId(), repBonus);
        livingworld.LivingWorldMod.LOGGER.debug(String.format(
            "[Living World] Defense Reputation: Player %s earned +%d trust in %s (silent).",
            player.getName().getString(), repBonus, settlement.getName()
        ));
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
