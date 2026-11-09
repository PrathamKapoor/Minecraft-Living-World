package livingworld.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FeatureConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static FeatureConfig INSTANCE = new FeatureConfig();

    // Feature Toggles (F01 - F18)
    public boolean enableZombieIntelligence = true;       // F01
    public boolean enableZombieDoorBreach = true;         // F01 door breach
    public boolean enableSkeletonTactics = true;          // F02
    public boolean enableCreeperBreach = true;            // F03
    public boolean enableIllagerTactics = true;           // F04
    public boolean enableVillageGuards = true;            // F05
    public boolean enableAutonomousConstruction = true;   // F06
    public boolean enableAnimalHusbandry = true;          // F07
    public boolean enableUndeadOccupation = true;         // F08
    public boolean enableZombieAnimals = true;            // F09
    public boolean enableZombieHordes = true;             // F10
    public boolean enableGiantZombieBoss = true;          // F10 giant boss
    public boolean enablePiglinTribes = true;             // F11
    public boolean enableAerialPredators = true;          // F12
    public boolean enableDragonEnhancements = true;       // F13
    public boolean enableDimensionBosses = true;          // F14
    public boolean enableSharks = true;                   // F15
    public boolean enableExplorationClues = true;         // F16
    public boolean enableMinecartImprovements = true;     // F17
    public boolean enableGlobalMap = true;                // F18
    public boolean enableAccessibleEnchanting = true;     // F18 accessible enchanting

    // Version 0.2 Extended Configuration & Addendum Requirements
    public boolean zombieMiningEnabled = true;             // Addendum 2: Tool-wielding zombies & siege mining
    public double zombieToolSpawnRate = 0.05;              // 5% (1 in 20 naturally spawned adult zombies)
    public double zombiePickaxeShare = 0.45;               // 45% of tool-bearers carry pickaxe
    public double zombieAxeShare = 0.35;                   // 35% of tool-bearers carry axe
    public double zombieShovelShare = 0.20;                // 20% of tool-bearers carry shovel
    public int maxSimultaneousSiegeMiners = 4;             // Maximum active siege miners in proximity
    public int maxMiningDistance = 16;                     // Maximum pursuit distance for mining barrier
    public boolean minGuardArmorTierIron = true;           // Section 10: Minimum iron-tier protection
    public int empireChance = 64;                          // Section 13: 1 in 64 village empire generation

    // Safety and Protection Settings
    public boolean protectPlayerBlocks = true;
    public boolean protectPlayerAnimals = true;
    public boolean respectMobGriefingRule = true;
    public int maxConstructionBlocksPerTick = 4;
    public int strategicTickInterval = 200; // Ticks between settlement strategic passes
    public int maxActiveHordesPerWorld = 4;
    public double minecartSpeedMultiplier = 2.5;

    public static FeatureConfig get() {
        return INSTANCE;
    }

    public static void load(File configFile) {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                FeatureConfig loaded = GSON.fromJson(reader, FeatureConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
            } catch (Exception e) {
                System.err.println("[LivingWorld] Failed to load config, falling back to defaults: " + e.getMessage());
            }
        } else {
            save(configFile);
        }
    }

    public static void save(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            System.err.println("[LivingWorld] Failed to save config: " + e.getMessage());
        }
    }
}
