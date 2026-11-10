package livingworld.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Functional Village Workforce & Profession Management Engine (Section 3, Living Village Update).
 * Enforces a balanced demographic workforce across all 13 vanilla professions:
 * Farmer, Fisherman, Fletcher, Shepherd, Butcher, Leatherworker, Librarian,
 * Cartographer, Cleric, Armorer, Weaponsmith, Toolsmith, and Mason.
 * Target distribution: At least 2 active working adults per profession (26 total).
 * Coordinates real economic production (food, stone, armory equipment, arrows, and medicine).
 */
public class VillageWorkforceManager {
    private static final VillageWorkforceManager INSTANCE = new VillageWorkforceManager();

    public static VillageWorkforceManager get() {
        return INSTANCE;
    }

    public enum ManagedProfession {
        FARMER("Farmer", Items.COMPOSTER, Stockpile.ResourceType.FOOD, 4),
        FISHERMAN("Fisherman", Items.BARREL, Stockpile.ResourceType.FOOD, 3),
        FLETCHER("Fletcher", Items.FLETCHING_TABLE, null, 0),
        SHEPHERD("Shepherd", Items.LOOM, Stockpile.ResourceType.WOOL, 2),
        BUTCHER("Butcher", Items.SMOKER, Stockpile.ResourceType.FOOD, 5),
        LEATHERWORKER("Leatherworker", Items.CAULDRON, Stockpile.ResourceType.LEATHER, 2),
        LIBRARIAN("Librarian", Items.LECTERN, null, 0),
        CARTOGRAPHER("Cartographer", Items.CARTOGRAPHY_TABLE, null, 0),
        CLERIC("Cleric", Items.BREWING_STAND, null, 0),
        ARMORER("Armorer", Items.BLAST_FURNACE, null, 0),
        WEAPONSMITH("Weaponsmith", Items.GRINDSTONE, null, 0),
        TOOLSMITH("Toolsmith", Items.SMITHING_TABLE, null, 0),
        MASON("Mason", Items.STONECUTTER, Stockpile.ResourceType.STONE, 4);

        private final String title;
        private final net.minecraft.world.item.Item workstationItem;
        private final Stockpile.ResourceType outputResource;
        private final int outputAmount;

        ManagedProfession(String title, net.minecraft.world.item.Item workstationItem, Stockpile.ResourceType outputResource, int outputAmount) {
            this.title = title;
            this.workstationItem = workstationItem;
            this.outputResource = outputResource;
            this.outputAmount = outputAmount;
        }

        public String getTitle() {
            return title;
        }

        public net.minecraft.world.item.Item getWorkstationItem() {
            return workstationItem;
        }

        public Stockpile.ResourceType getOutputResource() {
            return outputResource;
        }

        public int getOutputAmount() {
            return outputAmount;
        }
    }

    public static class WorkforceCensus {
        private final UUID settlementId;
        private final Map<ManagedProfession, Integer> counts = new EnumMap<>(ManagedProfession.class);
        private int unemployedCount = 0;
        private int childrenCount = 0;
        private int totalVillagers = 0;

        public WorkforceCensus(UUID settlementId) {
            this.settlementId = settlementId;
            for (ManagedProfession p : ManagedProfession.values()) {
                counts.put(p, 0);
            }
        }

        public int getCount(ManagedProfession p) {
            return counts.getOrDefault(p, 0);
        }

        public void increment(ManagedProfession p) {
            counts.put(p, getCount(p) + 1);
        }

        public int getUnemployedCount() {
            return unemployedCount;
        }

        public int getChildrenCount() {
            return childrenCount;
        }

        public int getTotalVillagers() {
            return totalVillagers;
        }

        public Map<ManagedProfession, Integer> getCounts() {
            return Collections.unmodifiableMap(counts);
        }

        public int getStaffedProfessionsCount() {
            int staffed = 0;
            for (int count : counts.values()) {
                if (count >= 2) staffed++;
            }
            return staffed;
        }
    }

    private final Map<UUID, WorkforceCensus> settlementCensuses = new ConcurrentHashMap<>();

    public WorkforceCensus getCensus(UUID settlementId) {
        return settlementCensuses.computeIfAbsent(settlementId, WorkforceCensus::new);
    }

