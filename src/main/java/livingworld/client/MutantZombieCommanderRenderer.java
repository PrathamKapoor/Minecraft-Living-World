package livingworld.client;

import livingworld.client.model.MutantZombieCommanderModel;
import livingworld.entity.MutantZombieCommanderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/**
 * Dedicated Client Renderer for 3-Headed Mutant Zombie Commander (Fix 4, F10).
 * Binds custom MutantZombieCommanderModel with 3 heads, broad hulking shoulders, and boss scale.
 */
public class MutantZombieCommanderRenderer extends MobRenderer<MutantZombieCommanderEntity, ZombieRenderState, MutantZombieCommanderModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("livingworld", "textures/entity/zombie/mutant_commander.png");

    public MutantZombieCommanderRenderer(EntityRendererProvider.Context context) {
        super(context, new MutantZombieCommanderModel(MutantZombieCommanderModel.createBodyLayer().bakeRoot()), 1.1f);
    }

    @Override
    protected void scale(ZombieRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(1.45f, 1.45f, 1.45f);
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return TEXTURE;
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }
}
