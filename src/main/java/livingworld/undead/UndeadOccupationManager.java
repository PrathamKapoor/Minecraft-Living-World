package livingworld.undead;

import java.util.List;
import livingworld.config.FeatureConfig;
import livingworld.entity.IllagerShieldbearerEntity;
import livingworld.entity.IllagerSwordsmanEntity;
import livingworld.entity.LivingWorldEntities;
import livingworld.entity.ZombieCowEntity;
import livingworld.entity.ZombiePigEntity;
import livingworld.entity.ZombieSheepEntity;
import livingworld.faction.Faction;
import livingworld.faction.FactionManager;
import livingworld.faction.FactionType;
import livingworld.settlement.ConstructionProject;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import livingworld.settlement.SettlementState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Settlement Occupation & Liberation Engine (Section 14 & Section 17, F08).
 * Resolves conquest ownership strictly to the actual victor:
 * - If Illagers overrun settlement: FactionType.ILLAGER ownership with illager garrison.
 * - If Undead overrun settlement: FactionType.UNDEAD ownership with undead garrison.
 * Supports daytime sun shelter, fortifications, livestock, and heroic liberation.
 */
public class UndeadOccupationManager {
    private static final UndeadOccupationManager INSTANCE = new UndeadOccupationManager();

    public static UndeadOccupationManager get() {
        return INSTANCE;
    }

    public void tickSettlementOccupations(ServerLevel level) {
        if (!FeatureConfig.get().enableUndeadOccupation) {
            return;
        }

        for (Settlement settlement : SettlementManager.get().getAllSettlements()) {
            Vec3 centerVec = new Vec3(settlement.getCenter().getX(), settlement.getCenter().getY(), settlement.getCenter().getZ());
            AABB searchBox = AABB.ofSize(centerVec, settlement.getRadius() * 2, 40, settlement.getRadius() * 2);

            List<Zombie> localZombies = level.getEntitiesOfClass(Zombie.class, searchBox, z -> z.isAlive());
            List<Raider> localIllagers = level.getEntitiesOfClass(Raider.class, searchBox, r -> r.isAlive());
            List<Villager> localVillagers = level.getEntitiesOfClass(Villager.class, searchBox, v -> v.isAlive());

            // 1. Takeover Checks for VILLAGER settlements
            if (settlement.getFactionType() == FactionType.VILLAGER) {
                if (localVillagers.size() <= 1) {
                    // Critical Ownership: check whether Illagers or Undead conquered the settlement
                    if (localIllagers.size() >= 4 && localIllagers.size() > localZombies.size()) {
                        executeIllagerTakeover(level, settlement);
                    } else if (localZombies.size() >= 5) {
                        executeUndeadTakeover(level, settlement);
                    }
                }
            }
            // 2. Ongoing Undead Occupation
            else if (settlement.getFactionType() == FactionType.UNDEAD) {
                if (localZombies.isEmpty()) {
                    executeLiberation(level, settlement, "Undead Scourge");
                } else {
                    manageUndeadOccupiedSettlement(level, settlement, localZombies);
                }
            }
            // 3. Ongoing Illager Occupation
            else if (settlement.getFactionType() == FactionType.ILLAGER) {
                if (localIllagers.isEmpty()) {
                    executeLiberation(level, settlement, "Illager Legion");
                } else {
                    manageIllagerOccupiedSettlement(level, settlement, localIllagers);
                }
            }
        }
    }

    private void manageUndeadOccupiedSettlement(ServerLevel level, Settlement settlement, List<Zombie> zombies) {
        boolean isDay = !level.isDarkOutside();

        for (int i = 0; i < zombies.size(); i++) {
            Zombie z = zombies.get(i);
            if (z.getCustomName() == null) {
                if (i == 0) {
                    z.setCustomName(Component.literal("§cUndead Guard"));
                    z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                } else if (i == 1) {
                    z.setCustomName(Component.literal("§cUndead Builder"));
                    z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                    z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
                } else if (i == 2) {
                    z.setCustomName(Component.literal("§cUndead Herder"));
                    z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                    z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.LEAD));
                }
            }

