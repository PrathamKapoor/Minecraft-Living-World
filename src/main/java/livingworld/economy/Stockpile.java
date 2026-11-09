package livingworld.economy;

import java.util.EnumMap;
import java.util.Map;

public class Stockpile {
    public enum ResourceType {
        FOOD,
        WOOD,
        STONE,
        IRON,
        WOOL,
        LEATHER
    }

    private final Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);

    public Stockpile() {
        for (ResourceType type : ResourceType.values()) {
            amounts.put(type, 0);
        }
    }

    public int get(ResourceType type) {
        return amounts.getOrDefault(type, 0);
    }

    public void add(ResourceType type, int amount) {
        if (amount > 0) {
            amounts.put(type, amounts.getOrDefault(type, 0) + amount);
        }
    }

    public boolean consume(ResourceType type, int amount) {
        if (amount <= 0) return true;
        int current = amounts.getOrDefault(type, 0);
        if (current >= amount) {
            amounts.put(type, current - amount);
            return true;
        }
        return false;
    }

    public boolean hasAll(Map<ResourceType, Integer> requirements) {
        for (Map.Entry<ResourceType, Integer> entry : requirements.entrySet()) {
            if (get(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean consumeAll(Map<ResourceType, Integer> requirements) {
        if (!hasAll(requirements)) {
            return false;
        }
        for (Map.Entry<ResourceType, Integer> entry : requirements.entrySet()) {
            consume(entry.getKey(), entry.getValue());
        }
        return true;
    }

    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new java.util.HashMap<>();
        for (Map.Entry<ResourceType, Integer> entry : amounts.entrySet()) {
            map.put(entry.getKey().name(), entry.getValue());
        }
        return map;
    }

    public void fromMap(Map<String, Integer> map) {
        if (map == null) return;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            try {
                ResourceType type = ResourceType.valueOf(entry.getKey());
                amounts.put(type, entry.getValue());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public String getSummary() {
        return String.format("Food: %d | Wood: %d | Stone: %d | Iron: %d | Wool: %d | Leather: %d",
            get(ResourceType.FOOD), get(ResourceType.WOOD), get(ResourceType.STONE),
            get(ResourceType.IRON), get(ResourceType.WOOL), get(ResourceType.LEATHER));
    }
}
