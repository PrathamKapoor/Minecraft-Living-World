package livingworld.bosses;

import java.util.*;
import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class NetherBehemothBoss {
    private static final Map<UUID, NetherBehemothBoss> ACTIVE_BOSSES = new HashMap<>();

    private final WitherSkeleton champion;
    private final ServerBossEvent bossEvent;
    private int flameBurstCooldown = 80;

    public NetherBehemothBoss(WitherSkeleton champion) {
        this.champion = champion;
        this.bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.literal("§6Nether Flame Champion"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_10
        );

        champion.getAttribute(Attributes.MAX_HEALTH).setBaseValue(250.0);
        champion.setHealth(250.0f);
        champion.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16.0);
        champion.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
        champion.setCustomName(Component.literal("§6Nether Flame Champion"));
        champion.setCustomNameVisible(true);

        champion.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        champion.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
    }

    public static boolean hasActiveBoss() {
        return !ACTIVE_BOSSES.isEmpty();
    }

    public static WitherSkeleton summon(ServerLevel level, BlockPos pos) {
        WitherSkeleton champion = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
        if (champion == null) return null;

        champion.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(champion);

        NetherBehemothBoss boss = new NetherBehemothBoss(champion);
        ACTIVE_BOSSES.put(champion.getUUID(), boss);
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Boss Awakened: Nether Flame Champion summoned at " + pos + " (HP: 250, BossBar activated)!");

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("§4[Living World] §6A blazing Nether Flame Champion has been summoned into the Nether!"));
        }
        return champion;
    }

    public static void tickAll(ServerLevel level) {
        if (!FeatureConfig.get().enableDimensionBosses || level.dimension() != Level.NETHER) {
            return;
        }

        Iterator<Map.Entry<UUID, NetherBehemothBoss>> it = ACTIVE_BOSSES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, NetherBehemothBoss> entry = it.next();
            NetherBehemothBoss boss = entry.getValue();

            if (!boss.champion.isAlive()) {
                boss.onDefeat(level);
                it.remove();
                continue;
            }

            boss.tick(level);
        }

        // Natural ambient fortress encounter
        if (!hasActiveBoss() && level.getGameTime() % 1200 == 0) {
            for (ServerPlayer player : level.players()) {
                BlockPos pPos = player.blockPosition();
                boolean nearNetherBricks = level.getBlockState(pPos.below()).is(net.minecraft.world.level.block.Blocks.NETHER_BRICKS)
                    || level.getBlockState(pPos.below(2)).is(net.minecraft.world.level.block.Blocks.NETHER_BRICKS);
                if (nearNetherBricks && level.getRandom().nextInt(3) == 0) {
                    summon(level, pPos.relative(player.getDirection(), 8));
                    break;
                }
            }
        }
    }

    public void tick(ServerLevel level) {
        bossEvent.setProgress(champion.getHealth() / champion.getMaxHealth());

        // Target nearest player
        if (champion.getTarget() == null || !champion.getTarget().isAlive()) {
            Player nearest = level.getNearestPlayer(champion, 32.0);
            if (nearest != null && nearest.isAlive()) {
                champion.setTarget(nearest);
            }
        }

        // Update Boss Bar players
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(champion) < 3600.0) {
                if (!bossEvent.getPlayers().contains(player)) {
                    bossEvent.addPlayer(player);
                }
            } else {
                bossEvent.removePlayer(player);
            }
        }

        // Flame Burst special attack
        flameBurstCooldown--;
        if (flameBurstCooldown <= 0 && champion.getTarget() != null) {
            flameBurstCooldown = 120;
            AABB flameArea = champion.getBoundingBox().inflate(6.0);
            for (Player p : level.getEntitiesOfClass(Player.class, flameArea)) {
                p.igniteForSeconds(5.0f);
                p.hurt(champion.damageSources().mobAttack(champion), 8.0f);
            }
            level.levelEvent(2004, champion.blockPosition(), 0); // Lava burst particles
        }
    }

    private void onDefeat(ServerLevel level) {
        bossEvent.removeAllPlayers();

        // Deterministic rare rewards
        BlockPos pos = champion.blockPosition();
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, new ItemStack(Items.NETHERITE_INGOT, 1)));
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, new ItemStack(Items.ANCIENT_DEBRIS, 2)));

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("§6[Living World] §aThe Nether Flame Champion was defeated!"));
        }
    }
}
