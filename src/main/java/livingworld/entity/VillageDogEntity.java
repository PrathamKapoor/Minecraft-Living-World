package livingworld.entity;

import java.util.UUID;
import livingworld.faction.AllegianceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Dedicated Village Dog & Guard Companion Entity (Section 10; BUG-12).
 * - Civilian Pet: Wanders residential districts, rests near homes, interacts peacefully with villagers.
 * - Guard Companion: Assigned to a specific military guard/archer handler via UUID. Follows handler,
 *   screens flankers, defends handler in combat, and returns to handler after skirmishes.
 * - Centralized Allegiance: Zero friendly-fire with villagers, guards, or golems.
 * - Complete NBT persistence and reload stability.
 */
public class VillageDogEntity extends Wolf {
    private String homeSettlementId = "";
    private UUID guardHandlerUuid = null;
    private DogRole role = DogRole.CIVILIAN_PET;
    private int followCheckCooldown = 0;

    public enum DogRole {
        CIVILIAN_PET,
        GUARD_COMPANION
    }

    public VillageDogEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired(); // Persistent village inhabitant (BUG-06/BUG-12)
        this.setCustomName(Component.literal("§6Village Dog"));
        this.setCustomNameVisible(false);

        if (!level.isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), homeSettlementId, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.CIVILIAN_SUPPORT);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Priority 1: Defend assigned guard handler
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                LivingEntity attacker = VillageDogEntity.this.getLastHurtByMob();
                return attacker != null && !AllegianceRegistry.get().areAllies(VillageDogEntity.this, attacker);
            }
        });

        // Priority 2: Engage hostiles targeting handler or civilians
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            (target, lvl) -> {
                if (target == null || !target.isAlive()) return false;
                if (AllegianceRegistry.get().areAllies(this, target)) return false;
                if (guardHandlerUuid != null) {
                    LivingEntity handler = getHandlerEntity();
                    if (handler != null && target.distanceToSqr(handler) < (16.0 * 16.0)) return true;
                }
                return (target instanceof net.minecraft.world.entity.Mob mob) &&
                    (mob.getTarget() instanceof Villager || mob.getTarget() instanceof VillageGuardEntity);
            }));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target == null) return false;
        if (AllegianceRegistry.get().areAllies(this, target)) return false;
        return super.canAttack(target);
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && AllegianceRegistry.get().areAllies(this, target)) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            if (role == DogRole.GUARD_COMPANION && guardHandlerUuid != null) {
                if (++followCheckCooldown >= 10) {
                    followCheckCooldown = 0;
                    LivingEntity handler = getHandlerEntity();
                    if (handler != null && handler.isAlive()) {
                        double distSq = this.distanceToSqr(handler);
                        // Follow handler closely when not in combat
                        if (this.getTarget() == null || !this.getTarget().isAlive()) {
                            if (distSq > (6.0 * 6.0)) {
                                this.getNavigation().moveTo(handler, 1.3);
                            }
                        } else {
                            // If handler attacks a target, assist handler
                            if (handler instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) {
                                LivingEntity ht = mob.getTarget();
                                if (!AllegianceRegistry.get().areAllies(this, ht)) {
                                    this.setTarget(ht);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public LivingEntity getHandlerEntity() {
        if (guardHandlerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(guardHandlerUuid);
        return entity instanceof LivingEntity le ? le : null;
    }

    public void pairWithGuard(LivingEntity guard) {
        if (guard == null) return;
        this.guardHandlerUuid = guard.getUUID();
        this.role = DogRole.GUARD_COMPANION;
        this.setCustomName(Component.literal("§6Guard Dog"));
        if (!this.level().isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), homeSettlementId, "VILLAGE_ALLIANCE", AllegianceRegistry.MilitaryRole.MELEE_GUARD);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("HomeSettlementId", this.homeSettlementId != null ? this.homeSettlementId : "");
        output.putString("DogRole", this.role.name());
        output.putString("HandlerUuid", this.guardHandlerUuid != null ? this.guardHandlerUuid.toString() : "");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.homeSettlementId = input.getStringOr("HomeSettlementId", "");
        String roleStr = input.getStringOr("DogRole", "CIVILIAN_PET");
        try {
            this.role = DogRole.valueOf(roleStr);
        } catch (Exception ignored) {}

        String handlerStr = input.getStringOr("HandlerUuid", "");
        if (!handlerStr.isEmpty()) {
            try {
                this.guardHandlerUuid = UUID.fromString(handlerStr);
            } catch (Exception ignored) {}
        }

        if (!this.homeSettlementId.isEmpty() && !this.level().isClientSide()) {
            AllegianceRegistry.get().register(this.getUUID(), homeSettlementId, "VILLAGE_ALLIANCE",
                role == DogRole.GUARD_COMPANION ? AllegianceRegistry.MilitaryRole.MELEE_GUARD : AllegianceRegistry.MilitaryRole.CIVILIAN_SUPPORT);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        if (reason == Entity.RemovalReason.KILLED) {
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Village Dog " + this.getUUID() + " killed in combat (DEATH)");
            AllegianceRegistry.get().unregister(this.getUUID());
        } else if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            livingworld.LivingWorldMod.LOGGER.debug("[Living World] Village Dog " + this.getUUID() + " safely unloaded to chunk (UNLOAD)");
        }
    }

    public String getHomeSettlementId() {
        return homeSettlementId;
    }

    public void setHomeSettlementId(String homeSettlementId) {
        this.homeSettlementId = homeSettlementId;
    }

    public UUID getGuardHandlerUuid() {
        return guardHandlerUuid;
    }

    public DogRole getRole() {
        return role;
    }

    public void setRole(DogRole role) {
        this.role = role;
    }
}
