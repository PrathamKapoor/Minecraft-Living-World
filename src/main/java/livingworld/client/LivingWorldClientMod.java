package livingworld.client;

import livingworld.entity.LivingWorldEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Client-side Mod Initializer for Living World.
 * Registers entity renderers and client visual bindings.
 */
public class LivingWorldClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(LivingWorldEntities.SHARK, SharkRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.GARGOYLE_SWOOPER, GargoyleSwooperRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.ZOMBIE_COW, ZombieCowRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.ZOMBIE_PIG, ZombiePigRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.ZOMBIE_SHEEP, ZombieSheepRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.END_VOID_STALKER, EndVoidStalkerRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.MUTANT_COMMANDER, MutantZombieCommanderRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.ILLAGER_SWORDSMAN, IllagerSwordsmanRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.ILLAGER_SHIELDBEARER, IllagerShieldbearerRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.VILLAGE_GUARD, VillageGuardRenderer::new);
        EntityRendererRegistry.register(LivingWorldEntities.VILLAGE_ARCHER, VillageArcherRenderer::new);
    }
}
