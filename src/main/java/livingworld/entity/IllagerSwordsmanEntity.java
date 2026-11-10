package livingworld.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Genuine Illager Swordsman Entity (Section 14, F04).
 * Fast-moving melee frontline raider wielding an Iron Sword with flanking maneuvers.
 */
public class IllagerSwordsmanEntity extends AbstractIllager {

    public IllagerSwordsmanEntity(EntityType<? extends AbstractIllager> entityType, Level level) {
        super(entityType, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.085f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 26.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FOLLOW_RANGE, 28.0)
            .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RaiderOpenDoorGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, AbstractIllager.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, VillageGuardEntity.class, true));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, VillageArcherEntity.class, true));
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean unused) {
        if (wave > 3) {
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.PILLAGER_CELEBRATE;
    }

    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        if (this.isAggressive()) {
            return AbstractIllager.IllagerArmPose.ATTACKING;
        }
        return AbstractIllager.IllagerArmPose.NEUTRAL;
    }

    static class RaiderOpenDoorGoal extends net.minecraft.world.entity.ai.goal.OpenDoorGoal {
        public RaiderOpenDoorGoal(Raider raider) {
            super(raider, false);
        }

        @Override
        public boolean canUse() {
            return super.canUse();
        }
    }
}
