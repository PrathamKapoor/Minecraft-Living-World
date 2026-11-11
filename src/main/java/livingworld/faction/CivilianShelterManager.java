package livingworld.faction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import livingworld.settlement.Blueprint;
import livingworld.settlement.BlueprintRegistry;
import livingworld.settlement.Settlement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Autonomous Civilian Shelter & Lockdown Machine (Section 6, v0.2.8 Stabilization).
 * State Machine Lifecycle:
 * WORKING -> THREAT_DETECTED -> SEEKING_SHELTER -> SHELTERED -> RECOVERY -> WORKING.
 * Features:
 * - Intelligent shelter selection (interior positions of valid, uncompromised residential/civic buildings).
 * - Avoids routing civilians towards the threat or through compromised structures.
 * - Door coordination: closes doors behind civilians upon entering, preventing open-close thrashing.
 * - Emergency work suspension: halts normal farming, trading, breeding, and construction during active raids.
 * - Safe recovery transition back to ordinary life once the threat is neutralized.
 */
public class CivilianShelterManager {
    private static final CivilianShelterManager INSTANCE = new CivilianShelterManager();

    public static CivilianShelterManager get() {
        return INSTANCE;
    }

    public enum CivilianState {
        WORKING,
        THREAT_DETECTED,
        SEEKING_SHELTER,
        SHELTERED,
        RECOVERY
    }

    public static class CivilianRecord {
        private final UUID villagerUuid;
        private CivilianState state = CivilianState.WORKING;
        private BlockPos assignedShelterPos = null;
        private long stateChangeTick = 0;
        private int shelterSearchCooldown = 0;

        public CivilianRecord(UUID villagerUuid, long tick) {
            this.villagerUuid = villagerUuid;
            this.stateChangeTick = tick;
        }

        public UUID getVillagerUuid() { return villagerUuid; }
        public CivilianState getState() { return state; }
        public void setState(CivilianState state, long tick) {
            this.state = state;
            this.stateChangeTick = tick;
        }
        public BlockPos getAssignedShelterPos() { return assignedShelterPos; }
        public void setAssignedShelterPos(BlockPos pos) { this.assignedShelterPos = pos; }
    }

    private final Map<UUID, CivilianRecord> civilianRecords = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> doorToggleCooldowns = new ConcurrentHashMap<>();

    public CivilianRecord getRecord(UUID villagerUuid, long tick) {
        return civilianRecords.computeIfAbsent(villagerUuid, id -> new CivilianRecord(id, tick));
    }

    public CivilianState getCivilianState(UUID villagerUuid) {
        CivilianRecord r = civilianRecords.get(villagerUuid);
        return r != null ? r.getState() : CivilianState.WORKING;
    }

