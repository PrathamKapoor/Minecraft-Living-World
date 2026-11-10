package livingworld.client;

import livingworld.client.model.SharkModel;
import livingworld.entity.SharkEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;
import net.minecraft.resources.Identifier;

/**
 * Dedicated Client Renderer for Great White Shark (Fix 1, F15).
 * Binds custom 3D SharkModel with pointed snout, dorsal fin, and vertical tail.
 */
public class SharkRenderer extends MobRenderer<SharkEntity, DolphinRenderState, SharkModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("livingworld", "textures/entity/shark/shark.png");

    public SharkRenderer(EntityRendererProvider.Context context) {
        super(context, new SharkModel(SharkModel.createBodyLayer().bakeRoot()), 0.85f);
    }

    @Override
    public Identifier getTextureLocation(DolphinRenderState state) {
        return TEXTURE;
    }

    @Override
    public DolphinRenderState createRenderState() {
        return new DolphinRenderState();
    }
}
