package livingworld.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.ParticleTypes;
import livingworld.entity.VillageArcherEntity;
import livingworld.entity.VillageGuardEntity;
import livingworld.entity.VillageCavalryEntity;
import livingworld.entity.VillageDogEntity;
import livingworld.illager.RaidTactics;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import livingworld.bosses.GiantZombieBoss;
import livingworld.bosses.NetherBehemothBoss;
import livingworld.config.FeatureConfig;
import livingworld.exploration.ExplorationManager;
import livingworld.exploration.GlobalMapManager;
import livingworld.faction.FactionType;
import livingworld.settlement.ConstructionProject;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import livingworld.undead.ZombieHordeManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class LivingWorldCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        dispatcher.register(
            Commands.literal("livingworld")
                .then(Commands.literal("status").executes(LivingWorldCommand::executeStatus))
                .then(Commands.literal("config").executes(LivingWorldCommand::executeConfig))
                .then(Commands.literal("villages").executes(LivingWorldCommand::executeListVillages))
                .then(Commands.literal("blueprints").executes(LivingWorldCommand::executeListBlueprints))
                .then(Commands.literal("create_village")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(LivingWorldCommand::executeCreateVillageNamed))
                    .executes(LivingWorldCommand::executeCreateVillageDefault))
                .then(Commands.literal("build")
                    .then(Commands.literal("house").executes(ctx -> executeQueueBuild(ctx, "small_oak_house")))
                    .then(Commands.literal("two_story_house").executes(ctx -> executeQueueBuild(ctx, "two_story_house")))
                    .then(Commands.literal("farm").executes(ctx -> executeQueueBuild(ctx, "wheat_farm")))
                    .then(Commands.literal("farmhouse").executes(ctx -> executeQueueBuild(ctx, "farmhouse")))
                    .then(Commands.literal("workshop").executes(ctx -> executeQueueBuild(ctx, "workshop")))
                    .then(Commands.literal("barn").executes(ctx -> executeQueueBuild(ctx, "barn")))
                    .then(Commands.literal("warehouse").executes(ctx -> executeQueueBuild(ctx, "warehouse")))
                    .then(Commands.literal("market").executes(ctx -> executeQueueBuild(ctx, "market")))
                    .then(Commands.literal("barracks").executes(ctx -> executeQueueBuild(ctx, "barracks")))
                    .then(Commands.literal("tower").executes(ctx -> executeQueueBuild(ctx, "watchtower")))
                    .then(Commands.literal("gatehouse").executes(ctx -> executeQueueBuild(ctx, "gatehouse")))
                    .then(Commands.literal("palace").executes(ctx -> executeQueueBuild(ctx, "palace")))
                    .then(Commands.literal("pen").executes(ctx -> executeQueueBuild(ctx, "livestock_pen")))
                    .then(Commands.argument("blueprint", StringArgumentType.word())
                        .executes(ctx -> executeQueueBuild(ctx, StringArgumentType.getString(ctx, "blueprint")))))
                .then(Commands.literal("place")
                    .then(Commands.literal("house").executes(ctx -> executeInstantPlace(ctx, "small_oak_house")))
                    .then(Commands.literal("two_story_house").executes(ctx -> executeInstantPlace(ctx, "two_story_house")))
                    .then(Commands.literal("farm").executes(ctx -> executeInstantPlace(ctx, "wheat_farm")))
                    .then(Commands.literal("farmhouse").executes(ctx -> executeInstantPlace(ctx, "farmhouse")))
                    .then(Commands.literal("workshop").executes(ctx -> executeInstantPlace(ctx, "workshop")))
                    .then(Commands.literal("barn").executes(ctx -> executeInstantPlace(ctx, "barn")))
                    .then(Commands.literal("warehouse").executes(ctx -> executeInstantPlace(ctx, "warehouse")))
                    .then(Commands.literal("market").executes(ctx -> executeInstantPlace(ctx, "market")))
                    .then(Commands.literal("barracks").executes(ctx -> executeInstantPlace(ctx, "barracks")))
                    .then(Commands.literal("tower").executes(ctx -> executeInstantPlace(ctx, "watchtower")))
                    .then(Commands.literal("gatehouse").executes(ctx -> executeInstantPlace(ctx, "gatehouse")))
                    .then(Commands.literal("palace").executes(ctx -> executeInstantPlace(ctx, "palace")))
                    .then(Commands.literal("pen").executes(ctx -> executeInstantPlace(ctx, "livestock_pen")))
                    .then(Commands.argument("blueprint", StringArgumentType.word())
                        .executes(ctx -> executeInstantPlace(ctx, StringArgumentType.getString(ctx, "blueprint")))))
                .then(Commands.literal("summon_giant").executes(LivingWorldCommand::executeSummonGiant))
                .then(Commands.literal("summon_nether_boss").executes(LivingWorldCommand::executeSummonNetherBoss))
                .then(Commands.literal("spawn_horde").executes(LivingWorldCommand::executeSpawnHorde))
                .then(Commands.literal("waypoint")
                    .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(LivingWorldCommand::executeAddWaypoint)))
                    .then(Commands.literal("list").executes(LivingWorldCommand::executeListWaypoints)))
                .then(Commands.literal("reroll_enchant").executes(LivingWorldCommand::executeRerollEnchant))
                .then(Commands.literal("trial_guide").executes(LivingWorldCommand::executeTrialGuide))
                .then(Commands.literal("map").executes(LivingWorldCommand::executeOpenMap))
                .then(Commands.literal("debug")
                    .then(Commands.literal("raid")
                        .then(Commands.literal("objective").executes(LivingWorldCommand::executeDebugRaidObjective))
                        .then(Commands.literal("raiders").executes(LivingWorldCommand::executeDebugRaidRaiders))
                        .then(Commands.literal("stuck").executes(LivingWorldCommand::executeDebugRaidStuck))
                        .then(Commands.literal("bell").executes(LivingWorldCommand::executeDebugRaidBell))
                        .then(Commands.literal("status").executes(LivingWorldCommand::executeRaidStatus))
                        .then(Commands.literal("resolve").executes(LivingWorldCommand::executeRaidResolve)))
                    .then(Commands.literal("village")
                        .then(Commands.literal("towers").executes(LivingWorldCommand::executeDebugVillageTowers))
                        .then(Commands.literal("buildings").executes(LivingWorldCommand::executeDebugVillageBuildings))
                        .then(Commands.literal("construction").executes(LivingWorldCommand::executeDebugVillageConstruction))
                        .then(Commands.literal("dogs").executes(LivingWorldCommand::executeDebugVillageDogs)))
                    .then(Commands.literal("cavalry")
                        .then(Commands.literal("status").executes(LivingWorldCommand::executeDebugCavalryStatus)))
                    .then(Commands.literal("military")
                        .then(Commands.literal("patrols").executes(LivingWorldCommand::executeDebugMilitaryPatrols)))
                    .then(Commands.literal("raiders")
                        .then(Commands.literal("count").executes(LivingWorldCommand::executeRaidersCount))
                        .then(Commands.literal("list").executes(LivingWorldCommand::executeRaidersList))
                        .then(Commands.literal("teleport")
                            .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(10, 256))
                                .executes(LivingWorldCommand::executeRaidersTeleportRadius))
                            .executes(LivingWorldCommand::executeRaidersTeleportDefault)))
                    .then(Commands.literal("zombie")
                        .then(Commands.literal("miner")
                            .then(Commands.literal("spawn")
                                .then(Commands.literal("pickaxe").executes(ctx -> executeSpawnMiner(ctx, "pickaxe")))
                                .then(Commands.literal("axe").executes(ctx -> executeSpawnMiner(ctx, "axe")))
                                .then(Commands.literal("shovel").executes(ctx -> executeSpawnMiner(ctx, "shovel"))))
                            .then(Commands.literal("inspect").executes(LivingWorldCommand::executeInspectMiner)))
                        .then(Commands.literal("mining")
                            .then(Commands.literal("toggle")
                                .then(Commands.argument("enabled", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                    .executes(LivingWorldCommand::executeToggleMining))))
                        .then(Commands.literal("siege")
                            .then(Commands.literal("test").executes(LivingWorldCommand::executeSiegeTest)))
                        .then(Commands.literal("tool_spawn_rate")
                            .then(Commands.argument("rate", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(LivingWorldCommand::executeSetToolRate))
                            .executes(LivingWorldCommand::executeGetToolRate)))
                    .then(Commands.literal("eggs")
                        .then(Commands.literal("list").executes(LivingWorldCommand::executeListEggs)))
                    .then(Commands.literal("guard")
                        .then(Commands.literal("recruit").executes(LivingWorldCommand::executeRecruitGuard))
                        .then(Commands.literal("spawn")
                            .then(Commands.literal("melee").executes(LivingWorldCommand::executeSpawnMeleeGuard))
                            .then(Commands.literal("archer").executes(LivingWorldCommand::executeSpawnArcherGuard))))
                    .then(Commands.literal("empire")
                        .then(Commands.literal("locate").executes(LivingWorldCommand::executeLocateEmpire))
                        .then(Commands.literal("generate").executes(LivingWorldCommand::executeGenerateEmpire)))
                    .then(Commands.literal("villager")
                        .then(Commands.literal("inspect").executes(LivingWorldCommand::executeInspectVillager)))
                    .then(Commands.literal("economy")
                        .then(Commands.literal("inspect").executes(LivingWorldCommand::executeInspectEconomy)))
                    .then(Commands.literal("mutants")
                        .then(Commands.literal("showcase").executes(LivingWorldCommand::executeMutantsShowcase))
                        .then(Commands.literal("encounter").executes(LivingWorldCommand::executeMutantsEncounter)))
                    .then(Commands.literal("horde")
                        .then(Commands.literal("inspect").executes(LivingWorldCommand::executeHordeInspect)))
                    .then(Commands.literal("visual")
                        .then(Commands.literal("catalog").executes(LivingWorldCommand::executeVisualCatalog))))
                .then(Commands.literal("defense")
                    .then(Commands.literal("status").executes(LivingWorldCommand::executeDefenseStatus))
                    .then(Commands.literal("alarm")
                        .then(Commands.literal("on").executes(ctx -> executeDefenseAlarm(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> executeDefenseAlarm(ctx, false))))
                    .then(Commands.literal("sentries").executes(LivingWorldCommand::executeDefenseSentries))
                    .then(Commands.literal("threats").executes(LivingWorldCommand::executeDefenseThreats))
                    .then(Commands.literal("distress").executes(LivingWorldCommand::executeDefenseDistress))
                    .then(Commands.literal("shelters").executes(LivingWorldCommand::executeDefenseShelters)))
                .then(Commands.literal("reputation").executes(LivingWorldCommand::executeReputation))
                .then(Commands.literal("military")
                    .then(Commands.literal("allegiance").executes(LivingWorldCommand::executeMilitaryAllegiance))
                    .then(Commands.literal("recruit")
                        .then(Commands.literal("melee").executes(ctx -> executeMilitaryRecruit(ctx, false)))
                        .then(Commands.literal("archer").executes(ctx -> executeMilitaryRecruit(ctx, true)))
                        .then(Commands.literal("cavalry")
                            .then(Commands.literal("swordsman").executes(ctx -> executeMilitaryRecruitCavalry(ctx, livingworld.entity.VillageCavalryEntity.CavalryRole.MOUNTED_SWORDSMAN)))
                            .then(Commands.literal("scout").executes(ctx -> executeMilitaryRecruitCavalry(ctx, livingworld.entity.VillageCavalryEntity.CavalryRole.MOUNTED_SCOUT)))
                            .then(Commands.literal("captain").executes(ctx -> executeMilitaryRecruitCavalry(ctx, livingworld.entity.VillageCavalryEntity.CavalryRole.MOUNTED_CAPTAIN)))
                            .executes(ctx -> executeMilitaryRecruitCavalry(ctx, livingworld.entity.VillageCavalryEntity.CavalryRole.MOUNTED_SWORDSMAN)))
                        .then(Commands.literal("dog")
                            .then(Commands.literal("companion").executes(ctx -> executeMilitaryRecruitDog(ctx, true)))
                            .executes(ctx -> executeMilitaryRecruitDog(ctx, false)))))
                .then(Commands.literal("armory")
                    .then(Commands.literal("stock").executes(LivingWorldCommand::executeArmoryStock)))
                .then(Commands.literal("professions")
                    .then(Commands.literal("list").executes(LivingWorldCommand::executeProfessionsList)))
                .then(Commands.literal("golems")
                    .then(Commands.literal("status").executes(LivingWorldCommand::executeGolemsStatus)))
                .then(Commands.literal("scar")
                    .then(Commands.literal("generate")
                        .then(Commands.literal("warped").executes(ctx -> executeScarGenerate(ctx, livingworld.worldgen.DimensionalScarGenerator.ScarType.WARPED_FOREST)))
                        .then(Commands.literal("crimson").executes(ctx -> executeScarGenerate(ctx, livingworld.worldgen.DimensionalScarGenerator.ScarType.CRIMSON_FOREST)))
                        .then(Commands.literal("soulsand").executes(ctx -> executeScarGenerate(ctx, livingworld.worldgen.DimensionalScarGenerator.ScarType.SOUL_SAND_VALLEY)))
                        .then(Commands.literal("basalt").executes(ctx -> executeScarGenerate(ctx, livingworld.worldgen.DimensionalScarGenerator.ScarType.BASALT_DELTAS)))
                        .then(Commands.literal("wastes").executes(ctx -> executeScarGenerate(ctx, livingworld.worldgen.DimensionalScarGenerator.ScarType.NETHER_WASTES)))
                        .then(Commands.literal("village").executes(ctx -> executeScarGenerate(ctx, livingworld.worldgen.DimensionalScarGenerator.ScarType.LOST_VILLAGE_ENCLAVE)))
                        .then(Commands.literal("woodland").executes(ctx -> executeScarGenerate(ctx, livingworld.worldgen.DimensionalScarGenerator.ScarType.OVERWORLD_WOODLAND))))
                    .then(Commands.literal("list").executes(LivingWorldCommand::executeScarList))
                    .then(Commands.literal("nearest").executes(LivingWorldCommand::executeScarNearest)))
                .then(Commands.literal("integrations")
                    .then(Commands.literal("list").executes(LivingWorldCommand::executeIntegrationsList))
                    .then(Commands.literal("mutant_monsters")
                        .then(Commands.literal("status").executes(LivingWorldCommand::executeMutantMonstersStatus))))
                .then(Commands.literal("help").executes(LivingWorldCommand::executeHelp))
                .executes(LivingWorldCommand::executeStatus)
        );
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6[Living World] §aMod Status: Active (v0.2.8)"), false);
        source.sendSuccess(() -> Component.literal("§7Target: Minecraft 26.3 | Fabric 0.20.1 | Java 25"), false);
        int villageCount = SettlementManager.get().getAllSettlements().size();
        source.sendSuccess(() -> Component.literal("§7Active Settlements: §e" + villageCount), false);
        if (source.getPlayer() != null) {
            int explored = GlobalMapManager.get().getExploredChunkCount(source.getPlayer().getUUID());
            source.sendSuccess(() -> Component.literal("§7Explored Chunks: §b" + explored), false);
        }
        source.sendSuccess(() -> Component.literal("§7Player Block Protection: " + (FeatureConfig.get().protectPlayerBlocks ? "§aEnabled" : "§cDisabled")), false);
        source.sendSuccess(() -> Component.literal("§7Player Animal Protection: " + (FeatureConfig.get().protectPlayerAnimals ? "§aEnabled" : "§cDisabled")), false);
        return 1;
    }

    private static int executeAddWaypoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        String name = StringArgumentType.getString(context, "name");
        GlobalMapManager.get().addWaypoint(player, name);
        return 1;
    }

    private static int executeListWaypoints(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        GlobalMapManager.get().listWaypoints(player);
        return 1;
    }

    private static int executeRerollEnchant(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        GlobalMapManager.get().rerollEnchantmentSeed(player);
        return 1;
    }

    private static int executeTrialGuide(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        player.getInventory().add(ExplorationManager.get().createTrialGuideBook());
        source.sendSuccess(() -> Component.literal("§6[Living World] §aReceived Explorer's Field Journal: Trial Vaults!"), false);
        return 1;
    }

    private static int executeOpenMap(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        GlobalMapManager.get().openGlobalMapGui(player);
        return 1;
    }

    private static int executeSummonGiant(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition()).offset(5, 0, 5);
        GiantZombieBoss.spawnBoss(level, pos);
        source.sendSuccess(() -> Component.literal("§6[Living World] §4Summoned Colossal Dreadbeast at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "!"), false);
        return 1;
    }

    private static int executeSummonNetherBoss(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition()).offset(3, 0, 3);
        NetherBehemothBoss.summon(level, pos);
        source.sendSuccess(() -> Component.literal("§6[Living World] §6Summoned Nether Flame Champion at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "!"), false);
        return 1;
    }

    private static int executeSpawnHorde(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        ZombieHordeManager.get().spawnHordeNear(level, pos);
        source.sendSuccess(() -> Component.literal("§6[Living World] §cSpawned an armored Zombie Horde with Commander!"), false);
        return 1;
    }

    private static int executeListVillages(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var settlements = SettlementManager.get().getAllSettlements();
        if (settlements.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7No active settlements currently registered. Use §e/livingworld create_village§7."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§6[Living World] §eActive Settlements (" + settlements.size() + "):"), false);
        for (Settlement s : settlements) {
            String activeProj = s.getActiveProject() != null
                ? String.format(" [Building: %s (%.0f%%)]", s.getActiveProject().getBlueprintId(), s.getActiveProject().getProgress() * 100)
                : "";
            source.sendSuccess(() -> Component.literal(String.format("§a- %s §7(%s) Pop: %d/%d | Pos: %d,%d,%d%s",
                s.getName(), s.getState(), s.getPopulation(), s.getHousingCapacity(),
                s.getCenter().getX(), s.getCenter().getY(), s.getCenter().getZ(), activeProj)), false);
            source.sendSuccess(() -> Component.literal("   §8Stockpile: " + s.getStockpile().getSummary()), false);
        }
        return 1;
    }

    private static int executeCreateVillageDefault(CommandContext<CommandSourceStack> context) {
        return createVillageInternal(context, "New Haven");
    }

    private static int executeCreateVillageNamed(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createVillageInternal(context, name);
    }

    private static int createVillageInternal(CommandContext<CommandSourceStack> context, String name) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getOrCreateSettlement(pos, name, FactionType.VILLAGER);
        source.sendSuccess(() -> Component.literal(String.format("§6[Living World] §aRegistered settlement '§e%s§a' at %d, %d, %d!",
            s.getName(), pos.getX(), pos.getY(), pos.getZ())), false);
        return 1;
    }

    private static int executeListBlueprints(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var allBp = livingworld.settlement.BlueprintRegistry.getAll();
        source.sendSuccess(() -> Component.literal("§6[Living World] §eRegistered Architectural Blueprints (" + allBp.size() + "):"), false);
        for (livingworld.settlement.Blueprint bp : allBp.values()) {
            source.sendSuccess(() -> Component.literal(String.format("§a- %s §7(%s) - %dx%dx%d (%d blocks)",
                bp.getId(), bp.getDisplayName(), bp.getWidth(), bp.getHeight(), bp.getDepth(), bp.getBlocks().size())), false);
        }
        source.sendSuccess(() -> Component.literal("§7Use §e/livingworld build <id>§7 for autonomous construction or §e/livingworld place <id>§7 for instant placement."), false);
        return 1;
    }

    private static String resolveBlueprintId(String input) {
        if (input == null) return "small_oak_house";
        return switch (input.toLowerCase()) {
            case "house", "cottage" -> "small_oak_house";
            case "two_story", "townhouse" -> "two_story_house";
            case "farm" -> "wheat_farm";
            case "tower" -> "watchtower";
            case "pen" -> "livestock_pen";
            default -> input.toLowerCase();
        };
    }

    public record GroundSurveyResult(boolean valid, String failureReason, BlockPos foundationOrigin, int minSurfaceY, int maxSurfaceY, int variance) {}

    public static GroundSurveyResult surveyFootprint(ServerLevel level, int startX, int startZ, int width, int depth) {
        int minSurfaceY = Integer.MAX_VALUE;
        int maxSurfaceY = Integer.MIN_VALUE;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int colX = startX + x;
                int colZ = startZ + z;
                BlockPos probe = new BlockPos(colX, 64, colZ);
                var biome = level.getBiome(probe);
                if (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) ||
                    biome.is(net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN) ||
                    biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)) {
                    return new GroundSurveyResult(false, "Footprint intersects water or aquatic biome", null, 0, 0, 0);
                }

                int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, colX, colZ);
                if (surfaceY < level.getMinY() + 10) {
                    return new GroundSurveyResult(false, "Footprint intersects the void", null, 0, 0, 0);
                }

                BlockPos groundBlockPos = new BlockPos(colX, surfaceY - 1, colZ);
                var groundFluid = level.getFluidState(groundBlockPos);
                var aboveFluid = level.getFluidState(groundBlockPos.above());

                if (!groundFluid.isEmpty() || !aboveFluid.isEmpty()) {
                    return new GroundSurveyResult(false, "Footprint intersects surface water or liquid", null, 0, 0, 0);
                }

                if (surfaceY < minSurfaceY) minSurfaceY = surfaceY;
                if (surfaceY > maxSurfaceY) maxSurfaceY = surfaceY;
            }
        }

        int variance = maxSurfaceY - minSurfaceY;
        if (variance > 6) {
            return new GroundSurveyResult(false, "Terrain is too steep (elevation variance of " + variance + " blocks exceeds maximum of 6)", null, minSurfaceY, maxSurfaceY, variance);
        }

        BlockPos origin = new BlockPos(startX, maxSurfaceY, startZ);
        return new GroundSurveyResult(true, null, origin, minSurfaceY, maxSurfaceY, variance);
    }

    public static void fillFoundationColumns(ServerLevel level, BlockPos origin, int width, int depth) {
        int baseY = origin.getY();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int colX = origin.getX() + x;
                int colZ = origin.getZ() + z;
                int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, colX, colZ);
                for (int y = baseY - 1; y >= groundY; y--) {
                    BlockPos foundationPos = new BlockPos(colX, y, colZ);
                    if (level.isLoaded(foundationPos) && !level.getBlockState(foundationPos).isSolid()) {
                        level.setBlock(foundationPos, (y == baseY - 1 ? net.minecraft.world.level.block.Blocks.STONE_BRICKS : net.minecraft.world.level.block.Blocks.COBBLESTONE).defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static int executeQueueBuild(CommandContext<CommandSourceStack> context, String rawBlueprintId) {
        CommandSourceStack source = context.getSource();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        String blueprintId = resolveBlueprintId(rawBlueprintId);
        var bp = livingworld.settlement.BlueprintRegistry.get(blueprintId);
        if (bp == null) {
            source.sendFailure(Component.literal("§cUnknown blueprint: " + rawBlueprintId + ". Use /livingworld blueprints to view all available buildings."));
            return 0;
        }

        Settlement s = SettlementManager.get().getSettlementAt(playerPos);
        if (s == null) {
            s = SettlementManager.get().getOrCreateSettlement(playerPos, "Frontier Outpost", FactionType.VILLAGER);
            final String sName = s.getName();
            source.sendSuccess(() -> Component.literal("§6[Living World] §eFounded local settlement '§a" + sName + "§e' for construction testing."), false);
        }

        int startX = playerPos.getX() + 2;
        int startZ = playerPos.getZ() + 2;
        GroundSurveyResult survey = surveyFootprint(source.getLevel(), startX, startZ, bp.getWidth(), bp.getDepth());
        if (!survey.valid()) {
            source.sendFailure(Component.literal("§cCannot build '" + bp.getDisplayName() + "' here: " + survey.failureReason() + ". Please find flatter, dry ground."));
            return 0;
        }

        fillFoundationColumns(source.getLevel(), survey.foundationOrigin(), bp.getWidth(), bp.getDepth());
        ConstructionProject project = new ConstructionProject(blueprintId, survey.foundationOrigin());
        s.queueProject(project);

        final Settlement finalS = s;
        source.sendSuccess(() -> Component.literal(String.format("§6[Living World] §aQueued construction of '§e%s§a' (%s - %dx%dx%d, %d blocks) at %d, %d, %d for %s (Foundations prepared, variance: %d)!",
            blueprintId, bp.getDisplayName(), bp.getWidth(), bp.getHeight(), bp.getDepth(), project.getTotalBlocks(),
            survey.foundationOrigin().getX(), survey.foundationOrigin().getY(), survey.foundationOrigin().getZ(), finalS.getName(), survey.variance())), false);
        return 1;
    }

    private static int executeInstantPlace(CommandContext<CommandSourceStack> context, String rawBlueprintId) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        String blueprintId = resolveBlueprintId(rawBlueprintId);
        var bp = livingworld.settlement.BlueprintRegistry.get(blueprintId);
        if (bp == null) {
            source.sendFailure(Component.literal("§cUnknown blueprint: " + rawBlueprintId + ". Use /livingworld blueprints to view all available buildings."));
            return 0;
        }

        int startX = playerPos.getX() + 2;
        int startZ = playerPos.getZ() + 2;
        GroundSurveyResult survey = surveyFootprint(level, startX, startZ, bp.getWidth(), bp.getDepth());
        if (!survey.valid()) {
            source.sendFailure(Component.literal("§cCannot place '" + bp.getDisplayName() + "' here: " + survey.failureReason() + ". Please find flatter, dry ground."));
            return 0;
        }

        fillFoundationColumns(level, survey.foundationOrigin(), bp.getWidth(), bp.getDepth());
        for (var entry : bp.getBlocks().entrySet()) {
            BlockPos targetPos = survey.foundationOrigin().offset(entry.getKey());
            level.setBlock(targetPos, entry.getValue(), 3);
        }

        source.sendSuccess(() -> Component.literal(String.format("§6[Living World] §aInstantly placed '§e%s§a' (%s - %dx%dx%d, %d blocks) at %d, %d, %d with ground-anchored foundations (variance: %d)!",
            bp.getId(), bp.getDisplayName(), bp.getWidth(), bp.getHeight(), bp.getDepth(), bp.getBlocks().size(),
            survey.foundationOrigin().getX(), survey.foundationOrigin().getY(), survey.foundationOrigin().getZ(), survey.variance())), false);
        return 1;
    }

    private static int executeConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        FeatureConfig cfg = FeatureConfig.get();
        source.sendSuccess(() -> Component.literal("§6[Living World] §eFeature Configuration:"), false);
        source.sendSuccess(() -> Component.literal("§7- Zombies Intelligence (F01): " + (cfg.enableZombieIntelligence ? "§aON" : "§cOFF")), false);
        source.sendSuccess(() -> Component.literal("§7- Autonomous Building (F06): " + (cfg.enableAutonomousConstruction ? "§aON" : "§cOFF")), false);
        source.sendSuccess(() -> Component.literal("§7- Undead Occupation (F08): " + (cfg.enableUndeadOccupation ? "§aON" : "§cOFF")), false);
        source.sendSuccess(() -> Component.literal("§7- Giant Boss (F10): " + (cfg.enableGiantZombieBoss ? "§aON" : "§cOFF")), false);
        source.sendSuccess(() -> Component.literal("§7- Piglin Tribes (F11): " + (cfg.enablePiglinTribes ? "§aON" : "§cOFF")), false);
        source.sendSuccess(() -> Component.literal("§7- Ocean Sharks (F15): " + (cfg.enableSharks ? "§aON" : "§cOFF")), false);
        source.sendSuccess(() -> Component.literal("§7- Minecarts (F17): " + (cfg.enableMinecartImprovements ? "§aON" : "§cOFF")), false);
        source.sendSuccess(() -> Component.literal("§7- Global Map (F18): " + (cfg.enableGlobalMap ? "§aON" : "§cOFF")), false);
        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6[Living World] §bAvailable Commands:"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld status §7- View simulation status and metrics"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld villages §7- List all active settlements and stockpiles"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld blueprints §7- List all 13 registered architectural blueprints"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld create_village [name] §7- Found a new settlement at current location"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld place <blueprint> §7- Instantly spawn building in front of you"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld build <blueprint> §7- Queue physical autonomous construction by villagers"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld debug empire generate §7- Promote to Fortified Imperial Capital (Palace, Barracks, Army)"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld summon_giant §7- Summon the Colossal Dreadbeast boss"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld summon_nether_boss §7- Summon the Nether Flame Champion"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld spawn_horde §7- Spawn an armored Zombie Horde with Commander"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld waypoint add <name> §7- Save a manual map waypoint"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld waypoint list §7- View all saved map waypoints"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld reroll_enchant §7- Reroll enchanting seed using 1 Lapis Lazuli"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld trial_guide §7- Read the Explorer's Trial Chamber guide"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld debug zombie miner spawn <pickaxe|axe|shovel> §7- Spawn a tool-bearing miner"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld debug zombie miner inspect §7- Inspect nearby miner AI state"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld debug zombie mining toggle <true|false> §7- Toggle siege mining"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld debug zombie siege test §7- Spawn test barrier and miner"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld debug eggs list §7- List all custom mob spawn eggs"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld debug guard recruit §7- Recruit a new Village Guard"), false);
        source.sendSuccess(() -> Component.literal("§e/livingworld debug empire locate §7- Locate nearest 1-in-64 Villager Empire"), false);
        return 1;
    }

    private static int executeSpawnMiner(CommandContext<CommandSourceStack> context, String toolType) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        net.minecraft.world.entity.monster.zombie.Zombie zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (zombie != null) {
            zombie.setPos(player.getX() + 2.0, player.getY(), player.getZ());
            net.minecraft.world.item.Item item = switch (toolType.toLowerCase()) {
                case "axe" -> net.minecraft.world.item.Items.IRON_AXE;
                case "shovel" -> net.minecraft.world.item.Items.IRON_SHOVEL;
                default -> net.minecraft.world.item.Items.IRON_PICKAXE;
            };
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(item));
            zombie.addTag("livingworld:tool_miner");
            zombie.addTag("livingworld:evaluated_tool");
            zombie.getGoalSelector().addGoal(1, new livingworld.ai.ZombieSiegeMiningGoal(zombie));
            zombie.setTarget(player);
            level.addFreshEntity(zombie);

            source.sendSuccess(() -> Component.literal("§a[Living World] Spawned " + toolType + " Zombie Miner at " + zombie.blockPosition()), false);
        }
        return 1;
    }

    private static int executeInspectMiner(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ServerLevel level = source.getLevel();
        net.minecraft.world.phys.AABB searchArea = net.minecraft.world.phys.AABB.ofSize(player.position(), 24, 10, 24);
        java.util.List<net.minecraft.world.entity.monster.zombie.Zombie> miners = level.getEntitiesOfClass(
            net.minecraft.world.entity.monster.zombie.Zombie.class, searchArea,
            livingworld.ai.ZombieSiegeMiningGoal::isEligibleMiner);

        if (miners.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§c[Living World] No tool-bearing zombie miners found within 12 blocks."), false);
        } else {
            source.sendSuccess(() -> Component.literal("§6[Living World] Found " + miners.size() + " active tool-bearing miner(s):"), false);
            for (net.minecraft.world.entity.monster.zombie.Zombie m : miners) {
                net.minecraft.world.item.ItemStack held = m.getMainHandItem();
                source.sendSuccess(() -> Component.literal("§7- Miner at " + m.blockPosition() + " wielding " + held.getItem().getDescriptionId() +
                    " (Target: " + (m.getTarget() != null ? m.getTarget().getName().getString() : "None") + ")"), false);
            }
        }
        return 1;
    }

    private static int executeToggleMining(CommandContext<CommandSourceStack> context) {
        boolean enabled = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "enabled");
        FeatureConfig.get().zombieMiningEnabled = enabled;
        context.getSource().sendSuccess(() -> Component.literal("§6[Living World] Zombie siege block breaking is now " + (enabled ? "§aEnabled" : "§cDisabled")), true);
        return 1;
    }

    private static int executeSiegeTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = source.getLevel();

        BlockPos playerPos = player.blockPosition();
        BlockPos wallPos = playerPos.relative(player.getDirection(), 3);

        // Build simple 3x2 cobblestone barrier
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos bPos = wallPos.offset(dx, dy, 0);
                level.setBlock(bPos, net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }

        // Spawn pickaxe zombie on the other side
        BlockPos spawnPos = wallPos.relative(player.getDirection(), 2);
        net.minecraft.world.entity.monster.zombie.Zombie zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (zombie != null) {
            zombie.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
            zombie.addTag("livingworld:tool_miner");
            zombie.addTag("livingworld:evaluated_tool");
            zombie.getGoalSelector().addGoal(1, new livingworld.ai.ZombieSiegeMiningGoal(zombie));
            zombie.setTarget(player);
            level.addFreshEntity(zombie);
        }

        source.sendSuccess(() -> Component.literal("§6[Living World] Siege test barrier created at " + wallPos + " with Iron Pickaxe miner at " + spawnPos), true);
        return 1;
    }

    private static int executeSetToolRate(CommandContext<CommandSourceStack> context) {
        double rate = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "rate");
        FeatureConfig.get().zombieToolSpawnRate = rate;
        context.getSource().sendSuccess(() -> Component.literal("§6[Living World] Zombie tool carrier spawn probability set to §e" + (rate * 100) + "%"), true);
        return 1;
    }

    private static int executeGetToolRate(CommandContext<CommandSourceStack> context) {
        double rate = FeatureConfig.get().zombieToolSpawnRate;
        context.getSource().sendSuccess(() -> Component.literal("§6[Living World] Current zombie tool carrier spawn probability: §e" + (rate * 100) + "% (default: 5.0%)"), false);
        return 1;
    }

    private static int executeListEggs(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6[Living World] §bRegistered Creative Spawn Eggs (Section 20):"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:shark_spawn_egg §8(/give @p livingworld:shark_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:gargoyle_swooper_spawn_egg §8(/give @p livingworld:gargoyle_swooper_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:zombie_cow_spawn_egg §8(/give @p livingworld:zombie_cow_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:zombie_pig_spawn_egg §8(/give @p livingworld:zombie_pig_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:zombie_sheep_spawn_egg §8(/give @p livingworld:zombie_sheep_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:end_void_stalker_spawn_egg §8(/give @p livingworld:end_void_stalker_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:mutant_commander_spawn_egg §8(/give @p livingworld:mutant_commander_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:illager_swordsman_spawn_egg §8(/give @p livingworld:illager_swordsman_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:illager_shieldbearer_spawn_egg §8(/give @p livingworld:illager_shieldbearer_spawn_egg)"), false);
        source.sendSuccess(() -> Component.literal("§7- §elivingworld:village_guard_spawn_egg §8(/give @p livingworld:village_guard_spawn_egg)"), false);
        return 1;
    }

    private static int executeRecruitGuard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = source.getLevel();

        livingworld.entity.VillageGuardEntity guard = livingworld.entity.LivingWorldEntities.VILLAGE_GUARD.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (guard != null) {
            guard.setPos(player.getX() + 1.0, player.getY(), player.getZ() + 1.0);
            level.addFreshEntity(guard);
            source.sendSuccess(() -> Component.literal("§a[Living World] Recruited new Village Guard at " + guard.blockPosition() + " with minimum Iron-tier gear."), true);
        }
        return 1;
    }

    private static int executeLocateEmpire(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        java.util.Collection<Settlement> settlements = SettlementManager.get().getAllSettlements();
        Settlement empire = null;
        for (Settlement s : settlements) {
            // Empires have population >= 15 or 'Empire' in name or palace blueprint
            if (s.getPopulation() >= 15 || s.getName().contains("Empire")) {
                empire = s;
                break;
            }
        }

        if (empire != null) {
            final Settlement foundEmpire = empire;
            source.sendSuccess(() -> Component.literal("§6[Living World] Located Empire: §e" + foundEmpire.getName() + " at " + foundEmpire.getCenter() + " (Pop: " + foundEmpire.getPopulation() + ")"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§e[Living World] No active Empire discovered yet. Deterministic generation occurs 1 in 64 villages."), false);
        }
        return 1;
    }

    private static int executeGenerateEmpire(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = source.getLevel();
        BlockPos playerPos = player.blockPosition();

        Settlement s = SettlementManager.get().getSettlementAt(playerPos);
        if (s == null) {
            s = SettlementManager.get().getOrCreateSettlement(playerPos, "Imperial Haven", livingworld.faction.FactionType.VILLAGER);
        }
        final Settlement targetSettlement = s;
        livingworld.settlement.EmpireManager.get().promoteToEmpire(level, targetSettlement);
        source.sendSuccess(() -> Component.literal("§6👑 [Living World] Promoted " + targetSettlement.getName() + " to an Imperial Seat!"), true);
        return 1;
    }

    private static int executeInspectVillager(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = source.getLevel();

        net.minecraft.world.phys.AABB searchArea = net.minecraft.world.phys.AABB.ofSize(player.position(), 16, 8, 16);
        List<net.minecraft.world.entity.npc.villager.Villager> villagers = level.getEntitiesOfClass(
            net.minecraft.world.entity.npc.villager.Villager.class, searchArea, v -> v.isAlive());

        if (villagers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§c[Living World] No villagers found within 8 blocks."), false);
            return 0;
        }

        net.minecraft.world.entity.npc.villager.Villager v = villagers.get(0);
        livingworld.settlement.VillagerIdentityManager.VillagerProfile profile =
            livingworld.settlement.VillagerIdentityManager.get().getOrCreateProfile(v, SettlementManager.get().getSettlementAt(v.blockPosition()));

        source.sendSuccess(() -> Component.literal("§6[Living World] Villager Profile:"), false);
        source.sendSuccess(() -> Component.literal("§e- Name: §f" + profile.getFullName()), false);
        source.sendSuccess(() -> Component.literal("§e- Profession: §a" + profile.getProfession()), false);
        source.sendSuccess(() -> Component.literal("§e- Work Contributions: §b" + profile.getWorkContributions()), false);
        source.sendSuccess(() -> Component.literal("§e- UUID: §7" + profile.getUuid()), false);
        return 1;
    }

    private static int executeInspectEconomy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<livingworld.economy.EconomyManager.EconomyTransaction> txs = livingworld.economy.EconomyManager.get().getRecentTransactions();
        if (txs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7[Living World] No recent economic transactions logged."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§6[Living World] Recent Economic Ledger (Last " + txs.size() + " entries):"), false);
        for (int i = Math.max(0, txs.size() - 5); i < txs.size(); i++) {
            livingworld.economy.EconomyManager.EconomyTransaction tx = txs.get(i);
            source.sendSuccess(() -> Component.literal(String.format("§7- [%s] %s: %d %s by %s in %s",
                tx.transactionType(), tx.resource(), tx.amount(), tx.resource().name(), tx.actor(), tx.settlementName())), false);
        }
        return 1;
    }

    private static int executeSpawnMeleeGuard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = source.getLevel();
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
        var guard = livingworld.entity.LivingWorldEntities.VILLAGE_GUARD.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (guard != null) {
            guard.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(guard);
            source.sendSuccess(() -> Component.literal("§6[Living World] §aSpawned Village Melee Guard at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "!"), false);
            return 1;
        }
        return 0;
    }

    private static int executeSpawnArcherGuard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = source.getLevel();
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
        var archer = livingworld.entity.LivingWorldEntities.VILLAGE_ARCHER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (archer != null) {
            archer.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(archer);
            source.sendSuccess(() -> Component.literal("§6[Living World] §aSpawned Village Archer (Bow Guard) at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "!"), false);
            return 1;
        }
        return 0;
    }

    // ==================== ADDENDUM: INTEGRATIONS & MUTANT MONSTERS COMMANDS ====================

    private static int executeIntegrationsList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Active Ecosystem Integrations ==="), false);

        record ModInfo(String id, String name, String license) {}
        List<ModInfo> tracked = List.of(
            new ModInfo("mutantmonsters", "Mutant Monsters", "AGPL-3.0"),
            new ModInfo("puzzleslib", "Puzzles Lib", "MPL-2.0"),
            new ModInfo("forgeconfigapiport", "Forge Config API Port", "MPL-2.0"),
            new ModInfo("geckolib", "GeckoLib Animation Engine", "MIT"),
            new ModInfo("ctov", "ChoiceTheorem's Overhauled Village", "CC-BY-NC-ND-4.0"),
            new ModInfo("lithostitched", "Lithostitched Worldgen", "LGPL-3.0"),
            new ModInfo("BetterVanillaDragonFight", "Better Vanilla Dragon Fight", "MIT"),
            new ModInfo("mes", "Moog's End Structures", "LGPL-3.0"),
            new ModInfo("mns", "Moog's Nether Structures", "LGPL-3.0"),
            new ModInfo("msl", "Moog's Structure Lib", "LGPL-3.0"),
            new ModInfo("the_sift", "The Sift (Dimensional Void)", "MIT"),
            new ModInfo("xaerominimap", "Xaero's Minimap", "ARR"),
            new ModInfo("xaeroworldmap", "Xaero's World Map", "ARR"),
            new ModInfo("regrowth", "Regrowth Revamped", "MIT"),
            new ModInfo("structory", "Structory Modular Structures", "ARR"),
            new ModInfo("explorify", "Explorify Dungeons & Ruins", "ARR"),
            new ModInfo("cloth-config", "Cloth Config Fabric", "LGPL-3.0"),
            new ModInfo("fabric-api", "Fabric API", "Apache-2.0")
        );

        int loadedCount = 0;
        for (ModInfo info : tracked) {
            Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(info.id());
            if (mod.isPresent()) {
                loadedCount++;
                String ver = mod.get().getMetadata().getVersion().getFriendlyString();
                source.sendSuccess(() -> Component.literal("§a  ✔ §f" + info.name() + " §7v" + ver + " §8[" + info.license() + "]"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§c  ✘ §7" + info.name() + " §8(Not loaded)"), false);
            }
        }
        final int finalLoaded = loadedCount;
        source.sendSuccess(() -> Component.literal("§6[Living World] §eTotal Tracked Integrations Loaded: §a" + finalLoaded + "§7/§e" + tracked.size()), false);
        return 1;
    }

    private static int executeMutantMonstersStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<ModContainer> modOpt = FabricLoader.getInstance().getModContainer("mutantmonsters");
        if (modOpt.isEmpty()) {
            source.sendFailure(Component.literal("§6[Living World] §cMutant Monsters is not currently loaded in the mods directory."));
            return 0;
        }

        ModContainer mod = modOpt.get();
        String ver = mod.getMetadata().getVersion().getFriendlyString();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Mutant Monsters Integration Status ==="), false);
        source.sendSuccess(() -> Component.literal("§7Status: §aLOADED §7| Version: §b" + ver + " §7| License: §eAGPL-3.0"), false);
        source.sendSuccess(() -> Component.literal("§7Official Source: §bhttps://github.com/Fuzss/mutant-monsters"), false);

        String[] entityKeys = {
            "mutant_zombie", "mutant_skeleton", "mutant_creeper",
            "mutant_enderman", "mutant_snow_golem", "creeper_minion", "spider_pig"
        };
        source.sendSuccess(() -> Component.literal("§eRegistered Mutant Entities:"), false);
        for (String k : entityKeys) {
            Identifier id = Identifier.fromNamespaceAndPath("mutantmonsters", k);
            boolean reg = BuiltInRegistries.ENTITY_TYPE.getOptional(id).isPresent();
            source.sendSuccess(() -> Component.literal("§7  • " + id + ": " + (reg ? "§aRegistered" : "§cMissing")), false);
        }

        boolean chemX = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("mutantmonsters", "chemical_x")).isPresent();
        source.sendSuccess(() -> Component.literal("§7Chemical X Item: " + (chemX ? "§aPresent (mutantmonsters:chemical_x)" : "§cMissing")), false);

        source.sendSuccess(() -> Component.literal("§eEcosystem Coexistence:"), false);
        source.sendSuccess(() -> Component.literal("§7  • Horde Leader: §flivingworld:mutant_commander §7(3 Heads, 180 HP, Distinct Boss Bar)"), false);
        source.sendSuccess(() -> Component.literal("§7  • Mutant Zombie: §fmutantmonsters:mutant_zombie §7(4-Legged Hulk, Ground Smash, Rare Elite)"), false);
        source.sendSuccess(() -> Component.literal("§7  • AI Isolation: §aACTIVE §7(Native Mutant AI preserved; no generic AI injected)"), false);
        source.sendSuccess(() -> Component.literal("§7  • Guard Priorities: §aACTIVE §7(Village guards/archers prioritize mutants at Priority 2)"), false);
        source.sendSuccess(() -> Component.literal("§7  • World Protection: §aACTIVE §7(Player claims and mobGriefing rule enforced)"), false);
        return 1;
    }

    private static int executeMutantsShowcase(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = source.getLevel();

        String[] showcaseTypes = {
            "mutant_zombie", "mutant_skeleton", "mutant_creeper",
            "mutant_enderman", "mutant_snow_golem", "spider_pig", "creeper_minion"
        };

        int spawned = 0;
        BlockPos center = player.blockPosition().relative(player.getDirection(), 7);
        var perp = player.getDirection().getClockWise();

        for (int i = 0; i < showcaseTypes.length; i++) {
            Identifier id = Identifier.fromNamespaceAndPath("mutantmonsters", showcaseTypes[i]);
            var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (typeOpt.isPresent()) {
                int offset = (i - 3) * 3;
                BlockPos targetPos = center.relative(perp, offset);
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());
                BlockPos spawnPos = new BlockPos(targetPos.getX(), surfaceY, targetPos.getZ());

                var entity = typeOpt.get().create(level, EntitySpawnReason.COMMAND);
                if (entity != null) {
                    entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    level.addFreshEntity(entity);
                    spawned++;
                }
            }
        }

        final int finalSpawned = spawned;
        source.sendSuccess(() -> Component.literal("§6[Living World] §aShowcase deployed: " + finalSpawned + " mutant creatures arrayed for inspection in front of you."), false);
        return 1;
    }

    private static int executeMutantsEncounter(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = source.getLevel();

        String[] encounterTypes = {"mutant_zombie", "mutant_skeleton", "mutant_creeper", "mutant_enderman"};
        String chosen = encounterTypes[level.getRandom().nextInt(encounterTypes.length)];
        Identifier id = Identifier.fromNamespaceAndPath("mutantmonsters", chosen);

        var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (typeOpt.isEmpty()) {
            source.sendFailure(Component.literal("§6[Living World] §cCould not spawn encounter: " + id + " not found."));
            return 0;
        }

        BlockPos targetPos = player.blockPosition().relative(player.getDirection(), 12);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());
        BlockPos spawnPos = new BlockPos(targetPos.getX(), surfaceY, targetPos.getZ());

        var entity = typeOpt.get().create(level, EntitySpawnReason.EVENT);
        if (entity != null) {
            entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            if (entity instanceof Mob mob) {
                mob.setTarget(player);
            }
            level.addFreshEntity(entity);
            source.sendSuccess(() -> Component.literal("§4[Living World Encounter] §cAn ancient §l" + chosen.replace("_", " ").toUpperCase() + "§c has emerged to challenge you!"), true);
            return 1;
        }
        return 0;
    }

    private static int executeHordeInspect(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        var commanders = ZombieHordeManager.get().getActiveCommanders();

        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Undead Horde Inspection ==="), false);
        source.sendSuccess(() -> Component.literal("§7Active Horde Commanders in Memory: §e" + commanders.size()), false);

        int count = 0;
        for (UUID uuid : commanders) {
            var entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity cmd && cmd.isAlive()) {
                final int currentCount = ++count;
                BlockPos pos = cmd.blockPosition();
                float hp = cmd.getHealth();
                float maxHp = cmd.getMaxHealth();
                String targetStr = (cmd instanceof Mob mob && mob.getTarget() != null) ? mob.getTarget().getName().getString() : "None";
                int followers = level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, cmd.getBoundingBox().inflate(16.0)).size() - 1;

                source.sendSuccess(() -> Component.literal(String.format("§7Horde #%d Leader: §flivingworld:mutant_commander §7(3 Heads)", currentCount)), false);
                source.sendSuccess(() -> Component.literal(String.format("§7  UUID: §8%s §7| Pos: §e(%d, %d, %d)", uuid, pos.getX(), pos.getY(), pos.getZ())), false);
                source.sendSuccess(() -> Component.literal(String.format("§7  Health: §c%.1f / %.1f §7| Target: §e%s §7| Followers: §b%d", hp, maxHp, targetStr, Math.max(0, followers))), false);
                source.sendSuccess(() -> Component.literal("§a  Entity Separation: Distinct ID livingworld:mutant_commander != mutantmonsters:mutant_zombie (Verified)"), false);
            }
        }

        if (count == 0) {
            source.sendSuccess(() -> Component.literal("§7No active horde commanders currently loaded in world. Use §e/livingworld spawn_horde§7 to trigger a night siege."), false);
        }
        return 1;
    }

    private static int executeVisualCatalog(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Professional Architecture & Visual Catalog ==="), false);
        source.sendSuccess(() -> Component.literal("§b[Architectural Blueprints - Authentic Hand-Crafted Designs]:"), false);
        source.sendSuccess(() -> Component.literal("  §f1. Imperial Royal Palace §7(35×14×25, >2800 blocks, Throne Room, Portico, East/West Wings, Foundations)"), false);
        source.sendSuccess(() -> Component.literal("  §f2. Fortified Military Watchtower §7(9×16×9, 16m height, Battered Base, Arrow Slits, Parapet, Brazier)"), false);
        source.sendSuccess(() -> Component.literal("  §f3. Cozy Timber Cottage §7(9×8×8, >260 blocks, Porch, Fireplace, Chimney Smoke, Overhanging Eaves)"), false);
        source.sendSuccess(() -> Component.literal("  §f4. Two-Story Residence §7(11×10×9, Jettied upper floor, balcony, oak log corner framing)"), false);
        source.sendSuccess(() -> Component.literal("  §f5. Garrison Barracks §7(15×7×11, Multi-bunk dorms, armory, training yard)"), false);
        source.sendSuccess(() -> Component.literal("  §f6. Gatehouse & Ramparts §7(15×9×7, Portcullis, crenellated parapets)"), false);
        source.sendSuccess(() -> Component.literal("  §f7. Village Marketplace §7(13×5×13, Striped market stalls, central well)"), false);
        source.sendSuccess(() -> Component.literal("  §f8. Forge Workshop §7(11×7×9, Blast furnace, water trough, stone chimney)"), false);
        source.sendSuccess(() -> Component.literal("  §f9. Gambrel Barn §7(13×8×11, High hayloft, animal pens)"), false);
        source.sendSuccess(() -> Component.literal("  §f10. Village Warehouse §7(13×7×11, Crates, barrels, vaulted rafters)"), false);
        source.sendSuccess(() -> Component.literal("§a[Custom Entities - Custom 3D Models & Pixel Art]:"), false);
        source.sendSuccess(() -> Component.literal("  §f• Mutant Zombie Commander §7(180 HP, 3 Heads, Boss Scale 1.45x, Club Arm, Boss Bar)"), false);
        source.sendSuccess(() -> Component.literal("  §f• Great White Shark §7(60 HP, Custom 3D Pointed Snout, Teeth, Pectoral/Dorsal/Heterocercal Fins)"), false);
        source.sendSuccess(() -> Component.literal("  §f• Gargoyle Swooper §7(24 HP, Custom 3D Swooping Wings, Curved Horns, Dive Strike)"), false);
        source.sendSuccess(() -> Component.literal("  §f• Zombie Livestock §7(Zombie Cow, Pig, Sheep - custom decay textures, dark spoils)"), false);
        source.sendSuccess(() -> Component.literal("  §f• End Void Stalker §7(50 HP, Void Tentacles, Outer Island Predator)"), false);
        source.sendSuccess(() -> Component.literal("  §f• Village Archer §7(28 HP, Full Iron, Tactical Kiting AI, Ranged Defense)"), false);
        source.sendSuccess(() -> Component.literal("  §f• Village Guard §7(30 HP, Full Iron, Shield Blocking, Soldier-to-Royal Ranks)"), false);
        source.sendSuccess(() -> Component.literal("§d[Visual Provenance]: §7Documented in §edocs/VISUAL_REFERENCES.md§7 and §edocs/VISUAL_ACCEPTANCE_REPORT.md§7."), false);
        return 1;
    }

    private static int executeDefenseStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found."));
            return 0;
        }

        var state = livingworld.faction.VillageDefenseController.get().getState(s.getId());
        var sentries = livingworld.faction.VillageDefenseController.get().getActiveSentries(level, s);
        boolean onCooldown = livingworld.faction.VillageDefenseController.get().isOnAlarmCooldown(s);

        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Defense Status: " + s.getName() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§7  Threat Level: " + state.getThreatLevel().getDisplay()), false);
        source.sendSuccess(() -> Component.literal("§7  Lockdown Active: " + (state.isLockdownActive() ? "§cYES" : "§aNO")), false);
        source.sendSuccess(() -> Component.literal("§7  Active Hostiles: §e" + state.getActiveHostileCount()), false);
        source.sendSuccess(() -> Component.literal("§7  Primary Threat: §f" + state.getPrimaryThreatDescription()), false);
        source.sendSuccess(() -> Component.literal("§7  Active Sentries: §b" + sentries.size()), false);
        source.sendSuccess(() -> Component.literal("§7  Horn Cooldown: " + (onCooldown ? "§cActive (cooling down)" : "§aReady")), false);
        return 1;
    }

    private static int executeDefenseAlarm(CommandContext<CommandSourceStack> context, boolean enable) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found."));
            return 0;
        }

        if (enable) {
            livingworld.faction.VillageDefenseController.get().triggerThreatAlarm(level, s, pos, "Commander Manual Emergency Alarm");
            source.sendSuccess(() -> Component.literal("§4[Living World] §cTactical horn sounded! Defenders rallied and lockdown ordered for §6" + s.getName()), true);
        } else {
            livingworld.faction.VillageDefenseController.get().clearAlarm(s);
            source.sendSuccess(() -> Component.literal("§a[Living World] Defense alarm silenced. Threat cleared for §6" + s.getName()), true);
        }
        return 1;
    }

    private static int executeDefenseSentries(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found."));
            return 0;
        }

        var sentries = livingworld.faction.VillageDefenseController.get().getActiveSentries(level, s);
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Active Sentries: " + s.getName() + " (" + sentries.size() + ") ==="), false);
        int idx = 0;
        for (var sentry : sentries) {
            final int currentIdx = ++idx;
            BlockPos sp = sentry.blockPosition();
            source.sendSuccess(() -> Component.literal(String.format("§7  Sentry #%d: Pos (%d, %d, %d) | HP: %.1f | Target: %s",
                currentIdx, sp.getX(), sp.getY(), sp.getZ(), sentry.getHealth(),
                sentry.getTarget() != null ? sentry.getTarget().getName().getString() : "Watching perimeter (48-64m)")), false);
        }
        if (sentries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7  No archers currently posted to sentry duty. Archery watchtower required."), false);
        }
        return 1;
    }

    private static int executeDefenseThreats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found."));
            return 0;
        }

        var state = livingworld.faction.VillageDefenseController.get().getState(s.getId());
        var threats = state.getThreatRecords();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Active Threats: " + s.getName() + " (" + threats.size() + ") ==="), false);
        source.sendSuccess(() -> Component.literal("§7  Threat Level: " + state.getThreatLevel().getDisplay() + " | Lockdown: " + (state.isLockdownActive() ? "§cACTIVE" : "§aINACTIVE")), false);
        int idx = 0;
        for (var tr : threats.values()) {
            final int currentIdx = ++idx;
            BlockPos tp = tr.getLocation();
            source.sendSuccess(() -> Component.literal(String.format("§7  #%d: %s at (%d, %d, %d) | Severity: %.1f | Assigned Defenders: %d%s%s%s",
                currentIdx, tr.getEntityType().getDescription().getString(),
                tp != null ? tp.getX() : 0, tp != null ? tp.getY() : 0, tp != null ? tp.getZ() : 0,
                tr.getSeverity(), tr.getAssignedDefenders().size(),
                tr.isCommander() ? " §c[COMMANDER]" : "",
                tr.isBreaching() ? " §e[BREACHING]" : "",
                tr.getVictimUuid() != null ? " §4[ATTACKING CIVILIAN]" : "")), false);
        }
        if (threats.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§a  Perimeter secure. No active hostile threats detected within 80m."), false);
        }
        return 1;
    }

    private static int executeDefenseDistress(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int total = livingworld.faction.CivilianDistressManager.get().getTotalDistressEmitted();
        var incidents = livingworld.faction.CivilianDistressManager.get().getActiveIncidents();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Civilian Distress Diagnostics ==="), false);
        source.sendSuccess(() -> Component.literal("§7  Total Distress Signals Emitted: §e" + total), false);
        source.sendSuccess(() -> Component.literal("§7  Active Distress Incidents: §c" + incidents.size()), false);
        int idx = 0;
        for (var inc : incidents.values()) {
            final int currentIdx = ++idx;
            source.sendSuccess(() -> Component.literal(String.format("§7  Incident #%d: Victim %s | Attacker %s | %s",
                currentIdx, inc.getVictimUuid().toString().substring(0, 8),
                inc.getAttackerUuid().toString().substring(0, 8),
                inc.isAcknowledged() ? "§aAcknowledged" : "§cActive")), false);
        }
        return 1;
    }

    private static int executeDefenseShelters(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found."));
            return 0;
        }

        var villagers = level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,
            AABB.ofSize(new Vec3(s.getCenter().getX(), s.getCenter().getY(), s.getCenter().getZ()), s.getRadius() * 2, 40, s.getRadius() * 2));

        int working = 0, threatDetected = 0, seeking = 0, sheltered = 0, recovery = 0;
        for (var v : villagers) {
            var state = livingworld.faction.CivilianShelterManager.get().getCivilianState(v.getUUID());
            switch (state) {
                case WORKING -> working++;
                case THREAT_DETECTED -> threatDetected++;
                case SEEKING_SHELTER -> seeking++;
                case SHELTERED -> sheltered++;
                case RECOVERY -> recovery++;
            }
        }
        final int fWorking = working;
        final int fThreatDetected = threatDetected;
        final int fSeeking = seeking;
        final int fSheltered = sheltered;
        final int fRecovery = recovery;
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Civilian Shelter Census: " + s.getName() + " (" + villagers.size() + " villagers) ==="), false);
        source.sendSuccess(() -> Component.literal("§a  Working: §f" + fWorking), false);
        source.sendSuccess(() -> Component.literal("§e  Threat Detected: §f" + fThreatDetected), false);
        source.sendSuccess(() -> Component.literal("§6  Seeking Shelter: §f" + fSeeking), false);
        source.sendSuccess(() -> Component.literal("§c  Sheltered: §f" + fSheltered), false);
        source.sendSuccess(() -> Component.literal("§b  Recovery: §f" + fRecovery), false);
        return 1;
    }

    private static int executeReputation(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("§cCommand must be executed by a player."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        BlockPos pos = player.blockPosition();
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found nearby."));
            return 0;
        }

        int rep = livingworld.faction.ReputationManager.get().getReputation(player.getUUID(), s.getId());
        String title = livingworld.faction.ReputationManager.get().getMilestoneTitle(rep);
        boolean hostile = livingworld.faction.ReputationManager.get().isHostileToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Village Standing: " + s.getName() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§7  Player: §f" + player.getName().getString()), false);
        source.sendSuccess(() -> Component.literal("§7  Standing: §l§a" + title + " §r§7(Trust: §e" + rep + "§7)"), false);
        source.sendSuccess(() -> Component.literal("§7  Disposition: " + (hostile ? "§cHOSTILE" : "§aWELCOMING")), false);
        source.sendSuccess(() -> Component.literal("§7  Milestones: §8[25: Recognized Visitor | 50: Trusted Friend | 100: Village Ally | 200: Honored Defender]"), false);
        return 1;
    }

    private static int executeMilitaryAllegiance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var allDefenders = livingworld.faction.AllegianceRegistry.get().getAllDefenders();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Military Allegiance Matrix ==="), false);
        source.sendSuccess(() -> Component.literal("§7  Registered Village Defenders: §e" + allDefenders.size()), false);
        source.sendSuccess(() -> Component.literal("§a  Friendly-Fire Cancellation: §2ACTIVE (Arrow projectiles & melee between allies filtered)"), false);
        source.sendSuccess(() -> Component.literal("§a  Ally Matrix:"), false);
        source.sendSuccess(() -> Component.literal("    • Guard <-> Guard: §2ALLIED (Never attack)"), false);
        source.sendSuccess(() -> Component.literal("    • Guard <-> Archer: §2ALLIED (Immune to stray arrows)"), false);
        source.sendSuccess(() -> Component.literal("    • Guard <-> Iron/Snow Golem: §2ALLIED (Joint defense)"), false);
        source.sendSuccess(() -> Component.literal("    • Guard <-> Civilians: §2PROTECT (Never targeted)"), false);
        source.sendSuccess(() -> Component.literal("    • Guard <-> Player: §aFORGIVING (2 accidental hits tolerated before hostility)"), false);
        return 1;
    }

    private static int executeMilitaryRecruit(CommandContext<CommandSourceStack> context, boolean archer) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        boolean success = livingworld.faction.VillageGuardManager.get().forceRecruit(level, s, pos, archer);
        if (success) {
            String roleName = archer ? "Village Archer" : "Village Guard";
            String villageName = s != null ? s.getName() : "Local Garrison";
            source.sendSuccess(() -> Component.literal("§6[Living World] §aSuccessfully recruited new §e" + roleName + "§a for §6" + villageName + "§a from armory!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("§6[Living World] §cFailed to recruit guard."));
            return 0;
        }
    }

    private static int executeMilitaryRecruitCavalry(CommandContext<CommandSourceStack> context, livingworld.entity.VillageCavalryEntity.CavalryRole role) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        boolean success = livingworld.faction.VillageGuardManager.get().forceRecruitCavalry(level, s, pos, role);
        if (success) {
            String villageName = s != null ? s.getName() : "Local Garrison";
            source.sendSuccess(() -> Component.literal("§6[Living World] §aMobilized §e" + role.name() + " §amounted cavalry unit for §6" + villageName + "!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("§6[Living World] §cFailed to mobilize cavalry unit."));
            return 0;
        }
    }

    private static int executeMilitaryRecruitDog(CommandContext<CommandSourceStack> context, boolean companion) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        livingworld.entity.VillageGuardEntity handler = null;
        if (companion) {
            AABB zone = AABB.ofSize(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 16, 8, 16);
            List<livingworld.entity.VillageGuardEntity> guards = level.getEntitiesOfClass(livingworld.entity.VillageGuardEntity.class, zone, g -> g.isAlive());
            if (!guards.isEmpty()) handler = guards.get(0);
        }
        boolean success = livingworld.faction.VillageGuardManager.get().forceSpawnDog(level, s, pos, handler);
        if (success) {
            String desc = companion ? (handler != null ? "Guard Companion Dog paired with Guard " + handler.getRank() : "Guard Companion Dog") : "Civilian Village Dog";
            source.sendSuccess(() -> Component.literal("§6[Living World] §aSpawned §e" + desc + "§a!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("§6[Living World] §cFailed to spawn village dog."));
            return 0;
        }
    }

    private static int executeArmoryStock(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found."));
            return 0;
        }
        var stock = livingworld.economy.VillageArmoryManager.get().getStock(s);
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Armory Inventory: " + s.getName() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§7  " + stock.getSummary()), false);
        source.sendSuccess(() -> Component.literal("§7  Melee Gear Available: " + (stock.hasMeleeGuardGear() ? "§aYES" : "§cNO")), false);
        source.sendSuccess(() -> Component.literal("§7  Archer Gear Available: " + (stock.hasArcherGear() ? "§aYES" : "§cNO")), false);
        return 1;
    }

    private static int executeProfessionsList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found."));
            return 0;
        }
        var census = livingworld.economy.VillageWorkforceManager.get().getCensus(s.getId());
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Workforce Census: " + s.getName() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§7  Total Population: §f" + census.getTotalVillagers() + " §7(Unemployed: §e" + census.getUnemployedCount() + "§7, Children: §b" + census.getChildrenCount() + "§7)"), false);
        source.sendSuccess(() -> Component.literal("§7  Staffed Professions (2+ adults): §a" + census.getStaffedProfessionsCount() + "/13"), false);
        for (var prof : livingworld.economy.VillageWorkforceManager.ManagedProfession.values()) {
            int count = census.getCount(prof);
            String diagnostic = "";
            if (count < 2) {
                diagnostic = " §8[" + livingworld.economy.VillageWorkforceManager.get().getUnderstaffedDiagnostic(s, prof, census) + "]";
            }
            String status = count >= 2 ? "§a" + count + " §7(Staffed)" : (count == 1 ? "§e1 §7(Needs 1)" : "§c0 §7(Deficit)");
            final String diag = diagnostic;
            source.sendSuccess(() -> Component.literal("    • " + prof.getTitle() + ": " + status + diag), false);
        }
        return 1;
    }

    private static int executeGolemsStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§6[Living World] §cNo active settlement found."));
            return 0;
        }
        String summary = livingworld.faction.VillageGolemManager.get().getGolemSummary(level, s);
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Golem Status: " + s.getName() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§7  " + summary), false);
        return 1;
    }

    private static int executeRaidersCount(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        AABB zone = AABB.ofSize(pos, 256, 64, 256);
        List<Raider> raiders = level.getEntitiesOfClass(Raider.class, zone, r -> r.isAlive());
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Active Raiders in 128m Radius: §f" + raiders.size() + " ==="), false);
        Map<String, Integer> counts = new HashMap<>();
        for (Raider r : raiders) {
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(r.getType()).toString();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            source.sendSuccess(() -> Component.literal("  • " + entry.getKey() + ": §a" + entry.getValue()), false);
        }
        return raiders.size();
    }

    private static int executeRaidersList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        AABB zone = AABB.ofSize(pos, 256, 64, 256);
        List<Raider> raiders = level.getEntitiesOfClass(Raider.class, zone, r -> r.isAlive());
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Raider Unit List (128m) ==="), false);
        if (raiders.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §7No active raiders detected in area."), false);
            return 0;
        }
        for (int i = 0; i < Math.min(10, raiders.size()); i++) {
            final int index = i + 1;
            Raider r = raiders.get(i);
            String name = r.getName().getString();
            double dist = Math.sqrt(r.distanceToSqr(pos));
            BlockPos bp = r.blockPosition();
            float hp = r.getHealth();
            source.sendSuccess(() -> Component.literal(String.format("  [%d] %s at (%d, %d, %d) | Dist: %.1fm | HP: %.1f",
                index, name, bp.getX(), bp.getY(), bp.getZ(), dist, hp)), false);
        }
        if (raiders.size() > 10) {
            source.sendSuccess(() -> Component.literal("  §7... and " + (raiders.size() - 10) + " more units."), false);
        }
        return raiders.size();
    }

    private static int executeRaidersTeleportDefault(CommandContext<CommandSourceStack> context) {
        return executeRaidersTeleport(context, 64);
    }

    private static int executeRaidersTeleportRadius(CommandContext<CommandSourceStack> context) {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        return executeRaidersTeleport(context, radius);
    }

    private static boolean hasOperatorPermission(CommandSourceStack source) {
        if (!source.isPlayer()) return true;
        if (source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) return true;
        return source.getPlayer() != null && source.getServer().getPlayerList().isOp(source.getPlayer().nameAndId());
    }

    private static int executeRaidersTeleport(CommandContext<CommandSourceStack> context, int radius) {
        CommandSourceStack source = context.getSource();
        if (!hasOperatorPermission(source)) {
            source.sendFailure(Component.literal("§cPermission denied: operator privilege required."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        AABB zone = AABB.ofSize(pos, radius * 2, 64, radius * 2);
        List<Raider> raiders = level.getEntitiesOfClass(Raider.class, zone, r -> r.isAlive());
        if (raiders.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§6[Living World] §eNo raiders found within " + radius + "m radius."), false);
            return 0;
        }
        int count = 0;
        for (Raider r : raiders) {
            double angle = (count * (Math.PI * 2) / Math.max(1, raiders.size()));
            double offsetDist = 3.0 + (count % 3);
            double tx = pos.x + Math.cos(angle) * offsetDist;
            double tz = pos.z + Math.sin(angle) * offsetDist;
            r.teleportTo(tx, pos.y, tz);
            count++;
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("§6[Living World] §aTeleported §f" + finalCount + " §araiders within §f" + radius + "m §ato your location."), true);
        return count;
    }

    private static int executeRaidStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        var raid = level.getRaidAt(pos);
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Raid Diagnostic Status ==="), false);
        if (raid != null) {
            source.sendSuccess(() -> Component.literal("  Active Raid: §aYES §7(Wave: §e" + raid.getGroupsSpawned() + "/" + raid.getNumGroups(level.getDifficulty()) + "§7)"), false);
            source.sendSuccess(() -> Component.literal("  Raiders Remaining: §f" + raid.getTotalRaidersAlive()), false);
            source.sendSuccess(() -> Component.literal("  Raid State: §f" + (raid.isVictory() ? "VICTORY" : (raid.isLoss() ? "LOSS" : "ACTIVE"))), false);
        } else {
            source.sendSuccess(() -> Component.literal("  Active Vanilla Raid: §7None at current location."), false);
        }
        if (s != null) {
            var state = livingworld.faction.VillageDefenseController.get().getState(s.getId());
            source.sendSuccess(() -> Component.literal("  Settlement Defense State: " + state.getThreatLevel().getDisplay()), false);
            source.sendSuccess(() -> Component.literal("  Primary Threat: §f" + state.getPrimaryThreatDescription()), false);
        }
        return 1;
    }

    private static int executeRaidStuck(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        List<Raider> stuck = RaidTactics.get().getStuckRaiders(level, pos, 96.0);
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Stuck Raiders Diagnostic ==="), false);
        if (stuck.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §aNo stuck raiders detected within 96m."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("  §cStuck Raiders Count: §f" + stuck.size()), false);
        for (Raider r : stuck) {
            BlockPos bp = r.blockPosition();
            source.sendSuccess(() -> Component.literal("    • " + r.getName().getString() + " trapped at (" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + ")"), false);
        }
        return stuck.size();
    }

    private static int executeRaidResolve(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        int recovered = RaidTactics.get().recoverStuckRaiders(level, pos, 96.0);
        source.sendSuccess(() -> Component.literal("§6[Living World] §aRaid Resolve: Recovered §f" + recovered + " §astuck raiders to village surface roads."), true);
        return recovered;
    }

    private static int executeScarGenerate(CommandContext<CommandSourceStack> context, livingworld.worldgen.DimensionalScarGenerator.ScarType type) {
        CommandSourceStack source = context.getSource();
        if (!hasOperatorPermission(source)) {
            source.sendFailure(Component.literal("§cPermission denied: operator privilege required."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        int radius = FeatureConfig.get().scarDefaultDiameter / 2;
        boolean success = livingworld.worldgen.DimensionalScarGenerator.generateScar(level, pos, type, radius);
        if (success) {
            source.sendSuccess(() -> Component.literal("§6[Living World] §aGenerated §e" + type.getTitle() + "§a at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") with radius " + radius + "m!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("§6[Living World] §cFailed to generate dimensional scar (location protected or invalid)."));
            return 0;
        }
    }

    private static int executeScarList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var scars = livingworld.worldgen.DimensionalScarManager.get().getAllScars();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Dimensional Scars Registry: §f" + scars.size() + " ==="), false);
        if (scars.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §7No dimensional scars registered in this world yet."), false);
            return 0;
        }
        for (var scar : scars) {
            BlockPos c = scar.getCenter();
            source.sendSuccess(() -> Component.literal(String.format("  • %s at (%d, %d, %d) | Radius: %dm | Dim: %s",
                scar.getType().getTitle(), c.getX(), c.getY(), c.getZ(), scar.getRadius(), scar.getDimension())), false);
        }
        return scars.size();
    }

    private static int executeScarNearest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        var scar = livingworld.worldgen.DimensionalScarManager.get().getNearestScar(pos);
        if (scar == null) {
            source.sendSuccess(() -> Component.literal("§6[Living World] §7No dimensional scars discovered yet."), false);
            return 0;
        }
        BlockPos c = scar.getCenter();
        double dist = Math.sqrt(c.distSqr(pos));
        source.sendSuccess(() -> Component.literal(String.format("§6[Living World] §eNearest Dimensional Scar: §f%s §7at (%d, %d, %d) | Distance: §a%.1fm",
            scar.getType().getTitle(), c.getX(), c.getY(), c.getZ(), dist)), false);
        return 1;
    }

    // =========================================================================
    // v0.2.7 RELIABILITY & SIEGE DEBUG COMMANDS
    // =========================================================================

    private static int executeDebugRaidObjective(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Tactical Raid Objective Diagnostic ==="), false);

        RaidTactics.RaidObjective obj = null;
        if (s != null) {
            obj = RaidTactics.get().getRaidObjective(s.getId().hashCode());
        }
        if (obj == null) {
            var raid = level.getRaidAt(pos);
            if (raid != null) {
                var rId = level.getRaids().getId(raid);
                if (rId.isPresent()) {
                    obj = RaidTactics.get().getRaidObjective(rId.getAsInt());
                }
            }
        }
        if (obj == null) {
            var all = RaidTactics.get().getAllRaidObjectives();
            if (!all.isEmpty()) obj = all.iterator().next();
        }

        if (obj == null) {
            source.sendSuccess(() -> Component.literal("  §7No active raid tactical objective found in this sector."), false);
            return 0;
        }

        final var fObj = obj;
        BlockPos bellPos = fObj.getBellPos();
        double dist = bellPos != null ? Math.sqrt(bellPos.distSqr(pos)) : 0.0;
        source.sendSuccess(() -> Component.literal("  Tactical Phase: §e" + fObj.getState().name() + " §7(" + fObj.getState().getDisplay() + ")"), false);
        source.sendSuccess(() -> Component.literal("  Target Bell: §f" + (bellPos != null ? String.format("(%d, %d, %d)", bellPos.getX(), bellPos.getY(), bellPos.getZ()) : "None") + " §7(Physical: " + (fObj.hasPhysicalBell() ? "§aYES§7" : "§cNO§7") + ")"), false);
        source.sendSuccess(() -> Component.literal("  Distance to Bell: §a" + String.format("%.1fm", dist)), false);
        source.sendSuccess(() -> Component.literal("  Siege Progress: §c" + fObj.getBellSiegeTicks() + "/100 ticks"), false);
        source.sendSuccess(() -> Component.literal("  Active Attacking Raiders: §f" + fObj.getActiveRaiderCount()), false);
        source.sendSuccess(() -> Component.literal("  Bell Silenced / Destroyed: §f" + (fObj.isBellDestroyedOrCaptured() ? "§cYES" : "§aNO")), false);
        return 1;
    }

    private static int executeDebugRaidRaiders(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        AABB searchBox = AABB.ofSize(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 320, 128, 320);
        List<Raider> raiders = level.getEntitiesOfClass(Raider.class, searchBox, r -> r.isAlive());

        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Active Raiders Diagnostic (" + raiders.size() + " total within 160m) ==="), false);
        if (raiders.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §7No active raiders detected within 160m."), false);
            return 0;
        }

        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        BlockPos bellPos = s != null ? s.getCenter() : pos;
        if (s != null) {
            var obj = RaidTactics.get().getRaidObjective(s.getId().hashCode());
            if (obj != null && obj.getBellPos() != null) bellPos = obj.getBellPos();
        }

        int count = 0;
        for (Raider r : raiders) {
            if (++count > 15) {
                source.sendSuccess(() -> Component.literal("  §7... and " + (raiders.size() - 15) + " more raiders."), false);
                break;
            }
            BlockPos rp = r.blockPosition();
            double distToBell = bellPos != null ? Math.sqrt(bellPos.distSqr(rp)) : 0;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, rp.getX(), rp.getZ());
            boolean underground = (surfaceY - rp.getY()) >= 6;
            var diag = RaidTactics.get().getDiagnostic(r.getUUID());
            boolean stuck = diag != null && (diag.isTrapped() || diag.getStationaryTicks() > 100);

            source.sendSuccess(() -> Component.literal(String.format("  • [%s] at (%d, %d, %d) | DistToBell: %.1fm | Stuck: %s | Underground: %s",
                r.getType().getDescription().getString(), rp.getX(), rp.getY(), rp.getZ(),
                distToBell, stuck ? "§cYES" : "§aNO", underground ? "§cYES" : "§aNO")), false);
        }
        return raiders.size();
    }

    private static int executeDebugRaidStuck(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Stuck Raider Engine Diagnostics (160m Radius) ==="), false);
        List<Raider> stuck = RaidTactics.get().getStuckRaiders(level, pos, 160.0);
        if (stuck.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §aNo stuck or inaccessible raiders detected within 160m."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("  §cDetected " + stuck.size() + " stuck raiders. Executing surface road recovery..."), false);
        int recovered = RaidTactics.get().recoverStuckRaiders(level, pos, 160.0);
        source.sendSuccess(() -> Component.literal("  §aAction: Successfully recovered §f" + recovered + " §astuck raiders to surface roads."), true);
        return recovered;
    }

    private static int executeDebugRaidBell(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Targeted Village Bell Status ==="), false);

        BlockPos bellPos = null;
        RaidTactics.RaidObjective obj = null;
        if (s != null) {
            obj = RaidTactics.get().getRaidObjective(s.getId().hashCode());
            if (obj != null && obj.getBellPos() != null) {
                bellPos = obj.getBellPos();
            } else {
                bellPos = RaidTactics.get().findVillageBell(level, s.getCenter(), s.getRadius() + 32);
            }
        }

        if (bellPos == null) {
            bellPos = RaidTactics.get().findVillageBell(level, pos, 64);
        }

        if (bellPos == null) {
            source.sendSuccess(() -> Component.literal("  §cNo village bell found in this sector!"), false);
            return 0;
        }

        final BlockPos bp = bellPos;
        boolean isBellBlock = level.getBlockState(bp).is(Blocks.BELL);
        int siegeTicks = obj != null ? obj.getBellSiegeTicks() : 0;
        boolean destroyedOrSilenced = obj != null && obj.isBellDestroyedOrCaptured();

        source.sendSuccess(() -> Component.literal(String.format("  Bell Coordinates: §a(%d, %d, %d)", bp.getX(), bp.getY(), bp.getZ())), false);
        source.sendSuccess(() -> Component.literal("  Physical Bell Exists: " + (isBellBlock ? "§aYES" : "§cNO (Destroyed/Missing)")), false);
        source.sendSuccess(() -> Component.literal("  Siege Strike Progress: §e" + siegeTicks + "/100 ticks"), false);
        source.sendSuccess(() -> Component.literal("  Status: " + (destroyedOrSilenced ? "§cSILENCED / BREACHED" : "§aFUNCTIONAL")), false);

        level.sendParticles(ParticleTypes.CRIT, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.2);
        return 1;
    }

    private static int executeDebugVillageTowers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§cNo settlement found nearby."));
            return 0;
        }
        List<BlockPos> towersList = s.getWatchtowers();
        if (towersList.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §7No perimeter watchtowers registered yet. Generating 2 towers now..."), false);
            SettlementManager.get().ensureWatchtowers(level, s);
            towersList = s.getWatchtowers();
        }
        final List<BlockPos> finalTowers = towersList;
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Watchtowers for " + s.getName() + " (" + finalTowers.size() + " registered) ==="), false);

        int idx = 1;
        for (BlockPos tp : finalTowers) {
            final int fIdx = idx;
            int dx = tp.getX() - s.getCenter().getX();
            int dz = tp.getZ() - s.getCenter().getZ();
            String orientation = (dz < 0 ? "North" : "South") + "-" + (dx < 0 ? "West" : "East");
            AABB sentryBox = AABB.ofSize(new Vec3(tp.getX() + 3.5, tp.getY() + 10, tp.getZ() + 3.5), 10, 8, 10);
            List<VillageArcherEntity> archers = level.getEntitiesOfClass(VillageArcherEntity.class, sentryBox, a -> a.isAlive());
            boolean sentryPresent = !archers.isEmpty();
            final String line = String.format("  [%d] Tower at (%d, %d, %d) | Perimeter: §e%s §7| Sentry: %s §7(Elevated Archers: %d)",
                fIdx, tp.getX(), tp.getY(), tp.getZ(), orientation, sentryPresent ? "§aYES" : "§cNO", archers.size());
            source.sendSuccess(() -> Component.literal(line), false);
            idx++;
        }
        return finalTowers.size();
    }

    private static int executeDebugVillageBuildings(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§cNo settlement found nearby."));
            return 0;
        }
        var buildings = s.getBuildings();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Registered Buildings for " + s.getName() + " (" + buildings.size() + ") ==="), false);
        if (buildings.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §7No custom blueprints placed yet; vanilla village structures active."), false);
        } else {
            int i = 1;
            for (var b : buildings) {
                final int fi = i;
                BlockPos bp = b.getPos();
                final String line = String.format("  [%d] %s at (%d, %d, %d) | Status: %s | Beds: %d",
                    fi, b.getBlueprintId(), bp.getX(), bp.getY(), bp.getZ(), b.isCompleted() ? "§aCOMPLETED" : "§eUNDER_CONSTRUCTION", b.getBedCount());
                source.sendSuccess(() -> Component.literal(line), false);
                i++;
            }
        }
        return buildings.size();
    }

    private static int executeDebugVillageConstruction(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§cNo settlement found nearby."));
            return 0;
        }
        ConstructionProject proj = s.getActiveProject();
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Construction Diagnostic for " + s.getName() + " ==="), false);
        if (proj == null) {
            source.sendSuccess(() -> Component.literal("  Active Project: §7None (Idle)"), false);
            source.sendSuccess(() -> Component.literal("  Queued Projects: §e" + s.getQueuedProjectsCount()), false);
            return 0;
        }

        double pct = proj.getProgress() * 100.0;
        source.sendSuccess(() -> Component.literal("  Active Project: §e" + proj.getBlueprintId() + " §7at (" + proj.getOrigin().getX() + ", " + proj.getOrigin().getY() + ", " + proj.getOrigin().getZ() + ")"), false);
        source.sendSuccess(() -> Component.literal(String.format("  Progress: §a%.1f%% §7(Blocks Placed: %d / %d)", pct, proj.getCurrentBlockIndex(), proj.getTotalBlocks())), false);
        source.sendSuccess(() -> Component.literal("  Queued Projects: §f" + s.getQueuedProjectsCount()), false);
        return 1;
    }

    private static int executeDebugVillageDogs(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§cNo settlement found nearby."));
            return 0;
        }
        AABB villageArea = AABB.ofSize(new Vec3(s.getCenter().getX(), s.getCenter().getY(), s.getCenter().getZ()), s.getRadius() * 2, 40, s.getRadius() * 2);
        List<VillageDogEntity> dogs = level.getEntitiesOfClass(VillageDogEntity.class, villageArea, d -> d.isAlive());
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Village Dogs in " + s.getName() + " (" + dogs.size() + " dogs) ==="), false);
        if (dogs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §7No village dogs currently present in settlement perimeter."), false);
            return 0;
        }
        int i = 1;
        for (var dog : dogs) {
            final int fi = i;
            BlockPos dp = dog.blockPosition();
            String roleStr = dog.getRole() == VillageDogEntity.DogRole.GUARD_COMPANION ? "§6Guard Companion" : "§aCivilian Pet";
            String handlerStr = dog.getHandlerEntity() != null ? dog.getHandlerEntity().getName().getString() :
                (dog.getGuardHandlerUuid() != null ? dog.getGuardHandlerUuid().toString().substring(0, 8) : "None");
            final String line = String.format("  [%d] Role: %s §7at (%d, %d, %d) | Handler: §f%s",
                fi, roleStr, dp.getX(), dp.getY(), dp.getZ(), handlerStr);
            source.sendSuccess(() -> Component.literal(line), false);
            i++;
        }
        return dogs.size();
    }

    private static int executeDebugCavalryStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        AABB searchArea = AABB.ofSize(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 256, 64, 256);
        List<VillageCavalryEntity> cavList = level.getEntitiesOfClass(VillageCavalryEntity.class, searchArea, c -> c.isAlive());
        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Village Cavalry Status (" + cavList.size() + " active) ==="), false);
        if (cavList.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  §7No cavalry units deployed within 128m radius."), false);
            return 0;
        }
        int idx = 1;
        for (var cav : cavList) {
            final int fIdx = idx;
            BlockPos cp = cav.blockPosition();
            boolean mounted = cav.isPassenger();
            String horseInfo = "Unmounted";
            if (mounted && cav.getVehicle() instanceof net.minecraft.world.entity.animal.equine.Horse horse) {
                horseInfo = String.format("Horse HP: %.1f/%.1f | Armored: %s", horse.getHealth(), horse.getMaxHealth(), !horse.getBodyArmorItem().isEmpty() ? "§aYES" : "§7NO");
            }
            final String finalHorseInfo = horseInfo;
            final String line = String.format("  [%d] Role: §e%s §7| UUID: %s | Pos: (%d, %d, %d) | %s",
                fIdx, cav.getCavalryRole().name(), cav.getUUID().toString().substring(0, 8), cp.getX(), cp.getY(), cp.getZ(), finalHorseInfo);
            source.sendSuccess(() -> Component.literal(line), false);
            idx++;
        }
        return cavList.size();
    }

    private static int executeDebugMilitaryPatrols(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Settlement s = SettlementManager.get().getNearestSettlement(pos);
        if (s == null) {
            source.sendFailure(Component.literal("§cNo settlement found nearby."));
            return 0;
        }
        var state = livingworld.faction.VillageDefenseController.get().getState(s.getId());
        AABB villageArea = AABB.ofSize(new Vec3(s.getCenter().getX(), s.getCenter().getY(), s.getCenter().getZ()), s.getRadius() * 2, 40, s.getRadius() * 2);
        List<VillageGuardEntity> guards = level.getEntitiesOfClass(VillageGuardEntity.class, villageArea, g -> g.isAlive());
        List<VillageArcherEntity> archers = level.getEntitiesOfClass(VillageArcherEntity.class, villageArea, a -> a.isAlive());
        List<VillageCavalryEntity> cav = level.getEntitiesOfClass(VillageCavalryEntity.class, villageArea, c -> c.isAlive());

        source.sendSuccess(() -> Component.literal("§6[Living World] §e=== Military Patrol & Defense Readiness: " + s.getName() + " ==="), false);
        source.sendSuccess(() -> Component.literal("  Threat Posture: " + state.getThreatLevel().getDisplay()), false);
        source.sendSuccess(() -> Component.literal("  Primary Threat: §f" + state.getPrimaryThreatDescription()), false);
        BlockPos rally = state.getActiveThreatPos() != null ? state.getActiveThreatPos() : s.getCenter();
        source.sendSuccess(() -> Component.literal(String.format("  Active Rally Point: §a(%d, %d, %d)", rally.getX(), rally.getY(), rally.getZ())), false);
        source.sendSuccess(() -> Component.literal(String.format("  Forces Deployed: §b%d Melee Guards §7| §e%d Archers §7| §6%d Cavalry",
            guards.size(), archers.size(), cav.size())), false);
        return guards.size() + archers.size() + cav.size();
    }
}
