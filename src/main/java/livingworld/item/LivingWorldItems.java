package livingworld.item;

import java.util.HashMap;
import java.util.Map;
import livingworld.entity.LivingWorldEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Creative Spawn Eggs Registry for Living World Custom Mobs (Section 20).
 * Registers genuine SpawnEggItems for all 10 Living World custom entities.
 */
public class LivingWorldItems {
    private static final Map<String, Item> SPAWN_EGGS = new HashMap<>();

    static {
        prepareRegistry();
    }

    private static void prepareRegistry() {
        try {
            java.lang.reflect.Field holdersField = net.minecraft.core.MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            holdersField.setAccessible(true);
            if (holdersField.get(BuiltInRegistries.ITEM) == null) {
                holdersField.set(BuiltInRegistries.ITEM, new java.util.IdentityHashMap<>());
            }
            java.lang.reflect.Field frozenField = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.set(BuiltInRegistries.ITEM, false);
        } catch (Throwable ignored) {
        }
    }

    public static final Item SHARK_SPAWN_EGG = registerSpawnEgg("shark_spawn_egg", LivingWorldEntities.SHARK);
    public static final Item GARGOYLE_SWOOPER_SPAWN_EGG = registerSpawnEgg("gargoyle_swooper_spawn_egg", LivingWorldEntities.GARGOYLE_SWOOPER);
    public static final Item ZOMBIE_COW_SPAWN_EGG = registerSpawnEgg("zombie_cow_spawn_egg", LivingWorldEntities.ZOMBIE_COW);
    public static final Item ZOMBIE_PIG_SPAWN_EGG = registerSpawnEgg("zombie_pig_spawn_egg", LivingWorldEntities.ZOMBIE_PIG);
    public static final Item ZOMBIE_SHEEP_SPAWN_EGG = registerSpawnEgg("zombie_sheep_spawn_egg", LivingWorldEntities.ZOMBIE_SHEEP);
    public static final Item END_VOID_STALKER_SPAWN_EGG = registerSpawnEgg("end_void_stalker_spawn_egg", LivingWorldEntities.END_VOID_STALKER);
    public static final Item MUTANT_COMMANDER_SPAWN_EGG = registerSpawnEgg("mutant_commander_spawn_egg", LivingWorldEntities.MUTANT_COMMANDER);
    public static final Item ILLAGER_SWORDSMAN_SPAWN_EGG = registerSpawnEgg("illager_swordsman_spawn_egg", LivingWorldEntities.ILLAGER_SWORDSMAN);
    public static final Item ILLAGER_SHIELDBEARER_SPAWN_EGG = registerSpawnEgg("illager_shieldbearer_spawn_egg", LivingWorldEntities.ILLAGER_SHIELDBEARER);
    public static final Item VILLAGE_GUARD_SPAWN_EGG = registerSpawnEgg("village_guard_spawn_egg", LivingWorldEntities.VILLAGE_GUARD);
    public static final Item VILLAGE_ARCHER_SPAWN_EGG = registerSpawnEgg("village_archer_spawn_egg", LivingWorldEntities.VILLAGE_ARCHER);
    public static final Item VILLAGE_DOG_SPAWN_EGG = registerSpawnEgg("village_dog_spawn_egg", LivingWorldEntities.VILLAGE_DOG);
    public static final Item VILLAGE_CAVALRY_SPAWN_EGG = registerSpawnEgg("village_cavalry_spawn_egg", LivingWorldEntities.VILLAGE_CAVALRY);

    private static Item registerSpawnEgg(String name, EntityType<? extends Mob> entityType) {
        Identifier id = Identifier.fromNamespaceAndPath("livingworld", name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties props = new Item.Properties().setId(key).spawnEgg(entityType);
        Item item = new SpawnEggItem(props);
        Item registered = Registry.register(BuiltInRegistries.ITEM, key, item);
        SPAWN_EGGS.put(name, registered);
        return registered;
    }

    public static Map<String, Item> getSpawnEggs() {
        return SPAWN_EGGS;
    }

    public static void init() {
        // Classloading triggers static registration
    }
}
