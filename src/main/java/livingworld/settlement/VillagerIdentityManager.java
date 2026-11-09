package livingworld.settlement;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.LivingWorldMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * Villager Identity and Career Manager (Section 10 & Milestone 3).
 * Provides culturally styled persistent names, titles, profession tracking,
 * and work contribution records across world saves.
 */
public class VillagerIdentityManager {
    private static final VillagerIdentityManager INSTANCE = new VillagerIdentityManager();

    private final Map<UUID, VillagerProfile> profiles = new ConcurrentHashMap<>();

    private static final List<String> PLAINS_NAMES = List.of(
        "Alden", "Rowan", "Cedric", "Elspeth", "Garrick", "Maren", "Osric", "Willa", "Theron", "Alina", "Bram", "Elowen"
    );
    private static final List<String> DESERT_NAMES = List.of(
        "Tariq", "Zarah", "Kareem", "Samira", "Farouk", "Layla", "Jamil", "Soraya", "Rashid", "Nadia", "Salim", "Yasmin"
    );
    private static final List<String> TAIGA_NAMES = List.of(
        "Bjorn", "Freja", "Sven", "Astrid", "Einar", "Ingrid", "Torstein", "Signe", "Leif", "Ragnor", "Kari", "Hakon"
    );
    private static final List<String> SAVANNA_NAMES = List.of(
        "Kofi", "Amina", "Jabari", "Zola", "Tendai", "Nia", "Kwame", "Makena", "Bakari", "Chipo", "Jengo", "Zuri"
    );
    private static final List<String> DEFAULT_NAMES = List.of(
        "Giles", "Edith", "Bartholomew", "Beatrix", "Tobias", "Agnes", "Godfrey", "Margery", "Clement", "Winifred"
    );

    public static VillagerIdentityManager get() {
        return INSTANCE;
    }

    public static class VillagerProfile {
        private final UUID uuid;
        private String name;
        private String title;
        private String profession;
        private UUID settlementId;
        private int workContributions;
        private int daysActive;

        public VillagerProfile(UUID uuid, String name, String title, String profession, UUID settlementId) {
            this.uuid = uuid;
            this.name = name;
            this.title = title;
            this.profession = profession;
            this.settlementId = settlementId;
            this.workContributions = 0;
            this.daysActive = 1;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getProfession() {
            return profession;
        }

        public void setProfession(String profession) {
            this.profession = profession;
        }

        public UUID getSettlementId() {
            return settlementId;
        }

        public void setSettlementId(UUID settlementId) {
            this.settlementId = settlementId;
        }

        public int getWorkContributions() {
            return workContributions;
        }

        public void addWorkContribution(int amount) {
            this.workContributions += amount;
        }

        public int getDaysActive() {
            return daysActive;
        }

        public void incrementDaysActive() {
            this.daysActive++;
        }

        public String getFullName() {
            if (title == null || title.isEmpty()) {
                return name;
            }
            return name + " " + title;
        }
    }

    public VillagerProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    public VillagerProfile getOrCreateProfile(Villager villager, Settlement settlement) {
        UUID uuid = villager.getUUID();
        VillagerProfile existing = profiles.get(uuid);
        if (existing != null) {
            return existing;
        }

        String biomeName = settlement != null ? settlement.getName().toLowerCase() : "plains";
        String chosenName = generateNameForBiome(biomeName, villager.getRandom());
        String professionKey = getProfessionKey(villager);
        String chosenTitle = generateTitle(professionKey, villager.isBaby());

        UUID settlementId = settlement != null ? settlement.getId() : null;
        VillagerProfile profile = new VillagerProfile(uuid, chosenName, chosenTitle, professionKey, settlementId);
        profiles.put(uuid, profile);

        applyProfileToEntity(villager, profile);
        return profile;
    }

    public void applyProfileToEntity(Villager villager, VillagerProfile profile) {
        if (villager == null || profile == null) return;
        villager.setCustomName(Component.literal("§e" + profile.getFullName()));
        villager.setCustomNameVisible(false); // Only display on crosshair / look
    }

    public void updateProfession(Villager villager) {
        VillagerProfile profile = profiles.get(villager.getUUID());
        if (profile == null) return;

        String newProf = getProfessionKey(villager);
        if (!newProf.equals(profile.getProfession())) {
            profile.setProfession(newProf);
            profile.setTitle(generateTitle(newProf, villager.isBaby()));
            applyProfileToEntity(villager, profile);
        }
    }

    public void recordWorkContribution(UUID villagerUuid, int amount) {
        VillagerProfile profile = profiles.get(villagerUuid);
        if (profile != null) {
            profile.addWorkContribution(amount);
            if (profile.getWorkContributions() >= 100 && !profile.getTitle().contains("Master")) {
                profile.setTitle("the Master " + profile.getProfession());
            }
        }
    }

    public Collection<VillagerProfile> getAllProfiles() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    private String generateNameForBiome(String biome, net.minecraft.util.RandomSource random) {
        List<String> pool;
        if (biome.contains("desert") || biome.contains("oasis") || biome.contains("dune")) {
            pool = DESERT_NAMES;
        } else if (biome.contains("taiga") || biome.contains("snow") || biome.contains("frost") || biome.contains("pines")) {
            pool = TAIGA_NAMES;
        } else if (biome.contains("savanna") || biome.contains("acacia") || biome.contains("mesa")) {
            pool = SAVANNA_NAMES;
        } else if (biome.contains("plains") || biome.contains("oak") || biome.contains("valley")) {
            pool = PLAINS_NAMES;
        } else {
            pool = DEFAULT_NAMES;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private String generateTitle(String profession, boolean isBaby) {
        if (isBaby) {
            return "the Youngster";
        }
        return switch (profession.toLowerCase()) {
            case "farmer" -> "the Harvester";
            case "fisherman" -> "the Angler";
            case "shepherd" -> "the Herder";
            case "fletcher" -> "the Bowyer";
            case "librarian" -> "the Scholar";
            case "cartographer" -> "the Mapmaker";
            case "cleric" -> "the Mystic";
            case "armorer" -> "the Armorsmith";
            case "weaponsmith" -> "the Bladesmith";
            case "toolsmith" -> "the Toolmaker";
            case "butcher" -> "the Butcher";
            case "leatherworker" -> "the Tanner";
            case "mason" -> "the Mason";
            case "nitwit" -> "the Wanderer";
            default -> "the Apprentice";
        };
    }

    private String getProfessionKey(Villager villager) {
        try {
            var holder = villager.getVillagerData().profession();
            if (holder.is(VillagerProfession.NONE)) return "unemployed";
            return holder.unwrapKey().map(k -> k.identifier().getPath()).orElse("unemployed");
        } catch (Throwable t) {
            return "unemployed";
        }
    }

    public void clear() {
        profiles.clear();
    }
}
