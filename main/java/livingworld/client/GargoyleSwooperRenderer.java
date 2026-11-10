package livingworld.client;

import livingworld.entity.GargoyleSwooperEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;

public class GargoyleSwooperRenderer extends MobRenderer<GargoyleSwooperEntity, PhantomRenderState, PhantomModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("livingworld", "textures/entity/swooper/gargoyle_swooper.png");

    public GargoyleSwooperRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel(context.bakeLayer(ModelLayers.PHANTOM)), 0.75f);
    }

    @Override
    public Identifier getTextureLocation(PhantomRenderState state) {
        return TEXTURE;
    }

    @Override
    public PhantomRenderState createRenderState() {
        return new PhantomRenderState();
    }
}
