package livingworld.mixin;

import livingworld.worldgen.SettlementTerrainStabilizer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into StructureStart.placeInChunk to intercept village, empire, and outpost generation
 * and stabilize the terrain before jigsaw pieces place buildings, roads, and mobs.
 */
@Mixin(StructureStart.class)
public abstract class StructureStartMixin {
    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void livingworld$stabilizeTerrainBeforePlacement(
            WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            RandomSource random, BoundingBox box, ChunkPos chunkPos, CallbackInfo ci) {
        StructureStart self = (StructureStart) (Object) this;
        Structure structure = self.getStructure();
        if (structure == null) return;

        try {
            var reg = structureManager.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            Identifier id = reg.getKey(structure);
            if (id != null) {
                String path = id.getPath().toLowerCase();
                if (path.contains("village") || path.contains("outpost") || path.contains("empire")) {
                    SettlementTerrainStabilizer.get().stabilizeStructureStart(level, self, chunkPos);
                }
            }
        } catch (Exception ignored) {}
    }
}
