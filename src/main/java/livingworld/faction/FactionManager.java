package livingworld.faction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import net.minecraft.core.BlockPos;

public class FactionManager {
    private static final FactionManager INSTANCE = new FactionManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Faction> factions = new HashMap<>();

    public FactionManager() {
        initDefaultFactions();
    }

    public static FactionManager get() {
        return INSTANCE;
    }

    private void initDefaultFactions() {
        register(new Faction("villagers", "Villager Commonwealth", FactionType.VILLAGER, BlockPos.ZERO, 500));
        register(new Faction("undead", "Undead Scourge", FactionType.UNDEAD, BlockPos.ZERO, 1000));
        register(new Faction("illagers", "Illager Legion", FactionType.ILLAGER, BlockPos.ZERO, 800));
        register(new Faction("piglin_crimson", "Crimson Tusks Tribe", FactionType.PIGLIN_TRIBE, BlockPos.ZERO, 600));
        register(new Faction("piglin_warped", "Warped Shadows Tribe", FactionType.PIGLIN_TRIBE, BlockPos.ZERO, 600));
    }

    public void register(Faction faction) {
        factions.put(faction.getId(), faction);
    }

    public Faction getFaction(String id) {
        return factions.get(id);
    }

    public Collection<Faction> getAllFactions() {
        return factions.values();
    }

    public Faction getFactionAt(BlockPos pos) {
        for (Faction f : factions.values()) {
            if (f.isInTerritory(pos)) {
                return f;
            }
        }
        return null;
    }

    public boolean isHostile(String factionA, String factionB) {
        if (factionA == null || factionB == null || factionA.equals(factionB)) return false;
        if (("undead".equals(factionA) || "illagers".equals(factionA)) && ("villagers".equals(factionB) || "VILLAGE_ALLIANCE".equals(factionB))) return true;
        if (("undead".equals(factionB) || "illagers".equals(factionB)) && ("villagers".equals(factionA) || "VILLAGE_ALLIANCE".equals(factionA))) return true;
        return false;
    }

    public void save(File saveFile) {
        try {
            saveFile.getParentFile().mkdirs();
            List<FactionData> dataList = new ArrayList<>();
            for (Faction f : factions.values()) {
                FactionData d = new FactionData();
                d.id = f.getId();
                d.name = f.getName();
                d.type = f.getType().name();
                d.centerX = f.getTerritoryCenter().getX();
                d.centerY = f.getTerritoryCenter().getY();
                d.centerZ = f.getTerritoryCenter().getZ();
                d.radius = f.getTerritoryRadius();
                d.morale = f.getMorale();
                d.reputations = new HashMap<>();
                for (Map.Entry<UUID, Integer> entry : f.getReputations().entrySet()) {
                    d.reputations.put(entry.getKey().toString(), entry.getValue());
                }
                dataList.add(d);
            }
            try (FileWriter writer = new FileWriter(saveFile)) {
                GSON.toJson(dataList, writer);
            }
        } catch (Exception e) {
            System.err.println("[LivingWorld] Failed to save factions: " + e.getMessage());
        }
    }

    public void load(File saveFile) {
        if (!saveFile.exists()) return;
        try (FileReader reader = new FileReader(saveFile)) {
            Type listType = new TypeToken<List<FactionData>>() {}.getType();
            List<FactionData> list = GSON.fromJson(reader, listType);
            if (list != null) {
                factions.clear();
                for (FactionData d : list) {
                    BlockPos center = new BlockPos(d.centerX, d.centerY, d.centerZ);
                    FactionType type = FactionType.valueOf(d.type);
                    Faction f = new Faction(d.id, d.name, type, center, d.radius);
                    f.setMorale(d.morale);
                    if (d.reputations != null) {
                        for (Map.Entry<String, Integer> rep : d.reputations.entrySet()) {
                            try {
                                f.adjustReputation(UUID.fromString(rep.getKey()), rep.getValue());
                            } catch (Exception ignored) {}
                        }
                    }
                    register(f);
                }
            }
        } catch (Exception e) {
            System.err.println("[LivingWorld] Failed to load factions: " + e.getMessage());
        }
    }

    private static class FactionData {
        String id;
        String name;
        String type;
        int centerX, centerY, centerZ;
        int radius;
        double morale;
        Map<String, Integer> reputations;
    }
}
