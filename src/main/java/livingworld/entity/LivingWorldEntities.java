package livingworld.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Authoritative Entity Registry for Living World custom creatures.
 * Registers distinct EntityType instances and binds attribute suppliers.
 */
public class LivingWorldEntities {
    static {
        prepareRegistry();
    }

    private static void prepareRegistry() {
        try {
            java.lang.reflect.Field holdersField = net.minecraft.core.MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            holdersField.setAccessible(true);
            if (holdersField.get(BuiltInRegistries.ENTITY_TYPE) == null) {
                holdersField.set(BuiltInRegistries.ENTITY_TYPE, new java.util.IdentityHashMap<>());
            }
            java.lang.reflect.Field frozenField = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.set(BuiltInRegistries.ENTITY_TYPE, false);
        } catch (Throwable ignored) {
        }
    }

    public static final EntityType<SharkEntity> SHARK = register("shark",
        EntityType.Builder.of(SharkEntity::new, MobCategory.WATER_CREATURE).sized(1.4f, 0.7f).eyeHeight(0.35f));

    public static final EntityType<GargoyleSwooperEntity> GARGOYLE_SWOOPER = register("gargoyle_swooper",
        EntityType.Builder.of(GargoyleSwooperEntity::new, MobCategory.MONSTER).sized(0.9f, 0.6f).eyeHeight(0.3f));

    public static final EntityType<ZombieCowEntity> ZOMBIE_COW = register("zombie_cow",
        EntityType.Builder.of(ZombieCowEntity::new, MobCategory.CREATURE).sized(0.9f, 1.4f).eyeHeight(1.3f));

    public static final EntityType<ZombiePigEntity> ZOMBIE_PIG = register("zombie_pig",
        EntityType.Builder.of(ZombiePigEntity::new, MobCategory.CREATURE).sized(0.9f, 0.9f).eyeHeight(0.8f));

    public static final EntityType<ZombieSheepEntity> ZOMBIE_SHEEP = register("zombie_sheep",
        EntityType.Builder.of(ZombieSheepEntity::new, MobCategory.CREATURE).sized(0.9f, 1.3f).eyeHeight(1.2f));

    public static final EntityType<EndVoidStalkerEntity> END_VOID_STALKER = register("end_void_stalker",
        EntityType.Builder.of(EndVoidStalkerEntity::new, MobCategory.MONSTER).sized(0.8f, 2.2f).eyeHeight(2.0f));

    public static final EntityType<MutantZombieCommanderEntity> MUTANT_COMMANDER = register("mutant_commander",
        EntityType.Builder.of(MutantZombieCommanderEntity::new, MobCategory.MONSTER).sized(1.2f, 2.4f).eyeHeight(2.1f));

    public static final EntityType<IllagerSwordsmanEntity> ILLAGER_SWORDSMAN = register("illager_swordsman",
        EntityType.Builder.of(IllagerSwordsmanEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).ridingOffset(-0.7f));

    public static final EntityType<IllagerShieldbearerEntity> ILLAGER_SHIELDBEARER = register("illager_shieldbearer",
        EntityType.Builder.of(IllagerShieldbearerEntity::new, MobCategory.MONSTER).sized(0.65f, 1.95f).eyeHeight(1.74f).ridingOffset(-0.7f));

    public static final EntityType<VillageGuardEntity> VILLAGE_GUARD = register("village_guard",
        EntityType.Builder.of(VillageGuardEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).eyeHeight(1.74f).ridingOffset(-0.7f));

    public static final EntityType<VillageArcherEntity> VILLAGE_ARCHER = register("village_archer",
        EntityType.Builder.of(VillageArcherEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).eyeHeight(1.74f).ridingOffset(-0.7f));

    public static final EntityType<VillageDogEntity> VILLAGE_DOG = register("village_dog",
        EntityType.Builder.of(VillageDogEntity::new, MobCategory.CREATURE).sized(0.6f, 0.85f).eyeHeight(0.7f));

    public static final EntityType<VillageCavalryEntity> VILLAGE_CAVALRY = register("village_cavalry",
        EntityType.Builder.of(VillageCavalryEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).eyeHeight(1.74f).ridingOffset(-0.7f));

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.fromNamespaceAndPath("livingworld", name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        EntityType<T> type = builder.build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
    }

    public static void init() {
        // Register default attributes
        FabricDefaultAttributeRegistry.register(SHARK, SharkEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GARGOYLE_SWOOPER, GargoyleSwooperEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_COW, ZombieCowEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_PIG, ZombiePigEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_SHEEP, ZombieSheepEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(END_VOID_STALKER, EndVoidStalkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MUTANT_COMMANDER, MutantZombieCommanderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ILLAGER_SWORDSMAN, IllagerSwordsmanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ILLAGER_SHIELDBEARER, IllagerShieldbearerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(VILLAGE_GUARD, VillageGuardEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(VILLAGE_ARCHER, VillageArcherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(VILLAGE_DOG, VillageDogEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(VILLAGE_CAVALRY, VillageCavalryEntity.createAttributes());
    }
}
