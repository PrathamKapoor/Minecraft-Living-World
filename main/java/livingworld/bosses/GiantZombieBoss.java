package livingworld.bosses;

import java.util.*;
import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GiantZombieBoss {
    private static final Map<UUID, GiantZombieBoss> ACTIVE_BOSSES = new HashMap<>();

    private final Giant giant;
    private final ServerBossEvent bossEvent;
    private int groundSlamCooldown = 100;
    private boolean enrageTriggered = false;

    public GiantZombieBoss(Giant giant) {
        this.giant = giant;
        this.bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.literal("§4Colossal Dreadbeast"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
        );

        setupGiantAttributesAndGoals();
    }

    private void setupGiantAttributesAndGoals() {
        giant.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        giant.setHealth(200.0f);
        giant.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(14.0);
        giant.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
        giant.setCustomName(Component.literal("§4Colossal Dreadbeast"));
        giant.setCustomNameVisible(true);

        // Attach active AI goals
        giant.getGoalSelector().addGoal(0, new FloatGoal(giant));
        giant.getGoalSelector().addGoal(1, new MeleeAttackGoal(giant, 1.15, false));
        giant.getGoalSelector().addGoal(2, new WaterAvoidingRandomStrollGoal(giant, 0.8));
    }

    public static boolean hasActiveBoss() {
        return !ACTIVE_BOSSES.isEmpty();
    }

    public static Giant spawnBoss(ServerLevel level, BlockPos pos) {
        Giant giant = EntityTypes.GIANT.create(level, EntitySpawnReason.EVENT);
        if (giant == null) return null;

        giant.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        level.addFreshEntity(giant);

        GiantZombieBoss boss = new GiantZombieBoss(giant);
        ACTIVE_BOSSES.put(giant.getUUID(), boss);
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Boss Awakened: Colossal Dreadbeast spawned at " + pos + " (HP: 200, BossBar activated)!");

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(giant) < 6400.0) {
                player.sendSystemMessage(Component.literal("§4[Living World] An earth-shaking roar echoes across the land! The Colossal Dreadbeast has arisen!"));
            }
        }
        return giant;
    }

    public static void tickAll(ServerLevel level) {
        Iterator<Map.Entry<UUID, GiantZombieBoss>> it = ACTIVE_BOSSES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, GiantZombieBoss> entry = it.next();
            GiantZombieBoss boss = entry.getValue();

            if (!boss.giant.isAlive()) {
                boss.onDefeat(level);
                it.remove();
                continue;
            }

            boss.tick(level);
        }
    }

    public void tick(ServerLevel level) {
        // Update Boss Bar
        bossEvent.setProgress(giant.getHealth() / giant.getMaxHealth());

        // Manage Boss Bar players
        Set<ServerPlayer> nearbyPlayers = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(giant) < 4096.0) { // 64 blocks
                nearbyPlayers.add(player);
                if (!bossEvent.getPlayers().contains(player)) {
                    bossEvent.addPlayer(player);
                }
            }
        }
        for (ServerPlayer tracked : new ArrayList<>(bossEvent.getPlayers())) {
            if (!nearbyPlayers.contains(tracked)) {
                bossEvent.removePlayer(tracked);
            }
        }

        // Target acquisition
        if (giant.getTarget() == null || !giant.getTarget().isAlive()) {
            Player nearest = level.getNearestPlayer(giant, 32.0);
            if (nearest != null && nearest.isAlive()) {
                giant.setTarget(nearest);
            }
        }

        // Phase 2 Enrage Check (under 50% health)
        if (!enrageTriggered && giant.getHealth() < giant.getMaxHealth() * 0.5f) {
            enrageTriggered = true;
            bossEvent.setName(Component.literal("§cColossal Dreadbeast (ENRAGED)"));
            giant.addEffect(new MobEffectInstance(MobEffects.SPEED, 12000, 1));
            giant.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 12000, 0));

            // Summon 2 armored bodyguard minions
            for (int i = 0; i < 2; i++) {
                Zombie minion = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.REINFORCEMENT);
                if (minion != null) {
                    minion.setPos(giant.getX() + (i == 0 ? 3 : -3), giant.getY(), giant.getZ());
                    minion.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    minion.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                    level.addFreshEntity(minion);
                }
            }
        }

        // Ground Slam Attack
        groundSlamCooldown--;
        if (groundSlamCooldown <= 0 && giant.getTarget() != null) {
            executeGroundSlam(level);
            groundSlamCooldown = enrageTriggered ? 120 : 180;
        }
    }

    private void executeGroundSlam(ServerLevel level) {
        AABB slamArea = giant.getBoundingBox().inflate(8.0);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, slamArea,
            e -> e != giant && !(e instanceof Zombie));

        for (LivingEntity victim : victims) {
            Vec3 knockback = victim.position().subtract(giant.position()).normalize().add(0, 0.45, 0);
            victim.setDeltaMovement(knockback.scale(1.4));
            victim.hurt(giant.damageSources().mobAttack(giant), enrageTriggered ? 16.0f : 10.0f);
        }

        level.levelEvent(2001, giant.blockPosition(), 3); // Dirt block break effect
    }

    private void onDefeat(ServerLevel level) {
        bossEvent.removeAllPlayers();

        // Drop authentic deterministic boss rewards
        BlockPos dropPos = giant.blockPosition();
        level.addFreshEntity(new ItemEntity(level, dropPos.getX() + 0.5, dropPos.getY() + 1.0, dropPos.getZ() + 0.5, new ItemStack(Items.NETHERITE_SCRAP, 2)));
        level.addFreshEntity(new ItemEntity(level, dropPos.getX() + 0.5, dropPos.getY() + 1.0, dropPos.getZ() + 0.5, new ItemStack(Items.DIAMOND, 3)));
        level.addFreshEntity(new ItemEntity(level, dropPos.getX() + 0.5, dropPos.getY() + 1.0, dropPos.getZ() + 0.5, new ItemStack(Items.EMERALD, 12)));

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(giant) < 6400.0) {
                player.sendSystemMessage(Component.literal("§6[Living World] §aThe Colossal Dreadbeast has been vanquished!"));
            }
        }
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Boss Defeated: Colossal Dreadbeast vanquished at " + dropPos + "! Dropped Netherite Scrap, Diamonds, and Emeralds.");
    }
}
