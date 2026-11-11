package livingworld;

import java.io.File;
import livingworld.ai.CreeperBreachGoal;
import livingworld.ai.SkeletonTacticsGoal;
import livingworld.ai.ZombieBreachGoal;
import livingworld.bosses.DragonPhaseController;
import livingworld.bosses.GiantZombieBoss;
import livingworld.bosses.NetherBehemothBoss;
import livingworld.command.LivingWorldCommand;
import livingworld.config.FeatureConfig;
import livingworld.core.ProtectionManager;
import livingworld.economy.AnimalHusbandryManager;
import livingworld.entity.AerialThreatManager;
import livingworld.entity.LivingWorldEntities;
import livingworld.exploration.ExplorationManager;
import livingworld.exploration.GlobalMapManager;
import livingworld.faction.FactionManager;
import livingworld.faction.ReputationManager;
import livingworld.illager.RaidTactics;
import livingworld.ocean.SharkManager;
import livingworld.piglin.PiglinTribeManager;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import livingworld.travel.MinecartSpeedManager;
import livingworld.travel.SwiftMountTraversalManager;
import livingworld.undead.InfectionManager;
import livingworld.undead.UndeadOccupationManager;
import livingworld.undead.ZombieHordeManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LivingWorldMod implements ModInitializer {
    public static final String MOD_ID = "livingworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static long serverTicks = 0;
    private static MinecraftServer currentServer = null;

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("[Living World] Initializing Minecraft: Living World v0.2.8 for Minecraft 26.3 (Java 25)");

        // 1. Initialize Configuration
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("livingworld.json").toFile();
        FeatureConfig.load(configFile);
        LOGGER.info("[Living World] Configuration loaded. Protection enabled: {}", FeatureConfig.get().protectPlayerBlocks);

        // 2. Initialize Custom Entity Attributes & Spawn Eggs (F09, F12, F13, F15, Addendum 2)
        LivingWorldEntities.init();
        livingworld.item.LivingWorldItems.init();

        // 3. Register Commands
        CommandRegistrationCallback.EVENT.register(LivingWorldCommand::register);

        // 4. Register Usable Map / Compass Cartography GUI & Waypoint Hook (F18)
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
                if (held.is(Items.MAP) || held.is(Items.FILLED_MAP) || held.is(Items.COMPASS)) {
                    if (player.isShiftKeyDown()) {
                        int count = GlobalMapManager.get().getWaypoints(sp.getUUID()).size() + 1;
                        GlobalMapManager.get().addWaypoint(sp, "Camp " + count);
                    } else {
                        GlobalMapManager.get().openGlobalMapGui(sp);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });

        // 5. Register Player Protection Events & Environmental Altar Rituals
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                BlockPos clickedPos = hitResult.getBlockPos();
                net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);

                // Nether Flame Champion Environmental Altar Summon (F14)
                if (level.dimension() == Level.NETHER && level.getBlockState(clickedPos).is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN)) {
                    if (held.is(Items.WITHER_SKELETON_SKULL) || held.is(Items.FIRE_CHARGE)) {
                        if (!NetherBehemothBoss.hasActiveBoss()) {
                            held.shrink(1);
                            NetherBehemothBoss.summon((ServerLevel) level, clickedPos.above());
                            sp.sendSystemMessage(Component.literal("§4[Living World] §6The crying obsidian burns with ancient wrath! The Nether Flame Champion awakens!"));
                            return InteractionResult.SUCCESS;
                        }
                    }
                }

                BlockPos placedPos = clickedPos.relative(hitResult.getDirection());
                ProtectionManager.get().registerPlayerBlock(placedPos);
            }
            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!level.isClientSide()) {
                ProtectionManager.get().unregisterPlayerBlock(pos);
            }
        });

        // 6. Register Animal & Golem Protection, Friendly-Fire Filtering and Livestock Infection Hook
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
            // Centralized Allegiance & Friendly-Fire Cancellation (Section 2, v0.2.5 Living Village Rebuild)
            if (livingworld.faction.AllegianceRegistry.get().shouldCancelFriendlyFire(victim, source)) {
                if (victim instanceof net.minecraft.world.entity.Mob mobVictim) {
                    livingworld.faction.AllegianceRegistry.get().sanitizeDefenderTarget(mobVictim);
                }
                if (source.getEntity() instanceof net.minecraft.world.entity.Mob mobAttacker) {
                    livingworld.faction.AllegianceRegistry.get().sanitizeDefenderTarget(mobAttacker);
                }
                return false; // Zero allied friendly-fire damage!
            }

            // Protect player pets/named animals from monster damage
            if (source.getEntity() instanceof Monster && ProtectionManager.get().isProtectedAnimal(victim)) {
                return false;
            }

            // Livestock infection conversion (F09)
            if (victim instanceof Animal animal && source.getEntity() instanceof Zombie zombie && victim.level() instanceof ServerLevel sl) {
                if (victim.getHealth() - amount <= 0.0f) {
                    InfectionManager.get().tryConvertLivestock(sl, animal, zombie);
                }
            }

            // Guard accidental hit forgiveness
            if ((victim instanceof livingworld.entity.VillageGuardEntity || victim instanceof livingworld.entity.VillageArcherEntity)
                && victim instanceof net.minecraft.world.entity.Mob guardMob && source.getEntity() instanceof ServerPlayer player) {
                BlockPos pos = victim.blockPosition();
                Settlement settlement = SettlementManager.get().getSettlementAt(pos);
                return ReputationManager.get().handleGuardHit(player, guardMob, settlement);
            }

            // Civilian Distress Signalling (Section 5, v0.2.8 Stabilization)
            if (victim instanceof net.minecraft.world.entity.npc.villager.Villager villager
                && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && victim.level() instanceof ServerLevel sl) {
                livingworld.faction.CivilianDistressManager.get().reportDistress(sl, villager, attacker);
            }

            // Golem accidental hit forgiveness (F05)
            if (victim instanceof IronGolem golem && source.getEntity() instanceof ServerPlayer player) {
                BlockPos pos = golem.blockPosition();
                Settlement settlement = SettlementManager.get().getSettlementAt(pos);
                if (settlement != null) {
                    return ReputationManager.get().handleGolemHit(player, golem, settlement);
                }
            }

            return true;
        });

        // 7. Entity Death Hook for Horde Commander Defeat (F10), Infected Livestock (F09), and Guard Promotions (Milestone 4)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity.level() instanceof ServerLevel sl) {
                ZombieHordeManager.get().onEntityDeath(sl, entity.getUUID());
                if (entity instanceof Animal animal) {
                    InfectionManager.get().onInfectedDeath(sl, animal);
                }
                if (damageSource.getEntity() instanceof livingworld.entity.VillageGuardEntity guard) {
                    livingworld.faction.VillageGuardManager.get().onGuardKill(sl, guard);
                }
                // Section 9: Player village defense reputation rewards
                if (damageSource.getEntity() instanceof ServerPlayer player && entity instanceof LivingEntity threat) {
                    ReputationManager.get().onMonsterDefeated(player, threat, sl);
                }
            }
        });

        // 7b. Player Trade / Contribution to Village Stockpile (Milestone 3)
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide() && player instanceof ServerPlayer sp && entity instanceof net.minecraft.world.entity.npc.villager.Villager villager) {
                net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
                Settlement settlement = SettlementManager.get().getSettlementAt(villager.blockPosition());
                if (settlement != null && !held.isEmpty()) {
                    boolean deposited = livingworld.economy.EconomyManager.get().depositPlayerContribution((ServerLevel) level, sp, settlement, held);
                    if (deposited) {
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        // 8. Attach Tactical AI Goals on Entity Spawn (F01, F02, F03, F04, F11, F16, Addendum 2, Milestone 3)
        ServerEntityEvents.ENTITY_LOAD.register((entity, serverLevel) -> {
            Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (entityId != null && "mutantmonsters".equals(entityId.getNamespace())) {
                // Strictly preserve Mutant Monsters' specialized native AI without interference
                return;
            }
            if (entity instanceof Zombie zombie) {
                zombie.getGoalSelector().addGoal(1, new ZombieBreachGoal(zombie));
                setupZombieToolMiner(zombie, serverLevel);
                if (livingworld.ai.ZombieSiegeMiningGoal.isEligibleMiner(zombie)) {
                    zombie.getGoalSelector().addGoal(1, new livingworld.ai.ZombieSiegeMiningGoal(zombie));
                }
            } else if (entity instanceof AbstractSkeleton skeleton) {
                skeleton.getGoalSelector().addGoal(1, new SkeletonTacticsGoal(skeleton));
            } else if (entity instanceof Creeper creeper) {
                creeper.getGoalSelector().addGoal(2, new CreeperBreachGoal(creeper));
            } else if (entity instanceof Pillager pillager) {
                RaidTactics.get().setupPillagerTactics(pillager);
            } else if (entity instanceof Piglin piglin) {
                PiglinTribeManager.get().assignTribeOnLoad(piglin, serverLevel);
            } else if (entity instanceof WanderingTrader trader) {
                ExplorationManager.get().enhanceWanderingTrader(trader);
            } else if (entity instanceof net.minecraft.world.entity.npc.villager.Villager villager) {
                Settlement settlement = SettlementManager.get().checkAndAutoDiscoverVillage(serverLevel, villager.blockPosition());
                if (settlement == null) {
                    settlement = SettlementManager.get().getSettlementAt(villager.blockPosition());
                }
                livingworld.settlement.VillagerIdentityManager.get().getOrCreateProfile(villager, settlement);
            }
        });

        // 8b. Natural Dimensional Scars Chunk Load Hook (Feature F19)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((serverLevel, chunk, isNew) -> {
            livingworld.worldgen.DimensionalScarManager.get().onChunkLoad(serverLevel, chunk.getPos());
        });

        // 9. Server Lifecycle: World Save & Load Hooks
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverTicks = 0;
            currentServer = server;
            LOGGER.info("[Living World] Server started. Shared world simulation activated.");
            ServerLevel overworld = server.overworld();
            if (overworld != null) {
                SettlementManager.get().load(overworld);
                File factionsFile = server.getWorldPath(LevelResource.ROOT).resolve("livingworld").resolve("factions.json").toFile();
                FactionManager.get().load(factionsFile);
            }
        });

        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            ServerLevel overworld = server.overworld();
            if (overworld != null) {
                SettlementManager.get().save(overworld);
                File factionsFile = server.getWorldPath(LevelResource.ROOT).resolve("livingworld").resolve("factions.json").toFile();
                FactionManager.get().save(factionsFile);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOGGER.info("[Living World] Server stopped. Checkpointing simulation state...");
            currentServer = null;
            ServerLevel overworld = server.overworld();
            if (overworld != null) {
                SettlementManager.get().save(overworld);
                File factionsFile = server.getWorldPath(LevelResource.ROOT).resolve("livingworld").resolve("factions.json").toFile();
                FactionManager.get().save(factionsFile);
            }
            SettlementManager.get().clear();
            ProtectionManager.get().clear();
            livingworld.worldgen.DimensionalScarManager.get().clear();
            RaidTactics.get().clear();
        });

        // 10. Server Tick Scheduler: Two-Scale Simulation (Strategic & Tactical across Dimensions)
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (level instanceof ServerLevel serverLevel) {
                if (serverLevel.dimension() == Level.OVERWORLD) {
                    serverTicks++;
                    SettlementManager.get().tick(serverLevel, serverTicks);

                    // Run animal husbandry, occupations, marine ecology, and aerial threats
                    if (serverTicks % 100 == 0) {
                        for (Settlement s : SettlementManager.get().getAllSettlements()) {
                            AnimalHusbandryManager.get().tickSettlementHerds(serverLevel, s);
                        }
                        UndeadOccupationManager.get().tickSettlementOccupations(serverLevel);
                        SharkManager.get().tickMarineEcology(serverLevel);
                    }

                    AerialThreatManager.get().tickAerialThreats(serverLevel, serverTicks);
                    ZombieHordeManager.get().tick(serverLevel, serverTicks);
                    GiantZombieBoss.tickAll(serverLevel);
                    MinecartSpeedManager.get().tickMinecartBoosts(serverLevel);
                    SwiftMountTraversalManager.get().tickTraversal(serverLevel);
                    RaidTactics.get().tickRaidTactics(serverLevel);
                    livingworld.worldgen.DimensionalScarManager.get().tick(serverLevel);

                    // Track player exploration, emergency support, visitor greetings & mineshaft cues
                    for (ServerPlayer player : serverLevel.players()) {
                        ReputationManager.get().checkRaidSupport(player, serverTicks);
                        GlobalMapManager.get().recordPlayerMovement(player);
                        ExplorationManager.get().checkMineshaftEncounters(serverLevel, player, serverTicks);
                        if (serverTicks % 40 == 0) {
                            Settlement s = SettlementManager.get().getSettlementAt(player.blockPosition());
                            if (s != null) {
                                ReputationManager.get().welcomeVisitor(player, s);
                            }
                        }
                    }
                } else if (serverLevel.dimension() == Level.NETHER) {
                    PiglinTribeManager.get().tickTribalSkirmishes(serverLevel);
                    NetherBehemothBoss.tickAll(serverLevel);
                    MinecartSpeedManager.get().tickMinecartBoosts(serverLevel);
                    livingworld.worldgen.DimensionalScarManager.get().tick(serverLevel);
                } else if (serverLevel.dimension() == Level.END) {
                    DragonPhaseController.get().tickDragonPhases(serverLevel);
                }
            }
        });
    }

    public static void setupZombieToolMiner(Zombie zombie, ServerLevel level) {
        if (!FeatureConfig.get().zombieMiningEnabled) return;
        if (zombie.isBaby()) return;

        // Prevent re-evaluating tools across chunk unloads
        if (zombie.entityTags().contains("livingworld:evaluated_tool")) {
            return;
        }
        zombie.addTag("livingworld:evaluated_tool");

        double roll = zombie.getRandom().nextDouble();
        if (roll < FeatureConfig.get().zombieToolSpawnRate) { // 5% by default
            double toolRoll = zombie.getRandom().nextDouble();
            net.minecraft.world.item.Item toolItem;
            double pickaxeLimit = FeatureConfig.get().zombiePickaxeShare; // 0.45
            double axeLimit = pickaxeLimit + FeatureConfig.get().zombieAxeShare; // 0.80

            double tierRoll = zombie.getRandom().nextDouble();
            if (toolRoll < pickaxeLimit) {
                if (tierRoll < 0.50) toolItem = Items.WOODEN_PICKAXE;
                else if (tierRoll < 0.85) toolItem = Items.STONE_PICKAXE;
                else if (tierRoll < 0.98) toolItem = Items.IRON_PICKAXE;
                else toolItem = Items.DIAMOND_PICKAXE;
            } else if (toolRoll < axeLimit) {
                if (tierRoll < 0.50) toolItem = Items.WOODEN_AXE;
                else if (tierRoll < 0.85) toolItem = Items.STONE_AXE;
                else if (tierRoll < 0.98) toolItem = Items.IRON_AXE;
                else toolItem = Items.DIAMOND_AXE;
            } else {
                if (tierRoll < 0.50) toolItem = Items.WOODEN_SHOVEL;
                else if (tierRoll < 0.85) toolItem = Items.STONE_SHOVEL;
                else if (tierRoll < 0.98) toolItem = Items.IRON_SHOVEL;
                else toolItem = Items.DIAMOND_SHOVEL;
            }

            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(toolItem));
            zombie.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.085f);
            zombie.addTag("livingworld:tool_miner");
            LOGGER.info("[Living World] Equipped tool-bearing zombie with {} at {}", toolItem.getDescriptionId(), zombie.blockPosition());
        }
    }

    public static long getServerTicks() {
        return serverTicks;
    }
}
