package livingworld.piglin;

import java.util.*;
import livingworld.config.FeatureConfig;
import livingworld.faction.Faction;
import livingworld.faction.FactionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Nether Piglin Tribes, Territory Border Warfare, and Trade (F11).
 */
public class PiglinTribeManager {
    private static final PiglinTribeManager INSTANCE = new PiglinTribeManager();

    public enum Tribe {
        CRIMSON_TUSKS("piglin_crimson", "Crimson Tusks", "§6[Crimson Tusks]"),
        WARPED_SHADOWS("piglin_warped", "Warped Shadows", "§b[Warped Shadows]");

        public final String factionId;
        public final String displayName;
        public final String tagPrefix;

        Tribe(String factionId, String displayName, String tagPrefix) {
            this.factionId = factionId;
            this.displayName = displayName;
            this.tagPrefix = tagPrefix;
        }
    }

    // Dynamic contested border coordinate along Z-axis
    private int territoryBorderZ = 0;
    // Map: Piglin UUID -> Assigned Tribe
    private final Map<UUID, Tribe> piglinTribes = new HashMap<>();

    public static PiglinTribeManager get() {
        return INSTANCE;
    }

    public int getTerritoryBorderZ() {
        return territoryBorderZ;
    }

    public void setTerritoryBorderZ(int borderZ) {
        this.territoryBorderZ = borderZ;
    }

    public void assignTribeOnLoad(Piglin piglin, ServerLevel level) {
        if (!FeatureConfig.get().enablePiglinTribes) {
            return;
        }

        // Only in Nether dimension
        if (level.dimension() != Level.NETHER) {
            return;
        }

        BlockPos pos = piglin.blockPosition();
        Tribe assigned = (pos.getZ() >= territoryBorderZ) ? Tribe.CRIMSON_TUSKS : Tribe.WARPED_SHADOWS;
        piglinTribes.put(piglin.getUUID(), assigned);

        piglin.setCustomName(Component.literal(assigned.tagPrefix + " Hunter"));
        piglin.setCustomNameVisible(false);
    }

    public void tickTribalSkirmishes(ServerLevel level) {
        if (!FeatureConfig.get().enablePiglinTribes) {
            return;
        }

        // Clean dead piglins
        piglinTribes.keySet().removeIf(uuid -> level.getEntity(uuid) == null || !level.getEntity(uuid).isAlive());

        for (Map.Entry<UUID, Tribe> entry : new ArrayList<>(piglinTribes.entrySet())) {
            if (level.getEntity(entry.getKey()) instanceof Piglin piglin && piglin.isAlive()) {
                if (piglin.getTarget() != null) continue;

                // Look for rival tribe members in border territory
                Tribe myTribe = entry.getValue();
                AABB searchArea = piglin.getBoundingBox().inflate(14.0);
                List<Piglin> nearby = level.getEntitiesOfClass(Piglin.class, searchArea, p -> p != piglin && p.isAlive());

                for (Piglin other : nearby) {
                    Tribe otherTribe = piglinTribes.get(other.getUUID());
                    if (otherTribe != null && otherTribe != myTribe) {
                        // Tribal Skirmish!
                        piglin.setTarget(other);
                        other.setTarget(piglin);

                        // Territorial contest outcome
                        if (myTribe == Tribe.CRIMSON_TUSKS) {
                            territoryBorderZ -= 2; // Crimson Tusks advance northward
                        } else {
                            territoryBorderZ += 2; // Warped Shadows advance southward
                        }
                        livingworld.LivingWorldMod.LOGGER.info("[Living World] Nether Tribal Skirmish: " + myTribe.displayName + " clashed with " + otherTribe.displayName + " at Z=" + (int)piglin.getZ() + "! Contested border displaced to Z=" + territoryBorderZ);
                        break;
                    }
                }
            }
        }
    }

    public void rewardTribeReputation(UUID playerUuid, Tribe tribe, int delta) {
        Faction faction = FactionManager.get().getFaction(tribe.factionId);
        if (faction != null) {
            faction.adjustReputation(playerUuid, delta);
        }
    }
}
