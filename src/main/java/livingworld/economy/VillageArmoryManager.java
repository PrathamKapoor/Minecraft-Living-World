package livingworld.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.faction.ReputationManager;
import livingworld.settlement.Settlement;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Functional Village Armory Management Engine (Section 6, Living Village Update).
 * Tracks physical military supply stockpiles per settlement:
 * Swords, Bows, Arrows, Shields, and Armor (Iron, Diamond, Netherite).
 * Receives weapons/armor produced by Weaponsmiths & Armorers,
 * supplies equipment requests for recruits, and manages combat gear repair/replacement.
 */
public class VillageArmoryManager {
    private static final VillageArmoryManager INSTANCE = new VillageArmoryManager();

    public static VillageArmoryManager get() {
        return INSTANCE;
    }

    public static class ArmoryStock {
        private final UUID settlementId;
        private int ironSwords = 6;
        private int diamondSwords = 0;
        private int bows = 4;
        private int arrows = 96;
        private int shields = 6;
        private int ironHelmets = 4;
        private int ironChestplates = 4;
        private int ironLeggings = 4;
        private int ironBoots = 4;
        private int diamondArmorPieces = 0;
        private int healingPotions = 4;

        public ArmoryStock(UUID settlementId) {
            this.settlementId = settlementId;
        }

        public UUID getSettlementId() {
            return settlementId;
        }

        public int getIronSwords() {
            return ironSwords;
        }

        public int getDiamondSwords() {
            return diamondSwords;
        }

        public int getBows() {
            return bows;
        }

        public int getArrows() {
            return arrows;
        }

        public int getShields() {
            return shields;
        }

        public int getIronChestplates() {
            return ironChestplates;
        }

        public int getHealingPotions() {
            return healingPotions;
        }

        public boolean hasMeleeGuardGear() {
            return ironSwords > 0 && shields > 0 && ironChestplates > 0;
        }

        public boolean hasArcherGear() {
            return bows > 0 && arrows >= 16 && ironChestplates > 0;
        }

        public boolean consumeMeleeGuardGear() {
            if (!hasMeleeGuardGear()) return false;
            ironSwords--;
            shields--;
            ironChestplates--;
            if (ironHelmets > 0) ironHelmets--;
            if (ironLeggings > 0) ironLeggings--;
            if (ironBoots > 0) ironBoots--;
            return true;
        }

        public boolean consumeArcherGear() {
            if (!hasArcherGear()) return false;
            bows--;
            arrows = Math.max(0, arrows - 16);
            ironChestplates--;
            if (ironHelmets > 0) ironHelmets--;
            if (ironLeggings > 0) ironLeggings--;
            if (ironBoots > 0) ironBoots--;
            return true;
        }

        public boolean consumeHealingPotion() {
            if (healingPotions > 0) {
                healingPotions--;
                return true;
            }
            return false;
        }

        public String getSummary() {
            return String.format("Swords: %d iron, %d dia | Bows: %d | Arrows: %d | Shields: %d | Helmets: %d | Chests: %d | Legs: %d | Boots: %d | Potions: %d",
                ironSwords, diamondSwords, bows, arrows, shields, ironHelmets, ironChestplates, ironLeggings, ironBoots, healingPotions);
        }
    }

    private final Map<UUID, ArmoryStock> armoryStocks = new ConcurrentHashMap<>();

    public ArmoryStock getStock(Settlement settlement) {
        if (settlement == null) return new ArmoryStock(UUID.randomUUID());
        return armoryStocks.computeIfAbsent(settlement.getId(), ArmoryStock::new);
    }

    public void depositEquipment(Settlement settlement, ItemStack item) {
        if (settlement == null || item == null || item.isEmpty()) return;
        ArmoryStock stock = getStock(settlement);

        if (item.is(Items.IRON_SWORD)) stock.ironSwords += item.getCount();
        else if (item.is(Items.DIAMOND_SWORD)) stock.diamondSwords += item.getCount();
        else if (item.is(Items.BOW)) stock.bows += item.getCount();
        else if (item.is(Items.ARROW)) stock.arrows += item.getCount();
        else if (item.is(Items.SHIELD)) stock.shields += item.getCount();
        else if (item.is(Items.IRON_HELMET)) stock.ironHelmets += item.getCount();
        else if (item.is(Items.IRON_CHESTPLATE)) stock.ironChestplates += item.getCount();
        else if (item.is(Items.IRON_LEGGINGS)) stock.ironLeggings += item.getCount();
        else if (item.is(Items.IRON_BOOTS)) stock.ironBoots += item.getCount();
        else if (item.is(Items.SPLASH_POTION)) stock.healingPotions += item.getCount();
    }

    /**
     * Accepts armory gear donations from players, rewarding +reputation and village gratitude.
     */
    public boolean donateFromPlayer(ServerLevel level, ServerPlayer player, Settlement settlement, ItemStack held) {
        if (settlement == null || player == null || held == null || held.isEmpty()) return false;

        boolean isMilitaryGear = held.is(Items.IRON_SWORD) || held.is(Items.DIAMOND_SWORD)
            || held.is(Items.BOW) || held.is(Items.ARROW) || held.is(Items.SHIELD)
            || held.is(Items.IRON_HELMET) || held.is(Items.IRON_CHESTPLATE)
            || held.is(Items.IRON_LEGGINGS) || held.is(Items.IRON_BOOTS)
            || held.is(Items.SPLASH_POTION);

        if (!isMilitaryGear) return false;

        depositEquipment(settlement, held.copy());
        held.shrink(held.getCount());

        ReputationManager.get().addReputation(player.getUUID(), settlement.getId(), 5);
        level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 10, 0.4, 0.4, 0.4, 0.1);

        player.sendSystemMessage(Component.literal("§a[Living World] Donated military equipment to §6" + settlement.getName() + " §aArmory! (+5 Reputation)"));
        return true;
    }

    public void clear() {
        armoryStocks.clear();
    }
}
