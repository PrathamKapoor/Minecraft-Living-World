package livingworld.economy;

import java.util.ArrayList;
import java.util.List;
import livingworld.config.FeatureConfig;
import livingworld.core.ProtectionManager;
import livingworld.settlement.Settlement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AnimalHusbandryManager {
    private static final AnimalHusbandryManager INSTANCE = new AnimalHusbandryManager();

    public static AnimalHusbandryManager get() {
        return INSTANCE;
    }

    public void tickSettlementHerds(ServerLevel level, Settlement settlement) {
        if (!FeatureConfig.get().enableAnimalHusbandry) {
            return;
        }

        Vec3 centerVec = new Vec3(settlement.getCenter().getX(), settlement.getCenter().getY(), settlement.getCenter().getZ());
        AABB searchBox = AABB.ofSize(centerVec, settlement.getRadius() * 2, 40, settlement.getRadius() * 2);

        List<Animal> localAnimals = level.getEntitiesOfClass(Animal.class, searchBox,
            animal -> !ProtectionManager.get().isProtectedAnimal(animal));

        List<Cow> cows = new ArrayList<>();
        List<Pig> pigs = new ArrayList<>();
        List<Sheep> sheep = new ArrayList<>();
        List<Chicken> chickens = new ArrayList<>();

        for (Animal a : localAnimals) {
            if (a instanceof Cow cow) cows.add(cow);
            else if (a instanceof Pig pig) pigs.add(pig);
            else if (a instanceof Sheep s) sheep.add(s);
            else if (a instanceof Chicken c) chickens.add(c);
        }

        manageLivestockGroup(cows, settlement, Stockpile.ResourceType.LEATHER);
        manageLivestockGroup(pigs, settlement, null);
        manageLivestockGroup(sheep, settlement, Stockpile.ResourceType.WOOL);
        manageLivestockGroup(chickens, settlement, null);
    }

    private <T extends Animal> void manageLivestockGroup(List<T> animals, Settlement settlement, Stockpile.ResourceType secondaryDrop) {
        List<T> adults = new ArrayList<>();
        for (T a : animals) {
            if (!a.isBaby()) {
                adults.add(a);
            }
        }

        // 1. Breeding: if food available and population under cap (10)
        if (adults.size() >= 2 && adults.size() < 10 && settlement.getStockpile().get(Stockpile.ResourceType.FOOD) >= 4) {
            T parent1 = adults.get(0);
            T parent2 = adults.get(1);
            if (parent1.canFallInLove() && parent2.canFallInLove()) {
                parent1.setInLoveTime(600);
                parent2.setInLoveTime(600);
                settlement.getStockpile().consume(Stockpile.ResourceType.FOOD, 2);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Husbandry: Bred livestock in " + settlement.getName() + " (Adults: " + adults.size() + ", food consumed: 2)");
            }
        }

        // 2. Sustainable Butchery: Cull surplus adults when herd is thriving (4+ adults), leaving at least 3
        if (adults.size() >= 4) {
            T toCull = adults.get(adults.size() - 1);
            // Verify protection again
            if (!ProtectionManager.get().isProtectedAnimal(toCull)) {
                // Add food and secondary drops into settlement stockpile
                settlement.getStockpile().add(Stockpile.ResourceType.FOOD, 3);
                if (secondaryDrop != null) {
                    settlement.getStockpile().add(secondaryDrop, 1);
                }
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Husbandry: Sustainable culling of excess livestock in " + settlement.getName() + " (Stockpile food +3, remaining: " + (adults.size() - 1) + ")");
                toCull.discard();
            }
        }
    }
}