    /**
     * Ticks civilian shelter machine for a settlement.
     */
    public void tickCivilianShelters(ServerLevel level, Settlement settlement, boolean isUnderAttack, BlockPos primaryThreatPos) {
        if (level == null || settlement == null) return;
        long currentTick = level.getGameTime();

        BlockPos center = settlement.getCenter();
        AABB villageZone = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()),
            settlement.getRadius() * 2.2, 28, settlement.getRadius() * 2.2);

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, villageZone, v -> v.isAlive());
        List<Monster> activeThreats = level.getEntitiesOfClass(Monster.class, villageZone,
            m -> m.isAlive() && !AllegianceRegistry.get().isVillageDefender(m));

        for (Villager villager : villagers) {
            CivilianRecord record = getRecord(villager.getUUID(), currentTick);

            if (isUnderAttack) {
                switch (record.getState()) {
                    case WORKING -> {
                        // Threat detected: transition to SEEKING_SHELTER
                        record.setState(CivilianState.THREAT_DETECTED, currentTick);
                        selectAndNavigateToShelter(level, settlement, villager, record, activeThreats, primaryThreatPos);
                    }
                    case THREAT_DETECTED -> {
                        record.setState(CivilianState.SEEKING_SHELTER, currentTick);
                        selectAndNavigateToShelter(level, settlement, villager, record, activeThreats, primaryThreatPos);
                    }
                    case SEEKING_SHELTER -> {
                        // Check if villager has reached shelter
                        BlockPos currentPos = villager.blockPosition();
                        boolean underRoof = !level.canSeeSky(currentPos);
                        boolean nearShelter = record.getAssignedShelterPos() != null &&
                            currentPos.distSqr(record.getAssignedShelterPos()) <= 16.0;

                        if (underRoof && (nearShelter || isSafeInterior(level, currentPos, activeThreats))) {
                            // Successfully reached shelter!
                            record.setState(CivilianState.SHELTERED, currentTick);
                            villager.getNavigation().stop();

                            // Close entrance door behind if applicable
                            coordinateDoorClosure(level, currentPos);
                        } else {
                            // Periodic re-navigation every 25 ticks
                            if (++record.shelterSearchCooldown >= 25) {
                                record.shelterSearchCooldown = 0;
                                selectAndNavigateToShelter(level, settlement, villager, record, activeThreats, primaryThreatPos);
                            }
                        }
                    }
                    case SHELTERED -> {
                        // Keep villager inside safe building
                        if (level.canSeeSky(villager.blockPosition())) {
                            // Slipped outside: return to shelter
                            record.setState(CivilianState.SEEKING_SHELTER, currentTick);
                        } else {
                            // Hold position inside
                            if (villager.getNavigation().isInProgress()) {
                                villager.getNavigation().stop();
                            }
                        }
                    }
                    case RECOVERY -> {
                        // Threat re-emerged while in recovery
                        record.setState(CivilianState.SEEKING_SHELTER, currentTick);
                    }
                }
            } else {
                // Not under attack
                if (record.getState() == CivilianState.SHELTERED || record.getState() == CivilianState.SEEKING_SHELTER) {
                    record.setState(CivilianState.RECOVERY, currentTick);
                } else if (record.getState() == CivilianState.RECOVERY) {
                    if (currentTick - record.stateChangeTick > 160) { // 8 seconds recovery delay
                        record.setState(CivilianState.WORKING, currentTick);
                        record.setAssignedShelterPos(null);
                    }
                }
            }
        }
    }

    /**
     * Identifies the best valid, uncompromised shelter and routes the civilian towards it.
     */
    private void selectAndNavigateToShelter(ServerLevel level, Settlement settlement, Villager villager,
                                            CivilianRecord record, List<Monster> threats, BlockPos threatOrigin) {
        BlockPos chosenShelter = null;
        double bestScore = Double.MAX_VALUE;

        // 1. Evaluate registered completed residential and civic buildings in settlement
        for (Settlement.RegisteredBuilding building : settlement.getBuildings()) {
            if (!building.isCompleted()) continue;
            String bpId = building.getBlueprintId();

            // Only residential/civic buildings provide valid civilian shelter
            if (bpId.contains("watchtower") || bpId.contains("training") || bpId.contains("farm") || bpId.contains("pen")) {
                continue;
            }

            Blueprint bp = BlueprintRegistry.get(bpId);
            int width = bp != null ? bp.getWidth() : 7;
            int depth = bp != null ? bp.getDepth() : 7;

            // Interior position of building
            BlockPos interiorPos = building.getPos().offset(width / 2, 1, depth / 2);

            // Compromise check: reject building if any monster is inside or within 8 blocks
            boolean compromised = false;
            for (Monster m : threats) {
                if (m.blockPosition().distSqr(interiorPos) < 64.0) {
                    compromised = true;
                    break;
                }
            }
            if (compromised) continue;

            // Distance from villager to shelter
            double distToVillager = villager.blockPosition().distSqr(interiorPos);

            // Avoid shelters that are closer to the threat origin than the villager currently is
            if (threatOrigin != null) {
                double shelterDistToThreat = interiorPos.distSqr(threatOrigin);
                double villagerDistToThreat = villager.blockPosition().distSqr(threatOrigin);
                if (shelterDistToThreat < villagerDistToThreat * 0.7) {
                    continue; // Walking towards the threat!
                }
            }

            if (distToVillager < bestScore) {
                bestScore = distToVillager;
                chosenShelter = interiorPos;
            }
        }

        // 2. Fallback: If no registered building matched, locate nearest covered block away from threat
        if (chosenShelter == null) {
            chosenShelter = findFallbackShelterBlock(level, villager.blockPosition(), threatOrigin);
        }

        if (chosenShelter != null) {
            record.setAssignedShelterPos(chosenShelter);
            villager.getNavigation().moveTo(chosenShelter.getX() + 0.5, chosenShelter.getY(), chosenShelter.getZ() + 0.5, 1.35);
        }
    }

    private BlockPos findFallbackShelterBlock(ServerLevel level, BlockPos currentPos, BlockPos threatPos) {
        for (int r = 4; r <= 20; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    BlockPos probe = currentPos.offset(dx, 0, dz);
                    if (level.isLoaded(probe) && !level.canSeeSky(probe) && level.getBlockState(probe).isAir()) {
                        if (threatPos == null || probe.distSqr(threatPos) >= currentPos.distSqr(threatPos)) {
                            return probe;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafeInterior(ServerLevel level, BlockPos pos, List<Monster> threats) {
        if (level.canSeeSky(pos)) return false;
        for (Monster m : threats) {
            if (m.blockPosition().distSqr(pos) < 36.0) return false;
        }
        return true;
    }

    public boolean canToggleDoor(BlockPos pos, long now) {
        Long lastToggle = doorToggleCooldowns.get(pos);
        return lastToggle == null || (now - lastToggle) > 40;
    }

    public void recordDoorToggle(BlockPos pos, long now) {
        doorToggleCooldowns.put(pos, now);
    }

    /**
     * Closes nearby open wooden doors to seal the shelter against invaders.
     * Rate-limited to prevent open-close thrashing loops.
     */
    private void coordinateDoorClosure(ServerLevel level, BlockPos villagerPos) {
        long now = level.getGameTime();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = villagerPos.offset(dx, dy, dz);
                    if (level.isLoaded(p)) {
                        BlockState state = level.getBlockState(p);
                        if (state.getBlock() instanceof DoorBlock door) {
                            if (state.getValue(DoorBlock.OPEN)) {
                                if (canToggleDoor(p, now)) { // 2s cooldown
                                    door.setOpen(null, level, state, p, false);
                                    recordDoorToggle(p, now);
                                    level.playSound(null, p, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isCivilianWorkSuspended(UUID villagerUuid) {
        CivilianState state = getCivilianState(villagerUuid);
        return state == CivilianState.THREAT_DETECTED || state == CivilianState.SEEKING_SHELTER || state == CivilianState.SHELTERED;
    }

    public void clear() {
        civilianRecords.clear();
        doorToggleCooldowns.clear();
    }
}
