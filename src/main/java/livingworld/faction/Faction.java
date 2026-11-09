package livingworld.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public class Faction {
    private final String id;
    private String name;
    private final FactionType type;
    private final Map<UUID, Integer> playerReputations = new HashMap<>();
    private BlockPos territoryCenter;
    private int territoryRadius;
    private double morale = 1.0;

    public Faction(String id, String name, FactionType type, BlockPos territoryCenter, int territoryRadius) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.territoryCenter = territoryCenter;
        this.territoryRadius = territoryRadius;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FactionType getType() {
        return type;
    }

    public BlockPos getTerritoryCenter() {
        return territoryCenter;
    }

    public void setTerritoryCenter(BlockPos center) {
        this.territoryCenter = center;
    }

    public int getTerritoryRadius() {
        return territoryRadius;
    }

    public void setTerritoryRadius(int radius) {
        this.territoryRadius = radius;
    }

    public double getMorale() {
        return morale;
    }

    public void setMorale(double morale) {
        this.morale = Math.max(0.0, Math.min(1.0, morale));
    }

    public int getReputation(UUID playerUuid) {
        return playerReputations.getOrDefault(playerUuid, 0);
    }

    public void adjustReputation(UUID playerUuid, int delta) {
        int current = getReputation(playerUuid);
        int updated = Math.max(-100, Math.min(100, current + delta));
        playerReputations.put(playerUuid, updated);
    }

    public boolean isHostileTo(UUID playerUuid) {
        if (type == FactionType.UNDEAD || type == FactionType.ILLAGER) {
            return true;
        }
        return getReputation(playerUuid) < -25;
    }

    public boolean isAlliedWith(UUID playerUuid) {
        return getReputation(playerUuid) >= 50;
    }

    public boolean isInTerritory(BlockPos pos) {
        if (territoryCenter == null) return false;
        double dx = pos.getX() - territoryCenter.getX();
        double dz = pos.getZ() - territoryCenter.getZ();
        return (dx * dx + dz * dz) <= (territoryRadius * territoryRadius);
    }

    public Map<UUID, Integer> getReputations() {
        return playerReputations;
    }
}
