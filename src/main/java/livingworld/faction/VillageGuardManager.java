package livingworld.faction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.entity.LivingWorldEntities;
import livingworld.entity.VillageGuardEntity;
import livingworld.settlement.Settlement;
import livingworld.settlement.VillagerIdentityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Village Guard Recruitment, Promotion & Garrison Pipeline (Milestone 4).
 * Recruits able-bodied villagers into town guards, enforces minimum iron armor tiers,
 * tracks combat experience for rank promotions (Recruit -> Soldier -> Veteran -> Elite -> Royal),
 * and maintains defensive perimeter garrisons.
 */
public class VillageGuardManager {
    private static final VillageGuardManager INSTANCE = new VillageGuardManager();

    private final Map<UUID, GuardRecord> guardRecords = new ConcurrentHashMap<>();

    public static VillageGuardManager get() {
        return INSTANCE;
    }

    public enum GuardRank {
        RECRUIT("Recruit", 1.0f),
        SOLDIER("Soldier", 1.2f),
        VETERAN("Veteran", 1.4f),
        ELITE("Elite", 1.7f),
        ROYAL("Royal Sentinel", 2.0f);

        private final String title;
        private final float powerMultiplier;

        GuardRank(String title, float powerMultiplier) {
            this.title = title;
            this.powerMultiplier = powerMultiplier;
        }

        public String getTitle() {
            return title;
        }

        public float getPowerMultiplier() {
            return powerMultiplier;
        }
    }

    public static class GuardRecord {
        private final UUID guardUuid;
        private final UUID settlementId;
        private String name;
        private GuardRank rank;
        private int kills;
        private int battlesSurvived;

        public GuardRecord(UUID guardUuid, UUID settlementId, String name, GuardRank rank) {
            this.guardUuid = guardUuid;
            this.settlementId = settlementId;
            this.name = name;
            this.rank = rank;
            this.kills = 0;
            this.battlesSurvived = 0;
        }

        public UUID getGuardUuid() {
            return guardUuid;
        }

        public UUID getSettlementId() {
            return settlementId;
        }

        public String getName() {
            return name;
        }

        public GuardRank getRank() {
            return rank;
        }

        public void setRank(GuardRank rank) {
            this.rank = rank;
        }

        public int getKills() {
            return kills;
        }

        public void recordKill() {
            this.kills++;
            evaluatePromotion();
        }

        public int getBattlesSurvived() {
            return battlesSurvived;
        }

        public void recordBattleSurvived() {
            this.battlesSurvived++;
            evaluatePromotion();
        }

        private void evaluatePromotion() {
            if (kills >= 20 || battlesSurvived >= 10) {
                rank = GuardRank.ROYAL;
            } else if (kills >= 10 || battlesSurvived >= 5) {
                rank = GuardRank.ELITE;
            } else if (kills >= 4 || battlesSurvived >= 2) {
                rank = GuardRank.VETERAN;
            } else if (kills >= 1) {
                rank = GuardRank.SOLDIER;
            }
        }
    }

    public GuardRecord getRecord(UUID guardUuid) {
        return guardRecords.get(guardUuid);
    }

    public void tickGuardRecruitment(ServerLevel level, Settlement settlement) {
        if (settlement == null) return;

        BlockPos center = settlement.getCenter();
        AABB villageArea = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 24, settlement.getRadius() * 2);

        List<VillageGuardEntity> currentGuards = level.getEntitiesOfClass(VillageGuardEntity.class, villageArea, g -> g.isAlive());
        List<livingworld.entity.VillageArcherEntity> currentArchers = level.getEntitiesOfClass(livingworld.entity.VillageArcherEntity.class, villageArea, a -> a.isAlive());
        int totalMilitary = currentGuards.size() + currentArchers.size();
        settlement.setGuardCount(totalMilitary);

        // Target guard count: 1 guard per 3 villagers, minimum 2
        int desiredGuards = Math.max(2, settlement.getPopulation() / 3);
        if (totalMilitary >= desiredGuards) {
            return;
        }