    /**
     * Conducts workforce census, assigns available adults to deficit professions,
     * and triggers tangible economic production cycles into village stockpiles and armories.
     */
    public void tickWorkforce(ServerLevel level, Settlement settlement) {
        if (level == null || settlement == null) return;

        BlockPos center = settlement.getCenter();
        AABB villageZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 24, settlement.getRadius() * 2);

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, villageZone, v -> v.isAlive());
        WorkforceCensus census = new WorkforceCensus(settlement.getId());
        census.totalVillagers = villagers.size();

        List<Villager> unemployedAdults = new ArrayList<>();

        for (Villager villager : villagers) {
            if (villager.isBaby()) {
                census.childrenCount++;
                continue;
            }

            VillagerData data = villager.getVillagerData();
            ManagedProfession mapped = mapProfession(data.profession());

            if (mapped != null) {
                census.increment(mapped);
            } else {
                census.unemployedCount++;
                unemployedAdults.add(villager);
            }
        }

        settlementCensuses.put(settlement.getId(), census);
        settlement.setPopulation(census.totalVillagers);

        // 1. Assign Unemployed Adults to Deficit Professions (Target: 2 per profession)
        if (!unemployedAdults.isEmpty()) {
            for (ManagedProfession prof : ManagedProfession.values()) {
                if (census.getCount(prof) < 2 && !unemployedAdults.isEmpty()) {
                    Villager recruit = unemployedAdults.remove(0);
                    assignProfession(level, recruit, prof);
                    census.increment(prof);
                    census.unemployedCount--;
                }
            }
        }

        // 2. Tangible Production Cycle into Settlement Stockpiles and Armory
        for (ManagedProfession prof : ManagedProfession.values()) {
            int workers = census.getCount(prof);
            if (workers > 0) {
                // Stockpile resource contribution
                if (prof.getOutputResource() != null && prof.getOutputAmount() > 0) {
                    settlement.getStockpile().add(prof.getOutputResource(), workers * prof.getOutputAmount());
                }

                // Armory & Military Support Contributions
                if (prof == ManagedProfession.ARMORER) {
                    VillageArmoryManager.get().depositEquipment(settlement, new ItemStack(Items.IRON_CHESTPLATE, workers));
                    VillageArmoryManager.get().depositEquipment(settlement, new ItemStack(Items.SHIELD, workers));
                } else if (prof == ManagedProfession.WEAPONSMITH) {
                    VillageArmoryManager.get().depositEquipment(settlement, new ItemStack(Items.IRON_SWORD, workers));
                } else if (prof == ManagedProfession.FLETCHER) {
                    VillageArmoryManager.get().depositEquipment(settlement, new ItemStack(Items.ARROW, workers * 16));
                    VillageArmoryManager.get().depositEquipment(settlement, new ItemStack(Items.BOW, Math.max(1, workers / 2)));
                } else if (prof == ManagedProfession.CLERIC) {
                    // Cleric emergency medicine reserves
                    VillageArmoryManager.get().depositEquipment(settlement, new ItemStack(Items.SPLASH_POTION, workers));
                }
            }
        }
    }

    private void assignProfession(ServerLevel level, Villager villager, ManagedProfession targetProf) {
        net.minecraft.resources.ResourceKey<VillagerProfession> key = getVanillaProfession(targetProf);
        if (key != null) {
            var reg = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION);
            villager.setVillagerData(villager.getVillagerData().withProfession(reg.getOrThrow(key)));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.2, villager.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
            level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_WORK_ARMORER, SoundSource.NEUTRAL, 0.9f, 1.0f);
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Profession assignment: Villager designated as " + targetProf.getTitle());
        }
    }

    private ManagedProfession mapProfession(net.minecraft.core.Holder<VillagerProfession> holder) {
        if (holder == null) return null;
        if (holder.is(VillagerProfession.FARMER)) return ManagedProfession.FARMER;
        if (holder.is(VillagerProfession.FISHERMAN)) return ManagedProfession.FISHERMAN;
        if (holder.is(VillagerProfession.FLETCHER)) return ManagedProfession.FLETCHER;
        if (holder.is(VillagerProfession.SHEPHERD)) return ManagedProfession.SHEPHERD;
        if (holder.is(VillagerProfession.BUTCHER)) return ManagedProfession.BUTCHER;
        if (holder.is(VillagerProfession.LEATHERWORKER)) return ManagedProfession.LEATHERWORKER;
        if (holder.is(VillagerProfession.LIBRARIAN)) return ManagedProfession.LIBRARIAN;
        if (holder.is(VillagerProfession.CARTOGRAPHER)) return ManagedProfession.CARTOGRAPHER;
        if (holder.is(VillagerProfession.CLERIC)) return ManagedProfession.CLERIC;
        if (holder.is(VillagerProfession.ARMORER)) return ManagedProfession.ARMORER;
        if (holder.is(VillagerProfession.WEAPONSMITH)) return ManagedProfession.WEAPONSMITH;
        if (holder.is(VillagerProfession.TOOLSMITH)) return ManagedProfession.TOOLSMITH;
        if (holder.is(VillagerProfession.MASON)) return ManagedProfession.MASON;
        return null;
    }

    private net.minecraft.resources.ResourceKey<VillagerProfession> getVanillaProfession(ManagedProfession prof) {
        return switch (prof) {
            case FARMER -> VillagerProfession.FARMER;
            case FISHERMAN -> VillagerProfession.FISHERMAN;
            case FLETCHER -> VillagerProfession.FLETCHER;
            case SHEPHERD -> VillagerProfession.SHEPHERD;
            case BUTCHER -> VillagerProfession.BUTCHER;
            case LEATHERWORKER -> VillagerProfession.LEATHERWORKER;
            case LIBRARIAN -> VillagerProfession.LIBRARIAN;
            case CARTOGRAPHER -> VillagerProfession.CARTOGRAPHER;
            case CLERIC -> VillagerProfession.CLERIC;
            case ARMORER -> VillagerProfession.ARMORER;
            case WEAPONSMITH -> VillagerProfession.WEAPONSMITH;
            case TOOLSMITH -> VillagerProfession.TOOLSMITH;
            case MASON -> VillagerProfession.MASON;
        };
    }

    public void clear() {
        settlementCensuses.clear();
    }
}