            // Daytime Sun Shelter Behavior
            if (isDay && !z.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                if (level.canSeeSky(z.blockPosition())) {
                    z.getNavigation().moveTo(settlement.getCenter().getX(), settlement.getCenter().getY(), settlement.getCenter().getZ(), 1.25);
                }
            }
        }

        // Fortification
        if (settlement.getActiveProject() == null && settlement.getQueuedProjectsCount() == 0 && level.getRandom().nextInt(4) == 0) {
            livingworld.settlement.Blueprint wt = livingworld.settlement.BlueprintRegistry.get("watchtower");
            int w = (wt != null) ? wt.getWidth() : 5;
            int d = (wt != null) ? wt.getDepth() : 5;
            BlockPos target = settlement.findClearPlot(level, w, d);
            if (target != null) {
                settlement.queueProject(new ConstructionProject("watchtower", target));
                level.setBlock(target, Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }

        // Livestock conversion/herding
        AABB searchBox = AABB.ofSize(new Vec3(settlement.getCenter().getX(), settlement.getCenter().getY(), settlement.getCenter().getZ()),
            settlement.getRadius(), 20, settlement.getRadius());
        int zAnimals = level.getEntitiesOfClass(ZombiePigEntity.class, searchBox, a -> true).size() +
                       level.getEntitiesOfClass(ZombieCowEntity.class, searchBox, a -> true).size() +
                       level.getEntitiesOfClass(ZombieSheepEntity.class, searchBox, a -> true).size();

        if (zAnimals < 2 && level.getRandom().nextInt(3) == 0) {
            BlockPos spawnPos = settlement.getCenter().offset(level.getRandom().nextInt(10) - 5, 0, level.getRandom().nextInt(10) - 5);
            ZombiePigEntity pig = LivingWorldEntities.ZOMBIE_PIG.create(level, EntitySpawnReason.EVENT);
            if (pig != null) {
                pig.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                level.addFreshEntity(pig);
            }
        }
    }

    private void manageIllagerOccupiedSettlement(ServerLevel level, Settlement settlement, List<Raider> illagers) {
        // Illagers fortify doorways and station crossfire perimeters
        for (Raider r : illagers) {
            if (r.getTarget() == null) {
                // Station at perimeter or watchtowers
                if (r.distanceToSqr(settlement.getCenter().getX(), settlement.getCenter().getY(), settlement.getCenter().getZ()) > (settlement.getRadius() * settlement.getRadius())) {
                    r.getNavigation().moveTo(settlement.getCenter().getX(), settlement.getCenter().getY(), settlement.getCenter().getZ(), 1.1);
                }
            }
        }
    }

    public void executeUndeadTakeover(ServerLevel level, Settlement settlement) {
        settlement.setFactionType(FactionType.UNDEAD);
        settlement.setState(SettlementState.OCCUPIED);
        settlement.setName(cleanName(settlement.getName()) + " [Undead Occupied]");
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Settlement fallen! Undead Scourge occupied " + settlement.getName() + " at " + settlement.getCenter());

        Zombie commander = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
        if (commander != null) {
            commander.setPos(settlement.getCenter().getX() + 0.5, settlement.getCenter().getY() + 1.0, settlement.getCenter().getZ() + 0.5);
            commander.setCustomName(Component.literal("§cUndead Garrison Commander"));
            commander.setCustomNameVisible(true);
            commander.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            commander.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            commander.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            level.addFreshEntity(commander);
        }

        notifyPlayers(level, settlement, "§4[Living World] §c" + settlement.getName() + " has fallen to the Undead Scourge!");
    }

    public void executeIllagerTakeover(ServerLevel level, Settlement settlement) {
        settlement.setFactionType(FactionType.ILLAGER);
        settlement.setState(SettlementState.OCCUPIED);
        settlement.setName(cleanName(settlement.getName()) + " [Illager Occupied]");
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Settlement conquered! Illager Legion captured " + settlement.getName() + " at " + settlement.getCenter());

        // Spawn Illager Garrison Captain & Defenders
        Pillager captain = EntityTypes.PILLAGER.create(level, EntitySpawnReason.EVENT);
        if (captain != null) {
            captain.setPos(settlement.getCenter().getX() + 0.5, settlement.getCenter().getY() + 1.0, settlement.getCenter().getZ() + 0.5);
            captain.setCustomName(Component.literal("§4Illager Garrison Captain"));
            captain.setCustomNameVisible(true);
            captain.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.BANNER.white()));
            level.addFreshEntity(captain);
        }

        IllagerSwordsmanEntity swordsman = LivingWorldEntities.ILLAGER_SWORDSMAN.create(level, EntitySpawnReason.EVENT);
        if (swordsman != null) {
            swordsman.setPos(settlement.getCenter().getX() + 2.5, settlement.getCenter().getY() + 1.0, settlement.getCenter().getZ() + 0.5);
            level.addFreshEntity(swordsman);
        }

        IllagerShieldbearerEntity shieldbearer = LivingWorldEntities.ILLAGER_SHIELDBEARER.create(level, EntitySpawnReason.EVENT);
        if (shieldbearer != null) {
            shieldbearer.setPos(settlement.getCenter().getX() - 2.5, settlement.getCenter().getY() + 1.0, settlement.getCenter().getZ() + 0.5);
            level.addFreshEntity(shieldbearer);
        }

        notifyPlayers(level, settlement, "§4[Living World] §c" + settlement.getName() + " has been seized by the Illager Legion!");
    }

    public void executeLiberation(ServerLevel level, Settlement settlement, String conquerorName) {
        settlement.setFactionType(FactionType.VILLAGER);
        settlement.setState(SettlementState.RECOVERING);
        String restoredName = cleanName(settlement.getName());
        settlement.setName(restoredName);
        settlement.setPopulation(Math.max(4, settlement.getPopulation()));
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Settlement liberated from " + conquerorName + "! " + restoredName + " reclaimed with +35 Hero reputation!");

        Faction villagerFaction = FactionManager.get().getFaction("villagers");
        for (ServerPlayer player : level.players()) {
            if (settlement.isInSettlement(player.blockPosition())) {
                if (villagerFaction != null) {
                    villagerFaction.adjustReputation(player.getUUID(), 35);
                }
                player.sendSystemMessage(Component.literal("§6[Living World] §aYou have liberated " + restoredName + " from the " + conquerorName + "! (+35 Hero Reputation)"));
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.5, 0.5, 0.5, 0.2);
            }
        }
    }

    private String cleanName(String name) {
        return name.replace(" [Occupied]", "").replace(" [Undead Occupied]", "").replace(" [Illager Occupied]", "");
    }

    private void notifyPlayers(ServerLevel level, Settlement settlement, String message) {
        for (ServerPlayer player : level.players()) {
            if (settlement.isInSettlement(player.blockPosition())) {
                player.sendSystemMessage(Component.literal(message));
            }
        }
    }

    public void forceConquerSettlement(Settlement settlement, FactionType conqueror) {
        if (conqueror == FactionType.ILLAGER) {
            settlement.setFactionType(FactionType.ILLAGER);
            settlement.setState(SettlementState.OCCUPIED);
            settlement.setName(cleanName(settlement.getName()) + " [Illager Occupied]");
        } else {
            settlement.setFactionType(FactionType.UNDEAD);
            settlement.setState(SettlementState.OCCUPIED);
            settlement.setName(cleanName(settlement.getName()) + " [Undead Occupied]");
        }
    }

    // Retain legacy method signature for backward compatibility
    public void executeTakeover(ServerLevel level, Settlement settlement) {
        executeUndeadTakeover(level, settlement);
    }

    public void executeLiberation(ServerLevel level, Settlement settlement) {
        executeLiberation(level, settlement, "Occupiers");
    }
}
