package livingworld.entity;

import java.util.UUID;
import livingworld.faction.AllegianceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Dedicated Village Cavalry Unit (Section 10; BUG-12).
 * Fast-moving mounted military unit for patrols, mobile combat, and rapid response:
 * - Mounted Swordsman: High-speed mobile patrol and sword combat
 * - Mounted Scout: Outskirts reconnaissance and early threat reporting
 * - Mounted Captain: Coordinates cavalry formations and heavy responses
 * - Rides tamed, saddled, and armored Horses with persistent identity
 * - Safe dismount: continues fighting on foot if mount is incapacitated
 */
public class VillageCavalryEntity extends VillageGuardEntity {
    public enum CavalryRole {
        MOUNTED_SWORDSMAN("Mounted Swordsman"),
        MOUNTED_SCOUT("Mounted Scout"),
        MOUNTED_CAPTAIN("Mounted Captain");

        private final String roleName;

        CavalryRole(String roleName) {
            this.roleName = roleName;
        }

        public String getRoleName() {
            return roleName;
        }
    }

    private CavalryRole cavalryRole = CavalryRole.MOUNTED_SWORDSMAN;
    private UUID horseUuid = null;
    private int mountCheckCooldown = 0;

    public VillageCavalryEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("§9Mounted Swordsman"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 36.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.FOLLOW_RANGE, 52.0)
            .add(Attributes.ARMOR, 16.0)
            .add(Attributes.ARMOR_TOUGHNESS, 2.0);
    }

    public void setCavalryRole(CavalryRole role) {
        this.cavalryRole = role;
        switch (role) {
            case MOUNTED_SCOUT -> {
                this.setCustomName(Component.literal("§bMounted Scout"));
                this.setMaxPursuitDistance(48.0);
            }
            case MOUNTED_CAPTAIN -> {
                this.setCustomName(Component.literal("§6Cavalry Captain"));
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                this.setMaxPursuitDistance(36.0);
            }
            default -> {
                this.setCustomName(Component.literal("§9Mounted Swordsman"));
                this.setMaxPursuitDistance(32.0);
            }
        }
    }

    public CavalryRole getCavalryRole() {
        return cavalryRole;
    }

    public UUID getHorseUuid() {
        return horseUuid;
    }

    public void setHorseUuid(UUID horseUuid) {
        this.horseUuid = horseUuid;
    }

    /**
     * Spawns an armored, saddled horse and mounts it.
     */
    public Horse spawnAndMountHorse(ServerLevel level) {
        Horse horse = EntityTypes.HORSE.create(level, EntitySpawnReason.EVENT);
        if (horse == null) return null;

        horse.setPos(this.getX(), this.getY(), this.getZ());
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        horse.setItemSlot(EquipmentSlot.BODY, new ItemStack(
            cavalryRole == CavalryRole.MOUNTED_CAPTAIN ? Items.DIAMOND_HORSE_ARMOR : Items.IRON_HORSE_ARMOR));
        horse.setPersistenceRequired();

        level.addFreshEntity(horse);
        this.startRiding(horse);
        this.horseUuid = horse.getUUID();

        AllegianceRegistry.get().register(horse.getUUID(), this.getHomeSettlementId(), "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.CIVILIAN_SUPPORT);
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Cavalry mount mobilized: Horse " + horse.getUUID() + " assigned to Cavalry " + this.getUUID());
        return horse;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Mounted Navigation Steering: Rider directs horse movement towards targets
            if (this.isPassenger() && this.getVehicle() instanceof Horse horse) {
                LivingEntity combatTarget = this.getTarget();
                if (combatTarget != null && combatTarget.isAlive()) {
                    horse.getNavigation().moveTo(combatTarget, 1.35);
                    horse.getLookControl().setLookAt(combatTarget, 30.0f, 30.0f);
                    this.getLookControl().setLookAt(combatTarget, 30.0f, 30.0f);
                } else if (this.getNavigation().isInProgress()) {
                    var path = this.getNavigation().getPath();
                    if (path != null && !horse.getNavigation().isInProgress()) {
                        horse.getNavigation().moveTo(path, 1.15);
                    }
                }
            }

            // Check mount association & remount logic
            if (++mountCheckCooldown >= 40) {
                mountCheckCooldown = 0;
                if (!this.isPassenger() && this.horseUuid != null) {
                    Entity mount = serverLevel.getEntity(this.horseUuid);
                    if (mount instanceof Horse h && h.isAlive()) {
                        double distSq = this.distanceToSqr(h);
                        if (distSq < 9.0) {
                            this.startRiding(h);
                        } else if (distSq < 256.0 && this.getTarget() == null) {
                            this.getNavigation().moveTo(h, 1.3);
                        }
                    } else {
                        // Mount was destroyed: continue combat on foot cleanly as infantry
                        this.horseUuid = null;
                        this.setCustomName(Component.literal("§9Veteran Swordsman (Infantry)"));
                    }
                }
            }
        }
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        if (this.horseUuid != null && !(this.level().isClientSide())) {
            livingworld.LivingWorldMod.LOGGER.debug("[Living World] Cavalry " + this.getUUID() + " dismounted, transitioning to foot combat.");
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("CavalryRole", this.cavalryRole.name());
        output.putString("HorseUuid", this.horseUuid != null ? this.horseUuid.toString() : "");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String roleStr = input.getStringOr("CavalryRole", "MOUNTED_SWORDSMAN");
        try {
            this.cavalryRole = CavalryRole.valueOf(roleStr);
        } catch (Exception ignored) {}

        String hStr = input.getStringOr("HorseUuid", "");
        if (!hStr.isEmpty()) {
            try {
                this.horseUuid = UUID.fromString(hStr);
            } catch (Exception ignored) {}
        }
    }
}
