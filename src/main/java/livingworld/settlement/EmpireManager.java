package livingworld.settlement;

import java.util.*;
import livingworld.config.FeatureConfig;
import livingworld.entity.LivingWorldEntities;
import livingworld.entity.VillageGuardEntity;
import livingworld.faction.FactionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fortified Villager Empire & Imperial Capital Manager (Fix 2 & 8, Section 15 & Milestone 5).
 * Elevates qualifying villages into expansive Imperial Capitals featuring:
 * - Multi-Building Fortified Layout: Imperial Palace, Barracks, Gatehouses, Markets, and Watchtowers
 * - Royal Court: Monarchs King Reginald II & Queen Seraphina
 * - Full Standing Imperial Army: Diamond Sentinels, Captain of the Guard, and Iron Heavy Infantry
 * - Greatly expanded imperial treasury, trade capacity, and 110-block civic territory
 */
public class EmpireManager {
    private static final EmpireManager INSTANCE = new EmpireManager();
    private final Set<UUID> activeEmpires = Collections.synchronizedSet(new HashSet<>());

    public static EmpireManager get() {
        return INSTANCE;
    }

    public static boolean isEmpireCandidate(long seed, BlockPos center, int chance) {
        if (chance <= 1) return true;
        long hash = (seed ^ center.asLong()) * 6364136223846793005L + 1442695040888963407L;
        return (Math.abs(hash) % chance) == 0;
    }

    public boolean isEmpire(UUID settlementId) {
        return activeEmpires.contains(settlementId);
    }

    public void checkAndPromoteToEmpire(ServerLevel level, Settlement settlement) {
        if (settlement == null || settlement.getFactionType() != FactionType.VILLAGER) return;
        if (activeEmpires.contains(settlement.getId())) return;

        long seed = level.getSeed();
        int chance = FeatureConfig.get().empireChance;

        if (isEmpireCandidate(seed, settlement.getCenter(), chance)) {
            promoteToEmpire(level, settlement);
        }
    }

    public void promoteToEmpire(ServerLevel level, Settlement settlement) {
        activeEmpires.add(settlement.getId());
        String imperialName = "Imperial Capital of " + settlement.getName();
        settlement.setName(imperialName);
        settlement.setRadius(110); // Expanded imperial territory
        settlement.setHousingCapacity(Math.max(64, settlement.getHousingCapacity() * 3));
        settlement.setPopulation(Math.max(24, settlement.getPopulation() * 2));

        // Substantial Imperial Treasury Endowment
        settlement.getStockpile().add(livingworld.economy.Stockpile.ResourceType.WOOD, 250);
        settlement.getStockpile().add(livingworld.economy.Stockpile.ResourceType.STONE, 200);
        settlement.getStockpile().add(livingworld.economy.Stockpile.ResourceType.FOOD, 200);
        settlement.getStockpile().add(livingworld.economy.Stockpile.ResourceType.IRON, 60);

        BlockPos center = settlement.getCenter();
        livingworld.worldgen.SettlementTerrainStabilizer.get().stabilizeSettlement(level, center, 110, null);

        // 1. Fortified Multi-Building Infrastructure: Queue Palace, Barracks, Gatehouse, Market, Watchtower
        queueImperialBuilding(level, settlement, "palace");
        queueImperialBuilding(level, settlement, "barracks");
        queueImperialBuilding(level, settlement, "gatehouse");
        queueImperialBuilding(level, settlement, "market");
        queueImperialBuilding(level, settlement, "watchtower");

        // 2. Spawn Villager Monarchs (King & Queen)
        Villager king = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
        if (king != null) {
            king.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
            king.setCustomName(Component.literal("§6King Reginald II"));
            king.setCustomNameVisible(true);
            level.addFreshEntity(king);
        }

        Villager queen = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
        if (queen != null) {
            queen.setPos(center.getX() - 0.5, center.getY() + 1.0, center.getZ() + 0.5);
            queen.setCustomName(Component.literal("§dQueen Seraphina"));
            queen.setCustomNameVisible(true);
            level.addFreshEntity(queen);
        }

        // 3. Standing Imperial Army: Captain, Sentinels, and Infantry
        // 3a. Captain of the Royal Guard (High-tier gear + enchanted aura)
        VillageGuardEntity captain = LivingWorldEntities.VILLAGE_GUARD.create(level, EntitySpawnReason.EVENT);
        if (captain != null) {
            captain.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 3.0);
            captain.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            captain.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            captain.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            captain.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            captain.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            captain.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            captain.setCustomName(Component.literal("§6[Royal Captain] Valerius"));
            captain.setCustomNameVisible(true);
            level.addFreshEntity(captain);
        }