        // Locate eligible unemployed villager to enlist
        List<Villager> availableCivilians = level.getEntitiesOfClass(Villager.class, villageArea,
            v -> v.isAlive() && !v.isBaby());

        if (availableCivilians.size() <= 2) {
            return; // Maintain at least 2 breeding adults
        }

        // Recruit civilian into military guard (maintain roughly 40% archers, 60% melee)
        boolean recruitArcher = currentArchers.size() < Math.max(1, (int) Math.round(desiredGuards * 0.4));
        Villager candidate = availableCivilians.get(level.getRandom().nextInt(availableCivilians.size()));
        BlockPos recruitPos = candidate.blockPosition();

        String guardName = candidate.getCustomName() != null ? candidate.getCustomName().getString() : "Alden";
        guardName = guardName.replace("§e", "").replaceAll(" the .*", "");

        if (recruitArcher) {
            livingworld.entity.VillageArcherEntity archer = LivingWorldEntities.VILLAGE_ARCHER.create(level, EntitySpawnReason.EVENT);
            if (archer != null) {
                archer.setPos(recruitPos.getX() + 0.5, recruitPos.getY(), recruitPos.getZ() + 0.5);
                archer.setHomeSettlementId(settlement.getId().toString());
                archer.setCustomName(Component.literal("§9[Archer] " + guardName));
                archer.setCustomNameVisible(true);
                level.addFreshEntity(archer);
                candidate.discard();

                level.playSound(null, recruitPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, recruitPos.getX() + 0.5, recruitPos.getY() + 1.2, recruitPos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.1);

                settlement.setGuardCount(totalMilitary + 1);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Military enlistment: " + guardName + " recruited as Village Archer in " + settlement.getName());
                return;
            }
        }

        VillageGuardEntity guard = LivingWorldEntities.VILLAGE_GUARD.create(level, EntitySpawnReason.EVENT);
        if (guard != null) {
            guard.setPos(recruitPos.getX() + 0.5, recruitPos.getY(), recruitPos.getZ() + 0.5);
            guard.setHomeSettlementId(settlement.getId().toString());

            // Enforce minimum iron tier equipment
            guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            guard.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            guard.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            guard.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            guard.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

            GuardRecord record = new GuardRecord(guard.getUUID(), settlement.getId(), guardName, GuardRank.RECRUIT);
            guardRecords.put(guard.getUUID(), record);

            guard.setCustomName(Component.literal("§9[Guard " + record.getRank().getTitle() + "] " + guardName));
            guard.setCustomNameVisible(true);

            level.addFreshEntity(guard);
            candidate.discard(); // Replaced with guard entity

            // Audio & visual recruitment ceremony
            level.playSound(null, recruitPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, recruitPos.getX() + 0.5, recruitPos.getY() + 1.2, recruitPos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.1);

            settlement.setGuardCount(totalMilitary + 1);
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Military enlistment: " + guardName + " recruited as Village Guard in " + settlement.getName());
        }
    }

    public void onGuardKill(ServerLevel level, VillageGuardEntity guard) {
        GuardRecord record = guardRecords.get(guard.getUUID());
        if (record != null) {
            GuardRank oldRank = record.getRank();
            record.recordKill();
            if (record.getRank() != oldRank) {
                // Promoted!
                guard.setCustomName(Component.literal("§9[Guard " + record.getRank().getTitle() + "] " + record.getName()));
                level.playSound(null, guard.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.2f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, guard.getX(), guard.getY() + 1.5, guard.getZ(), 16, 0.5, 0.5, 0.5, 0.1);

                if (record.getRank() == GuardRank.ELITE || record.getRank() == GuardRank.ROYAL) {
                    guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                    guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                }
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Military promotion: Guard " + record.getName() + " promoted to " + record.getRank().getTitle() + "!");
            }
        }
    }

    public void clear() {
        guardRecords.clear();
    }
}
