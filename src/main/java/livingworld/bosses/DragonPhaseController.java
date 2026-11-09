package livingworld.bosses;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import livingworld.config.FeatureConfig;
import livingworld.entity.EndVoidStalkerEntity;
import livingworld.entity.LivingWorldEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.Endermite;

/**
 * Multi-Phase Ender Dragon Controller & Outer End Encounters (F13).
 */
public class DragonPhaseController {
    private static final DragonPhaseController INSTANCE = new DragonPhaseController();
    private final Set<UUID> phase2Triggered = new HashSet<>();
    private final Set<UUID> phase3Triggered = new HashSet<>();
    private long lastOuterEndCheck = 0;

    public static DragonPhaseController get() {
        return INSTANCE;
    }

    public void tickDragonPhases(ServerLevel level) {
        if (!FeatureConfig.get().enableDragonEnhancements) {
            return;
        }

        // Only in End dimension
        if (level.dimension() != net.minecraft.world.level.Level.END) {
            return;
        }

        // 1. Central Dragon Arena Boss Combat Phases
        List<EnderDragon> dragons = level.getEntitiesOfClass(EnderDragon.class,
            new net.minecraft.world.phys.AABB(-300, 0, -300, 300, 256, 300), d -> d.isAlive());

        for (EnderDragon dragon : dragons) {
            UUID id = dragon.getUUID();
            float hpRatio = dragon.getHealth() / dragon.getMaxHealth();

            // Phase 2: Ground Assault & Swarm Summon (under 60% HP)
            if (hpRatio < 0.60f && !phase2Triggered.contains(id)) {
                phase2Triggered.add(id);
                dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING);

                // Summon Endermite distraction swarm near fountain
                for (int i = 0; i < 4; i++) {
                    Endermite mite = EntityTypes.ENDERMITE.create(level, EntitySpawnReason.REINFORCEMENT);
                    if (mite != null) {
                        mite.setPos(level.getRandom().nextInt(10) - 5 + 0.5, 65, level.getRandom().nextInt(10) - 5 + 0.5);
                        level.addFreshEntity(mite);
                    }
                }

                notifyEndPlayers(level, "§d[Living World] The Ender Dragon descends to perch! Endermites erupt from the void!");
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Ender Dragon entered Phase 2 (Landing Assault & Endermite Swarm)!");
            }

            // Phase 3: Enraged Dive Bomb Frenzy (under 25% HP)
            if (hpRatio < 0.25f && !phase3Triggered.contains(id)) {
                phase3Triggered.add(id);
                dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
                notifyEndPlayers(level, "§4[Living World] §cThe Ender Dragon enters an ENRAGED frenzy, diving relentlessly!");
                livingworld.LivingWorldMod.LOGGER.info("[Living World] Ender Dragon entered Phase 3 (Enraged Charge Frenzy)!");
            }
        }

        // 2. Outer End Natural Hostile Encounter (F13 - Outside the Dragon Arena > 800 blocks)
        long gameTime = level.getGameTime();
        if (gameTime - lastOuterEndCheck > 400) { // Every 20s
            lastOuterEndCheck = gameTime;
            for (ServerPlayer player : level.players()) {
                double distSqFromCenter = player.position().horizontalDistanceSqr();
                if (distSqFromCenter > 640000.0) { // Beyond 800 blocks (Outer Islands)
                    int stalkerCount = level.getEntitiesOfClass(EndVoidStalkerEntity.class, player.getBoundingBox().inflate(48.0), s -> s.isAlive()).size();
                    if (stalkerCount < 2 && level.getRandom().nextInt(2) == 0) {
                        BlockPos spawnPos = player.blockPosition().offset(level.getRandom().nextInt(20) - 10, 0, level.getRandom().nextInt(20) - 10);
                        EndVoidStalkerEntity stalker = LivingWorldEntities.END_VOID_STALKER.create(level, EntitySpawnReason.NATURAL);
                        if (stalker != null) {
                            stalker.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                            level.addFreshEntity(stalker);
                            livingworld.LivingWorldMod.LOGGER.info("[Living World] Outer End encounter: End Void Stalker naturally spawned at " + spawnPos);
                        }
                    }
                }
            }
        }
    }

    private void notifyEndPlayers(ServerLevel level, String message) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(message));
        }
    }
}
