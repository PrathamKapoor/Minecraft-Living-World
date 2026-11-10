package livingworld.settlement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import livingworld.config.FeatureConfig;
import livingworld.faction.FactionType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

public class SettlementManager {
    private static final SettlementManager INSTANCE = new SettlementManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Settlement> settlements = new HashMap<>();

    public static SettlementManager get() {
        return INSTANCE;
    }

    public void register(Settlement settlement) {
        settlements.put(settlement.getId(), settlement);
    }

    public Settlement getSettlement(UUID id) {
        return settlements.get(id);
    }

    public Collection<Settlement> getAllSettlements() {
        return settlements.values();
    }

    public Settlement getSettlementAt(BlockPos pos) {
        for (Settlement s : settlements.values()) {
            if (s.isInSettlement(pos)) {
                return s;
            }
        }
        return null;
    }

    public Settlement getNearestSettlement(BlockPos pos) {
        Settlement atPos = getSettlementAt(pos);
        if (atPos != null) {
            return atPos;
        }
        Settlement nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Settlement s : settlements.values()) {
            double distSq = s.getCenter().distSqr(pos);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = s;
            }
        }
        return nearest;
    }

    public Settlement getOrCreateSettlement(BlockPos pos, String defaultName, FactionType factionType) {
        Settlement existing = getSettlementAt(pos);
        if (existing != null) {
            return existing;
        }
        Settlement created = new Settlement(UUID.randomUUID(), defaultName, pos, 64, factionType);
        register(created);
        return created;
    }

    public Settlement checkAndAutoDiscoverVillage(ServerLevel level, BlockPos pos) {
        if (!FeatureConfig.get().enableVillageGuards && !FeatureConfig.get().enableAutonomousConstruction) {
            return null;
        }

        // Safeguard 1: Never auto-discover villages in oceans, deep oceans, or rivers
        var biome = level.getBiome(pos);
        if (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) ||
            biome.is(net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN) ||
            biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)) {
            return null;
        }

        // Safeguard 2: Center must be at or above sea level
        if (pos.getY() < 62) {
            return null;
        }

        Settlement existing = getSettlementAt(pos);
        if (existing != null) {
            return existing;
        }

        // Query nearby villagers to confirm a legitimate village community (radius 80 blocks)
        net.minecraft.world.phys.AABB searchArea = net.minecraft.world.phys.AABB.ofSize(
            new net.minecraft.world.phys.Vec3(pos.getX(), pos.getY(), pos.getZ()), 160, 48, 160);
        List<net.minecraft.world.entity.npc.villager.Villager> nearby = level.getEntitiesOfClass(
            net.minecraft.world.entity.npc.villager.Villager.class, searchArea, v -> v.isAlive());

        // Safeguard 3: Outcast boat / floating villager rejection
        long dryLandVillagers = nearby.stream().filter(v -> !v.isInWater() && v.getVehicle() == null).count();
        if (dryLandVillagers == 0) {
            return null;
        }

        // Community threshold: at least 1 living villager on dry land confirms living settlement
        if (nearby.size() >= 1) {
            String name = generateVillageName(level, pos);
            Settlement settlement = new Settlement(UUID.randomUUID(), name, pos, 64, FactionType.VILLAGER);
            settlement.setPopulation(Math.max(4, nearby.size()));
            settlement.setHousingCapacity(settlement.getPopulation() + 1); // Triggers autonomous construction
            register(settlement);
            EmpireManager.get().checkAndPromoteToEmpire(level, settlement);
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Auto-discovered natural village: " + name + " at " + pos + " with population " + settlement.getPopulation());

            // Notify nearby players
            for (net.minecraft.server.level.ServerPlayer player : level.players()) {
                if (settlement.isInSettlement(player.blockPosition())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6[Living World] §eDiscovered §a" + name + "§e! Civilization engine & stockpiles activated."));
                }
            }
            return settlement;
        }
        return null;
    }

    private String generateVillageName(ServerLevel level, BlockPos pos) {
        String biomeKey = level.getBiome(pos).unwrapKey().map(k -> k.identifier().getPath()).orElse("plains");
        if (biomeKey.contains("desert")) {
            return "Dunehaven Oasis";
        } else if (biomeKey.contains("savanna")) {
            return "Acacia Ridge";
        } else if (biomeKey.contains("taiga")) {
            return "Pinehurst Enclave";
        } else if (biomeKey.contains("snow") || biomeKey.contains("ice")) {
            return "Frostwood Camp";
        } else if (biomeKey.contains("swamp")) {
            return "Stillwater Refuge";
        } else {
            int hash = Math.abs(pos.getX() * 31 + pos.getZ()) % 4;
            String[] names = {"Oakvale Settlement", "Suncrest Village", "Riverwood Hamlet", "Greenfield Community"};
            return names[hash];
        }
    }

    public void tick(ServerLevel level, long currentTicks) {
        int strategicInterval = FeatureConfig.get().strategicTickInterval;
        boolean runStrategic = (currentTicks % strategicInterval == 0);

        // Periodic auto-discovery check around active players (every 5 seconds)
        if (currentTicks % 100 == 0) {
            for (net.minecraft.server.level.ServerPlayer player : level.players()) {
                BlockPos ppos = player.blockPosition();
                if (getSettlementAt(ppos) == null) {
                    net.minecraft.world.phys.AABB pArea = net.minecraft.world.phys.AABB.ofSize(
                        new net.minecraft.world.phys.Vec3(ppos.getX(), ppos.getY(), ppos.getZ()), 160, 48, 160);
                    List<net.minecraft.world.entity.npc.villager.Villager> vList = level.getEntitiesOfClass(
                        net.minecraft.world.entity.npc.villager.Villager.class, pArea, v -> v.isAlive());
                    if (!vList.isEmpty()) {
                        checkAndAutoDiscoverVillage(level, vList.get(0).blockPosition());
                    }
                }
            }
        }

        for (Settlement settlement : settlements.values()) {
            if (runStrategic) {
                settlement.strategicTick(level, currentTicks);
            }
            settlement.tacticalTick(level, FeatureConfig.get().maxConstructionBlocksPerTick);

            if (currentTicks % 40 == 0) {
                livingworld.economy.EconomyManager.get().tickLogistics(level, settlement);
                livingworld.settlement.NaturalBreedingManager.get().tickSettlementDemographics(level, settlement);
            }
            if (currentTicks % 60 == 0) {
                livingworld.economy.VillageWorkforceManager.get().tickWorkforce(level, settlement);
                livingworld.faction.VillageGolemManager.get().tickGolems(level, settlement);
                livingworld.faction.CivilianSupportManager.get().tickCivilianSupport(level, settlement);
            }
            if (currentTicks % 100 == 0) {
                livingworld.faction.VillageGuardManager.get().tickGuardRecruitment(level, settlement);
            }
            // Continuous Threat & Defense Controller tick
            livingworld.faction.VillageDefenseController.get().tickDefense(level, settlement);
        }

        // Periodic checkpoint to disk (every 15 seconds)
        if (currentTicks % 300 == 0 && !settlements.isEmpty()) {
            save(level);
        }
    }

    public void save(ServerLevel level) {
        try {
            Path rootPath = level.getServer().getWorldPath(LevelResource.ROOT);
            File saveFile = rootPath.resolve("livingworld").resolve("settlements.json").toFile();
            saveFile.getParentFile().mkdirs();

            List<SettlementData> dataList = new ArrayList<>();
            for (Settlement s : settlements.values()) {
                SettlementData d = new SettlementData();
                d.id = s.getId().toString();
                d.name = s.getName();
                d.centerX = s.getCenter().getX();
                d.centerY = s.getCenter().getY();
                d.centerZ = s.getCenter().getZ();
                d.radius = s.getRadius();
                d.factionType = s.getFactionType().name();
                d.state = s.getState().name();
                d.population = s.getPopulation();
                d.housingCapacity = s.getHousingCapacity();
                d.guardCount = s.getGuardCount();
                d.stockpile = s.getStockpile().toMap();

                // Save active project if any
                if (s.getActiveProject() != null) {
                    ConstructionProject ap = s.getActiveProject();
                    d.activeBlueprintId = ap.getBlueprintId();
                    d.activeOriginX = ap.getOrigin().getX();
                    d.activeOriginY = ap.getOrigin().getY();
                    d.activeOriginZ = ap.getOrigin().getZ();
                }

                dataList.add(d);
            }

            try (FileWriter writer = new FileWriter(saveFile)) {
                GSON.toJson(dataList, writer);
            }
        } catch (Exception e) {
            System.err.println("[LivingWorld] Failed to save settlements: " + e.getMessage());
        }
    }

    public void load(ServerLevel level) {
        try {
            Path rootPath = level.getServer().getWorldPath(LevelResource.ROOT);
            File saveFile = rootPath.resolve("livingworld").resolve("settlements.json").toFile();
            if (!saveFile.exists()) return;

            try (FileReader reader = new FileReader(saveFile)) {
                Type listType = new TypeToken<List<SettlementData>>() {}.getType();
                List<SettlementData> dataList = GSON.fromJson(reader, listType);
                if (dataList != null) {
                    settlements.clear();
                    for (SettlementData d : dataList) {
                        BlockPos center = new BlockPos(d.centerX, d.centerY, d.centerZ);
                        FactionType fType = FactionType.valueOf(d.factionType);
                        Settlement s = new Settlement(UUID.fromString(d.id), d.name, center, d.radius, fType);
                        s.setState(SettlementState.valueOf(d.state));
                        s.setPopulation(d.population);
                        s.setHousingCapacity(d.housingCapacity > 0 ? d.housingCapacity : 10);
                        s.setGuardCount(d.guardCount);
                        s.getStockpile().fromMap(d.stockpile);

                        if (d.activeBlueprintId != null) {
                            BlockPos origin = new BlockPos(d.activeOriginX, d.activeOriginY, d.activeOriginZ);
                            s.queueProject(new ConstructionProject(d.activeBlueprintId, origin));
                        }

                        register(s);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LivingWorld] Failed to load settlements: " + e.getMessage());
        }
    }

    public void clear() {
        settlements.clear();
    }

    private static class SettlementData {
        String id;
        String name;
        int centerX, centerY, centerZ;
        int radius;
        String factionType;
        String state;
        int population;
        int housingCapacity;
        int guardCount;
        Map<String, Integer> stockpile;
        String activeBlueprintId;
        int activeOriginX, activeOriginY, activeOriginZ;
    }
}
