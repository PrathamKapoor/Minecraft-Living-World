package livingworld.faction;

import java.util.List;
import livingworld.economy.VillageArmoryManager;
import livingworld.entity.VillageArcherEntity;
import livingworld.entity.VillageGuardEntity;
import livingworld.settlement.Settlement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Civilian Defender Support System (Section 8, Living Village Update).
 * Orchestrates non-combat civilian participation in village sustainability and defense:
 * - Clerics heal wounded guards, archers, and iron golems after combat
 * - Fletchers restock ammunition and deliver arrows to archers
 * - Farmers and Butchers maintain emergency food supplies
 * - Masons and Builders conduct post-siege repairs
 */
public class CivilianSupportManager {
    private static final CivilianSupportManager INSTANCE = new CivilianSupportManager();

    public static CivilianSupportManager get() {
        return INSTANCE;
    }

    public void tickCivilianSupport(ServerLevel level, Settlement settlement) {
        if (level == null || settlement == null) return;

        BlockPos center = settlement.getCenter();
        AABB villageZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 24, settlement.getRadius() * 2);

        // 1. Cleric Medical Healing Loop: Heal Wounded Defenders
        List<Villager> clerics = level.getEntitiesOfClass(Villager.class, villageZone,
            v -> v.isAlive() && v.getVillagerData().profession().is(VillagerProfession.CLERIC));

        if (!clerics.isEmpty()) {
            // Find wounded melee guards
            List<VillageGuardEntity> woundedGuards = level.getEntitiesOfClass(VillageGuardEntity.class, villageZone,
                g -> g.isAlive() && g.getHealth() < g.getMaxHealth());
            for (VillageGuardEntity guard : woundedGuards) {
                if (VillageArmoryManager.get().getStock(settlement).consumeHealingPotion()) {
                    guard.heal(8.0f);
                    level.playSound(null, guard.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 1.0f, 1.2f);
                    level.sendParticles(ParticleTypes.HEART, guard.getX(), guard.getY() + 1.2, guard.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
                    break;
                }
            }

            // Find wounded archers
            List<VillageArcherEntity> woundedArchers = level.getEntitiesOfClass(VillageArcherEntity.class, villageZone,
                a -> a.isAlive() && a.getHealth() < a.getMaxHealth());
            for (VillageArcherEntity archer : woundedArchers) {
                if (VillageArmoryManager.get().getStock(settlement).consumeHealingPotion()) {
                    archer.heal(8.0f);
                    level.playSound(null, archer.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 1.0f, 1.2f);
                    level.sendParticles(ParticleTypes.HEART, archer.getX(), archer.getY() + 1.2, archer.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
                    break;
                }
            }

            // Heal wounded Iron Golems
            List<IronGolem> woundedGolems = level.getEntitiesOfClass(IronGolem.class, villageZone,
                g -> g.isAlive() && g.getHealth() < g.getMaxHealth());
            for (IronGolem golem : woundedGolems) {
                if (settlement.getStockpile().consume(livingworld.economy.Stockpile.ResourceType.STONE, 2)) {
                    golem.heal(15.0f);
                    level.playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + 1.5, golem.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
                    break;
                }
            }
        }
    }
}
