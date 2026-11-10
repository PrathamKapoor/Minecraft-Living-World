package livingworld.client;

import livingworld.entity.EndVoidStalkerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;

public class EndVoidStalkerRenderer extends MobRenderer<EndVoidStalkerEntity, EndermanRenderState, EndermanModel<EndermanRenderState>> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("livingworld", "textures/entity/end/end_void_stalker.png");

    public EndVoidStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5f);
    }

    @Override
    public Identifier getTextureLocation(EndermanRenderState state) {
        return TEXTURE;
    }

    @Override
    public EndermanRenderState createRenderState() {
        return new EndermanRenderState();
    }
}
