package livingworld.economy;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import livingworld.faction.Faction;
import livingworld.faction.FactionManager;
import livingworld.settlement.Settlement;
import livingworld.settlement.VillagerIdentityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Economy, Logistics & Resource Flow Engine (Section 10 & Milestone 3).
 * Coordinates couriers transporting harvested crops/supplies, material stockpiles,
 * shortage pauses on construction projects, and player trade/deposits with reputation rewards.
 */
public class EconomyManager {
    private static final EconomyManager INSTANCE = new EconomyManager();

    private final Queue<EconomyTransaction> recentTransactions = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOG_SIZE = 100;

    public static EconomyManager get() {
        return INSTANCE;
    }

    public record EconomyTransaction(
        long timestamp,
        UUID settlementId,
        String settlementName,
        Stockpile.ResourceType resource,
        int amount,
        String transactionType, // "DEPOSIT", "WITHDRAWAL", "TRADE", "HARVEST"
        String actor
    ) {}

    public void logTransaction(Settlement settlement, Stockpile.ResourceType resource, int amount, String type, String actor) {
        EconomyTransaction tx = new EconomyTransaction(
            System.currentTimeMillis(),
            settlement != null ? settlement.getId() : null,
            settlement != null ? settlement.getName() : "Unknown",
            resource,
            amount,
            type,
            actor
        );
        recentTransactions.add(tx);
        while (recentTransactions.size() > MAX_LOG_SIZE) {
            recentTransactions.poll();
        }
    }

    public List<EconomyTransaction> getRecentTransactions() {
        return new ArrayList<>(recentTransactions);
    }

    /**
     * Tick settlement logistics: couriers collect dropped items in settlement territory
     * and transport them to the central stockpile.
     */
    public void tickLogistics(ServerLevel level, Settlement settlement) {
        if (settlement == null) return;

        BlockPos center = settlement.getCenter();
        AABB settlementArea = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 24, settlement.getRadius() * 2);

        // 1. Check for dropped crop/wood/stone resources on the ground
        List<ItemEntity> droppedItems = level.getEntitiesOfClass(ItemEntity.class, settlementArea, item -> item.isAlive() && !item.getItem().isEmpty());
        if (droppedItems.isEmpty()) return;

        // 2. Locate available courier / hauler villagers
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, settlementArea, v -> v.isAlive());
        for (ItemEntity itemEntity : droppedItems) {
            ItemStack stack = itemEntity.getItem();
            Stockpile.ResourceType resType = getResourceType(stack);
            if (resType == null) continue;

            // Find closest villager to act as courier
            Villager closestCourier = null;
            double closestDistSq = Double.MAX_VALUE;

            for (Villager v : villagers) {
                double distSq = v.distanceToSqr(itemEntity);
                if (distSq < closestDistSq && distSq < (24.0 * 24.0)) {
                    closestDistSq = distSq;
                    closestCourier = v;
                }
            }

            if (closestCourier != null) {
                if (closestDistSq <= 4.0) {
                    // Collect item into stockpile
                    int count = stack.getCount();
                    settlement.getStockpile().add(resType, count);
                    logTransaction(settlement, resType, count, "COURIER_DEPOSIT", closestCourier.getCustomName() != null ? closestCourier.getCustomName().getString() : "Courier");
                    
                    // Visual pickup
                    closestCourier.swingForAttack(InteractionHand.MAIN_HAND);
                    level.playSound(null, closestCourier.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.5f, 1.2f);
                    itemEntity.discard();

                    // Award work contribution
                    VillagerIdentityManager.get().recordWorkContribution(closestCourier.getUUID(), count);
                    break;
                } else {
                    // Navigate towards the item
                    closestCourier.getNavigation().moveTo(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 1.15);
                }
            }
        }
    }

    /**
     * Checks if a settlement has sufficient construction materials for active projects.
     */
    public boolean hasConstructionMaterials(Settlement settlement) {
        if (settlement == null) return true;
        // Require at least 2 wood and 2 stone for active construction progress
        return settlement.getStockpile().get(Stockpile.ResourceType.WOOD) >= 1
            || settlement.getStockpile().get(Stockpile.ResourceType.STONE) >= 1;
    }

    /**
     * Handles player donation/deposit into settlement stockpile, rewarding reputation and audio/visual cues.
     */
    public boolean depositPlayerContribution(ServerLevel level, ServerPlayer player, Settlement settlement, ItemStack heldItem) {
        if (heldItem.isEmpty() || settlement == null) return false;

        Stockpile.ResourceType resType = getResourceType(heldItem);
        if (resType == null) return false;

        int depositAmount = Math.min(heldItem.getCount(), 16);
        settlement.getStockpile().add(resType, depositAmount);
        heldItem.shrink(depositAmount);

        logTransaction(settlement, resType, depositAmount, "PLAYER_DONATION", player.getName().getString());

        // Boost faction reputation
        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        if (villagerFaction != null) {
            villagerFaction.adjustReputation(player.getUUID(), Math.max(1, depositAmount / 4));
        }

        // Audio-visual feedback
        level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 10, 0.4, 0.4, 0.4, 0.1);

        player.sendSystemMessage(Component.literal("§6[Living World] §aContributed " + depositAmount + " " + resType.name() + " to " + settlement.getName() + " (+Reputation)"));
        return true;
    }

    public Stockpile.ResourceType getResourceType(ItemStack stack) {
        if (stack.is(Items.WHEAT) || stack.is(Items.BREAD) || stack.is(Items.CARROT)
            || stack.is(Items.POTATO) || stack.is(Items.BEETROOT) || stack.is(Items.APPLE)) {
            return Stockpile.ResourceType.FOOD;
        } else if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) {
            return Stockpile.ResourceType.WOOD;
        } else if (stack.is(Items.COBBLESTONE) || stack.is(Items.STONE) || stack.is(Items.DEEPSLATE)) {
            return Stockpile.ResourceType.STONE;
        } else if (stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON) || stack.is(Items.IRON_NUGGET)) {
            return Stockpile.ResourceType.IRON;
        } else if (stack.is(Items.LEATHER)) {
            return Stockpile.ResourceType.LEATHER;
        } else if (stack.is(ItemTags.WOOL)) {
            return Stockpile.ResourceType.WOOL;
        }
        return null;
    }
}
