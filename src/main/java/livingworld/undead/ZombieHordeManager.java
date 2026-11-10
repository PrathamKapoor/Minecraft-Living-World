package livingworld.undead;

import java.util.*;
import livingworld.config.FeatureConfig;
import livingworld.entity.LivingWorldEntities;
import livingworld.entity.MutantZombieCommanderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Cohesive Zombie Horde & Boss Encounter Manager (Fix 4, Section 16 & Milestone 2).
 * Guarantees atomic spawning of true 8-16 member hordes led by the 3-headed Mutant Zombie Commander.
 * Enforces strict land foundation checks (no oceans, rivers, or water top spawns).
 */
public class ZombieHordeManager {
    private static final ZombieHordeManager INSTANCE = new ZombieHordeManager();
    private final Set<UUID> activeCommanders = new HashSet<>();
    private long lastHordeSpawnAttempt = 0;

    public static ZombieHordeManager get() {
        return INSTANCE;
    }

    public Set<UUID> getActiveCommanders() {
        return Collections.unmodifiableSet(activeCommanders);
    }

    public void tick(ServerLevel level, long currentTicks) {
        if (!FeatureConfig.get().enableZombieHordes) {
            return;
        }

        // Clean dead commanders
        activeCommanders.removeIf(uuid -> level.getEntity(uuid) == null || !level.getEntity(uuid).isAlive());

        // Horde Commander Aura buff and target synchronization
        for (UUID cmdUuid : new ArrayList<>(activeCommanders)) {
            net.minecraft.world.entity.Entity cmdEntity = level.getEntity(cmdUuid);
            if (cmdEntity instanceof LivingEntity commander && commander.isAlive()) {
                AABB auraBox = commander.getBoundingBox().inflate(16.0);
                List<Zombie> followers = level.getEntitiesOfClass(Zombie.class, auraBox,
                    z -> z.isAlive() && !z.getUUID().equals(cmdUuid));

                LivingEntity commanderTarget = (commander instanceof Mob mob) ? mob.getTarget() : null;

                for (Zombie follower : followers) {
                    follower.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, false, false));
                    follower.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, false, false));
                    if (commanderTarget != null && commanderTarget.isAlive() && follower.getTarget() == null) {
                        follower.setTarget(commanderTarget);
                    }
                }
            }
        }

        // Night Horde Spawning (capped)
        if (currentTicks - lastHordeSpawnAttempt > 1200) { // Every 60 seconds
            lastHordeSpawnAttempt = currentTicks;
            if (level.isDarkOutside() && activeCommanders.size() < FeatureConfig.get().maxActiveHordesPerWorld) {
                for (ServerPlayer player : level.players()) {
                    livingworld.settlement.Settlement near = livingworld.settlement.SettlementManager.get().getNearestSettlement(player.blockPosition());
                    // Underdeveloped village protection: reduce horde spawn frequency if population < 8
                    int spawnChance = (near != null && near.getPopulation() < 8) ? 6 : 3;
                    if (level.getRandom().nextInt(spawnChance) == 0) {
                        spawnHordeNear(level, player.blockPosition());
                        break;
                    }
                }
            }

            // Natural Colossal Dreadbeast encounter during Full Moon
            if (FeatureConfig.get().enableGiantZombieBoss && !livingworld.bosses.GiantZombieBoss.hasActiveBoss()) {
                boolean isFullMoon = ((int) (level.getOverworldClockTime() / 24000L % 8L + 8L) % 8L == 0);
                if (level.isDarkOutside() && isFullMoon && level.getRandom().nextInt(3) == 0) {
                    for (ServerPlayer player : level.players()) {
                        BlockPos pPos = player.blockPosition();
                        int ox = (level.getRandom().nextInt(20) + 20) * (level.getRandom().nextBoolean() ? 1 : -1);
                        int oz = (level.getRandom().nextInt(20) + 20) * (level.getRandom().nextBoolean() ? 1 : -1);
                        int oy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pPos.getX() + ox, pPos.getZ() + oz);
                        BlockPos bossPos = new BlockPos(pPos.getX() + ox, oy, pPos.getZ() + oz);
                        if (isValidDryLand(level, bossPos)) {
                            livingworld.bosses.GiantZombieBoss.spawnBoss(level, bossPos);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean isValidDryLand(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        if (pos.getY() < 62) return false;

        var biome = level.getBiome(pos);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
            return false;
        }

        BlockPos groundPos = pos.below();
        var ground = level.getBlockState(groundPos);
        var groundFluid = level.getFluidState(groundPos);
        var spawnFluid = level.getFluidState(pos);

        if (!ground.isSolid() || !groundFluid.isEmpty() || !spawnFluid.isEmpty()) {
            return false;
        }

        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private List<BlockPos> findValidSpawnPositions(ServerLevel level, BlockPos origin, int minRequired, int maxDesired) {
        List<BlockPos> validPositions = new ArrayList<>();
        Random rand = new Random(origin.asLong() ^ level.getGameTime());

        int originAngle = rand.nextInt(360);
        int distance = 25 + rand.nextInt(20); // 25-45 blocks away from origin
        int centerX = origin.getX() + (int) (Math.cos(Math.toRadians(originAngle)) * distance);
        int centerZ = origin.getZ() + (int) (Math.sin(Math.toRadians(originAngle)) * distance);

        for (int attempts = 0; attempts < 60 && validPositions.size() < maxDesired; attempts++) {
            int offsetX = rand.nextInt(15) - 7;
            int offsetZ = rand.nextInt(15) - 7;
            int testX = centerX + offsetX;
            int testZ = centerZ + offsetZ;

            int testY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, testX, testZ);
            BlockPos testPos = new BlockPos(testX, testY, testZ);

            if (isValidDryLand(level, testPos)) {
                validPositions.add(testPos);
            }
        }

        return validPositions;
    }

    public void spawnHordeNear(ServerLevel level, BlockPos origin) {
        // Enforce atomic horde requirement: 8 to 16 zombies
        int desiredSize = 10 + level.getRandom().nextInt(5); // 10 to 14 zombies
        List<BlockPos> positions = findValidSpawnPositions(level, origin, 8, desiredSize);

        if (positions.size() < 8) {
            // Abort: Unable to find valid dry terrain for full horde. Never spawn 1-zombie hordes!
            return;
        }

        int totalSpawned = 0;

        // 1. Leader: Mutant Zombie Commander (3 heads, 180 HP, single active boss bar)
        // Ensure no other active commander exists within 96 blocks (BUG-15 Dedup)
        net.minecraft.world.phys.AABB commanderCheckArea = net.minecraft.world.phys.AABB.ofSize(new net.minecraft.world.phys.Vec3(origin.getX(), origin.getY(), origin.getZ()), 192, 48, 192);
        List<MutantZombieCommanderEntity> existingCommanders = level.getEntitiesOfClass(MutantZombieCommanderEntity.class, commanderCheckArea, c -> c.isAlive());
        if (!existingCommanders.isEmpty()) {
            return; // Commander already active in this zone, prevent duplicate boss bars
        }

        BlockPos leaderPos = positions.get(0);
        MutantZombieCommanderEntity commander = LivingWorldEntities.MUTANT_COMMANDER.create(level, EntitySpawnReason.EVENT);
        if (commander == null) {
            return;
        }

        commander.setPos(leaderPos.getX() + 0.5, leaderPos.getY(), leaderPos.getZ() + 0.5);
        level.addFreshEntity(commander);
        activeCommanders.add(commander.getUUID());
        totalSpawned++;

        int posIdx = 1;

        // 2. 2-3 Armored Shocktroopers (Iron/Chainmail gear)
        int armoredCount = 2 + level.getRandom().nextInt(2);
        for (int i = 0; i < armoredCount && posIdx < positions.size(); i++, posIdx++) {
            BlockPos p = positions.get(posIdx);
            Zombie armored = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
            if (armored != null) {
                armored.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                armored.setCustomName(Component.literal("§cArmored Horde Soldier"));
                armored.setCustomNameVisible(false);
                armored.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40.0);
                armored.setHealth(40.0f);
                armored.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(4.0);
                armored.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(4.5); // Rebalanced: ~10% damage tuning
                armored.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                armored.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                level.addFreshEntity(armored);
                totalSpawned++;
            }
        }

        // 3. 2-3 Tool-Wielding Miners (Equipped with pickaxes/axes/shovels + mining AI)
        int minerCount = 2 + level.getRandom().nextInt(2);
        for (int i = 0; i < minerCount && posIdx < positions.size(); i++, posIdx++) {
            BlockPos p = positions.get(posIdx);
            Zombie miner = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
            if (miner != null) {
                miner.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                miner.setCustomName(Component.literal("§cZombie Siege Miner"));
                miner.setCustomNameVisible(false);
                miner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(32.0);
                miner.setHealth(32.0f);
                miner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(2.0);
                miner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(3.6); // Rebalanced ~10%
                ItemStack tool = (i % 2 == 0) ? new ItemStack(Items.IRON_PICKAXE) : new ItemStack(Items.IRON_AXE);
                miner.setItemSlot(EquipmentSlot.MAINHAND, tool);
                miner.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                miner.addTag("livingworld:tool_miner");
                miner.addTag("livingworld:evaluated_tool");
                miner.getGoalSelector().addGoal(1, new livingworld.ai.ZombieSiegeMiningGoal(miner));
                level.addFreshEntity(miner);
                totalSpawned++;
            }
        }

        // 3b. Rare Elite Siege Brute accompaniment (1 in 8 hordes if Mutant Monsters installed)
        if (level.getRandom().nextInt(8) == 0 && posIdx < positions.size()) {
            var optType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("mutantmonsters", "mutant_zombie"));
            if (optType.isPresent()) {
                BlockPos p = positions.get(posIdx++);
                net.minecraft.world.entity.Entity brute = optType.get().create(level, EntitySpawnReason.EVENT);
                if (brute != null) {
                    brute.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                    level.addFreshEntity(brute);
                    totalSpawned++;
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Rare Mutant Zombie brute accompanying horde at " + p);
                }
            }
        }

        // 4. Remaining followers: Regular Horde Zombies
        while (posIdx < positions.size()) {
            BlockPos p = positions.get(posIdx++);
            Zombie regular = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
            if (regular != null) {
                regular.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                regular.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(24.0);
                regular.setHealth(24.0f);
                regular.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(2.7); // Rebalanced ~10%
                level.addFreshEntity(regular);
                totalSpawned++;
            }
        }

        livingworld.LivingWorldMod.LOGGER.info("[Living World] Atomic Night Horde successfully spawned at " + leaderPos +
            " led by Mutant Zombie Commander. Total verified members: " + totalSpawned + " (Armored: " + armoredCount +
            ", Miners: " + minerCount + ").");
    }

    public void onEntityDeath(ServerLevel level, UUID deadUuid) {
        if (activeCommanders.remove(deadUuid)) {
            livingworld.LivingWorldMod.LOGGER.info("[Living World] Horde Commander defeated! Followers scattered in panic.");
            net.minecraft.world.entity.Entity deadEnt = level.getEntity(deadUuid);
            if (deadEnt != null) {
                AABB auraBox = deadEnt.getBoundingBox().inflate(24.0);
                List<Zombie> followers = level.getEntitiesOfClass(Zombie.class, auraBox, z -> z.isAlive());
                for (Zombie f : followers) {
                    f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1));
                    f.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 1));
                    f.setTarget(null);
                    f.getNavigation().moveTo(f.getX() + (f.getX() - deadEnt.getX()) * 2, f.getY(), f.getZ() + (f.getZ() - deadEnt.getZ()) * 2, 1.25);
                }
            }
        }
    }
}
