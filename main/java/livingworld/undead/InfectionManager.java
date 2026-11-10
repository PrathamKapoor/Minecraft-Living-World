package livingworld.undead;

import livingworld.config.FeatureConfig;
import livingworld.core.ProtectionManager;
import livingworld.economy.Stockpile;
import livingworld.entity.LivingWorldEntities;
import livingworld.entity.ZombieCowEntity;
import livingworld.entity.ZombiePigEntity;
import livingworld.entity.ZombieSheepEntity;
import livingworld.settlement.Settlement;
import livingworld.settlement.SettlementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Manages genuine livestock infection, conversions, and dark domestic economy (F09).
 */
public class InfectionManager {
    private static final InfectionManager INSTANCE = new InfectionManager();

    public static InfectionManager get() {
        return INSTANCE;
    }

    public void tryConvertLivestock(ServerLevel level, Animal animal, Zombie attacker) {
        if (!FeatureConfig.get().enableZombieAnimals) {
            return;
        }

        // Never convert protected or player-owned animals
        if (ProtectionManager.get().isProtectedAnimal(animal)) {
            return;
        }

        BlockPos pos = animal.blockPosition();

        if (animal instanceof Horse horse) {
            ZombieHorse zHorse = EntityTypes.ZOMBIE_HORSE.create(level, EntitySpawnReason.CONVERSION);
            if (zHorse != null) {
                zHorse.setPos(horse.getX(), horse.getY(), horse.getZ());
                zHorse.setTamed(true);
                level.addFreshEntity(zHorse);

                // Attacker mounts the zombie horse to form cavalry!
                if (attacker != null && attacker.isAlive()) {
                    attacker.startRiding(zHorse);
                }
                animal.discard();
                level.sendParticles(ParticleTypes.ITEM_SLIME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.05);
            }
        } else if (animal instanceof Pig) {
            ZombiePigEntity zPig = LivingWorldEntities.ZOMBIE_PIG.create(level, EntitySpawnReason.CONVERSION);
            if (zPig != null) {
                zPig.setPos(animal.getX(), animal.getY(), animal.getZ());
                level.addFreshEntity(zPig);
                animal.discard();
                level.sendParticles(ParticleTypes.ITEM_SLIME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Livestock Infection: Pig converted to ZombiePigEntity at " + pos);
            }
        } else if (animal instanceof Cow) {
            ZombieCowEntity zCow = LivingWorldEntities.ZOMBIE_COW.create(level, EntitySpawnReason.CONVERSION);
            if (zCow != null) {
                zCow.setPos(animal.getX(), animal.getY(), animal.getZ());
                level.addFreshEntity(zCow);
                animal.discard();
                level.sendParticles(ParticleTypes.ITEM_SLIME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.05);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Livestock Infection: Cow converted to ZombieCowEntity at " + pos);
            }
        } else if (animal instanceof Sheep) {
            ZombieSheepEntity zSheep = LivingWorldEntities.ZOMBIE_SHEEP.create(level, EntitySpawnReason.CONVERSION);
            if (zSheep != null) {
                zSheep.setPos(animal.getX(), animal.getY(), animal.getZ());
                level.addFreshEntity(zSheep);
                animal.discard();
                level.sendParticles(ParticleTypes.ITEM_SLIME, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.05);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Livestock Infection: Sheep converted to ZombieSheepEntity at " + pos);
            }
        }

        // Contribute dark spoils to nearby occupied settlements
        Settlement settlement = SettlementManager.get().getSettlementAt(pos);
        if (settlement != null) {
            settlement.getStockpile().add(Stockpile.ResourceType.LEATHER, 2);
            settlement.getStockpile().add(Stockpile.ResourceType.FOOD, 1);
        }
    }

    public void onInfectedDeath(ServerLevel level, Animal animal) {
        BlockPos pos = animal.blockPosition();
        if (animal instanceof ZombieCowEntity) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.ROTTEN_FLESH, 2)));
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.LEATHER, 2)));
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.BONE, 1)));
        } else if (animal instanceof ZombiePigEntity) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.ROTTEN_FLESH, 2)));
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.BONE, 1)));
        } else if (animal instanceof ZombieSheepEntity) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.ROTTEN_FLESH, 2)));
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.WOOL.black(), 1)));
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.BONE, 1)));
        }
    }
}
