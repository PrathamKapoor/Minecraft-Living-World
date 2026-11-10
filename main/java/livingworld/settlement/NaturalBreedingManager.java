package livingworld.settlement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.economy.EconomyManager;
import livingworld.economy.Stockpile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Natural Population Growth & Villager Breeding Engine (Milestone 3).
 * Governs authentic village demographic growth requiring valid housing capacity,
 * food reserves (12+ food per child), baby identity assignment, and life maturation cycles.
 */
public class NaturalBreedingManager {
    private static final NaturalBreedingManager INSTANCE = new NaturalBreedingManager();

    private final Map<UUID, Long> settlementBreedingCooldowns = new ConcurrentHashMap<>();
    private static final long BREEDING_COOLDOWN_MS = 45_000; // 45 seconds between births per settlement
    private static final int FOOD_PER_CHILD = 12;

    public static NaturalBreedingManager get() {
        return INSTANCE;
    }

    public void tickSettlementDemographics(ServerLevel level, Settlement settlement) {
        if (settlement == null) return;

        // 1. Check housing capacity
        if (settlement.getPopulation() >= settlement.getHousingCapacity()) {
            return;
        }

        // 2. Check breeding cooldown
        long now = System.currentTimeMillis();
        Long lastBreeding = settlementBreedingCooldowns.get(settlement.getId());
        if (lastBreeding != null && (now - lastBreeding) < BREEDING_COOLDOWN_MS) {
            return;
        }

        // 3. Check food stockpile requirement
        int currentFood = settlement.getStockpile().get(Stockpile.ResourceType.FOOD);
        if (currentFood < FOOD_PER_CHILD) {
            return;
        }

        // 4. Verify existing adult villagers are present to conceive
        BlockPos center = settlement.getCenter();
        AABB villageArea = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 20, settlement.getRadius() * 2);

        List<Villager> adults = level.getEntitiesOfClass(Villager.class, villageArea, v -> v.isAlive() && !v.isBaby());
        if (adults.size() < 2) {
            return; // Requires at least two adults
        }

        // 5. Execute birth
        settlement.getStockpile().consume(Stockpile.ResourceType.FOOD, FOOD_PER_CHILD);
        settlementBreedingCooldowns.put(settlement.getId(), now);

        // Spawn baby villager near first adult
        Villager parent = adults.get(0);
        Villager baby = EntityTypes.VILLAGER.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setPos(parent.getX() + 0.5, parent.getY(), parent.getZ() + 0.5);
            baby.setBaby(true);
            baby.setAge(-24000); // Standard Minecraft 20 minute baby duration

            level.addFreshEntity(baby);

            // Assign unique name and profile
            VillagerIdentityManager.VillagerProfile profile = VillagerIdentityManager.get().getOrCreateProfile(baby, settlement);

            // Update settlement population
            settlement.setPopulation(settlement.getPopulation() + 1);

            // Visual love/birth particles & sound
            level.sendParticles(ParticleTypes.HEART, parent.getX(), parent.getY() + 1.2, parent.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, baby.getX(), baby.getY() + 0.6, baby.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, baby.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 0.8f, 1.2f);

            EconomyManager.get().logTransaction(settlement, Stockpile.ResourceType.FOOD, FOOD_PER_CHILD, "BREEDING_CONSUMPTION", profile.getName());
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Demographic expansion: " + profile.getFullName() + " born in " + settlement.getName() + " (Pop: " + settlement.getPopulation() + "/" + settlement.getHousingCapacity() + ")");
        }
    }

    public void clear() {
        settlementBreedingCooldowns.clear();
    }
}