        // 3b. 2 Diamond Throne Sentinels
        for (int i = 0; i < 2; i++) {
            VillageGuardEntity sentinel = LivingWorldEntities.VILLAGE_GUARD.create(level, EntitySpawnReason.EVENT);
            if (sentinel != null) {
                double offsetX = (i == 0) ? 2.5 : -2.5;
                sentinel.setPos(center.getX() + offsetX, center.getY() + 1.0, center.getZ() + 1.5);
                sentinel.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                sentinel.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                sentinel.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                sentinel.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                sentinel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                sentinel.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                sentinel.setCustomName(Component.literal("§6[Imperial Sentinel] " + (i == 0 ? "Theron" : "Garrick")));
                sentinel.setCustomNameVisible(true);
                level.addFreshEntity(sentinel);
            }
        }

        // 3c. 4 Imperial Heavy Infantry (Iron equipment, perimeter defense)
        String[] infantryNames = {"Julian", "Cassian", "Marcus", "Tiberius"};
        for (int i = 0; i < 4; i++) {
            VillageGuardEntity infantry = LivingWorldEntities.VILLAGE_GUARD.create(level, EntitySpawnReason.EVENT);
            if (infantry != null) {
                double angle = (i * Math.PI / 2.0);
                double dist = 8.0;
                infantry.setPos(center.getX() + Math.cos(angle) * dist, center.getY() + 1.0, center.getZ() + Math.sin(angle) * dist);
                infantry.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                infantry.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                infantry.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                infantry.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                infantry.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                infantry.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                infantry.setCustomName(Component.literal("§e[Imperial Infantry] " + infantryNames[i]));
                infantry.setCustomNameVisible(false);
                level.addFreshEntity(infantry);
            }
        }

        // 3d. 2 Imperial Rampart Archers / Marksmen (Diamond-trimmed bows, elevated defense)
        String[] archerNames = {"Hawthorne", "Caelen"};
        for (int i = 0; i < 2; i++) {
            livingworld.entity.VillageArcherEntity archer = LivingWorldEntities.VILLAGE_ARCHER.create(level, EntitySpawnReason.EVENT);
            if (archer != null) {
                double angle = (i * Math.PI) + (Math.PI / 4.0);
                double dist = 10.0;
                archer.setPos(center.getX() + Math.cos(angle) * dist, center.getY() + 1.0, center.getZ() + Math.sin(angle) * dist);
                archer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                archer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                archer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                archer.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                archer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW));
                archer.setCustomName(Component.literal("§6[Imperial Marksman] " + archerNames[i]));
                archer.setCustomNameVisible(true);
                level.addFreshEntity(archer);
            }
        }

        // Visual and sound celebration
        level.playSound(null, center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 1.0f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, center.getX() + 0.5, center.getY() + 2.0, center.getZ() + 0.5, 45, 1.5, 1.5, 1.5, 0.2);

        // Notify nearby players
        for (ServerPlayer player : level.players()) {
            if (settlement.isInSettlement(player.blockPosition())) {
                player.sendSystemMessage(Component.literal(
                    "§6★ §eA grand civilization rises! §6" + imperialName +
                    "§e has been proclaimed an §6IMPERIAL CAPITAL§e with a fortified multi-building layout and standing army!"));
            }
        }

        livingworld.LivingWorldMod.LOGGER.info("[Living World] Proclaimed " + imperialName + " at " + center +
            " with Palace, Barracks, Gatehouse, Market, Watchtower, Royal Court, and 7-man Standing Army.");
    }

    private void queueImperialBuilding(ServerLevel level, Settlement settlement, String blueprintId) {
        Blueprint bp = BlueprintRegistry.get(blueprintId);
        if (bp != null) {
            BlockPos plot = settlement.findClearPlot(level, bp.getWidth(), bp.getDepth());
            if (plot != null) {
                settlement.queueProject(new ConstructionProject(blueprintId, plot));
            }
        }
    }
}
