package livingworld.faction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.economy.VillageArmoryManager;
import livingworld.entity.LivingWorldEntities;
import livingworld.entity.VillageArcherEntity;
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
 * Village Guard Recruitment, Promotion & Garrison Pipeline (v0.2.5 Living Village Rebuild).
 * Coordinates military recruitment through central Guard Captain / Recruiting Officer authority:
 * - Identifies personnel shortages vs village population targets
 * - Requests authentic physical weapons and armor from the Village Armory
 * - Enlists eligible unemployed adult villagers while preserving civilian workforce
 * - Conducts visible recruitment ceremonies with audio/visual equip routines
 * - Tracks combat experience for promotions (Recruit -> Soldier -> Veteran -> Elite -> Royal)
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
        if (settlement == null || level == null) return;

        BlockPos center = settlement.getCenter();
        AABB villageArea = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2, 24, settlement.getRadius() * 2);

        List<VillageGuardEntity> currentGuards = level.getEntitiesOfClass(VillageGuardEntity.class, villageArea, g -> g.isAlive());
        List<VillageArcherEntity> currentArchers = level.getEntitiesOfClass(VillageArcherEntity.class, villageArea, a -> a.isAlive());
        int totalMilitary = currentGuards.size() + currentArchers.size();
        settlement.setGuardCount(totalMilitary);

        // Target military force: 1 guard per 3 villagers, minimum 2 for small villages, up to 12
        int desiredGuards = Math.max(2, Math.min(12, settlement.getPopulation() / 3));
        if (totalMilitary >= desiredGuards) {
            return;
        }

        // Locate eligible unemployed adult villagers to enlist
        List<Villager> availableCivilians = level.getEntitiesOfClass(Villager.class, villageArea,
            v -> v.isAlive() && !v.isBaby());

        // Maintain at least 4 civilians to preserve essential community professions and breeding
        if (availableCivilians.size() <= 4) {
            return;
        }

        // Check Village Armory supply
        VillageArmoryManager.ArmoryStock armory = VillageArmoryManager.get().getStock(settlement);

        // Maintain approximately 40% archers, 60% melee
        boolean recruitArcher = currentArchers.size() < Math.max(1, (int) Math.round(desiredGuards * 0.4));

        if (recruitArcher) {
            if (!armory.hasArcherGear()) {
                // Insufficient bows/arrows in armory stock
                return;
            }
            armory.consumeArcherGear();

            Villager candidate = availableCivilians.get(level.getRandom().nextInt(availableCivilians.size()));
            BlockPos recruitPos = candidate.blockPosition();

            String guardName = candidate.getCustomName() != null ? candidate.getCustomName().getString() : "Alden";
            guardName = guardName.replace("§e", "").replaceAll(" the .*", "");

            VillageArcherEntity archer = LivingWorldEntities.VILLAGE_ARCHER.create(level, EntitySpawnReason.EVENT);
            if (archer != null) {
                archer.setPos(recruitPos.getX() + 0.5, recruitPos.getY(), recruitPos.getZ() + 0.5);
                archer.setHomeSettlementId(settlement.getId().toString());
                archer.setCustomName(Component.literal("§9[Archer] " + guardName));
                archer.setCustomNameVisible(true);

                // Assign watchtower sentry duty if needed
                if (currentArchers.size() < 2) {
                    archer.setWatchtowerSentry(true);
                }

                AllegianceRegistry.get().register(archer.getUUID(), settlement.getId().toString(), "VILLAGE_ALLIANCE",
                    archer.isWatchtowerSentry() ? AllegianceRegistry.MilitaryRole.WATCHTOWER_SENTRY : AllegianceRegistry.MilitaryRole.ARCHER);

                level.addFreshEntity(archer);
                candidate.discard();

                // Audible & visual enlistment ceremony
                level.playSound(null, recruitPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                level.playSound(null, recruitPos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 0.8f, 1.2f);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, recruitPos.getX() + 0.5, recruitPos.getY() + 1.2, recruitPos.getZ() + 0.5, 16, 0.5, 0.5, 0.5, 0.1);

                settlement.setGuardCount(totalMilitary + 1);
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Enlistment: " + guardName + " equipped from Armory as Village Archer in " + settlement.getName());
                return;
            }
        }

        // Melee Guard Recruitment
        if (!armory.hasMeleeGuardGear()) {
            // Insufficient sword/shield/armor in armory stock
            return;
        }
        armory.consumeMeleeGuardGear();

        Villager candidate = availableCivilians.get(level.getRandom().nextInt(availableCivilians.size()));
        BlockPos recruitPos = candidate.blockPosition();

        String guardName = candidate.getCustomName() != null ? candidate.getCustomName().getString() : "Gareth";
        guardName = guardName.replace("§e", "").replaceAll(" the .*", "");

        VillageGuardEntity guard = LivingWorldEntities.VILLAGE_GUARD.create(level, EntitySpawnReason.EVENT);
        if (guard != null) {
            guard.setPos(recruitPos.getX() + 0.5, recruitPos.getY(), recruitPos.getZ() + 0.5);
            guard.setHomeSettlementId(settlement.getId().toString());

            GuardRecord record = new GuardRecord(guard.getUUID(), settlement.getId(), guardName, GuardRank.RECRUIT);
            guardRecords.put(guard.getUUID(), record);

            guard.setCustomName(Component.literal("§9[Guard " + record.getRank().getTitle() + "] " + guardName));
            guard.setCustomNameVisible(true);

            AllegianceRegistry.get().register(guard.getUUID(), settlement.getId().toString(), "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.MELEE_GUARD);

            level.addFreshEntity(guard);
            candidate.discard();

            // Audio & visual recruitment ceremony
            level.playSound(null, recruitPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            level.playSound(null, recruitPos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 0.8f, 1.2f);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, recruitPos.getX() + 0.5, recruitPos.getY() + 1.2, recruitPos.getZ() + 0.5, 16, 0.5, 0.5, 0.5, 0.1);

            settlement.setGuardCount(totalMilitary + 1);
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Enlistment: " + guardName + " equipped from Armory as Village Guard in " + settlement.getName());
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

    public boolean forceRecruit(ServerLevel level, Settlement settlement, BlockPos pos, boolean isArcher) {
        if (level == null || pos == null) return false;
        String settlementId = settlement != null ? settlement.getId().toString() : UUID.randomUUID().toString();

        // Consume armory stock if available
        if (settlement != null) {
            VillageArmoryManager.ArmoryStock armory = VillageArmoryManager.get().getStock(settlement);
            if (isArcher && armory.hasArcherGear()) {
                armory.consumeArcherGear();
            } else if (!isArcher && armory.hasMeleeGuardGear()) {
                armory.consumeMeleeGuardGear();
            }
        }

        if (isArcher) {
            VillageArcherEntity archer = LivingWorldEntities.VILLAGE_ARCHER.create(level, EntitySpawnReason.COMMAND);
            if (archer != null) {
                archer.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                archer.setHomeSettlementId(settlementId);
                archer.setCustomName(Component.literal("§9[Archer Recruit] Alden"));
                archer.setCustomNameVisible(true);
                AllegianceRegistry.get().register(archer.getUUID(), settlementId, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.ARCHER);
                level.addFreshEntity(archer);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 16, 0.5, 0.5, 0.5, 0.1);
                if (settlement != null) settlement.setGuardCount(settlement.getGuardCount() + 1);
                return true;
            }
        } else {
            VillageGuardEntity guard = LivingWorldEntities.VILLAGE_GUARD.create(level, EntitySpawnReason.COMMAND);
            if (guard != null) {
                guard.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                guard.setHomeSettlementId(settlementId);
                GuardRecord record = new GuardRecord(guard.getUUID(), settlement != null ? settlement.getId() : UUID.randomUUID(), "Gareth", GuardRank.RECRUIT);
                guardRecords.put(guard.getUUID(), record);
                guard.setCustomName(Component.literal("§9[Guard Recruit] Gareth"));
                guard.setCustomNameVisible(true);
                AllegianceRegistry.get().register(guard.getUUID(), settlementId, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.MELEE_GUARD);
                level.addFreshEntity(guard);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 16, 0.5, 0.5, 0.5, 0.1);
                if (settlement != null) settlement.setGuardCount(settlement.getGuardCount() + 1);
                return true;
            }
        }
        return false;
    }

    public boolean forceRecruitCavalry(ServerLevel level, Settlement settlement, BlockPos pos, livingworld.entity.VillageCavalryEntity.CavalryRole role) {
        if (level == null || pos == null) return false;
        String settlementId = settlement != null ? settlement.getId().toString() : UUID.randomUUID().toString();

        livingworld.entity.VillageCavalryEntity cavalry = LivingWorldEntities.VILLAGE_CAVALRY.create(level, EntitySpawnReason.COMMAND);
        if (cavalry != null) {
            cavalry.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            cavalry.setHomeSettlementId(settlementId);
            cavalry.setCavalryRole(role != null ? role : livingworld.entity.VillageCavalryEntity.CavalryRole.MOUNTED_SWORDSMAN);
            cavalry.setCustomNameVisible(true);
            AllegianceRegistry.get().register(cavalry.getUUID(), settlementId, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.MELEE_GUARD);

            level.addFreshEntity(cavalry);
            cavalry.spawnAndMountHorse(level);

            level.playSound(null, pos, SoundEvents.HORSE_ARMOR.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 16, 0.5, 0.5, 0.5, 0.1);
            if (settlement != null) settlement.setGuardCount(settlement.getGuardCount() + 1);
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Cavalry Enlistment: Mounted unit mobilized in " + (settlement != null ? settlement.getName() : "Wilderness"));
            return true;
        }
        return false;
    }

    public boolean forceSpawnDog(ServerLevel level, Settlement settlement, BlockPos pos, livingworld.entity.VillageGuardEntity handler) {
        if (level == null || pos == null) return false;
        String settlementId = settlement != null ? settlement.getId().toString() : UUID.randomUUID().toString();

        livingworld.entity.VillageDogEntity dog = LivingWorldEntities.VILLAGE_DOG.create(level, EntitySpawnReason.COMMAND);
        if (dog != null) {
            dog.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            dog.setHomeSettlementId(settlementId);
            if (handler != null) {
                dog.pairWithGuard(handler);
                handler.setCompanionDogUuid(dog.getUUID());
            } else {
                dog.setRole(livingworld.entity.VillageDogEntity.DogRole.CIVILIAN_PET);
            }
            level.addFreshEntity(dog);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_WOLF.value(), SoundSource.NEUTRAL, 1.0f, 1.1f);
            level.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
            return true;
        }
        return false;
    }

    public void clear() {
        guardRecords.clear();
    }
}
